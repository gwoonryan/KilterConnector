package net.gwoonryan.kilterconnector.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import net.gwoonryan.kilterconnector.logic.bluetooth.BLEManager
import net.gwoonryan.kilterconnector.logic.kilter.KilterConnector



@Composable
fun MoveScreen(deviceAddress: String, bleManager: BLEManager, context: Context){
    var kilterConnector: KilterConnector? = remember{
        bleManager.connectToDevice(deviceAddress, context);
    }
    Box(modifier= Modifier.fillMaxSize()) {
        Button(modifier=Modifier.align(Alignment.Center), onClick = {
            kilterConnector!!.sendPacket()
        }) {Text(deviceAddress) }

    }
}