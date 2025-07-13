package marcus.hansen

data class ShippingUpdate(
    val updateType: String,
    val shipmentId: String,
    val timestamp: Long,
    val otherInfo: List<String> = emptyList()
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
            val parts = line.split(",", limit = 4)

            if (parts.size < 3) {
                throw IllegalArgumentException("Invalid update line format (missing type, ID, or timestamp): $line")
            }

            val updateType = parts[0].trim()
            val shipmentId = parts[1].trim()
            val timestamp = parts[2].trim().toLong()

            val otherInfoList: List<String>

            if (parts.size == 4) {
                val rawOtherInfo = parts[3].trim()
                otherInfoList = when (updateType) {
                    "noteadded" -> listOf(rawOtherInfo)
                    else -> listOf(rawOtherInfo)
                }
            } else {
                otherInfoList = emptyList()
            }

            return ShippingUpdate(updateType, shipmentId, timestamp, otherInfoList)
        }
    }
}