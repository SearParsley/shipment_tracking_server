package marcus.hansen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import java.time.Instant
import java.time.Duration
import java.time.ZoneId // Ensure this is imported

// A dummy strategy to access the default performRuleValidation method for testing
private class DummyUpdateStrategy : UpdateStrategy {
    override fun update(shipment: Shipment, update: ShippingUpdate) {
        // Not relevant for these tests, as we're testing performRuleValidation directly
    }
}

class RuleValidationTest {

    // Helper to create a shipment with a specific creation timestamp and type
    private fun createTestShipment(id: String, type: ShipmentType, createdMillis: Long): Shipment {
        // We need to bypass the default System.currentTimeMillis() in Shipment constructor
        // For testing, we can either make createdTimestamp settable (internal set)
        // or create a test-specific Shipment class.
        // Let's make it settable for tests for simplicity.
        // Add 'internal set' to 'createdTimestamp' in Shipment.kt for testing purposes.
        val shipment = Shipment(id, type, createdMillis)
        return shipment
    }

    // --- Express Shipment Rules ---

    @Test
    fun `ExpressShipment should have violation if delivery is more than 3 days after creation`() {
        val strategy = DummyUpdateStrategy()
        val creationTime = Instant.parse("2023-01-01T10:00:00Z").toEpochMilli()
        val expectedDeliveryTime = Instant.parse("2023-01-05T09:00:00Z").toEpochMilli() // 4 days later (3 days + almost 24h)
        val shipment = createTestShipment("EXPRESS_V_01", ShipmentType.EXPRESS, creationTime)

        strategy.performRuleValidation(shipment, expectedDeliveryTime, "shipped")

        assertEquals(1, shipment.ruleViolations.size)
        assertTrue(shipment.ruleViolations[0].contains("Express shipment delivery date") &&
                shipment.ruleViolations[0].contains("is more than 3 days after creation"))
    }

    @Test
    fun `ExpressShipment should NOT have violation if delivery is exactly 3 days after creation`() {
        val strategy = DummyUpdateStrategy()
        val creationTime = Instant.parse("2023-01-01T10:00:00Z").toEpochMilli()
        val expectedDeliveryTime = Instant.parse("2023-01-04T10:00:00Z").toEpochMilli() // Exactly 3 days later
        val shipment = createTestShipment("EXPRESS_NV_01", ShipmentType.EXPRESS, creationTime)

        strategy.performRuleValidation(shipment, expectedDeliveryTime, "shipped")

        assertTrue(shipment.ruleViolations.isEmpty())
    }

    @Test
    fun `ExpressShipment should NOT have violation if delivery is less than 3 days after creation`() {
        val strategy = DummyUpdateStrategy()
        val creationTime = Instant.parse("2023-01-01T10:00:00Z").toEpochMilli()
        val expectedDeliveryTime = Instant.parse("2023-01-03T09:00:00Z").toEpochMilli() // 2 days later (1 day + almost 24h)
        val shipment = createTestShipment("EXPRESS_NV_02", ShipmentType.EXPRESS, creationTime)

        strategy.performRuleValidation(shipment, expectedDeliveryTime, "shipped")

        assertTrue(shipment.ruleViolations.isEmpty())
    }

    // --- Overnight Shipment Rules ---

    @Test
    fun `OvernightShipment should have violation if delivery is not exactly 1 day after creation (shipped)`() {
        val strategy = DummyUpdateStrategy()
        val creationTime = Instant.parse("2023-01-01T10:00:00Z").toEpochMilli()
        val expectedDeliveryTime = Instant.parse("2023-01-03T10:00:00Z").toEpochMilli() // 2 days later
        val shipment = createTestShipment("OVERNIGHT_V_01", ShipmentType.OVERNIGHT, creationTime)

        strategy.performRuleValidation(shipment, expectedDeliveryTime, "shipped")

        assertEquals(1, shipment.ruleViolations.size)
        assertTrue(shipment.ruleViolations[0].contains("Overnight shipment delivery date") &&
                shipment.ruleViolations[0].contains("is not exactly 1 day after creation"))
    }

    @Test
    fun `OvernightShipment should NOT have violation if delivery is exactly 1 day after creation`() {
        val strategy = DummyUpdateStrategy()
        val creationTime = Instant.parse("2023-01-01T10:00:00Z").toEpochMilli()
        val expectedDeliveryTime = Instant.parse("2023-01-02T10:00:00Z").toEpochMilli() // Exactly 1 day later
        val shipment = createTestShipment("OVERNIGHT_NV_01", ShipmentType.OVERNIGHT, creationTime)

        strategy.performRuleValidation(shipment, expectedDeliveryTime, "shipped")

        assertTrue(shipment.ruleViolations.isEmpty())
    }

    @Test
    fun `OvernightShipment should NOT have violation if updateType is 'delayed' even if not 1 day`() {
        val strategy = DummyUpdateStrategy()
        val creationTime = Instant.parse("2023-01-01T10:00:00Z").toEpochMilli()
        val expectedDeliveryTime = Instant.parse("2023-01-05T10:00:00Z").toEpochMilli() // 4 days later
        val shipment = createTestShipment("OVERNIGHT_NV_02", ShipmentType.OVERNIGHT, creationTime)

        strategy.performRuleValidation(shipment, expectedDeliveryTime, "delayed") // updateType is 'delayed'

        assertTrue(shipment.ruleViolations.isEmpty()) // No violation message should be added
    }

    // --- Bulk Shipment Rules ---

    @Test
    fun `BulkShipment should have violation if delivery is sooner than 3 days after creation`() {
        val strategy = DummyUpdateStrategy()
        val creationTime = Instant.parse("2023-01-01T10:00:00Z").toEpochMilli()
        val expectedDeliveryTime = Instant.parse("2023-01-03T09:00:00Z").toEpochMilli() // 2 days later (1 day + almost 24h)
        val shipment = createTestShipment("BULK_V_01", ShipmentType.BULK, creationTime)

        strategy.performRuleValidation(shipment, expectedDeliveryTime, "shipped")

        assertEquals(1, shipment.ruleViolations.size)
        assertTrue(shipment.ruleViolations[0].contains("Bulk shipment delivery date") &&
                shipment.ruleViolations[0].contains("is sooner than 3 days after creation"))
    }

    @Test
    fun `BulkShipment should NOT have violation if delivery is exactly 3 days after creation`() {
        val strategy = DummyUpdateStrategy()
        val creationTime = Instant.parse("2023-01-01T10:00:00Z").toEpochMilli()
        val expectedDeliveryTime = Instant.parse("2023-01-04T10:00:00Z").toEpochMilli() // Exactly 3 days later
        val shipment = createTestShipment("BULK_NV_01", ShipmentType.BULK, creationTime)

        strategy.performRuleValidation(shipment, expectedDeliveryTime, "shipped")

        assertTrue(shipment.ruleViolations.isEmpty())
    }

    @Test
    fun `BulkShipment should NOT have violation if delivery is more than 3 days after creation`() {
        val strategy = DummyUpdateStrategy()
        val creationTime = Instant.parse("2023-01-01T10:00:00Z").toEpochMilli()
        val expectedDeliveryTime = Instant.parse("2023-01-05T10:00:00Z").toEpochMilli() // 4 days later
        val shipment = createTestShipment("BULK_NV_02", ShipmentType.BULK, creationTime)

        strategy.performRuleValidation(shipment, expectedDeliveryTime, "shipped")

        assertTrue(shipment.ruleViolations.isEmpty())
    }

    // --- General Validation Behavior ---

    @Test
    fun `performRuleValidation should clear previous violations`() {
        val strategy = DummyUpdateStrategy()
        val creationTime = Instant.parse("2023-01-01T10:00:00Z").toEpochMilli()
        val shipment = createTestShipment("CLEAR_V_01", ShipmentType.EXPRESS, creationTime)
        shipment.ruleViolations.add("Old violation message") // Add a fake violation

        val expectedDeliveryTime = Instant.parse("2023-01-02T10:00:00Z").toEpochMilli() // Valid delivery
        strategy.performRuleValidation(shipment, expectedDeliveryTime, "shipped")

        assertTrue(shipment.ruleViolations.isEmpty(), "Rule violations list should be cleared")
    }

    @Test
    fun `StandardShipment should never have rule violations`() {
        val strategy = DummyUpdateStrategy()
        val creationTime = Instant.parse("2023-01-01T10:00:00Z").toEpochMilli()
        val shipment = createTestShipment("STANDARD_NV_01", ShipmentType.STANDARD, creationTime)

        // Test with various durations
        strategy.performRuleValidation(shipment, Instant.parse("2023-01-01T11:00:00Z").toEpochMilli(), "shipped")
        assertTrue(shipment.ruleViolations.isEmpty())
        strategy.performRuleValidation(shipment, Instant.parse("2023-01-05T10:00:00Z").toEpochMilli(), "shipped")
        assertTrue(shipment.ruleViolations.isEmpty())
        strategy.performRuleValidation(shipment, Instant.parse("2023-01-02T10:00:00Z").toEpochMilli(), "delayed")
        assertTrue(shipment.ruleViolations.isEmpty())
    }
}