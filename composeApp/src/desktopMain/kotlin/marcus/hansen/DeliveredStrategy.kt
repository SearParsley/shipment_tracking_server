package marcus.hansen

class DeliveredStrategy : UpdateStrategy {
    override fun update(shipment: Shipment, update: ShippingUpdate) {
        shipment.status = "Delivered"
        shipment.expectedDeliveryDateTimestamp = null
        shipment.addUpdate(update)
        shipment.notifyObservers()
    }
}