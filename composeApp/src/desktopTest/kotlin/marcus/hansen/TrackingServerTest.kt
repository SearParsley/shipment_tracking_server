package marcus.hansen

import java.time.Instant
import kotlin.test.*

class TrackingServerTest {

    @org.junit.jupiter.api.BeforeEach
    fun setup() {
//        TrackingServer.resetForTesting() // good practice for testing, but not needed in this implementation
    }

    @Test
    fun `processUpdateString should correctly create a StandardShipment on 'created' update without type`() {
        val shipmentId = "TS_001"
        val updateString = "created,$shipmentId,1678886400000" // No type specified, should default

        val response = TrackingServer.processUpdateString(updateString)

        assertEquals("Success: Update processed for $shipmentId.", response)
        val shipment = TrackingServer.findShipment(shipmentId)
        assertNotNull(shipment, "Shipment $shipmentId should be found")
        assertEquals(ShipmentType.STANDARD, shipment.type, "Shipment type should default to STANDARD")
        assertTrue(shipment is StandardShipment, "Shipment should be an instance of StandardShipment")
        assertEquals("Created", shipment.status)
        assertEquals(
            1,
            shipment.getImmutableUpdateHistory().size
        )
        assertTrue(shipment.ruleViolations.isEmpty(), "No rule violations expected for standard creation")
    }

    @Test
    fun `processUpdateString should correctly create an ExpressShipment on 'created' update with type`() {
        val shipmentId = "TS_002"
        val updateString = "created,$shipmentId,1678886400000,EXPRESS" // Type specified

        val response = TrackingServer.processUpdateString(updateString)

        assertEquals("Success: Update processed for $shipmentId.", response)
        val shipment = TrackingServer.findShipment(shipmentId)
        assertNotNull(shipment)
        assertEquals(ShipmentType.EXPRESS, shipment.type, "Shipment type should be EXPRESS")
        assertTrue(shipment is ExpressShipment, "Shipment should be an instance of ExpressShipment")
        assertEquals("Created", shipment.status)
        assertEquals(1, shipment.getImmutableUpdateHistory().size)
        assertTrue(shipment.ruleViolations.isEmpty(), "No rule violations expected on creation")
    }

    @Test
    fun `processUpdateString should correctly create an OvernightShipment on 'created' update with type`() {
        val shipmentId = "TS_003"
        val updateString = "created,$shipmentId,1678886400000,OVERNIGHT" // Type specified

        val response = TrackingServer.processUpdateString(updateString)

        assertEquals("Success: Update processed for $shipmentId.", response)
        val shipment = TrackingServer.findShipment(shipmentId)
        assertNotNull(shipment)
        assertEquals(ShipmentType.OVERNIGHT, shipment.type, "Shipment type should be OVERNIGHT")
        assertTrue(shipment is OvernightShipment, "Shipment should be an instance of OvernightShipment")
        assertEquals("Created", shipment.status)
        assertEquals(1, shipment.getImmutableUpdateHistory().size)
        assertTrue(shipment.ruleViolations.isEmpty(), "No rule violations expected on creation")
    }

    @Test
    fun `processUpdateString should correctly create a BulkShipment on 'created' update with type`() {
        val shipmentId = "TS_004"
        val updateString = "created,$shipmentId,1678886400000,BULK" // Type specified

        val response = TrackingServer.processUpdateString(updateString)

        assertEquals("Success: Update processed for $shipmentId.", response)
        val shipment = TrackingServer.findShipment(shipmentId)
        assertNotNull(shipment)
        assertEquals(ShipmentType.BULK, shipment.type, "Shipment type should be BULK")
        assertTrue(shipment is BulkShipment, "Shipment should be an instance of BulkShipment")
        assertEquals("Created", shipment.status)
        assertEquals(1, shipment.getImmutableUpdateHistory().size)
        assertTrue(shipment.ruleViolations.isEmpty(), "No rule violations expected on creation")
    }

    @Test
    fun `processUpdateString should correctly process a subsequent update on a created shipment`() {
        val shipmentId = "TS_006"
        // First, create the shipment
        TrackingServer.processUpdateString("created,$shipmentId,1678886400000,STANDARD")
        val initialShipment = TrackingServer.findShipment(shipmentId)
        assertNotNull(initialShipment)
        assertEquals("Created", initialShipment.status)

        // Now, process a subsequent update
        val shippedTimestamp = 1678886500000L
        val expectedDelivery = 1678900000000L
        val shippedUpdateString = "shipped,$shipmentId,$shippedTimestamp,$expectedDelivery"
        val response = TrackingServer.processUpdateString(shippedUpdateString)

        assertEquals("Success: Update processed for $shipmentId.", response)
        val updatedShipment = TrackingServer.findShipment(shipmentId)
        assertNotNull(updatedShipment)
        assertEquals("Shipped", updatedShipment.status)
        assertEquals(expectedDelivery, updatedShipment.expectedDeliveryDateTimestamp)
        assertEquals(2, updatedShipment.getImmutableUpdateHistory().size) // Created + Shipped update
        assertTrue(updatedShipment.ruleViolations.isEmpty(), "No rule violations expected for standard shipment")
    }

    // --- Rule Validation Tests ---

    @Test
    fun `ShippedStrategy should add violation for ExpressShipment delivered more than 3 days after creation`() {
        val shipmentId = "EXPRESS_VIOLATION_01"
        val creationTime = Instant.parse("2023-01-01T10:00:00Z").toEpochMilli()
        val shippedTime = Instant.parse("2023-01-01T11:00:00Z").toEpochMilli()
        val expectedDeliveryTime = Instant.parse("2023-01-05T10:00:00Z").toEpochMilli() // 4 days after creation

        TrackingServer.processUpdateString("created,$shipmentId,$creationTime,EXPRESS")
        val response = TrackingServer.processUpdateString("shipped,$shipmentId,$shippedTime,$expectedDeliveryTime")

        assertEquals("Success: Update processed for $shipmentId.", response)
        val shipment = TrackingServer.findShipment(shipmentId)
        assertNotNull(shipment)
        assertEquals("Shipped", shipment.status)
        assertEquals(1, shipment.ruleViolations.size, "Should have one rule violation")
        assertTrue(shipment.ruleViolations[0].contains("Express shipment delivery date"))
        assertTrue(shipment.ruleViolations[0].contains("is more than 3 days after creation"))
    }

    @Test
    fun `ShippedStrategy should NOT add violation for ExpressShipment delivered within 3 days after creation`() {
        val shipmentId = "EXPRESS_NO_VIOLATION_01"
        val creationTime = Instant.parse("2023-01-01T10:00:00Z").toEpochMilli()
        val shippedTime = Instant.parse("2023-01-01T11:00:00Z").toEpochMilli()
        val expectedDeliveryTime = Instant.parse("2023-01-03T10:00:00Z").toEpochMilli() // 2 days after creation

        TrackingServer.processUpdateString("created,$shipmentId,$creationTime,EXPRESS")
        val response = TrackingServer.processUpdateString("shipped,$shipmentId,$shippedTime,$expectedDeliveryTime")

        assertEquals("Success: Update processed for $shipmentId.", response)
        val shipment = TrackingServer.findShipment(shipmentId)
        assertNotNull(shipment)
        assertEquals("Shipped", shipment.status)
        assertTrue(shipment.ruleViolations.isEmpty(), "Should have no rule violations")
    }

    @Test
    fun `ShippedStrategy should add violation for OvernightShipment not delivered next day`() {
        val shipmentId = "OVERNIGHT_VIOLATION_01"
        val creationTime = Instant.parse("2023-01-01T10:00:00Z").toEpochMilli()
        val shippedTime = Instant.parse("2023-01-01T11:00:00Z").toEpochMilli()
        val expectedDeliveryTime = Instant.parse("2023-01-03T10:00:00Z").toEpochMilli() // 2 days after creation

        TrackingServer.processUpdateString("created,$shipmentId,$creationTime,OVERNIGHT")
        val response = TrackingServer.processUpdateString("shipped,$shipmentId,$shippedTime,$expectedDeliveryTime")

        assertEquals("Success: Update processed for $shipmentId.", response)
        val shipment = TrackingServer.findShipment(shipmentId)
        assertNotNull(shipment)
        assertEquals("Shipped", shipment.status)
        assertEquals(1, shipment.ruleViolations.size, "Should have one rule violation")
        assertTrue(shipment.ruleViolations[0].contains("Overnight shipment delivery date"))
        assertTrue(shipment.ruleViolations[0].contains("is not exactly 1 day after creation"))
    }

    @Test
    fun `ShippedStrategy should NOT add violation for OvernightShipment delivered next day`() {
        val shipmentId = "OVERNIGHT_NO_VIOLATION_01"
        val creationTime = Instant.parse("2023-01-01T10:00:00Z").toEpochMilli()
        val shippedTime = Instant.parse("2023-01-01T11:00:00Z").toEpochMilli()
        val expectedDeliveryTime = Instant.parse("2023-01-02T10:00:00Z").toEpochMilli() // 1 day after creation

        TrackingServer.processUpdateString("created,$shipmentId,$creationTime,OVERNIGHT")
        val response = TrackingServer.processUpdateString("shipped,$shipmentId,$shippedTime,$expectedDeliveryTime")

        assertEquals("Success: Update processed for $shipmentId.", response)
        val shipment = TrackingServer.findShipment(shipmentId)
        assertNotNull(shipment)
        assertEquals("Shipped", shipment.status)
        assertTrue(shipment.ruleViolations.isEmpty(), "Should have no rule violations")
    }

    @Test
    fun `ShippedStrategy should add violation for BulkShipment delivered sooner than 3 days`() {
        val shipmentId = "BULK_VIOLATION_01"
        val creationTime = Instant.parse("2023-01-01T10:00:00Z").toEpochMilli()
        val shippedTime = Instant.parse("2023-01-01T11:00:00Z").toEpochMilli()
        val expectedDeliveryTime = Instant.parse("2023-01-02T10:00:00Z").toEpochMilli() // 1 day after creation

        TrackingServer.processUpdateString("created,$shipmentId,$creationTime,BULK")
        val response = TrackingServer.processUpdateString("shipped,$shipmentId,$shippedTime,$expectedDeliveryTime")

        assertEquals("Success: Update processed for $shipmentId.", response)
        val shipment = TrackingServer.findShipment(shipmentId)
        assertNotNull(shipment)
        assertEquals("Shipped", shipment.status)
        assertEquals(1, shipment.ruleViolations.size, "Should have one rule violation")
        assertTrue(shipment.ruleViolations[0].contains("Bulk shipment delivery date"))
        assertTrue(shipment.ruleViolations[0].contains("is sooner than 3 days after creation"))
    }

    @Test
    fun `ShippedStrategy should NOT add violation for BulkShipment delivered 3 or more days later`() {
        val shipmentId = "BULK_NO_VIOLATION_01"
        val creationTime = Instant.parse("2023-01-01T10:00:00Z").toEpochMilli()
        val shippedTime = Instant.parse("2023-01-01T11:00:00Z").toEpochMilli()
        val expectedDeliveryTime = Instant.parse("2023-01-04T10:00:00Z").toEpochMilli() // 3 days after creation

        TrackingServer.processUpdateString("created,$shipmentId,$creationTime,BULK")
        val response = TrackingServer.processUpdateString("shipped,$shipmentId,$shippedTime,$expectedDeliveryTime")

        assertEquals("Success: Update processed for $shipmentId.", response)
        val shipment = TrackingServer.findShipment(shipmentId)
        assertNotNull(shipment)
        assertEquals("Shipped", shipment.status)
        assertTrue(shipment.ruleViolations.isEmpty(), "Should have no rule violations")
    }

    @Test
    fun `DelayedStrategy should NOT add violation for OvernightShipment when delayed beyond 1 day`() {
        val shipmentId = "OVERNIGHT_DELAYED_NO_VIOLATION_01"
        val creationTime = Instant.parse("2023-01-01T10:00:00Z").toEpochMilli()
        val initialShippedTime = Instant.parse("2023-01-02T10:00:00Z").toEpochMilli() // 1 day after
        val delayedTime = Instant.parse("2023-01-02T11:00:00Z").toEpochMilli()
        val newExpectedDeliveryTime = Instant.parse("2023-01-05T10:00:00Z").toEpochMilli() // 4 days after creation

        TrackingServer.processUpdateString("created,$shipmentId,$creationTime,OVERNIGHT")
        TrackingServer.processUpdateString("shipped,$shipmentId,$initialShippedTime,$initialShippedTime") // Initial ship to set status
        val response = TrackingServer.processUpdateString("delayed,$shipmentId,$delayedTime,$newExpectedDeliveryTime")

        assertEquals("Success: Update processed for $shipmentId.", response)
        val shipment = TrackingServer.findShipment(shipmentId)
        assertNotNull(shipment)
        assertEquals("Delayed", shipment.status)
        // The key assertion: no violation for the '1 day' rule because it's a 'delayed' update
        assertTrue(shipment.ruleViolations.isEmpty(), "Should have no rule violations for delayed overnight")
    }

    @Test
    fun `Rule violations should be cleared before new validation`() {
        val shipmentId = "CLEAR_VIOLATIONS_01"
        val creationTime = Instant.parse("2023-01-01T10:00:00Z").toEpochMilli()
        val shippedTime = Instant.parse("2023-01-01T11:00:00Z").toEpochMilli()

        // Create a violation
        val initialExpectedDeliveryTime = Instant.parse("2023-01-05T10:00:00Z").toEpochMilli() // 4 days after creation
        TrackingServer.processUpdateString("created,$shipmentId,$creationTime,EXPRESS")
        TrackingServer.processUpdateString("shipped,$shipmentId,$shippedTime,$initialExpectedDeliveryTime")
        var shipment = TrackingServer.findShipment(shipmentId)
        assertNotNull(shipment)
        assertEquals(1, shipment.ruleViolations.size, "Should have initial violation")

        // Now, update with a valid delivery date
        val validExpectedDeliveryTime = Instant.parse("2023-01-03T10:00:00Z").toEpochMilli() // 2 days after creation
        TrackingServer.processUpdateString("shipped,$shipmentId,$shippedTime,$validExpectedDeliveryTime") // Re-ship with valid date
        shipment = TrackingServer.findShipment(shipmentId)
        assertNotNull(shipment)
        assertTrue(shipment.ruleViolations.isEmpty(), "Violations should be cleared after valid update")
    }

    @Test
    fun `processUpdateString should return error for invalid shipment type string`() {
        val shipmentId = "TS_004"
        val updateString = "created,$shipmentId,1678886400000,INVALID_TYPE"

        val response = TrackingServer.processUpdateString(updateString)

        assertTrue(response.contains("Error processing update"))
        assertTrue(response.contains("Invalid shipment type string: INVALID_TYPE"))
        assertNull(TrackingServer.findShipment(shipmentId), "Shipment should not be created with invalid type")
    }

    @Test
    fun `processUpdateString should return error for non-existent shipment on non-created update`() {
        val shipmentId = "NON_EXISTENT_SHIP"
        val updateString = "shipped,$shipmentId,1678886400000,1679000000000"

        val response = TrackingServer.processUpdateString(updateString)

        assertTrue(response.contains("Error: Shipment $shipmentId not found."))
        assertNull(TrackingServer.findShipment(shipmentId))
    }

    @Test
    fun `processUpdateString should handle malformed lines gracefully`() {
        val malformedString = "malformed_line_missing_parts"
        val response = TrackingServer.processUpdateString(malformedString)
        assertTrue(response.contains("Error processing update"))
        assertTrue(response.contains("Invalid update line format"))
    }
}