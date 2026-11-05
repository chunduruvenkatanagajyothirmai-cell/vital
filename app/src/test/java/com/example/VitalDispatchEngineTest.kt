package com.example

import com.example.data.model.Hospital
import com.example.data.model.SeverityLevel
import com.example.domain.engine.VitalDispatchEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VitalDispatchEngineTest {

  @Test
  fun testDepartmentClassification() {
    assertEquals("Trauma", VitalDispatchEngine.classifyDepartment("Severe crash with heavy bleeding and unconscious victim"))
    assertEquals("Cardiology", VitalDispatchEngine.classifyDepartment("Patient having crushing chest pain radiating to left arm and sweating"))
    assertEquals("Neurology", VitalDispatchEngine.classifyDepartment("Sudden stroke symptoms, facial droop, and speech loss"))
    assertEquals("Pulmonology", VitalDispatchEngine.classifyDepartment("Severe asthma attack, gasping for air, suffocating"))
    assertEquals("Pediatrics", VitalDispatchEngine.classifyDepartment("Toddler swallowed coin and is crying"))
    assertEquals("ENT", VitalDispatchEngine.classifyDepartment("Severe ear bleeding and foreign object in throat"))
    assertEquals("Orthopedics", VitalDispatchEngine.classifyDepartment("Broken leg bone dislocation after fall from roof"))
    assertEquals("General Medicine", VitalDispatchEngine.classifyDepartment("Mild dizziness and weakness after fever"))
  }

  @Test
  fun testSeverityClassification() {
    val critical = VitalDispatchEngine.classifySeverity("Unconscious patient with heavy bleeding", "Trauma")
    assertEquals(SeverityLevel.CRITICAL, critical)

    val serious = VitalDispatchEngine.classifySeverity("Fracture and bleeding but conscious", "Orthopedics")
    assertEquals(SeverityLevel.SERIOUS, serious)
  }

  @Test
  fun testEtaCalculator() {
    // straight-distance formula: (distance_km / 30) * 60 = distance_km * 2
    assertEquals(6, VitalDispatchEngine.calculateEtaMinutes(3.0))
    assertEquals(10, VitalDispatchEngine.calculateEtaMinutes(5.0))
    assertEquals(1, VitalDispatchEngine.calculateEtaMinutes(0.3)) // Coerced to at least 1 min
  }

  @Test
  fun testClosedLoopQueueBuilder() {
    val hospitals = listOf(
      Hospital("h1", "Hospital Far", "Loc Far", 8.0, "111", listOf("Cardiology")),
      Hospital("h2", "Hospital Near", "Loc Near", 2.0, "222", listOf("Cardiology")),
      Hospital("h3", "Hospital General", "Loc Mid", 4.0, "333", listOf("General Medicine"))
    )

    val queue = VitalDispatchEngine.buildAttemptQueue("Cardiology", hospitals)
    assertEquals(2, queue.size)
    assertEquals("h2", queue[0].hospitalId)
    assertEquals("h1", queue[1].hospitalId)
    assertEquals(4, queue[0].etaMinutes) // 2.0 / 30 * 60 = 4 mins
  }

  @Test
  fun testNearestHospitalFallback() {
    val hospitals = listOf(
      Hospital("h1", "Hosp A", "Loc A", 6.5, "111", listOf("Trauma")),
      Hospital("h2", "Hosp B", "Loc B", 1.8, "222", listOf("Pediatrics")),
      Hospital("h3", "Hosp C", "Loc C", 4.2, "333", listOf("ENT"))
    )
    val nearest = VitalDispatchEngine.getNearestHospital(hospitals)
    assertEquals("h2", nearest.id)
  }
}
