package marcus.hansen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

class NoteAddedStrategyTest {

    @Test
    fun `update should add a note to the notes list and add update to history`() {
        val shipmentId = "SHIP_NOTE_001"
        val shipment = Shipment(shipmentId)
        val noteContent = "Customer requested signature on delivery."
        val updateData = ShippingUpdate.fromString("noteadded,$shipmentId,1678886400000,$noteContent")

        val strategy = NoteAddedStrategy()
        strategy.update(shipment, updateData)

        assertEquals(1, shipment.getImmutableNotes().size)
        assertEquals(noteContent, shipment.getImmutableNotes()[0])
        assertEquals(1, shipment.getImmutableUpdateHistory().size)
        assertEquals(updateData, shipment.getImmutableUpdateHistory()[0])
    }

    @Test
    fun `update should notify observers after adding note`() {
        val shipmentId = "SHIP_NOTE_002"
        val shipment = Shipment(shipmentId)
        val noteContent = "Fragile contents, handle with care."
        val updateData = ShippingUpdate.fromString("noteadded,$shipmentId,1678886400000,$noteContent")
        val mockObserver = MockShipmentObserver()
        shipment.addObserver(mockObserver)

        val strategy = NoteAddedStrategy()
        strategy.update(shipment, updateData)

        assertTrue(mockObserver.updateCalled)
        assertNotNull(mockObserver.receivedShipment)
        assertEquals(shipmentId, mockObserver.receivedShipment?.id)
        assertEquals(1, mockObserver.receivedShipment?.getImmutableNotes()?.size)
        assertEquals(noteContent, mockObserver.receivedShipment?.getImmutableNotes()?.get(0))
    }

    @Test
    fun `update should not add note if otherInfo is missing`() {
        val shipmentId = "SHIP_NOTE_003"
        val shipment = Shipment(shipmentId)
        val updateData = ShippingUpdate.fromString("noteadded,$shipmentId,1678886400000") // No otherInfo

        val strategy = NoteAddedStrategy()
        strategy.update(shipment, updateData)

        assertTrue(shipment.getImmutableNotes().isEmpty()) // No note should be added
        assertEquals(1, shipment.getImmutableUpdateHistory().size)
    }
}