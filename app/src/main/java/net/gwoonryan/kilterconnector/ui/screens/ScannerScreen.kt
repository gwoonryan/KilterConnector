package net.gwoonryan.kilterconnector.ui.screens

import android.Manifest
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import net.gwoonryan.kilterconnector.logic.bluetooth.BLEManager
import net.gwoonryan.kilterconnector.logic.bluetooth.BluetoothViewModel
import net.gwoonryan.kilterconnector.ui.theme.KilterConnectorTheme


private val permissionsToRequest = arrayOf(
    Manifest.permission.BLUETOOTH_SCAN,
    Manifest.permission.BLUETOOTH_CONNECT
)

private fun areAllPermissionsGranted(context: Context): Boolean {
    return permissionsToRequest.all { permission ->
        ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }
}

@Composable
fun ScannerScreen(context: Context, bluetoothViewModel: BluetoothViewModel, bleManager: BLEManager, navController: NavHostController){
    // This state will track if all permissions are granted
    var hasPermissions by remember {
        mutableStateOf(areAllPermissionsGranted(context))
    }

    // This is the modern way to request permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        // Check if ALL permissions in the map were granted
        if (permissionsMap.values.all { it }) {
            hasPermissions = true
        } else {
            // Handle the case where the user denies permissions
            // You might want to show a message to the user
        }
    }

    // This effect will run once when the composable is first displayed
    LaunchedEffect(key1 = true) {
        if (!hasPermissions) {
            permissionLauncher.launch(permissionsToRequest)
        }
    }

    KilterConnectorTheme {
        val scanResults by bluetoothViewModel.scanResults.collectAsState()
        if (hasPermissions) {
            DeviceListScreen(scanResults = scanResults, context, bluetoothViewModel, bleManager, navController)
        } else {
            // Permissions are not granted, show a placeholder or a request button
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Button(onClick = { permissionLauncher.launch(permissionsToRequest) }) {
                    Text("Request Permissions")
                }
            }
        }
    }
}

@Composable
fun DeviceListScreen(scanResults: List<ScanResult>, context: Context, bluetoothViewModel: BluetoothViewModel, bleManager: BLEManager, navController: NavHostController) {
    Box(modifier = Modifier
        .fillMaxSize()
        .padding(WindowInsets.safeDrawing.asPaddingValues())) {
        // Use LazyColumn for efficient list display
        LazyColumn(modifier = Modifier.align(Alignment.TopCenter), contentPadding = PaddingValues(20.dp)) {
            items(scanResults) { result ->
                // You still need to handle permissions, but this structure is better
                val deviceName = try {
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        result.device.name ?: "Unnamed Device"
                    } else {
                        "Permission Needed"
                    }
                } catch (e: SecurityException) {
                    "Permission Error"
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth() // Make the box take the full width of the LazyColumn
                        .height(200.dp) // Set a fixed height of 200dp
                        .padding(vertical = 8.dp) // Add some space between cards
                        .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp)) // Add a subtle shadow
                        .background(MaterialTheme.colorScheme.surfaceVariant, shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) // Add background color and round the corners
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) // Clip the content to the rounded shape
                        .clickable {
                            // TODO: Handle the click event, e.g., navigate to a detail screen
                             navController.navigate("move/${result.device.address}")
                        },
                    contentAlignment = Alignment.Center // Center the content (the Text) inside the Box
                ) {
                    Text(text = deviceName)
                }

            }
        }
        Button(modifier = Modifier.align(Alignment.BottomCenter),
            onClick = {
                bluetoothViewModel.clearResults()
                bleManager.startScan()
            }) {
            Text("Scan for devices")
        }
    }
}