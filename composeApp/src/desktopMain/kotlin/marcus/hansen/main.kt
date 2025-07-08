package marcus.hansen

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "shipment_tracking_simulator",
    ) {
        App()
    }
}