"""
Vital - Dispatch Logic Engine
Level-2 Rule-based NLP Keyword-to-Department and Severity Classifier,
straight-distance ETA calculator, and closed-loop queue builder.
"""

import math
from typing import List, Dict, Any, Tuple

DEPARTMENTS = [
    "Trauma",
    "Cardiology",
    "Neurology",
    "Pulmonology",
    "Pediatrics",
    "ENT",
    "Orthopedics",
    "General Medicine"
]

DEPARTMENT_RULES = {
    "Trauma": [
        "accident", "crash", "bleeding", "blood loss", "fall", "injury", "fracture open",
        "wound", "stab", "burn", "trauma", "unconscious", "head injury", "crush", "collision",
        "hemorrhage", "laceration deep", "hit by", "run over"
    ],
    "Cardiology": [
        "chest pain", "heart", "cardiac", "angina", "attack", "pulse", "palpitations",
        "left arm", "chest tightness", "cardiovascular", "arrhythmia", "heavy chest",
        "heart rate", "sweating chest"
    ],
    "Neurology": [
        "stroke", "paralysis", "seizure", "convulsion", "speech loss", "facial droop",
        "numbness", "coma", "blackout", "slurred", "fainting", "aneurysm", "headache severe",
        "sudden confusion", "vision loss", "paralyzed"
    ],
    "Pulmonology": [
        "breathing", "choking", "suffocating", "asthma", "gasping", "shortness of breath",
        "cyanosis", "lungs", "respiratory", "inability to breathe", "blue lips", "wheezing"
    ],
    "Pediatrics": [
        "infant", "baby", "child", "toddler", "pediatric", "newborn", "kid", "little boy",
        "little girl", "neonate"
    ],
    "ENT": [
        "throat", "ear", "nosebleed", "swallowed coin", "swallowed object", "vocal", "ear bleed",
        "foreign object", "epistaxis", "airway blockage", "stridor", "ear discharge"
    ],
    "Orthopedics": [
        "bone", "joint", "dislocation", "sprain", "knee", "spine", "hip", "ankle",
        "broken leg", "broken arm", "shoulder", "ligament", "fracture"
    ]
}

CRITICAL_KEYWORDS = [
    "unconscious", "not breathing", "cardiac", "heart attack", "heavy bleeding",
    "profuse", "stroke", "coma", "choking", "gasping", "severe burn", "cyanosis",
    "blue lips", "convulsing", "massive", "arterial bleed"
]

SERIOUS_KEYWORDS = [
    "fracture", "deep wound", "severe pain", "bleeding", "seizure", "chest pain",
    "dislocation", "high fever", "breathing difficulty", "paralysis", "swallowed",
    "head injury", "vomiting blood", "injury"
]

SUPREME_COURT_MANDATE_TITLE = "Supreme Court Emergency Fallback Mandate"
SUPREME_COURT_MANDATE_MESSAGE = (
    "Under Supreme Court mandate (Pt. Parmanand Katara v. Union of India, 1989), "
    "every medical facility is legally bound to provide immediate, unconditional emergency "
    "stabilization to critical patients regardless of department status or administrative admission. "
    "Routing to the nearest emergency facility instantly."
)


def classify_department(description: str) -> str:
    """
    Rule-based NLP classifier mapping clinical symptoms to hospital departments.
    Defaults to 'General Medicine' if no specific department rules match.
    """
    text = description.lower()

    # Prioritize Pediatrics if child/infant keywords are detected
    if any(kw in text for kw in DEPARTMENT_RULES["Pediatrics"]):
        return "Pediatrics"

    best_dept = "General Medicine"
    max_matches = 0

    for dept, keywords in DEPARTMENT_RULES.items():
        if dept == "Pediatrics":
            continue
        matches = sum(1 for kw in keywords if kw in text)
        if matches > max_matches:
            max_matches = matches
            best_dept = dept

    return best_dept


def classify_severity(description: str, department: str) -> str:
    """
    Severity Rule Classifier: Returns 'Critical', 'Serious', or 'Normal'.
    """
    text = description.lower()

    if any(kw in text for kw in CRITICAL_KEYWORDS):
        return "Critical"

    if department in ["Trauma", "Cardiology", "Neurology"] and any(
        kw in text for kw in ["severe", "acute", "sudden", "blood"]
    ):
        return "Critical"

    if any(kw in text for kw in SERIOUS_KEYWORDS):
        return "Serious"

    if department != "General Medicine":
        return "Serious"

    return "Normal"


def calculate_eta_minutes(distance_km: float) -> int:
    """
    Straight-distance ETA formula: (distance_km / 30) * 60 minutes.
    Assumes an average 30 km/h urban emergency response transit speed.
    """
    eta = (float(distance_km) / 30.0) * 60.0
    return max(1, int(round(eta)))


def build_attempt_queue(department: str, hospitals: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    """
    Builds the closed-loop sequential attempt queue:
    1. Filters hospitals with matching department capability.
    2. Sorts candidates by distance ascending.
    3. Returns queue with 30-second window metadata.
    """
    matching = [
        h for h in hospitals
        if department in h.get("departments", [])
    ]
    candidates = matching if matching else hospitals
    candidates.sort(key=lambda h: float(h.get("distance_km", 999.0)))

    queue = []
    for idx, h in enumerate(candidates):
        dist = float(h.get("distance_km", 5.0))
        queue.append({
            "hospital_id": h["id"],
            "hospital_name": h["name"],
            "distance_km": dist,
            "eta_minutes": calculate_eta_minutes(dist),
            "attempt_number": idx + 1,
            "status": "WAITING" if idx == 0 else "STANDBY",
            "window_seconds": 30,
            "phone": h.get("phone", "")
        })
    return queue


def get_nearest_hospital(hospitals: List[Dict[str, Any]]) -> Dict[str, Any]:
    """Returns the absolute nearest hospital regardless of department (Supreme Court Fallback)."""
    return min(hospitals, key=lambda h: float(h.get("distance_km", 999.0)))
