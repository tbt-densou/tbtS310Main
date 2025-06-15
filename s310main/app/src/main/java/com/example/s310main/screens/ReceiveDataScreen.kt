package com.example.s310main.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.s310main.viewmodel.DeviceViewModel
import com.example.s310main.screens.views.ReceiveDataView

@Composable
fun ReceiveDataScreen(deviceViewModel: DeviceViewModel = viewModel()) {
    // ViewModelから最新データを取得し、Stateとして監視する
    val speedD by deviceViewModel.speed.collectAsState()//A
    val rpmD by deviceViewModel.rpm.collectAsState()//I
    val heiD by deviceViewModel.height.collectAsState()//H

    val rollD by deviceViewModel.roll.collectAsState()//O
    val pitchD by deviceViewModel.pitch.collectAsState()//P
    val yawD by deviceViewModel.yaw.collectAsState()//Q

    val eAngD by deviceViewModel.eAngle.collectAsState()//B
    val rAngD by deviceViewModel.rAngle.collectAsState()//C

    val sw1D by deviceViewModel.sw1.collectAsState()//V
    val sw2D by deviceViewModel.sw2.collectAsState()//W
    val sw3D by deviceViewModel.sw3.collectAsState()//X

    val selectedValue by deviceViewModel.selectedValue.collectAsState()


    // 最新データを表示するビューにデータを渡す
    ReceiveDataView(
        speedD = speedD,
        rpmD = rpmD,
        heiD = heiD,

        rollD = rollD,
        pitchD = pitchD,
        yawD = yawD,

        eAngD = eAngD,
        rAngD = rAngD,

        sw1D = sw1D,
        sw2D = sw2D,
        sw3D = sw3D,
        saveData = true,
        selectedValue = selectedValue,
    )
}
