package marcus.hansen

interface UpdateStrategy {
    /**
     * Applies a specific type of update to a Shipment object.
     * After applying the update, the strategy should call shipment.notifyObservers().
     *
     * @param shipment The Shipment object to be updated.
     * @param updateData The data containing the details of the update.
     */
    fun update(shipment: Shipment, update: ShippingUpdate)
}