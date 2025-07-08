package marcus.hansen

class LocationStrategy : UpdateStrategy {
    override fun update(shipment: Shipment, update: ShippingUpdate) {
        if (update.otherInfo.isNotEmpty()) {
            shipment.currentLocation = update.otherInfo[0]
        }
        shipment.addUpdate(update)
        shipment.notifyObservers()
    }
}