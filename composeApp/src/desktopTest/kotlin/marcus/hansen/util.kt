package marcus.hansen

import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// Helper class to act as a mock observer for testing notifyObservers()
internal class MockShipmentObserver : ShipmentObserver {
    /**
     * Flag to indicate if the update method was called.
     */
    var updateCalled = false

    /**
     * Stores the last Shipment object received by the update method.
     */
    var receivedShipment: Shipment? = null

    /**
     * Overrides the update method from the ShipmentObserver interface.
     * Sets flags and stores the received Shipment for assertion in tests.
     * @param shipment The Shipment object that notified this observer.
     */
    override fun update(shipment: Shipment) {
        updateCalled = true
        receivedShipment = shipment
    }

    /**
     * Helper function to reset the state of the observer.
     * Useful if you're reusing the same observer instance across multiple test assertions
     * within a single test method (though creating new instances per assertion is often clearer).
     */
    fun reset() {
        updateCalled = false
        receivedShipment = null
    }
}

internal class MockComplexShipmentObserver : ShipmentObserver {
    var updateCalledCount = 0
    var lastReceivedShipment: Shipment? = null
    var receivedUpdatesDuringSimulation: MutableList<Shipment> = mutableListOf()

    /**
     * Overrides the update method from the ShipmentObserver interface.
     * Sets flags and stores the received Shipment for assertion in tests.
     * @param shipment The Shipment object that notified this observer.
     */
    override fun update(shipment: Shipment) {
        updateCalledCount++
        lastReceivedShipment = shipment
        receivedUpdatesDuringSimulation.add(shipment.copyStateForTest())
    }

    /**
     * Helper function to reset the state of the observer.
     * Useful if you're reusing the same observer instance across multiple test assertions
     * within a single test method (though creating new instances per assertion is often clearer).
     */
    fun reset() {
        updateCalledCount = 0
        lastReceivedShipment = null
        receivedUpdatesDuringSimulation.clear()
    }
}

/**
 * Creates a deep copy of the current state of this Shipment object.
 * This is used in tests (e.g., by MockShipmentObserver) to capture
 * a snapshot of the object's state at a specific point in time,
 * especially when the original object is mutable and changes over time.
 *
 * @return A new Shipment object with the same ID and copied state properties.
 */
internal fun Shipment.copyStateForTest(): Shipment {
    val copy = Shipment(this.id) // Create a new Shipment instance with the same ID
    copy.status = this.status // Copy status
    copy.currentLocation = this.currentLocation // Copy current location
    copy.expectedDeliveryDateTimestamp = this.expectedDeliveryDateTimestamp // Copy expected delivery timestamp

    // Copy the contents of notes list using addNote
    this.getImmutableNotes().forEach { copy.addNote(it) }

    // Copy the contents of updateHistory list using addUpdate
    this.getImmutableUpdateHistory().forEach { copy.addUpdate(it) }

    return copy
}

/**
 * A mock implementation of Shipment for testing purposes.
 * It allows controlling its properties and verifying observer interactions.
 */
class MockShipment(override val id: String) : Shipment(id) {
    // Override properties to make them settable for mocking purposes within the test
    override var status: String = "Unknown"
        internal set
    override var currentLocation: String? = null
        internal set
    override var expectedDeliveryDateTimestamp: Long? = null
        internal set

    // Mock the internal lists and methods as well if needed for specific tests
    private val _mockNotes = mutableListOf<String>()
    private val _mockUpdateHistory = mutableListOf<ShippingUpdate>()

    // For simplicity, we'll use a direct list of mock observers to verify calls
    val mockObservers = mutableListOf<ShipmentObserver>()

    override fun addObserver(observer: ShipmentObserver) {
        mockObservers.add(observer)
        // Simulate initial update to observer if that's the test's intent
        // observer.update(this) // Uncomment if addObserver should immediately notify
    }

    override fun removeObserver(observer: ShipmentObserver) {
        mockObservers.remove(observer)
    }

    // In a mock, notifyObservers might be spied on or just ensure observers are present
    // The actual update logic is handled by the real Tracker
    override fun notifyObservers() {
        // For tests, you might spy on this, or ensure it's called.
        // The MockShipmentObserver used by Tracker already verifies update() call.
    }

    // Ensure mock getters return controlled values if needed, or rely on super for basic tests
    override fun getImmutableNotes(): List<String> = _mockNotes.toList()
    override fun getImmutableUpdateHistory(): List<ShippingUpdate> = _mockUpdateHistory.toList()

    // Helpers to populate mock state for tests
    fun addMockNote(note: String) { _mockNotes.add(note) }
    fun addMockUpdate(update: ShippingUpdate) { _mockUpdateHistory.add(update) }

    // Helper for tests to manually trigger an update notification on *this mock*
    // This is useful for simulating a Shipment changing its state and notifying its direct observers.
    fun triggerObserverUpdate() {
        mockObservers.forEach { it.update(this) }
    }
}

/**
 * A mock implementation of TrackingSimulator for testing purposes.
 * It allows controlling the behavior of findShipment().
 */
class MockTrackingSimulator : TrackingSimulator() {
    var findShipmentResult: Shipment? = null // What findShipment will return
    var findShipmentCalledWith: String? = null
    var findShipmentCallCount = 0

    // Override findShipment to control its return value for tests
    override fun findShipment(id: String): Shipment? {
        findShipmentCalledWith = id
        findShipmentCallCount++
        return findShipmentResult
    }

    // Mock runSimulation since it involves file I/O and delays, not relevant for UI logic tests directly
    override suspend fun runSimulation(filePath: String) {
        // Do nothing or mock specific behavior if needed for UI integration
    }
}