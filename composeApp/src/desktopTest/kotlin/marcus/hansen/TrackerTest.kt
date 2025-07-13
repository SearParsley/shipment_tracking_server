package marcus.hansen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.assertContentEquals

class TrackerTest {

    @Test
    fun `Tracker should receive and store initial shipment state upon update`() {
        val shipmentId = "SHIP_TRACK_001"
        val shipment = Shipment(shipmentId)
        shipment.status = "Created"
        shipment.currentLocation = "Warehouse A"
        shipment.expectedDeliveryDateTimestamp = 123456789L
        shipment.addNote("Initial note.")
        val createdUpdate = ShippingUpdate.fromString("created,$shipmentId,1000")
        shipment.addUpdate(createdUpdate)

        val viewModel = TrackerViewHelper(shipmentId)
        val tracker = Tracker(shipmentId, viewModel)
        shipment.addObserver(tracker)

        shipment.notifyObservers()

        // Assert TrackerViewHelper's state
        assertEquals("Created", viewModel.shipmentStatus)
        assertEquals("Warehouse A", viewModel.currentLocation)
        assertEquals("1970-01-02 03:17:36", viewModel.expectedShipmentDeliveryDate) // String format
        assertContentEquals(arrayOf("Initial note."), viewModel.shipmentNotes)
        assertEquals(1, viewModel.shipmentUpdateHistory.size)
        assertTrue(viewModel.shipmentUpdateHistory[0].contains("Created"), "Formatted history should contain 'Created'")
    }

    @Test
    fun `Tracker should update its TrackerViewHelper state when shipment changes`() {
        val shipmentId = "SHIP_TRACK_002"
        val shipment = Shipment(shipmentId)
        val initialCreatedUpdate = ShippingUpdate.fromString("created,$shipmentId,1000")
        shipment.status = "Created"
        shipment.addUpdate(initialCreatedUpdate)

        val viewModel = TrackerViewHelper(shipmentId)
        val tracker = Tracker(shipmentId, viewModel)
        shipment.addObserver(tracker)

        shipment.notifyObservers() // Initial notification
        assertEquals("Created", viewModel.shipmentStatus)
        assertEquals(1, viewModel.shipmentUpdateHistory.size)


        // Simulate a status and location change
        shipment.status = "Shipped"
        shipment.currentLocation = "Shipping Hub"
        val shippedUpdate = ShippingUpdate.fromString("shipped,$shipmentId,2000,3000")
        shipment.addUpdate(shippedUpdate)

        shipment.notifyObservers() // Notify again after changes

        // Assert that the TrackerViewHelper's state is now updated
        assertEquals("Shipped", viewModel.shipmentStatus)
        assertEquals("Shipping Hub", viewModel.currentLocation)
        assertEquals(2, viewModel.shipmentUpdateHistory.size)
        assertTrue(viewModel.shipmentUpdateHistory[1].contains("Shipped"), "Formatted history should contain 'Shipped'")
    }

    @Test
    fun `Tracker should not update its TrackerViewHelper for unrelated shipments`() {
        val shipmentId1 = "SHIP_TRACK_003"
        val shipmentId2 = "SHIP_TRACK_004"
        val shipment1 = Shipment(shipmentId1)
        val initialCreatedUpdate1 = ShippingUpdate.fromString("created,$shipmentId1,1000")
        shipment1.status = "Created"
        shipment1.addUpdate(initialCreatedUpdate1)

        val shipment2 = Shipment(shipmentId2)
        val initialCreatedUpdate2 = ShippingUpdate.fromString("created,$shipmentId2,1000")
        shipment2.status = "Created"
        shipment2.addUpdate(initialCreatedUpdate2)

        val viewModel1 = TrackerViewHelper(shipmentId1) // ViewModel for shipment1
        val trackerForShipment1 = Tracker(shipmentId1, viewModel1) // Tracker for SHIP_TRACK_003
        shipment1.addObserver(trackerForShipment1)

        shipment1.notifyObservers() // Give trackerForShipment1 its initial state
        assertEquals("Created", viewModel1.shipmentStatus)
        assertEquals(1, viewModel1.shipmentUpdateHistory.size)


        // Capture initial state of viewModel1 before unrelated update
        val initialStatus1 = viewModel1.shipmentStatus
        val initialLocation1 = viewModel1.currentLocation
        val initialNotes1 = viewModel1.shipmentNotes.toList()
        val initialHistory1 = viewModel1.shipmentUpdateHistory.toList()

        // Simulate update for shipment2 (unrelated to trackerForShipment1)
        shipment2.status = "Shipped"
        val shippedUpdate2 = ShippingUpdate.fromString("shipped,$shipmentId2,2000,3000")
        shipment2.addUpdate(shippedUpdate2)
        shipment2.notifyObservers() // This notification is for shipment2's observers ONLY

        // Assert that trackerForShipment1's ViewModel state remains unchanged
        assertEquals(initialStatus1, viewModel1.shipmentStatus, "Tracker for shipment1's status should NOT change due to unrelated update")
        assertEquals(initialLocation1, viewModel1.currentLocation, "Tracker for shipment1's location should NOT change due to unrelated update")
        assertContentEquals(initialNotes1.toTypedArray(), viewModel1.shipmentNotes, "Tracker for shipment1's notes should NOT change due to unrelated update")
        assertContentEquals(initialHistory1.toTypedArray(), viewModel1.shipmentUpdateHistory, "Tracker for shipment1's history should NOT change due to unrelated update")


        // Now, update shipment1 and verify trackerForShipment1 updates its ViewModel
        shipment1.status = "Delivered"
        val deliveredUpdate1 = ShippingUpdate.fromString("delivered,$shipmentId1,4000")
        shipment1.addUpdate(deliveredUpdate1)
        shipment1.notifyObservers()
        assertEquals("Delivered", viewModel1.shipmentStatus, "Tracker for shipment1 should update when its own shipment notifies")
        assertEquals(2, viewModel1.shipmentUpdateHistory.size)
        assertTrue(viewModel1.shipmentUpdateHistory[1].contains("Delivered"), "Formatted history should contain 'Delivered'")
    }

    @Test
    fun `Tracker should stop receiving updates after being removed as observer`() {
        val shipmentId = "SHIP_TRACK_005"
        val shipment = Shipment(shipmentId)
        val initialCreatedUpdate = ShippingUpdate.fromString("created,$shipmentId,1000")
        shipment.status = "Created"
        shipment.addUpdate(initialCreatedUpdate)

        val viewModel = TrackerViewHelper(shipmentId)
        val tracker = Tracker(shipmentId, viewModel)
        shipment.addObserver(tracker)

        // Initial update
        shipment.notifyObservers()
        assertEquals("Created", viewModel.shipmentStatus)
        assertEquals(1, viewModel.shipmentUpdateHistory.size)


        // Remove observer
        shipment.removeObserver(tracker)

        // Simulate another change
        shipment.status = "Lost"
        val lostUpdate = ShippingUpdate.fromString("lost,$shipmentId,2000")
        shipment.addUpdate(lostUpdate)
        shipment.notifyObservers() // This notification should NOT reach the tracker

        // Assert that the TrackerViewHelper's state is still from the previous update
        assertEquals("Created", viewModel.shipmentStatus) // Should NOT be "Lost"
        assertEquals(1, viewModel.shipmentUpdateHistory.size, "History should NOT gain new updates after observer removal")
    }

    @Test
    fun `Tracker should correctly handle null properties from Shipment`() {
        val shipmentId = "SHIP_TRACK_006"
        val shipment = Shipment(shipmentId) // Location, ExpectedDelivery are null by default
        shipment.status = "Created"
        val createdUpdate = ShippingUpdate.fromString("created,$shipmentId,1000")
        shipment.addUpdate(createdUpdate)

        val viewModel = TrackerViewHelper(shipmentId)
        val tracker = Tracker(shipmentId, viewModel)
        shipment.addObserver(tracker)

        shipment.notifyObservers()

        assertEquals("Created", viewModel.shipmentStatus)
        assertNull(viewModel.currentLocation)
        assertEquals("N/A", viewModel.expectedShipmentDeliveryDate)
        assertTrue(viewModel.shipmentNotes.isEmpty())
        assertEquals(1, viewModel.shipmentUpdateHistory.size)
        assertTrue(viewModel.shipmentUpdateHistory[0].contains("Created"))
    }
}