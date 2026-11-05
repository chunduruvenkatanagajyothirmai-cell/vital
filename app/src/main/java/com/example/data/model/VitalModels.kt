package com.example.data.model

data class Hospital(
  val id: String,
  val name: String,
  val location: String,
  val distanceKm: Double,
  val phone: String,
  val departments: List<String>,
  val erBedsAvailable: Int = 3,
  val hasTraumaCenter: Boolean = true,
  val standbyDoctor: String = "Dr. Emergency On-Duty",
) {
  // Straight-distance ETA formula: (distance_km / 30) * 60 minutes
  val etaMinutes: Int
    get() = ((distanceKm / 30.0) * 60.0).toInt().coerceAtLeast(1)
}

data class Doctor(
  val id: String,
  val name: String,
  val department: String,
  val expertise: String,
  val timings: String,
  val hospital: String,
  val qualification: String = "MBBS, MD",
  val phone: String = "+91 80 4000 1100"
)

data class BloodBank(
  val id: String,
  val name: String,
  val location: String,
  val distanceKm: Double,
  val phone: String,
  val stock: Map<String, Int>, // e.g. "A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-"
  val operates24x7: Boolean = true
) {
  val totalUnits: Int
    get() = stock.values.sum()
}

enum class SeverityLevel(val label: String, val code: String) {
  CRITICAL("Critical", "CRITICAL"),
  SERIOUS("Serious", "SERIOUS"),
  NORMAL("Normal", "NORMAL")
}

enum class AttemptStatus {
  WAITING,
  ACCEPTED,
  REJECTED,
  TIMED_OUT
}

data class CaseAttempt(
  val hospitalId: String,
  val hospitalName: String,
  val distanceKm: Double,
  val etaMinutes: Int,
  val attemptNumber: Int,
  val status: AttemptStatus = AttemptStatus.WAITING,
  val windowSeconds: Int = 30,
  val note: String = "ER Notification Transmitted"
)

enum class CaseStatus {
  DISPATCHING,          // Active closed loop queue in progress
  CONFIRMED,            // A hospital confirmed acceptance
  EXHAUSTED_FALLBACK,   // All matched hospitals timed out/declined; Supreme Court rule activated
  COMPLETED,
  CANCELLED
}

data class CaseTimestamps(
  val createdAt: String,
  val responseExpiresAt: String,
  val confirmedAt: String? = null
)

enum class EventLevel {
  INFO,      // Blue
  PENDING,   // Amber
  SUCCESS,   // Green
  ALERT,     // Red
  REDIRECT   // Blue
}

data class TimelineEvent(
  val id: String,
  val timestamp: String,
  val message: String,
  val level: EventLevel = EventLevel.INFO
)

data class EmergencyCase(
  val id: String,
  val patientName: String,
  val location: String,
  val department: String,
  val severity: SeverityLevel,
  val description: String,
  val conditionIcon: String = "🚨",
  val conditionLabel: String = "Emergency",
  val requestedBloodGroup: String? = null,
  val requestedUnits: Int = 1,
  val bloodReservation: String? = null,
  val attemptQueue: List<CaseAttempt> = emptyList(),
  val currentAttemptIndex: Int = 0,
  val status: CaseStatus = CaseStatus.DISPATCHING,
  val timestamps: CaseTimestamps,
  val remainingSeconds: Int = 30,
  val confirmedHospital: Hospital? = null,
  val firstAidNotes: List<String> = emptyList(),
  val timelineEvents: List<TimelineEvent> = emptyList()
)
