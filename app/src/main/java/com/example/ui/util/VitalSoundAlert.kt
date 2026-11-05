package com.example.ui.util

import android.media.AudioManager
import android.media.ToneGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object VitalSoundAlert {
  private var toneGenerator: ToneGenerator? = null

  init {
    try {
      toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
    } catch (_: Exception) {
      // Graceful fallback if device audio service is restricted
    }
  }

  /**
   * Synthesized two-tone emergency alert sound (no external MP3 dependence).
   */
  fun playTwoToneEmergencyAlert() {
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val tg = toneGenerator ?: ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
        tg.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 300)
        delay(350)
        tg.startTone(ToneGenerator.TONE_PROP_BEEP2, 250)
      } catch (_: Exception) {
        // Safe fail
      }
    }
  }

  /**
   * Chime played when ER staff acknowledges a case.
   */
  fun playAcknowledgeChime() {
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val tg = toneGenerator ?: ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
        tg.startTone(ToneGenerator.TONE_PROP_ACK, 250)
      } catch (_: Exception) {
        // Safe fail
      }
    }
  }
}
