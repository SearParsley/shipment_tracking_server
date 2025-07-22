package marcus.hansen

object ShipmentFactory {
    fun createShipment(id: String, typeString: String): Shipment {
        val type = try {
            ShipmentType.valueOf(typeString.uppercase())
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid shipment type string: $typeString. Must be STANDARD, EXPRESS, OVERNIGHT, or BULK.")
        }

        return when (type) {
            ShipmentType.STANDARD -> StandardShipment(id)
            ShipmentType.EXPRESS -> ExpressShipment(id)
            ShipmentType.OVERNIGHT -> OvernightShipment(id)
            ShipmentType.BULK -> BulkShipment(id)
        }
    }
}