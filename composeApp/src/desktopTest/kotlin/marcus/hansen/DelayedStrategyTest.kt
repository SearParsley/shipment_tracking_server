package marcus.hansen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class DelayedStrategyTest {

    @Test
    fun `update should set status to Delayed and update expected delivery timestamp`() {
        val shipmentId = "SHIP_DELAY_001"
        val shipment = Shipment(shipmentId)
        shipment.status = "Shipped"
        val newExpectedTimestamp = 1679999999000L
        val updateData = ShippingUpdate.fromString("delayed,$shipmentId,1678886400000,$newExpectedTimestamp")

        val strategy = DelayedStrategy() // Use the original strategy
        strategy.update(shipment, updateData)

        assertEquals("Delayed", shipment.status)
        assertEquals(newExpectedTimestamp, shipment.expectedDeliveryDateTimestamp)
        assertEquals(1, shipment.getImmutableUpdateHistory().size)
        assertEquals(updateData, shipment.getImmutableUpdateHistory()[0])
    }

    @Test
    fun `update should notify Tracker and update TrackerViewHelper correctly for delayed`() {
        val shipmentId = "SHIP_DELAY_002_TRACKER"
        val creationTime = Instant.parse("2023-01-01T10:00:00Z").toEpochMilli()
        val shipment = Shipment(shipmentId, ShipmentType.STANDARD, creationTime)
        shipment.status = "Shipped"
        shipment.addUpdate(ShippingUpdate.fromString("created,$shipmentId,$creationTime"))
        shipment.addUpdate(ShippingUpdate.fromString("shipped,$shipmentId,${creationTime + 1000},${creationTime + 10000}"))

        val newExpectedTimestamp = Instant.parse("2023-01-05T10:00:00Z").toEpochMilli()
        val updateData = ShippingUpdate.fromString("delayed,$shipmentId,${Instant.now().toEpochMilli()},$newExpectedTimestamp")
        val viewModel = TrackerViewHelper(shipmentId)
        val tracker = Tracker(shipmentId, viewModel)
        shipment.addObserver(tracker)

        val strategy = DelayedStrategy() // Use the original strategy
        strategy.update(shipment, updateData)

        assertEquals("Delayed", viewModel.shipmentStatus)
        val instant = Instant.ofEpochMilli(newExpectedTimestamp)
        val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        assertEquals(dateTime.format(formatter), viewModel.expectedShipmentDeliveryDate)
        assertEquals(3, viewModel.shipmentUpdateHistory.size)
        assertTrue(viewModel.shipmentUpdateHistory[2].contains("Delayed"), "History should contain 'Delayed' update")
        assertTrue(viewModel.ruleViolations.isEmpty(), "No rule violations expected for standard shipment delayed")
    }

    @Test
    fun `update should not change expected delivery if otherInfo is missing`() {
        val shipmentId = "SHIP_DELAY_003"
        val shipment = Shipment(shipmentId)
        shipment.status = "Shipped"
        shipment.expectedDeliveryDateTimestamp = 1000L
        val updateData = ShippingUpdate.fromString("delayed,$shipmentId,1678886400000")

        val strategy = DelayedStrategy() // Use the original strategy
        strategy.update(shipment, updateData)

        assertEquals("Delayed", shipment.status)
        assertEquals(1000L, shipment.expectedDeliveryDateTimestamp)
        assertEquals(1, shipment.getImmutableUpdateHistory().size)
    }

    @Test
    fun `update should log error if expected delivery timestamp is invalid`() {
        val originalErr = System.err
        val bos = ByteArrayOutputStream()
        val ps = PrintStream(bos, true) // 'true' for auto-flush
        System.setErr(ps) // Redirect stderr

        try {
            val shipmentId = "SHIP_DELAY_004"
            val shipment = Shipment(shipmentId)
            shipment.status = "Shipped"
            val updateData = ShippingUpdate.fromString("delayed,$shipmentId,1678886400000,not_a_timestamp")

            val strategy = DelayedStrategy() // Use the original strategy
            strategy.update(shipment, updateData)

            assertEquals("Delayed", shipment.status)
            assertNull(shipment.expectedDeliveryDateTimestamp)

            ps.close() // CRUCIAL: Close the PrintStream to flush all content

            val loggedOutput = bos.toString()
            assertTrue(loggedOutput.contains("Invalid timestamp format"), "Error message should contain 'Invalid timestamp format'")
            assertTrue(loggedOutput.contains(shipmentId), "Error message should contain shipment ID")
            assertTrue(loggedOutput.contains("not_a_timestamp"), "Error message should contain the invalid timestamp")

        } finally {
            System.setErr(originalErr) // Restore stderr
            // Ensure ps is closed even if an exception occurs in the try block
            // If ps is declared outside try, close it in finally.
            // If declared inside, it will be closed by the finally block's execution path.
        }
    }

    @Test
    fun `update should notify observers after setting status and timestamp`() {
        val shipmentId = "SHIP_DELAY_002"
        val shipment = Shipment(shipmentId)
        shipment.status = "Shipped"
        val newExpectedTimestamp = 1679999999000L
        val update = ShippingUpdate.fromString("delayed,$shipmentId,1678886400000,$newExpectedTimestamp")
        val viewModel = TrackerViewHelper(shipmentId)
        val tracker = Tracker(shipmentId, viewModel)
        shipment.addObserver(tracker)

        val strategy = DelayedStrategy()
        strategy.update(shipment, update)

        assertEquals(shipmentId, viewModel.shipmentId)
        assertEquals("Delayed", viewModel.shipmentStatus)
        val instant = Instant.ofEpochMilli(newExpectedTimestamp)
        val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        assertEquals(dateTime.format(formatter), viewModel.expectedShipmentDeliveryDate)    }

    @Test
    fun `DelayedStrategy should NOT add violation for OvernightShipment when delayed beyond 1 day`() {
        val shipmentId = "OVERNIGHT_DELAYED_NV_01"
        val creationTime = Instant.parse("2023-01-01T10:00:00Z").toEpochMilli()
        val expectedDeliveryTime = Instant.parse("2023-01-05T10:00:00Z").toEpochMilli() // 4 days later
        val shipment = Shipment(shipmentId, ShipmentType.OVERNIGHT, creationTime)
        val updateData = ShippingUpdate.fromString("delayed,$shipmentId,${Instant.now().toEpochMilli()},$expectedDeliveryTime")

        val strategy = DelayedStrategy()
        strategy.update(shipment, updateData)

        assertEquals("Delayed", shipment.status)
        assertEquals(expectedDeliveryTime, shipment.expectedDeliveryDateTimestamp)
        // This is the key assertion for the 'delayed' exception rule
        assertTrue(shipment.ruleViolations.isEmpty(), "Overnight shipment should NOT have a violation when delayed beyond 1 day")
    }

    @Test
    fun `DelayedStrategy should add violation for ExpressShipment if delayed beyond 3 days`() {
        val shipmentId = "EXPRESS_DELAYED_V_01"
        val creationTime = Instant.parse("2023-01-01T10:00:00Z").toEpochMilli()
        val expectedDeliveryTime = Instant.parse("2023-01-05T10:00:00Z").toEpochMilli() // 4 days later
        val shipment = Shipment(shipmentId, ShipmentType.EXPRESS, creationTime)
        val updateData = ShippingUpdate.fromString("delayed,$shipmentId,${Instant.now().toEpochMilli()},$expectedDeliveryTime")

        val strategy = DelayedStrategy()
        strategy.update(shipment, updateData)

        assertEquals("Delayed", shipment.status)
        assertEquals(1, shipment.ruleViolations.size, "Should have one rule violation")
        assertTrue(shipment.ruleViolations[0].contains("Express shipment delivery date") &&
                shipment.ruleViolations[0].contains("is more than 3 days after creation"))
    }

    @Test
    fun `DelayedStrategy should add violation for BulkShipment if delayed to be sooner than 3 days`() {
        val shipmentId = "BULK_DELAYED_V_01"
        val creationTime = Instant.parse("2023-01-01T10:00:00Z").toEpochMilli()
        val expectedDeliveryTime = Instant.parse("2023-01-02T10:00:00Z").toEpochMilli() // 1 day later
        val shipment = Shipment(shipmentId, ShipmentType.BULK, creationTime)
        val updateData = ShippingUpdate.fromString("delayed,$shipmentId,${Instant.now().toEpochMilli()},$expectedDeliveryTime")

        val strategy = DelayedStrategy()
        strategy.update(shipment, updateData)

        assertEquals("Delayed", shipment.status)
        assertEquals(1, shipment.ruleViolations.size, "Should have one rule violation")
        assertTrue(shipment.ruleViolations[0].contains("Bulk shipment delivery date") &&
                shipment.ruleViolations[0].contains("is sooner than 3 days after creation"))
    }

    @Test
    fun `DelayedStrategy should clear previous violations before new validation`() {
        val shipmentId = "DELAYED_CLEAR_V_01"
        val creationTime = Instant.parse("2023-01-01T10:00:00Z").toEpochMilli()
        val shipment = Shipment(shipmentId, ShipmentType.EXPRESS, creationTime)
        shipment.ruleViolations.add("Previous violation") // Add a dummy violation

        val validExpectedDeliveryTime = Instant.parse("2023-01-03T10:00:00Z").toEpochMilli() // Valid (2 days)
        val updateData = ShippingUpdate.fromString("delayed,$shipmentId,${Instant.now().toEpochMilli()},$validExpectedDeliveryTime")

        val strategy = DelayedStrategy()
        strategy.update(shipment, updateData)

        assertTrue(shipment.ruleViolations.isEmpty(), "Violations should be cleared for a valid update")
    }
}