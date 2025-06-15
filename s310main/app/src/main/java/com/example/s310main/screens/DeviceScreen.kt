package com.example.s310main.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.s310main.screens.views.DeviceListView
import com.example.s310main.viewmodel.DeviceViewModel

@Composable
fun DeviceScreen(
    deviceViewModel: DeviceViewModel,
    openBluetoothSettings: () -> Unit
) {

    val devicesInfo by deviceViewModel.devicesInfo.collectAsState()
    val isLoading by deviceViewModel.isLoading.collectAsState()
    val selectedValue by deviceViewModel.selectedValue.collectAsState()


    // デバイスがクリックされたときの処理
    val handleDeviceClick = { index: Int ->
        deviceViewModel.onDeviceClicked(index)
    }

    LaunchedEffect(key1 = "loadPairedDevices") {
        deviceViewModel.loadPairedDevicesPeriodically(5000) // 例: 5秒ごと
    }

    DisposableEffect(key1 = "cancelLoading") {
        onDispose {
            deviceViewModel.cancelPeriodicLoading()
        }
    }

    DeviceListView(
        devicesInfo = devicesInfo,
        onDeviceClick = handleDeviceClick,
        isLoading = isLoading,
        onSettingsClick = openBluetoothSettings
    )

}
