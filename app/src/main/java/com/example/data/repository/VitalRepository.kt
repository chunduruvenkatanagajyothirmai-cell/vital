package com.example.data.repository

import com.example.data.model.AttemptStatus
import com.example.data.model.BloodBank
import com.example.data.model.CaseAttempt
import com.example.data.model.CaseStatus
import com.example.data.model.CaseTimestamps
import com.example.data.model.Doctor
import com.example.data.model.EmergencyCase
import com.example.data.model.EventLevel
import com.example.data.model.Hospital
import com.example.data.model.SeverityLevel
import com.example.data.model.TimelineEvent
import com.example.domain.engine.VitalDispatchEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class VitalRepository(
  private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {

  private val _hospitals = MutableStateFlow<List<Hospital>>(emptyList())
  val hospitals: StateFlow<List<Hospital>> = _hospitals.asStateFlow()

  private val _doctors = MutableStateFlow<List<Doctor>>(emptyList())
  val doctors: StateFlow<List<Doctor>> = _doctors.asStateFlow()

  private val _bloodBanks = MutableStateFlow<List<BloodBank>>(emptyList())
  val bloodBanks: StateFlow<List<BloodBank>> = _bloodBanks.asStateFlow()

  private val _activeCase = MutableStateFlow<EmergencyCase?>(null)
  val activeCase: StateFlow<EmergencyCase?> = _activeCase.asStateFlow()

  private val _caseHistory = MutableStateFlow<List<EmergencyCase>>(emptyList())
  val caseHistory: StateFlow<List<EmergencyCase>> = _caseHistory.asStateFlow()

  private var countdownJob: Job? = null

  init {
    loadSeedData()
  }

  fun resetDemo() {
    countdownJob?.cancel()
    _activeCase.value = null
    _caseHistory.value = emptyList()
    loadSeedData()
  }

  private fun loadSeedData() {
    val hospitalList = listOf(
      Hospital(
        id = "hosp-001",
        name = "Apex Metro Super-Speciality Hospital",
        location = "84 Inner Ring Road, Koramangala, Bengaluru",
        distanceKm = 2.4,
        phone = "+91 80 2553 9000",
        departments = listOf("Trauma", "Cardiology", "Neurology", "Orthopedics", "Pulmonology", "General Medicine"),
        erBedsAvailable = 5,
        hasTraumaCenter = true,
        standbyDoctor = "Dr. Rajesh Iyer (Chief ER Consultant)"
      ),
      Hospital(
        id = "hosp-002",
        name = "St. Jude Memorial Institute of Cardiac Sciences",
        location = "12 Hosur Main Rd, BTM 1st Stage, Bengaluru",
        distanceKm = 3.8,
        phone = "+91 80 4112 4400",
        departments = listOf("Cardiology", "Pulmonology", "General Medicine"),
        erBedsAvailable = 4,
        hasTraumaCenter = false,
        standbyDoctor = "Dr. Ananya Roy (Cardiothoracic ER)"
      ),
      Hospital(
        id = "hosp-003",
        name = "National Neuro & Trauma Center",
        location = "45 Indira Gandhi Rd, Jayanagar 4th Block, Bengaluru",
        distanceKm = 4.6,
        phone = "+91 80 2665 1200",
        departments = listOf("Trauma", "Neurology", "Orthopedics", "ENT", "General Medicine"),
        erBedsAvailable = 3,
        hasTraumaCenter = true,
        standbyDoctor = "Dr. Vikram Sethi (Trauma Neurosurgeon)"
      ),
      Hospital(
        id = "hosp-004",
        name = "Lotus Children & Women's Hospital",
        location = "22 Sarjapur Main Rd, HSR Layout Sector 2, Bengaluru",
        distanceKm = 5.1,
        phone = "+91 80 3090 8800",
        departments = listOf("Pediatrics", "ENT", "General Medicine"),
        erBedsAvailable = 6,
        hasTraumaCenter = false,
        standbyDoctor = "Dr. Preeti Sen (Pediatric ER Lead)"
      ),
      Hospital(
        id = "hosp-005",
        name = "Manipal City General & Pulmonary Hospital",
        location = "98 Airport Rd, Domlur, Bengaluru",
        distanceKm = 6.4,
        phone = "+91 80 2502 4444",
        departments = listOf("Pulmonology", "ENT", "Orthopedics", "General Medicine"),
        erBedsAvailable = 4,
        hasTraumaCenter = true,
        standbyDoctor = "Dr. Arvind Menon (Pulmonary Critical Care)"
      ),
      Hospital(
        id = "hosp-006",
        name = "Fortis HealthCity Center",
        location = "154 Bannerghatta Rd, Bilekahalli, Bengaluru",
        distanceKm = 7.2,
        phone = "+91 80 6621 4444",
        departments = listOf("Trauma", "Cardiology", "Neurology", "Orthopedics", "Pediatrics", "ENT", "Pulmonology", "General Medicine"),
        erBedsAvailable = 8,
        hasTraumaCenter = true,
        standbyDoctor = "Dr. Meera Nambiar (Emergency Medicine)"
      )
    )
    _hospitals.value = hospitalList

    val doctorList = listOf(
      Doctor(
        id = "doc-001",
        name = "Dr. Rajesh Iyer",
        department = "Trauma",
        expertise = "Polytrauma, Acute Resuscitation, Shock Management",
        timings = "08:00 AM - 04:00 PM (Mon-Sat)",
        hospital = "Apex Metro Super-Speciality Hospital",
        qualification = "MBBS, MS (General Surgery), Trauma Fellowship",
        phone = "+91 80 2553 9001"
      ),
      Doctor(
        id = "doc-002",
        name = "Dr. Ananya Roy",
        department = "Cardiology",
        expertise = "Acute Coronary Syndromes, Primary Angioplasty, Arrhythmias",
        timings = "09:00 AM - 05:00 PM (Mon-Fri)",
        hospital = "St. Jude Memorial Institute of Cardiac Sciences",
        qualification = "MBBS, MD, DM (Cardiology)",
        phone = "+91 80 4112 4405"
      ),
      Doctor(
        id = "doc-003",
        name = "Dr. Vikram Sethi",
        department = "Neurology",
        expertise = "Acute Ischemic Stroke, Thrombolysis, Head Trauma",
        timings = "10:00 AM - 06:00 PM (Tue-Sun)",
        hospital = "National Neuro & Trauma Center",
        qualification = "MBBS, MCh (Neurosurgery)",
        phone = "+91 80 2665 1205"
      ),
      Doctor(
        id = "doc-004",
        name = "Dr. Arvind Menon",
        department = "Pulmonology",
        expertise = "Severe Asthma, ARDS, Bronchoscopy, Critical Care",
        timings = "09:30 AM - 04:30 PM (Mon-Sat)",
        hospital = "Manipal City General & Pulmonary Hospital",
        qualification = "MBBS, MD (Pulmonary Medicine)",
        phone = "+91 80 2502 4448"
      ),
      Doctor(
        id = "doc-005",
        name = "Dr. Preeti Sen",
        department = "Pediatrics",
        expertise = "Pediatric Resuscitation, Neonatal ICU, Foreign Body Aspiration",
        timings = "08:30 AM - 03:30 PM (Mon-Sat)",
        hospital = "Lotus Children & Women's Hospital",
        qualification = "MBBS, MD (Pediatrics), DNB",
        phone = "+91 80 3090 8802"
      ),
      Doctor(
        id = "doc-006",
        name = "Dr. Shalini Kulkarni",
        department = "ENT",
        expertise = "Tracheostomy, Epistaxis Management, Foreign Body Extraction",
        timings = "11:00 AM - 07:00 PM (Mon-Fri)",
        hospital = "National Neuro & Trauma Center",
        qualification = "MBBS, MS (ENT)",
        phone = "+91 80 2665 1208"
      ),
      Doctor(
        id = "doc-007",
        name = "Dr. Karthik Rao",
        department = "Orthopedics",
        expertise = "Complex Fractures, Joint Dislocation, Spine Stabilization",
        timings = "09:00 AM - 05:00 PM (Mon-Sat)",
        hospital = "Apex Metro Super-Speciality Hospital",
        qualification = "MBBS, MS (Orthopedics), DNB",
        phone = "+91 80 2553 9005"
      ),
      Doctor(
        id = "doc-008",
        name = "Dr. Meera Nambiar",
        department = "General Medicine",
        expertise = "Sepsis Management, Diabetic Emergencies, Fever of Unknown Origin",
        timings = "08:00 AM - 08:00 PM (Daily)",
        hospital = "Fortis HealthCity Center",
        qualification = "MBBS, MD (Internal Medicine)",
        phone = "+91 80 6621 4445"
      )
    )
    _doctors.value = doctorList

    val bloodBankList = listOf(
      BloodBank(
        id = "bb-001",
        name = "Red Cross Central Blood Bank",
        location = "26 Mahatma Gandhi Rd, Central Bengaluru",
        distanceKm = 3.2,
        phone = "+91 80 2226 8450",
        stock = mapOf(
          "A+" to 24, "A-" to 8,
          "B+" to 31, "B-" to 6,
          "O+" to 42, "O-" to 14,
          "AB+" to 18, "AB-" to 4
        )
      ),
      BloodBank(
        id = "bb-002",
        name = "Apex Metro Hospital Rotary Blood Center",
        location = "84 Inner Ring Road, Koramangala, Bengaluru",
        distanceKm = 2.4,
        phone = "+91 80 2553 9044",
        stock = mapOf(
          "A+" to 15, "A-" to 4,
          "B+" to 22, "B-" to 3,
          "O+" to 28, "O-" to 9,
          "AB+" to 11, "AB-" to 2
        )
      ),
      BloodBank(
        id = "bb-003",
        name = "Lions LifeLine Blood Bank & Component Center",
        location = "14 Brigade Rd, Ashok Nagar, Bengaluru",
        distanceKm = 4.1,
        phone = "+91 80 2558 1111",
        stock = mapOf(
          "A+" to 19, "A-" to 5,
          "B+" to 17, "B-" to 4,
          "O+" to 35, "O-" to 7,
          "AB+" to 14, "AB-" to 3
        )
      ),
      BloodBank(
        id = "bb-004",
        name = "South City Voluntary Blood Center",
        location = "77 9th Main, Jayanagar 3rd Block, Bengaluru",
        distanceKm = 5.0,
        phone = "+91 80 2656 7788",
        stock = mapOf(
          "A+" to 8,  "A-" to 2,
          "B+" to 12, "B-" to 1,
          "O+" to 14, "O-" to 3,
          "AB+" to 6,  "AB-" to 1
        )
      )
    )
    _bloodBanks.value = bloodBankList
  }

  /**
   * Check and reserve blood units from nearest blood bank
   */
  fun checkAndReserveBlood(bloodGroup: String, units: Int = 1): Pair<Boolean, String> {
    val availableBank = _bloodBanks.value
      .sortedBy { it.distanceKm }
      .firstOrNull { (it.stock[bloodGroup] ?: 0) >= units }

    return if (availableBank != null) {
      val availableUnits = availableBank.stock[bloodGroup] ?: 0
      // Decrement stock
      _bloodBanks.update { banks ->
        banks.map { bank ->
          if (bank.id == availableBank.id) {
            val updated = bank.stock.toMutableMap()
            updated[bloodGroup] = (availableUnits - units).coerceAtLeast(0)
            bank.copy(stock = updated)
          } else bank
        }
      }
      true to "$bloodGroup ($units unit${if (units > 1) "s" else ""} reserved at ${availableBank.name}, ${availableBank.distanceKm} km)"
    } else {
      false to "$bloodGroup ($units unit${if (units > 1) "s" else ""} currently scarce in immediate proximity — alerting regional network)"
    }
  }

  /**
   * Dispatch Emergency Case with full timeline logging
   */
  fun dispatchEmergencyCase(
    patientName: String,
    location: String,
    description: String,
    conditionIcon: String = "🚨",
    conditionLabel: String = "Emergency",
    bloodGroupNeeded: String? = null,
    unitsNeeded: Int = 1
  ): EmergencyCase {
    countdownJob?.cancel()

    val dept = VitalDispatchEngine.classifyDepartment(description)
    val severity = VitalDispatchEngine.classifySeverity(description, dept)
    val queue = VitalDispatchEngine.buildAttemptQueue(dept, _hospitals.value)

    val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
    val now = Date()
    val createdAt = timeFormat.format(now)
    val expiresAt = timeFormat.format(Date(now.time + 30_000))

    val events = mutableListOf<TimelineEvent>()
    events.add(
      TimelineEvent(
        id = UUID.randomUUID().toString(),
        timestamp = createdAt,
        message = "Emergency alert initiated for ${patientName.ifBlank { "Unidentified Patient" }} at $location.",
        level = EventLevel.INFO
      )
    )
    events.add(
      TimelineEvent(
        id = UUID.randomUUID().toString(),
        timestamp = createdAt,
        message = "Condition classified as $dept (${severity.label} severity) based on triage symptoms.",
        level = if (severity == SeverityLevel.CRITICAL) EventLevel.ALERT else EventLevel.SUCCESS
      )
    )

    // Handle blood reservation
    var bloodReservationText: String? = null
    if (!bloodGroupNeeded.isNullOrBlank() && bloodGroupNeeded != "None") {
      val (success, reservationMsg) = checkAndReserveBlood(bloodGroupNeeded, unitsNeeded)
      bloodReservationText = reservationMsg
      events.add(
        TimelineEvent(
          id = UUID.randomUUID().toString(),
          timestamp = createdAt,
          message = "Blood Bank: $reservationMsg",
          level = if (success) EventLevel.SUCCESS else EventLevel.ALERT
        )
      )
    }

    val firstAttempt = queue.firstOrNull()
    if (firstAttempt != null) {
      events.add(
        TimelineEvent(
          id = UUID.randomUUID().toString(),
          timestamp = createdAt,
          message = "Querying Attempt #1: ${firstAttempt.hospitalName} (${firstAttempt.distanceKm} km, ETA: ${firstAttempt.etaMinutes} mins). 30s confirmation window active.",
          level = EventLevel.PENDING
        )
      )
    }

    val instructions = VitalDispatchEngine.getFirstAidInstructions(dept, severity)

    val newCase = EmergencyCase(
      id = "CASE-" + UUID.randomUUID().toString().substring(0, 8).uppercase(Locale.ROOT),
      patientName = patientName.ifBlank { "Unidentified Patient" },
      location = location.ifBlank { "GPS: 12.9352° N, 77.6245° E (Bengaluru)" },
      department = dept,
      severity = severity,
      description = description,
      conditionIcon = conditionIcon,
      conditionLabel = conditionLabel,
      requestedBloodGroup = bloodGroupNeeded,
      requestedUnits = unitsNeeded,
      bloodReservation = bloodReservationText,
      attemptQueue = queue,
      currentAttemptIndex = 0,
      status = CaseStatus.DISPATCHING,
      timestamps = CaseTimestamps(createdAt, expiresAt),
      remainingSeconds = 30,
      firstAidNotes = instructions,
      timelineEvents = events
    )

    _activeCase.value = newCase
    startCountdownLoop()
    return newCase
  }

  /**
   * Hospital Confirms Acceptance
   */
  fun confirmCurrentHospital(note: String = "Confirmed by ER Trauma Team") {
    val current = _activeCase.value ?: return
    if (current.status != CaseStatus.DISPATCHING) return

    val currentAttempt = current.attemptQueue.getOrNull(current.currentAttemptIndex) ?: return
    val hospital = _hospitals.value.find { it.id == currentAttempt.hospitalId }

    countdownJob?.cancel()

    val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
    val confirmedTime = timeFormat.format(Date())

    val updatedQueue = current.attemptQueue.mapIndexed { index, attempt ->
      if (index == current.currentAttemptIndex) {
        attempt.copy(status = AttemptStatus.ACCEPTED, note = note)
      } else {
        attempt
      }
    }

    val newEvent = TimelineEvent(
      id = UUID.randomUUID().toString(),
      timestamp = confirmedTime,
      message = "${hospital?.name ?: currentAttempt.hospitalName} ER Desk confirmed acceptance! Standby bed assigned. Trauma team standing by.",
      level = EventLevel.SUCCESS
    )

    val updatedCase = current.copy(
      status = CaseStatus.CONFIRMED,
      attemptQueue = updatedQueue,
      confirmedHospital = hospital,
      timestamps = current.timestamps.copy(confirmedAt = confirmedTime),
      timelineEvents = current.timelineEvents + newEvent
    )

    _activeCase.value = updatedCase
    _caseHistory.update { listOf(updatedCase) + it.take(19) }
  }

  /**
   * Hospital Rejects / Diverts / Times Out
   */
  fun rejectOrTimeoutCurrentHospital(reason: String = "Response Window Expired (30s)") {
    val current = _activeCase.value ?: return
    if (current.status != CaseStatus.DISPATCHING) return

    val nextIndex = current.currentAttemptIndex + 1
    val currentAttempt = current.attemptQueue.getOrNull(current.currentAttemptIndex)

    val updatedQueue = current.attemptQueue.mapIndexed { index, attempt ->
      if (index == current.currentAttemptIndex) {
        attempt.copy(
          status = if (reason.contains("Expired") || reason.contains("timed out")) AttemptStatus.TIMED_OUT else AttemptStatus.REJECTED,
          note = reason
        )
      } else {
        attempt
      }
    }

    val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
    val nowTime = timeFormat.format(Date())

    val timeoutEvent = TimelineEvent(
      id = UUID.randomUUID().toString(),
      timestamp = nowTime,
      message = "Attempt #${current.currentAttemptIndex + 1} (${currentAttempt?.hospitalName}): $reason.",
      level = EventLevel.ALERT
    )

    if (nextIndex < current.attemptQueue.size) {
      val nextHosp = current.attemptQueue[nextIndex]
      val newExpiresAt = timeFormat.format(Date(System.currentTimeMillis() + 30_000))

      val nextEvent = TimelineEvent(
        id = UUID.randomUUID().toString(),
        timestamp = nowTime,
        message = "Advancing to Attempt #${nextIndex + 1}: ${nextHosp.hospitalName} (${nextHosp.distanceKm} km, ETA: ${nextHosp.etaMinutes} mins). 30s response window active.",
        level = EventLevel.PENDING
      )

      _activeCase.value = current.copy(
        currentAttemptIndex = nextIndex,
        attemptQueue = updatedQueue,
        remainingSeconds = 30,
        timestamps = current.timestamps.copy(responseExpiresAt = newExpiresAt),
        timelineEvents = current.timelineEvents + timeoutEvent + nextEvent
      )
    } else {
      // Queue Exhausted! Activate Supreme Court Emergency Fallback Mandate
      countdownJob?.cancel()
      val nearest = VitalDispatchEngine.getNearestHospital(_hospitals.value)

      val fallbackEvent = TimelineEvent(
        id = UUID.randomUUID().toString(),
        timestamp = nowTime,
        message = "All department candidate queues exhausted. Activating Supreme Court Emergency Fallback Mandate (Pt. Parmanand Katara v. Union of India). Routing unconditionally to nearest facility: ${nearest.name} (${nearest.distanceKm} km, ETA: ${nearest.etaMinutes} mins).",
        level = EventLevel.REDIRECT
      )

      val exhaustedCase = current.copy(
        status = CaseStatus.EXHAUSTED_FALLBACK,
        attemptQueue = updatedQueue,
        currentAttemptIndex = current.attemptQueue.size - 1,
        remainingSeconds = 0,
        confirmedHospital = nearest,
        timestamps = current.timestamps.copy(confirmedAt = nowTime),
        timelineEvents = current.timelineEvents + timeoutEvent + fallbackEvent
      )

      _activeCase.value = exhaustedCase
      _caseHistory.update { listOf(exhaustedCase) + it.take(19) }
    }
  }

  private fun startCountdownLoop() {
    countdownJob?.cancel()
    countdownJob = scope.launch {
      while (isActive) {
        delay(1000)
        val current = _activeCase.value ?: break
        if (current.status != CaseStatus.DISPATCHING) break

        if (current.remainingSeconds > 1) {
          _activeCase.update { it?.copy(remainingSeconds = it.remainingSeconds - 1) }
        } else {
          // Timeout reached (30s)
          rejectOrTimeoutCurrentHospital("Hospital response window timed out (30s elapsed)")
        }
      }
    }
  }

  fun cancelActiveCase() {
    countdownJob?.cancel()
    _activeCase.update { it?.copy(status = CaseStatus.CANCELLED) }
  }

  fun clearActiveCase() {
    countdownJob?.cancel()
    _activeCase.value = null
  }
}
