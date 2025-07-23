package marcus.hansen

class LocationStrategy : UpdateStrategy {
    override fun update(shipment: Shipment, shippingUpdate: ShippingUpdate) {
        if (shippingUpdate.otherInfo.isNotEmpty()) {
            shipment.currentLocation = shippingUpdate.otherInfo[0]
        }
        shipment.addUpdate(shippingUpdate)
        shipment.notifyObservers()
    }
}