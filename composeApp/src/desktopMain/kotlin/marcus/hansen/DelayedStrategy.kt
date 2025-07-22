package marcus.hansen

class DelayedStrategy : UpdateStrategy {
    override fun update(shipment: Shipment, update: ShippingUpdate) {
        shipment.status = "Delayed"
        if (update.otherInfo.isNotEmpty()) {
            try {
                val newExpectedTimestamp = update.otherInfo[0].toLong()
                try {
                    shipment.expectedDeliveryDateTimestamp = newExpectedTimestamp
                    performRuleValidation(shipment, newExpectedTimestamp, update.updateType)
                } catch (e: Exception) {
                    shipment.ruleViolations.clear()
                }
            } catch (e: NumberFormatException) {
                System.err.println("DelayedStrategy: Invalid timestamp format in otherInfo for shipment ${shipment.id}: ${update.otherInfo[0]}")
            }
        }
        shipment.addUpdate(update)
        shipment.notifyObservers()
    }
}