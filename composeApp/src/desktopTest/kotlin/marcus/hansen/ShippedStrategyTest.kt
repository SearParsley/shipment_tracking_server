package marcus.hansen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.time.Instant

class ShippedStrategyTest {

    @Test
    fun `update should set status to Shipped and update expected delivery timestamp`() {
        val shipmentId = "SHIP_SHIPPED_001"
        val shipment = Shipment(shipmentId)
        shipment.status = "Created"
        val expectedDeliveryTimestamp = 1679999999000L
        val update = ShippingUpdate.fromString("shipped,$shipmentId,1678886400000,$expectedDeliveryTimestamp")

        val strategy = ShippedStrategy()
        strategy.update(shipment, update)

        assertEquals("Shipped", shipment.status)
        assertEquals(expectedDeliveryTimestamp, shipment.expectedDeliveryDateTimestamp)
        assertEquals(1, shipment.getImmutableUpdateHistory().size)
        assertEquals(update, shipment.getImmutableUpdateHistory()[0])
    }

    @Test
    fun `update should notify observers after setting status and timestamp`() {
        val shipmentId = "SHIP_SHIPPED_002"
        val shipment = Shipment(shipmentId)
        shipment.status = "Created"
        val expectedDeliveryTimestamp = 1679999999000L
        val update = ShippingUpdate.fromString("shipped,$shipmentId,1678886400000,$expectedDeliveryTimestamp")
        val mockObserver = MockShipmentObserver()
        shipment.addObserver(mockObserver)

        val strategy = ShippedStrategy()
        strategy.update(shipment, update)

        assertTrue(mockObserver.updateCalled)
        assertNotNull(mockObserver.receivedShipment)
        assertEquals(shipmentId, mockObserver.receivedShipment?.id)
        assertEquals("Shipped", mockObserver.receivedShipment?.status)
        assertEquals(expectedDeliveryTimestamp, mockObserver.receivedShipment?.expectedDeliveryDateTimestamp)
    }

    @Test
    fun `update should not change expected delivery if otherInfo is missing`() {
        val shipmentId = "SHIP_SHIPPED_003"
        val shipment = Shipment(shipmentId)
        shipment.status = "Created"
        shipment.expectedDeliveryDateTimestamp = 1000L // Pre-existing timestamp
        val update = ShippingUpdate.fromString("shipped,$shipmentId,1678886400000") // No otherInfo

        val strategy = ShippedStrategy()
        strategy.update(shipment, update)

        assertEquals("Shipped", shipment.status)
        assertEquals(1000L, shipment.expectedDeliveryDateTimestamp) // Should remain unchanged
        assertEquals(1, shipment.getImmutableUpdateHistory().size)
    }

    @Test
    fun `update should log error if expected delivery timestamp is invalid`() {
        val originalErr = System.err
        val bos = ByteArrayOutputStream()
        System.setErr(PrintStream(bos)) // Redirect stderr

        try {
            val shipmentId = "SHIP_SHIPPED_004"
            val shipment = Shipment(shipmentId)
            shipment.status = "Created"
            val update = ShippingUpdate.fromString("shipped,$shipmentId,1678886400000,not_a_timestamp")

            val strategy = ShippedStrategy()
            strategy.update(shipment, update)

            assertEquals("Shipped", shipment.status) // Status should still change
            assertNull(shipment.expectedDeliveryDateTimestamp) // Timestamp should not be set
            assertTrue(bos.toString().contains("Invalid timestamp format"), "Error message should be logged")
        } finally {
            System.setErr(originalErr) // Restore stderr
        }
    }

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