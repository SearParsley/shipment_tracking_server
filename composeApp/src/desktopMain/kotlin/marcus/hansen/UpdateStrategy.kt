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

//    /**
//     * Validates shipment-specific rules based on the update type and expected delivery.
//     * If a rule is violated, a message is added to the shipment's ruleViolations list.
//     * This method has a default implementation, so only strategies that need to validate rules
//     * (e.g., Shipped, Delayed) need to override it or call it.
//     *
//     * @param shipment The Shipment object whose rules are being validated.
//     * @param currentUpdateTimestamp The timestamp of the current update being processed.
//     * @param expectedDeliveryTimestamp The new expected delivery timestamp.
//     * @param updateType The type of the current update (e.g., "shipped", "delayed").
//     */
//    fun validateShipmentRules(
//        shipment: Shipment,
//        currentUpdateTimestamp: Long, // Use this as the "creation" reference if needed, or get from shipment.createdTimestamp
//        expectedDeliveryTimestamp: Long,
//        updateType: String
//    ) {
//
//    }

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

            }
        }
    }
}