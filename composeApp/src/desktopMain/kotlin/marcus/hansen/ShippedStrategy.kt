package marcus.hansen

class ShippedStrategy : UpdateStrategy {
    override fun update(shipment: Shipment, update: ShippingUpdate) {
        shipment.status = "Shipped"
        if (update.otherInfo.isNotEmpty()) {
            shipment.expectedDeliveryDateTimestamp = update.otherInfo[0].toLong()
        }
        shipment.addUpdate(update)
        shipment.notifyObservers()
    }
}