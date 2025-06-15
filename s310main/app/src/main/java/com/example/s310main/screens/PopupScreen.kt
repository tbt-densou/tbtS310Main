package com.example.s310main.screens

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.s310main.ui.theme.S310mainTheme
import com.example.s310main.viewmodel.DeviceViewModel
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.PasswordVisualTransformation // PasswordVisualTransformation のインポートを追加
import androidx.compose.runtime.collectAsState // collectAsState のインポートを追加
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.text.font.FontWeight
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.StateFlow // StateFlow のインポートを確認
import kotlinx.coroutines.delay


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PopupScreen(
    viewModel: DeviceViewModel,
    onClose: () -> Unit
) {
    val TAG = "SimpleFunctionCallerUI"
    val resultText by viewModel.resultText.collectAsState() // 処理結果のメッセージ
    val errorMessage by viewModel.errorMessage.observeAsState(null) // 初期値を指定 (null)

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var registrationCode by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    val loggedInEmail by viewModel.loggedInEmail.collectAsState()

    // パスワードリセットポップアップの表示状態
    var showResetPassPopup by remember { mutableStateOf(false) }
    var showAccountSetting by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .wrapContentSize()
            .padding(8.dp)
            .clickable { keyboardController?.hide() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "ログイン/新規登録",
                fontSize = 24.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                viewModel.clearAll()
                onClose()
            }) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "閉じる")
            }
        }
        // ログイン中のユーザーがいればメールアドレスを表示
        loggedInEmail?.let { email ->
            Text(
                text = "ログイン中: $email",
                color = Color.Green,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        if (loggedInEmail == null) {
            TextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("メールアドレス") },
                modifier = Modifier.fillMaxWidth() // TextFieldの幅を広げる
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("パスワード") },
                visualTransformation = PasswordVisualTransformation(), // パスワードを隠す
                modifier = Modifier.fillMaxWidth() // TextFieldの幅を広げる
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = registrationCode,
                onValueChange = { registrationCode = it },
                label = { Text("登録コード") },
                modifier = Modifier.fillMaxWidth() // TextFieldの幅を広げる
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val enteredEmail = email
                    val enteredPassword = password

                    if (enteredEmail.isNotEmpty() && enteredPassword.isNotEmpty()) {
                        viewModel.signInWithEmailAndPassword(enteredEmail, enteredPassword)
                    } else {
                        viewModel.setErrorMessage("メールアドレスとパスワードを入力してください。")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("ログイン")
            }
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val enteredEmail = email
                    val enteredPassword = password
                    val enteredCode = registrationCode

                    Log.d(TAG, "--- New Registration Attempt ---")
                    Log.d(TAG, "Email: \"$enteredEmail\" (Length: ${enteredEmail.length})")
                    Log.d(TAG, "Password: \"$enteredPassword\" (Length: ${enteredPassword.length})")
                    Log.d(TAG, "Code: \"$enteredCode\" (Length: ${enteredCode.length})")
                    Log.d(TAG, "Email Empty: ${enteredEmail.isEmpty()}, Password Empty: ${enteredPassword.isEmpty()}, Code Empty: ${enteredCode.isEmpty()}")

                    if (enteredEmail.isNotEmpty() && enteredPassword.isNotEmpty() && enteredCode.isNotEmpty()) {
                        // ViewModelに新規登録用の関数がある場合、それを呼び出す
                        viewModel.callHelloWorldWithName(enteredEmail, enteredPassword, enteredCode)
                        // 成功時はViewModel内でclearErrorMessage()が呼ばれる想定
                    } else {
                        viewModel.setErrorMessage("すべての項目を入力してください。")
                        Log.d(TAG, "Validation failed: Fields are empty.")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("新規登録")
            }
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    showResetPassPopup = true
                },
                modifier = Modifier.fillMaxWidth()
            ){
                Text("パスワードをお忘れの場合")
            }
        } else {
            Button(
                onClick = {
                    viewModel.signOut()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("ログアウト")
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = {
                    viewModel.clearAll()
                    showAccountSetting = true
                },
                modifier = Modifier.fillMaxWidth()
            ){
                Text("アカウント設定")
            }
        }

        // ★エラーメッセージ表示の修正★
        // errorMessageがnullではない場合のみ表示
        errorMessage?.let { message ->
            Text(
                text = message,
                color = Color.Red, // エラーメッセージは赤色
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
        }
        // ★結果メッセージ表示の修正★
        // resultTextがデフォルトのメッセージ（"結果がここに表示されます。"）でない場合のみ表示、かつ色をデフォルト（または緑など）に
        if (resultText != "結果がここに表示されます。") {
            Text(
                text = resultText,
                // エラーメッセージと区別するために色をデフォルト（または緑など）にする
                color = MaterialTheme.colorScheme.onSurface, // 例: デフォルトのテキスト色
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
        }
    }

    // showResetPassPopupがtrueの場合のみResetPassポップアップを表示
    if (showResetPassPopup) {
        AlertDialog(
            onDismissRequest = {
                viewModel.clearAll()
                onClose()
            }, // ポップアップ外をタップで閉じる
            text = {
                ResetPass(
                    viewModel = viewModel,
                    onClose = { showResetPassPopup = false } // ResetPass内の閉じるボタンで状態をfalseに
                )
            },
            confirmButton = {
                // confirmButtonは必要なければ空にしてもOK
            }
        )
    }

    if (showAccountSetting) {
        AccountSetting(
            viewModel = viewModel,
            onClose = { showAccountSetting = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSetting(viewModel: DeviceViewModel, onClose: () -> Unit) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val currentEmail = currentUser?.email ?: "不明なユーザー"

    var password by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var step by remember { mutableStateOf("reauth") }
    val resultText by viewModel.resultText.collectAsState()
    val errorMessage by viewModel.errorMessage.observeAsState()
    var cloased by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "アカウント管理",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    viewModel.clearAll()
                    onClose()
                }) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "閉じる")
                }

            }
        },
        text = {
            Column {
                if (step == "reauth") {
                    Text("登録済みのメールアドレスとパスワードで再認証してください")
                    Text("メール: $currentEmail", fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("パスワード") },
                        visualTransformation = PasswordVisualTransformation()
                    )
                } else if (step == "change") {
                    Text("新しいパスワードを入力してください")
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("新しいパスワード") },
                        visualTransformation = PasswordVisualTransformation()
                    )
                } else if (step == "delete") {
                    Text("アカウントを本当に削除しますか？この操作は取り消せません。")
                }

                if (resultText.isNotBlank()) Text("✅ $resultText", color = Color.Green)
                if (errorMessage?.isNotBlank() == true) Text("⚠️ $errorMessage", color = Color.Red)
            }
        },
        confirmButton = {
            when (step) {
                "reauth" -> {
                    Column {
                        Button(onClick = {
                            if (password.isNotEmpty()) {
                                viewModel.reauthenticateAndRun(
                                    password,
                                    onSuccess = { step = "change" },
                                    onFailure = { viewModel.setErrorMessage(it) }
                                )
                            } else {
                                viewModel.setErrorMessage("パスワードを入力してください。")
                            }
                        },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("パスワード変更")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            if (password.isNotEmpty()) {
                                viewModel.reauthenticateAndRun(
                                    password,
                                    onSuccess = { step = "delete" },
                                    onFailure = { viewModel.setErrorMessage(it) }
                                )
                            } else {
                                viewModel.setErrorMessage("パスワードを入力してください。")
                            }
                        },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("アカウント削除")
                        }
                    }
                }
                "change" -> {
                    Button(onClick = {
                        viewModel.changePassword(newPassword) { success, error ->
                            if (success) {
                                viewModel.setResult("パスワードを変更しました")
                                cloased = true
                            } else {
                                viewModel.setErrorMessage(error ?: "エラー")
                            }
                        }
                    },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("変更する")
                    }
                }
                "delete" -> {
                    Button(onClick = {
                        viewModel.deleteUser { success, error ->
                            if (success) {
                                viewModel.setResult("アカウントを削除しました")
                                cloased = true
                            } else {
                                viewModel.setErrorMessage(error ?: "エラー")
                            }
                        }
                    },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("削除する")
                    }
                }
            }
        }
    )

    if (cloased) {
        LaunchedEffect(Unit) {
            delay(1000)
            viewModel.clearAll()
            onClose()
            cloased = false
        }
    }
}


@Preview(showBackground = true)
@Composable
fun PopupScreenPreview() {
    S310mainTheme {
        PopupScreen(viewModel = DeviceViewModel(), onClose = {})
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPass(
    viewModel: DeviceViewModel,
    onClose: () -> Unit // ポップアップを閉じるためのコールバック
) {
    var email by remember { mutableStateOf("") } // パスワードリセット用のメールアドレス
    var cloased by remember { mutableStateOf(false) } // ポップアップを閉じるためのフラグ
    Column(
        modifier = Modifier
            .wrapContentSize()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "パスワード再登録",
                fontSize = 24.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                viewModel.clearAll()
                onClose()
            }) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "閉じる")
            }
        }
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("登録済みメールアドレス") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                // ここでパスワードリセットのロジックを呼び出す
                // 例: viewModel.sendPasswordResetEmail(email)
                // ViewModelに実装されていない場合は、FirebaseAuthなどのAPIを直接利用
                if (email.isEmpty()) {
                    viewModel.setResetError("メールアドレスを入力してください。")
                } else {
                    viewModel.sendPasswordResetEmailWithCheck(email)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("メールを送信")
        }
        // ViewModelからのエラーメッセージ表示
        val errorMessage by viewModel.resetError.observeAsState(null)
        errorMessage?.let { message ->
            Text(
                text = message,
                color = Color.Red,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
        // ViewModelからの結果メッセージ表示 (成功時など)
        val resultText by viewModel.resetResult.collectAsState()
        if (resultText != "") {
            Text(
                text = resultText,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
            if (resultText == "パスワードリセットメールを送信しました。") {
                cloased = true
            }
        }
    }
    if (cloased) {
        LaunchedEffect(Unit) {
            delay(1000)
            viewModel.clearAll()
            onClose()
            cloased = false
        }
    }
}


