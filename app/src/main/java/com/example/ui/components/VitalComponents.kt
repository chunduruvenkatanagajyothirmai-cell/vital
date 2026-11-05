package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EventLevel
import com.example.data.model.TimelineEvent
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
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.example.R
import com.example.ui.theme.VitalTextLight
import com.example.ui.theme.VitalTextMuted

@Composable
fun VitalHeader(
  modifier: Modifier = Modifier,
  onStaffLogin: (() -> Unit)? = null,
  onResetDemo: (() -> Unit)? = null
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    color = VitalCard,
    border = BorderStroke(1.dp, VitalBorder)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Box(
            modifier = Modifier
              .size(38.dp)
              .clip(RoundedCornerShape(8.dp))
              .border(1.dp, Color(0xFFBAE6FD), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
          ) {
            Image(
              painter = painterResource(id = R.drawable.vital_official_logo),
              contentDescription = "Vital Official Logo",
              modifier = Modifier.size(38.dp),
              contentScale = ContentScale.Crop
            )
          }
          Column {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text(
                text = "VITAL",
                style = Typography.titleMedium.copy(
                  fontWeight = FontWeight.Black,
                  letterSpacing = 1.2.sp,
                  color = Color(0xFF0A2540)
                )
              )
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(4.dp))
                  .background(VitalPanel)
                  .padding(horizontal = 5.dp, vertical = 1.dp)
              ) {
                Text(
                  text = "DISPATCH",
                  style = Typography.labelSmall.copy(
                    color = VitalTextMuted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp
                  )
                )
              }
            }
            Text(
              text = "Not just an alert. A confirmed response.",
              style = Typography.bodySmall.copy(color = VitalTextMuted, fontSize = 10.5.sp, fontWeight = FontWeight.Medium)
            )
          }
        }

        // Header Actions: Staff Login & Reset Demo
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (onStaffLogin != null) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(VitalPanel)
                .border(1.dp, VitalBorder, RoundedCornerShape(6.dp))
                .clickable { onStaffLogin() }
                .padding(horizontal = 8.dp, vertical = 5.dp)
                .testTag("header_staff_login_button")
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.LocalHospital,
                  contentDescription = null,
                  modifier = Modifier.size(12.dp),
                  tint = VitalText
                )
                Text(
                  text = "Hospital Staff Login",
                  style = Typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VitalText
                  )
                )
              }
            }
          }

          if (onResetDemo != null) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(VitalPanel)
                .border(1.dp, VitalBorder, RoundedCornerShape(6.dp))
                .clickable { onResetDemo() }
                .padding(horizontal = 8.dp, vertical = 5.dp)
                .testTag("header_reset_demo_button")
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Refresh,
                  contentDescription = null,
                  modifier = Modifier.size(12.dp),
                  tint = VitalTextMuted
                )
                Text(
                  text = "Reset Demo",
                  style = Typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal,
                    color = VitalTextMuted
                  )
                )
              }
            }
          }
        }
      }
    }
  }
}

/**
 * Official Vital Brand Hero Card:
 * Renders the rounded-square app icon with light blue border,
 * deep navy bold VITAL title, thin blue pulse line, and tagline.
 */
@Composable
fun VitalBrandHeroCard(
  modifier: Modifier = Modifier
) {
  VitalCard(
    modifier = modifier.fillMaxWidth(),
    backgroundColor = VitalCard,
    borderColor = VitalBorder
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 12.dp, horizontal = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Box(
        modifier = Modifier
          .size(76.dp)
          .clip(RoundedCornerShape(18.dp))
          .border(2.dp, Color(0xFFBAE6FD), RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center
      ) {
        Image(
          painter = painterResource(id = R.drawable.vital_official_logo),
          contentDescription = "Official Vital Logo Icon",
          modifier = Modifier.size(76.dp),
          contentScale = ContentScale.Crop
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = "VITAL",
        style = Typography.titleLarge.copy(
          fontWeight = FontWeight.Black,
          letterSpacing = 2.sp,
          color = Color(0xFF0A2540)
        )
      )

      Spacer(modifier = Modifier.height(4.dp))

      // Thin blue dividing line with central ECG heartbeat wave indicator
      Row(
        modifier = Modifier
          .width(220.dp)
          .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
      ) {
        Box(
          modifier = Modifier
            .weight(1f)
            .height(1.5.dp)
            .background(Color(0xFF2563EB).copy(alpha = 0.4f))
        )
        Text(
          text = " ﮩ٨ـﮩﮩ٨ـ ",
          style = Typography.bodySmall.copy(
            color = Color(0xFF1D4ED8),
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
          )
        )
        Box(
          modifier = Modifier
            .weight(1f)
            .height(1.5.dp)
            .background(Color(0xFF2563EB).copy(alpha = 0.4f))
        )
      }

      Spacer(modifier = Modifier.height(2.dp))

      Text(
        text = "\"Not just an alert. A confirmed response.\"",
        style = Typography.bodyMedium.copy(
          fontWeight = FontWeight.SemiBold,
          color = VitalText,
          fontSize = 12.5.sp
        )
      )
    }
  }
}


@Composable
fun StatusBadge(
  text: String,
  color: Color,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  isMonospace: Boolean = false
) {
  val (tintBg, borderColor) = when (color) {
    VitalRed -> VitalRedTint to VitalRed.copy(alpha = 0.4f)
    VitalGreen -> VitalGreenTint to VitalGreen.copy(alpha = 0.4f)
    VitalAmber -> VitalAmberTint to VitalAmber.copy(alpha = 0.4f)
    VitalBlue -> VitalBlueTint to VitalBlue.copy(alpha = 0.4f)
    else -> VitalPanel to VitalBorder
  }

  Row(
    modifier = modifier
      .clip(RoundedCornerShape(6.dp))
      .background(tintBg)
      .border(1.dp, borderColor, RoundedCornerShape(6.dp))
      .padding(horizontal = 8.dp, vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    if (icon != null) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = color,
        modifier = Modifier.size(13.dp)
      )
    }
    Text(
      text = text,
      color = color,
      fontSize = 11.sp,
      fontWeight = FontWeight.SemiBold,
      fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.SansSerif
    )
  }
}

@Composable
fun VitalCard(
  modifier: Modifier = Modifier,
  borderColor: Color = VitalBorder,
  backgroundColor: Color = VitalCard,
  content: @Composable () -> Unit
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(10.dp),
    color = backgroundColor,
    border = BorderStroke(1.dp, borderColor)
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      content()
    }
  }
}

@Composable
fun RadialCountdownTimer(
  remainingSeconds: Int,
  totalSeconds: Int = 30,
  modifier: Modifier = Modifier
) {
  val progress = (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
  val animatedProgress by animateFloatAsState(
    targetValue = progress,
    animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
    label = "countdownProgress"
  )

  val timerColor = when {
    remainingSeconds <= 5 -> VitalRed
    remainingSeconds <= 15 -> VitalAmber
    else -> VitalAmber
  }

  Box(
    modifier = modifier.size(68.dp),
    contentAlignment = Alignment.Center
  ) {
    CircularProgressIndicator(
      progress = { animatedProgress },
      modifier = Modifier.size(68.dp),
      color = timerColor,
      strokeWidth = 5.dp,
      trackColor = VitalPanel
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = "${remainingSeconds}s",
        style = Typography.titleMedium.copy(
          fontWeight = FontWeight.Bold,
          color = timerColor,
          fontFamily = FontFamily.Monospace
        )
      )
      Text(
        text = "WINDOW",
        style = Typography.labelSmall.copy(
          fontSize = 8.sp,
          color = VitalTextMuted
        )
      )
    }
  }
}

@Composable
fun TimestampText(
  label: String,
  timestamp: String,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    Text(
      text = "$label:",
      style = Typography.bodySmall.copy(color = VitalTextMuted, fontSize = 11.sp)
    )
    Text(
      text = timestamp,
      style = Typography.labelSmall.copy(color = VitalTextLight, fontSize = 11.sp)
    )
  }
}

@Composable
fun TimelineEventRow(
  event: TimelineEvent,
  modifier: Modifier = Modifier
) {
  val dotColor = when (event.level) {
    EventLevel.SUCCESS -> VitalGreen
    EventLevel.PENDING -> VitalAmber
    EventLevel.ALERT -> VitalRed
    EventLevel.REDIRECT -> VitalBlue
    EventLevel.INFO -> VitalTextMuted
  }

  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.Top,
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Box(
      modifier = Modifier
        .padding(top = 4.dp)
        .size(8.dp)
        .clip(CircleShape)
        .background(dotColor)
    )
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = event.message,
        style = Typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp, color = VitalText)
      )
      Text(
        text = event.timestamp,
        style = Typography.labelSmall.copy(fontSize = 10.sp, color = VitalTextLight)
      )
    }
  }
}
