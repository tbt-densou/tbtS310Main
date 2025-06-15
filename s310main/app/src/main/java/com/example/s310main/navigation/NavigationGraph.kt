package com.example.s310main.navigation


import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.s310main.screens.MainScreen
import com.example.s310main.screens.ReceiveDataScreen
import com.example.s310main.screens.views.FirebaseReader
import com.example.s310main.viewmodel.DeviceViewModel
import com.example.s310main.viewmodel.FirebaseViewModel


/**
 * アプリケーション内の画面遷移（ナビゲーション）を管理するComposable関数
 *
 * @param navController: NavHostController - 画面遷移を管理するコントローラー
 * @param openBluetoothSettings: () -> Unit - Bluetooth設定画面を開くためのコールバック関数
 */
@Composable
fun NavigationGraph(
    navController: NavHostController,
    deviceViewModel: DeviceViewModel = viewModel(),  // ViewModel を作成
    firebaseViewModel: FirebaseViewModel = viewModel(), // ここで ViewModel を作成
    openBluetoothSettings: () -> Unit
) {
    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainScreen(navController, deviceViewModel, firebaseViewModel)
        }
        composable("firebase") {
            FirebaseReader(firebaseViewModel) // 修正後の `FirebaseReader` に渡す
        }
        composable("epcReader") {
            ReceiveDataScreen(deviceViewModel)
        }
    }
}
