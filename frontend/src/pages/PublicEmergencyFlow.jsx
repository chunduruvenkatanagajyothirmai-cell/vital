import React, { useState, useEffect } from "react";
import {
  dispatchEmergencyAlert,
  confirmEmergencyCase,
  timeoutEmergencyCase,
  checkBloodAvailability,
  socket
} from "../services/api";

import VitalLogo from "../components/VitalLogo";

export default function PublicEmergencyFlow({ onNavigateToCheckup, onStaffLogin }) {
  // Navigation & View States
  const [viewState, setViewState] = useState("HOME"); // "HOME" | "FORM" | "LIVE_CASE"

  // Step 1: Patient Name
  const [patientName, setPatientName] = useState("");

  // Step 2: Location
  const [location, setLocation] = useState("Outer Ring Road, Koramangala, Bengaluru");
  const [isManualLocation, setIsManualLocation] = useState(false);
  const [locationStatus, setLocationStatus] = useState("");

  // Step 3: Condition Selection (Universal Emoji Pictograms)
  const conditions = [
    {
      id: "trauma",
      icon: "🩸",
      label: "Bleeding / Injury",
      department: "Trauma",
      isTrauma: true,
      defaultDesc: "Severe bleeding from deep laceration following road crash, patient in shock."
    },
    {
      id: "pulmonology",
      icon: "🫁",
      label: "Can't Breathe Well",
      department: "Pulmonology",
      isTrauma: false,
      defaultDesc: "Acute respiratory distress, gasping for air, severe wheezing and choking sensation."
    },
    {
      id: "neurology",
      icon: "🆘",
      label: "Unconscious",
      department: "Neurology",
      isTrauma: false,
      defaultDesc: "Sudden collapse, unresponsive to voice or stimulation, breathing shallow."
    },
    {
      id: "cardiology",
      icon: "❤️",
      label: "Chest Pain",
      department: "Cardiology",
      isTrauma: false,
      defaultDesc: "Severe crushing chest pain radiating to left shoulder, profuse sweating, cold skin."
    },
    {
      id: "general",
      icon: "➕",
      label: "Something Else",
      department: "General Medicine",
      isTrauma: false,
      defaultDesc: "Sudden severe acute distress, extreme weakness, vomiting, and high fever."
    }
  ];

  const [selectedCondition, setSelectedCondition] = useState(conditions[0]);

  // Step 4: Description
  const [description, setDescription] = useState(conditions[0].defaultDesc);

  // Step 5: Blood Bank Step (Trauma only)
  const [bloodGroup, setBloodGroup] = useState("O+");
  const [bloodUnits, setBloodUnits] = useState(1);
  const [bloodCheckResult, setBloodCheckResult] = useState(null);
  const [isCheckingBlood, setIsCheckingBlood] = useState(false);

  // Step 6 / Live Case State
  const [activeCase, setActiveCase] = useState(null);
  const [countdown, setCountdown] = useState(30);
  const [timelineEvents, setTimelineEvents] = useState([]);
  const [isDispatching, setIsDispatching] = useState(false);

  // Location detection with graceful manual fallback
  const handleUseMyLocation = () => {
    if (!navigator.geolocation) {
      setIsManualLocation(true);
      setLocationStatus("Geolocation not supported by browser. Enter manually.");
      return;
    }
    setLocationStatus("Detecting GPS coordinates...");
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        const lat = pos.coords.latitude.toFixed(4);
        const lng = pos.coords.longitude.toFixed(4);
        setLocation(`GPS: ${lat}° N, ${lng}° E (Detected Location)`);
        setLocationStatus("GPS Location Locked");
      },
      (err) => {
        // Auto-fallback to manual if GPS fails/denied. Never block dispatch.
        setIsManualLocation(true);
        setLocationStatus("GPS signal unavailable. Please enter address manually.");
      },
      { timeout: 7000 }
    );
  };

  // Check Blood Availability & Reserve
  const handleCheckAndReserveBlood = async () => {
    setIsCheckingBlood(true);
    try {
      const res = await checkBloodAvailability(bloodGroup, bloodUnits);
      setBloodCheckResult(res);
    } catch (err) {
      setBloodCheckResult({
        available: false,
        message: `Error checking blood bank stock. Emergency triage will notify banks upon dispatch.`
      });
    } finally {
      setIsCheckingBlood(false);
    }
  };

  // Dispatch Emergency Alert (Step 6)
  const handleSendEmergencyAlert = async () => {
    setIsDispatching(true);
    try {
      const payload = {
        patient_name: patientName.trim() || "Unidentified Patient",
        location: location.trim() || "Central Emergency Corridor",
        description: description.trim() || selectedCondition.defaultDesc,
        blood_group: selectedCondition.isTrauma ? bloodGroup : null,
        units_needed: selectedCondition.isTrauma ? bloodUnits : 1
      };

      const res = await dispatchEmergencyAlert(payload);
      const newCase = res.case;
      setActiveCase(newCase);
      setCountdown(30);

      // Initialize Timeline Events
      const initialEvents = [
        {
          id: "ev-1",
          timestamp: new Date().toLocaleTimeString(),
          message: `Emergency alert initiated for ${payload.patient_name} at ${payload.location}.`,
          level: "info"
        },
        {
          id: "ev-2",
          timestamp: new Date().toLocaleTimeString(),
          message: `Triage assigned: ${newCase.department} (${newCase.severity} severity).`,
          level: newCase.severity === "Critical" ? "alert" : "success"
        }
      ];

      if (newCase.blood_reservation) {
        initialEvents.push({
          id: "ev-3",
          timestamp: new Date().toLocaleTimeString(),
          message: `Blood Bank: ${newCase.blood_reservation}`,
          level: "success"
        });
      }

      const firstAttempt = newCase.attempt_queue?.[0];
      if (firstAttempt) {
        initialEvents.push({
          id: "ev-4",
          timestamp: new Date().toLocaleTimeString(),
          message: `Querying Attempt #1: ${firstAttempt.hospital_name} (${firstAttempt.distance_km} km, ETA: ${firstAttempt.eta_minutes} mins). 30s confirmation window active.`,
          level: "pending"
        });
      }

      setTimelineEvents(initialEvents);
      setViewState("LIVE_CASE");

      // Subscribe to real-time socket events for this case
      socket.emit("subscribe_case", { case_id: newCase.id });
    } catch (err) {
      alert(`Dispatch Error: ${err.message}`);
    } finally {
      setIsDispatching(false);
    }
  };

  // Real-time WebSocket Listeners
  useEffect(() => {
    socket.on("timer_tick", (data) => {
      if (activeCase && data.case_id === activeCase.id) {
        setCountdown(data.remaining_seconds);
      }
    });

    socket.on("case_confirmed", (data) => {
      if (activeCase && data.case_id === activeCase.id) {
        setActiveCase((prev) => ({
          ...prev,
          status: "CONFIRMED",
          confirmed_hospital: data.hospital,
          timestamps: { ...prev.timestamps, confirmed_at: data.timestamps?.confirmed_at }
        }));
        setTimelineEvents((prev) => [
          ...prev,
          {
            id: `ev-${Date.now()}`,
            timestamp: new Date().toLocaleTimeString(),
            message: `${data.hospital.name} ER Trauma Desk confirmed acceptance! ER Bed reserved. Staff standing by.`,
            level: "success"
          }
        ]);
      }
    });

    socket.on("queue_advanced", (data) => {
      if (activeCase && data.case_id === activeCase.id) {
        setCountdown(30);
        setActiveCase((prev) => ({
          ...prev,
          current_attempt_index: data.next_attempt_index
        }));
        setTimelineEvents((prev) => [
          ...prev,
          {
            id: `ev-to-${Date.now()}`,
            timestamp: new Date().toLocaleTimeString(),
            message: `Previous attempt response window timed out (30s elapsed).`,
            level: "alert"
          },
          {
            id: `ev-next-${Date.now()}`,
            timestamp: new Date().toLocaleTimeString(),
            message: `Advancing closed-loop queue to Attempt #${data.next_attempt_index + 1}: ${data.hospital_name} (ETA: ${data.eta_minutes} mins). 30s response window active.`,
            level: "pending"
          }
        ]);
      }
    });

    socket.on("supreme_court_fallback", (data) => {
      if (activeCase && data.case_id === activeCase.id) {
        setActiveCase((prev) => ({
          ...prev,
          status: "EXHAUSTED_FALLBACK",
          confirmed_hospital: data.nearest_hospital
        }));
        setTimelineEvents((prev) => [
          ...prev,
          {
            id: `ev-sc-${Date.now()}`,
            timestamp: new Date().toLocaleTimeString(),
            message: `All departmental ER candidate queues exhausted without confirmation.`,
            level: "alert"
          },
          {
            id: `ev-mandate-${Date.now()}`,
            timestamp: new Date().toLocaleTimeString(),
            message: `SUPREME COURT MANDATE ACTIVATED (Pt. Parmanand Katara v. Union of India). Routing unconditionally to nearest facility: ${data.nearest_hospital.name} (${data.nearest_hospital.distance_km} km).`,
            level: "fallback"
          }
        ]);
      }
    });

    return () => {
      socket.off("timer_tick");
      socket.off("case_confirmed");
      socket.off("queue_advanced");
      socket.off("supreme_court_fallback");
    };
  }, [activeCase]);

  // Demo Simulation Actions
  const handleSimulateAccept = async () => {
    if (!activeCase) return;
    await confirmEmergencyCase(activeCase.id, "Simulated acceptance by ER Trauma Team");
  };

  const handleSimulateTimeout = async () => {
    if (!activeCase) return;
    await timeoutEmergencyCase(activeCase.id, "Simulated hospital divert/timeout");
  };

  const handleResetDemo = () => {
    setActiveCase(null);
    setTimelineEvents([]);
    setViewState("HOME");
  };

  return (
    <div className="min-h-screen bg-[#F4F6F4] text-[#1E2B27] flex justify-center py-4 px-3 sm:px-6">
      <div className="w-full max-w-lg space-y-4">

        {/* Global Vital Official Branding Header */}
        <header className="bg-white border border-[#DCE4E0] rounded-xl p-3.5 flex flex-wrap items-center justify-between gap-3 shadow-sm">
          <VitalLogo variant="header" size="sm" />

          <div className="flex items-center gap-2">
            <button
              onClick={onStaffLogin}
              className="text-xs font-semibold px-2.5 py-1.5 bg-[#EEF2EF] border border-[#DCE4E0] rounded-lg text-[#1E2B27] hover:bg-[#DCE4E0] transition"
            >
              Hospital Staff Login
            </button>
            <button
              onClick={handleResetDemo}
              className="text-xs text-[#5A6B65] px-2 py-1.5 bg-[#EEF2EF] border border-[#DCE4E0] rounded-lg hover:bg-[#DCE4E0] transition"
            >
              Reset Demo
            </button>
          </div>
        </header>

        {/* VIEW 1: HOME NAVIGATION CARDS */}
        {viewState === "HOME" && (
          <div className="space-y-4">
            {/* Prominent Official Brand Hero Card */}
            <VitalLogo variant="full" size="md" className="shadow-sm border-[#DCE4E0]" />
            {/* Card 1: Emergency */}
            <div className="bg-white border-2 border-[#C0392B] rounded-xl p-5 shadow-sm space-y-3">
              <div className="flex items-start gap-3">
                <span className="text-3xl">🚨</span>
                <div>
                  <div className="flex items-center gap-2">
                    <h2 className="text-xl font-bold text-[#C0392B]">Emergency</h2>
                    <span className="bg-[#FBEBEA] text-[#C0392B] text-xs font-bold px-2 py-0.5 rounded border border-[#C0392B]/30">
                      CRITICAL
                    </span>
                  </div>
                  <p className="text-xs text-[#5A6B65] mt-1 leading-relaxed">
                    Immediate hospital confirmation with real-time 30-second closed-loop queue, trauma ER bed standby & routing.
                  </p>
                </div>
              </div>
              <button
                onClick={() => setViewState("FORM")}
                className="w-full py-3.5 bg-[#C0392B] text-white rounded-lg font-bold text-sm tracking-wide hover:bg-[#a93224] transition flex items-center justify-center gap-2"
              >
                <span>🚨</span> START EMERGENCY DISPATCH
              </button>
            </div>

            {/* Card 2: Normal Checkup */}
            <div className="bg-white border border-[#DCE4E0] rounded-xl p-5 shadow-sm space-y-3">
              <div className="flex items-start gap-3">
                <span className="text-3xl">🩺</span>
                <div>
                  <div className="flex items-center gap-2">
                    <h2 className="text-xl font-bold text-[#1E2B27]">Normal Checkup</h2>
                    <span className="bg-[#EEF2EF] text-[#5A6B65] text-xs font-medium px-2 py-0.5 rounded">
                      NON-URGENT
                    </span>
                  </div>
                  <p className="text-xs text-[#5A6B65] mt-1 leading-relaxed">
                    Find specialist doctors, review clinical consultation hours, and schedule appointments across network hospitals.
                  </p>
                </div>
              </div>
              <button
                onClick={onNavigateToCheckup}
                className="w-full py-3 bg-[#EEF2EF] text-[#1E2B27] border border-[#DCE4E0] rounded-lg font-semibold text-sm hover:bg-[#DCE4E0] transition flex items-center justify-center gap-2"
              >
                <span>🩺</span> FIND DOCTOR & APPOINTMENTS
              </button>
            </div>
          </div>
        )}

        {/* VIEW 2: 6-STEP PUBLIC BYSTANDER EMERGENCY FORM */}
        {viewState === "FORM" && (
          <div className="bg-white border border-[#DCE4E0] rounded-xl p-5 shadow-sm space-y-5">
            <div className="flex items-center justify-between border-b border-[#DCE4E0] pb-3">
              <div>
                <h2 className="text-lg font-bold flex items-center gap-2">
                  <span>🚨</span> Public Bystander Emergency Flow
                </h2>
                <p className="text-xs text-[#5A6B65]">Step-by-step triage & closed-loop hospital dispatch</p>
              </div>
              <button
                onClick={() => setViewState("HOME")}
                className="w-7 h-7 rounded-full bg-[#EEF2EF] flex items-center justify-center text-sm font-bold text-[#5A6B65] hover:bg-[#DCE4E0]"
              >
                ✕
              </button>
            </div>

            {/* Step 1: Patient Name (Optional) */}
            <div className="space-y-1.5">
              <label className="text-xs font-bold text-[#1E2B27] flex items-center gap-1.5">
                <span className="text-[#C0392B]">STEP 1:</span> Patient Name (Optional)
              </label>
              <input
                type="text"
                placeholder="e.g. John Doe, or select quick label"
                value={patientName}
                onChange={(e) => setPatientName(e.target.value)}
                className="w-full px-3 py-2.5 text-sm bg-white border border-[#DCE4E0] rounded-lg focus:outline-none focus:border-[#C0392B]"
              />
              <div className="flex gap-2 pt-1">
                {["👤 Me", "❓ Unknown Male", "❓ Unknown Female", "🧒 Child"].map((preset) => (
                  <button
                    key={preset}
                    type="button"
                    onClick={() => setPatientName(preset)}
                    className="text-[11px] px-2.5 py-1 bg-[#EEF2EF] border border-[#DCE4E0] rounded text-[#1E2B27] hover:bg-[#DCE4E0]"
                  >
                    {preset}
                  </button>
                ))}
              </div>
            </div>

            {/* Step 2: Location Selector */}
            <div className="space-y-2">
              <label className="text-xs font-bold text-[#1E2B27] flex items-center gap-1.5">
                <span className="text-[#C0392B]">STEP 2:</span> Incident Location
              </label>
              <div className="grid grid-cols-2 gap-2">
                <button
                  type="button"
                  onClick={handleUseMyLocation}
                  className="py-2.5 px-3 bg-[#EEF2EF] border border-[#DCE4E0] rounded-lg text-xs font-semibold text-[#1E2B27] hover:bg-[#DCE4E0] flex items-center justify-center gap-1.5"
                >
                  <span>📍</span> Use My Location
                </button>
                <button
                  type="button"
                  onClick={() => setIsManualLocation(!isManualLocation)}
                  className="py-2.5 px-3 bg-white border border-[#DCE4E0] rounded-lg text-xs font-semibold text-[#1E2B27] hover:bg-[#EEF2EF] flex items-center justify-center gap-1.5"
                >
                  <span>✏️</span> {isManualLocation ? "Hide Manual" : "Enter Manually"}
                </button>
              </div>

              {locationStatus && (
                <p className="text-[11px] text-[#5A6B65] italic">{locationStatus}</p>
              )}

              <input
                type="text"
                value={location}
                onChange={(e) => setLocation(e.target.value)}
                placeholder="Incident street address or landmarks"
                className="w-full px-3 py-2 text-sm bg-white border border-[#DCE4E0] rounded-lg focus:outline-none focus:border-[#C0392B]"
              />
            </div>

            {/* Step 3: Quick-Select Condition Grid */}
            <div className="space-y-2">
              <label className="text-xs font-bold text-[#1E2B27] flex items-center gap-1.5">
                <span className="text-[#C0392B]">STEP 3:</span> Patient Condition (Universal Pictograms)
              </label>
              <div className="grid grid-cols-1 gap-2">
                {conditions.map((cond) => {
                  const isSelected = selectedCondition.id === cond.id;
                  return (
                    <div
                      key={cond.id}
                      onClick={() => {
                        setSelectedCondition(cond);
                        setDescription(cond.defaultDesc);
                        if (!cond.isTrauma) setBloodCheckResult(null);
                      }}
                      className={`p-3 rounded-lg border cursor-pointer transition flex items-center justify-between ${
                        isSelected
                          ? "border-[#C0392B] bg-[#FBEBEA]/40"
                          : "border-[#DCE4E0] bg-[#EEF2EF]/40 hover:bg-[#EEF2EF]"
                      }`}
                    >
                      <div className="flex items-center gap-3">
                        <span className="text-2xl">{cond.icon}</span>
                        <div>
                          <div className={`text-sm font-bold ${isSelected ? "text-[#C0392B]" : "text-[#1E2B27]"}`}>
                            {cond.label}
                          </div>
                          <div className="text-[11px] text-[#5A6B65]">
                            Routes to {cond.department} Department
                          </div>
                        </div>
                      </div>
                      {isSelected && <span className="text-xs font-bold text-[#C0392B]">✓</span>}
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Step 4: Free-text Description Box */}
            <div className="space-y-1.5">
              <label className="text-xs font-bold text-[#1E2B27] flex items-center gap-1.5">
                <span className="text-[#C0392B]">STEP 4:</span> Symptoms & Emergency Description
              </label>
              <textarea
                rows={3}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Describe patient condition, conscious state, visible injuries..."
                className="w-full px-3 py-2 text-sm bg-white border border-[#DCE4E0] rounded-lg focus:outline-none focus:border-[#C0392B]"
              />
            </div>

            {/* Step 5: Blood Bank Step (Conditionally visible ONLY for Trauma/Bleeding) */}
            {selectedCondition.isTrauma && (
              <div className="p-3.5 bg-[#FBEBEA]/50 border border-[#C0392B]/30 rounded-lg space-y-3">
                <label className="text-xs font-bold text-[#C0392B] flex items-center gap-1.5">
                  <span>🩸</span> STEP 5: Blood Pre-Reservation (Trauma / Bleeding)
                </label>
                
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <span className="text-[11px] font-semibold text-[#1E2B27]">Blood Group:</span>
                    <select
                      value={bloodGroup}
                      onChange={(e) => {
                        setBloodGroup(e.target.value);
                        setBloodCheckResult(null);
                      }}
                      className="w-full mt-1 px-2.5 py-1.5 text-xs bg-white border border-[#DCE4E0] rounded-md focus:outline-none focus:border-[#C0392B]"
                    >
                      {["O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-"].map((bg) => (
                        <option key={bg} value={bg}>{bg}</option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <span className="text-[11px] font-semibold text-[#1E2B27]">Units Needed:</span>
                    <select
                      value={bloodUnits}
                      onChange={(e) => {
                        setBloodUnits(Number(e.target.value));
                        setBloodCheckResult(null);
                      }}
                      className="w-full mt-1 px-2.5 py-1.5 text-xs bg-white border border-[#DCE4E0] rounded-md focus:outline-none focus:border-[#C0392B]"
                    >
                      {[1, 2, 3].map((u) => (
                        <option key={u} value={u}>{u} Unit{u > 1 ? "s" : ""}</option>
                      ))}
                    </select>
                  </div>
                </div>

                <button
                  type="button"
                  onClick={handleCheckAndReserveBlood}
                  disabled={isCheckingBlood}
                  className="w-full py-2 bg-[#C0392B] text-white rounded text-xs font-bold hover:bg-[#a93224] transition disabled:opacity-50"
                >
                  {isCheckingBlood ? "Checking Stock..." : "Check & Reserve Blood"}
                </button>

                {bloodCheckResult && (
                  <div
                    className={`p-2.5 rounded text-xs font-semibold border ${
                      bloodCheckResult.available
                        ? "bg-[#EAF5EF] text-[#2F8F5B] border-[#2F8F5B]/30"
                        : "bg-[#FBF4EA] text-[#B9791E] border-[#B9791E]/30"
                    }`}
                  >
                    {bloodCheckResult.message}
                  </div>
                )}
              </div>
            )}

            {/* Step 6: Send Emergency Alert Button */}
            <button
              onClick={handleSendEmergencyAlert}
              disabled={isDispatching}
              className="w-full py-4 bg-[#C0392B] text-white rounded-lg font-bold text-sm tracking-wide hover:bg-[#a93224] transition flex items-center justify-center gap-2 shadow-sm disabled:opacity-50"
            >
              <span className="text-lg">🚨</span>
              <span>{isDispatching ? "DISPATCHING ALERT..." : "SEND EMERGENCY ALERT"}</span>
            </button>
          </div>
        )}

        {/* VIEW 3: CASE STATUS & LIVE CASE TIMELINE PANEL */}
        {viewState === "LIVE_CASE" && activeCase && (
          <div className="space-y-4">
            
            {/* Status Indicator Card */}
            <div
              className={`bg-white border rounded-xl p-4 shadow-sm space-y-2.5 ${
                activeCase.status === "CONFIRMED"
                  ? "border-[#2F8F5B]"
                  : activeCase.status === "EXHAUSTED_FALLBACK"
                  ? "border-[#3E6B99]"
                  : "border-[#B9791E]"
              }`}
            >
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <span className="text-xl">
                    {activeCase.status === "CONFIRMED" ? "✅" : activeCase.status === "EXHAUSTED_FALLBACK" ? "⚖️" : "⏳"}
                  </span>
                  <div>
                    <h3
                      className={`text-sm font-bold ${
                        activeCase.status === "CONFIRMED"
                          ? "text-[#2F8F5B]"
                          : activeCase.status === "EXHAUSTED_FALLBACK"
                          ? "text-[#3E6B99]"
                          : "text-[#B9791E]"
                      }`}
                    >
                      {activeCase.status === "CONFIRMED"
                        ? "CONFIRMED RESPONSE — ER STANDBY"
                        : activeCase.status === "EXHAUSTED_FALLBACK"
                        ? "SUPREME COURT FALLBACK PROTOCOL"
                        : "WAITING FOR ER CONFIRMATION"}
                    </h3>
                    <p className="text-[11px] font-mono text-[#5A6B65]">Ref: {activeCase.id}</p>
                  </div>
                </div>

                <span
                  className={`text-xs font-mono font-bold px-2 py-1 rounded ${
                    activeCase.status === "CONFIRMED"
                      ? "bg-[#EAF5EF] text-[#2F8F5B]"
                      : activeCase.status === "EXHAUSTED_FALLBACK"
                      ? "bg-[#EBF1F7] text-[#3E6B99]"
                      : "bg-[#FBF4EA] text-[#B9791E]"
                  }`}
                >
                  {activeCase.status}
                </span>
              </div>

              <div className="border-t border-[#DCE4E0] pt-2 flex items-center justify-between text-xs">
                <span>Patient: <strong>{activeCase.patient_name}</strong></span>
                <span className="bg-[#EEF2EF] px-2 py-0.5 rounded font-semibold text-[#1E2B27]">
                  {activeCase.department} ({activeCase.severity})
                </span>
              </div>
            </div>

            {/* Waiting State: Active Query & Countdown */}
            {activeCase.status === "DISPATCHING" && (
              <div className="bg-white border border-[#B9791E] rounded-xl p-4 shadow-sm space-y-3">
                <div className="flex items-center justify-between">
                  <div>
                    <span className="text-[11px] font-bold text-[#B9791E]">
                      ATTEMPT #{activeCase.current_attempt_index + 1} OF {activeCase.attempt_queue?.length || 1}
                    </span>
                    <h4 className="text-base font-bold text-[#1E2B27]">
                      {activeCase.attempt_queue?.[activeCase.current_attempt_index]?.hospital_name || "Querying candidate..."}
                    </h4>
                    <p className="text-xs text-[#5A6B65]">
                      {activeCase.attempt_queue?.[activeCase.current_attempt_index]?.distance_km} km away • ETA:{" "}
                      {activeCase.attempt_queue?.[activeCase.current_attempt_index]?.eta_minutes} mins
                    </p>
                  </div>

                  {/* 30s Countdown Dial */}
                  <div className="w-16 h-16 rounded-full border-4 border-[#B9791E] flex flex-col items-center justify-center bg-[#FBF4EA]">
                    <span className="font-mono text-base font-bold text-[#B9791E]">{countdown}s</span>
                    <span className="text-[8px] text-[#5A6B65] font-bold uppercase">WINDOW</span>
                  </div>
                </div>

                {/* Reviewer Simulation Controls */}
                <div className="pt-2 border-t border-[#DCE4E0] space-y-1.5">
                  <span className="text-[10px] font-bold text-[#5A6B65] uppercase">
                    ER Desk Simulation Controls (For Reviewers)
                  </span>
                  <div className="grid grid-cols-2 gap-2">
                    <button
                      onClick={handleSimulateAccept}
                      className="py-2 bg-[#2F8F5B] text-white rounded text-xs font-bold hover:bg-[#27784c] transition"
                    >
                      Simulate ER Accept
                    </button>
                    <button
                      onClick={handleSimulateTimeout}
                      className="py-2 bg-[#EEF2EF] border border-[#B9791E] text-[#B9791E] rounded text-xs font-bold hover:bg-[#FBF4EA] transition"
                    >
                      Divert / Timeout
                    </button>
                  </div>
                </div>
              </div>
            )}

            {/* Confirmed State Card */}
            {activeCase.status === "CONFIRMED" && activeCase.confirmed_hospital && (
              <div className="bg-white border-2 border-[#2F8F5B] rounded-xl p-5 shadow-sm space-y-3">
                <div className="flex items-center gap-2 text-[#2F8F5B] font-bold text-sm">
                  <span>✅</span> HOSPITAL CONFIRMED RESPONSE
                </div>
                <div>
                  <h4 className="text-lg font-bold">{activeCase.confirmed_hospital.name}</h4>
                  <p className="text-xs text-[#5A6B65]">{activeCase.confirmed_hospital.location}</p>
                </div>
                <div className="grid grid-cols-2 gap-2 text-xs">
                  <div className="bg-[#EEF2EF] p-2.5 rounded">
                    <div className="text-[10px] text-[#5A6B65]">ESTIMATED ETA</div>
                    <div className="font-bold text-[#2F8F5B] text-base">{activeCase.confirmed_hospital.eta_minutes || 5} mins</div>
                    <div className="text-[11px] text-[#5A6B65]">{activeCase.confirmed_hospital.distance_km} km straight</div>
                  </div>
                  <div className="bg-[#EEF2EF] p-2.5 rounded">
                    <div className="text-[10px] text-[#5A6B65]">ER BED RESERVATION</div>
                    <div className="font-bold text-[#1E2B27] text-base">Bay 1 Cleared</div>
                    <div className="text-[11px] text-[#5A6B65]">Trauma Team Ready</div>
                  </div>
                </div>
                <a
                  href={`tel:${activeCase.confirmed_hospital.phone}`}
                  className="block text-center w-full py-3 bg-[#2F8F5B] text-white rounded-lg font-bold text-xs hover:bg-[#27784c] transition"
                >
                  CALL HOSPITAL ER DESK ({activeCase.confirmed_hospital.phone})
                </a>
              </div>
            )}

            {/* Supreme Court Fallback Card */}
            {activeCase.status === "EXHAUSTED_FALLBACK" && activeCase.confirmed_hospital && (
              <div className="bg-white border-2 border-[#3E6B99] rounded-xl p-5 shadow-sm space-y-3">
                <div className="flex items-center gap-2 text-[#3E6B99] font-bold text-sm">
                  <span>⚖️</span> SUPREME COURT EMERGENCY MANDATE
                </div>
                <p className="text-xs text-[#3E6B99] bg-[#EBF1F7] p-2.5 rounded leading-relaxed border border-[#3E6B99]/30">
                  Under Supreme Court mandate (Pt. Parmanand Katara v. Union of India), every medical facility is legally bound to provide immediate stabilization regardless of administrative barriers. Routing to nearest emergency facility.
                </p>
                <div>
                  <h4 className="text-base font-bold">{activeCase.confirmed_hospital.name}</h4>
                  <p className="text-xs text-[#5A6B65]">
                    {activeCase.confirmed_hospital.location} ({activeCase.confirmed_hospital.distance_km} km)
                  </p>
                </div>
                <a
                  href={`tel:${activeCase.confirmed_hospital.phone}`}
                  className="block text-center w-full py-3 bg-[#3E6B99] text-white rounded-lg font-bold text-xs hover:bg-[#32567b] transition"
                >
                  CALL CLOSEST FACILITY ({activeCase.confirmed_hospital.phone})
                </a>
              </div>
            )}

            {/* Live Case Timeline Panel (Color-Coded Dots) */}
            <div className="bg-white border border-[#DCE4E0] rounded-xl p-4 shadow-sm space-y-3">
              <div className="flex items-center justify-between border-b border-[#DCE4E0] pb-2">
                <h4 className="text-xs font-bold text-[#5A6B65] uppercase">Live Case Timeline</h4>
                <span className="text-[10px] text-[#5A6B65]">{timelineEvents.length} Events</span>
              </div>

              <div className="space-y-3 max-h-60 overflow-y-auto">
                {timelineEvents.map((ev) => {
                  const dotColorClass =
                    ev.level === "success"
                      ? "status-dot-green"
                      : ev.level === "alert"
                      ? "status-dot-red"
                      : ev.level === "pending"
                      ? "status-dot-amber"
                      : ev.level === "fallback"
                      ? "status-dot-blue"
                      : "bg-[#5A6B65]";

                  return (
                    <div key={ev.id} className="flex items-start gap-2.5">
                      <div className={`w-2.5 h-2.5 rounded-full mt-1 flex-shrink-0 ${dotColorClass}`} />
                      <div className="flex-1">
                        <p className="text-xs text-[#1E2B27] leading-snug">{ev.message}</p>
                        <span className="text-[10px] font-mono text-[#5A6B65]">{ev.timestamp}</span>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Dismiss / Return to Home Button */}
            <button
              onClick={() => {
                setActiveCase(null);
                setViewState("HOME");
              }}
              className="w-full py-3 bg-[#1E2B27] text-white rounded-lg font-semibold text-xs hover:bg-black transition"
            >
              DISMISS CASE & RETURN TO HOME
            </button>
          </div>
        )}

      </div>
    </div>
  );
}
