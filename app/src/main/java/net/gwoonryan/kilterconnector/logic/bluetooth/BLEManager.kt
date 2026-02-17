package net.gwoonryan.kilterconnector.logic.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import androidx.core.content.ContextCompat.getSystemService
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startActivityForResult
import net.gwoonryan.kilterconnector.MainActivity
import net.gwoonryan.kilterconnector.REQUEST_ENABLE_BT
import net.gwoonryan.kilterconnector.advertisementUUID
import net.gwoonryan.kilterconnector.logic.kilter.KilterConnector

class BLEManager(val context: Context, val activity: MainActivity, val onResult: (ScanResult) -> Unit): ScanCallback(){
    var bluetoothManager: BluetoothManager
    var bluetoothAdapter: BluetoothAdapter

    init {;
        var manager = getSystemService(context, BluetoothManager::class.java)!!
        bluetoothManager = manager;
        var adapter = bluetoothManager.adapter!!
        bluetoothAdapter = adapter

        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(activity, enableBtIntent, REQUEST_ENABLE_BT, null)
        }
    }

    fun startScan(){
        scanLeDevice()
    }

    private val bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
    private var scanning = false
    private val handler = Handler()

    // Stops scanning after 10 seconds.
    private val SCAN_PERIOD: Long = 10000

    private val scanned_devices: HashMap<String, ScanResult> = HashMap()

    private fun scanLeDevice() {
        if(ActivityCompat.checkSelfPermission(
            this.context,
            Manifest.permission.BLUETOOTH_SCAN
        ) != PackageManager.PERMISSION_GRANTED){
            Log.e("BLEScanner","permission denied!")
            return
        }
        val scanFilter = ScanFilter.Builder()
            .setServiceUuid(advertisementUUID)
            .build()
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        handler.postDelayed({
            scanning = false
            bluetoothLeScanner.stopScan(this)
        }, SCAN_PERIOD)
        scanning = true
        bluetoothLeScanner.startScan(listOf(scanFilter), scanSettings,this)
        Log.e("BLEScanner","starting scan!")
    }

    override fun onScanResult(callbackType: Int, result: ScanResult?) {
        if (result == null){
            return
        }
        val deviceAddress = try {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                result.device.address ?: "Unnamed Device"
            } else {
                "Permission Needed"
            }
        } catch (e: SecurityException) {
            "Permission Error"
        }
        scanned_devices[deviceAddress] = result
        onResult(result);
//        Log.e("BLEScanner","scanned device!")
    }

    override fun onScanFailed(errorCode: Int) {
        return
    }

    fun connectToDevice(address: String, context: Context): KilterConnector?{
        val result = scanned_devices[address] ?: return null
        val connector = KilterConnector(result, context);
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("BLEScanner", "No bluetooth permission");
            return null
        }
        connector.connect();
        return connector
    }
}