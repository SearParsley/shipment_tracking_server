package marcus.hansen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

class DeliveredStrategyTest {

    @Test
    fun `update should set status to Delivered and add update to history`() {
        val shipmentId = "SHIP_DELIVER_001"
        val shipment = Shipment(shipmentId)
        shipment.status = "In Transit"
        val updateData = ShippingUpdate.fromString("delivered,$shipmentId,1678886400000")

        val strategy = DeliveredStrategy()
        strategy.update(shipment, updateData)

        assertEquals("Delivered", shipment.status)
        assertEquals(1, shipment.getImmutableUpdateHistory().size)
        assertEquals(updateData, shipment.getImmutableUpdateHistory()[0])
    }

    @Test
    fun `update should notify observers after setting status`() {
        val shipmentId = "SHIP_DELIVER_002"
        val shipment = Shipment(shipmentId)
        shipment.status = "Out for Delivery"
        val updateData = ShippingUpdate.fromString("delivered,$shipmentId,1678886400000")
        val viewModel = TrackerViewHelper(shipmentId)
        val tracker = Tracker(shipmentId, viewModel)
        shipment.addObserver(tracker)

        val strategy = DeliveredStrategy()
        strategy.update(shipment, updateData)

        assertEquals(shipmentId, viewModel.shipmentId)
        assertEquals("Delivered", viewModel.shipmentStatus)
    }
}