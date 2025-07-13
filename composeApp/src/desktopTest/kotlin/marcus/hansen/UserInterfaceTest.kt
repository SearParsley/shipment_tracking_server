// UserInterfaceTest.kt
package marcus.hansen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlinx.coroutines.test.runTest // For running suspend functions in tests
import androidx.compose.runtime.mutableStateListOf // Used by UserInterface internally
import androidx.compose.runtime.mutableStateMapOf // Used by UserInterface internally
import androidx.compose.runtime.mutableStateOf // Used by UserInterface internally
import androidx.compose.runtime.getValue // For 'by mutableStateOf'
import androidx.compose.runtime.setValue // For 'by mutableStateOf'

// Mocking dependencies
import io.mockk.* // Imports for mockk (spyk, verify, etc.)
import androidx.compose.runtime.LaunchedEffect


// --- Mocking Classes for Testing ---




// --- UserInterface Test Suite ---
class UserInterfaceTest {

    private lateinit var mockTrackingSimulator: MockTrackingSimulator

    // Setup function to initialize mocks before each test
    @org.junit.jupiter.api.BeforeEach // Use JUnit 5's BeforeEach
    fun setup() {
        mockTrackingSimulator = MockTrackingSimulator()
    }

    @Test
    fun `trackShipment should add shipment to tracked list if found`() = runTest {
        val shipmentId = "SHIP_TEST_001"
        // Use MockShipment instead of real Shipment
        val mockShipment = spyk(MockShipment(shipmentId)) // Spying to verify observer calls
        mockShipment.status = "Created" // Set initial status for the mock
        mockShipment.addMockUpdate(ShippingUpdate.fromString("created,$shipmentId,1000")) // Add initial history
        mockTrackingSimulator.findShipmentResult = mockShipment

        // Manually create the states that the Composable would manage
        // We're testing the logic that manipulates these states, not the Composable rendering directly.
        val trackedShipments = mutableStateListOf<TrackerViewHelper>()
        val activeTrackers = mutableStateMapOf<String, Tracker>()
        var errorMessage by mutableStateOf<String?>(null) // Simulate Compose state for error message

        // Simulate the trackShipment lambda from the Composable
        val trackShipmentLambda: (String) -> Unit = { idToTrack ->
            errorMessage = null // Clear previous error
            val shipment = mockTrackingSimulator.findShipment(idToTrack) // Calls mock
            if (shipment != null) {
                if (trackedShipments.any { it.shipmentId == idToTrack }) {
                    errorMessage = "Shipment $idToTrack is already being tracked."
                } else {
                    val viewModel = TrackerViewHelper(idToTrack)
                    val tracker = Tracker(idToTrack, viewModel)
                    shipment.addObserver(tracker) // Calls mock's addObserver
                    activeTrackers[idToTrack] = tracker // Stores real tracker
                    tracker.update(shipment) // Calls real tracker.update with mock shipment
                    trackedShipments.add(viewModel) // Adds real ViewModel
                }
            } else {
                errorMessage = "Shipment with ID '$idToTrack' not found."
            }
        }

        trackShipmentLambda(shipmentId)

        assertEquals(1, trackedShipments.size)
        assertEquals(shipmentId, trackedShipments[0].shipmentId)
        assertEquals("Created", trackedShipments[0].shipmentStatus)
        assertEquals(1, trackedShipments[0].shipmentUpdateHistory.size) // Check history from initial update
        assertNull(errorMessage)
        assertEquals(1, activeTrackers.size)
        assertTrue(activeTrackers.containsKey(shipmentId))

        // Verify that addObserver was called on the mock shipment
        verify(exactly = 1) { mockShipment.addObserver(any()) }
    }

    @Test
    fun `trackShipment should display error if shipment not found`() = runTest {
        val shipmentId = "NON_EXISTENT_SHIP"
        mockTrackingSimulator.findShipmentResult = null // Simulate not found

        val trackedShipments = mutableStateListOf<TrackerViewHelper>()
        val activeTrackers = mutableStateMapOf<String, Tracker>()
        var errorMessage by mutableStateOf<String?>(null)

        val trackShipmentLambda: (String) -> Unit = { idToTrack ->
            errorMessage = null
            val shipment = mockTrackingSimulator.findShipment(idToTrack)
            if (shipment != null) { /* ... same logic as above if found ... */ }
            else {
                errorMessage = "Shipment with ID '$idToTrack' not found."
            }
        }

        trackShipmentLambda(shipmentId)

        assertEquals(0, trackedShipments.size) // No shipment should be added
        assertNotNull(errorMessage) // Error message should be set
        assertTrue(errorMessage!!.contains("not found"))
        assertEquals(0, activeTrackers.size) // No active trackers
        assertEquals(1, mockTrackingSimulator.findShipmentCallCount) // findShipment should be called once
        assertEquals(shipmentId, mockTrackingSimulator.findShipmentCalledWith)
    }

    @Test
    fun `stopTracking should remove shipment from tracked list and observer`() = runTest {
        val shipmentId = "SHIP_TEST_002"
        val mockShipment = spyk(MockShipment(shipmentId)) // Spy on mock shipment
        mockShipment.status = "In Transit"
        mockTrackingSimulator.findShipmentResult = mockShipment // Setup for stopTracking to find shipment for removeObserver

        // Manually set up a tracked shipment as if trackShipment was already called
        val viewModel = TrackerViewHelper(shipmentId)
        viewModel.shipmentStatus = "In Transit" // Simulate it being tracked
        val tracker = Tracker(shipmentId, viewModel)

        // Ensure the mockShipment has the tracker as an observer initially
        mockShipment.addObserver(tracker) // Manually add observer for test setup

        val trackedShipments = mutableStateListOf(viewModel)
        val activeTrackers = mutableStateMapOf(shipmentId to tracker)

        val stopTrackingLambda: (String) -> Unit = { idToStopTracking ->
            val viewModelToRemove = trackedShipments.find { it.shipmentId == idToStopTracking }
            if (viewModelToRemove != null) {
                trackedShipments.remove(viewModelToRemove)
                val removedTracker = activeTrackers.remove(idToStopTracking)
                if (removedTracker != null) {
                    val shipment = mockTrackingSimulator.findShipment(idToStopTracking) // Calls mock
                    // Need to cast to MockShipment to call removeObserver method defined on mock
                    (shipment as MockShipment).removeObserver(removedTracker)
                }
            }
        }

        stopTrackingLambda(shipmentId)

        assertEquals(0, trackedShipments.size) // ViewModel removed from list
        assertEquals(0, activeTrackers.size) // Tracker removed from map

        // Verify that removeObserver was called on the mock shipment
        verify(exactly = 1) { mockShipment.removeObserver(tracker) }
        assertEquals(1, mockTrackingSimulator.findShipmentCallCount) // findShipment called once
        assertEquals(shipmentId, mockTrackingSimulator.findShipmentCalledWith)
    }

    @Test
    fun `trackShipment should not add if already tracking`() = runTest {
        val shipmentId = "SHIP_TEST_003"
        val mockShipment = MockShipment(shipmentId)
        mockShipment.status = "Created"
        mockTrackingSimulator.findShipmentResult = mockShipment

        val viewModel = TrackerViewHelper(shipmentId)
        viewModel.shipmentStatus = "Created"
        val tracker = Tracker(shipmentId, viewModel)

        val trackedShipments = mutableStateListOf(viewModel) // Already tracking this shipment
        val activeTrackers = mutableStateMapOf(shipmentId to tracker)
        var errorMessage by mutableStateOf<String?>(null)

        val trackShipmentLambda: (String) -> Unit = { idToTrack ->
            errorMessage = null
            val shipment = mockTrackingSimulator.findShipment(idToTrack)
            if (shipment != null) {
                if (trackedShipments.any { it.shipmentId == idToTrack }) {
                    errorMessage = "Shipment $idToTrack is already being tracked." // This path should be taken
                } else { /* ... should not reach here ... */ }
            } else { /* ... should not reach here ... */ }
        }

        trackShipmentLambda(shipmentId)

        assertEquals(1, trackedShipments.size) // Size should remain 1
        assertNotNull(errorMessage)
        assertTrue(errorMessage!!.contains("already being tracked"))
        assertEquals(1, mockTrackingSimulator.findShipmentCallCount) // findShipment should be called once
    }

    @Test
    fun `stopTracking should do nothing if shipment is not tracked`() = runTest {
        val shipmentId = "NON_TRACKED_SHIP"
        mockTrackingSimulator.findShipmentResult = null // Doesn't matter for this test, but good practice

        val trackedShipments = mutableStateListOf<TrackerViewHelper>() // Empty list
        val activeTrackers = mutableStateMapOf<String, Tracker>()
        var errorMessage by mutableStateOf<String?>(null)

        val stopTrackingLambda: (String) -> Unit = { idToStopTracking ->
            val viewModelToRemove = trackedShipments.find { it.shipmentId == idToStopTracking }
            if (viewModelToRemove != null) { /* ... remove logic ... */ }
            else {
                errorMessage = "Shipment $idToStopTracking is not currently tracked."
            }
        }

        stopTrackingLambda(shipmentId)

        assertEquals(0, trackedShipments.size)
        assertNotNull(errorMessage)
        assertTrue(errorMessage!!.contains("not currently tracked"))
        assertEquals(0, activeTrackers.size) // Should remain empty
        assertEquals(0, mockTrackingSimulator.findShipmentCallCount) // findShipment should not be called
    }
}