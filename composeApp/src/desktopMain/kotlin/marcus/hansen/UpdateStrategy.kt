package marcus.hansen

interface UpdateStrategy {
    fun update(shipment: Shipment, update: ShippingUpdate)
}