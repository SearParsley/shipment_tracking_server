package marcus.hansen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking // For running suspend functions in tests
import java.io.File

class TrackingServerTest {

    @org.junit.jupiter.api.BeforeEach
    fun setup() {
        TrackingServer.resetForTesting()
    }

    @Test
    fun `processUpdateString should correctly create a StandardShipment on 'created' update without type`() {
        val shipmentId = "TS_001"
        val updateString = "created,$shipmentId,1678886400000" // No type specified, should default

        val response = TrackingServer.processUpdateString(updateString)

        assertEquals("Success: Update processed for $shipmentId.", response)
        val shipment = TrackingServer.findShipment(shipmentId)
        assertNotNull(shipment, "Shipment $shipmentId should be found")
        assertEquals(ShipmentType.STANDARD, shipment?.type, "Shipment type should default to STANDARD")
        assertTrue(shipment is StandardShipment, "Shipment should be an instance of StandardShipment")
        assertEquals("Created", shipment?.status)
        assertEquals(1, shipment?.getImmutableUpdateHistory()?.size)
    }

    @Test
    fun `processUpdateString should correctly create an ExpressShipment on 'created' update with type`() {
        val shipmentId = "TS_002"
        val updateString = "created,$shipmentId,1678886400000,EXPRESS" // Type specified

        val response = TrackingServer.processUpdateString(updateString)

        assertEquals("Success: Update processed for $shipmentId.", response)
        val shipment = TrackingServer.findShipment(shipmentId)
        assertNotNull(shipment)
        assertEquals(ShipmentType.EXPRESS, shipment?.type, "Shipment type should be EXPRESS")
        assertTrue(shipment is ExpressShipment, "Shipment should be an instance of ExpressShipment")
        assertEquals("Created", shipment?.status)
        assertEquals(1, shipment?.getImmutableUpdateHistory()?.size)
    }

    @Test
    fun `processUpdateString should correctly process a subsequent update on a created shipment`() {
        val shipmentId = "TS_003"
        // First, create the shipment
        TrackingServer.processUpdateString("created,$shipmentId,1678886400000,STANDARD")
        val initialShipment = TrackingServer.findShipment(shipmentId)
        assertNotNull(initialShipment)
        assertEquals("Created", initialShipment?.status)

        // Now, process a subsequent update
        val shippedTimestamp = 1678886500000L
        val expectedDelivery = 1678900000000L
        val shippedUpdateString = "shipped,$shipmentId,$shippedTimestamp,$expectedDelivery"
        val response = TrackingServer.processUpdateString(shippedUpdateString)

        assertEquals("Success: Update processed for $shipmentId.", response)
        val updatedShipment = TrackingServer.findShipment(shipmentId)
        assertNotNull(updatedShipment)
        assertEquals("Shipped", updatedShipment?.status)
        assertEquals(expectedDelivery, updatedShipment?.expectedDeliveryDateTimestamp)
        assertEquals(2, updatedShipment?.getImmutableUpdateHistory()?.size) // Created + Shipped update
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

    // Add a reset method to TrackingServer for testing purposes
    // This is crucial for singletons to ensure tests don't interfere with each other.
    // Add this to TrackingServer.kt:
    // fun resetForTesting() { shipments.clear() }
}