package marcus.hansen

class CancelledStrategy : UpdateStrategy {
    override fun update(shipment: Shipment, shippingUpdate: ShippingUpdate) {
        shipment.status = "Canceled"
        shipment.addUpdate(shippingUpdate)
        shipment.notifyObservers()
    }
}