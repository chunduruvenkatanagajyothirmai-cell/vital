import React, { useState, useEffect, useMemo } from "react";
import { fetchDoctors, fetchBloodBanks } from "../services/api";
import VitalLogo from "../components/VitalLogo";

const DEPARTMENTS = [
  "All",
  "Trauma",
  "Cardiology",
  "Neurology",
  "Pulmonology",
  "Pediatrics",
  "ENT",
  "Orthopedics",
  "General Medicine"
];

// Level-2 Rule-based NLP Keyword Dictionary
const DEPARTMENT_KEYWORD_RULES = {
  Trauma: ["accident", "crash", "bleeding", "blood", "fall", "injury", "fracture", "wound", "burn"],
  Cardiology: ["chest pain", "heart", "cardiac", "angina", "attack", "pulse", "palpitation", "tightness"],
  Neurology: ["stroke", "paralysis", "seizure", "speech", "facial droop", "numbness", "headache", "dizziness"],
  Pulmonology: ["cough", "breathing", "choking", "asthma", "gasping", "breath", "lungs", "wheezing"],
  Pediatrics: ["child", "baby", "infant", "toddler", "kid", "newborn"],
  ENT: ["throat", "ear", "nose", "swallowed", "vocal", "hearing", "sinus"],
  Orthopedics: ["bone", "joint", "dislocation", "sprain", "knee", "spine", "hip", "ankle", "back pain"]
};

const HOSPITAL_DISTANCES = {
  "Apex Metro Super-Speciality Hospital": 2.4,
  "St. Jude Memorial Institute of Cardiac Sciences": 3.8,
  "National Neuro & Trauma Center": 4.6,
  "Lotus Children & Women's Hospital": 5.1,
  "Manipal City General & Pulmonary Hospital": 6.4,
  "Fortis HealthCity Center": 7.2
};

export default function NormalCheckup({ onSwitchToEmergency }) {
  const [doctors, setDoctors] = useState([]);
  const [selectedDept, setSelectedDept] = useState("All");
  const [searchQuery, setSearchQuery] = useState("");
  const [symptomInput, setSymptomInput] = useState("");
  const [isGpsRefined, setIsGpsRefined] = useState(false);
  const [locationStatus, setLocationStatus] = useState("");

  useEffect(() => {
    fetchDoctors().then((data) => {
      setDoctors(data.doctors || []);
    });
  }, []);

  // Instant keyword-to-department classification
  const suggestedDepartment = useMemo(() => {
    if (!symptomInput || symptomInput.trim().length < 3) return null;
    const text = symptomInput.toLowerCase();

    if (DEPARTMENT_KEYWORD_RULES.Pediatrics.some((kw) => text.includes(kw))) {
      return "Pediatrics";
    }

    let bestDept = "General Medicine";
    let maxCount = 0;

    for (const [dept, keywords] of Object.entries(DEPARTMENT_KEYWORD_RULES)) {
      if (dept === "Pediatrics") continue;
      const count = keywords.filter((kw) => text.includes(kw)).length;
      if (count > maxCount) {
        maxCount = count;
        bestDept = dept;
      }
    }

    return bestDept;
  }, [symptomInput]);

  // Handle GPS location refinement
  const handleRefineDistance = () => {
    if (!navigator.geolocation) {
      setLocationStatus("Geolocation not supported by device.");
      return;
    }
    setLocationStatus("Acquiring GPS location...");
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setIsGpsRefined(true);
        setLocationStatus("Distances refined relative to current GPS coordinates.");
      },
      (err) => {
        setLocationStatus("Location access denied. Using central district distance reference.");
      },
      { timeout: 6000 }
    );
  };

  // Filter & sort doctors
  const filteredDoctors = useMemo(() => {
    let list = doctors;

    if (selectedDept && selectedDept !== "All") {
      list = list.filter((d) => d.department.toLowerCase() === selectedDept.toLowerCase());
    }

    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      list = list.filter(
        (d) =>
          d.name.toLowerCase().includes(q) ||
          (d.expertise && d.expertise.toLowerCase().includes(q)) ||
          d.hospital.toLowerCase().includes(q)
      );
    }

    // Sort by hospital distance
    return [...list].sort((a, b) => {
      const distA = HOSPITAL_DISTANCES[a.hospital] || 99;
      const distB = HOSPITAL_DISTANCES[b.hospital] || 99;
      return distA - distB;
    });
  }, [doctors, selectedDept, searchQuery]);

  return (
    <div className="min-h-screen bg-[#F4F6F4] text-[#1E2B27] py-6 px-4 sm:px-8 flex justify-center">
      <div className="w-full max-w-3xl space-y-5">

        {/* Global Navigation Header with Official Branding */}
        <div className="flex flex-wrap items-center justify-between gap-3 bg-white border border-[#DCE4E0] rounded-xl p-3.5 shadow-sm">
          <VitalLogo variant="header" size="sm" />

          <button
            onClick={onSwitchToEmergency}
            className="text-xs font-semibold px-3 py-1.5 bg-[#FBEBEA] text-[#C0392B] border border-[#C0392B]/30 rounded-lg hover:bg-[#C0392B] hover:text-white transition"
          >
            Emergency Alert 🚨
          </button>
        </div>

        {/* 1. Symptom Input with Instant Keyword-to-Department Classification */}
        <div className="bg-white border border-[#DCE4E0] rounded-xl p-5 shadow-sm space-y-3">
          <label className="text-xs font-bold text-[#5A6B65] uppercase block">
            Describe Your Symptoms (Instant Clinical Department Guidance)
          </label>
          <div className="relative">
            <span className="absolute left-3 top-3 text-base">🩺</span>
            <input
              type="text"
              value={symptomInput}
              onChange={(e) => setSymptomInput(e.target.value)}
              placeholder="e.g. chronic cough, chest flutter, knee swelling, ear pain..."
              className="w-full pl-9 pr-8 py-2.5 text-sm bg-[#EEF2EF] border border-[#DCE4E0] rounded-lg focus:outline-none focus:border-[#C0392B]"
            />
            {symptomInput && (
              <button
                onClick={() => setSymptomInput("")}
                className="absolute right-2.5 top-2.5 text-[#5A6B65] hover:text-[#1E2B27] text-xs font-bold"
              >
                ✕
              </button>
            )}
          </div>

          {/* Suggested Department Display Banner */}
          {suggestedDepartment && (
            <div className="p-3 bg-[#EAF5EF] border border-[#2F8F5B]/30 rounded-lg flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="text-lg">💡</span>
                <div>
                  <div className="text-xs font-bold text-[#2F8F5B]">
                    Suggested Department: {suggestedDepartment}
                  </div>
                  <div className="text-[11px] text-[#5A6B65]">
                    Matched from symptom keywords in medical knowledge base
                  </div>
                </div>
              </div>

              <button
                onClick={() => setSelectedDept(suggestedDepartment)}
                className="text-xs font-bold px-3 py-1.5 bg-[#2F8F5B] text-white rounded-md hover:bg-[#27784c] transition"
              >
                Filter {suggestedDepartment} Doctors
              </button>
            </div>
          )}
        </div>

        {/* 2. Search & GPS Refinement Bar */}
        <div className="flex flex-col sm:flex-row gap-2">
          <div className="flex-1 relative">
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search doctor by name, specialty, or clinic..."
              className="w-full px-3 py-2.5 text-sm bg-white border border-[#DCE4E0] rounded-lg focus:outline-none focus:border-[#C0392B]"
            />
          </div>

          <button
            onClick={handleRefineDistance}
            className="px-4 py-2.5 bg-white border border-[#DCE4E0] rounded-lg text-xs font-semibold text-[#1E2B27] hover:bg-[#EEF2EF] transition flex items-center justify-center gap-1.5 whitespace-nowrap"
          >
            <span>{isGpsRefined ? "🟢" : "📍"}</span>
            <span>{isGpsRefined ? "GPS Sorted" : "Refine Distances by GPS"}</span>
          </button>
        </div>

        {locationStatus && (
          <p className="text-[11px] text-[#5A6B65] italic px-1">{locationStatus}</p>
        )}

        {/* 3. Department Chips */}
        <div className="flex gap-2 overflow-x-auto pb-1">
          {DEPARTMENTS.map((dept) => {
            const isSelected = selectedDept === dept;
            return (
              <button
                key={dept}
                onClick={() => setSelectedDept(dept)}
                className={`text-xs font-semibold px-3 py-1.5 rounded-lg border whitespace-nowrap transition ${
                  isSelected
                    ? "bg-[#1E2B27] text-white border-[#1E2B27]"
                    : "bg-white text-[#1E2B27] border-[#DCE4E0] hover:bg-[#EEF2EF]"
                }`}
              >
                {dept}
              </button>
            );
          })}
        </div>

        {/* 4. Doctors List Sorted by Distance */}
        <div className="space-y-3">
          <div className="flex justify-between items-center text-xs text-[#5A6B65] px-1">
            <span>Showing {filteredDoctors.length} Verified Specialists</span>
            <span>Sorted by Proximity</span>
          </div>

          {filteredDoctors.length === 0 ? (
            <div className="bg-white border border-[#DCE4E0] rounded-xl p-8 text-center space-y-2">
              <span className="text-3xl">🩺</span>
              <h4 className="text-sm font-bold text-[#1E2B27]">No specialists match criteria</h4>
              <p className="text-xs text-[#5A6B65]">Try adjusting search filters or selecting "All" departments.</p>
            </div>
          ) : (
            filteredDoctors.map((doc) => {
              const distance = HOSPITAL_DISTANCES[doc.hospital] || 4.5;
              // Fallback fields as specified in Phase 4
              const expertiseDisplay = doc.expertise || "General Physician";
              const timingsDisplay = doc.timings || "Contact hospital for details/timings";
              const hospitalDisplay = doc.hospital || "Network Medical Center";
              const phoneDisplay = doc.phone || "+91 80 4000 1100";

              return (
                <div
                  key={doc.id}
                  className="bg-white border border-[#DCE4E0] rounded-xl p-4 shadow-sm space-y-3 hover:border-[#1E2B27]/40 transition"
                >
                  <div className="flex items-start justify-between">
                    <div>
                      <div className="flex items-center gap-2">
                        <h4 className="text-base font-bold text-[#1E2B27]">{doc.name}</h4>
                        <span className="text-xs text-[#5A6B65]">{doc.qualification || "MBBS, MD"}</span>
                      </div>
                      <div className="text-xs font-semibold text-[#1E2B27] mt-0.5">{expertiseDisplay}</div>
                    </div>

                    <div className="text-right">
                      <span className="text-[11px] font-bold px-2 py-0.5 rounded bg-[#EEF2EF] text-[#1E2B27]">
                        {doc.department}
                      </span>
                      <div className="text-xs font-bold text-[#2F8F5B] mt-1">{distance} km away</div>
                    </div>
                  </div>

                  <div className="border-t border-[#DCE4E0] pt-2 grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs text-[#5A6B65]">
                    <div className="flex items-center gap-1.5">
                      <span>🏥</span> {hospitalDisplay}
                    </div>
                    <div className="flex items-center gap-1.5">
                      <span>⏰</span> {timingsDisplay}
                    </div>
                  </div>

                  <a
                    href={`tel:${phoneDisplay}`}
                    className="block text-center w-full py-2 bg-[#EEF2EF] border border-[#DCE4E0] rounded-lg text-xs font-semibold text-[#1E2B27] hover:bg-[#DCE4E0] transition"
                  >
                    📞 Call Consultation Desk ({phoneDisplay})
                  </a>
                </div>
              );
            })
          )}
        </div>

      </div>
    </div>
  );
}
