package marcus.hansen

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import java.io.File // For initial_updates.txt loading

// Ktor Server Imports
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation

fun main() = application {
    LaunchedEffect(Unit) {
        launch(Dispatchers.IO) {
            try {
                embeddedServer(Netty, port = 8080, host = "127.0.0.1") {
                    install(ContentNegotiation) {
                        json()
                    }
                    routing {
                        post("/update-shipment") {
                            val updateString = call.receiveText()
                            println("KtorServer: Received update request: $updateString")
                            val responseMessage = TrackingServer.processUpdateString(updateString)
                            call.respondText(responseMessage)
                        }
                        get("/status") {
                            call.respondText("Shipment Tracking Server is running.")
                        }
                    }
                }.start(wait = false)
                println("Ktor server started successfully on http://127.0.0.1:8080")
            } catch (e: Exception) {
                System.err.println("KtorServer ERROR: Failed to start server: ${e.message}")
                e.printStackTrace()
            }
        }

        launch(Dispatchers.IO) {
            val filePath = "initial_updates.txt"
            val file = File(filePath)
            if (file.exists()) {
                println("KtorServer: Loading initial data from $filePath...")
                file.readLines().forEach { line ->
                    val result = TrackingServer.processUpdateString(line)
                    println("KtorServer: Initial load result for '$line': $result")
                }
                println("KtorServer: Initial data loading complete.")
            } else {
                println("KtorServer: No initial data file '$filePath' found. Starting with empty state.")
            }
        }
    }

    Window(onCloseRequest = ::exitApplication, title = "Shipment Tracking Server & Client") {
        ServerUI()
    }
}