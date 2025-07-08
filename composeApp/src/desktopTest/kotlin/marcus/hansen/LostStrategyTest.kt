package marcus.hansen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

class LostStrategyTest {

    @Test
    fun `update should set status to Lost and add update to history`() {
        val shipmentId = "SHIP_LOST_001"
        val shipment = Shipment(shipmentId)
        shipment.status = "In Transit"
        val updateData = ShippingUpdate.fromString("lost,$shipmentId,1678886400000")

        val strategy = LostStrategy()
        strategy.update(shipment, updateData)

        assertEquals("Lost", shipment.status)
        assertEquals(1, shipment.getImmutableUpdateHistory().size)
        assertEquals(updateData, shipment.getImmutableUpdateHistory()[0])
    }

    @Test
    fun `update should notify observers after setting status`() {
        val shipmentId = "SHIP_LOST_002"
        val shipment = Shipment(shipmentId)
        shipment.status = "Delayed"
        val updateData = ShippingUpdate.fromString("lost,$shipmentId,1678886400000")
        val mockObserver = MockShipmentObserver()
        shipment.addObserver(mockObserver)

        val strategy = LostStrategy()
        strategy.update(shipment, updateData)

        assertTrue(mockObserver.updateCalled)
        assertNotNull(mockObserver.receivedShipment)
        assertEquals(shipmentId, mockObserver.receivedShipment?.id)
        assertEquals("Lost", mockObserver.receivedShipment?.status)
    }
}