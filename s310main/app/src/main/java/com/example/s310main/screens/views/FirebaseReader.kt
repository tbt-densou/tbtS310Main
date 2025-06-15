package com.example.s310main.screens.views

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.s310main.viewmodel.FirebaseViewModel
import android.util.Log
import com.example.s310main.screens.views.ReceiveDataView

@Composable
fun FirebaseReader(viewModel: FirebaseViewModel) {
    val latestData by viewModel.latestData.collectAsState()
    val selectedValue by viewModel.selectedValue.collectAsState()

    // デバッグ用: latestData の値をログに出力
    LaunchedEffect(latestData) {
        Log.d("FirebaseReader", "Latest Data: $latestData")
    }


    latestData?.let { data ->
        val speedD=data.speed?:null
        val heiD=data.height?:null
        val rpmD=data.rpm?:null
        val rollD=data.roll?:null
        val pitchD=data.pitch?:null
        val yawD=data.yaw?:null
        val eAngD=data.eAngle?:null
        val rAngD=data.rAngle?:null
        val sw1D=data.sw1?:null
        val sw2D=data.sw1?:null
        val sw3D=data.sw1?:null

        // 最新データを表示するビューにデータを渡す
        ReceiveDataView(
            speedD = speedD.toString(),
            rpmD = rpmD.toString(),
            heiD = heiD.toString(),

            rollD = rollD.toString(),
            pitchD = pitchD.toString(),
            yawD = yawD.toString(),

            eAngD = eAngD.toString(),
            rAngD = rAngD.toString(),

            sw1D = sw1D.toString(),
            sw2D = sw2D.toString(),
            sw3D = sw3D.toString(),
            saveData = false,
            selectedValue = selectedValue,
        )
    } ?: Text("No data available.")

}
