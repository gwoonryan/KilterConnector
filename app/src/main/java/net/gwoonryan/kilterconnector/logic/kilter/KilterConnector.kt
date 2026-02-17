package net.gwoonryan.kilterconnector.logic.kilter

import android.Manifest
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanResult
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.collections.sortWith
import kotlin.math.abs

const val serviceUUID = "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"
const val characteristicUUID = "6E400002-B5A3-F393-E0A9-E50E24DCCA9E";
const val BOLT_ON_WIDTH = 23;
const val BOLT_ON_HEIGHT = 19;

class KilterConnector(val scanResult: ScanResult, val context: Context): BluetoothGattCallback() {

    lateinit var connection: BluetoothGatt;

    var connected = false;
    var db: DataBase = DataBase(context);

    val boltOns: MutableList<HoldLocation> = mutableListOf();

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connect(){
        connection = scanResult.device.connectGatt(context, true, this);
        createGrid()
    }

    fun sendPacket(){
//        val hold = getHoldOnPos(0, 0) ?: return;
//        val leds = listOf(
//            KilterLed(ledId = hold.ledPosition, ledColorHex = "FF0000")
//        )
//
//        val pkt = KilterPacket(leds = leds, numPackets = 1, packetId = 0)
//        pkt.sendPacket(connection, this);

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch(Dispatchers.IO) {
            for (loc in boltOns){
                val pkt = KilterPacket(
                    leds = listOf(KilterLed(loc.ledPosition, "FF0000")),
                    numPackets = 1,
                    packetId = 0
                )
                pkt.sendPacket(connection, this@KilterConnector)
                delay(400)
            }
        }
    }

    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            // successfully connected to the GATT Server
            connected = true;
            // Attempts to discover services after successful connection.

            try {
                connection.discoverServices()
            }catch (e: SecurityException){
                return
            }
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            // disconnected from the GATT Server
            connected = false;
        }
    }

    fun createGrid(){
        val locs: List<HoldLocation> = db.getHoldLocations()
        db.close()
        val xPosses: MutableSet<Int> = mutableSetOf();
        val yPosses: MutableSet<Int> = mutableSetOf();

        for (loc in locs){
            if (loc.holdType == HoldType.BOLT_ON) {
                xPosses.add(loc.x)
                yPosses.add(loc.y)
                boltOns += loc;
            }
//
        }
        if (xPosses.size != BOLT_ON_WIDTH){
            Log.e("Grid Maker", "Expected another X dim, got: " + xPosses.size + " expected: " + BOLT_ON_WIDTH)
            return
        }
        else if (yPosses.size != BOLT_ON_HEIGHT){
            Log.e("Grid Maker", "Expected another Y dim, got: " + yPosses.size + " expected: " + BOLT_ON_HEIGHT)
            return
        }
        boltOns.sortWith(compareBy({ it.y }, { it.x }))
//        for (loc in boltOns){
//            Log.e("holds", loc.x.toString() + " " + loc.y.toString() + " " + loc.ledPosition.toString())
//        }
    }

    fun getHoldOnPos(x: Int, y: Int): HoldLocation?{
        if (x < 0 || x >= BOLT_ON_WIDTH){
            return null
        }
        if (y < 0 || y >= BOLT_ON_HEIGHT){
            return null
        }
        return boltOns[x + (x*y)]
    }
}