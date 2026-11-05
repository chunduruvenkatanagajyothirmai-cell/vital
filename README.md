# Vital — Emergency-Response & Checkup-Guidance Platform
> **Tagline:** *"Not just an alert. A confirmed response."*  
> **A Comprehensive Emergency Dispatch, Hospital Closed-Loop Confirmation & Specialist Guidance System.**

---

## 1. Quickstart (2-Command Execution)

### Command 1: Start Backend (Python Flask + WebSockets + SQLite)
```bash
python3 seed.py && python3 backend/app.py
```
*Seeds database (`vital.db`), starts REST API and WebSocket broadcast server at `http://localhost:5000`.*

### Command 2: Start Mobile App or Web Frontend
- **Android App:** Already pre-compiled and running live in the Android Studio streaming emulator.  
  *(To test local unit tests: `gradle :app:testDebugUnitTest`)*
- **React Frontend:**
```bash
cd frontend && npm install && npm run dev
```
*Launches modern React + Tailwind UI at `http://localhost:5173`.*

---

## 2. Complete Pre-Review Checklist & Verification Steps

Follow this 5-minute evaluation script to test all 4 phases during college review:

| Step | Action | Expected Result | Edge-Case Verified |
|---|---|---|---|
| **1. Bystander Home** | View Home Screen | Header shows "Hospital Staff Login" & "Reset Demo". Two cards: 🚨 Emergency and 🩺 Normal Checkup. | Soft off-white theme (#F4F6F4), no dark mode or neon. |
| **2. Emergency Flow** | Tap "Start Emergency Dispatch" | 6-step form opens with universal pictograms (🩸, 🫁, 🆘, ❤️, ➕). | Mobile-first single-column design. |
| **3. GPS Handling** | Tap "📍 Use My Location" | If location denied, system gracefully switches to manual input without crashing or blocking dispatch. | **Edge Case 1:** GPS denial fallback. |
| **4. Condition & Blood** | Tap "🩸 Bleeding / Injury" | Step 5 (Blood Pre-Reservation) appears dynamically. Tap "Check & Reserve Blood". Stock decrements in real-time. | Conditional blood bank step & stock tracking. |
| **5. Closed-Loop Queue** | Tap "Send Emergency Alert" | Department classified (Trauma, Critical). 30-second radial countdown dial starts. Candidate Attempt #1 queried. | Real-time dispatch initialized. |
| **6. ER Divert / Timeout** | Tap "Redirect / Divert" | Case automatically advances to Attempt #2. Countdown resets to 30s. Case timeline logs event with colored dot. | **Edge Case 2:** Closed-loop queue auto-advancement. |
| **7. Queue Exhaustion** | Divert remaining candidates | All hospital queues exhaust. Activates **Supreme Court Fallback Mandate** (*Pt. Parmanand Katara v. Union of India, 1989*) routing to closest hospital. | **Edge Case 3:** Legal emergency fallback. |
| **8. Staff Portal Login** | Switch to ER Console tab | Select hospital from dropdown. Type wrong password $\to$ clean inline error appears. Type `<firstword>123` (e.g. `apex123`) $\to$ authenticates. | **Edge Case 4:** Staff authentication security. |
| **9. Staff Acknowledge** | Trigger test alert in ER Console | Two-tone synthesized emergency sound plays (no MP3 needed). Live 30s countdown bar moves. Click "✓ Acknowledge" $\to$ chime plays, bed Bay 1 cleared, bystander screen shows Confirmed. | Web Audio API / ToneGenerator & full socket sync. |
| **10. Normal Checkup** | Open "Specialists" tab | Type symptoms (e.g. "chest pressure and flutter"). Banner shows "Suggested Department: Cardiology". Doctor cards list name, hospital, proximity (km), and consultation timings. | **Edge Case 5:** Graceful fallbacks for missing doctor/timing fields. |

---

## 3. Robust Edge-Case Handling Architecture

1. **GPS Denial & Hardware Failures:**
   - The location selector never blocks dispatch. If GPS hardware is uncalibrated, denied, or timed out, the form gracefully enables manual landmark typing.
2. **Missing Doctor & Hospital Data:**
   - Field fallbacks ensure resilience: missing expertise defaults to `"General Physician"`, missing timings default to `"Contact hospital for details/timings"`, missing hospital names default to `"Network Medical Center"`.
3. **Hospital Non-Response & Capacity Diversion:**
   - Strict 30-second bounded window per hospital. If ER triage staff fails to respond or clicks "Mark Busy", the system automatically re-routes to Attempt #2 in the queue.
4. **Queue Exhaustion (Supreme Court Mandate Fallback):**
   - If every matching specialty hospital is occupied or times out, the system does not give up. It activates the legal mandate established in *Pt. Parmanand Katara v. Union of India (1989)*, which binds every medical facility to provide unconditional emergency stabilization to critical patients.
5. **Staff Authentication Validation:**
   - Standardized demo formula: `first word in lowercase + "123"` (e.g. `apex123`, `st123`, `national123`). Invalid attempts trigger a non-disruptive inline error alert.

---

## 4. Non-Obvious Design Decisions (Viva Defense Reference)

1. **Why Closed-Loop Confirmation Instead of Broadcast Alerts?**
   - Traditional alert apps simply blast SMS or push notifications to nearby hospitals without confirmation. An unconfirmed alert causes bystanders to transport critical patients to hospitals that are overcapacity or lack on-duty specialists. Vital's closed-loop queue ensures **guaranteed bed, doctor, and blood readiness before arrival**.

2. **Why a Strict 30-Second Bounded Window?**
   - In trauma and acute cardiac events ("The Golden Hour"), every minute lost reduces survival by 7-10%. A 30-second window forces immediate triage action and prevents cases from idling indefinitely at a distracted hospital desk.

3. **Why Level-2 Rule-Based NLP Instead of Deep Learning (ML)?**
   - Deep learning LLMs are non-deterministic, can hallucinate, introduce latency (1-3 seconds), and fail when cloud networks fluctuate during an emergency. Deterministic Level-2 keyword rule trees execute in $<1$ millisecond, are 100% auditable, and guarantee predictable clinical mapping.

4. **Why Straight-Distance Formula $( \text{distance\_km} / 30 ) \times 60$?**
   - Emergency ambulances and siren-assisted transits in urban corridors travel at an average operational speed of 30 km/h. This yields a transparent calculation: $\text{distance} \times 2$ minutes, preventing reliance on external mapping APIs that fail in offline or degraded mobile conditions.

5. **Why Native Synthesized Web Audio API & ToneGenerator?**
   - External MP3/WAV files depend on network downloading, browser caching, and mime-type configurations that can fail silently. Synthesizing dual frequencies (880 Hz $\to$ 660 Hz) directly through the browser `AudioContext` and Android `ToneGenerator` ensures 100% offline reliability.

6. **Why Soft Off-White (#F4F6F4) Over Dark Mode / Neon?**
   - Dark mode and high-contrast neon palettes create visual fatigue and terminal-like confusion during stressful emergency situations. The soft off-white design (#F4F6F4 background, pure white cards, forest green, emergency red, and amber badges) matches clinical healthcare standards and ensures maximum daylight readability.
