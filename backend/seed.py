#!/usr/bin/env python3
"""
Vital - Phase 1 Starter Seed Data Generator Script
Generates both `seed.json` and a populated SQLite database `vital.db`.
Includes realistic fallback handling for missing or optional fields.
"""

import json
import sqlite3
import os
import sys

SEED_FILE = "seed.json"
DB_FILE = "vital.db"

DEFAULT_DEPARTMENTS = ["General Medicine"]
DEFAULT_TIMINGS = "09:00 AM - 05:00 PM (Mon-Fri)"
DEFAULT_EXPERTISE = "General Medical Consultation & Emergency Triage"
DEFAULT_PHONE = "+91 80 0000 0000"
DEFAULT_LOCATION = "Central Bengaluru, Karnataka"

SAMPLE_DATA = {
    "hospitals": [
        {
            "id": "hosp-001",
            "name": "Apex Metro Super-Speciality Hospital",
            "location": "84 Inner Ring Road, Koramangala, Bengaluru",
            "distance_km": 2.4,
            "phone": "+91 80 2553 9000",
            "departments": ["Trauma", "Cardiology", "Neurology", "Orthopedics", "Pulmonology", "General Medicine"],
            "er_beds_available": 5,
            "has_trauma_center": True
        },
        {
            "id": "hosp-002",
            "name": "St. Jude Memorial Institute of Cardiac Sciences",
            "location": "12 Hosur Main Rd, BTM 1st Stage, Bengaluru",
            "distance_km": 3.8,
            "phone": "+91 80 4112 4400",
            "departments": ["Cardiology", "Pulmonology", "General Medicine"],
            "er_beds_available": 4,
            "has_trauma_center": False
        },
        {
            "id": "hosp-003",
            "name": "National Neuro & Trauma Center",
            "location": "45 Indira Gandhi Rd, Jayanagar 4th Block, Bengaluru",
            "distance_km": 4.6,
            "phone": "+91 80 2665 1200",
            "departments": ["Trauma", "Neurology", "Orthopedics", "ENT", "General Medicine"],
            "er_beds_available": 3,
            "has_trauma_center": True
        },
        {
            "id": "hosp-004",
            "name": "Lotus Children & Women's Hospital",
            "location": "22 Sarjapur Main Rd, HSR Layout Sector 2, Bengaluru",
            "distance_km": 5.1,
            "phone": "+91 80 3090 8800",
            "departments": ["Pediatrics", "ENT", "General Medicine"],
            "er_beds_available": 6,
            "has_trauma_center": False
        },
        {
            "id": "hosp-005",
            "name": "Manipal City General & Pulmonary Hospital",
            "location": "98 Airport Rd, Domlur, Bengaluru",
            "distance_km": 6.4,
            "phone": "+91 80 2502 4444",
            "departments": ["Pulmonology", "ENT", "Orthopedics", "General Medicine"],
            "er_beds_available": 4,
            "has_trauma_center": True
        },
        {
            "id": "hosp-006",
            "name": "Fortis HealthCity Center",
            "location": "154 Bannerghatta Rd, Bilekahalli, Bengaluru",
            "distance_km": 7.2,
            "phone": "+91 80 6621 4444",
            "departments": ["Trauma", "Cardiology", "Neurology", "Orthopedics", "Pediatrics", "ENT", "Pulmonology", "General Medicine"],
            "er_beds_available": 8,
            "has_trauma_center": True
        }
    ],
    "doctors": [
        {
            "id": "doc-001",
            "name": "Dr. Rajesh Iyer",
            "department": "Trauma",
            "expertise": "Polytrauma, Acute Resuscitation, Shock Management",
            "timings": "08:00 AM - 04:00 PM (Mon-Sat)",
            "hospital": "Apex Metro Super-Speciality Hospital",
            "phone": "+91 80 2553 9001"
        },
        {
            "id": "doc-002",
            "name": "Dr. Ananya Roy",
            "department": "Cardiology",
            "expertise": "Acute Coronary Syndromes, Primary Angioplasty",
            "timings": "09:00 AM - 05:00 PM (Mon-Fri)",
            "hospital": "St. Jude Memorial Institute of Cardiac Sciences",
            "phone": "+91 80 4112 4405"
        },
        {
            "id": "doc-003",
            "name": "Dr. Vikram Sethi",
            "department": "Neurology",
            "expertise": "Acute Ischemic Stroke, Thrombolysis, Head Trauma",
            "timings": "10:00 AM - 06:00 PM (Tue-Sun)",
            "hospital": "National Neuro & Trauma Center",
            "phone": "+91 80 2665 1205"
        },
        {
            "id": "doc-004",
            "name": "Dr. Arvind Menon",
            "department": "Pulmonology",
            "expertise": "Severe Asthma, ARDS, Bronchoscopy, Critical Care",
            "timings": "09:30 AM - 04:30 PM (Mon-Sat)",
            "hospital": "Manipal City General & Pulmonary Hospital",
            "phone": "+91 80 2502 4448"
        },
        {
            "id": "doc-005",
            "name": "Dr. Preeti Sen",
            "department": "Pediatrics",
            "expertise": "Pediatric Resuscitation, Neonatal ICU",
            "timings": "08:30 AM - 03:30 PM (Mon-Sat)",
            "hospital": "Lotus Children & Women's Hospital",
            "phone": "+91 80 3090 8802"
        },
        {
            "id": "doc-006",
            "name": "Dr. Shalini Kulkarni",
            "department": "ENT",
            "expertise": "Tracheostomy, Epistaxis, Airway Blockage",
            "timings": "11:00 AM - 07:00 PM (Mon-Fri)",
            "hospital": "National Neuro & Trauma Center",
            "phone": "+91 80 2665 1208"
        },
        {
            "id": "doc-007",
            "name": "Dr. Karthik Rao",
            "department": "Orthopedics",
            "expertise": "Complex Fractures, Joint Dislocation, Spine",
            "timings": "09:00 AM - 05:00 PM (Mon-Sat)",
            "hospital": "Apex Metro Super-Speciality Hospital",
            "phone": "+91 80 2553 9005"
        },
        {
            "id": "doc-008",
            "name": "Dr. Meera Nambiar",
            "department": "General Medicine",
            "expertise": "Sepsis Management, Diabetic Emergencies",
            "timings": "08:00 AM - 08:00 PM (Daily)",
            "hospital": "Fortis HealthCity Center",
            "phone": "+91 80 6621 4445"
        }
    ],
    "blood_banks": [
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
        },
        {
            "id": "bb-002",
            "name": "Apex Metro Hospital Rotary Blood Center",
            "location": "84 Inner Ring Road, Koramangala, Bengaluru",
            "distance_km": 2.4,
            "phone": "+91 80 2553 9044",
            "stock": {
                "A+": 15, "A-": 4, "B+": 22, "B-": 3,
                "O+": 28, "O-": 9, "AB+": 11, "AB-": 2
            }
        },
        {
            "id": "bb-003",
            "name": "Lions LifeLine Blood Bank",
            "location": "14 Brigade Rd, Ashok Nagar, Bengaluru",
            "distance_km": 4.1,
            "phone": "+91 80 2558 1111",
            "stock": {
                "A+": 19, "A-": 5, "B+": 17, "B-": 4,
                "O+": 35, "O-": 7, "AB+": 14, "AB-": 3
            }
        },
        {
            "id": "bb-004",
            "name": "South City Voluntary Blood Center",
            "location": "77 9th Main, Jayanagar 3rd Block, Bengaluru",
            "distance_km": 5.0,
            "phone": "+91 80 2656 7788",
            "stock": {
                "A+": 8,  "A-": 2, "B+": 12, "B-": 1,
                "O+": 14, "O-": 3, "AB+": 6,  "AB-": 1
            }
        }
    ]
}


def sanitize_hospital(h):
    """Sanitize hospital records with sensible fallbacks."""
    return {
        "id": h.get("id", f"hosp-{hash(h.get('name', '')) % 1000:03d}"),
        "name": h.get("name", "Unknown Hospital Facility"),
        "location": h.get("location") or h.get("address") or DEFAULT_LOCATION,
        "distance_km": float(h.get("distance_km", 5.0)),
        "phone": h.get("phone", DEFAULT_PHONE),
        "departments": h.get("departments") if isinstance(h.get("departments"), list) and h.get("departments") else DEFAULT_DEPARTMENTS,
        "er_beds_available": int(h.get("er_beds_available", 2)),
        "has_trauma_center": bool(h.get("has_trauma_center", False))
    }


def sanitize_doctor(d):
    """Sanitize doctor records with optional field fallbacks."""
    return {
        "id": d.get("id", f"doc-{hash(d.get('name', '')) % 1000:03d}"),
        "name": d.get("name", "Duty Medical Officer"),
        "department": d.get("department", "General Medicine"),
        "expertise": d.get("expertise") or DEFAULT_EXPERTISE,
        "timings": d.get("timings") or DEFAULT_TIMINGS,
        "hospital": d.get("hospital", "Affiliated Hospital"),
        "phone": d.get("phone", DEFAULT_PHONE)
    }


def sanitize_blood_bank(bb):
    """Sanitize blood bank records and ensure all 8 blood groups exist."""
    standard_groups = ["A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-"]
    raw_stock = bb.get("stock", {})
    normalized_stock = {grp: int(raw_stock.get(grp, 0)) for grp in standard_groups}

    return {
        "id": bb.get("id", f"bb-{hash(bb.get('name', '')) % 1000:03d}"),
        "name": bb.get("name", "City Blood Storage Center"),
        "location": bb.get("location") or bb.get("address") or DEFAULT_LOCATION,
        "distance_km": float(bb.get("distance_km", 4.0)),
        "phone": bb.get("phone", DEFAULT_PHONE),
        "stock": normalized_stock
    }


def init_sqlite_db(data):
    """Initialize SQLite database with normalized schema."""
    conn = sqlite3.connect(DB_FILE)
    c = conn.cursor()

    c.execute("DROP TABLE IF EXISTS cases")
    c.execute("DROP TABLE IF EXISTS blood_banks")
    c.execute("DROP TABLE IF EXISTS doctors")
    c.execute("DROP TABLE IF EXISTS hospitals")

    # Hospitals table
    c.execute("""
        CREATE TABLE hospitals (
            id TEXT PRIMARY KEY,
            name TEXT NOT NULL,
            location TEXT NOT NULL,
            distance_km REAL NOT NULL,
            phone TEXT NOT NULL,
            departments TEXT NOT NULL,
            er_beds_available INTEGER NOT NULL,
            has_trauma_center INTEGER NOT NULL
        )
    """)

    # Doctors table
    c.execute("""
        CREATE TABLE doctors (
            id TEXT PRIMARY KEY,
            name TEXT NOT NULL,
            department TEXT NOT NULL,
            expertise TEXT,
            timings TEXT,
            hospital TEXT NOT NULL,
            phone TEXT
        )
    """)

    # Blood banks table
    c.execute("""
        CREATE TABLE blood_banks (
            id TEXT PRIMARY KEY,
            name TEXT NOT NULL,
            location TEXT NOT NULL,
            distance_km REAL NOT NULL,
            phone TEXT NOT NULL,
            stock TEXT NOT NULL
        )
    """)

    # Emergency cases table
    c.execute("""
        CREATE TABLE cases (
            id TEXT PRIMARY KEY,
            patient_name TEXT NOT NULL,
            location TEXT NOT NULL,
            department TEXT NOT NULL,
            severity TEXT NOT NULL,
            description TEXT NOT NULL,
            blood_reservation TEXT,
            attempt_queue TEXT NOT NULL,
            current_attempt_index INTEGER DEFAULT 0,
            status TEXT NOT NULL,
            timestamps TEXT NOT NULL,
            confirmed_hospital_id TEXT
        )
    """)

    # Insert sanitized hospitals
    for h in data["hospitals"]:
        c.execute("""
            INSERT INTO hospitals (id, name, location, distance_km, phone, departments, er_beds_available, has_trauma_center)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """, (h["id"], h["name"], h["location"], h["distance_km"], h["phone"], json.dumps(h["departments"]), h["er_beds_available"], int(h["has_trauma_center"])))

    # Insert sanitized doctors
    for d in data["doctors"]:
        c.execute("""
            INSERT INTO doctors (id, name, department, expertise, timings, hospital, phone)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """, (d["id"], d["name"], d["department"], d["expertise"], d["timings"], d["hospital"], d["phone"]))

    # Insert sanitized blood banks
    for bb in data["blood_banks"]:
        c.execute("""
            INSERT INTO blood_banks (id, name, location, distance_km, phone, stock)
            VALUES (?, ?, ?, ?, ?, ?)
        """, (bb["id"], bb["name"], bb["location"], bb["distance_km"], bb["phone"], json.dumps(bb["stock"])))

    conn.commit()
    conn.close()
    print(f"[OK] Seeded {len(data['hospitals'])} hospitals, {len(data['doctors'])} doctors, {len(data['blood_banks'])} blood banks into {DB_FILE}")


def main():
    sanitized_hospitals = [sanitize_hospital(h) for h in SAMPLE_DATA["hospitals"]]
    sanitized_doctors = [sanitize_doctor(d) for d in SAMPLE_DATA["doctors"]]
    sanitized_blood_banks = [sanitize_blood_bank(bb) for bb in SAMPLE_DATA["blood_banks"]]

    data = {
        "hospitals": sanitized_hospitals,
        "doctors": sanitized_doctors,
        "blood_banks": sanitized_blood_banks
    }

    # Write JSON seed
    with open(SEED_FILE, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)
    print(f"[OK] Wrote validated seed data to {SEED_FILE}")

    # Populate SQLite database
    init_sqlite_db(data)


if __name__ == "__main__":
    main()
