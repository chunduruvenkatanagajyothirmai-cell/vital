"""
Vital - Flask Backend Application
REST API & Flask-SocketIO Event Server for Emergency Dispatch and Checkup Guidance.
"""

from datetime import datetime, timedelta
import uuid
import threading
import time
from flask import Flask, request, jsonify
from flask_socketio import SocketIO, emit, join_room

from classifier import (
    classify_department,
    classify_severity,
    calculate_eta_minutes,
    build_attempt_queue,
    get_nearest_hospital,
    SUPREME_COURT_MANDATE_TITLE,
    SUPREME_COURT_MANDATE_MESSAGE
)
from models import (
    get_all_hospitals,
    get_hospital_by_id,
    get_all_doctors,
    get_all_blood_banks,
    reserve_blood_unit,
    save_emergency_case,
    get_emergency_case
)

app = Flask(__name__)
app.config["SECRET_KEY"] = "vital-secret-key-2026"
socketio = SocketIO(app, cors_allowed_origins="*")

# In-memory timer tracker for active cases
active_timers = {}


def case_countdown_worker(case_id: str):
    """30-second response window countdown thread per attempt."""
    for remaining in range(30, 0, -1):
        if case_id not in active_timers:
            return  # Cancelled or Confirmed early
        socketio.emit("timer_tick", {"case_id": case_id, "remaining_seconds": remaining}, to=case_id)
        time.sleep(1)

    # 30 seconds elapsed without confirmation -> Advance Queue
    if case_id in active_timers:
        handle_timeout_internal(case_id, reason="Hospital 30-second response window timed out")


def handle_timeout_internal(case_id: str, reason: str):
    case = get_emergency_case(case_id)
    if not case or case["status"] != "DISPATCHING":
        return

    curr_idx = case["current_attempt_index"]
    queue = case["attempt_queue"]

    # Mark current attempt as timed out
    queue[curr_idx]["status"] = "TIMED_OUT"
    queue[curr_idx]["note"] = reason

    next_idx = curr_idx + 1
    all_hospitals = get_all_hospitals()

    if next_idx < len(queue):
        # Advance to next hospital in queue
        queue[next_idx]["status"] = "WAITING"
        case["current_attempt_index"] = next_idx
        case["attempt_queue"] = queue
        case["timestamps"]["response_expires_at"] = (datetime.now() + timedelta(seconds=30)).strftime("%I:%M:%S %p")
        save_emergency_case(case)

        # Notify clients
        socketio.emit("queue_advanced", {
            "case_id": case_id,
            "next_attempt_index": next_idx,
            "hospital_name": queue[next_idx]["hospital_name"],
            "eta_minutes": queue[next_idx]["eta_minutes"],
            "remaining_seconds": 30
        }, to=case_id)

        # Restart countdown
        active_timers[case_id] = True
        threading.Thread(target=case_countdown_worker, args=(case_id,), daemon=True).start()
    else:
        # Queue exhausted! Activate Supreme Court Emergency Fallback Mandate
        active_timers.pop(case_id, None)
        nearest = get_nearest_hospital(all_hospitals)

        case["status"] = "EXHAUSTED_FALLBACK"
        case["confirmed_hospital_id"] = nearest["id"]
        case["timestamps"]["confirmed_at"] = datetime.now().strftime("%I:%M:%S %p")
        save_emergency_case(case)

        socketio.emit("supreme_court_fallback", {
            "case_id": case_id,
            "title": SUPREME_COURT_MANDATE_TITLE,
            "message": SUPREME_COURT_MANDATE_MESSAGE,
            "nearest_hospital": nearest,
            "status": "EXHAUSTED_FALLBACK"
        }, to=case_id)


# ---------------- REST ENDPOINTS ---------------- #

@app.route("/api/health", methods=["GET"])
def health_check():
    return jsonify({
        "status": "healthy",
        "service": "Vital Emergency Dispatch Engine",
        "tagline": "Not just an alert. A confirmed response."
    })


@app.route("/api/hospitals", methods=["GET"])
def list_hospitals():
    return jsonify({"hospitals": get_all_hospitals()})


@app.route("/api/doctors", methods=["GET"])
def list_doctors():
    department = request.args.get("department")
    query = request.args.get("query")
    doctors = get_all_doctors(department=department, query=query)
    return jsonify({"doctors": doctors})


@app.route("/api/auth/staff-login", methods=["POST"])
def staff_login():
    payload = request.get_json() or {}
    hospital_id = payload.get("hospital_id")
    password = payload.get("password", "").strip()

    hospital = get_hospital_by_id(hospital_id)
    if not hospital:
        return jsonify({"error": "Hospital not found."}), 404

    import re
    first_word = re.split(r"[\s.-]+", hospital["name"].strip())[0].lower()
    clean_word = re.sub(r"[^a-z0-9]", "", first_word)
    expected_password = f"{clean_word}123"

    if password == expected_password:
        return jsonify({
            "success": True,
            "hospital": hospital,
            "message": "Staff authentication successful."
        }), 200
    else:
        return jsonify({
            "success": False,
            "error": f"Invalid password. Expected format: first word in lowercase + 123 (e.g. '{expected_password}')."
        }), 401


@app.route("/api/emergency/dispatch", methods=["POST"])
def dispatch_emergency():
    payload = request.get_json() or {}
    patient_name = payload.get("patient_name", "Unidentified Patient")
    location = payload.get("location", "Central Bengaluru")
    description = payload.get("description", "")
    requested_blood = payload.get("blood_group")

    if not description.strip():
        return jsonify({"error": "Emergency symptoms description is required."}), 400

    # 1. Rule-based NLP Department Classifier
    dept = classify_department(description)

    # 2. Severity Rule Classifier
    severity = classify_severity(description, dept)

    # 3. Closed-Loop Queue Builder
    all_hospitals = get_all_hospitals()
    queue = build_attempt_queue(dept, all_hospitals)

    # 4. Blood reservation check
    blood_reservation = None
    if requested_blood and requested_blood != "None":
        blood_reservation = reserve_blood_unit(requested_blood)

    case_id = f"CASE-{uuid.uuid4().hex[:8].upper()}"
    now = datetime.now()
    created_at = now.strftime("%I:%M:%S %p")
    expires_at = (now + timedelta(seconds=30)).strftime("%I:%M:%S %p")

    case_data = {
        "id": case_id,
        "patient_name": patient_name,
        "location": location,
        "department": dept,
        "severity": severity,
        "description": description,
        "blood_reservation": blood_reservation,
        "attempt_queue": queue,
        "current_attempt_index": 0,
        "status": "DISPATCHING",
        "timestamps": {
            "created_at": created_at,
            "response_expires_at": expires_at,
            "confirmed_at": None
        },
        "confirmed_hospital_id": None
    }

    save_emergency_case(case_data)

    # Start 30s background countdown thread
    active_timers[case_id] = True
    threading.Thread(target=case_countdown_worker, args=(case_id,), daemon=True).start()

    # Emit WebSocket event
    socketio.emit("case_dispatched", case_data)

    return jsonify({"case": case_data}), 201


@app.route("/api/emergency/case/<case_id>", methods=["GET"])
def get_case_status(case_id):
    case = get_emergency_case(case_id)
    if not case:
        return jsonify({"error": "Case not found."}), 404

    # Attach hospital detail if confirmed
    if case.get("confirmed_hospital_id"):
        case["confirmed_hospital"] = get_hospital_by_id(case["confirmed_hospital_id"])

    return jsonify({"case": case})


@app.route("/api/emergency/case/<case_id>/confirm", methods=["POST"])
def confirm_case(case_id):
    case = get_emergency_case(case_id)
    if not case:
        return jsonify({"error": "Case not found."}), 404
    if case["status"] != "DISPATCHING":
        return jsonify({"error": f"Cannot confirm case in '{case['status']}' state."}), 400

    payload = request.get_json() or {}
    note = payload.get("note", "Confirmed by Receiving Hospital ER Desk")

    # Stop countdown timer
    active_timers.pop(case_id, None)

    curr_idx = case["current_attempt_index"]
    queue = case["attempt_queue"]
    target_hospital_id = queue[curr_idx]["hospital_id"]

    queue[curr_idx]["status"] = "ACCEPTED"
    queue[curr_idx]["note"] = note

    case["status"] = "CONFIRMED"
    case["confirmed_hospital_id"] = target_hospital_id
    case["timestamps"]["confirmed_at"] = datetime.now().strftime("%I:%M:%S %p")
    save_emergency_case(case)

    confirmed_hospital = get_hospital_by_id(target_hospital_id)
    case["confirmed_hospital"] = confirmed_hospital

    socketio.emit("case_confirmed", {
        "case_id": case_id,
        "status": "CONFIRMED",
        "hospital": confirmed_hospital,
        "note": note,
        "timestamps": case["timestamps"]
    }, to=case_id)

    return jsonify({"case": case, "message": "Emergency response successfully confirmed."})


@app.route("/api/emergency/case/<case_id>/timeout", methods=["POST"])
def trigger_timeout(case_id):
    payload = request.get_json() or {}
    reason = payload.get("reason", "Hospital ER Divert Triggered")
    handle_timeout_internal(case_id, reason=reason)
    return jsonify({"message": f"Timeout/divert processed for case {case_id}."})


# ---------------- WEBSOCKET EVENTS ---------------- #

@socketio.on("connect")
def on_connect():
    emit("connected", {"message": "Connected to Vital Real-time Dispatch Socket"})


@socketio.on("subscribe_case")
def on_subscribe_case(data):
    case_id = data.get("case_id")
    if case_id:
        join_room(case_id)
        emit("subscribed", {"case_id": case_id})


@socketio.on("er_accept_alert")
def on_er_accept(data):
    case_id = data.get("case_id")
    if case_id:
        confirm_case(case_id)


if __name__ == "__main__":
    print("[*] Starting Vital Backend Server on http://0.0.0.0:5000 ...")
    socketio.run(app, host="0.0.0.0", port=5000, debug=True)
