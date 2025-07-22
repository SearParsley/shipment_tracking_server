package marcus.hansen

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
    val copy = Shipment(this.id)
    copy.status = this.status
    copy.currentLocation = this.currentLocation
    copy.expectedDeliveryDateTimestamp = this.expectedDeliveryDateTimestamp
    this.getImmutableNotes().forEach { copy.addNote(it) }
    this.getImmutableUpdateHistory().forEach { copy.addUpdate(it) }
    return copy
}

/**
 * A mock implementation of Shipment for testing purposes.
 * It allows controlling its properties and verifying observer interactions.
 */
class MockShipment(override val id: String) : Shipment(id) {
    override var status: String = "Unknown"
    override var currentLocation: String? = null
    override var expectedDeliveryDateTimestamp: Long? = null
    private val _mockNotes = mutableListOf<String>()
    private val _mockUpdateHistory = mutableListOf<ShippingUpdate>()
    val mockObservers = mutableListOf<ShipmentObserver>()

    override fun addObserver(observer: ShipmentObserver) {
        mockObservers.add(observer)
    }

    override fun removeObserver(observer: ShipmentObserver) {
        mockObservers.remove(observer)
    }

    override fun notifyObservers() {

    }

    override fun getImmutableNotes(): List<String> = _mockNotes.toList()
    override fun getImmutableUpdateHistory(): List<ShippingUpdate> = _mockUpdateHistory.toList()

    fun addMockNote(note: String) { _mockNotes.add(note) }
    fun addMockUpdate(update: ShippingUpdate) { _mockUpdateHistory.add(update) }

    fun triggerObserverUpdate() {
        mockObservers.forEach { it.update(this) }
    }
}