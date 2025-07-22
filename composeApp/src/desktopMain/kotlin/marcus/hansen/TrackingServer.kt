package marcus.hansen

object TrackingServer {
    private val shipments: MutableMap<String, Shipment> = mutableMapOf()

    private val updateStrategies: Map<String, UpdateStrategy> = mapOf(
        "created" to CreatedStrategy(),
        "shipped" to ShippedStrategy(),
        "location" to LocationStrategy(),
        "delivered" to DeliveredStrategy(),
        "delayed" to DelayedStrategy(),
        "lost" to LostStrategy(),
        "canceled" to CancelledStrategy(),
        "noteadded" to NoteAddedStrategy()
    )

    private val shipmentFactory: ShipmentFactory = ShipmentFactory

    fun addShipment(shipment: Shipment) {
        shipments[shipment.id] = shipment
    }

    fun findShipment(id: String): Shipment? {
        return shipments[id]
    }

    fun processUpdateString(updateString: String): String {
        try {
            val updateData = ShippingUpdate.fromString(updateString)
            val shipmentId = updateData.shipmentId
            var shipment = shipments[shipmentId]

            if (updateData.updateType == "created") {
                if (shipment == null) {
                    val shipmentTypeString = updateData.otherInfo.firstOrNull() ?: "Standard"
                    shipment = ShipmentFactory.createShipment(shipmentId, shipmentTypeString)
                    addShipment(shipment)
                    println("Server: Created new shipment: $shipmentId (Type: ${shipment.type})")
                } else {
                    println("Server: Shipment $shipmentId already exists. Applying 'created' update again.")
                }
            } else if (shipment == null) {
                println("Server ERROR: Update for non-existent shipment ID: $shipmentId (Type: ${updateData.updateType}).")
                return "Error: Shipment $shipmentId not found."
            }

            val strategy = updateStrategies[updateData.updateType]
            if (strategy != null) {
                strategy.update(shipment, updateData)
                println("Server: Processed '${updateData.updateType}' update for $shipmentId.")
                return "Success: Update processed for $shipmentId."
            } else {
                println("Server ERROR: Strategy not found for type '${updateData.updateType}' or shipment is null.")
                return "Error: Unknown update type '${updateData.updateType}'."
            }
        } catch (e: Exception) {
            println("Server ERROR: Failed to process update string '$updateString': ${e.message}")
            return "Error processing update: ${e.message}"
        }
    }

    private fun getStrategy(updateType: String): UpdateStrategy? {
        return updateStrategies[updateType]
    }

    internal fun resetForTesting() { shipments.clear() }
}