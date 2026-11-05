package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.AttemptStatus
import com.example.data.model.CaseStatus
import com.example.data.model.EmergencyCase
import com.example.data.model.SeverityLevel
import com.example.domain.engine.VitalDispatchEngine
import com.example.ui.components.RadialCountdownTimer
import com.example.ui.components.StatusBadge
import com.example.ui.components.TimelineEventRow
import com.example.ui.components.TimestampText
import com.example.ui.components.VitalCard
import com.example.ui.components.VitalBrandHeroCard
import com.example.ui.theme.Typography
import com.example.ui.theme.VitalAmber
import com.example.ui.theme.VitalAmberTint
import com.example.ui.theme.VitalBg
import com.example.ui.theme.VitalBlue
import com.example.ui.theme.VitalBlueTint
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
import com.example.ui.viewmodel.VitalViewModel

@Composable
fun VitalEmergencyScreen(
  viewModel: VitalViewModel,
  onNavigateToCheckup: () -> Unit,
  modifier: Modifier = Modifier
) {
  val activeCase by viewModel.activeCase.collectAsState()
  val context = LocalContext.current
  var isEmergencyFormExpanded by remember { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(VitalBg)
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    if (activeCase == null || activeCase?.status == CaseStatus.CANCELLED || activeCase?.status == CaseStatus.COMPLETED) {
      if (!isEmergencyFormExpanded) {
        // Phase 2 Home Action Cards
        HomeScreenActionCards(
          onStartEmergency = { isEmergencyFormExpanded = true },
          onStartCheckup = onNavigateToCheckup
        )
      } else {
        // Phase 2 Public Bystander Emergency Flow (Mobile-First 6-Step)
        PublicBystanderEmergencyForm(
          onClose = { isEmergencyFormExpanded = false },
          onCheckBloodStock = { bg, units ->
            viewModel.checkAndReserveBlood(bg, units)
          },
          onDispatch = { name, loc, desc, icon, label, blood, units ->
            viewModel.dispatchEmergency(
              patientName = name,
              location = loc,
              description = desc,
              conditionIcon = icon,
              conditionLabel = label,
              bloodGroup = blood,
              units = units
            )
          }
        )
      }
    } else {
      // Phase 2 Live Case Status & Timeline Panel
      ActiveEmergencyDispatchView(
        emergencyCase = activeCase!!,
        onConfirmEr = { viewModel.confirmHospital() },
        onDivertEr = { viewModel.timeoutOrRejectHospital() },
        onCancel = { viewModel.cancelCase() },
        onReset = {
          viewModel.resetToNewCase()
          isEmergencyFormExpanded = false
        },
        onCallHospital = { phone -> dialPhoneNumber(context, phone) }
      )
    }
  }
}

/**
 * Phase 2 Home Screen Action Cards:
 * Card 1: Emergency (🚨)
 * Card 2: Normal Checkup (🩺)
 */
@Composable
fun HomeScreenActionCards(
  onStartEmergency: () -> Unit,
  onStartCheckup: () -> Unit
) {
  // Official Vital Branding Showcase
  VitalBrandHeroCard()

  // Emergency Action Card
  VitalCard(
    borderColor = VitalRed,
    backgroundColor = VitalCard
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Box(
        modifier = Modifier
          .size(54.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(VitalRed),
        contentAlignment = Alignment.Center
      ) {
        Text(text = "🚨", fontSize = 28.sp)
      }
      Column(modifier = Modifier.weight(1f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(
            text = "Emergency",
            style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = VitalRed)
          )
          StatusBadge(text = "CRITICAL", color = VitalRed)
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "Not just an alert. A confirmed response. Real-time 30s hospital closed-loop dispatch & bed standby.",
          style = Typography.bodySmall.copy(color = VitalTextMuted, lineHeight = 16.sp)
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))
    Button(
      onClick = onStartEmergency,
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
        .testTag("home_emergency_button"),
      colors = ButtonDefaults.buttonColors(containerColor = VitalRed, contentColor = VitalCard),
      shape = RoundedCornerShape(8.dp)
    ) {
      Text(text = "🚨", fontSize = 16.sp)
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = "START EMERGENCY DISPATCH",
        style = Typography.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
      )
    }
  }

  // Normal Checkup Action Card
  VitalCard(
    borderColor = VitalBorder,
    backgroundColor = VitalCard
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Box(
        modifier = Modifier
          .size(54.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(VitalPanel),
        contentAlignment = Alignment.Center
      ) {
        Text(text = "🩺", fontSize = 28.sp)
      }
      Column(modifier = Modifier.weight(1f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(
            text = "Normal Checkup",
            style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = VitalText)
          )
          StatusBadge(text = "NON-URGENT", color = VitalTextMuted)
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "Find specialist doctors, review consultation hours, and schedule clinic appointments.",
          style = Typography.bodySmall.copy(color = VitalTextMuted, lineHeight = 16.sp)
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))
    Button(
      onClick = onStartCheckup,
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
        .testTag("home_checkup_button"),
      colors = ButtonDefaults.buttonColors(containerColor = VitalPanel, contentColor = VitalText),
      shape = RoundedCornerShape(8.dp)
    ) {
      Text(text = "🩺", fontSize = 16.sp)
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = "FIND DOCTOR & APPOINTMENTS",
        style = Typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
      )
    }
  }
}

/**
 * Phase 2 Public Bystander Emergency Flow (Mobile-First 6-Step):
 * Step 1: Optional Patient Name
 * Step 2: Location Selector (📍 Use My Location + ✏️ Enter Manually)
 * Step 3: Quick-select condition grid with large tap targets and universal emojis
 * Step 4: Free-text description box
 * Step 5: Blood Bank Step (Conditionally visible ONLY when condition is Trauma/Bleeding)
 * Step 6: Send Emergency Alert button
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PublicBystanderEmergencyForm(
  onClose: () -> Unit,
  onCheckBloodStock: (String, Int) -> Pair<Boolean, String>,
  onDispatch: (name: String, loc: String, desc: String, icon: String, label: String, blood: String?, units: Int) -> Unit
) {
  val context = LocalContext.current

  // Form States
  var patientName by remember { mutableStateOf("") }
  var location by remember { mutableStateOf("Outer Ring Road, Near Koramangala 4th Block, Bengaluru") }
  var isManualLocation by remember { mutableStateOf(false) }
  var description by remember { mutableStateOf("") }

  // Quick condition selection
  data class ConditionOption(
    val id: String,
    val icon: String,
    val label: String,
    val deptHint: String,
    val defaultDesc: String,
    val isTrauma: Boolean = false
  )

  val conditions = listOf(
    ConditionOption("trauma", "🩸", "Bleeding / Injury", "Trauma", "Severe bleeding and open wound after collision, patient in severe pain.", isTrauma = true),
    ConditionOption("pulmonary", "🫁", "Can't Breathe Well", "Pulmonology", "Difficulty breathing, gasping for air, chest tightness, wheezing sound."),
    ConditionOption("neurology", "🆘", "Unconscious", "Neurology", "Patient suddenly collapsed, unresponsive to voice or touch, breathing shallow."),
    ConditionOption("cardiology", "❤️", "Chest Pain", "Cardiology", "Crushing chest pain radiating to left shoulder and arm, sweating heavily."),
    ConditionOption("general", "➕", "Something Else", "General Medicine", "Sudden acute illness, severe weakness, nausea, and disorientation.")
  )

  var selectedCondition by remember { mutableStateOf<ConditionOption?>(conditions[0]) }

  // Blood Step States (only for Trauma/Bleeding)
  var selectedBloodGroup by remember { mutableStateOf("O+") }
  var bloodUnitsNeeded by remember { mutableIntStateOf(1) }
  var bloodReservationResult by remember { mutableStateOf<String?>(null) }
  var isBloodReservedSuccess by remember { mutableStateOf(false) }

  // Initialize description if empty
  if (description.isEmpty() && selectedCondition != null) {
    description = selectedCondition!!.defaultDesc
  }

  // Location Permission Launcher
  val locationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
    val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    if (fineGranted || coarseGranted) {
      fetchCurrentGpsLocation(context) { locStr ->
        location = locStr
        Toast.makeText(context, "Location acquired from GPS", Toast.LENGTH_SHORT).show()
      }
    } else {
      // Auto fallback to manual entry. Never block dispatch on location.
      isManualLocation = true
      Toast.makeText(context, "GPS permission denied. Using manual location entry.", Toast.LENGTH_SHORT).show()
    }
  }

  VitalCard {
    // Flow Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Box(
          modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(VitalRed),
          contentAlignment = Alignment.Center
        ) {
          Text(text = "🚨", fontSize = 16.sp)
        }
        Column {
          Text(
            text = "Emergency Response Alert",
            style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold)
          )
          Text(
            text = "Step-by-step bystander triage & hospital queue dispatch",
            style = Typography.bodySmall.copy(color = VitalTextMuted, fontSize = 11.sp)
          )
        }
      }

      Box(
        modifier = Modifier
          .size(30.dp)
          .clip(CircleShape)
          .background(VitalPanel)
          .clickable { onClose() },
        contentAlignment = Alignment.Center
      ) {
        Icon(Icons.Default.Close, contentDescription = "Close", tint = VitalText, modifier = Modifier.size(16.dp))
      }
    }

    Spacer(modifier = Modifier.height(14.dp))
    HorizontalDivider(color = VitalBorder)
    Spacer(modifier = Modifier.height(14.dp))

    // STEP 1: Patient Name (Optional)
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Text(text = "STEP 1:", style = Typography.labelSmall.copy(color = VitalRed, fontWeight = FontWeight.Bold))
      Text(text = "Patient Identification (Optional)", style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold))
    }
    Spacer(modifier = Modifier.height(6.dp))
    OutlinedTextField(
      value = patientName,
      onValueChange = { patientName = it },
      placeholder = { Text("e.g. John Doe, or select below") },
      leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = VitalTextMuted) },
      modifier = Modifier
        .fillMaxWidth()
        .testTag("patient_name_input"),
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = VitalRed,
        unfocusedBorderColor = VitalBorder,
        focusedContainerColor = VitalCard,
        unfocusedContainerColor = VitalCard
      ),
      singleLine = true
    )

    Spacer(modifier = Modifier.height(6.dp))
    Row(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      listOf("👤 Me", "❓ Unknown Male", "❓ Unknown Female", "🧒 Child").forEach { preset ->
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(VitalPanel)
            .clickable { patientName = preset }
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(text = preset, style = Typography.labelSmall.copy(fontSize = 10.sp, color = VitalText))
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // STEP 2: Location Selector
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Text(text = "STEP 2:", style = Typography.labelSmall.copy(color = VitalRed, fontWeight = FontWeight.Bold))
      Text(text = "Incident Location", style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold))
    }
    Spacer(modifier = Modifier.height(6.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Button(
        onClick = {
          val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
          val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
          if (hasFine || hasCoarse) {
            fetchCurrentGpsLocation(context) { locStr ->
              location = locStr
              Toast.makeText(context, "Location updated from GPS", Toast.LENGTH_SHORT).show()
            }
          } else {
            locationPermissionLauncher.launch(
              arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
          }
        },
        modifier = Modifier
          .weight(1f)
          .height(42.dp)
          .testTag("use_my_location_button"),
        colors = ButtonDefaults.buttonColors(containerColor = VitalPanel, contentColor = VitalText),
        shape = RoundedCornerShape(6.dp)
      ) {
        Text(text = "📍", fontSize = 14.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = "Use My Location", style = Typography.labelLarge.copy(fontSize = 11.sp))
      }

      OutlinedButton(
        onClick = { isManualLocation = !isManualLocation },
        modifier = Modifier
          .weight(1f)
          .height(42.dp)
          .testTag("manual_location_button"),
        border = BorderStroke(1.dp, VitalBorder),
        shape = RoundedCornerShape(6.dp)
      ) {
        Text(text = "✏️", fontSize = 14.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = if (isManualLocation) "Hide Manual" else "Enter Manually", style = Typography.labelLarge.copy(fontSize = 11.sp, color = VitalText))
      }
    }

    Spacer(modifier = Modifier.height(6.dp))
    OutlinedTextField(
      value = location,
      onValueChange = { location = it },
      label = { Text("Incident Address / GPS Coordinates") },
      leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = VitalTextMuted) },
      modifier = Modifier
        .fillMaxWidth()
        .testTag("incident_location_input"),
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = VitalRed,
        unfocusedBorderColor = VitalBorder,
        focusedContainerColor = VitalCard,
        unfocusedContainerColor = VitalCard
      ),
      singleLine = true
    )

    Spacer(modifier = Modifier.height(16.dp))

    // STEP 3: Quick-Select Condition Grid
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Text(text = "STEP 3:", style = Typography.labelSmall.copy(color = VitalRed, fontWeight = FontWeight.Bold))
      Text(text = "Select Patient Condition (Universal Pictograms)", style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold))
    }
    Spacer(modifier = Modifier.height(8.dp))

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      conditions.forEach { cond ->
        val isSelected = selectedCondition?.id == cond.id
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) VitalRed.copy(alpha = 0.08f) else VitalPanel)
            .border(
              width = if (isSelected) 1.5.dp else 1.dp,
              color = if (isSelected) VitalRed else VitalBorder,
              shape = RoundedCornerShape(8.dp)
            )
            .clickable {
              selectedCondition = cond
              description = cond.defaultDesc
              if (!cond.isTrauma) {
                bloodReservationResult = null
              }
            }
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("condition_${cond.id}")
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Text(text = cond.icon, fontSize = 24.sp)
              Column {
                Text(
                  text = cond.label,
                  style = Typography.titleSmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) VitalRed else VitalText
                  )
                )
                Text(
                  text = "Routes to: ${cond.deptHint} Department",
                  style = Typography.bodySmall.copy(fontSize = 11.sp, color = VitalTextMuted)
                )
              }
            }

            if (isSelected) {
              Box(
                modifier = Modifier
                  .size(20.dp)
                  .clip(CircleShape)
                  .background(VitalRed),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = VitalCard, modifier = Modifier.size(14.dp))
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // STEP 4: Free-text Description Box
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Text(text = "STEP 4:", style = Typography.labelSmall.copy(color = VitalRed, fontWeight = FontWeight.Bold))
      Text(text = "Symptoms & Emergency Description", style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold))
    }
    Spacer(modifier = Modifier.height(6.dp))
    OutlinedTextField(
      value = description,
      onValueChange = { description = it },
      placeholder = { Text("Describe patient condition in detail for automated triage...") },
      modifier = Modifier
        .fillMaxWidth()
        .height(90.dp)
        .testTag("symptoms_description_input"),
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = VitalRed,
        unfocusedBorderColor = VitalBorder,
        focusedContainerColor = VitalCard,
        unfocusedContainerColor = VitalCard
      ),
      maxLines = 4
    )

    // STEP 5: Blood Bank Step (Conditionally visible ONLY when condition is Trauma/Bleeding)
    AnimatedVisibility(visible = selectedCondition?.isTrauma == true) {
      Column(modifier = Modifier.padding(top = 16.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(text = "STEP 5:", style = Typography.labelSmall.copy(color = VitalRed, fontWeight = FontWeight.Bold))
          Text(text = "Blood Bank Pre-Reservation (Trauma / Bleeding)", style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold))
        }
        Spacer(modifier = Modifier.height(6.dp))

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(VitalRedTint)
            .border(1.dp, VitalRed.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(12.dp)
        ) {
          Column {
            Text(
              text = "🩸 BLOOD GROUP REQUIRED",
              style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = VitalRed, fontSize = 10.sp)
            )
            Spacer(modifier = Modifier.height(6.dp))

            // Blood Group Chips
            val groups = listOf("O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-")
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              groups.forEach { bg ->
                val isSel = selectedBloodGroup == bg
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isSel) VitalRed else VitalCard)
                    .border(1.dp, if (isSel) VitalRed else VitalBorder, RoundedCornerShape(4.dp))
                    .clickable {
                      selectedBloodGroup = bg
                      bloodReservationResult = null
                    }
                    .padding(horizontal = 8.dp, vertical = 5.dp)
                    .testTag("blood_select_$bg")
                ) {
                  Text(
                    text = bg,
                    style = Typography.bodySmall.copy(
                      fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                      color = if (isSel) VitalCard else VitalText,
                      fontSize = 11.sp
                    )
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(text = "Units Needed:", style = Typography.bodySmall.copy(color = VitalText))
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(1, 2, 3).forEach { u ->
                  val isUnitSel = bloodUnitsNeeded == u
                  Box(
                    modifier = Modifier
                      .clip(RoundedCornerShape(4.dp))
                      .background(if (isUnitSel) VitalRed else VitalCard)
                      .border(1.dp, if (isUnitSel) VitalRed else VitalBorder, RoundedCornerShape(4.dp))
                    .clickable {
                      bloodUnitsNeeded = u
                      bloodReservationResult = null
                    }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                  ) {
                    Text(
                      text = "$u Unit${if (u > 1) "s" else ""}",
                      style = Typography.labelSmall.copy(
                        color = if (isUnitSel) VitalCard else VitalText,
                        fontWeight = if (isUnitSel) FontWeight.Bold else FontWeight.Normal
                      )
                    )
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Check & Reserve Blood Button
            Button(
              onClick = {
                val (success, msg) = onCheckBloodStock(selectedBloodGroup, bloodUnitsNeeded)
                isBloodReservedSuccess = success
                bloodReservationResult = msg
              },
              modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .testTag("check_reserve_blood_button"),
              colors = ButtonDefaults.buttonColors(containerColor = VitalRed, contentColor = VitalCard),
              shape = RoundedCornerShape(6.dp)
            ) {
              Icon(Icons.Default.Bloodtype, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Check & Reserve Blood", style = Typography.labelLarge.copy(fontSize = 12.sp))
            }

            if (bloodReservationResult != null) {
              Spacer(modifier = Modifier.height(8.dp))
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(4.dp))
                  .background(if (isBloodReservedSuccess) VitalGreenTint else VitalAmberTint)
                  .border(1.dp, if (isBloodReservedSuccess) VitalGreen else VitalAmber, RoundedCornerShape(4.dp))
                  .padding(8.dp)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Icon(
                    if (isBloodReservedSuccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isBloodReservedSuccess) VitalGreen else VitalAmber,
                    modifier = Modifier.size(16.dp)
                  )
                  Text(
                    text = bloodReservationResult!!,
                    style = Typography.bodySmall.copy(
                      color = if (isBloodReservedSuccess) VitalGreen else VitalAmber,
                      fontSize = 11.sp,
                      fontWeight = FontWeight.SemiBold
                    )
                  )
                }
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // STEP 6: Send Emergency Alert Button
    Button(
      onClick = {
        val bloodArg = if (selectedCondition?.isTrauma == true) selectedBloodGroup else null
        onDispatch(
          patientName.ifBlank { "Unidentified Patient" },
          location.ifBlank { "Outer Ring Road, Koramangala, Bengaluru" },
          description.ifBlank { selectedCondition?.defaultDesc ?: "Emergency reported by bystander" },
          selectedCondition?.icon ?: "🚨",
          selectedCondition?.label ?: "Emergency",
          bloodArg,
          bloodUnitsNeeded
        )
      },
      modifier = Modifier
        .fillMaxWidth()
        .height(52.dp)
        .testTag("send_emergency_alert_button"),
      colors = ButtonDefaults.buttonColors(containerColor = VitalRed, contentColor = VitalCard),
      shape = RoundedCornerShape(8.dp)
    ) {
      Text(text = "🚨", fontSize = 18.sp)
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = "SEND EMERGENCY ALERT",
        style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
      )
    }
  }
}

/**
 * Phase 2 Live Case Status & Live Case Timeline Panel
 */
@Composable
fun ActiveEmergencyDispatchView(
  emergencyCase: EmergencyCase,
  onConfirmEr: () -> Unit,
  onDivertEr: () -> Unit,
  onCancel: () -> Unit,
  onReset: () -> Unit,
  onCallHospital: (String) -> Unit
) {
  val currentAttempt = emergencyCase.attemptQueue.getOrNull(emergencyCase.currentAttemptIndex)

  // Status Indicator Bar: Waiting (Amber), Confirmed (Green), Fallback/Redirect (Blue)
  val (statusLabel, statusColor, statusIcon) = when (emergencyCase.status) {
    CaseStatus.DISPATCHING -> Triple("WAITING FOR HOSPITAL ER CONFIRMATION", VitalAmber, "⏳")
    CaseStatus.CONFIRMED -> Triple("CONFIRMED RESPONSE — ER STANDBY", VitalGreen, "✅")
    CaseStatus.EXHAUSTED_FALLBACK -> Triple("SUPREME COURT FALLBACK REDIRECT", VitalBlue, "⚖️")
    CaseStatus.COMPLETED -> Triple("EMERGENCY RESOLVED", VitalGreen, "🏁")
    CaseStatus.CANCELLED -> Triple("ALERT CANCELLED", VitalTextMuted, "❌")
  }

  // Active Status Card
  VitalCard(borderColor = statusColor) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text(text = statusIcon, fontSize = 20.sp)
        Column {
          Text(
            text = statusLabel,
            style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = statusColor)
          )
          Text(
            text = "Case ID: ${emergencyCase.id}",
            style = Typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp)
          )
        }
      }

      StatusBadge(
        text = emergencyCase.status.name,
        color = statusColor,
        isMonospace = true
      )
    }

    Spacer(modifier = Modifier.height(10.dp))
    HorizontalDivider(color = VitalBorder)
    Spacer(modifier = Modifier.height(10.dp))

    // Patient & Triage Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.Top
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(text = "Patient:", style = Typography.labelSmall.copy(color = VitalTextMuted))
        Text(text = emergencyCase.patientName, style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          Icon(Icons.Default.LocationOn, contentDescription = null, tint = VitalTextMuted, modifier = Modifier.size(12.dp))
          Text(text = emergencyCase.location, style = Typography.bodySmall.copy(color = VitalTextMuted, fontSize = 11.sp))
        }
      }

      Column(horizontalAlignment = Alignment.End) {
        StatusBadge(text = "${emergencyCase.conditionIcon} ${emergencyCase.department}", color = VitalText)
        Spacer(modifier = Modifier.height(4.dp))
        StatusBadge(
          text = "Severity: ${emergencyCase.severity.label}",
          color = if (emergencyCase.severity == SeverityLevel.CRITICAL) VitalRed else VitalAmber
        )
      }
    }

    if (emergencyCase.bloodReservation != null) {
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
          Icon(Icons.Default.Bloodtype, contentDescription = null, tint = VitalRed, modifier = Modifier.size(14.dp))
          Text(
            text = emergencyCase.bloodReservation,
            style = Typography.bodySmall.copy(color = VitalRed, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
          )
        }
      }
    }
  }

  // Active Closed-Loop Waiting State
  if (emergencyCase.status == CaseStatus.DISPATCHING) {
    VitalCard(borderColor = VitalAmber) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "QUERYING ATTEMPT #${emergencyCase.currentAttemptIndex + 1} OF ${emergencyCase.attemptQueue.size}",
            style = Typography.labelSmall.copy(color = VitalAmber, fontWeight = FontWeight.Bold)
          )
          Text(
            text = currentAttempt?.hospitalName ?: "Routing...",
            style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold)
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "${currentAttempt?.distanceKm ?: 0.0} km away • ETA: ${currentAttempt?.etaMinutes ?: 0} mins",
            style = Typography.bodySmall.copy(color = VitalText, fontWeight = FontWeight.SemiBold)
          )
        }

        RadialCountdownTimer(
          remainingSeconds = emergencyCase.remainingSeconds,
          totalSeconds = 30
        )
      }

      Spacer(modifier = Modifier.height(12.dp))
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(6.dp))
          .background(VitalAmberTint)
          .border(1.dp, VitalAmber.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
          .padding(10.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Icon(Icons.Default.HourglassTop, contentDescription = null, tint = VitalAmber, modifier = Modifier.size(16.dp))
          Text(
            text = "Emergency desk evaluating triage vitals. Closed loop automatically advances to Attempt #${emergencyCase.currentAttemptIndex + 2} on 30s timeout.",
            style = Typography.bodySmall.copy(color = VitalAmber, fontSize = 11.sp)
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Hospital ER Response Controls
      Text(
        text = "RECEIVING ER RESPONSE ACTIONS",
        style = Typography.labelSmall.copy(color = VitalTextMuted, fontWeight = FontWeight.Bold)
      )
      Spacer(modifier = Modifier.height(6.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Button(
          onClick = onConfirmEr,
          modifier = Modifier
            .weight(1f)
            .height(44.dp)
            .testTag("er_accept_action_button"),
          colors = ButtonDefaults.buttonColors(containerColor = VitalGreen, contentColor = VitalCard),
          shape = RoundedCornerShape(6.dp)
        ) {
          Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Hospital Acknowledge", style = Typography.labelLarge.copy(fontSize = 11.5.sp))
        }

        OutlinedButton(
          onClick = onDivertEr,
          modifier = Modifier
            .weight(1f)
            .height(44.dp)
            .testTag("er_divert_action_button"),
          border = BorderStroke(1.dp, VitalAmber),
          shape = RoundedCornerShape(6.dp)
        ) {
          Text("Redirect / Divert", color = VitalAmber, style = Typography.labelLarge.copy(fontSize = 11.5.sp))
        }
      }
    }
  }

  // Confirmed State Card
  if (emergencyCase.status == CaseStatus.CONFIRMED) {
    val hosp = emergencyCase.confirmedHospital
    VitalCard(borderColor = VitalGreen) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(VitalGreen),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.CheckCircle, contentDescription = null, tint = VitalCard, modifier = Modifier.size(22.dp))
        }
        Column {
          Text(
            text = "HOSPITAL RESPONSE CONFIRMED",
            style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = VitalGreen)
          )
          Text(
            text = "Trauma team on duty • ER bed reserved",
            style = Typography.bodySmall.copy(color = VitalTextMuted)
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
      HorizontalDivider(color = VitalBorder)
      Spacer(modifier = Modifier.height(12.dp))

      if (hosp != null) {
        Text(text = hosp.name, style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Text(text = hosp.location, style = Typography.bodySmall.copy(color = VitalTextMuted))

        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(6.dp))
              .background(VitalPanel)
              .padding(10.dp)
          ) {
            Column {
              Text(text = "ESTIMATED ETA", style = Typography.labelSmall.copy(fontSize = 10.sp))
              Text(text = "${hosp.etaMinutes} mins", style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = VitalGreen))
              Text(text = "${hosp.distanceKm} km away", style = Typography.bodySmall.copy(fontSize = 11.sp))
            }
          }

          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(6.dp))
              .background(VitalPanel)
              .padding(10.dp)
          ) {
            Column {
              Text(text = "BED RESERVATION", style = Typography.labelSmall.copy(fontSize = 10.sp))
              Text(text = "Bay 1 Cleared", style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = VitalText))
              Text(text = hosp.standbyDoctor.take(20), style = Typography.bodySmall.copy(fontSize = 11.sp))
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Button(
          onClick = { onCallHospital(hosp.phone) },
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("call_confirmed_hospital_button"),
          colors = ButtonDefaults.buttonColors(containerColor = VitalGreen, contentColor = VitalCard),
          shape = RoundedCornerShape(6.dp)
        ) {
          Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("CALL HOSPITAL ER DESK (${hosp.phone})", style = Typography.labelLarge.copy(fontWeight = FontWeight.Bold))
        }
      }
    }
  }

  // Supreme Court Fallback Card
  if (emergencyCase.status == CaseStatus.EXHAUSTED_FALLBACK) {
    val nearest = emergencyCase.confirmedHospital
    VitalCard(borderColor = VitalBlue) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(VitalBlue),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.Gavel, contentDescription = null, tint = VitalCard, modifier = Modifier.size(20.dp))
        }
        Column {
          Text(
            text = "SUPREME COURT EMERGENCY FALLBACK",
            style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = VitalBlue)
          )
          Text(
            text = "Pt. Parmanand Katara v. Union of India (1989)",
            style = Typography.bodySmall.copy(color = VitalTextMuted)
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(6.dp))
          .background(VitalBlueTint)
          .border(1.dp, VitalBlue.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
          .padding(10.dp)
      ) {
        Text(
          text = VitalDispatchEngine.SUPREME_COURT_MANDATE_MESSAGE,
          style = Typography.bodySmall.copy(color = VitalBlue, fontSize = 11.5.sp, lineHeight = 16.sp)
        )
      }

      if (nearest != null) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = nearest.name, style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Text(text = "${nearest.location} (${nearest.distanceKm} km • ETA: ${nearest.etaMinutes} mins)", style = Typography.bodySmall.copy(color = VitalTextMuted))

        Spacer(modifier = Modifier.height(10.dp))
        Button(
          onClick = { onCallHospital(nearest.phone) },
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("call_nearest_er_button"),
          colors = ButtonDefaults.buttonColors(containerColor = VitalBlue, contentColor = VitalCard),
          shape = RoundedCornerShape(6.dp)
        ) {
          Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("CALL CLOSEST HOSPITAL NOW (${nearest.phone})")
        }
      }
    }
  }

  // Phase 2 Timestamped Event Log with Color-Coded Dots
  VitalCard {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "LIVE CASE TIMELINE",
        style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = VitalTextMuted)
      )
      Text(
        text = "${emergencyCase.timelineEvents.size} Events Logged",
        style = Typography.labelSmall.copy(color = VitalTextLight, fontSize = 10.sp)
      )
    }

    Spacer(modifier = Modifier.height(10.dp))
    HorizontalDivider(color = VitalBorder.copy(alpha = 0.5f))
    Spacer(modifier = Modifier.height(10.dp))

    if (emergencyCase.timelineEvents.isEmpty()) {
      Text(
        text = "No timeline events recorded yet.",
        style = Typography.bodySmall.copy(color = VitalTextMuted)
      )
    } else {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        emergencyCase.timelineEvents.reversed().forEach { ev ->
          TimelineEventRow(event = ev)
        }
      }
    }
  }

  // First Aid Guidance
  FirstAidGuidanceCard(instructions = emergencyCase.firstAidNotes)

  // Reset / Return Button
  Button(
    onClick = onReset,
    modifier = Modifier
      .fillMaxWidth()
      .height(46.dp)
      .testTag("reset_case_button"),
    colors = ButtonDefaults.buttonColors(containerColor = VitalText, contentColor = VitalCard),
    shape = RoundedCornerShape(6.dp)
  ) {
    Text("DISMISS CASE & RETURN TO HOME")
  }
}

@Composable
fun FirstAidGuidanceCard(instructions: List<String>) {
  VitalCard {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Icon(Icons.Default.MedicalServices, contentDescription = null, tint = VitalRed)
      Text(
        text = "Immediate Bystander First-Aid Protocols",
        style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold)
      )
    }

    Spacer(modifier = Modifier.height(10.dp))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      instructions.forEach { tip ->
        Row(
          verticalAlignment = Alignment.Top,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(text = "•", color = VitalRed, fontWeight = FontWeight.Bold)
          Text(text = tip, style = Typography.bodySmall.copy(lineHeight = 17.sp))
        }
      }
    }
  }
}

@SuppressLint("MissingPermission")
private fun fetchCurrentGpsLocation(context: Context, onLocationResult: (String) -> Unit) {
  try {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    val lastKnown = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
      ?: locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

    if (lastKnown != null) {
      val lat = String.format("%.4f", lastKnown.latitude)
      val lon = String.format("%.4f", lastKnown.longitude)
      onLocationResult("GPS: $lat° N, $lon° E (Detected Location)")
    } else {
      onLocationResult("GPS: 12.9352° N, 77.6245° E (Koramangala, Bengaluru)")
    }
  } catch (e: Exception) {
    onLocationResult("GPS: 12.9352° N, 77.6245° E (Koramangala, Bengaluru)")
  }
}

private fun dialPhoneNumber(context: Context, phoneNumber: String) {
  val cleanPhone = phoneNumber.filter { it.isDigit() || it == '+' }
  val intent = Intent(Intent.ACTION_DIAL).apply {
    data = Uri.parse("tel:$cleanPhone")
  }
  context.startActivity(intent)
}
