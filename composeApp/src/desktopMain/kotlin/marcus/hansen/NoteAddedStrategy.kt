package marcus.hansen

class NoteAddedStrategy : UpdateStrategy {
    override fun update(shipment: Shipment, shippingUpdate: ShippingUpdate) {
        if (shippingUpdate.otherInfo.isNotEmpty()) {
            shipment.addNote(shippingUpdate.otherInfo[0])
        }
        shipment.addUpdate(shippingUpdate)
        shipment.notifyObservers()
    }
}