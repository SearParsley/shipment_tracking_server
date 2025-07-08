package marcus.hansen

interface ShipmentObserver {
    /**
     * Called by a Shipment (Subject) when its state changes.
     * The observer should pull the necessary updated information from the provided shipment object.
     *
     * @param shipment The Shipment object whose state has changed.
     */
    fun update(shipment: Shipment)
}