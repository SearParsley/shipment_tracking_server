package marcus.hansen

import androidx.compose.ui.platform.findComposeDefaultViewModelStoreOwner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

class LocationStrategyTest {

    @Test
    fun `update should set current location and add update to history`() {
        val shipmentId = "SHIP_LOC_001"
        val shipment = Shipment(shipmentId)
        shipment.status = "Shipped"
        val location = "Los Angeles CA"
        val updateData = ShippingUpdate.fromString("location,$shipmentId,1678886400000,$location")

        val strategy = LocationStrategy()
        strategy.update(shipment, updateData)

        assertEquals(location, shipment.currentLocation)
        assertEquals(1, shipment.getImmutableUpdateHistory().size)
        assertEquals(updateData, shipment.getImmutableUpdateHistory()[0])
    }

    @Test
    fun `update should notify observers after setting location`() {
        val shipmentId = "SHIP_LOC_002"
        val shipment = Shipment(shipmentId)
        shipment.status = "In Transit"
        val location = "New York NY"
        val updateData = ShippingUpdate.fromString("location,$shipmentId,1678886400000,$location")
        val viewModel = TrackerViewHelper(shipmentId)
        val tracker = Tracker(shipmentId, viewModel)
        shipment.addObserver(tracker)

        val strategy = LocationStrategy()
        strategy.update(shipment, updateData)

        assertEquals(shipmentId, viewModel.shipmentId)
        assertEquals(location, viewModel.currentLocation)
    }

    @Test
    fun `update should not change location if otherInfo is missing`() {
        val shipmentId = "SHIP_LOC_003"
        val shipment = Shipment(shipmentId)
        shipment.status = "In Transit"
        shipment.currentLocation = "Previous Location" // Pre-existing location
        val updateData = ShippingUpdate.fromString("location,$shipmentId,1678886400000") // No otherInfo

        val strategy = LocationStrategy()
        strategy.update(shipment, updateData)

        assertEquals("Previous Location", shipment.currentLocation) // Should remain unchanged
        assertEquals(1, shipment.getImmutableUpdateHistory().size)
    }
}