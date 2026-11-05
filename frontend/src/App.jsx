import React, { useState } from "react";
import PublicEmergencyFlow from "./pages/PublicEmergencyFlow";
import NormalCheckup from "./pages/NormalCheckup";
import HospitalStaffDashboard from "./pages/HospitalStaffDashboard";
import "./styles/theme.css";

export default function App() {
  const [currentPage, setCurrentPage] = useState("EMERGENCY"); // "EMERGENCY" | "CHECKUP" | "STAFF"

  return (
    <div className="min-h-screen bg-[#F4F6F4] text-[#1E2B27]">
      {currentPage === "EMERGENCY" && (
        <PublicEmergencyFlow
          onNavigateToCheckup={() => setCurrentPage("CHECKUP")}
          onStaffLogin={() => setCurrentPage("STAFF")}
        />
      )}

      {currentPage === "CHECKUP" && (
        <NormalCheckup
          onSwitchToEmergency={() => setCurrentPage("EMERGENCY")}
        />
      )}

      {currentPage === "STAFF" && (
        <HospitalStaffDashboard
          onSwitchToBystander={() => setCurrentPage("EMERGENCY")}
        />
      )}
    </div>
  );
}
