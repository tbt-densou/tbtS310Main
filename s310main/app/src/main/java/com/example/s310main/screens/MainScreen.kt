package com.example.s310main.screens

import android.os.AsyncTask
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.s310main.viewmodel.DeviceViewModel
import com.example.s310main.viewmodel.FirebaseViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import com.google.firebase.auth.FirebaseAuth
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.ValueRange
import java.io.IOException
// 認証ダイアログ関連のインポートを追加
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextField
import androidx.compose.material3.ExperimentalMaterial3Api // TextFieldなどに必要
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.livedata.observeAsState // For observeAsState
import androidx.compose.ui.text.input.KeyboardType // For KeyboardType.Password
import androidx.compose.ui.tooling.preview.Preview
import com.example.s310main.ui.theme.S310mainTheme

/**
 * ホーム画面を表示するComposable関数
 *
 * @param navController ナビゲーションを制御するNavHostController
 */
@OptIn(ExperimentalMaterial3Api::class) // TextFieldなどに必要
@Composable
fun MainScreen(navController: NavHostController, deviceViewModel: DeviceViewModel, firebaseViewModel: FirebaseViewModel) {
    val options = listOf("デバッグ", "全体接合", "1st走行", "2nd走行", "1stTF", "2ndTF", "3rdTF", "4thTF", "5thTF", "6thTF", "7thTF", "最終TF", "")

    // ViewModelから選択された値を取得
    val selectedValue by deviceViewModel.selectedValue.collectAsState()

    val currentSelectedValue = selectedValue ?: options[1]  // nullの場合はデフォルト値を使用

    //使われてないし不要?最初はデータ保存に必要かと思ってた
    val onSelect = { value: String ->
        deviceViewModel.setSelectedValue(value)
        firebaseViewModel.setSelectedValue(value)
    }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = options.indexOf(currentSelectedValue))

    // 認証状態の監視
    val currentUser by deviceViewModel.currentUser.observeAsState() // LiveDataをStateに変換
    var showPopup by remember { mutableStateOf(false) } // 認証ダイアログの表示状態

    // LaunchedEffectで認証状態を確認し、未認証ならダイアログを表示
    LaunchedEffect(currentUser) {
        if (currentUser == null) {
            showPopup = true
        } else {
            showPopup = false // 認証済みならダイアログを非表示にする
        }
    }

    // 認証ダイアログの表示
    // 認証ダイアログの表示
    if (showPopup) {
        AlertDialog(
            onDismissRequest = {
                showPopup = false
            },
            text = {
                // PopupScreen を AlertDialog の内容として表示
                PopupScreen(
                    viewModel = deviceViewModel,
                    onClose = { showPopup = false } // PopupScreen 内の閉じるボタンでポップアップを閉じる
                )
            },
            // ★AlertDialogのtitleとtext引数を削除し、コンテンツブロック（ラムダ）を直接使用★
            confirmButton = {},
            dismissButton = {},
            // usePlatformDefaultWidth = false を設定し、Modifierで幅を制御
            properties = DialogProperties(usePlatformDefaultWidth = false),
            // wrapContentWidth() と wrapContentHeight() を直接適用し、
            // ポップアップの左右のマージンを完全に無くす場合は horizontal = 0.dp を設定
            // しかし、ダイアログの見た目を考慮し、少し余白を設けるのが一般的
            // 例えば、横幅を画面の80%にするなど
            modifier = Modifier
                .wrapContentHeight()
                .fillMaxWidth(0.85f) // ★画面幅の85%に設定する例★
            // .padding(horizontal = 0.dp) // これをすると画面端に近くなりすぎることがある
        )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (deviceViewModel.isSliderVisible) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            contentPadding = PaddingValues(vertical = 71.dp)
                        ) {
                            items(options.size) { index ->
                                val isSelected = listState.firstVisibleItemIndex == index
                                Text(
                                    text = options[index],
                                    fontSize = if (isSelected) 28.sp else 20.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.Black else Color.Gray,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = currentSelectedValue,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                Button(
                    onClick = {
                        // スライダー中央の値を選択
                        deviceViewModel.setSelectedValue(options[listState.firstVisibleItemIndex])//viweModelにスライダーの状態を記録
                        firebaseViewModel.setSelectedValue(options[listState.firstVisibleItemIndex])
                        deviceViewModel.toggleSlider() // スライダーの表示(確定されているか)を切り替え
                    },
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Text(if (deviceViewModel.isSliderVisible) "▽" else "△")
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            val connectionStatus by deviceViewModel.connectionStatus.collectAsState()

            LaunchedEffect(key1 = "loadPairedDevices") {
                deviceViewModel.loadPairedDevicesPeriodically(5000)
            }

            DisposableEffect(key1 = "cancelLoading") {
                onDispose {
                    deviceViewModel.cancelPeriodicLoading()
                }
            }

            LaunchedEffect(key1 = Unit) {
                showPopup = true;
            }

            LaunchedEffect(key1 = "monitorConnectionStatus") {
                var lastCheckedTime = System.currentTimeMillis()
                while (isActive) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastCheckedTime >= 2000) {
                        lastCheckedTime = currentTime
                        deviceViewModel.isDeviceConnected("接続するESP32のマックアドレスが入ります").let { isConnected ->
                            if (!isConnected) {
                                deviceViewModel.updateConnectionStatus("接続が切れています")
                            }
                        }
                    }
                    delay(50)
                }
            }

            LaunchedEffect(key1 = "connectUntilSuccess") {
                var lastCheckedTime = System.currentTimeMillis()
                while (isActive) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastCheckedTime >= 2000) {
                        lastCheckedTime = currentTime
                        if (connectionStatus == "接続が完了しました") break
                        deviceViewModel.onButtonClicked("接続するESP32のマックアドレスが入ります")
                    }
                    delay(50)
                }
            }

            Text("メインアプリ")
            Button(onClick = {
                navController.navigate("firebase")
            }) {
                Text("Firebaseのデータ")
            }
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                navController.navigate("epcReader")
            }) {
                Text("受信データ表示")
            }
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                deviceViewModel.onButtonClicked("接続するESP32のマックアドレスが入ります")
            }) {
                Text("手動接続")
            }

            if (connectionStatus.isNotEmpty()) {
                Text(text = connectionStatus)
            }

            Button(onClick = {
                showPopup = true
            }) {
                Text("設定")
            }
        }
    }
}