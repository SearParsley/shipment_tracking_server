package marcus.hansen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertNotNull

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
        shipment.addUpdate(createdUpdate) // Explicitly add the created update to history

        val tracker = Tracker(shipmentId) // Create Tracker instance
        shipment.addObserver(tracker) // Register tracker as observer

        // Manually call notifyObservers to simulate a state change and notification
        shipment.notifyObservers()

        // Assert that the tracker's internal state reflects the shipment's state
        assertEquals("Created", tracker.lastReceivedStatus)
        assertEquals("Warehouse A", tracker.lastReceivedLocation)
        assertEquals(123456789L, tracker.lastReceivedExpectedDelivery)
        assertEquals(1, tracker.lastReceivedNotes.size)
        assertEquals("Initial note.", tracker.lastReceivedNotes[0])
        assertEquals(1, tracker.lastReceivedUpdateHistory.size)
        assertEquals(createdUpdate, tracker.lastReceivedUpdateHistory[0])
    }

    @Test
    fun `Tracker should update its internal state when shipment changes`() {
        val shipmentId = "SHIP_TRACK_002"
        val shipment = Shipment(shipmentId)
        val initialCreatedUpdate = ShippingUpdate.fromString("created,$shipmentId,1000")
        shipment.status = "Created" // Initial status set
        shipment.addUpdate(initialCreatedUpdate)

        val tracker = Tracker(shipmentId)
        shipment.addObserver(tracker)

        // Initial notification to give tracker its first state (e.g., "Created" and initial history)
        shipment.notifyObservers()
        assertEquals("Created", tracker.lastReceivedStatus) // Verify initial state is received
        assertEquals(1, tracker.lastReceivedUpdateHistory.size, "Initial history should have 1 update after first notify")


        // Simulate a status and location change
        shipment.status = "Shipped"
        shipment.currentLocation = "Shipping Hub"
        val shippedUpdate = ShippingUpdate.fromString("shipped,$shipmentId,2000,3000")
        shipment.addUpdate(shippedUpdate) // This adds the second update to history

        shipment.notifyObservers() // Notify again after changes

        // Assert that the tracker's internal state is now updated
        assertEquals("Shipped", tracker.lastReceivedStatus)
        assertEquals("Shipping Hub", tracker.lastReceivedLocation)
        // Now expecting 2 updates in history: initial "created" and the "shipped" one
        assertEquals(2, tracker.lastReceivedUpdateHistory.size, "Shipment history should contain two updates")
        // Verify the second update (the new one)
        assertEquals(shippedUpdate, tracker.lastReceivedUpdateHistory[1], "The second update in history should be the shipped one")
    }

    @Test
    fun `Tracker should not update for unrelated shipments`() {
        val shipmentId1 = "SHIP_TRACK_003"
        val shipmentId2 = "SHIP_TRACK_004"
        val shipment1 = Shipment(shipmentId1)
        val initialCreatedUpdate1 = ShippingUpdate.fromString("created,$shipmentId1,1000")
        shipment1.status = "Created" // Set initial status for shipment1
        shipment1.addUpdate(initialCreatedUpdate1)
        val shipment2 = Shipment(shipmentId2)
        val initialCreatedUpdate2 = ShippingUpdate.fromString("created,$shipmentId2,1000")
        shipment2.status = "Created" // Set initial status for shipment2
        shipment2.addUpdate(initialCreatedUpdate2) // Add initial update to history for shipment2

        val trackerForShipment1 = Tracker(shipmentId1) // Tracker for SHIP_TRACK_003
        shipment1.addObserver(trackerForShipment1) // ONLY adds to shipment1's observers

        // Call notifyObservers() for shipment1 to give trackerForShipment1 its initial state and history
        shipment1.notifyObservers()
        assertEquals("Created", trackerForShipment1.lastReceivedStatus, "Tracker for shipment1 should receive initial 'Created' status")
        assertEquals(1, trackerForShipment1.lastReceivedUpdateHistory.size, "Tracker for shipment1's history should have its initial update")


        // Simulate update for shipment2 (unrelated to trackerForShipment1)
        shipment2.status = "Shipped" // Changes shipment2's status
        val shippedUpdate2 = ShippingUpdate.fromString("shipped,$shipmentId2,2000,3000")
        shipment2.addUpdate(shippedUpdate2)
        shipment2.notifyObservers() // This notification is for shipment2's observers ONLY

        // Assert that trackerForShipment1's state remains unchanged from its initial "Created" status
        assertEquals("Created", trackerForShipment1.lastReceivedStatus, "Tracker for shipment1 should NOT change status due to unrelated update")
        assertNull(trackerForShipment1.lastReceivedLocation, "Tracker for shipment1's location should remain null")
        assertTrue(trackerForShipment1.lastReceivedNotes.isEmpty(), "Tracker for shipment1's notes should remain empty")
        assertEquals(1, trackerForShipment1.lastReceivedUpdateHistory.size, "Tracker for shipment1's history should only have its initial update")

        // Now, update shipment1 and verify trackerForShipment1 updates
        shipment1.status = "Delivered"
        val deliveredUpdate1 = ShippingUpdate.fromString("delivered,$shipmentId1,4000")
        shipment1.addUpdate(deliveredUpdate1)
        shipment1.notifyObservers()
        assertEquals("Delivered", trackerForShipment1.lastReceivedStatus, "Tracker for shipment1 should update when its own shipment notifies")
        assertEquals(2, trackerForShipment1.lastReceivedUpdateHistory.size, "Tracker for shipment1's history should now have two updates")
    }

    @Test
    fun `Tracker should stop receiving updates after being removed as observer`() {
        val shipmentId = "SHIP_TRACK_005"
        val shipment = Shipment(shipmentId)
        val initialCreatedUpdate = ShippingUpdate.fromString("created,$shipmentId,1000")
        shipment.status = "Created"
        shipment.addUpdate(initialCreatedUpdate)
        
        val tracker = Tracker(shipmentId)
        shipment.addObserver(tracker)

        // Initial update
        shipment.notifyObservers()
        assertEquals("Created", tracker.lastReceivedStatus)
        assertEquals(1, tracker.lastReceivedUpdateHistory.size, "History should have 1 update initially")


        // Remove observer
        shipment.removeObserver(tracker)

        // Simulate another change
        shipment.status = "Lost"
        val lostUpdate = ShippingUpdate.fromString("lost,$shipmentId,2000")
        shipment.addUpdate(lostUpdate) // Add another update to shipment's history
        shipment.notifyObservers() // This notification should NOT reach the tracker

        // Assert that the tracker's state is still from the previous update
        assertEquals("Created", tracker.lastReceivedStatus) // Should NOT be "Lost"
        assertEquals(1, tracker.lastReceivedUpdateHistory.size, "History should NOT gain new updates after observer removal")
    }

    @Test
    fun `Tracker should correctly handle null properties from Shipment`() {
        val shipmentId = "SHIP_TRACK_006"
        val shipment = Shipment(shipmentId) // Location, ExpectedDelivery are null by default
        shipment.status = "Created"
        val createdUpdate = ShippingUpdate.fromString("created,$shipmentId,1000")
        shipment.addUpdate(createdUpdate) // Add initial update to history

        val tracker = Tracker(shipmentId)
        shipment.addObserver(tracker)

        shipment.notifyObservers()

        assertEquals("Created", tracker.lastReceivedStatus)
        assertNull(tracker.lastReceivedLocation)
        assertNull(tracker.lastReceivedExpectedDelivery)
        assertTrue(tracker.lastReceivedNotes.isEmpty())
        assertEquals(1, tracker.lastReceivedUpdateHistory.size)
        assertEquals(createdUpdate, tracker.lastReceivedUpdateHistory[0])
    }
}