package marcus.hansen

class Tracker(
    private val trackedShipmentId: String,
    var lastReceivedStatus: String? = null,
    var lastReceivedLocation: String? = null,
    var lastReceivedExpectedDelivery: Long? = null,
    var lastReceivedNotes: List<String> = emptyList(),
    var lastReceivedUpdateHistory: List<ShippingUpdate> = emptyList()
) : ShipmentObserver {

    /**
     * Called by the observed Shipment when its state changes.
     * This method pulls the latest data from the Shipment and stores it internally.
     *
     * @param shipment The Shipment object that has been updated.
     */
    override fun update(shipment: Shipment) {
        if (shipment.id == trackedShipmentId) {
            lastReceivedStatus = shipment.status
            lastReceivedLocation = shipment.currentLocation
            lastReceivedExpectedDelivery = shipment.expectedDeliveryDateTimestamp
            lastReceivedNotes = shipment.getImmutableNotes()
            lastReceivedUpdateHistory = shipment.getImmutableUpdateHistory()

            // initial verification
            println("Tracker for ${shipment.id} received update:")
            println("    Status: ${shipment.status}")
            println("    Location: ${shipment.currentLocation ?: "N/A"}")
            println("    Expected Delivery: ${shipment.expectedDeliveryDateTimestamp ?: "N/A"}")
            println("    Notes Count: ${shipment.getImmutableNotes().size}")
            println("    Update History Count: ${shipment.getImmutableUpdateHistory().size}")
        } else {
            println("Tracker: Received update for unexpected shipment ID: ${shipment.id}. Expected: $trackedShipmentId")
        }
    }
}