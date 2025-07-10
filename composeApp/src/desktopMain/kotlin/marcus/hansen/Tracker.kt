package marcus.hansen

import java.util.Locale
import java.util.Locale.getDefault

class Tracker(
    private val trackedShipmentId: String,
    private val trackerViewModel: TrackerViewHelper
) : ShipmentObserver {

    /**
     * Called by the observed Shipment when its state changes.
     * This method pulls the latest data from the Shipment and updates the TrackerViewHelper.
     *
     * @param shipment The Shipment object that has been updated.
     */
    override fun update(shipment: Shipment) {
        if (shipment.id == trackedShipmentId) {
            trackerViewModel.shipmentStatus = shipment.status
            trackerViewModel.currentLocation = shipment.currentLocation
            // Convert Long timestamp to String for display as per TrackerViewHelper's type
            trackerViewModel.expectedShipmentDeliveryDate = shipment.expectedDeliveryDateTimestamp?.toString()
            trackerViewModel.shipmentNotes = shipment.getImmutableNotes().toTypedArray()

            // Format update history for display. This might need more sophisticated logic
            // to achieve the "Shipment went from {previousStatus} to {newStatus} on {dateOfUpdate}" format.
            // For now, a simpler representation of each update in history.
            trackerViewModel.shipmentUpdateHistory = shipment.getImmutableUpdateHistory().map { update ->
                "${update.updateType.replaceFirstChar { if (it.isLowerCase()) it.titlecase(getDefault()) else it.toString() }} on ${update.timestamp} (ID: ${update.shipmentId})"
                // Further formatting for detailed updates can be added here
            }.toTypedArray()
        } else {
            // This case should ideally not happen if observer registration is managed correctly
            // but is useful for debugging if an observer somehow gets an unrelated notification.
             println("Tracker: Received update for unexpected shipment ID: ${shipment.id}. Expected: $trackedShipmentId (Correctly ignored)")
        }
    }
}