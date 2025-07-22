package marcus.hansen

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

interface UpdateStrategy {
    /**
     * Applies a specific type of update to a Shipment object.
     * After applying the update, the strategy should call shipment.notifyObservers().
     *
     * @param shipment The Shipment object to be updated.
     * @param update The data containing the details of the update.
     */
    fun update(shipment: Shipment, update: ShippingUpdate)

    /**
     * Helper function to perform the actual rule validation logic.
     * This can be called by specific strategies.
     * It clears existing violations and then checks for new ones.
     *
     * @param shipment The Shipment object to validate.
     * @param expectedDeliveryTimestamp The timestamp of the expected delivery date.
     * @param updateType The type of update being applied (e.g., "shipped", "delayed").
     */
    fun performRuleValidation(
        shipment: Shipment,
        expectedDeliveryTimestamp: Long,
        updateType: String
    ) {
        shipment.ruleViolations.clear()

        val creationInstant = Instant.ofEpochMilli(shipment.createdTimestamp)
        val expectedDeliveryInstant = Instant.ofEpochMilli(expectedDeliveryTimestamp)

        val creationDate = LocalDate.ofInstant(creationInstant, ZoneId.systemDefault())
        val expectedDeliveryDate = LocalDate.ofInstant(expectedDeliveryInstant, ZoneId.systemDefault())

        val daysDifference = Duration.between(creationDate.atStartOfDay(ZoneId.systemDefault()).toInstant(),
            expectedDeliveryDate.atStartOfDay(ZoneId.systemDefault()).toInstant()).toDays()

        when (shipment.type) {
            ShipmentType.EXPRESS -> {
                if (daysDifference > 3) {
                    shipment.ruleViolations.add("Express shipment delivery date (${expectedDeliveryInstant}) is more than 3 days after creation (${creationInstant}).")
                }
            }
            ShipmentType.OVERNIGHT -> {
                if (updateType != "delayed" && daysDifference != 1L) {
                    shipment.ruleViolations.add("Overnight shipment delivery date (${expectedDeliveryInstant}) is not exactly 1 day after creation (${creationInstant}).")
                }
            }
            ShipmentType.BULK -> {
                if (daysDifference < 3) {
                    shipment.ruleViolations.add("Bulk shipment delivery date (${expectedDeliveryInstant}) is sooner than 3 days after creation (${creationInstant}).")
                }
            }
            ShipmentType.STANDARD -> {
                // no rules to follow
            }
        }
    }
}