package marcus.hansen

class CancelledStrategy : UpdateStrategy {
    override fun update(shipment: Shipment, update: ShippingUpdate) {
        shipment.status = "Canceled"
        shipment.addUpdate(update)
        shipment.notifyObservers()
    }
}