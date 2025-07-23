package marcus.hansen

class LostStrategy : UpdateStrategy {
    override fun update(shipment: Shipment, shippingUpdate: ShippingUpdate) {
        shipment.status = "Lost"
        shipment.addUpdate(shippingUpdate)
        shipment.notifyObservers()
    }
}