package marcus.hansen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

class CancelledStrategyTest {

    @Test
    fun `update should set status to Canceled and add update to history`() {
        val shipmentId = "SHIP_CANCEL_001"
        val shipment = Shipment(shipmentId)
        shipment.status = "Created" // Set initial status for context
        val updateData = ShippingUpdate.fromString("canceled,$shipmentId,1678886400000")

        val strategy = CancelledStrategy()
        strategy.update(shipment, updateData)

        assertEquals("Canceled", shipment.status)
        assertEquals(1, shipment.getImmutableUpdateHistory().size)
        assertEquals(updateData, shipment.getImmutableUpdateHistory()[0])
    }

    @Test
    fun `update should notify observers after setting status`() {
        val shipmentId = "SHIP_CANCEL_002"
        val shipment = Shipment(shipmentId)
        shipment.status = "Shipped"
        val updateData = ShippingUpdate.fromString("canceled,$shipmentId,1678886400000")
        val mockObserver = MockShipmentObserver()
        shipment.addObserver(mockObserver)

        val strategy = CancelledStrategy()
        strategy.update(shipment, updateData)

        assertTrue(mockObserver.updateCalled)
        assertNotNull(mockObserver.receivedShipment)
        assertEquals(shipmentId, mockObserver.receivedShipment?.id)
        assertEquals("Canceled", mockObserver.receivedShipment?.status)
    }
}