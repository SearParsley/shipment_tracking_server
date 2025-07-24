package marcus.hansen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientUI() {
    var updateInput by remember { mutableStateOf("") }
    var serverResponse by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val httpClient = remember { HttpClient(CIO) }

    val sendUpdateToServer: (String) -> Unit = { updateString ->
        serverResponse = null // Clear previous response
        coroutineScope.launch {
            try {
                val response: HttpResponse = httpClient.post("http://127.0.0.1:8080/update-shipment") {
                    setBody(updateString) // Body is the raw string
                    contentType(ContentType.Text.Plain) // Explicitly set content type
                }
                serverResponse = "Server Response (${response.status.value}): ${response.bodyAsText()}"
                updateInput = "" // Clear input field
            } catch (e: Exception) {
                serverResponse = "Network Error: ${e.message}. Is the server running at http://127.0.0.1:8080?"
                System.err.println("Client Error: ${e.message}") // Log client-side network errors
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Shipment Update Client") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Enter Shipment Update String:", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = updateInput,
                onValueChange = { updateInput = it },
                label = { Text("Update String") },
                placeholder = { Text("updateType,shipmentId,timestampOfUpdate,otherInfo (optional)") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { sendUpdateToServer(updateInput) },
                enabled = updateInput.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Send Update to Server")
            }
            Spacer(Modifier.height(16.dp))

            serverResponse?.let { response ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (response.startsWith("Network Error") || response.startsWith("Server Response (5") || response.startsWith("Server Response (4")) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = response,
                        modifier = Modifier.padding(16.dp),
                        color = if (response.startsWith("Network Error") || response.startsWith("Server Response (5") || response.startsWith("Server Response (4")) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}