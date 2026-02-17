package net.gwoonryan.kilterconnector.logic.kilter

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.util.Log
import java.util.UUID

// Define your own exception type, matching the Python code's intent.
class KilterError(message: String) : Exception(message)

data class KilterLed(
    val ledId: Int,
    /** 6 hex chars like "FFA07A" (no leading '#') */
    val ledColorHex: String
)

class KilterPacket(
    val leds: List<KilterLed>,
    val numPackets: Int,
    val packetId: Int
) {
    val byteData: ByteArray

    init {
        // 84 holds max:
        require(leds.size <= 84) { throw KilterError("Too many leds in one packet") }

        val dataInts = getDataInts()
        val checksum = getChecksum(dataInts)

        // Header:
        // [1, size, checksum, 2] + data + [3]
        val sizeByte = 1 + leds.size * 3 // matches Python: 1 + len(leds) * 3

        val out = ArrayList<Int>(4 + dataInts.size + 1)
        out += 1
        out += sizeByte
        out += checksum
        out += 2
        out.addAll(dataInts)
        out += 3

        byteData = out.map { (it and 0xFF).toByte() }.toByteArray()
    }

    private fun getDataInts(): List<Int> {
        val data = ArrayList<Int>()

        // The first byte in the data is dependent on where the packet is in the message
        // first: 82
        // last: 83
        // middle: 81
        // only packet: 84
        val firstInt = when {
            packetId == 0 && numPackets == 1 -> 84
            packetId == 0 -> 82
            packetId == numPackets - 1 -> 83
            else -> 81
        }
        data += firstInt

        for (led in leds) {
            val holdId = led.ledId
            val low = holdId and 0xFF
            val high = (holdId ushr 8) and 0xFF
            data += low
            data += high

            val col = led.ledColorHex
            // Expecting 6 chars: RRGGBB
            val r = col.substring(0, 2).toInt(16)
            val g = col.substring(2, 4).toInt(16)
            val b = col.substring(4, 6).toInt(16)

            data += (rgb888ToRgb332(r, g, b) and 0xFF)
        }

        return data
    }


    companion object {
        fun getChecksum(data: List<Int>): Int {
            var i = 0
            for (valInt in data) {
                i = (i + (valInt and 0xFF)) and 0xFF // wrap to 0–255
            }
            return i.inv() and 0xFF
        }

        /**
         * r,g,b in 0..255 → one byte RGB332.
         */
        fun rgb888ToRgb332(r: Int, g: Int, b: Int): Int {
            val r3 = r ushr 5 // keep top 3 bitsq
            val g3 = g ushr 5 // keep top 3 bits
            val b2 = b ushr 6 // keep top 2 bits
            return (r3 shl 5) or (g3 shl 2) or b2
        }
    }

    fun sendPacket(connection: BluetoothGatt, connector: KilterConnector){
        if (!connector.connected){
            return
        }
        val chunks: List<ByteArray> = byteData.chunked20()
        val service = connection.getService(UUID.fromString(serviceUUID))
        if (service == null){
            Log.e("kilterconnector","service was null")
            return
        }
        val characteristic = service.getCharacteristic(UUID.fromString(characteristicUUID))
        if (characteristic == null){
            Log.e("kilterconnector","characteristic was null")
            return
        }
        try {
            Log.e("Sender", "sending packet?")
            for (chunk in chunks) {
                connection.writeCharacteristic(
                    characteristic,
                    chunk,
                    BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                )
            }
        }catch (e: SecurityException){
            return
        }
    }
}

fun ByteArray.chunked20(chunkSize: Int = 20): List<ByteArray> {
    require(chunkSize > 0)
    val out = ArrayList<ByteArray>((this.size + chunkSize - 1) / chunkSize)
    var i = 0
    while (i < this.size) {
        val end = minOf(i + chunkSize, this.size)
        out.add(this.copyOfRange(i, end))
        i = end
    }
    return out
}
