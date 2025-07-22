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
            val update = ShippingUpdate.fromString(updateString)
            val shipmentId = update.shipmentId
            var shipment = shipments[shipmentId]

            if (update.updateType == "created") {
                if (shipment == null) {
                    val shipmentTypeString = update.otherInfo.firstOrNull() ?: "Standard"
                    shipment = ShipmentFactory.createShipment(shipmentId, shipmentTypeString, update.timestamp)
                    addShipment(shipment)
                    println("Server: Created new shipment: $shipmentId (Type: ${shipment.type})")
                } else {
                    println("Server: Shipment $shipmentId already exists. Applying 'created' update again.")
                }
            } else if (shipment == null) {
                println("Server ERROR: Update for non-existent shipment ID: $shipmentId (Type: ${update.updateType}).")
                return "Error: Shipment $shipmentId not found."
            }

            val strategy = updateStrategies[update.updateType]
            if (strategy != null) {
                strategy.update(shipment, update)
                println("Server: Processed '${update.updateType}' update for $shipmentId.")
                return "Success: Update processed for $shipmentId."
            } else {
                println("Server ERROR: Strategy not found for type '${update.updateType}' or shipment is null.")
                return "Error: Unknown update type '${update.updateType}'."
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