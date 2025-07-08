package marcus.hansen

class CreatedStrategy : UpdateStrategy {
    override fun update(shipment: Shipment, update: ShippingUpdate) {
        shipment.status = "Created"
        shipment.addUpdate(update)
        shipment.notifyObservers()
    }
}