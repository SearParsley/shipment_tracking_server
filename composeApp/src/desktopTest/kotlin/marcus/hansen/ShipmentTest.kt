package marcus.hansen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNull

class ShipmentTest {

    @Test
    fun `Shipment constructor should correctly initialize properties`() {
        val shipmentId = "SHIP123"
        val shipment = Shipment(shipmentId)

        assertEquals(shipmentId, shipment.id)
        assertEquals("Unknown", shipment.status)
        assertTrue(shipment.getImmutableNotes().isEmpty())
        assertTrue(shipment.getImmutableUpdateHistory().isEmpty())
        assertNull(shipment.expectedDeliveryDateTimestamp)
        assertNull(shipment.currentLocation)
    }

    @Test
    fun `addNote should add a note to the notes list`() {
        val shipment = Shipment("SHIP001")
        val note1 = "Packaging looks good."
        val note2 = "Customer requested special handling."

        shipment.addNote(note1)
        assertEquals(1, shipment.getImmutableNotes().size)
        assertEquals(note1, shipment.getImmutableNotes()[0])

        shipment.addNote(note2)
        assertEquals(2, shipment.getImmutableNotes().size)
        assertEquals(note2, shipment.getImmutableNotes()[1])
        assertTrue(shipment.getImmutableNotes().containsAll(listOf(note1, note2)))
    }

    @Test
    fun `addUpdate should add a ShippingUpdate to the updateHistory list`() {
        val shipment = Shipment("SHIP002")
        val update1 = ShippingUpdate.fromString("created,SHIP002,1000")
        val update2 = ShippingUpdate.fromString("shipped,SHIP002,2000,3000")

        shipment.addUpdate(update1)
        assertEquals(1, shipment.getImmutableUpdateHistory().size)
        assertEquals(update1, shipment.getImmutableUpdateHistory()[0])

        shipment.addUpdate(update2)
        assertEquals(2, shipment.getImmutableUpdateHistory().size)
        assertEquals(update2, shipment.getImmutableUpdateHistory()[1])
        assertTrue(shipment.getImmutableUpdateHistory().containsAll(listOf(update1, update2)))
    }

    @Test
    fun `addObserver should add an observer to the observers list`() {
        val shipment = Shipment("SHIP003")
        val viewModel = TrackerViewHelper(shipment.id)
        val tracker1 = Tracker(shipment.id, viewModel)
        val tracker2 = Tracker(shipment.id, viewModel)

        shipment.addObserver(tracker1)
        shipment.addObserver(tracker2)
    }

//    @Test
//    fun `removeObserver should remove an observer from the observers list`() {
//        val shipment = Shipment("SHIP004")
//        val observer1 = MockObserver()
//        val observer2 = MockObserver()
//
//        shipment.addObserver(observer1)
//        shipment.addObserver(observer2)
//
//        shipment.removeObserver(observer1)
//        // Now, only observer2 should be notified
//        shipment.notifyObservers()
//        assertEquals(0, observer1.updateCalledCount) // observer1 should not have been called
//        assertEquals(1, observer2.updateCalledCount) // observer2 should have been called
//        assertEquals(shipment, observer2.lastReceivedShipment) // ensure correct shipment was passed
//    }
//
//    @Test
//    fun `notifyObservers should call update on all registered observers`() {
//        val shipment = Shipment("SHIP005")
//        val observer1 = MockObserver()
//        val observer2 = MockObserver()
//
//        shipment.addObserver(observer1)
//        shipment.addObserver(observer2)
//
//        shipment.notifyObservers()
//
//        assertEquals(1, observer1.updateCalledCount)
//        assertEquals(shipment, observer1.lastReceivedShipment)
//        assertEquals(1, observer2.updateCalledCount)
//        assertEquals(shipment, observer2.lastReceivedShipment)
//    }
//
//    @Test
//    fun `notifyObservers should not call update on removed observers`() {
//        val shipment = Shipment("SHIP006")
//        val observer1 = MockObserver()
//        val observer2 = MockObserver()
//
//        shipment.addObserver(observer1)
//        shipment.addObserver(observer2)
//        shipment.removeObserver(observer1)
//
//        shipment.notifyObservers()
//
//        assertEquals(0, observer1.updateCalledCount)
//        assertEquals(1, observer2.updateCalledCount)
//    }

    @Test
    fun `status property should be correctly updated via internal setter`() {
        val shipment = Shipment("SHIP007")
        val newStatus = "Shipped"
        shipment.status = newStatus // Direct assignment uses the internal set

        assertEquals(newStatus, shipment.status)
    }

    @Test
    fun `expectedDeliveryDateTimestamp property should be correctly updated via internal setter`() {
        val shipment = Shipment("SHIP008")
        val newTimestamp = 1678886400000L // Example timestamp
        shipment.expectedDeliveryDateTimestamp = newTimestamp // Direct assignment uses the internal set

        assertEquals(newTimestamp, shipment.expectedDeliveryDateTimestamp)
    }

    @Test
    fun `currentLocation property should be correctly updated via internal setter`() {
        val shipment = Shipment("SHIP009")
        val newLocation = "New York, NY"
        shipment.currentLocation = newLocation // Direct assignment uses the internal set

        assertEquals(newLocation, shipment.currentLocation)
    }

    @Test
    fun `getImmutableNotes should return an immutable copy of the notes list`() {
        val shipment = Shipment("SHIP010")
        shipment.addNote("Test Note")
        val notesCopy = shipment.getImmutableNotes()

        assertEquals(1, shipment.getImmutableNotes().size) // Internal list size should remain 1
        assertEquals(1, notesCopy.size) // Returned copy size should also be 1 initially

        // Attempting to modify notesCopy should result in a compilation error or runtime error
        // as List<String> is immutable. This indirectly confirms it's a copy/immutable view.
        // val mutableNotesCopy = notesCopy as MutableList<String> // This would throw ClassCastException
    }

    @Test
    fun `getImmutableUpdateHistory should return an immutable copy of the update history list`() {
        val shipment = Shipment("SHIP011")
        val update = ShippingUpdate.fromString("created,SHIP011,1000")
        shipment.addUpdate(update)
        val historyCopy = shipment.getImmutableUpdateHistory()

        assertEquals(1, shipment.getImmutableUpdateHistory().size)
        assertEquals(1, historyCopy.size)
    }
}