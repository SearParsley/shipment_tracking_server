// TrackerViewHelper.kt
package marcus.hansen

import androidx.compose.runtime.getValue // Add this import for the 'by' delegate
import androidx.compose.runtime.mutableStateOf // Add this import for creating observable state
import androidx.compose.runtime.setValue // Add this import for the 'by' delegate

class TrackerViewHelper(val shipmentId: String) {

    var shipmentStatus: String by mutableStateOf("N/A")
    var currentLocation: String? by mutableStateOf(null)
    var expectedShipmentDeliveryDate: String? by mutableStateOf(null)
    var shipmentNotes: Array<String> by mutableStateOf(emptyArray())
    var shipmentUpdateHistory: Array<String> by mutableStateOf(emptyArray())
    
    internal fun reset() {
        shipmentStatus = "N/A"
        currentLocation = null
        expectedShipmentDeliveryDate = null
        shipmentNotes = emptyArray()
        shipmentUpdateHistory = emptyArray()
    }
}