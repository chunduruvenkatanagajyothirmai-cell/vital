package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CaseStatus
import com.example.data.model.Hospital
import com.example.data.model.SeverityLevel
import com.example.ui.components.StatusBadge
import com.example.ui.components.TimestampText
import com.example.ui.components.VitalCard
import com.example.ui.theme.Typography
import com.example.ui.theme.VitalAmber
import com.example.ui.theme.VitalAmberTint
import com.example.ui.theme.VitalBg
import com.example.ui.theme.VitalBlue
import com.example.ui.theme.VitalBorder
import com.example.ui.theme.VitalCard
import com.example.ui.theme.VitalGreen
import com.example.ui.theme.VitalGreenTint
import com.example.ui.theme.VitalPanel
import com.example.ui.theme.VitalRed
import com.example.ui.theme.VitalRedTint
import com.example.ui.theme.VitalText
import com.example.ui.theme.VitalTextLight
import com.example.ui.theme.VitalTextMuted
import com.example.ui.util.VitalSoundAlert
import com.example.ui.viewmodel.VitalViewModel

@Composable
fun VitalErDeskScreen(
  viewModel: VitalViewModel,
  modifier: Modifier = Modifier
) {
  val authHospital by viewModel.authenticatedHospital.collectAsState()
  val activeCase by viewModel.activeCase.collectAsState()
  val hospitals by viewModel.hospitals.collectAsState()
  val isSoundEnabled by viewModel.isSoundAlertEnabled.collectAsState()
  val context = LocalContext.current

  // Trigger sound automatically when case is actively dispatching to this hospital
  LaunchedEffect(activeCase?.id, activeCase?.currentAttemptIndex) {
    val currentAttempt = activeCase?.attemptQueue?.getOrNull(activeCase?.currentAttemptIndex ?: 0)
    if (activeCase?.status == CaseStatus.DISPATCHING && isSoundEnabled) {
      if (authHospital == null || currentAttempt?.hospitalId == authHospital?.id) {
        VitalSoundAlert.playTwoToneEmergencyAlert()
      }
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(VitalBg)
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    if (authHospital == null) {
      // PHASE 3: Hospital Staff Login Modal / Screen
      HospitalStaffLoginCard(
        hospitals = hospitals,
        onLogin = { hospitalId, password ->
          val (success, message) = viewModel.loginStaff(hospitalId, password)
          if (success) {
            Toast.makeText(context, "Welcome to Hospital ER Portal", Toast.LENGTH_SHORT).show()
          }
          success to message
        }
      )
    } else {
      // PHASE 3: Hospital Staff Dashboard UI (Desktop / Tablet view)
      HospitalStaffDashboard(
        hospital = authHospital!!,
        viewModel = viewModel,
        onLogout = {
          viewModel.logoutStaff()
          Toast.makeText(context, "Logged out of ER Terminal", Toast.LENGTH_SHORT).show()
        }
      )
    }
  }
}

/**
 * Phase 3: Hospital Staff Login Card
 */
@Composable
fun HospitalStaffLoginCard(
  hospitals: List<Hospital>,
  onLogin: (String, String) -> Pair<Boolean, String>
) {
  var selectedHospital by remember { mutableStateOf(hospitals.firstOrNull()) }
  var password by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var isDropdownExpanded by remember { mutableStateOf(false) }

  // Set default selection when hospitals list populates
  LaunchedEffect(hospitals) {
    if (selectedHospital == null && hospitals.isNotEmpty()) {
      selectedHospital = hospitals.first()
    }
  }

  VitalCard {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Box(
        modifier = Modifier
          .size(44.dp)
          .clip(CircleShape)
          .background(VitalRed),
        contentAlignment = Alignment.Center
      ) {
        Icon(Icons.Default.Lock, contentDescription = null, tint = VitalCard, modifier = Modifier.size(24.dp))
      }
      Column {
        Text(
          text = "Hospital Staff Portal",
          style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
        Text(
          text = "Secure emergency terminal for on-duty ER triage nurses & consultants",
          style = Typography.bodySmall.copy(color = VitalTextMuted)
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))
    HorizontalDivider(color = VitalBorder)
    Spacer(modifier = Modifier.height(14.dp))

    // Hospital Dropdown Selector
    Text(
      text = "SELECT REGISTERED HOSPITAL FACILITY",
      style = Typography.labelSmall.copy(color = VitalTextMuted, fontWeight = FontWeight.Bold)
    )
    Spacer(modifier = Modifier.height(6.dp))

    Box(modifier = Modifier.fillMaxWidth()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .background(VitalPanel)
          .border(1.dp, VitalBorder, RoundedCornerShape(8.dp))
          .clickable { isDropdownExpanded = true }
          .padding(horizontal = 12.dp, vertical = 12.dp)
          .testTag("hospital_dropdown_trigger")
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = selectedHospital?.name ?: "Select Hospital...",
              style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
              text = selectedHospital?.location ?: "",
              style = Typography.bodySmall.copy(color = VitalTextMuted, fontSize = 11.sp)
            )
          }
          Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = VitalText)
        }
      }

      DropdownMenu(
        expanded = isDropdownExpanded,
        onDismissRequest = { isDropdownExpanded = false },
        modifier = Modifier
          .background(VitalCard)
          .border(1.dp, VitalBorder)
      ) {
        hospitals.forEach { hosp ->
          DropdownMenuItem(
            text = {
              Column {
                Text(text = hosp.name, style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                Text(text = "${hosp.distanceKm} km away • ${hosp.departments.take(3).joinToString()}", style = Typography.bodySmall.copy(fontSize = 11.sp, color = VitalTextMuted))
              }
            },
            onClick = {
              selectedHospital = hosp
              isDropdownExpanded = false
              errorMessage = null
            }
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Password Field
    Text(
      text = "STAFF ACCESS PASSWORD",
      style = Typography.labelSmall.copy(color = VitalTextMuted, fontWeight = FontWeight.Bold)
    )
    Spacer(modifier = Modifier.height(6.dp))

    OutlinedTextField(
      value = password,
      onValueChange = {
        password = it
        errorMessage = null
      },
      placeholder = { Text("Enter staff access password") },
      visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
      trailingIcon = {
        IconButton(onClick = { passwordVisible = !passwordVisible }) {
          Text(text = if (passwordVisible) "Hide" else "Show", style = Typography.labelSmall.copy(color = VitalTextMuted))
        }
      },
      modifier = Modifier
        .fillMaxWidth()
        .testTag("staff_password_input"),
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = VitalRed,
        unfocusedBorderColor = VitalBorder,
        focusedContainerColor = VitalCard,
        unfocusedContainerColor = VitalCard
      ),
      singleLine = true
    )

    // Inline Error Message
    if (errorMessage != null) {
      Spacer(modifier = Modifier.height(8.dp))
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(6.dp))
          .background(VitalRedTint)
          .border(1.dp, VitalRed.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
          .padding(8.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Icon(Icons.Default.Warning, contentDescription = null, tint = VitalRed, modifier = Modifier.size(16.dp))
          Text(
            text = errorMessage!!,
            style = Typography.bodySmall.copy(color = VitalRed, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
          )
        }
      }
    }

    // Demo password helper pill
    Spacer(modifier = Modifier.height(10.dp))
    selectedHospital?.let { hosp ->
      val firstWord = hosp.name.trim().split(" ", "-", ".").firstOrNull { it.isNotBlank() }?.filter { it.isLetterOrDigit() }?.lowercase() ?: "apex"
      val demoPass = "${firstWord}123"

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(4.dp))
          .background(VitalPanel)
          .clickable { password = demoPass }
          .padding(horizontal = 8.dp, vertical = 4.dp)
      ) {
        Text(
          text = "Standard Shift Passcode: $demoPass",
          style = Typography.labelSmall.copy(color = VitalTextMuted, fontSize = 10.5.sp)
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Login Action Button
    Button(
      onClick = {
        if (selectedHospital == null) {
          errorMessage = "Please choose a hospital."
          return@Button
        }
        val (success, msg) = onLogin(selectedHospital!!.id, password)
        if (!success) {
          errorMessage = msg
        }
      },
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
        .testTag("staff_login_button"),
      colors = ButtonDefaults.buttonColors(containerColor = VitalRed, contentColor = VitalCard),
      shape = RoundedCornerShape(8.dp)
    ) {
      Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text("SIGN IN TO HOSPITAL TERMINAL", style = Typography.labelLarge.copy(fontWeight = FontWeight.Bold))
    }
  }
}

/**
 * Phase 3: Hospital Staff Dashboard UI (Desktop / Tablet view)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HospitalStaffDashboard(
  hospital: Hospital,
  viewModel: VitalViewModel,
  onLogout: () -> Unit
) {
  val activeCase by viewModel.activeCase.collectAsState()
  val isSoundEnabled by viewModel.isSoundAlertEnabled.collectAsState()
  val caseHistory by viewModel.caseHistory.collectAsState()

  // 1. Header showing logged-in hospital name, distance, covered departments, active doctor count, ER beds
  VitalCard(backgroundColor = VitalPanel) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.Top
    ) {
      Row(
        modifier = Modifier.weight(1f),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
      ) {
        Box(
          modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(VitalRed),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.LocalHospital, contentDescription = null, tint = VitalCard, modifier = Modifier.size(24.dp))
        }

        Column {
          Text(text = hospital.name, style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold))
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = VitalTextMuted, modifier = Modifier.size(12.dp))
            Text(text = "${hospital.location} (${hospital.distanceKm} km from center)", style = Typography.bodySmall.copy(color = VitalTextMuted, fontSize = 11.sp))
          }
        }
      }

      OutlinedButton(
        onClick = onLogout,
        border = BorderStroke(1.dp, VitalBorder),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.testTag("staff_logout_button")
      ) {
        Text("Logout", style = Typography.labelSmall.copy(color = VitalTextMuted))
      }
    }

    Spacer(modifier = Modifier.height(10.dp))
    HorizontalDivider(color = VitalBorder.copy(alpha = 0.5f))
    Spacer(modifier = Modifier.height(10.dp))

    // Statistics Bar: Covered Departments, Active Doctors, Free Beds, Audio Alert Toggle
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Box(
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(6.dp))
          .background(VitalCard)
          .border(1.dp, VitalBorder, RoundedCornerShape(6.dp))
          .padding(8.dp)
      ) {
        Column {
          Text(text = "AVAILABLE ER BEDS", style = Typography.labelSmall.copy(fontSize = 9.5.sp))
          Text(text = "${hospital.erBedsAvailable} Beds Ready", style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = VitalGreen))
        }
      }

      Box(
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(6.dp))
          .background(VitalCard)
          .border(1.dp, VitalBorder, RoundedCornerShape(6.dp))
          .padding(8.dp)
      ) {
        Column {
          Text(text = "ON-DUTY STAFF", style = Typography.labelSmall.copy(fontSize = 9.5.sp))
          Text(text = "3 Specialists", style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = VitalText))
        }
      }

      Box(
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(6.dp))
          .background(VitalCard)
          .border(1.dp, VitalBorder, RoundedCornerShape(6.dp))
          .clickable { viewModel.toggleSoundAlert(!isSoundEnabled) }
          .padding(8.dp)
      ) {
        Column {
          Text(text = "AUDIO ALERT", style = Typography.labelSmall.copy(fontSize = 9.5.sp))
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(
              imageVector = if (isSoundEnabled) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
              contentDescription = null,
              tint = if (isSoundEnabled) VitalGreen else VitalTextMuted,
              modifier = Modifier.size(16.dp)
            )
            Text(
              text = if (isSoundEnabled) "ON" else "MUTED",
              style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = if (isSoundEnabled) VitalGreen else VitalTextMuted)
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = "COVERED DEPARTMENTS:",
      style = Typography.labelSmall.copy(color = VitalTextMuted, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
    )
    Spacer(modifier = Modifier.height(4.dp))
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      hospital.departments.forEach { dept ->
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(VitalCard)
            .border(1.dp, VitalBorder, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
          Text(text = dept, style = Typography.labelSmall.copy(fontSize = 10.sp, color = VitalText))
        }
      }
    }
  }

  // 2. Incoming Case Display Card
  val currentAttempt = activeCase?.attemptQueue?.getOrNull(activeCase?.currentAttemptIndex ?: 0)
  val isCaseTargetingThisHospital = currentAttempt?.hospitalId == hospital.id

  if (activeCase != null && activeCase?.status == CaseStatus.DISPATCHING && isCaseTargetingThisHospital) {
    val c = activeCase!!

    VitalCard(borderColor = VitalRed) {
      // Flashing alert header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = VitalRed, modifier = Modifier.size(24.dp))
          Column {
            Text(
              text = "INCOMING EMERGENCY ALERT",
              style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = VitalRed)
            )
            Text(
              text = "Case Reference: ${c.id}",
              style = Typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            )
          }
        }

        StatusBadge(text = "LIVE QUEUE", color = VitalRed, isMonospace = true)
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Live 30-Second Visual Countdown Bar
      val progress = (c.remainingSeconds.toFloat() / 30f).coerceIn(0f, 1f)
      val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "erBarProgress"
      )
      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "30-SECOND CONFIRMATION WINDOW:",
            style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = VitalAmber, fontSize = 10.sp)
          )
          Text(
            text = "${c.remainingSeconds}s Remaining",
            style = Typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = if (c.remainingSeconds <= 5) VitalRed else VitalAmber)
          )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
          progress = { animatedProgress },
          modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp)),
          color = if (c.remainingSeconds <= 5) VitalRed else VitalAmber,
          trackColor = VitalPanel
        )
      }

      Spacer(modifier = Modifier.height(12.dp))
      HorizontalDivider(color = VitalBorder)
      Spacer(modifier = Modifier.height(12.dp))

      // Patient Name & Location
      Text(text = "Patient Identification:", style = Typography.labelSmall.copy(color = VitalTextMuted))
      Text(text = c.patientName, style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold))

      Spacer(modifier = Modifier.height(4.dp))
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(Icons.Default.LocationOn, contentDescription = null, tint = VitalTextMuted, modifier = Modifier.size(14.dp))
        Text(text = c.location, style = Typography.bodySmall.copy(color = VitalTextMuted))
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Severity Tag & Department
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatusBadge(
          text = "Severity: ${c.severity.label}",
          color = if (c.severity == SeverityLevel.CRITICAL) VitalRed else VitalAmber,
          icon = Icons.Default.Warning
        )
        StatusBadge(text = "Dept: ${c.department}", color = VitalText)
        StatusBadge(text = "ETA: ${currentAttempt.etaMinutes} mins", color = VitalGreen)
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Reported Issue & Blood Reservation status
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(6.dp))
          .background(VitalPanel)
          .padding(10.dp)
      ) {
        Column {
          Text(text = "REPORTED SYMPTOMS & TRIAGE SUMMARY:", style = Typography.labelSmall.copy(fontSize = 10.sp, color = VitalTextMuted))
          Spacer(modifier = Modifier.height(2.dp))
          Text(text = c.description, style = Typography.bodySmall.copy(lineHeight = 16.sp))
        }
      }

      if (c.bloodReservation != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(VitalRedTint)
            .border(1.dp, VitalRed.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(8.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Default.Bloodtype, contentDescription = null, tint = VitalRed, modifier = Modifier.size(16.dp))
            Text(text = c.bloodReservation, style = Typography.bodySmall.copy(color = VitalRed, fontWeight = FontWeight.Bold, fontSize = 11.sp))
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Phase 3 Action Buttons: "✓ Acknowledge" and "✕ Mark Busy / Redirect"
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Button(
          onClick = {
            VitalSoundAlert.playAcknowledgeChime()
            viewModel.confirmHospital("Confirmed by ${hospital.name} ER Trauma Staff")
          },
          modifier = Modifier
            .weight(1.2f)
            .height(48.dp)
            .testTag("staff_acknowledge_button"),
          colors = ButtonDefaults.buttonColors(containerColor = VitalGreen, contentColor = VitalCard),
          shape = RoundedCornerShape(6.dp)
        ) {
          Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("✓ Acknowledge & Prepare Bed", style = Typography.labelLarge.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold))
        }

        OutlinedButton(
          onClick = {
            viewModel.timeoutOrRejectHospital("${hospital.name} ER at capacity — redirected")
          },
          modifier = Modifier
            .weight(0.9f)
            .height(48.dp)
            .testTag("staff_redirect_button"),
          border = BorderStroke(1.dp, VitalAmber),
          shape = RoundedCornerShape(6.dp)
        ) {
          Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp), tint = VitalAmber)
          Spacer(modifier = Modifier.width(4.dp))
          Text("✕ Mark Busy", style = Typography.labelLarge.copy(fontSize = 11.5.sp, color = VitalAmber))
        }
      }
    }
  } else if (activeCase != null && activeCase?.status == CaseStatus.CONFIRMED && activeCase?.confirmedHospital?.id == hospital.id) {
    // Confirmed Case in Transit
    val c = activeCase!!
    VitalCard(borderColor = VitalGreen) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = VitalGreen)
        Text(
          text = "Case ${c.id} Acknowledged & In Transit",
          style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = VitalGreen)
        )
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "Trauma Bay 1 cleared. Standby physician ${hospital.standbyDoctor} notified. Estimated patient arrival in ${hospital.etaMinutes} minutes.",
        style = Typography.bodySmall.copy(color = VitalText)
      )
      Spacer(modifier = Modifier.height(10.dp))
      TimestampText(label = "Confirmation Logged", timestamp = c.timestamps.confirmedAt ?: "")
    }
  } else {
    // Idle ER State
    VitalCard {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(Icons.Default.LocalHospital, contentDescription = null, tint = VitalTextLight, modifier = Modifier.size(44.dp))
        Text(
          text = "${hospital.name} ER Standing By",
          style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Text(
          text = "No emergency cases currently queued for this trauma center.",
          style = Typography.bodySmall.copy(color = VitalTextMuted)
        )

        Spacer(modifier = Modifier.height(10.dp))
        Button(
          onClick = {
            viewModel.dispatchEmergency(
              patientName = "Suresh Patil (Male, 42)",
              location = "Inner Ring Road, Koramangala 100ft Rd",
              description = "High-speed vehicular crash, multi-organ blunt trauma, heavy bleeding from head.",
              conditionIcon = "🩸",
              conditionLabel = "Trauma",
              bloodGroup = "O+",
              units = 2
            )
          },
          colors = ButtonDefaults.buttonColors(containerColor = VitalRed, contentColor = VitalCard),
          shape = RoundedCornerShape(6.dp),
          modifier = Modifier.testTag("trigger_er_drill_button")
        ) {
          Icon(Icons.Default.Emergency, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Transmit Emergency Alert Test")
        }
      }
    }
  }

  // Hospital Case History Log
  VitalCard {
    Text(
      text = "HOSPITAL EMERGENCY LOG HISTORY",
      style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = VitalTextMuted)
    )
    Spacer(modifier = Modifier.height(8.dp))

    if (caseHistory.isEmpty()) {
      Text(text = "No prior cases logged this shift.", style = Typography.bodySmall.copy(color = VitalTextLight))
    } else {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        caseHistory.take(5).forEach { h ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(text = "${h.id} • ${h.patientName}", style = Typography.bodySmall.copy(fontWeight = FontWeight.Bold))
              Text(text = "${h.department} • ${h.timestamps.createdAt}", style = Typography.labelSmall.copy(color = VitalTextLight, fontSize = 10.sp))
            }
            StatusBadge(text = h.status.name, color = if (h.status == CaseStatus.CONFIRMED) VitalGreen else VitalTextMuted, isMonospace = true)
          }
          HorizontalDivider(color = VitalBorder.copy(alpha = 0.5f))
        }
      }
    }
  }
}
