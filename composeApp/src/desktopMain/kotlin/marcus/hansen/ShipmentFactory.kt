package marcus.hansen

object ShipmentFactory {
    fun createShipment(id: String, typeString: String, createdTimestamp: Long = System.currentTimeMillis()): Shipment {
        val type = try {
            ShipmentType.valueOf(typeString.uppercase())
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid shipment type string: $typeString. Must be STANDARD, EXPRESS, OVERNIGHT, or BULK.")
        }

        return when (type) {
            ShipmentType.STANDARD -> StandardShipment(id, createdTimestamp)
            ShipmentType.EXPRESS -> ExpressShipment(id, createdTimestamp)
            ShipmentType.OVERNIGHT -> OvernightShipment(id, createdTimestamp)
            ShipmentType.BULK -> BulkShipment(id, createdTimestamp)
        }
    }
}