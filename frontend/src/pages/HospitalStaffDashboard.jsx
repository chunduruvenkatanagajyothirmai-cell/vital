import React, { useState, useEffect } from "react";
import {
  fetchBloodBanks,
  confirmEmergencyCase,
  timeoutEmergencyCase,
  socket
} from "../services/api";
import { playEmergencyAlertSound, playAcknowledgeChime } from "../utils/audioAlert";
import VitalLogo from "../components/VitalLogo";

const REGISTERED_HOSPITALS = [
  {
    id: "hosp-001",
    name: "Apex Metro Super-Speciality Hospital",
    location: "84 Inner Ring Road, Koramangala, Bengaluru",
    distance_km: 2.4,
    phone: "+91 80 2553 9000",
    departments: ["Trauma", "Cardiology", "Neurology", "Orthopedics", "Pulmonology", "General Medicine"],
    er_beds_available: 5,
    active_doctors: 3
  },
  {
    id: "hosp-002",
    name: "St. Jude Memorial Institute of Cardiac Sciences",
    location: "12 Hosur Main Rd, BTM 1st Stage, Bengaluru",
    distance_km: 3.8,
    phone: "+91 80 4112 4400",
    departments: ["Cardiology", "Pulmonology", "General Medicine"],
    er_beds_available: 4,
    active_doctors: 2
  },
  {
    id: "hosp-003",
    name: "National Neuro & Trauma Center",
    location: "45 Indira Gandhi Rd, Jayanagar 4th Block, Bengaluru",
    distance_km: 4.6,
    phone: "+91 80 2665 1200",
    departments: ["Trauma", "Neurology", "Orthopedics", "ENT", "General Medicine"],
    er_beds_available: 3,
    active_doctors: 2
  },
  {
    id: "hosp-004",
    name: "Lotus Children & Women's Hospital",
    location: "22 Sarjapur Main Rd, HSR Layout Sector 2, Bengaluru",
    distance_km: 5.1,
    phone: "+91 80 3090 8800",
    departments: ["Pediatrics", "ENT", "General Medicine"],
    er_beds_available: 6,
    active_doctors: 2
  },
  {
    id: "hosp-005",
    name: "Manipal City General & Pulmonary Hospital",
    location: "98 Airport Rd, Domlur, Bengaluru",
    distance_km: 6.4,
    phone: "+91 80 2502 4444",
    departments: ["Pulmonology", "ENT", "Orthopedics", "General Medicine"],
    er_beds_available: 4,
    active_doctors: 2
  },
  {
    id: "hosp-006",
    name: "Fortis HealthCity Center",
    location: "154 Bannerghatta Rd, Bilekahalli, Bengaluru",
    distance_km: 7.2,
    phone: "+91 80 6621 4444",
    departments: ["Trauma", "Cardiology", "Neurology", "Orthopedics", "Pediatrics", "ENT", "Pulmonology", "General Medicine"],
    er_beds_available: 8,
    active_doctors: 4
  }
];

export default function HospitalStaffDashboard({ onSwitchToBystander }) {
  // Authentication State
  const [selectedHospitalId, setSelectedHospitalId] = useState(REGISTERED_HOSPITALS[0].id);
  const [password, setPassword] = useState("");
  const [authError, setAuthError] = useState("");
  const [authenticatedHospital, setAuthenticatedHospital] = useState(() => {
    const saved = localStorage.getItem("vital_auth_hospital");
    return saved ? JSON.parse(saved) : null;
  });

  // Dashboard & Case States
  const [activeCase, setActiveCase] = useState(null);
  const [countdown, setCountdown] = useState(30);
  const [soundEnabled, setSoundEnabled] = useState(true);
  const [caseHistory, setCaseHistory] = useState([]);

  // Compute expected demo password: first word + 123
  const getExpectedPassword = (hospName) => {
    const firstWord = hospName.trim().split(/[\s.-]+/)[0].toLowerCase();
    const cleanWord = firstWord.replace(/[^a-z0-9]/g, "");
    return `${cleanWord}123`;
  };

  // Staff Login Handler
  const handleLogin = (e) => {
    e.preventDefault();
    setAuthError("");

    const hospital = REGISTERED_HOSPITALS.find((h) => h.id === selectedHospitalId);
    if (!hospital) {
      setAuthError("Please select a valid hospital facility.");
      return;
    }

    const expected = getExpectedPassword(hospital.name);
    if (password.trim() === expected) {
      setAuthenticatedHospital(hospital);
      localStorage.setItem("vital_auth_hospital", JSON.stringify(hospital));
      setPassword("");
    } else {
      setAuthError(`Invalid password. Demo rule: first word in lowercase + 123 (e.g. "${expected}")`);
    }
  };

  // Staff Logout Handler
  const handleLogout = () => {
    setAuthenticatedHospital(null);
    localStorage.removeItem("vital_auth_hospital");
  };

  // WebSocket Integration for Real-Time Dispatch
  useEffect(() => {
    socket.on("case_dispatched", (caseData) => {
      // Check if this hospital is the current attempt target
      const currAttempt = caseData.attempt_queue?.[caseData.current_attempt_index];
      if (authenticatedHospital && currAttempt?.hospital_id === authenticatedHospital.id) {
        setActiveCase(caseData);
        setCountdown(30);
        if (soundEnabled) {
          playEmergencyAlertSound();
        }
      }
    });

    socket.on("timer_tick", (data) => {
      if (activeCase && data.case_id === activeCase.id) {
        setCountdown(data.remaining_seconds);
      }
    });

    socket.on("queue_advanced", (data) => {
      if (activeCase && data.case_id === activeCase.id) {
        // Check if now targeting this hospital
        setCountdown(30);
        if (data.hospital_id === authenticatedHospital?.id) {
          if (soundEnabled) playEmergencyAlertSound();
        } else {
          // If was targeting us and timed out, clear active alert
          setActiveCase(null);
        }
      }
    });

    socket.on("case_confirmed", (data) => {
      if (activeCase && data.case_id === activeCase.id) {
        setActiveCase((prev) => ({ ...prev, status: "CONFIRMED" }));
      }
    });

    return () => {
      socket.off("case_dispatched");
      socket.off("timer_tick");
      socket.off("queue_advanced");
      socket.off("case_confirmed");
    };
  }, [authenticatedHospital, activeCase, soundEnabled]);

  // ER Action 1: Acknowledge Case
  const handleAcknowledge = async () => {
    if (!activeCase) return;
    try {
      playAcknowledgeChime();
      await confirmEmergencyCase(
        activeCase.id,
        `Acknowledged by ${authenticatedHospital.name} ER Trauma Staff`
      );
      setCaseHistory((prev) => [
        {
          id: activeCase.id,
          patient: activeCase.patient_name,
          time: new Date().toLocaleTimeString(),
          status: "ACKNOWLEDGED"
        },
        ...prev
      ]);
      setActiveCase((prev) => ({ ...prev, status: "CONFIRMED" }));
    } catch (err) {
      alert(`Error acknowledging case: ${err.message}`);
    }
  };

  // ER Action 2: Mark Busy / Redirect
  const handleRedirect = async () => {
    if (!activeCase) return;
    try {
      await timeoutEmergencyCase(
        activeCase.id,
        `${authenticatedHospital.name} ER overcapacity — redirected`
      );
      setCaseHistory((prev) => [
        {
          id: activeCase.id,
          patient: activeCase.patient_name,
          time: new Date().toLocaleTimeString(),
          status: "REDIRECTED"
        },
        ...prev
      ]);
      setActiveCase(null);
    } catch (err) {
      alert(`Error redirecting case: ${err.message}`);
    }
  };

  // Trigger test case for demo evaluation
  const handleTriggerDrillAlert = () => {
    const drillCase = {
      id: `DRILL-${Math.floor(1000 + Math.random() * 9000)}`,
      patient_name: "Rahul Verma (Male, 28)",
      location: "Outer Ring Rd, Koramangala 100ft Junction",
      department: "Trauma",
      severity: "Critical",
      description: "Severe multi-vehicle collision, patient unconscious, arterial hemorrhage from lower limb.",
      blood_reservation: "O+ (1 Unit Reserved at Rotary Blood Center)",
      eta_minutes: 5,
      status: "DISPATCHING",
      attempt_queue: [
        {
          hospital_id: authenticatedHospital.id,
          hospital_name: authenticatedHospital.name,
          distance_km: authenticatedHospital.distance_km,
          eta_minutes: 5
        }
      ],
      current_attempt_index: 0
    };
    setActiveCase(drillCase);
    setCountdown(30);
    if (soundEnabled) {
      playEmergencyAlertSound();
    }
  };

  return (
    <div className="min-h-screen bg-[#F4F6F4] text-[#1E2B27] py-6 px-4 sm:px-8 flex justify-center">
      <div className="w-full max-w-4xl space-y-5">

        {/* Global Navigation Bar with Official Branding */}
        <div className="flex flex-wrap items-center justify-between gap-3 bg-white border border-[#DCE4E0] rounded-xl p-3.5 shadow-sm">
          <VitalLogo variant="header" size="sm" />

          <button
            onClick={onSwitchToBystander}
            className="text-xs font-semibold px-3 py-1.5 bg-[#EEF2EF] border border-[#DCE4E0] rounded-lg text-[#1E2B27] hover:bg-[#DCE4E0] transition"
          >
            Switch to Public Bystander SOS 🚨
          </button>
        </div>

        {/* PHASE 3 SCREEN 1: HOSPITAL STAFF LOGIN */}
        {!authenticatedHospital ? (
          <div className="max-w-md mx-auto bg-white border border-[#DCE4E0] rounded-xl p-6 shadow-sm space-y-5">
            <div className="text-center space-y-2">
              <div className="flex justify-center mb-1">
                <VitalLogo variant="icon" size="md" />
              </div>
              <h2 className="text-lg font-bold text-[#1E2B27]">Hospital Staff Login</h2>
              <p className="text-xs text-[#5A6B65]">Select registered hospital to open ER triage terminal</p>
            </div>

            <form onSubmit={handleLogin} className="space-y-4">
              <div>
                <label className="text-xs font-bold text-[#1E2B27] block mb-1">
                  Registered Hospital Facility:
                </label>
                <select
                  value={selectedHospitalId}
                  onChange={(e) => {
                    setSelectedHospitalId(e.target.value);
                    setAuthError("");
                  }}
                  className="w-full p-2.5 text-xs bg-white border border-[#DCE4E0] rounded-lg focus:outline-none focus:border-[#C0392B]"
                >
                  {REGISTERED_HOSPITALS.map((h) => (
                    <option key={h.id} value={h.id}>
                      {h.name} ({h.distance_km} km)
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="text-xs font-bold text-[#1E2B27] block mb-1">
                  Staff Access Password:
                </label>
                <input
                  type="password"
                  placeholder="Enter staff password"
                  value={password}
                  onChange={(e) => {
                    setPassword(e.target.value);
                    setAuthError("");
                  }}
                  className="w-full p-2.5 text-xs bg-white border border-[#DCE4E0] rounded-lg focus:outline-none focus:border-[#C0392B]"
                />
              </div>

              {/* Demo Password Auto-Fill Pill */}
              {selectedHospitalId && (
                <div
                  onClick={() => {
                    const h = REGISTERED_HOSPITALS.find((x) => x.id === selectedHospitalId);
                    if (h) setPassword(getExpectedPassword(h.name));
                  }}
                  className="p-2 bg-[#EEF2EF] rounded text-[11px] text-[#5A6B65] cursor-pointer hover:bg-[#DCE4E0] transition"
                >
                  Demo Quick Fill: <strong>{getExpectedPassword(REGISTERED_HOSPITALS.find((x) => x.id === selectedHospitalId)?.name || "")}</strong>
                </div>
              )}

              {/* Inline Error Message */}
              {authError && (
                <div className="p-2.5 bg-[#FBEBEA] border border-[#C0392B]/40 rounded-lg text-xs font-semibold text-[#C0392B]">
                  {authError}
                </div>
              )}

              <button
                type="submit"
                className="w-full py-3 bg-[#C0392B] text-white rounded-lg font-bold text-xs hover:bg-[#a93224] transition tracking-wide"
              >
                AUTHENTICATE ER TERMINAL
              </button>
            </form>
          </div>
        ) : (
          /* PHASE 3 SCREEN 2: HOSPITAL STAFF DASHBOARD */
          <div className="space-y-5">
            {/* 1. Header showing logged-in hospital name, distance, covered departments, active doctor count */}
            <div className="bg-white border border-[#DCE4E0] rounded-xl p-5 shadow-sm space-y-4">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                <div>
                  <div className="flex items-center gap-2">
                    <h2 className="text-xl font-bold text-[#1E2B27]">{authenticatedHospital.name}</h2>
                    <span className="bg-[#EAF5EF] text-[#2F8F5B] text-xs font-bold px-2 py-0.5 rounded border border-[#2F8F5B]/30">
                      LIVE TERMINAL
                    </span>
                  </div>
                  <p className="text-xs text-[#5A6B65] mt-0.5">
                    {authenticatedHospital.location} • {authenticatedHospital.distance_km} km from central hub
                  </p>
                </div>

                <div className="flex items-center gap-2">
                  <button
                    onClick={() => setSoundEnabled(!soundEnabled)}
                    className="text-xs px-3 py-1.5 border border-[#DCE4E0] rounded-lg bg-[#EEF2EF] text-[#1E2B27] hover:bg-[#DCE4E0] transition flex items-center gap-1.5"
                  >
                    <span>{soundEnabled ? "🔊" : "🔇"}</span>
                    <span>Audio Alert: {soundEnabled ? "ON" : "MUTED"}</span>
                  </button>

                  <button
                    onClick={handleLogout}
                    className="text-xs px-3 py-1.5 border border-[#DCE4E0] rounded-lg text-[#C0392B] hover:bg-[#FBEBEA] transition font-semibold"
                  >
                    Logout
                  </button>
                </div>
              </div>

              {/* Facility Capabilities Stats */}
              <div className="grid grid-cols-3 gap-3 border-t border-[#DCE4E0] pt-3 text-xs">
                <div className="bg-[#EEF2EF] p-2.5 rounded-lg">
                  <span className="text-[#5A6B65] text-[10px] uppercase font-bold">Available ER Beds</span>
                  <div className="font-bold text-base text-[#2F8F5B] mt-0.5">
                    {authenticatedHospital.er_beds_available} Beds Free
                  </div>
                </div>

                <div className="bg-[#EEF2EF] p-2.5 rounded-lg">
                  <span className="text-[#5A6B65] text-[10px] uppercase font-bold">On-Duty Medical Staff</span>
                  <div className="font-bold text-base text-[#1E2B27] mt-0.5">
                    {authenticatedHospital.active_doctors} Specialists
                  </div>
                </div>

                <div className="bg-[#EEF2EF] p-2.5 rounded-lg">
                  <span className="text-[#5A6B65] text-[10px] uppercase font-bold">Covered Departments</span>
                  <div className="font-semibold text-xs text-[#1E2B27] mt-0.5 truncate">
                    {authenticatedHospital.departments.slice(0, 3).join(", ")}
                    {authenticatedHospital.departments.length > 3 ? "..." : ""}
                  </div>
                </div>
              </div>
            </div>

            {/* 2. Incoming Case Display Card */}
            {activeCase && activeCase.status === "DISPATCHING" ? (
              <div className="bg-white border-2 border-[#C0392B] rounded-xl p-5 shadow-sm space-y-4">
                <div className="flex items-center justify-between border-b border-[#DCE4E0] pb-3">
                  <div className="flex items-center gap-2">
                    <span className="text-2xl animate-pulse">🚨</span>
                    <div>
                      <h3 className="text-base font-bold text-[#C0392B]">
                        INCOMING EMERGENCY DISPATCH ALERT
                      </h3>
                      <p className="text-xs font-mono text-[#5A6B65]">Ref: {activeCase.id}</p>
                    </div>
                  </div>

                  <span className="bg-[#FBEBEA] text-[#C0392B] text-xs font-bold px-2 py-0.5 rounded border border-[#C0392B]/30">
                    ACTION REQUIRED
                  </span>
                </div>

                {/* Live 30-Second Visual Countdown Bar */}
                <div className="space-y-1.5">
                  <div className="flex justify-between text-xs font-bold">
                    <span className="text-[#B9791E]">30-SECOND CONFIRMATION WINDOW</span>
                    <span className="font-mono text-[#C0392B]">{countdown}s Remaining</span>
                  </div>
                  <div className="w-full h-3 bg-[#EEF2EF] rounded-full overflow-hidden border border-[#DCE4E0]">
                    <div
                      className="h-full bg-[#C0392B] transition-all duration-1000 ease-linear rounded-full"
                      style={{ width: `${(countdown / 30) * 100}%` }}
                    />
                  </div>
                </div>

                {/* Case Info Grid */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
                  <div>
                    <span className="text-[#5A6B65] text-[11px]">Patient Identification:</span>
                    <div className="font-bold text-sm text-[#1E2B27]">{activeCase.patient_name}</div>
                    <div className="text-[11px] text-[#5A6B65] mt-0.5">📍 {activeCase.location}</div>
                  </div>

                  <div>
                    <span className="text-[#5A6B65] text-[11px]">Department & ETA:</span>
                    <div className="flex items-center gap-2 mt-0.5">
                      <span className="bg-[#EEF2EF] font-bold px-2 py-0.5 rounded text-xs text-[#1E2B27]">
                        {activeCase.department}
                      </span>
                      <span className="bg-[#FBEBEA] text-[#C0392B] font-bold px-2 py-0.5 rounded text-xs">
                        {activeCase.severity}
                      </span>
                      <span className="text-xs font-semibold text-[#2F8F5B]">
                        ETA: {activeCase.eta_minutes || 5} mins
                      </span>
                    </div>
                  </div>
                </div>

                {/* Reported Issue Text */}
                <div className="bg-[#EEF2EF] p-3 rounded-lg text-xs space-y-1">
                  <span className="font-bold text-[#5A6B65] text-[10px] uppercase">
                    Reported Symptoms & Triage Description:
                  </span>
                  <p className="text-[#1E2B27] leading-relaxed">{activeCase.description}</p>
                </div>

                {/* Blood Reservation Status */}
                {activeCase.blood_reservation && (
                  <div className="bg-[#FBEBEA] border border-[#C0392B]/30 p-2.5 rounded-lg text-xs font-semibold text-[#C0392B] flex items-center gap-2">
                    <span>🩸</span> {activeCase.blood_reservation}
                  </div>
                )}

                {/* Action Buttons: "✓ Acknowledge" and "✕ Mark Busy / Redirect" */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-2">
                  <button
                    onClick={handleAcknowledge}
                    className="py-3 bg-[#2F8F5B] text-white rounded-lg font-bold text-xs hover:bg-[#27784c] transition flex items-center justify-center gap-2"
                  >
                    <span>✓</span> Acknowledge & Prepare Bed
                  </button>

                  <button
                    onClick={handleRedirect}
                    className="py-3 bg-[#EEF2EF] border border-[#B9791E] text-[#B9791E] rounded-lg font-bold text-xs hover:bg-[#FBF4EA] transition flex items-center justify-center gap-2"
                  >
                    <span>✕</span> Mark Busy / Redirect
                  </button>
                </div>
              </div>
            ) : activeCase && activeCase.status === "CONFIRMED" ? (
              /* Confirmed Transit View */
              <div className="bg-white border-2 border-[#2F8F5B] rounded-xl p-5 shadow-sm space-y-3">
                <div className="flex items-center gap-2 text-[#2F8F5B] font-bold text-sm">
                  <span>✅</span> CASE ACKNOWLEDGED & ER BED CLEARED
                </div>
                <p className="text-xs text-[#1E2B27] leading-relaxed">
                  Patient <strong>{activeCase.patient_name}</strong> is in transit. Bay 1 trauma bed is cleared and on-duty specialists have been alerted.
                </p>
                <div className="text-[11px] text-[#5A6B65]">
                  Status synced in real-time with public bystander screen.
                </div>
              </div>
            ) : (
              /* Idle Standby Terminal */
              <div className="bg-white border border-[#DCE4E0] rounded-xl p-8 text-center space-y-3 shadow-sm">
                <span className="text-4xl">🏥</span>
                <h3 className="text-base font-bold text-[#1E2B27]">ER Terminal Standing By</h3>
                <p className="text-xs text-[#5A6B65] max-w-sm mx-auto">
                  Monitoring regional emergency dispatch network. Incoming critical alerts will sound the synthesized tone and trigger the 30-second response window.
                </p>
                <div className="pt-2">
                  <button
                    onClick={handleTriggerDrillAlert}
                    className="px-4 py-2.5 bg-[#C0392B] text-white rounded-lg font-bold text-xs hover:bg-[#a93224] transition shadow-sm"
                  >
                    Simulate Incoming Trauma Alert (Drill Mode)
                  </button>
                </div>
              </div>
            )}

            {/* 3. Hospital Emergency Log History */}
            <div className="bg-white border border-[#DCE4E0] rounded-xl p-4 shadow-sm space-y-3">
              <h4 className="text-xs font-bold text-[#5A6B65] uppercase">Recent Shift Dispatch Activity</h4>
              {caseHistory.length === 0 ? (
                <p className="text-xs text-[#5A6B65] italic">No prior cases handled during this shift.</p>
              ) : (
                <div className="space-y-2">
                  {caseHistory.map((item, idx) => (
                    <div key={idx} className="flex items-center justify-between text-xs border-b border-[#DCE4E0] pb-2">
                      <div>
                        <span className="font-mono font-bold text-[#1E2B27]">{item.id}</span> • {item.patient}
                      </div>
                      <div className="flex items-center gap-2">
                        <span className="text-[10px] text-[#5A6B65] font-mono">{item.time}</span>
                        <span
                          className={`text-[10px] font-bold px-2 py-0.5 rounded ${
                            item.status === "ACKNOWLEDGED"
                              ? "bg-[#EAF5EF] text-[#2F8F5B]"
                              : "bg-[#FBF4EA] text-[#B9791E]"
                          }`}
                        >
                          {item.status}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

          </div>
        )}

      </div>
    </div>
  );
}
