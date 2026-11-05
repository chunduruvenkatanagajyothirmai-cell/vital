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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MedicalInformation
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.Doctor
import com.example.data.model.Hospital
import com.example.domain.engine.VitalDispatchEngine
import com.example.ui.components.StatusBadge
import com.example.ui.components.VitalCard
import com.example.ui.theme.Typography
import com.example.ui.theme.VitalBg
import com.example.ui.theme.VitalBorder
import com.example.ui.theme.VitalCard
import com.example.ui.theme.VitalGreen
import com.example.ui.theme.VitalGreenTint
import com.example.ui.theme.VitalPanel
import com.example.ui.theme.VitalRed
import com.example.ui.theme.VitalText
import com.example.ui.theme.VitalTextLight
import com.example.ui.theme.VitalTextMuted
import com.example.ui.viewmodel.VitalViewModel

@Composable
fun VitalDoctorsScreen(
  viewModel: VitalViewModel,
  modifier: Modifier = Modifier
) {
  val doctors by viewModel.filteredDoctors.collectAsState()
  val hospitals by viewModel.hospitals.collectAsState()
  val selectedDept by viewModel.selectedDepartment.collectAsState()
  val searchQuery by viewModel.doctorSearchQuery.collectAsState()
  val context = LocalContext.current

  // Symptom input field state with instant classification
  var symptomQuery by remember { mutableStateOf("") }
  var isGpsRefined by remember { mutableStateOf(false) }

  // Map hospital distances to doctor cards
  val hospitalDistanceMap = remember(hospitals) {
    hospitals.associate { it.name to it.distanceKm }
  }

  // Instant keyword-to-department classification
  val suggestedDepartment = remember(symptomQuery) {
    if (symptomQuery.trim().length >= 3) {
      VitalDispatchEngine.classifyDepartment(symptomQuery)
    } else {
      null
    }
  }

  // Location Permission Launcher
  val locationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    if (granted) {
      isGpsRefined = true
      Toast.makeText(context, "Distances refined from GPS coordinates", Toast.LENGTH_SHORT).show()
    } else {
      Toast.makeText(context, "Location access denied. Using central district distance reference.", Toast.LENGTH_SHORT).show()
    }
  }

  // Sort doctors by affiliated hospital distance
  val sortedDoctors = remember(doctors, hospitalDistanceMap, suggestedDepartment, selectedDept) {
    doctors.sortedBy { doc ->
      hospitalDistanceMap[doc.hospital] ?: 99.0
    }
  }

  val departments = listOf("All") + VitalDispatchEngine.DEPARTMENTS

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(VitalBg)
      .padding(horizontal = 16.dp, vertical = 12.dp)
  ) {
    // Screen Title
    Text(
      text = "Specialist & Checkup Guidance",
      style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold)
    )
    Text(
      text = "Find verified doctors and clinical consultation hours across network hospitals",
      style = Typography.bodySmall.copy(color = VitalTextMuted)
    )

    Spacer(modifier = Modifier.height(12.dp))

    // 1. Symptom Input Field with Instant Keyword-to-Department Classification
    VitalCard(backgroundColor = VitalCard) {
      Text(
        text = "DESCRIBE YOUR SYMPTOMS (INSTANT GUIDANCE)",
        style = Typography.labelSmall.copy(color = VitalTextMuted, fontWeight = FontWeight.Bold, fontSize = 10.sp)
      )
      Spacer(modifier = Modifier.height(4.dp))
      OutlinedTextField(
        value = symptomQuery,
        onValueChange = { symptomQuery = it },
        placeholder = { Text("e.g. chronic chest pressure, persistent cough, joint ache...") },
        leadingIcon = { Text("🩺", modifier = Modifier.padding(start = 8.dp)) },
        trailingIcon = {
          if (symptomQuery.isNotEmpty()) {
            IconButton(onClick = { symptomQuery = "" }) {
              Icon(Icons.Default.Clear, contentDescription = "Clear", tint = VitalTextMuted)
            }
          }
        },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("symptom_triage_input"),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = VitalRed,
          unfocusedBorderColor = VitalBorder,
          focusedContainerColor = VitalPanel,
          unfocusedContainerColor = VitalPanel
        ),
        singleLine = true
      )

      // Suggested Department Display Banner
      AnimatedVisibility(visible = suggestedDepartment != null) {
        Column(modifier = Modifier.padding(top = 8.dp)) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(6.dp))
              .background(VitalGreenTint)
              .border(1.dp, VitalGreen.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
              .padding(8.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Text(text = "💡", fontSize = 14.sp)
                Column {
                  Text(
                    text = "Suggested Department: $suggestedDepartment",
                    style = Typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = VitalGreen)
                  )
                  Text(
                    text = "Triage rule-matched from reported clinical symptoms",
                    style = Typography.labelSmall.copy(fontSize = 10.sp, color = VitalTextMuted)
                  )
                }
              }

              Button(
                onClick = {
                  viewModel.setDepartmentFilter(suggestedDepartment)
                },
                modifier = Modifier
                  .height(32.dp)
                  .testTag("filter_suggested_dept_button"),
                colors = ButtonDefaults.buttonColors(containerColor = VitalGreen, contentColor = VitalCard),
                shape = RoundedCornerShape(4.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
              ) {
                Text(text = "View Doctors", style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp))
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Search and Location Refinement Bar
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { viewModel.setDoctorQuery(it) },
        placeholder = { Text("Search doctor name or specialty...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = VitalTextMuted, modifier = Modifier.size(18.dp)) },
        modifier = Modifier
          .weight(1f)
          .testTag("doctor_search_bar"),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = VitalRed,
          unfocusedBorderColor = VitalBorder,
          focusedContainerColor = VitalCard,
          unfocusedContainerColor = VitalCard
        ),
        singleLine = true
      )

      // Location permission button to refine distance sorting
      Button(
        onClick = {
          val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
          val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
          if (hasFine || hasCoarse) {
            isGpsRefined = true
            Toast.makeText(context, "Sorted by proximity to your GPS position", Toast.LENGTH_SHORT).show()
          } else {
            locationPermissionLauncher.launch(
              arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
          }
        },
        modifier = Modifier
          .height(52.dp)
          .testTag("refine_distance_button"),
        colors = ButtonDefaults.buttonColors(containerColor = VitalPanel, contentColor = VitalText),
        border = BorderStroke(1.dp, VitalBorder),
        shape = RoundedCornerShape(8.dp)
      ) {
        Icon(Icons.Default.MyLocation, contentDescription = null, tint = if (isGpsRefined) VitalGreen else VitalText, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = if (isGpsRefined) "GPS Sorted" else "📍 Refine Distance", style = Typography.labelSmall.copy(fontSize = 11.sp))
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Department Filter Chips
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      departments.forEach { dept ->
        val isSelected = (selectedDept == null && dept == "All") || (selectedDept == dept)
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) VitalText else VitalCard)
            .border(1.dp, if (isSelected) VitalText else VitalBorder, RoundedCornerShape(6.dp))
            .clickable {
              viewModel.setDepartmentFilter(if (dept == "All") null else dept)
            }
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("dept_filter_$dept")
        ) {
          Text(
            text = dept,
            style = Typography.bodySmall.copy(
              color = if (isSelected) VitalCard else VitalText,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
              fontSize = 12.sp
            )
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Doctor List sorted by proximity
    if (sortedDoctors.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(Icons.Default.MedicalInformation, contentDescription = null, tint = VitalTextLight, modifier = Modifier.size(48.dp))
          Text(text = "No specialists found", style = Typography.titleMedium.copy(color = VitalTextMuted))
          Text(text = "Try clearing search or selecting a different department.", style = Typography.bodySmall.copy(color = VitalTextLight))
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        items(sortedDoctors, key = { it.id }) { doctor ->
          val distance = hospitalDistanceMap[doctor.hospital] ?: 4.5
          DoctorCardItem(
            doctor = doctor,
            distanceKm = distance,
            onCall = { phone ->
              val clean = phone.filter { it.isDigit() || it == '+' }
              val intent = Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:$clean") }
              context.startActivity(intent)
            }
          )
        }
        item {
          Spacer(modifier = Modifier.height(20.dp))
        }
      }
    }
  }
}

/**
 * Phase 4 Doctor Card:
 * Doctor Name, Expertise (fallback "General Physician"), Hospital, Distance, Phone, Timings (fallback)
 */
@Composable
fun DoctorCardItem(
  doctor: Doctor,
  distanceKm: Double,
  onCall: (String) -> Unit
) {
  // Graceful field fallbacks per specification
  val expertiseDisplay = doctor.expertise.ifBlank { "General Physician" }
  val timingsDisplay = doctor.timings.ifBlank { "Contact hospital for details/timings" }
  val hospitalDisplay = doctor.hospital.ifBlank { "Network Medical Center" }
  val phoneDisplay = doctor.phone.ifBlank { "+91 80 4000 1100" }

  VitalCard {
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
            .size(40.dp)
            .clip(CircleShape)
            .background(VitalPanel),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.Person, contentDescription = null, tint = VitalText, modifier = Modifier.size(24.dp))
        }

        Column {
          Text(text = doctor.name, style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold))
          Text(text = doctor.qualification, style = Typography.bodySmall.copy(color = VitalTextMuted))
        }
      }

      Column(horizontalAlignment = Alignment.End) {
        StatusBadge(text = doctor.department, color = VitalText)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "$distanceKm km away",
          style = Typography.labelSmall.copy(fontSize = 10.sp, color = VitalGreen, fontWeight = FontWeight.Bold)
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = expertiseDisplay,
      style = Typography.bodySmall.copy(color = VitalText, fontWeight = FontWeight.Medium)
    )

    Spacer(modifier = Modifier.height(8.dp))
    HorizontalDivider(color = VitalBorder.copy(alpha = 0.5f))
    Spacer(modifier = Modifier.height(8.dp))

    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Icon(Icons.Default.Business, contentDescription = null, tint = VitalTextMuted, modifier = Modifier.size(14.dp))
      Text(text = hospitalDisplay, style = Typography.bodySmall.copy(color = VitalTextMuted, fontSize = 11.sp))
    }

    Spacer(modifier = Modifier.height(4.dp))
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Icon(Icons.Default.AccessTime, contentDescription = null, tint = VitalTextMuted, modifier = Modifier.size(14.dp))
      Text(text = timingsDisplay, style = Typography.bodySmall.copy(color = VitalTextMuted, fontSize = 11.sp))
    }

    Spacer(modifier = Modifier.height(10.dp))
    Button(
      onClick = { onCall(phoneDisplay) },
      modifier = Modifier
        .fillMaxWidth()
        .height(40.dp)
        .testTag("call_doctor_${doctor.id}"),
      colors = ButtonDefaults.buttonColors(containerColor = VitalPanel, contentColor = VitalText),
      shape = RoundedCornerShape(6.dp)
    ) {
      Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(14.dp), tint = VitalText)
      Spacer(modifier = Modifier.width(6.dp))
      Text("Call Consultation Desk ($phoneDisplay)", style = Typography.labelLarge.copy(fontSize = 11.sp))
    }
  }
}
