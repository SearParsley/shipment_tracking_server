package marcus.hansen

class DeliveredStrategy : UpdateStrategy {
    override fun update(shipment: Shipment, shippingUpdate: ShippingUpdate) {
        shipment.status = "Delivered"
        shipment.expectedDeliveryDateTimestamp = null
        shipment.addUpdate(shippingUpdate)
        shipment.notifyObservers()
    }
}