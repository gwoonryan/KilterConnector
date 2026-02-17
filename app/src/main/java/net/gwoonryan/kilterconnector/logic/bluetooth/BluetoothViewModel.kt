package net.gwoonryan.kilterconnector.logic.bluetooth

import android.bluetooth.le.ScanResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BluetoothViewModel : ViewModel() {

    // Private mutable state that can be updated from this ViewModel
    private val _scanResults = MutableStateFlow<List<ScanResult>>(emptyList())

    // Public, immutable state that the UI can observe
    val scanResults: StateFlow<List<ScanResult>> = _scanResults.asStateFlow()

    // Your BLEManager can live here, though you might need to pass the context
    // For now, we'll just have a method to update the list
    fun addScanResult(result: ScanResult) {
        viewModelScope.launch {
            // Avoid adding duplicates
            if (!_scanResults.value.any { it.device.address == result.device.address }) {
                _scanResults.value += result
            }
        }
    }

    fun clearResults() {
        viewModelScope.launch {
            _scanResults.value = emptyList()
        }
    }
}