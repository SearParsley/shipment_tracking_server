package marcus.hansen

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class Tracker(
    private val trackedShipmentId: String,
    private val trackerViewModel: TrackerViewHelper
) : ShipmentObserver {

    /**
     * Called by the observed Shipment when its state changes.
     * This method pulls the latest data from the Shipment and updates the TrackerViewHelper.
     * It now formats the timestamp to a human-readable form.
     *
     * @param shipment The Shipment object that has been updated.
     */
    override fun update(shipment: Shipment) {
        if (shipment.id == trackedShipmentId) {
            trackerViewModel.shipmentStatus = shipment.status
            trackerViewModel.currentLocation = shipment.currentLocation
            trackerViewModel.expectedShipmentDeliveryDate = shipment.expectedDeliveryDateTimestamp?.let { timestamp ->
                val instant = Instant.ofEpochMilli(timestamp)
                val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                dateTime.format(formatter)
            } ?: "N/A"
            trackerViewModel.shipmentNotes = shipment.getImmutableNotes().toTypedArray()
            trackerViewModel.shipmentUpdateHistory = shipment.getImmutableUpdateHistory().map { update ->
                val formattedTimestamp = Instant.ofEpochMilli(update.timestamp)
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                "${update.updateType.capitalize()} on $formattedTimestamp (ID: ${update.shipmentId})"
            }.toTypedArray()

        } else {
            // Log this case if an observer somehow gets an unrelated notification (for debugging)
        }
    }
}