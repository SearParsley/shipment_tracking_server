package marcus.hansen

import kotlinx.coroutines.delay // Import for the delay function
import java.io.File

open class TrackingSimulator {
    private val shipments: MutableMap<String, Shipment> = mutableMapOf()

    // Initialize map with ALL concrete UpdateStrategy implementations
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
     *
     * @param filePath The path to the file containing shipment update data.
     */
    open suspend fun runSimulation(filePath: String) {
        println("TrackingSimulator: Starting comprehensive simulation from file: $filePath")
        val lines = try {
            File(filePath).readLines()
        } catch (e: Exception) {
            println("TrackingSimulator: Error reading file $filePath: ${e.message}")
            return
        }

        val updates = lines.mapNotNull { line ->
            try {
                ShippingUpdate.fromString(line)
            } catch (e: IllegalArgumentException) {
                println("TrackingSimulator: Skipping malformed update line: '$line' - ${e.message}")
                null
            } catch (e: NumberFormatException) {
                println("TrackingSimulator: Skipping update with invalid timestamp: '$line' - ${e.message}")
                null
            }
        }

        for (update in updates) {
            val shipmentId = update.shipmentId
            var shipment = shipments[shipmentId]

            // Handle "created" updates: create shipment if it doesn't exist
            if (update.updateType == "created") {
                if (shipment == null) {
                    shipment = Shipment(shipmentId)
                    addShipment(shipment)
                    println("TrackingSimulator: Created new shipment: $shipmentId")
                } else {
                    println("TrackingSimulator: Shipment $shipmentId already exists. Applying 'created' update again.")
                }
            } else if (shipment == null) {
                // For non-creation updates, if shipment doesn't exist, log and skip
                println("TrackingSimulator: Error: Update for non-existent shipment ID: $shipmentId (Type: ${update.updateType}). Skipping.")
                delay(1000L) // Still apply delay even if skipping, to simulate real-time processing
                continue
            }

            val strategy = updateStrategies[update.updateType]
            if (strategy != null && shipment != null) {
                strategy.update(shipment, update)
                println("TrackingSimulator: Processed '${update.updateType}' update for $shipmentId.")
            } else {
                println("TrackingSimulator: Strategy not found for type '${update.updateType}' or shipment is null.")
            }
            delay(1000L) // Apply 1-second delay as per requirement
        }
        println("TrackingSimulator: Comprehensive simulation finished.")
    }

    // getStrategy is now private and only used internally by runSimulation
    private fun getStrategy(updateType: String): UpdateStrategy? {
        return updateStrategies[updateType]
    }
}