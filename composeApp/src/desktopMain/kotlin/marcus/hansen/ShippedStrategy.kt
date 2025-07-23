package marcus.hansen

class ShippedStrategy : UpdateStrategy {
    override fun update(shipment: Shipment, shippingUpdate: ShippingUpdate) {
        shipment.status = "Shipped"
        if (shippingUpdate.otherInfo.isNotEmpty()) {
            try {
                val newExpectedTimestamp = shippingUpdate.otherInfo[0].toLong()
                try {
                    shipment.expectedDeliveryDateTimestamp = newExpectedTimestamp
                    performRuleValidation(shipment, newExpectedTimestamp, shippingUpdate.updateType)
                } catch (e: Exception) {
                    shipment.ruleViolations.clear()
                }
            } catch (e: NumberFormatException) {
                System.err.println("ShippedStrategy: Invalid timestamp format in otherInfo for shipment ${shipment.id}: ${shippingUpdate.otherInfo[0]}")
            }
        }
        shipment.addUpdate(shippingUpdate)
        shipment.notifyObservers()
    }
}