package com.example.s310main.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.s310main.screens.SensorData
import com.google.firebase.database.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.tasks.await

class FirebaseViewModel : ViewModel() {
    private val databaseRef: DatabaseReference =
        FirebaseDatabase.getInstance().getReference("sensorData")

    private val _latestData = MutableStateFlow<SensorData?>(null)
    val latestData: StateFlow<SensorData?> = _latestData.asStateFlow()

    private val _selectedValue = MutableStateFlow<String?>("全体接合") // 初期値
    val selectedValue: StateFlow<String?> = _selectedValue

    init {
        startPollingFirebase()
    }

    // Firebaseをポーリングしてデータを取得
    private fun startPollingFirebase() {
        viewModelScope.launch {
            while (true) {
                try {
                    fetchLatestData()  // Firebaseから最新データを取得
                    delay(100)  // 100msごとにデータを取得
                } catch (e: Exception) {
                    Log.e("FirebaseViewModel", "Error polling Firebase: ${e.message}")
                }
            }
        }
    }

    // Firebaseから最新データを取得する
    private suspend fun fetchLatestData() {
        try {
            val snapshot = databaseRef.orderByChild("timestamp").limitToLast(1).get().await()

            if (snapshot.exists()) {
                // FirebaseからのデータをMapとして取得
                val dataMap = snapshot.children.firstOrNull()?.value as? Map<String, Any>

                if (dataMap != null) {
                    var sensorData = SensorData(
                        sw1 = (dataMap["sw1"] as? String)?.toDouble(),
                        speed = (dataMap["speed"] as? String)?.toDouble(),
                        height = (dataMap["height"] as? String)?.toDouble(),
                        rpm = (dataMap["rpm"] as? String)?.toDouble(),
                        roll = (dataMap["roll"] as? String)?.toDouble(),
                        pitch = (dataMap["pitch"] as? String)?.toDouble(),
                        yaw = (dataMap["yaw"] as? String)?.toDouble(),
                        eAngle = (dataMap["eAngle"] as? String)?.toDouble(),
                        rAngle = (dataMap["rAngle"] as? String)?.toDouble(),
                        timestamp = dataMap["timestamp"] as? Long
                    )

                    // タイムステップデータの場合、前のデータを取得する
                    while (sensorData.timestamp != null && isTimeStepOnly(sensorData)) {
                        // timestampがnullでないことを確認し、Long型からDouble型にキャストして計算
                        val previousTimestamp = (sensorData.timestamp?.minus(1))?.toDouble() ?: break

                        // Firebaseに渡す前に、previousTimestampをDouble型として確実に渡す
                        val previousSnapshot = databaseRef.orderByChild("timestamp")
                            .endAt(previousTimestamp).limitToLast(1).get().await()

                        if (previousSnapshot.exists()) {
                            val prevDataMap = previousSnapshot.children.firstOrNull()?.value as? Map<String, Any>
                            if (prevDataMap != null) {
                                sensorData = SensorData(
                                    sw1 = (prevDataMap["sw1"] as? String)?.toDouble(),
                                    speed = (prevDataMap["speed"] as? String)?.toDouble(),
                                    height = (prevDataMap["height"] as? String)?.toDouble(),
                                    rpm = (prevDataMap["rpm"] as? String)?.toDouble(),
                                    roll = (prevDataMap["roll"] as? String)?.toDouble(),
                                    pitch = (prevDataMap["pitch"] as? String)?.toDouble(),
                                    yaw = (prevDataMap["yaw"] as? String)?.toDouble(),
                                    eAngle = (prevDataMap["eAngle"] as? String)?.toDouble(),
                                    rAngle = (prevDataMap["rAngle"] as? String)?.toDouble(),
                                    timestamp = prevDataMap["timestamp"] as? Long
                                )
                            }
                        }
                    }

                    // 最終的に取得したデータを設定
                    _latestData.value = sensorData
                    Log.d("FirebaseViewModel", "Fetched data: $sensorData")
                } else {
                    Log.e("FirebaseViewModel", "Invalid data received from Firebase.")
                }
            } else {
                Log.e("FirebaseViewModel", "No data found in Firebase.")
            }
        } catch (e: Exception) {
            Log.e("FirebaseViewModel", "Error fetching data from Firebase: ${e.message}")
        }
    }

    // タイムステップデータかどうかを判定するメソッド
    private fun isTimeStepOnly(sensorData: SensorData): Boolean {
        return sensorData.timestamp != null && sensorData.rpm == null && sensorData.speed == null
    }



    fun setSelectedValue(value: String) {
        _selectedValue.value = value
    }

}
