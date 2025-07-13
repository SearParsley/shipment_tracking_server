package marcus.hansen

import kotlinx.coroutines.delay
import java.io.File

open class TrackingSimulator {
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

    open fun findShipment(id: String): Shipment? {
        return shipments[id]
    }

    /**
     * Runs the simulation by reading update data from a file and processing each update.
     * Includes all strategy types and applies a 1-second delay between updates.
     * Added logging for file loading debug.
     *
     * @param filePath The path to the file containing shipment update data.
     */
    open suspend fun runSimulation(filePath: String) {
        val file = File(filePath)
        println("Tracking Simulator: Attempting to load updates from: ${file.absolutePath}")
        if (!file.exists()) {
            System.err.println("Tracking Simulator ERROR: File not found at path: ${file.absolutePath}")
            System.err.println("Tracking Simulator: Current working directory: ${System.getProperty("user.dir")}")
            return
        }
        println("Tracking Simulator: File found. Starting comprehensive simulation.")

        val lines = try {
            file.readLines()
        } catch (e: Exception) {
            System.err.println("Tracking Simulator ERROR: Failed to read file $filePath: ${e.message}")
            return
        }

        val updates = lines.mapNotNull { line ->
            try {
                ShippingUpdate.fromString(line)
            } catch (e: IllegalArgumentException) {
                System.err.println("Tracking Simulator: Skipping malformed update line: '$line' - ${e.message}")
                null
            } catch (e: NumberFormatException) {
                System.err.println("Tracking Simulator: Skipping update with invalid timestamp: '$line' - ${e.message}")
                null
            }
        }

        for (update in updates) {
            val shipmentId = update.shipmentId
            var shipment = shipments[shipmentId]

            if (update.updateType == "created") {
                if (shipment == null) {
                    shipment = Shipment(shipmentId)
                    addShipment(shipment)
                    println("Tracking Simulator: Created new shipment: $shipmentId")
                } else {
                    println("Tracking Simulator: Shipment $shipmentId already exists. Applying 'created' update again.")
                }
            } else if (shipment == null) {
                println("Tracking Simulator: Error: Update for non-existent shipment ID: $shipmentId (Type: ${update.updateType}). Skipping.")
                delay(1000L)
                continue
            }

            val strategy = updateStrategies[update.updateType]
            if (strategy != null) {
                strategy.update(shipment, update)
                println("Tracking Simulator: Processed '${update.updateType}' update for $shipmentId.")
            } else {
                println("Tracking Simulator: Strategy not found for type '${update.updateType}' or shipment is null.")
            }
            delay(1000L)
        }
        println("Tracking Simulator: Comprehensive simulation finished.")
    }

    private fun getStrategy(updateType: String): UpdateStrategy? {
        return updateStrategies[updateType]
    }
}