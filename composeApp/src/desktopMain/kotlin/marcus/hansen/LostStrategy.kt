package marcus.hansen

class LostStrategy : UpdateStrategy {
    override fun update(shipment: Shipment, update: ShippingUpdate) {
        shipment.status = "Lost"
        shipment.addUpdate(update)
        shipment.notifyObservers()
    }
}