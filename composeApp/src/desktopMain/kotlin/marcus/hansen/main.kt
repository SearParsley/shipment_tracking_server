package marcus.hansen

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Shipment Tracking Server & Client") {
        UserInterface()
    }
}