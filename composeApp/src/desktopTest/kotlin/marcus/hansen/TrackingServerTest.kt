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
    fun `processUpdateString should correctly process a 'created' update`() {
        val shipmentId = "TEST_SHIP_001"
        val updateString = "created,$shipmentId,1678886400000"

        val response = TrackingServer.processUpdateString(updateString)

        assertEquals("Success: Update processed for $shipmentId.", response)
        val shipment = TrackingServer.findShipment(shipmentId)
        assertNotNull(shipment, "Shipment $shipmentId should be found after processing")
        assertEquals("Created", shipment.status, "Shipment status should be 'Created'")
        assertEquals(1, shipment.getImmutableUpdateHistory().size, "Shipment history should contain one update")
    }

    @Test
    fun `processUpdateString should correctly process multiple 'created' updates for different shipments`() {
        val shipmentId1 = "TEST_SHIP_002"
        val shipmentId2 = "TEST_SHIP_003"

        TrackingServer.processUpdateString("created,$shipmentId1,1678886401000")
        TrackingServer.processUpdateString("created,$shipmentId2,1678886402000")

        val shipment1 = TrackingServer.findShipment(shipmentId1)
        assertNotNull(shipment1)
        assertEquals("Created", shipment1.status)

        val shipment2 = TrackingServer.findShipment(shipmentId2)
        assertNotNull(shipment2)
        assertEquals("Created", shipment2.status)
    }

    @Test
    fun `processUpdateString should handle 'created' update for already existing shipment`() {
        val shipmentId = "TEST_SHIP_004"
        TrackingServer.processUpdateString("created,$shipmentId,1678886400000")
        val initialShipment = TrackingServer.findShipment(shipmentId)
        assertNotNull(initialShipment)
        assertEquals(1, initialShipment.getImmutableUpdateHistory().size)

        // Process second 'created' update for same ID
        TrackingServer.processUpdateString("created,$shipmentId,1678886500000")

        val shipment = TrackingServer.findShipment(shipmentId)
        assertNotNull(shipment)
        assertEquals("Created", shipment.status)
        assertEquals(2, shipment.getImmutableUpdateHistory().size, "History should contain both 'created' updates")
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