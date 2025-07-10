package marcus.hansen

class TrackerViewHelper(val shipmentId: String) {

    var shipmentStatus: String = "N/A"
        internal set
    var currentLocation: String? = null
        internal set
    var expectedShipmentDeliveryDate: String? = null // Stored as String for display
        internal set
    var shipmentNotes: Array<String> = emptyArray()
        internal set
    var shipmentUpdateHistory: Array<String> = emptyArray()
        internal set

    internal fun reset() {
        shipmentStatus = "N/A"
        currentLocation = null
        expectedShipmentDeliveryDate = null
        shipmentNotes = emptyArray()
        shipmentUpdateHistory = emptyArray()
    }
}