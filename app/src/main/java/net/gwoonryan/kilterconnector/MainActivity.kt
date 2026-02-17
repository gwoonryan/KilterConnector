package net.gwoonryan.kilterconnector

import android.content.Intent
import android.os.Bundle
import android.os.ParcelUuid
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.Text
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import net.gwoonryan.kilterconnector.logic.bluetooth.BLEManager
import net.gwoonryan.kilterconnector.logic.bluetooth.BluetoothViewModel
import net.gwoonryan.kilterconnector.ui.screens.MoveScreen
import net.gwoonryan.kilterconnector.ui.screens.ScannerScreen
import net.gwoonryan.kilterconnector.ui.theme.KilterConnectorTheme

var REQUEST_ENABLE_BT = 0
val advertisementUUID: ParcelUuid = ParcelUuid.fromString("4488B571-7806-4DF6-BCFF-A2897E4953FF")

class MainActivity : ComponentActivity() {

    // Get a ViewModel instance
    private val bluetoothViewModel: BluetoothViewModel by viewModels()
    private lateinit var bleManager: BLEManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize BLEManager here, passing the ViewModel's update function
        bleManager = BLEManager(this.baseContext, this) { result ->
            bluetoothViewModel.addScanResult(result)
        }

        val context = this.baseContext

        enableEdgeToEdge()
        setContent {
            KilterConnectorTheme() {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "scan") {
                    composable("scan") {
                        ScannerScreen(
                            context,
                            bluetoothViewModel,
                            bleManager,
                            navController
                        )
                    }
                    composable(
                        route = "move/{deviceAddress}",
                        arguments = listOf(navArgument("deviceAddress") {
                            type = NavType.StringType
                        })
                    ) { backStackEntry ->
                        // 3. Retrieve the argument from the backStackEntry
                        val address = backStackEntry.arguments?.getString("deviceAddress")

                        // 4. Call your new screen, passing the retrieved address
                        if (address != null) {
                            MoveScreen(deviceAddress = address, bleManager, context)
                        } else {
                            Text("Error: Device address not found.")
                        }
                    }
                }
            }
        }

    }



    @Deprecated("Deprecated")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT){
            if (resultCode == RESULT_OK){
//                bleManager.startScan()
            }
        }
    }
}

//@Composable
//fun Greeting(name: String, modifier: Modifier = Modifier) {
//    Text(
//        text = "Hello $name!",
//        modifier = modifier
//    )
//}
//
//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    KilterConnectorTheme {
//        Greeting("Android")
//    }
//}