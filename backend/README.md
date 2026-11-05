# Vital — Emergency-Response & Checkup-Guidance Platform
> **Tagline:** *"Not just an alert. A confirmed response."*  
> **Phase 1 Architecture, Data Schemas, Dispatch Engine & API Specifications**

---

## 1. Complete Project Folder Structure

```text
vital-platform/
│
├── seed.json                      # Seed dataset (Hospitals, Doctors, Blood Banks)
├── seed.py                        # Python data validation & SQLite seeder
├── vital.db                       # Generated SQLite database
│
├── backend/                       # Python Flask Backend & WebSocket Service
│   ├── app.py                     # Flask app, REST routes, WebSocket events
│   ├── classifier.py              # Rule-based NLP Level-2 classifier & queue builder
│   ├── models.py                  # SQLite data access layer
│   ├── requirements.txt           # Python dependencies (Flask, Flask-SocketIO)
│   └── README.md                  # Phase 1 documentation & setup manual
│
├── frontend/                      # React + Vite Web Client (Reference Structure)
│   ├── src/
│   │   ├── components/            # UI Cards, Timers, Status Badges
│   │   ├── hooks/                 # useSocket, useCountdown
│   │   ├── pages/                 # EmergencySOS, DoctorDirectory, BloodBanks, ERConsole
│   │   └── App.jsx
│   └── tailwind.config.js
│
└── app/                           # Android Kotlin Jetpack Compose Application
    └── src/main/java/com/example/
        ├── MainActivity.kt        # Edge-to-edge container & tab navigation
        ├── data/
        │   ├── model/             # Hospital, Doctor, BloodBank, EmergencyCase
        │   └── repository/        # Reactive VitalRepository with seed data
        ├── domain/engine/         # VitalDispatchEngine (Classifiers, ETA, Queue)
        ├── ui/
        │   ├── components/        # VitalHeader, StatusBadge, RadialCountdownTimer
        │   ├── screens/           # VitalEmergencyScreen, VitalDoctorsScreen, etc.
        │   └── theme/             # Color tokens, Theme (soft off-white), Typography
        └── res/
            └── drawable/          # ic_vital_logo, ic_launcher adaptive icons
```

---

## 2. Data Schemas

### 2.1 Hospital
```json
{
  "id": "hosp-001",
  "name": "Apex Metro Super-Speciality Hospital",
  "location": "84 Inner Ring Road, Koramangala, Bengaluru",
  "distance_km": 2.4,
  "phone": "+91 80 2553 9000",
  "departments": ["Trauma", "Cardiology", "Neurology", "Orthopedics", "Pulmonology", "General Medicine"],
  "er_beds_available": 5,
  "has_trauma_center": true
}
```

### 2.2 Doctor
```json
{
  "id": "doc-001",
  "name": "Dr. Rajesh Iyer",
  "department": "Trauma",
  "expertise": "Polytrauma, Acute Resuscitation, Shock Management",
  "timings": "08:00 AM - 04:00 PM (Mon-Sat)",
  "hospital": "Apex Metro Super-Speciality Hospital",
  "phone": "+91 80 2553 9001"
}
```

### 2.3 Blood Bank
```json
{
  "id": "bb-001",
  "name": "Red Cross Central Blood Bank",
  "location": "26 Mahatma Gandhi Rd, Central Bengaluru",
  "distance_km": 3.2,
  "phone": "+91 80 2226 8450",
  "stock": {
    "A+": 24, "A-": 8, "B+": 31, "B-": 6,
    "O+": 42, "O-": 14, "AB+": 18, "AB-": 4
  }
}
```

### 2.4 Emergency Case
```json
{
  "id": "CASE-4A9B1C2D",
  "patient_name": "Unidentified Male (Approx 35)",
  "location": "Outer Ring Road, Koramangala, Bengaluru",
  "department": "Trauma",
  "severity": "Critical",
  "description": "High-speed crash, severe arterial bleeding from leg, unresponsive.",
  "blood_reservation": "O+ (1 Unit Reserved at Apex Metro Hospital Rotary Blood Center)",
  "attempt_queue": [
    {
      "hospital_id": "hosp-001",
      "hospital_name": "Apex Metro Super-Speciality Hospital",
      "distance_km": 2.4,
      "eta_minutes": 5,
      "attempt_number": 1,
      "status": "WAITING",
      "window_seconds": 30
    }
  ],
  "current_attempt_index": 0,
  "status": "DISPATCHING",
  "timestamps": {
    "created_at": "03:15:20 PM",
    "response_expires_at": "03:15:50 PM",
    "confirmed_at": null
  },
  "confirmed_hospital_id": null
}
```

---

## 3. Dispatch Logic Engine Specification

### 3.1 Keyword-to-Department Rule Classifier (Level-2 Rule-based NLP)
- **Trauma:** `accident`, `crash`, `bleeding`, `blood loss`, `fall`, `injury`, `fracture open`, `wound`, `stab`, `burn`, `trauma`, `unconscious`, `head injury`, `crush`, `collision`, `hemorrhage`.
- **Cardiology:** `chest pain`, `heart`, `cardiac`, `angina`, `attack`, `pulse`, `palpitations`, `left arm`, `chest tightness`, `arrhythmia`.
- **Neurology:** `stroke`, `paralysis`, `seizure`, `convulsion`, `speech loss`, `facial droop`, `numbness`, `coma`, `blackout`, `slurred`.
- **Pulmonology:** `breathing`, `choking`, `suffocating`, `asthma`, `gasping`, `shortness of breath`, `cyanosis`, `lungs`.
- **Pediatrics:** `infant`, `baby`, `child`, `toddler`, `pediatric`, `newborn`, `kid`.
- **ENT:** `throat`, `ear`, `nosebleed`, `swallowed coin`, `swallowed object`, `vocal`, `ear bleed`.
- **Orthopedics:** `bone`, `joint`, `dislocation`, `sprain`, `knee`, `spine`, `broken leg`.
- **General Medicine:** Default fallback when no specific acute department keywords match.

### 3.2 Severity Classifier
- **Critical:** Triggered by unconsciousness, arterial bleeding, cardiac arrest, stroke signs, choking/cyanosis.
- **Serious:** High pain, non-compound fractures, deep lacerations, dyspnea.
- **Normal:** Stable vital indications, non-acute illness.

### 3.3 ETA Calculator
$$\text{ETA (minutes)} = \left(\frac{\text{distance\_km}}{30}\right) \times 60 = \text{distance\_km} \times 2$$
*(Assumes 30 km/h urban emergency transit rate. Clamped to minimum 1 minute).*

### 3.4 Closed-Loop Queue & Supreme Court Fallback Protocol
1. Hospitals matching the classified department are sorted by `distance_km` ascending.
2. Attempt 1 receives an emergency notification with a strict **30-second response window**.
3. If confirmed by ER staff, the case transitions to `CONFIRMED`.
4. If 30 seconds expire or ER diverts, Attempt 2 is queried automatically.
5. If all matching hospitals exhaust without confirmation:
   - System triggers the **Supreme Court Fallback Mandate** (*Pt. Parmanand Katara v. Union of India, 1989*).
   - Directs the bystander unconditionally to the absolute nearest hospital for mandatory stabilization.

---

## 4. REST API & WebSocket Event Definitions

### 4.1 REST Endpoints
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/health` | Health & service info |
| `GET` | `/api/hospitals` | List all network hospitals |
| `GET` | `/api/doctors` | List doctors with `?department=` & `?query=` filters |
| `GET` | `/api/blood-banks` | List blood bank live stock |
| `POST` | `/api/emergency/dispatch` | Transmit new emergency alert, run classifier, build queue |
| `GET` | `/api/emergency/case/<id>` | Fetch current case state & timestamps |
| `POST` | `/api/emergency/case/<id>/confirm` | Hospital ER confirms bed and trauma readiness |
| `POST` | `/api/emergency/case/<id>/timeout` | Timeout / divert trigger advancing closed loop |

### 4.2 WebSocket Events
| Event Name | Direction | Payload |
|---|---|---|
| `subscribe_case` | Client $\to$ Server | `{"case_id": "CASE-1234"}` |
| `timer_tick` | Server $\to$ Client | `{"case_id": "...", "remaining_seconds": 24}` |
| `case_confirmed` | Server $\to$ Client | `{"case_id": "...", "hospital": {...}, "status": "CONFIRMED"}` |
| `queue_advanced` | Server $\to$ Client | `{"case_id": "...", "next_attempt_index": 1, "hospital_name": "..."}` |
| `supreme_court_fallback` | Server $\to$ Client | `{"case_id": "...", "nearest_hospital": {...}, "mandate": "..."}` |

---

## 5. Setup & Execution Instructions

```bash
# 1. Install dependencies
pip install -r backend/requirements.txt

# 2. Run the seed script to validate data and create vital.db
python3 seed.py

# 3. Start the Flask backend server
python3 backend/app.py
```
Server runs at `http://localhost:5000`.

---

## 6. Phase 2: Public Bystander Emergency Flow

### 6.1 Home Screen Navigation
- **Header:** Features `Hospital Staff Login` and `Reset Demo` for quick state resets during live reviews.
- **Two Core Action Cards:**
  - `🚨 Emergency`: Immediate hospital confirmation with 30s closed-loop queue.
  - `🩺 Normal Checkup`: Specialist directory, consultation timings & appointments.

### 6.2 6-Step Mobile-First Bystander Emergency Form
1. **Step 1:** Patient Identification (Optional) with quick buttons (`👤 Me`, `❓ Unknown Male`, `❓ Unknown Female`, `🧒 Child`).
2. **Step 2:** Incident Location featuring `📍 Use My Location` (GPS) with automatic fallback to manual address input (`✏️ Enter Manually`). Never blocks dispatch on location.
3. **Step 3:** Quick-Select Condition Grid with universal pictograms:
   - `🩸 Bleeding / Injury` (Trauma)
   - `🫁 Can't Breathe Well` (Pulmonology)
   - `🆘 Unconscious` (Neurology)
   - `❤️ Chest Pain` (Cardiology)
   - `➕ Something Else` (General Medicine)
4. **Step 4:** Free-text Description Box with automated pre-fill from condition selection.
5. **Step 5:** Blood Bank Step (Conditionally visible ONLY for Trauma/Bleeding):
   - Blood Group selection (`A+`, `A-`, `B+`, `B-`, `O+`, `O-`, `AB+`, `AB-`)
   - Units needed selector (`1`, `2`, `3` units)
   - `Check & Reserve Blood` button querying nearest regional stock with live availability confirmation.
6. **Step 6:** `SEND EMERGENCY ALERT` button triggering API dispatch, setting up 30s countdown, and opening the live timeline.

### 6.3 Live Case Status & Case Timeline Panel
- **Status Indicator:**
  - `WAITING` (Amber `#B9791E`) — 30-second candidate response countdown window.
  - `CONFIRMED` (Green `#2F8F5B`) — Hospital ER confirmed bed, trauma doctor & routing.
  - `EXHAUSTED_FALLBACK` (Blue `#3E6B99`) — Supreme Court emergency fallback to closest facility.
- **Timestamped Event Log:** Plain language updates with color-coded dot indicators (`green`, `amber`, `red`, `blue`) documenting each milestone in the closed-loop dispatch cycle.

---

## 7. Phase 3: Private Authenticated Hospital Staff Dashboard

### 7.1 Authentication Architecture & Credentials
- **Isolation:** Completely separate from bystander views; requires hospital selection and password validation.
- **Demo Password Pattern:** `[first-word-in-lowercase]123` (e.g., `Apex Metro Super-Speciality Hospital` $\to$ `apex123`, `St. Jude...` $\to$ `st123`, `National Neuro...` $\to$ `national123`).
- **Error Handling:** Clean inline validation error on incorrect credentials.
- **Session:** State persisted with immediate logout affordance.

### 7.2 Hospital Staff Dashboard UI
- **Hospital Capability Header:** Displays facility name, distance, covered departments, active on-duty specialists, and available ER beds.
- **Incoming Case Card:**
  - Patient Name & GPS / street location.
  - Severity Tag (`Critical`, `Serious`, `Normal`) with soft tints.
  - Required Department & Straight-Line ETA.
  - Reported Symptoms text & Blood Pre-Reservation status badge.
  - **Live 30-Second Visual Countdown Bar:** Progress indicator counting down the response window.
  - **Action Buttons:**
    - `✓ Acknowledge`: Locks ER trauma bed, assigns standby doctor, immediately updates bystander screen to `CONFIRMED`.
    - `✕ Mark Busy / Redirect`: Diverts case, advancing closed-loop queue to next hospital.
- **Synthesized Two-Tone Emergency Sound System:**
  - Dual-tone synthesized audio alert (880 Hz $\to$ 660 Hz via Web Audio API, or Android ToneGenerator).
  - 100% self-contained without external MP3 files.
  - AudioContext auto-resumed on first user tap.


