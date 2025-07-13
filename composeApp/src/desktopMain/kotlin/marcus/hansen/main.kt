package marcus.hansen

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect

fun main() = application {
    // Create a single instance of the TrackingSimulator for the application lifetime
    val trackingSimulator = remember { TrackingSimulator() } // Use remember to retain instance across recompositions

    // Launch the simulation in a background coroutine when the application starts
    // Use Dispatchers.IO for file operations to avoid blocking the UI thread
    // This is launched from the main application scope, not per-Composable.
    LaunchedEffect(trackingSimulator) { // LaunchedEffect ensures it runs once when trackingSimulator is stable
        // Replace "updates.txt" with the actual path to your simulation file.
        // This file should be accessible from where your JAR runs (e.g., project root).
        launch(Dispatchers.IO) {
            trackingSimulator.runSimulation("updates.txt")
        }
    }

    Window(onCloseRequest = ::exitApplication, title = "Shipment Tracking Simulator") {
        // Call your main UI Composable, passing the simulator instance
        UserInterface(trackingSimulator)
    }
}