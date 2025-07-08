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
            // Split the line by the first 3 commas to separate fixed fields from potential 'otherInfo'
            // Using limit = 4 means it will split into at most 4 parts:
            // [updateType, shipmentId, timestamp, rest_of_line_as_one_string]
            val parts = line.split(",", limit = 4)

            if (parts.size < 3) {
                throw IllegalArgumentException("Invalid update line format (missing type, ID, or timestamp): $line")
            }

            val updateType = parts[0].trim()
            val shipmentId = parts[1].trim()
            val timestamp = parts[2].trim().toLong()

            val otherInfoList: List<String>

            // Check if there's a 4th part which would be the 'otherInfo'
            if (parts.size == 4) {
                val rawOtherInfo = parts[3].trim()
                otherInfoList = when (updateType) {
                    "noteadded" -> listOf(rawOtherInfo) // Treat entire string as one note
                    else -> listOf(rawOtherInfo)
                }
            } else {
                otherInfoList = emptyList()
            }

            return ShippingUpdate(updateType, shipmentId, timestamp, otherInfoList)
        }
    }
}