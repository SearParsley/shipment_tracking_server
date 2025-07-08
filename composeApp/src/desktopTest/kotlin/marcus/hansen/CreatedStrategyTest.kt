package marcus.hansen

// CreatedStrategyTest.kt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CreatedStrategyTest {

    // Mock observer to verify notification
    private class MockShipmentObserver : ShipmentObserver {
        var updateCalled = false
        var receivedShipment: Shipment? = null
        override fun update(shipment: Shipment) {
            updateCalled = true
            receivedShipment = shipment
        }
    }

    @Test
    fun `update should set status to Created and add update to history`() {
        val shipmentId = "SHIP_NEW_001"
        val shipment = Shipment(shipmentId) // Create a new Shipment instance
        val updateData = ShippingUpdate.fromString("created,$shipmentId,1678886400000")

        val strategy = CreatedStrategy()

        // Before update, status should be "Unknown" and history empty
        assertEquals("Unknown", shipment.status)
        assertTrue(shipment.getImmutableUpdateHistory().isEmpty())

        strategy.update(shipment, updateData)

        // Verify status is "Created"
        assertEquals("Created", shipment.status)

        // Verify update is added to history
        assertEquals(1, shipment.getImmutableUpdateHistory().size)
        assertEquals(updateData, shipment.getImmutableUpdateHistory()[0])
    }

    @Test
    fun `update should notify observers after setting status and adding update`() {
        val shipmentId = "SHIP_NEW_002"
        val shipment = Shipment(shipmentId)
        val updateData = ShippingUpdate.fromString("created,$shipmentId,1678886400000")
        val mockObserver = MockShipmentObserver()

        shipment.addObserver(mockObserver) // Register the mock observer

        val strategy = CreatedStrategy()
        strategy.update(shipment, updateData)

        // Verify that the observer was notified
        assertTrue(mockObserver.updateCalled)
        assertNotNull(mockObserver.receivedShipment)
        assertEquals(shipmentId, mockObserver.receivedShipment?.id)
        assertEquals("Created", mockObserver.receivedShipment?.status) // Observer should see the updated status
        assertEquals(1, mockObserver.receivedShipment?.getImmutableUpdateHistory()?.size) // Observer should see the added update
    }

    @Test
    fun `update should handle otherInfo being empty for created update`() {
        val shipmentId = "SHIP_NEW_003"
        val shipment = Shipment(shipmentId)
        // Ensure the fromString method correctly produces empty otherInfo for 'created'
        val updateData = ShippingUpdate.fromString("created,$shipmentId,1678886400000")

        val strategy = CreatedStrategy()
        strategy.update(shipment, updateData)

        assertTrue(updateData.otherInfo.isEmpty()) // Confirm the update data itself has no otherInfo
        // No other properties should be affected by otherInfo for a 'created' update
        assertNull(shipment.currentLocation)
        assertNull(shipment.expectedDeliveryDateTimestamp)
    }
}