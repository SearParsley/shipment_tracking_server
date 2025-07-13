package marcus.hansen

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect

fun main() = application {
    val trackingSimulator = remember { TrackingSimulator() }

    LaunchedEffect(trackingSimulator) {
        launch(Dispatchers.IO) {
            trackingSimulator.runSimulation("updates.txt")
        }
    }

    Window(onCloseRequest = ::exitApplication, title = "Shipment Tracking Simulator") {
        UserInterface(trackingSimulator)
    }
}