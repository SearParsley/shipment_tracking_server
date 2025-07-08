package marcus.hansen

class DelayedStrategy : UpdateStrategy {
    override fun update(shipment: Shipment, update: ShippingUpdate) {
        shipment.status = "Delayed"
        if (update.otherInfo.isNotEmpty()) {
            try {
                shipment.expectedDeliveryDateTimestamp = update.otherInfo[0].toLong()
            } catch (e: NumberFormatException) {
                System.err.println("DelayedStrategy: Invalid timestamp format in otherInfo for shipment ${shipment.id}: ${update.otherInfo[0]}")
            }
        }
        shipment.addUpdate(update)
        shipment.notifyObservers()
    }
}