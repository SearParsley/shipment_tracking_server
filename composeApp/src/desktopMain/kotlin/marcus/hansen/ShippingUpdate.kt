package marcus.hansen

data class ShippingUpdate(
    val updateType: String,      // e.g., "created", "shipped", "location"
    val shipmentId: String,      // The ID of the shipment being updated
    val timestamp: Long,         // The timestamp of when this update occurred
    val otherInfo: List<String> = emptyList() // Optional additional information
) {
    companion object {
        /**
         * Factory method to parse a single line from the input file into a ShippingUpdate object.
         * Expected format: "updateType,shipmentId,timestampOfUpdate,otherInfo (optional)"
         *
         * @param line The string line to parse.
         * @return A new ShippingUpdate object.
         */
        fun fromString(line: String): ShippingUpdate {
            val parts = line.split(",")
            if (parts.size < 3) {
                throw IllegalArgumentException("Invalid update line format: $line")
            }
            val updateType = parts[0].trim()
            val shipmentId = parts[1].trim()
            val timestamp = parts[2].trim().toLong()
            val otherInfo = if (parts.size > 3) parts.subList(3, parts.size).map { it.trim() } else emptyList()
            return ShippingUpdate(updateType, shipmentId, timestamp, otherInfo)
        }
    }
}