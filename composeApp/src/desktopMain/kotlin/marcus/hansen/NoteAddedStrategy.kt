package marcus.hansen

class NoteAddedStrategy : UpdateStrategy {
    override fun update(shipment: Shipment, update: ShippingUpdate) {
        if (update.otherInfo.isNotEmpty()) {
            shipment.addNote(update.otherInfo[0]) // Using the public addNote method
        }
        shipment.addUpdate(update)
        shipment.notifyObservers()
    }
}