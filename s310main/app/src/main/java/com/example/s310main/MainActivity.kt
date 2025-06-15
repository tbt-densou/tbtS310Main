package com.example.s310main

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.s310main.ui.theme.S310mainTheme
import com.example.s310main.viewmodel.DeviceViewModel
import com.google.firebase.FirebaseApp
import android.content.Intent
import android.net.Uri
import androidx.navigation.compose.rememberNavController
import com.example.s310main.navigation.NavigationGraph
import android.provider.Settings
import com.example.s310main.model.BTPermissionHandler
import com.example.s310main.screens.PermissionDeniedSnackbar

class MainActivity : ComponentActivity() {
    // Bluetoothのパーミッションを管理するハンドラーを宣言
    private lateinit var btPermissionHandler: BTPermissionHandler

    // アクティビティが初めて作成されたときに呼び出されるメソッド
    // Bundleは、アクティビティの状態を保存および復元するために使用されるコンテナです。
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // BTPermissionHandlerの初期化
        btPermissionHandler = BTPermissionHandler(
            activity = this,
            onPermissionGranted = {
                loadUI()
            },
            onPermissionDenied = {
                showPermissionDeniedView()
                loadUI() // UI をセットすることでクラッシュを回避
            }
        )

        // パーミッションランチャーのセットアップ
        btPermissionHandler.setupPermissionLauncher()
        // bluetoothPermissionHandler.requestBluetoothPermissions()

        if (FirebaseApp.initializeApp(this) == null) {
            Log.e("FirebaseInit", "FirebaseApp.initializeApp failed. Check your google-services.json")
        }
    }

    // アクティビティがユーザーと再び対話を始める直前に呼び出されるメソッド
    // ここで再度Bluetoothのパーミッションをリクエストします。
    override fun onResume() {
        super.onResume()
        btPermissionHandler.requestBluetoothPermissions()
    }

    // UIを読み込むメソッド
    // パーミッションが許可された場合にUIを設定します。
    private fun loadUI() {
        setContent {
            // ナビゲーションコントローラの作成
            val navController = rememberNavController()
            // アプリのテーマを設定
            S310mainTheme {
                // ナビゲーショングラフを設定
                NavigationGraph(
                    navController = navController,
                    // Bluetooth設定画面を開く関数の設定
                    openBluetoothSettings = { openBluetoothSettings() },
                    deviceViewModel = DeviceViewModel()
                )
            }
        }
    }

    // パーミッションが拒否された場合に呼び出されるビューを表示するメソッド
    private fun showPermissionDeniedView() {
        setContent {
            // スナックバーを表示し、設定画面を開くオプションを提供
            PermissionDeniedSnackbar(onSettingsClick = { openAppSettings() })
        }
    }

    // アプリの設定画面を開くメソッド
    private fun openAppSettings() {
        // 設定画面を開くIntentを作成
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    // Bluetooth設定画面を開くメソッド
    private fun openBluetoothSettings() {
        // Bluetooth設定画面を開くIntentを作成
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
        startActivity(intent)
    }
}