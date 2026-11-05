/**
 * Vital - Web Audio API Synthesized Two-Tone Emergency Sound System
 * Generates emergency tone alerts natively via browser AudioContext without external MP3 files.
 */

let audioCtx = null;

function getAudioContext() {
  if (!audioCtx) {
    const AudioContextClass = window.AudioContext || window.webkitAudioContext;
    if (AudioContextClass) {
      audioCtx = new AudioContextClass();
    }
  }
  if (audioCtx && audioCtx.state === "suspended") {
    audioCtx.resume();
  }
  return audioCtx;
}

/**
 * Synthesizes a two-tone emergency siren alert:
 * Plays 880Hz (high tone) followed by 660Hz (low tone) with clean gain envelopes.
 */
export function playEmergencyAlertSound() {
  try {
    const ctx = getAudioContext();
    if (!ctx) return;

    const now = ctx.currentTime;

    // Tone 1: High tone (880 Hz)
    const osc1 = ctx.createOscillator();
    const gain1 = ctx.createGain();
    osc1.type = "sine";
    osc1.frequency.setValueAtTime(880, now);

    gain1.gain.setValueAtTime(0, now);
    gain1.gain.linearRampToValueAtTime(0.25, now + 0.05);
    gain1.gain.linearRampToValueAtTime(0.25, now + 0.3);
    gain1.gain.linearRampToValueAtTime(0, now + 0.35);

    osc1.connect(gain1);
    gain1.connect(ctx.destination);
    osc1.start(now);
    osc1.stop(now + 0.35);

    // Tone 2: Low tone (660 Hz)
    const osc2 = ctx.createOscillator();
    const gain2 = ctx.createGain();
    osc2.type = "sine";
    osc2.frequency.setValueAtTime(660, now + 0.38);

    gain2.gain.setValueAtTime(0, now + 0.38);
    gain2.gain.linearRampToValueAtTime(0.25, now + 0.43);
    gain2.gain.linearRampToValueAtTime(0.25, now + 0.68);
    gain2.gain.linearRampToValueAtTime(0, now + 0.73);

    osc2.connect(gain2);
    gain2.connect(ctx.destination);
    osc2.start(now + 0.38);
    osc2.stop(now + 0.73);
  } catch (err) {
    console.warn("Web Audio API synthesis bypassed:", err);
  }
}

/**
 * Pleasant single-frequency confirmation chime on ER bed acknowledgement
 */
export function playAcknowledgeChime() {
  try {
    const ctx = getAudioContext();
    if (!ctx) return;

    const now = ctx.currentTime;
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();

    osc.type = "triangle";
    osc.frequency.setValueAtTime(523.25, now); // C5
    osc.frequency.exponentialRampToValueAtTime(659.25, now + 0.2); // E5

    gain.gain.setValueAtTime(0.2, now);
    gain.gain.exponentialRampToValueAtTime(0.001, now + 0.45);

    osc.connect(gain);
    gain.connect(ctx.destination);
    osc.start(now);
    osc.stop(now + 0.45);
  } catch (err) {
    console.warn("Chime synthesis error:", err);
  }
}
