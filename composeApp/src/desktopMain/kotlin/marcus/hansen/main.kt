package marcus.hansen

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    val trackingServer = TrackingServer

    Window(onCloseRequest = ::exitApplication, title = "Shipment Tracking Server & Client") {
        UserInterface(trackingServer)
    }
}