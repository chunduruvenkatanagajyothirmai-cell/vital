package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BloodBank
import com.example.ui.components.StatusBadge
import com.example.ui.components.VitalCard
import com.example.ui.theme.Typography
import com.example.ui.theme.VitalAmber
import com.example.ui.theme.VitalBg
import com.example.ui.theme.VitalBorder
import com.example.ui.theme.VitalCard
import com.example.ui.theme.VitalGreen
import com.example.ui.theme.VitalPanel
import com.example.ui.theme.VitalRed
import com.example.ui.theme.VitalText
import com.example.ui.theme.VitalTextLight
import com.example.ui.theme.VitalTextMuted
import com.example.ui.viewmodel.VitalViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VitalBloodBanksScreen(
  viewModel: VitalViewModel,
  modifier: Modifier = Modifier
) {
  val bloodBanks by viewModel.bloodBanks.collectAsState()
  val selectedBg by viewModel.selectedBloodGroup.collectAsState()
  val context = LocalContext.current

  val bloodGroups = listOf("All", "O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-")

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(VitalBg)
      .padding(horizontal = 16.dp, vertical = 12.dp)
  ) {
    // Screen Title
    Text(
      text = "City Blood Bank Inventory",
      style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold)
    )
    Text(
      text = "Live stock units tracked across verified regional component centers",
      style = Typography.bodySmall.copy(color = VitalTextMuted)
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Blood Group Filter
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      bloodGroups.forEach { bg ->
        val isSelected = (selectedBg == null && bg == "All") || (selectedBg == bg)
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) VitalRed else VitalCard)
            .border(1.dp, if (isSelected) VitalRed else VitalBorder, RoundedCornerShape(6.dp))
            .clickable {
              viewModel.setBloodGroupFilter(if (bg == "All") null else bg)
            }
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("blood_filter_$bg")
        ) {
          Text(
            text = bg,
            style = Typography.bodySmall.copy(
              color = if (isSelected) VitalCard else VitalText,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
              fontSize = 12.sp
            )
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    LazyColumn(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      items(bloodBanks, key = { it.id }) { bank ->
        BloodBankCardItem(
          bank = bank,
          selectedGroup = selectedBg,
          onCall = { phone ->
            val clean = phone.filter { it.isDigit() || it == '+' }
            val intent = Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:$clean") }
            context.startActivity(intent)
          },
          onReserve = { group ->
            Toast.makeText(context, "Reserved 1 unit of $group at ${bank.name}", Toast.LENGTH_SHORT).show()
          }
        )
      }
      item {
        Spacer(modifier = Modifier.height(20.dp))
      }
    }
  }
}

@Composable
fun BloodBankCardItem(
  bank: BloodBank,
  selectedGroup: String?,
  onCall: (String) -> Unit,
  onReserve: (String) -> Unit
) {
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
            .size(38.dp)
            .clip(CircleShape)
            .background(VitalPanel),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.Bloodtype, contentDescription = null, tint = VitalRed, modifier = Modifier.size(22.dp))
        }

        Column {
          Text(text = bank.name, style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold))
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = VitalTextMuted, modifier = Modifier.size(12.dp))
            Text(text = "${bank.location} (${bank.distanceKm} km)", style = Typography.bodySmall.copy(color = VitalTextMuted, fontSize = 11.sp))
          }
        }
      }

      StatusBadge(text = if (bank.operates24x7) "24x7 OPEN" else "8AM-8PM", color = VitalGreen)
    }

    Spacer(modifier = Modifier.height(10.dp))
    HorizontalDivider(color = VitalBorder.copy(alpha = 0.5f))
    Spacer(modifier = Modifier.height(10.dp))

    Text(
      text = "UNITS IN STOCK (${bank.totalUnits} Total Available)",
      style = Typography.labelSmall.copy(color = VitalTextMuted, fontWeight = FontWeight.Bold)
    )
    Spacer(modifier = Modifier.height(8.dp))

    // 8-Group Stock Table Grid
    val groups = listOf("O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-")
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      groups.take(4).forEach { grp ->
        val units = bank.stock[grp] ?: 0
        StockBox(group = grp, units = units, isHighlighted = selectedGroup == grp, modifier = Modifier.weight(1f))
      }
    }
    Spacer(modifier = Modifier.height(4.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      groups.drop(4).forEach { grp ->
        val units = bank.stock[grp] ?: 0
        StockBox(group = grp, units = units, isHighlighted = selectedGroup == grp, modifier = Modifier.weight(1f))
      }
    }

    Spacer(modifier = Modifier.height(12.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Button(
        onClick = { onCall(bank.phone) },
        modifier = Modifier
          .weight(1f)
          .height(42.dp)
          .testTag("call_bloodbank_${bank.id}"),
        colors = ButtonDefaults.buttonColors(containerColor = VitalCard, contentColor = VitalText),
        border = BorderStroke(1.dp, VitalBorder),
        shape = RoundedCornerShape(6.dp)
      ) {
        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(14.dp), tint = VitalText)
        Spacer(modifier = Modifier.width(6.dp))
        Text("Call Desk (${bank.phone})", style = Typography.labelLarge.copy(fontSize = 11.sp))
      }

      Button(
        onClick = { onReserve(selectedGroup ?: "O+") },
        modifier = Modifier
          .weight(1f)
          .height(42.dp)
          .testTag("reserve_blood_${bank.id}"),
        colors = ButtonDefaults.buttonColors(containerColor = VitalRed, contentColor = VitalCard),
        shape = RoundedCornerShape(6.dp)
      ) {
        Text("Reserve ${selectedGroup ?: "O+"} Unit", style = Typography.labelLarge.copy(fontSize = 11.sp))
      }
    }
  }
}

@Composable
fun StockBox(
  group: String,
  units: Int,
  isHighlighted: Boolean,
  modifier: Modifier = Modifier
) {
  val (bgColor, textColor) = when {
    units > 15 -> VitalPanel to VitalGreen
    units in 5..15 -> VitalPanel to VitalAmber
    else -> VitalPanel to VitalRed
  }

  Box(
    modifier = modifier
      .clip(RoundedCornerShape(4.dp))
      .background(if (isHighlighted) VitalRed.copy(alpha = 0.15f) else bgColor)
      .border(
        width = if (isHighlighted) 1.5.dp else 1.dp,
        color = if (isHighlighted) VitalRed else VitalBorder.copy(alpha = 0.5f),
        shape = RoundedCornerShape(4.dp)
      )
      .padding(vertical = 6.dp, horizontal = 2.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = group,
        style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp)
      )
      Text(
        text = "$units",
        style = Typography.bodySmall.copy(
          fontWeight = FontWeight.Bold,
          color = textColor,
          fontFamily = FontFamily.Monospace,
          fontSize = 12.sp
        )
      )
    }
  }
}
