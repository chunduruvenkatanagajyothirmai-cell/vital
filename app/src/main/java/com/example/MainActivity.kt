package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.outlined.Bloodtype
import androidx.compose.material.icons.outlined.Emergency
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.VitalHeader
import com.example.ui.screens.VitalBloodBanksScreen
import com.example.ui.screens.VitalDoctorsScreen
import com.example.ui.screens.VitalEmergencyScreen
import com.example.ui.screens.VitalErDeskScreen
import com.example.ui.theme.Typography
import com.example.ui.theme.VitalBg
import com.example.ui.theme.VitalCard
import com.example.ui.theme.VitalPanel
import com.example.ui.theme.VitalRed
import com.example.ui.theme.VitalTextMuted
import com.example.ui.theme.VitalTheme
import com.example.ui.viewmodel.VitalViewModel

data class NavTab(
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String
)

class MainActivity : ComponentActivity() {

  private val viewModel: VitalViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      VitalTheme {
        var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
        val context = LocalContext.current

        val navTabs = listOf(
          NavTab(
            label = "Emergency SOS",
            selectedIcon = Icons.Filled.Emergency,
            unselectedIcon = Icons.Outlined.Emergency,
            testTag = "nav_emergency"
          ),
          NavTab(
            label = "Specialists",
            selectedIcon = Icons.Filled.MedicalServices,
            unselectedIcon = Icons.Outlined.MedicalServices,
            testTag = "nav_doctors"
          ),
          NavTab(
            label = "Blood Banks",
            selectedIcon = Icons.Filled.Bloodtype,
            unselectedIcon = Icons.Outlined.Bloodtype,
            testTag = "nav_blood"
          ),
          NavTab(
            label = "ER Console",
            selectedIcon = Icons.Filled.LocalHospital,
            unselectedIcon = Icons.Outlined.LocalHospital,
            testTag = "nav_er_desk"
          )
        )

        Scaffold(
          modifier = Modifier.fillMaxSize(),
          contentWindowInsets = WindowInsets.safeDrawing,
          topBar = {
            VitalHeader(
              onStaffLogin = {
                selectedTabIndex = 3 // Switch to ER Console tab
                Toast.makeText(context, "Logged into Apex Metro ER Terminal", Toast.LENGTH_SHORT).show()
              },
              onResetDemo = {
                viewModel.resetDemo()
                selectedTabIndex = 0
                Toast.makeText(context, "Vital Demo Reset Complete", Toast.LENGTH_SHORT).show()
              }
            )
          },
          bottomBar = {
            NavigationBar(
              containerColor = VitalCard,
              tonalElevation = 0.dp,
              modifier = Modifier.background(VitalCard)
            ) {
              navTabs.forEachIndexed { index, tab ->
                val isSelected = selectedTabIndex == index
                NavigationBarItem(
                  selected = isSelected,
                  onClick = { selectedTabIndex = index },
                  icon = {
                    Icon(
                      imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                      contentDescription = tab.label
                    )
                  },
                  label = {
                    Text(
                      text = tab.label,
                      style = Typography.labelSmall.copy(
                        fontSize = 10.sp,
                        color = if (isSelected) VitalRed else VitalTextMuted
                      )
                    )
                  },
                  colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = VitalRed,
                    unselectedIconColor = VitalTextMuted,
                    indicatorColor = VitalPanel
                  ),
                  modifier = Modifier.testTag(tab.testTag)
                )
              }
            }
          }
        ) { innerPadding ->
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(VitalBg)
              .padding(innerPadding)
          ) {
            when (selectedTabIndex) {
              0 -> VitalEmergencyScreen(
                viewModel = viewModel,
                onNavigateToCheckup = { selectedTabIndex = 1 }
              )
              1 -> VitalDoctorsScreen(viewModel = viewModel)
              2 -> VitalBloodBanksScreen(viewModel = viewModel)
              3 -> VitalErDeskScreen(viewModel = viewModel)
            }
          }
        }
      }
    }
  }
}
