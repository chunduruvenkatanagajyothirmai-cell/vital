/**
 * Vital - API & Real-time WebSocket Client (Phase 2)
 */

import { io } from "socket.io-client";

const API_BASE_URL = process.env.REACT_APP_API_URL || "http://localhost:5000";

export const socket = io(API_BASE_URL, {
  autoConnect: true,
  transports: ["websocket", "polling"]
});

export async function dispatchEmergencyAlert(payload) {
  const response = await fetch(`${API_BASE_URL}/api/emergency/dispatch`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  });
  if (!response.ok) {
    const errorData = await response.json();
    throw new Error(errorData.error || "Failed to dispatch emergency alert.");
  }
  return await response.json();
}

export async function fetchCaseStatus(caseId) {
  const response = await fetch(`${API_BASE_URL}/api/emergency/case/${caseId}`);
  if (!response.ok) {
    throw new Error("Failed to fetch case status.");
  }
  return await response.json();
}

export async function confirmEmergencyCase(caseId, note) {
  const response = await fetch(`${API_BASE_URL}/api/emergency/case/${caseId}/confirm`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ note })
  });
  return await response.json();
}

export async function timeoutEmergencyCase(caseId, reason) {
  const response = await fetch(`${API_BASE_URL}/api/emergency/case/${caseId}/timeout`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ reason })
  });
  return await response.json();
}

export async function fetchDoctors(department = "", query = "") {
  const params = new URLSearchParams();
  if (department && department !== "All") params.append("department", department);
  if (query) params.append("query", query);

  const response = await fetch(`${API_BASE_URL}/api/doctors?${params.toString()}`);
  return await response.json();
}

export async function fetchBloodBanks() {
  const response = await fetch(`${API_BASE_URL}/api/blood-banks`);
  return await response.json();
}

export async function checkBloodAvailability(bloodGroup, unitsNeeded = 1) {
  const data = await fetchBloodBanks();
  const banks = data.blood_banks || [];
  
  // Find nearest bank with adequate stock
  banks.sort((a, b) => a.distance_km - b.distance_km);
  const found = banks.find(b => (b.stock[bloodGroup] || 0) >= unitsNeeded);

  if (found) {
    return {
      available: true,
      message: `${bloodGroup} (${unitsNeeded} unit${unitsNeeded > 1 ? "s" : ""} available at ${found.name}, ${found.distance_km} km away)`,
      bank: found
    };
  } else {
    return {
      available: false,
      message: `${bloodGroup} (${unitsNeeded} unit${unitsNeeded > 1 ? "s" : ""} critical shortage locally — alerting regional storage network)`
    };
  }
}
