package marcus.hansen

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

interface UpdateStrategy {
    /**
     * Applies a specific type of update to a Shipment object.
     * After applying the update, the strategy should call shipment.notifyObservers().
     *
     * @param shipment The Shipment object to be updated.
     * @param shippingUpdate The data containing the details of the update.
     */
    fun update(shipment: Shipment, shippingUpdate: ShippingUpdate)

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

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val creationDatetime = LocalDateTime.ofInstant(creationInstant, ZoneId.systemDefault()).format(formatter).toString()
        val expectedDeliveryDatetime = LocalDateTime.ofInstant(expectedDeliveryInstant, ZoneId.systemDefault()).format(formatter).toString()

        when (shipment.type) {
            ShipmentType.EXPRESS -> {
                if (daysDifference > 3) {
                    shipment.ruleViolations.add("Express shipment delivery date (${expectedDeliveryDatetime}) is more than 3 days after creation (${creationDatetime}).")
                }
            }
            ShipmentType.OVERNIGHT -> {
                if (updateType != "delayed" && daysDifference != 1L) {
                    shipment.ruleViolations.add("Overnight shipment delivery date (${expectedDeliveryDatetime}) is not exactly 1 day after creation (${creationDatetime}).")
                }
            }
            ShipmentType.BULK -> {
                if (daysDifference < 3) {
                    shipment.ruleViolations.add("Bulk shipment delivery date (${expectedDeliveryDatetime}) is sooner than 3 days after creation (${creationDatetime}).")
                }
            }
            ShipmentType.STANDARD -> {
                // no rules to follow
            }
        }
    }
}