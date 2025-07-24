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

    fun addShipment(shipment: Shipment) {
        shipments[shipment.id] = shipment
    }

    fun findShipment(id: String): Shipment? {
        return shipments[id]
    }

    fun processUpdateString(updateString: String): String {
        try {
            val shippingUpdate = ShippingUpdate.fromString(updateString)
            val shipmentId = shippingUpdate.shipmentId
            var shipment = shipments[shipmentId]

            if (shippingUpdate.updateType == "created") {
                if (shipment == null) {
                    val shipmentTypeString = shippingUpdate.otherInfo.firstOrNull() ?: "Standard"
                    shipment = ShipmentFactory.createShipment(shipmentId, shipmentTypeString, shippingUpdate.timestamp)
                    addShipment(shipment)
                    println("Server: Created new shipment: $shipmentId (Type: ${shipment.type})")
                } else {
                    println("Server: Shipment $shipmentId already exists. Applying 'created' update again.")
                }
            } else if (shipment == null) {
                println("Server ERROR: Update for non-existent shipment ID: $shipmentId (Type: ${shippingUpdate.updateType}).")
                return "Error: Shipment $shipmentId not found."
            }

            val strategy = updateStrategies[shippingUpdate.updateType]
            if (strategy != null) {
                strategy.update(shipment, shippingUpdate)
                println("Server: Processed '${shippingUpdate.updateType}' update for $shipmentId.")
                return "Success: Update processed for $shipmentId."
            } else {
                println("Server ERROR: Strategy not found for type '${shippingUpdate.updateType}' or shipment is null.")
                return "Error: Unknown update type '${shippingUpdate.updateType}'."
            }
        } catch (e: Exception) {
            println("Server ERROR: Failed to process update string '$updateString': ${e.message}")
            return "Error processing update: ${e.message}"
        }
    }

    internal fun resetForTesting() { shipments.clear() }
}