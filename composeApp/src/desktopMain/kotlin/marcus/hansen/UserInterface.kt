package marcus.hansen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.* // Essential Compose runtime APIs (remember, mutableStateOf, etc.)
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch // For launching coroutines for suspend functions

@OptIn(ExperimentalMaterial3Api::class) // For OutlinedTextField
@Composable
fun UserInterface(trackingSimulator: TrackingSimulator) {
    // ---- UI State Management ----
    // These states are managed within the Composable's lifecycle (remember)
    // and trigger recomposition when their values change. This is standard Compose behavior.
    var shipmentIdInput by remember { mutableStateOf("") } // Input field text
    val trackedShipments = remember { mutableStateListOf<TrackerViewHelper>() } // List of items to display
    var errorMessage by remember { mutableStateOf<String?>(null) } // Error message display

    // Coroutine scope tied to the Composable's lifecycle.
    // Necessary for launching suspend functions like trackingSimulator.findShipment().
    val coroutineScope = rememberCoroutineScope()

    // Map to keep track of the Tracker instances. This allows us to remove observers later.
    // Uses mutableStateMapOf for Compose's observability.
    val activeTrackers = remember { mutableStateMapOf<String, Tracker>() }

    // ---- Event Handlers (Lambdas for UI Actions) ----

    // Function to handle tracking a shipment when the "Track" button is clicked.
    val trackShipment: (String) -> Unit = { idToTrack ->
        errorMessage = null // Clear any previous error message
        coroutineScope.launch { // Launch in a coroutine as findShipment might be suspend (or for good practice)
            val shipment = trackingSimulator.findShipment(idToTrack) // Interact with the core simulator logic
            if (shipment != null) {
                if (trackedShipments.any { it.shipmentId == idToTrack }) {
                    // Check if already tracking this shipment
                    errorMessage = "Shipment $idToTrack is already being tracked."
                } else {
                    // Create ViewModel and Tracker for the new tracked shipment
                    val viewModel = TrackerViewHelper(idToTrack)
                    val tracker = Tracker(idToTrack, viewModel)

                    shipment.addObserver(tracker) // Register the Tracker as an observer to the Shipment
                    activeTrackers[idToTrack] = tracker // Store the Tracker instance for later removal

                    // Immediately update the ViewModel with the shipment's current data for initial display
                    // This ensures the UI shows data before any further simulation updates occur.
                    tracker.update(shipment)

                    trackedShipments.add(viewModel) // Add the ViewModel to the observable list, triggering UI update
                    shipmentIdInput = "" // Clear the input field after tracking
                    println("UI: Started tracking shipment $idToTrack") // Console log for desktop debugging
                }
            } else {
                errorMessage = "Shipment with ID '$idToTrack' not found." // Display error for non-existent shipment
                println("UI: Shipment $idToTrack not found.") // Console log
            }
        }
    }

    // Function to handle stopping tracking a shipment when an "X" or "Stop Tracking" button is clicked.
    val stopTracking: (String) -> Unit = { idToStopTracking ->
        val viewModelToRemove = trackedShipments.find { it.shipmentId == idToStopTracking }
        if (viewModelToRemove != null) {
            trackedShipments.remove(viewModelToRemove) // Remove from UI's observable list

            val tracker = activeTrackers.remove(idToStopTracking) // Get and remove the Tracker instance
            if (tracker != null) {
                coroutineScope.launch {
                    val shipment = trackingSimulator.findShipment(idToStopTracking)
                    shipment?.removeObserver(tracker) // Remove the Tracker as an observer from the Shipment
                    println("UI: Stopped tracking shipment $idToStopTracking") // Console log
                }
            }
        } else {
            errorMessage = "Shipment $idToStopTracking is not currently tracked."
        }
    }

    // ---- Compose UI Layout ----
    Scaffold( // Basic Material Design layout structure
        topBar = {
            TopAppBar(title = { Text("Shipment Tracking Simulator") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier // Standard Compose modifier for layout
                .fillMaxSize() // Takes up all available space
                .padding(paddingValues) // Respects Scaffold's padding
                .padding(16.dp), // Adds internal padding
            horizontalAlignment = Alignment.CenterHorizontally // Centers content horizontally
        ) {
            // Input field and buttons
            Row(
                modifier = Modifier.fillMaxWidth(), // Fills width
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp) // Spacing between elements
            ) {
                OutlinedTextField( // Material Design text field
                    value = shipmentIdInput,
                    onValueChange = { shipmentIdInput = it },
                    label = { Text("Shipment ID") },
                    singleLine = true,
                    modifier = Modifier.weight(1f) // Takes remaining width in the row
                )
                Button( // Standard Material Design button
                    onClick = { trackShipment(shipmentIdInput) },
                    enabled = shipmentIdInput.isNotBlank() // Button enabled only if input is not blank
                ) {
                    Text("Track")
                }
                Button(
                    onClick = { stopTracking(shipmentIdInput) },
                    enabled = shipmentIdInput.isNotBlank()
                ) {
                    Text("Stop Tracking")
                }
            }

            // Error message display area
            errorMessage?.let { message -> // Only display if errorMessage is not null
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error, // Red error color
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Spacer(Modifier.height(16.dp)) // Vertical spacing

            // List of tracked shipments displayed in a scrollable column
            if (trackedShipments.isEmpty()) {
                Text("No shipments currently tracked. Enter an ID above to start tracking.")
            } else {
                LazyColumn( // Efficiently renders only visible items in a scrollable list
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(trackedShipments, key = { it.shipmentId }) { viewModel ->
                        ShipmentCard(viewModel = viewModel, onStopTracking = stopTracking)
                    }
                }
            }
        }
    }
}

// Composable for displaying a single shipment's details in a card format
@Composable
fun ShipmentCard(viewModel: TrackerViewHelper, onStopTracking: (String) -> Unit) {
    Card( // Material Design card component
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Shipment ID: ${viewModel.shipmentId}", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { onStopTracking(viewModel.shipmentId) }) {
                    Text("X", color = MaterialTheme.colorScheme.error) // Simple "X" button to stop tracking
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Status: ${viewModel.shipmentStatus}")
            Text("Location: ${viewModel.currentLocation ?: "N/A"}")
            Text("Expected Delivery: ${viewModel.expectedShipmentDeliveryDate ?: "N/A"}")

            if (viewModel.shipmentNotes.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text("Notes:", style = MaterialTheme.typography.titleSmall)
                viewModel.shipmentNotes.forEach { note ->
                    Text("  - $note")
                }
            }

            if (viewModel.shipmentUpdateHistory.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text("Update History:", style = MaterialTheme.typography.titleSmall)
                viewModel.shipmentUpdateHistory.forEach { update ->
                    Text("  - $update")
                }
            }
        }
    }
}