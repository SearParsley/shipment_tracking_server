package marcus.hansen

class CreatedStrategy : UpdateStrategy {
    override fun update(shipment: Shipment, shippingUpdate: ShippingUpdate) {
        shipment.status = "Created"
        shipment.addUpdate(shippingUpdate)
        shipment.notifyObservers()
    }
}