package marcus.hansen

import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// Helper class to act as a mock observer for testing notifyObservers()
internal class MockShipmentObserver : ShipmentObserver {
    var updateCalled = false
    var receivedShipment: Shipment? = null
    override fun update(shipment: Shipment) {
        updateCalled = true
        receivedShipment = shipment
    }

    // Helper to reset observer state between tests if needed, though new instance per test is usually better
    fun reset() {
        updateCalled = false
        receivedShipment = null
    }
}