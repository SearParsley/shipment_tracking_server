package marcus.hansen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNull
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ShippedStrategyTest {

    @Test
    fun `update should notify Tracker and update TrackerViewHelper correctly`() {
        val shipmentId = "SHIP_SHIPPED_002_TRACKER"
        val creationTime = Instant.parse("2023-01-01T10:00:00Z").toEpochMilli()
        val shipment = Shipment(shipmentId, ShipmentType.STANDARD, creationTime)
        shipment.status = "Created"
        shipment.addUpdate(ShippingUpdate.fromString("created,$shipmentId,$creationTime")) // Add initial update to history

        val expectedDeliveryTimestamp = Instant.parse("2023-01-03T10:00:00Z").toEpochMilli() // 2 days later (valid for Standard)
        val updateData = ShippingUpdate.fromString("shipped,$shipmentId,${Instant.now().toEpochMilli()},$expectedDeliveryTimestamp")

        val viewModel = TrackerViewHelper(shipmentId)
        val tracker = Tracker(shipmentId, viewModel)
        shipment.addObserver(tracker) // Register the actual tracker

        val strategy = ShippedStrategy() // Use the original strategy
        strategy.update(shipment, updateData) // This will call shipment.notifyObservers()

        // Assertions on the TrackerViewHelper
        assertEquals("Shipped", viewModel.shipmentStatus)
        val instant = Instant.ofEpochMilli(expectedDeliveryTimestamp)
        val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        assertEquals(dateTime.format(formatter), viewModel.expectedShipmentDeliveryDate)
        assertEquals(2, viewModel.shipmentUpdateHistory.size) // Created + Shipped
        assertTrue(viewModel.shipmentUpdateHistory[1].contains("Shipped"), "History should contain 'Shipped' update")
        assertTrue(viewModel.ruleViolations.isEmpty(), "No rule violations expected for standard shipment")
    }

    @Test
    fun `update should set status to Shipped and update expected delivery timestamp`() {
        val shipmentId = "SHIP_SHIPPED_001"
        val shipment = Shipment(shipmentId)
        shipment.status = "Created"
        val expectedDeliveryTimestamp = 1679999999000L
        val updateData = ShippingUpdate.fromString("shipped,$shipmentId,1678886400000,$expectedDeliveryTimestamp")

        val strategy = ShippedStrategy()
        strategy.update(shipment, updateData)

        assertEquals("Shipped", shipment.status)
        assertEquals(expectedDeliveryTimestamp, shipment.expectedDeliveryDateTimestamp)
        assertEquals(1, shipment.getImmutableUpdateHistory().size)
        assertEquals(updateData, shipment.getImmutableUpdateHistory()[0])
    }

    @Test
    fun `update should not change expected delivery if otherInfo is missing`() {
        val shipmentId = "SHIP_SHIPPED_003"
        val shipment = Shipment(shipmentId)
        shipment.status = "Created"
        shipment.expectedDeliveryDateTimestamp = 1000L // Pre-existing timestamp
        val updateData = ShippingUpdate.fromString("shipped,$shipmentId,1678886400000") // No otherInfo

        val strategy = ShippedStrategy()
        strategy.update(shipment, updateData)

        assertEquals("Shipped", shipment.status)
        assertEquals(1000L, shipment.expectedDeliveryDateTimestamp) // Should remain unchanged
        assertEquals(1, shipment.getImmutableUpdateHistory().size)
    }

    @Test
    fun `update should log error if expected delivery timestamp is invalid`() {
        val originalErr = System.err
        val bos = ByteArrayOutputStream()
        val ps = PrintStream(bos, true) // 'true' for auto-flush
        System.setErr(ps) // Redirect stderr

        try {
            val shipmentId = "SHIP_SHIPPED_004"
            val shipment = Shipment(shipmentId)
            shipment.status = "Created"
            val updateData = ShippingUpdate.fromString("shipped,$shipmentId,1678886400000,not_a_timestamp")

            val strategy = ShippedStrategy()
            strategy.update(shipment, updateData)

            assertEquals("Shipped", shipment.status)
            assertNull(shipment.expectedDeliveryDateTimestamp)

            ps.close() // CRUCIAL: Close the PrintStream to flush all content

            val loggedOutput = bos.toString()
            assertTrue(loggedOutput.contains("Invalid timestamp format"), "Error message should contain 'Invalid timestamp format'")
            assertTrue(loggedOutput.contains(shipmentId), "Error message should contain shipment ID")
            assertTrue(loggedOutput.contains("not_a_timestamp"), "Error message should contain the invalid timestamp")

        } finally {
            System.setErr(originalErr) // Restore stderr
        }
    }

    // --- RULE VALIDATION TESTS ---

    @Test
    fun `ShippedStrategy should add violation for ExpressShipment if delivery is too far out`() {
        val shipmentId = "EXPRESS_SHIPPED_V_01"
        val creationTime = Instant.parse("2023-01-01T10:00:00Z").toEpochMilli()
        val expectedDeliveryTime = Instant.parse("2023-01-05T10:00:00Z").toEpochMilli() // 4 days later
        val shipment = Shipment(shipmentId, ShipmentType.EXPRESS, creationTime) // Create specific type
        val updateData = ShippingUpdate.fromString("shipped,$shipmentId,${Instant.now().toEpochMilli()},$expectedDeliveryTime")

        val strategy = ShippedStrategy()
        strategy.update(shipment, updateData)

        assertEquals("Shipped", shipment.status)
        assertEquals(1, shipment.ruleViolations.size, "Should have one rule violation")
        assertTrue(shipment.ruleViolations[0].contains("Express shipment delivery date") &&
                shipment.ruleViolations[0].contains("is more than 3 days after creation"))
    }

    @Test
    fun `ShippedStrategy should add violation for OvernightShipment if delivery is not next day`() {
        val shipmentId = "OVERNIGHT_SHIPPED_V_01"
        val creationTime = Instant.parse("2023-01-01T10:00:00Z").toEpochMilli()
        val expectedDeliveryTime = Instant.parse("2023-01-03T10:00:00Z").toEpochMilli() // 2 days later
        val shipment = Shipment(shipmentId, ShipmentType.OVERNIGHT, creationTime)
        val updateData = ShippingUpdate.fromString("shipped,$shipmentId,${Instant.now().toEpochMilli()},$expectedDeliveryTime")

        val strategy = ShippedStrategy()
        strategy.update(shipment, updateData)

        assertEquals("Shipped", shipment.status)
        assertEquals(1, shipment.ruleViolations.size, "Should have one rule violation")
        assertTrue(shipment.ruleViolations[0].contains("Overnight shipment delivery date") &&
                shipment.ruleViolations[0].contains("is not exactly 1 day after creation"))
    }

    @Test
    fun `ShippedStrategy should add violation for BulkShipment if delivery is too soon`() {
        val shipmentId = "BULK_SHIPPED_V_01"
        val creationTime = Instant.parse("2023-01-01T10:00:00Z").toEpochMilli()
        val expectedDeliveryTime = Instant.parse("2023-01-02T10:00:00Z").toEpochMilli() // 1 day later
        val shipment = Shipment(shipmentId, ShipmentType.BULK, creationTime)
        val updateData = ShippingUpdate.fromString("shipped,$shipmentId,${Instant.now().toEpochMilli()},$expectedDeliveryTime")

        val strategy = ShippedStrategy()
        strategy.update(shipment, updateData)

        assertEquals("Shipped", shipment.status)
        assertEquals(1, shipment.ruleViolations.size, "Should have one rule violation")
        assertTrue(shipment.ruleViolations[0].contains("Bulk shipment delivery date") &&
                shipment.ruleViolations[0].contains("is sooner than 3 days after creation"))
    }

    @Test
    fun `ShippedStrategy should clear previous violations before new validation`() {
        val shipmentId = "SHIPPED_CLEAR_V_01"
        val creationTime = Instant.parse("2023-01-01T10:00:00Z").toEpochMilli()
        val shipment = Shipment(shipmentId, ShipmentType.EXPRESS, creationTime)
        shipment.ruleViolations.add("Previous violation") // Add a dummy violation

        val validExpectedDeliveryTime = Instant.parse("2023-01-03T10:00:00Z").toEpochMilli() // Valid (2 days)
        val updateData = ShippingUpdate.fromString("shipped,$shipmentId,${Instant.now().toEpochMilli()},$validExpectedDeliveryTime")

        val strategy = ShippedStrategy()
        strategy.update(shipment, updateData)

        assertTrue(shipment.ruleViolations.isEmpty(), "Violations should be cleared for a valid update")
    }
}