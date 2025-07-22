package marcus.hansen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class TrackerViewHelper(val shipmentId: String) {

    var shipmentStatus: String by mutableStateOf("N/A")
    var currentLocation: String? by mutableStateOf(null)
    var expectedShipmentDeliveryDate: String? by mutableStateOf(null)
    var shipmentNotes: Array<String> by mutableStateOf(emptyArray())
    var shipmentUpdateHistory: Array<String> by mutableStateOf(emptyArray())
    var ruleViolations: Array<String> by mutableStateOf(emptyArray())

    internal fun reset() {
        shipmentStatus = "N/A"
        currentLocation = null
        expectedShipmentDeliveryDate = null
        shipmentNotes = emptyArray()
        shipmentUpdateHistory = emptyArray()
        ruleViolations = emptyArray()
    }
}