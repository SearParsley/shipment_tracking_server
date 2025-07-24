package marcus.hansen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserInterface() {
    var shipmentIdInput by remember { mutableStateOf("") }
    val trackedShipments = remember { mutableStateListOf<TrackerViewHelper>() }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    val activeTrackers = remember { mutableStateMapOf<String, Tracker>() }

    val trackShipment: (String) -> Unit = { idToTrack ->
        errorMessage = null
        coroutineScope.launch {
            val shipment = TrackingServer.findShipment(idToTrack)
            if (shipment != null) {
                if (trackedShipments.any { it.shipmentId == idToTrack }) {
                    errorMessage = "Shipment $idToTrack is already being tracked."
                } else {
                    val viewModel = TrackerViewHelper(idToTrack)

                    val tracker = Tracker(idToTrack, viewModel)

                    shipment.addObserver(tracker)
                    activeTrackers[idToTrack] = tracker
                    tracker.update(shipment)
                    trackedShipments.add(viewModel)
                    shipmentIdInput = ""
                    println("UI: Started tracking shipment $idToTrack")
                }
            } else {
                errorMessage = "Shipment with ID '$idToTrack' not found."
                println("UI: Shipment $idToTrack not found.")
            }
        }
    }

    val stopTracking: (String) -> Unit = { idToStopTracking ->
        val viewModelToRemove = trackedShipments.find { it.shipmentId == idToStopTracking }
        if (viewModelToRemove != null) {
            trackedShipments.remove(viewModelToRemove)

            val tracker = activeTrackers.remove(idToStopTracking)

            if (tracker != null) {
                coroutineScope.launch {
                    val shipment = TrackingServer.findShipment(idToStopTracking)
                    shipment?.removeObserver(tracker)
                    println("UI: Stopped tracking shipment $idToStopTracking")
                }
            }
        } else {
            errorMessage = "Shipment $idToStopTracking is not currently tracked."
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Shipment Tracking Simulator") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField( // Material Design text field
                    value = shipmentIdInput,
                    onValueChange = { shipmentIdInput = it },
                    label = { Text("Shipment ID") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { trackShipment(shipmentIdInput) },
                    enabled = shipmentIdInput.isNotBlank()
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

            errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            if (trackedShipments.isEmpty()) {
                Text("No shipments currently tracked. Enter an ID above to start tracking.")
            } else {
                LazyColumn(
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

@Composable
fun ShipmentCard(viewModel: TrackerViewHelper, onStopTracking: (String) -> Unit) {
    Card(
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
                    Text("X", color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Status: ${viewModel.shipmentStatus}")
            Text("Location: ${viewModel.currentLocation ?: "N/A"}")
            Text("Expected Delivery: ${viewModel.expectedShipmentDeliveryDate ?: "N/A"}")

            if (viewModel.ruleViolations.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text("Rule Violations:", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error)
                viewModel.ruleViolations.forEach { violation ->
                    Text("  - $violation", color = MaterialTheme.colorScheme.error)
                }
            }

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