package marcus.hansen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest // Import runTest for controlling virtual time
import java.io.File

class TrackingSimulatorIntegrationTest {

    private val TEST_FILE_PATH = "test_comprehensive_updates.txt"

    private fun createTestFile(content: List<String>) {
        File(TEST_FILE_PATH).writeText(content.joinToString("\n"))
    }

    private fun deleteTestFile() {
        File(TEST_FILE_PATH).delete()
    }

    @Test
    fun `runSimulation should correctly process a comprehensive set of updates for multiple shipments`() = runTest {
        // Base Timestamp: Nov 15, 2023 00:00:00 GMT
        val baseTimestamp = 1700000000000L

        // Comprehensive test data covering all update types and multiple shipments
        val testContent = listOf(
            // Shipment A (ID: SHIP_A) - Full Lifecycle
            "created,SHIP_A,${baseTimestamp}",
            "shipped,SHIP_A,${baseTimestamp + 1000},${baseTimestamp + 5000}", // +1s, Expected Delivery +5s
            "location,SHIP_A,${baseTimestamp + 2000},Los Angeles CA", // +2s
            "location,SHIP_A,${baseTimestamp + 3000},New York NY", // +3s
            "noteadded,SHIP_A,${baseTimestamp + 3500},Customer called to confirm address", // +3.5s
            "delivered,SHIP_A,${baseTimestamp + 4000}", // +4s

            // Shipment B (ID: SHIP_B) - Delayed & Lost
            "created,SHIP_B,${baseTimestamp + 10000}", // +10s
            "shipped,SHIP_B,${baseTimestamp + 11000},${baseTimestamp + 15000}", // +11s, Expected Delivery +15s
            "delayed,SHIP_B,${baseTimestamp + 12000},${baseTimestamp + 18000}", // +12s, New Expected Delivery +18s
            "noteadded,SHIP_B,${baseTimestamp + 12500},Weather delay in transit", // +12.5s
            "lost,SHIP_B,${baseTimestamp + 13000}", // +13s

            // Shipment C (ID: SHIP_C) - Canceled
            "created,SHIP_C,${baseTimestamp + 20000}", // +20s
            "canceled,SHIP_C,${baseTimestamp + 21000}" // +21s
        )
        createTestFile(testContent)

        val simulator = TrackingSimulator()
        simulator.runSimulation(TEST_FILE_PATH) // runTest automatically advances virtual time for delays

        // --- Assertions for Shipment A (Full Lifecycle) ---
        val shipmentA = simulator.findShipment("SHIP_A")
        assertNotNull(shipmentA, "Shipment A should exist")
        assertEquals("Delivered", shipmentA?.status, "Shipment A final status should be Delivered")
        assertEquals("New York NY", shipmentA?.currentLocation, "Shipment A final location should be New York NY")
        // Delivered timestamp is the last expected delivery, or null if not set by delivered update
        // Based on your requirements, delivered update sets status, not expected delivery timestamp.
        assertNull(shipmentA?.expectedDeliveryDateTimestamp, "Shipment A expected delivery should be null after delivered")

        assertEquals(1, shipmentA?.getImmutableNotes()?.size, "Shipment A should have 1 note")
        assertEquals("Customer called to confirm address", shipmentA?.getImmutableNotes()?.get(0))

        assertEquals(6, shipmentA?.getImmutableUpdateHistory()?.size, "Shipment A history should have 6 updates")
        assertEquals("created", shipmentA?.getImmutableUpdateHistory()?.get(0)?.updateType)
        assertEquals("shipped", shipmentA?.getImmutableUpdateHistory()?.get(1)?.updateType)
        assertEquals("location", shipmentA?.getImmutableUpdateHistory()?.get(2)?.updateType)
        assertEquals("location", shipmentA?.getImmutableUpdateHistory()?.get(3)?.updateType)
        assertEquals("noteadded", shipmentA?.getImmutableUpdateHistory()?.get(4)?.updateType)
        assertEquals("delivered", shipmentA?.getImmutableUpdateHistory()?.get(5)?.updateType)


        // --- Assertions for Shipment B (Delayed & Lost) ---
        val shipmentB = simulator.findShipment("SHIP_B")
        assertNotNull(shipmentB, "Shipment B should exist")
        assertEquals("Lost", shipmentB?.status, "Shipment B final status should be Lost")
        assertEquals(baseTimestamp + 18000, shipmentB?.expectedDeliveryDateTimestamp, "Shipment B expected delivery should be updated by delayed")

        assertEquals(1, shipmentB?.getImmutableNotes()?.size, "Shipment B should have 1 note")
        assertEquals("Weather delay in transit", shipmentB?.getImmutableNotes()?.get(0))

        assertEquals(5, shipmentB?.getImmutableUpdateHistory()?.size, "Shipment B history should have 5 updates")
        assertEquals("created", shipmentB?.getImmutableUpdateHistory()?.get(0)?.updateType)
        assertEquals("shipped", shipmentB?.getImmutableUpdateHistory()?.get(1)?.updateType)
        assertEquals("delayed", shipmentB?.getImmutableUpdateHistory()?.get(2)?.updateType)
        assertEquals("noteadded", shipmentB?.getImmutableUpdateHistory()?.get(3)?.updateType)
        assertEquals("lost", shipmentB?.getImmutableUpdateHistory()?.get(4)?.updateType)


        // --- Assertions for Shipment C (Canceled) ---
        val shipmentC = simulator.findShipment("SHIP_C")
        assertNotNull(shipmentC, "Shipment C should exist")
        assertEquals("Canceled", shipmentC?.status, "Shipment C final status should be Canceled")
        assertEquals(2, shipmentC?.getImmutableUpdateHistory()?.size, "Shipment C history should have 2 updates")
        assertEquals("created", shipmentC?.getImmutableUpdateHistory()?.get(0)?.updateType)
        assertEquals("canceled", shipmentC?.getImmutableUpdateHistory()?.get(1)?.updateType)

        deleteTestFile()
    }

    @Test
    fun `runSimulation should handle updates for non-existent shipments gracefully`() = runTest {
        val testContent = listOf(
            "shipped,NON_EXISTENT_SHIP,1678886400000,1678887400000", // This should be skipped
            "created,EXISTING_SHIP,1678886500000"
        )
        createTestFile(testContent)

        val simulator = TrackingSimulator()
        simulator.runSimulation(TEST_FILE_PATH)

        assertNull(simulator.findShipment("NON_EXISTENT_SHIP"), "Non-existent shipment should not be created by non-create update")
        assertNotNull(simulator.findShipment("EXISTING_SHIP"), "Existing shipment should be created")

        deleteTestFile()
    }
}