package marcus.hansen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class DelayedStrategyTest {

    @Test
    fun `update should set status to Delayed and update expected delivery timestamp`() {
        val shipmentId = "SHIP_DELAY_001"
        val shipment = Shipment(shipmentId)
        shipment.status = "Shipped"
        val newExpectedTimestamp = 1679999999000L
        val updateData = ShippingUpdate.fromString("delayed,$shipmentId,1678886400000,$newExpectedTimestamp")

        val strategy = DelayedStrategy()
        strategy.update(shipment, updateData)

        assertEquals("Delayed", shipment.status)
        assertEquals(newExpectedTimestamp, shipment.expectedDeliveryDateTimestamp)
        assertEquals(1, shipment.getImmutableUpdateHistory().size)
        assertEquals(updateData, shipment.getImmutableUpdateHistory()[0])
    }

    @Test
    fun `update should notify observers after setting status and timestamp`() {
        val shipmentId = "SHIP_DELAY_002"
        val shipment = Shipment(shipmentId)
        shipment.status = "Shipped"
        val newExpectedTimestamp = 1679999999000L
        val updateData = ShippingUpdate.fromString("delayed,$shipmentId,1678886400000,$newExpectedTimestamp")
        val mockObserver = MockShipmentObserver()
        shipment.addObserver(mockObserver)

        val strategy = DelayedStrategy()
        strategy.update(shipment, updateData)

        assertTrue(mockObserver.updateCalled)
        assertNotNull(mockObserver.receivedShipment)
        assertEquals(shipmentId, mockObserver.receivedShipment?.id)
        assertEquals("Delayed", mockObserver.receivedShipment?.status)
        assertEquals(newExpectedTimestamp, mockObserver.receivedShipment?.expectedDeliveryDateTimestamp)
    }

    @Test
    fun `update should not change expected delivery if otherInfo is missing`() {
        val shipmentId = "SHIP_DELAY_003"
        val shipment = Shipment(shipmentId)
        shipment.status = "Shipped"
        shipment.expectedDeliveryDateTimestamp = 1000L // Pre-existing timestamp
        val updateData = ShippingUpdate.fromString("delayed,$shipmentId,1678886400000") // No otherInfo

        val strategy = DelayedStrategy()
        strategy.update(shipment, updateData)

        assertEquals("Delayed", shipment.status)
        assertEquals(1000L, shipment.expectedDeliveryDateTimestamp) // Should remain unchanged
        assertEquals(1, shipment.getImmutableUpdateHistory().size)
    }

    @Test
    fun `update should log error if expected delivery timestamp is invalid`() {
        val originalErr = System.err
        val bos = ByteArrayOutputStream()
        System.setErr(PrintStream(bos)) // Redirect stderr

        try {
            val shipmentId = "SHIP_DELAY_004"
            val shipment = Shipment(shipmentId)
            shipment.status = "Shipped"
            val updateData = ShippingUpdate.fromString("delayed,$shipmentId,1678886400000,not_a_timestamp")

            val strategy = DelayedStrategy()
            strategy.update(shipment, updateData)

            assertEquals("Delayed", shipment.status) // Status should still change
            assertNull(shipment.expectedDeliveryDateTimestamp) // Timestamp should not be set
            assertTrue(bos.toString().contains("Invalid timestamp format"), "Error message should be logged")
        } finally {
            System.setErr(originalErr) // Restore stderr
        }
    }
}