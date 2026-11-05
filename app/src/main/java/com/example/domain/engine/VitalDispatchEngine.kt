package com.example.domain.engine

import com.example.data.model.CaseAttempt
import com.example.data.model.Hospital
import com.example.data.model.SeverityLevel
import java.util.Locale

object VitalDispatchEngine {

  val DEPARTMENTS = listOf(
    "Trauma",
    "Cardiology",
    "Neurology",
    "Pulmonology",
    "Pediatrics",
    "ENT",
    "Orthopedics",
    "General Medicine"
  )

  // Level-2 Rule-based Keyword Dictionary for Departments
  private val DEPARTMENT_RULES = mapOf(
    "Trauma" to listOf(
      "accident", "crash", "bleeding", "blood loss", "fall", "injury", "fracture open",
      "wound", "stab", "burn", "trauma", "unconscious", "head injury", "crush", "collision",
      "hemorrhage", "laceration deep", "hit by", "run over"
    ),
    "Cardiology" to listOf(
      "chest pain", "heart", "cardiac", "angina", "attack", "pulse", "palpitations",
      "left arm", "chest tightness", "cardiovascular", "arrhythmia", "heavy chest",
      "heart rate", "sweating chest"
    ),
    "Neurology" to listOf(
      "stroke", "paralysis", "seizure", "convulsion", "speech loss", "facial droop",
      "numbness", "coma", "blackout", "slurred", "fainting", "aneurysm", "headache severe",
      "sudden confusion", "vision loss", "paralyzed"
    ),
    "Pulmonology" to listOf(
      "breathing", "choking", "suffocating", "asthma", "gasping", "shortness of breath",
      "cyanosis", "lungs", "respiratory", "inability to breathe", "blue lips", "wheezing"
    ),
    "Pediatrics" to listOf(
      "infant", "baby", "child", "toddler", "pediatric", "newborn", "kid", "little boy",
      "little girl", "neonate"
    ),
    "ENT" to listOf(
      "throat", "ear", "nosebleed", "swallowed coin", "swallowed object", "vocal", "ear bleed",
      "foreign object", "epistaxis", "airway blockage", "stridor", "ear discharge"
    ),
    "Orthopedics" to listOf(
      "bone", "joint", "dislocation", "sprain", "knee", "spine", "hip", "ankle",
      "broken leg", "broken arm", "shoulder", "ligament", "fracture"
    )
  )

  // Level-2 Rule-based Keyword Dictionary for Severity
  private val CRITICAL_KEYWORDS = listOf(
    "unconscious", "not breathing", "cardiac", "heart attack", "heavy bleeding",
    "profuse", "stroke", "coma", "choking", "gasping", "severe burn", "cyanosis",
    "blue lips", "convulsing", "massive", "arterial bleed"
  )

  private val SERIOUS_KEYWORDS = listOf(
    "fracture", "deep wound", "severe pain", "bleeding", "seizure", "chest pain",
    "dislocation", "high fever", "breathing difficulty", "paralysis", "swallowed",
    "head injury", "vomiting blood", "injury"
  )

  // Supreme Court Rule Fallback Message (Pt. Parmanand Katara v. Union of India, 1989)
  const val SUPREME_COURT_MANDATE_TITLE = "Supreme Court Emergency Fallback Mandate"
  const val SUPREME_COURT_MANDATE_MESSAGE =
    "Under Supreme Court mandate (Pt. Parmanand Katara v. Union of India), every medical facility is legally bound to provide immediate, unconditional emergency stabilization to critical patients regardless of department status or administrative admission. Routing to the nearest hospital facility instantly."

  /**
   * Rule-based Keyword-to-Department Classifier
   * Defaults to "General Medicine" if no specific department keywords match.
   */
  fun classifyDepartment(description: String): String {
    val text = description.lowercase(Locale.ROOT)

    // Check Pediatrics first if child/infant keywords are paired
    if (DEPARTMENT_RULES["Pediatrics"]?.any { text.contains(it) } == true) {
      return "Pediatrics"
    }

    var bestDepartment = "General Medicine"
    var maxMatches = 0

    for ((dept, keywords) in DEPARTMENT_RULES) {
      if (dept == "Pediatrics") continue
      var matches = 0
      for (kw in keywords) {
        if (text.contains(kw)) {
          matches++
        }
      }
      if (matches > maxMatches) {
        maxMatches = matches
        bestDepartment = dept
      }
    }

    return bestDepartment
  }

  /**
   * Severity Rule Classifier: Critical / Serious / Normal
   */
  fun classifySeverity(description: String, department: String): SeverityLevel {
    val text = description.lowercase(Locale.ROOT)

    // Check critical triggers
    if (CRITICAL_KEYWORDS.any { text.contains(it) }) {
      return SeverityLevel.CRITICAL
    }
    // High-risk departments with any injury/pain
    if ((department == "Trauma" || department == "Cardiology" || department == "Neurology") &&
      (text.contains("severe") || text.contains("acute") || text.contains("sudden") || text.contains("blood"))
    ) {
      return SeverityLevel.CRITICAL
    }

    // Check serious triggers
    if (SERIOUS_KEYWORDS.any { text.contains(it) }) {
      return SeverityLevel.SERIOUS
    }

    if (department != "General Medicine") {
      return SeverityLevel.SERIOUS
    }

    return SeverityLevel.NORMAL
  }

  /**
   * Straight-distance ETA Calculator: (distance_km / 30) * 60
   * Assumes 30 km/h emergency response city travel rate.
   */
  fun calculateEtaMinutes(distanceKm: Double): Int {
    return ((distanceKm / 30.0) * 60.0).toInt().coerceAtLeast(1)
  }

  /**
   * Closed-Loop Queue Builder:
   * 1. Filters hospitals possessing the classified department.
   * 2. Sorts by distance ascending.
   * 3. Constructs CaseAttempt objects for sequential closed-loop confirmation.
   */
  fun buildAttemptQueue(
    department: String,
    allHospitals: List<Hospital>
  ): List<CaseAttempt> {
    val matching = allHospitals
      .filter { it.departments.contains(department) }
      .sortedBy { it.distanceKm }

    val candidates = if (matching.isNotEmpty()) {
      matching
    } else {
      // If no hospital explicitly lists the dept, fallback to trauma/general hospital sorted by distance
      allHospitals.sortedBy { it.distanceKm }
    }

    return candidates.mapIndexed { index, hospital ->
      CaseAttempt(
        hospitalId = hospital.id,
        hospitalName = hospital.name,
        distanceKm = hospital.distanceKm,
        etaMinutes = calculateEtaMinutes(hospital.distanceKm),
        attemptNumber = index + 1,
        windowSeconds = 30
      )
    }
  }

  /**
   * Fallback Nearest Hospital (used when closed loop attempts exhaust)
   */
  fun getNearestHospital(allHospitals: List<Hospital>): Hospital {
    return allHospitals.minByOrNull { it.distanceKm } ?: allHospitals.first()
  }

  /**
   * Immediate bystander instructions based on classified department
   */
  fun getFirstAidInstructions(department: String, severity: SeverityLevel): List<String> {
    return when (department) {
      "Cardiology" -> listOf(
        "Keep the patient sitting upright and calm; loosen tight clothing around the neck and chest.",
        "Do not allow patient to walk or exert physical effort.",
        "Ask if patient has prescribed Aspirin or Nitroglycerin; do not give food or drink.",
        "If patient becomes unresponsive and loses normal breathing, begin hands-only CPR immediately."
      )
      "Trauma" -> listOf(
        "Apply firm, continuous pressure directly over bleeding wounds with a clean cloth or gauze.",
        "Do not attempt to move patient if spine or neck injury is suspected.",
        "Keep the patient warm with a jacket or blanket to prevent hypothermic shock.",
        "Do not remove deeply embedded objects; stabilize around them."
      )
      "Neurology" -> listOf(
        "Note the exact time symptoms started (critical for thrombolytic treatment window).",
        "Lay patient on their side in the recovery position if vomiting or experiencing seizures.",
        "Never place anything inside the patient's mouth during a seizure.",
        "Keep airway clear and protect patient's head with something soft."
      )
      "Pulmonology" -> listOf(
        "Position patient upright leaning forward slightly to ease airflow.",
        "Ensure maximum fresh air ventilation; keep surrounding crowd at a distance.",
        "If asthma inhaler is available with the patient, assist them in administering it.",
        "Watch for blue discoloration around lips or fingernails."
      )
      "Pediatrics" -> listOf(
        "Keep child in their parent or caregiver's comforting posture.",
        "Do not give oral fluids if child is lethargic or breathing rapidly.",
        "For choking: perform infant back blows / child abdominal thrusts according to age.",
        "Keep airways unobstructed and monitor responsiveness continuously."
      )
      "Orthopedics" -> listOf(
        "Immobilize the injured limb in the position found; do not try to realign broken bones.",
        "Apply an ice pack wrapped in cloth to reduce swelling if skin is intact.",
        "Elevate injured extremity above heart level if comfortable and not visibly fractured.",
        "Support the joint with folded clothing or a makeshift splint."
      )
      else -> listOf(
        "Keep the patient comfortable, calm, and hydrated if conscious.",
        "Monitor breathing, pulse, and mental alertness.",
        "Keep bystander phone line open for hospital emergency desk coordination."
      )
    }
  }
}
