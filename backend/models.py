"""
Vital - SQLite Data Layer & Helper Functions
"""

import sqlite3
import json
from typing import List, Dict, Any, Optional

DB_FILE = "vital.db"


def get_db():
    conn = sqlite3.connect(DB_FILE)
    conn.row_factory = sqlite3.Row
    return conn


def get_all_hospitals() -> List[Dict[str, Any]]:
    conn = get_db()
    c = conn.cursor()
    c.execute("SELECT * FROM hospitals ORDER BY distance_km ASC")
    rows = c.fetchall()
    conn.close()

    hospitals = []
    for r in rows:
        hospitals.append({
            "id": r["id"],
            "name": r["name"],
            "location": r["location"],
            "distance_km": r["distance_km"],
            "phone": r["phone"],
            "departments": json.loads(r["departments"]),
            "er_beds_available": r["er_beds_available"],
            "has_trauma_center": bool(r["has_trauma_center"])
        })
    return hospitals


def get_hospital_by_id(hospital_id: str) -> Optional[Dict[str, Any]]:
    conn = get_db()
    c = conn.cursor()
    c.execute("SELECT * FROM hospitals WHERE id = ?", (hospital_id,))
    r = c.fetchone()
    conn.close()
    if not r:
        return None
    return {
        "id": r["id"],
        "name": r["name"],
        "location": r["location"],
        "distance_km": r["distance_km"],
        "phone": r["phone"],
        "departments": json.loads(r["departments"]),
        "er_beds_available": r["er_beds_available"],
        "has_trauma_center": bool(r["has_trauma_center"])
    }


def get_all_doctors(department: Optional[str] = None, query: Optional[str] = None) -> List[Dict[str, Any]]:
    conn = get_db()
    c = conn.cursor()
    sql = "SELECT * FROM doctors WHERE 1=1"
    params = []
    if department and department != "All":
        sql += " AND LOWER(department) = LOWER(?)"
        params.append(department)
    if query:
        q = f"%{query}%"
        sql += " AND (LOWER(name) LIKE LOWER(?) OR LOWER(expertise) LIKE LOWER(?) OR LOWER(hospital) LIKE LOWER(?))"
        params.extend([q, q, q])

    c.execute(sql, params)
    rows = c.fetchall()
    conn.close()
    return [dict(r) for r in rows]


def get_all_blood_banks() -> List[Dict[str, Any]]:
    conn = get_db()
    c = conn.cursor()
    c.execute("SELECT * FROM blood_banks ORDER BY distance_km ASC")
    rows = c.fetchall()
    conn.close()

    banks = []
    for r in rows:
        banks.append({
            "id": r["id"],
            "name": r["name"],
            "location": r["location"],
            "distance_km": r["distance_km"],
            "phone": r["phone"],
            "stock": json.loads(r["stock"])
        })
    return banks


def reserve_blood_unit(blood_group: str) -> Optional[str]:
    conn = get_db()
    c = conn.cursor()
    c.execute("SELECT * FROM blood_banks ORDER BY distance_km ASC")
    banks = c.fetchall()

    for b in banks:
        stock = json.loads(b["stock"])
        if stock.get(blood_group, 0) > 0:
            stock[blood_group] -= 1
            c.execute("UPDATE blood_banks SET stock = ? WHERE id = ?", (json.dumps(stock), b["id"]))
            conn.commit()
            conn.close()
            return f"{blood_group} (1 Unit Reserved at {b['name']})"

    conn.close()
    return None


def save_emergency_case(case_data: Dict[str, Any]):
    conn = get_db()
    c = conn.cursor()
    c.execute("""
        INSERT OR REPLACE INTO cases (
            id, patient_name, location, department, severity, description,
            blood_reservation, attempt_queue, current_attempt_index, status,
            timestamps, confirmed_hospital_id
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """, (
        case_data["id"],
        case_data["patient_name"],
        case_data["location"],
        case_data["department"],
        case_data["severity"],
        case_data["description"],
        case_data.get("blood_reservation"),
        json.dumps(case_data["attempt_queue"]),
        case_data["current_attempt_index"],
        case_data["status"],
        json.dumps(case_data["timestamps"]),
        case_data.get("confirmed_hospital_id")
    ))
    conn.commit()
    conn.close()


def get_emergency_case(case_id: str) -> Optional[Dict[str, Any]]:
    conn = get_db()
    c = conn.cursor()
    c.execute("SELECT * FROM cases WHERE id = ?", (case_id,))
    r = c.fetchone()
    conn.close()
    if not r:
        return None

    return {
        "id": r["id"],
        "patient_name": r["patient_name"],
        "location": r["location"],
        "department": r["department"],
        "severity": r["severity"],
        "description": r["description"],
        "blood_reservation": r["blood_reservation"],
        "attempt_queue": json.loads(r["attempt_queue"]),
        "current_attempt_index": r["current_attempt_index"],
        "status": r["status"],
        "timestamps": json.loads(r["timestamps"]),
        "confirmed_hospital_id": r["confirmed_hospital_id"]
    }
