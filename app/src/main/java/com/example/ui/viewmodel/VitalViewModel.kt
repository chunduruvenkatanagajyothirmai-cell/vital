package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BloodBank
import com.example.data.model.Doctor
import com.example.data.model.EmergencyCase
import com.example.data.model.Hospital
import com.example.data.repository.VitalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Locale

class VitalViewModel(
  private val repository: VitalRepository = VitalRepository()
) : ViewModel() {

  val activeCase: StateFlow<EmergencyCase?> = repository.activeCase
  val caseHistory: StateFlow<List<EmergencyCase>> = repository.caseHistory
  val hospitals: StateFlow<List<Hospital>> = repository.hospitals
  val bloodBanks: StateFlow<List<BloodBank>> = repository.bloodBanks

  // Staff Authentication State
  private val _authenticatedHospital = MutableStateFlow<Hospital?>(null)
  val authenticatedHospital: StateFlow<Hospital?> = _authenticatedHospital.asStateFlow()

  private val _isSoundAlertEnabled = MutableStateFlow(true)
  val isSoundAlertEnabled: StateFlow<Boolean> = _isSoundAlertEnabled.asStateFlow()

  private val _selectedDepartment = MutableStateFlow<String?>(null)
  val selectedDepartment: StateFlow<String?> = _selectedDepartment.asStateFlow()

  private val _doctorSearchQuery = MutableStateFlow("")
  val doctorSearchQuery: StateFlow<String> = _doctorSearchQuery.asStateFlow()

  private val _selectedBloodGroup = MutableStateFlow<String?>("All")
  val selectedBloodGroup: StateFlow<String?> = _selectedBloodGroup.asStateFlow()

  val filteredDoctors: StateFlow<List<Doctor>> = combine(
    repository.doctors,
    _selectedDepartment,
    _doctorSearchQuery
  ) { docs, dept, query ->
    docs.filter { doc ->
      val matchesDept = dept == null || dept == "All" || doc.department.equals(dept, ignoreCase = true)
      val matchesQuery = query.isBlank() ||
        doc.name.contains(query, ignoreCase = true) ||
        doc.expertise.contains(query, ignoreCase = true) ||
        doc.hospital.contains(query, ignoreCase = true)
      matchesDept && matchesQuery
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  fun computeStaffPassword(hospitalName: String): String {
    val firstWord = hospitalName.trim().split(" ", "-", ".").firstOrNull { it.isNotBlank() } ?: "hospital"
    val clean = firstWord.filter { it.isLetterOrDigit() }.lowercase(Locale.ROOT)
    return "${clean}123"
  }

  fun loginStaff(hospitalId: String, passwordAttempt: String): Pair<Boolean, String> {
    val hosp = repository.hospitals.value.find { it.id == hospitalId }
      ?: return false to "Selected hospital facility not found."
    val expected = computeStaffPassword(hosp.name)
    return if (passwordAttempt.trim() == expected) {
      _authenticatedHospital.value = hosp
      true to "Authentication successful."
    } else {
      false to "Invalid password. Format: first word in lowercase + 123 (e.g. '$expected')."
    }
  }

  fun logoutStaff() {
    _authenticatedHospital.value = null
  }

  fun setAuthenticatedHospitalDirect(hospital: Hospital?) {
    _authenticatedHospital.value = hospital
  }

  fun toggleSoundAlert(enabled: Boolean) {
    _isSoundAlertEnabled.value = enabled
  }

  fun dispatchEmergency(
    patientName: String,
    location: String,
    description: String,
    conditionIcon: String = "🚨",
    conditionLabel: String = "Emergency",
    bloodGroup: String? = null,
    units: Int = 1
  ) {
    repository.dispatchEmergencyCase(
      patientName = patientName,
      location = location,
      description = description,
      conditionIcon = conditionIcon,
      conditionLabel = conditionLabel,
      bloodGroupNeeded = bloodGroup,
      unitsNeeded = units
    )
  }

  fun checkAndReserveBlood(bloodGroup: String, units: Int): Pair<Boolean, String> {
    return repository.checkAndReserveBlood(bloodGroup, units)
  }

  fun confirmHospital(note: String = "Confirmed by Receiving Hospital ER Desk") {
    repository.confirmCurrentHospital(note)
  }

  fun timeoutOrRejectHospital(reason: String = "Hospital Diverted / Window Expired") {
    repository.rejectOrTimeoutCurrentHospital(reason)
  }

  fun cancelCase() {
    repository.cancelActiveCase()
  }

  fun resetToNewCase() {
    repository.clearActiveCase()
  }

  fun resetDemo() {
    repository.resetDemo()
    _authenticatedHospital.value = null
  }

  fun setDepartmentFilter(dept: String?) {
    _selectedDepartment.value = if (_selectedDepartment.value == dept) null else dept
  }

  fun setDoctorQuery(query: String) {
    _doctorSearchQuery.value = query
  }

  fun setBloodGroupFilter(bg: String?) {
    _selectedBloodGroup.value = bg
  }
}
