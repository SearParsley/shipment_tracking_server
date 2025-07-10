package marcus.hansen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNull
import kotlin.test.assertContentEquals

class TrackerViewHelperTest {

    @Test
    fun `TrackerViewHelper should initialize with correct shipmentId and default values`() {
        val shipmentId = "VIEW_HELPER_001"
        val viewModel = TrackerViewHelper(shipmentId)

        assertEquals(shipmentId, viewModel.shipmentId)
        assertEquals("N/A", viewModel.shipmentStatus)
        assertNull(viewModel.currentLocation)
        assertNull(viewModel.expectedShipmentDeliveryDate)
        assertTrue(viewModel.shipmentNotes.isEmpty())
        assertTrue(viewModel.shipmentUpdateHistory.isEmpty())
    }

    @Test
    fun `shipmentStatus should be settable internally`() {
        val viewModel = TrackerViewHelper("VIEW_HELPER_002")
        val newStatus = "In Transit"
        viewModel.shipmentStatus = newStatus // Using internal set

        assertEquals(newStatus, viewModel.shipmentStatus)
    }

    @Test
    fun `currentLocation should be settable internally`() {
        val viewModel = TrackerViewHelper("VIEW_HELPER_003")
        val newLocation = "Los Angeles, CA"
        viewModel.currentLocation = newLocation // Using internal set

        assertEquals(newLocation, viewModel.currentLocation)
        viewModel.currentLocation = null // Test setting to null
        assertNull(viewModel.currentLocation)
    }

    @Test
    fun `expectedShipmentDeliveryDate should be settable internally`() {
        val viewModel = TrackerViewHelper("VIEW_HELPER_004")
        val newDate = "1701000000000"
        viewModel.expectedShipmentDeliveryDate = newDate // Using internal set

        assertEquals(newDate, viewModel.expectedShipmentDeliveryDate)
        viewModel.expectedShipmentDeliveryDate = null // Test setting to null
        assertNull(viewModel.expectedShipmentDeliveryDate)
    }

    @Test
    fun `shipmentNotes should be settable internally and correctly store array`() {
        val viewModel = TrackerViewHelper("VIEW_HELPER_005")
        val notes = arrayOf("Note 1", "Note 2")
        viewModel.shipmentNotes = notes // Using internal set

        assertContentEquals(notes, viewModel.shipmentNotes)
        assertEquals(2, viewModel.shipmentNotes.size)

        val emptyNotes = emptyArray<String>()
        viewModel.shipmentNotes = emptyNotes
        assertTrue(viewModel.shipmentNotes.isEmpty())
    }

    @Test
    fun `shipmentUpdateHistory should be settable internally and correctly store array`() {
        val viewModel = TrackerViewHelper("VIEW_HELPER_006")
        val history = arrayOf("Update A", "Update B")
        viewModel.shipmentUpdateHistory = history // Using internal set

        assertContentEquals(history, viewModel.shipmentUpdateHistory)
        assertEquals(2, viewModel.shipmentUpdateHistory.size)

        val emptyHistory = emptyArray<String>()
        viewModel.shipmentUpdateHistory = emptyHistory
        assertTrue(viewModel.shipmentUpdateHistory.isEmpty())
    }

    @Test
    fun `reset method should restore default values`() {
        val viewModel = TrackerViewHelper("VIEW_HELPER_007")
        viewModel.shipmentStatus = "Some Status"
        viewModel.currentLocation = "Some Location"
        viewModel.expectedShipmentDeliveryDate = "Some Date"
        viewModel.shipmentNotes = arrayOf("N1")
        viewModel.shipmentUpdateHistory = arrayOf("U1")

        viewModel.reset()

        assertEquals("N/A", viewModel.shipmentStatus)
        assertNull(viewModel.currentLocation)
        assertNull(viewModel.expectedShipmentDeliveryDate)
        assertTrue(viewModel.shipmentNotes.isEmpty())
        assertTrue(viewModel.shipmentUpdateHistory.isEmpty())
    }
}