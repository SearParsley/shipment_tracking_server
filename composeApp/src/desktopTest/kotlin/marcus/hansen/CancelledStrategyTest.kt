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
        val viewModel = TrackerViewHelper(shipmentId)
        val tracker = Tracker(shipmentId, viewModel)
        shipment.addObserver(tracker)

        val strategy = CancelledStrategy()
        strategy.update(shipment, updateData)

        assertEquals(shipmentId, viewModel.shipmentId)
        assertEquals("Canceled", viewModel.shipmentStatus)
    }
}