package com.example.s310main.viewmodel


import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.s310main.model.BTClientManager
import com.example.s310main.model.BTDeviceInfo
import com.example.s310main.model.BTRecieveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.sql.Timestamp
import com.google.firebase.database.*
import androidx.compose.runtime.mutableStateOf
import com.google.firebase.auth.FirebaseUser
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.tasks.await
import com.google.firebase.FirebaseException // Firebase の基本例外をインポート
import com.google.firebase.auth.FirebaseAuthException // Firebase Auth の例外をインポート
import android.util.Patterns
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.EmailAuthProvider


class DeviceViewModel : ViewModel() {
    private val TAG = "MySimpleViewModel"
    private val functions = Firebase.functions
    private val auth = FirebaseAuth.getInstance() // 匿名認証用

    private var _btClientManager: BTClientManager = BTClientManager()
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    private val _devicesInfo = MutableStateFlow<List<BTDeviceInfo>>(emptyList())
    val devicesInfo: StateFlow<List<BTDeviceInfo>> = _devicesInfo.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _receivedDataList = MutableStateFlow<List<BTRecieveData>>(emptyList())
    val receivedDataList: StateFlow<List<BTRecieveData>> = _receivedDataList.asStateFlow()

    // 最新のデータを保持するState
    private val _tbData = MutableStateFlow<String?>(null)//TB用データ
    val tbData: StateFlow<String?> = _tbData.asStateFlow()

    private val _speed = MutableStateFlow<String?>(null)//機速予定
    val speed: StateFlow<String?> = _speed.asStateFlow()

    private val _eAngle = MutableStateFlow<String?>(null)//E角度予定
    val eAngle: StateFlow<String?> = _eAngle.asStateFlow()

    private val _rAngle = MutableStateFlow<String?>(null)//R角度予定
    val rAngle: StateFlow<String?> = _rAngle.asStateFlow()


    private val _mainData = MutableStateFlow<String?>(null)//メイン基板用データ
    val mainData: StateFlow<String?> = _mainData.asStateFlow()

    private val _height = MutableStateFlow<String?>(null)//高度予定
    val height: StateFlow<String?> = _height.asStateFlow()

    private val _rpm = MutableStateFlow<String?>(null)//回転数予定
    val rpm: StateFlow<String?> = _rpm.asStateFlow()


    private val _atiData = MutableStateFlow<String?>(null)//姿勢角データ
    val atiData: StateFlow<String?> = _atiData.asStateFlow()

    private val _roll = MutableStateFlow<String?>(null)//ロール予定
    val roll: StateFlow<String?> = _roll.asStateFlow()

    private val _pitch = MutableStateFlow<String?>(null)//ピッチ予定
    val pitch: StateFlow<String?> = _pitch.asStateFlow()

    private val _yaw = MutableStateFlow<String?>(null)//ヨー予定
    val yaw: StateFlow<String?> = _yaw.asStateFlow()


    private val _swData = MutableStateFlow<String?>(null)//スイッチデータ
    val swData: StateFlow<String?> = _swData.asStateFlow()

    private val _sw1 = MutableStateFlow<String?>(null)
    val sw1: StateFlow<String?> = _sw1.asStateFlow()

    private val _sw2 = MutableStateFlow<String?>(null)
    val sw2: StateFlow<String?> = _sw2.asStateFlow()

    private val _sw3 = MutableStateFlow<String?>(null)
    val sw3: StateFlow<String?> = _sw3.asStateFlow()

    private val _selectedValue = MutableStateFlow<String?>("全体接合") // 初期値
    val selectedValue: StateFlow<String?> = _selectedValue

    private val _isSliderVisible = mutableStateOf(true) // 初期状態
    val isSliderVisible: Boolean get() = _isSliderVisible.value


    private val _currentUser = MutableLiveData<FirebaseUser?>()
    val currentUser: LiveData<FirebaseUser?> = _currentUser

    private val _loggedInEmail = MutableStateFlow<String?>(auth.currentUser?.email)
    val loggedInEmail: StateFlow<String?> = _loggedInEmail

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // UIに表示する結果のテキスト
    private val _resultText = MutableStateFlow("")
    val resultText: StateFlow<String> = _resultText

    private val _resetResult = MutableStateFlow("入力されたメールアドレスにパスワードリセットメールを送信します。")
    val resetResult: StateFlow<String> = _resetResult

    private val _resetError = MutableLiveData<String?>()
    val resetError: LiveData<String?> = _resetError

    init {
        // 現在のfirebaseユーザーを監視
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
            _loggedInEmail.value = auth.currentUser?.email
        }
    }


    /*mutableStateOf
    *     int型の書き方の例
    *     private val _currentY = MutableStateFlow<Int>(0)
    *     val currentY: StateFlow<Int> = _currentY.asStateFlow()
    */

    private var periodicLoadJob: Job? = null

    // 接続状態を管理するStateFlow
    private val _connectionStatus = MutableStateFlow<String>("")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    fun loadPairedDevicesPeriodically(repeatInterval: Long) {
        periodicLoadJob = viewModelScope.launch {
            while (isActive) {
                Log.d("mopi", "loadPairedDevicesPeriodically")
                loadPairedDevices()
                delay(repeatInterval)
            }
        }
    }

    fun cancelPeriodicLoading() {
        periodicLoadJob?.cancel()
    }

    private fun loadPairedDevices() {
        viewModelScope.launch {
            _devicesInfo.value = pairedDevicesInfo
        }
    }

    val pairedDevicesInfo: List<BTDeviceInfo>
        @SuppressLint("MissingPermission")
        get() = bluetoothAdapter?.bondedDevices?.map { device ->
            val isConnected = _btClientManager.isConnected(device.address)
            Log.d("mopi", "device: ${device.address}")
            Log.d("mopi", "isConnected: $isConnected")

            BTDeviceInfo(
                name = device.name ?: "N/A",
                address = device.address,
                isPaired = true,
                isConnected = isConnected
            )
        } ?: emptyList()

    fun onDeviceClicked(index: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            val deviceAddress = _devicesInfo.value[index].address

            withContext(Dispatchers.IO) {
                try {
                    _btClientManager.connectToDevice(deviceAddress)
                } catch (e: IOException) {
                    e.printStackTrace()
                } catch (e: IllegalArgumentException) {
                    e.printStackTrace()
                }
            }

            _isLoading.value = false

            val isConnected = _btClientManager.isConnected(deviceAddress)
            if (isConnected) {
                startReceivingData(deviceAddress)
            }

            val currentDevices = _devicesInfo.value.toMutableList()
            val updatedDevice = currentDevices[index].copy(isConnected = isConnected)
            currentDevices[index] = updatedDevice
            _devicesInfo.value = currentDevices
        }
    }

    fun onButtonClicked(deviceAddress: String) {
        viewModelScope.launch {
            // 接続状態の初期化
            _connectionStatus.value = "接続中..."
            Log.d("DeviceViewModel","接続中")

            // IOスレッドで接続処理を実行
            withContext(Dispatchers.IO) {
                try {
                    _btClientManager.connectToDevice(deviceAddress)
                } catch (e: IOException) {
                    e.printStackTrace()
                    _connectionStatus.value = "接続に失敗しました: ${e.message}"
                    return@withContext
                } catch (e: IllegalArgumentException) {
                    e.printStackTrace()
                    _connectionStatus.value = "接続に失敗しました: ${e.message}"
                    return@withContext
                }
            }

            // ロード完了後の処理
            _isLoading.value = false

            // 接続状態の確認
            val isConnected = _btClientManager.isConnected(deviceAddress)
            if (isConnected) {
                _connectionStatus.value = "接続が完了しました"
                Log.d("DeviceViewModel","接続完了")
                startReceivingData(deviceAddress)
            } else {
                _connectionStatus.value = "接続できませんでした"
                Log.d("DeviceViewModel","接続失敗")
            }

            // 接続状態をデバイスリストに反映
            val currentDevices = _devicesInfo.value.toMutableList()

            // deviceAddressでデバイスを検索して、接続状態を更新
            val deviceIndex = currentDevices.indexOfFirst { it.address == deviceAddress }

            // デバイスがリストに存在する場合
            if (deviceIndex != -1) {
                val updatedDevice = currentDevices[deviceIndex].copy(isConnected = isConnected)
                currentDevices[deviceIndex] = updatedDevice
                _devicesInfo.value = currentDevices
            }
        }
    }



    private fun startReceivingData(deviceAddress: String) {
        viewModelScope.launch(Dispatchers.IO) {
            while (_btClientManager.isConnected(deviceAddress)) {
                try {
                    val rawData = _btClientManager.receiveDataFromDevice(deviceAddress)
                    val deviceName: String? = _btClientManager.deviceName(deviceAddress)

                    if (rawData != null) {
                        val receiveData = BTRecieveData(
                            deviceName = deviceName,
                            deviceAddress = deviceAddress,
                            data = rawData,  // カテゴライズせずにそのまま保存
                            timestamp = Timestamp(System.currentTimeMillis())
                        )
                        withContext(Dispatchers.Main) {
                            _receivedDataList.value += listOf(receiveData)
                            updateLatestData(rawData)
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    break
                }
            }
        }
    }

    private fun updateLatestData(rawData: String) {
        // データをカンマ（,）で分割
        val dataParts = rawData.split(",")

        for (part in dataParts) {
            val keyValue = part.split(":")
            if (keyValue.size == 2) {
                val key = keyValue[0].trim()
                val value = keyValue[1].trim()

                when (key) {
                    // TBデータ
                    "A" -> _speed.value = value
                    "B" -> _eAngle.value = value
                    "C" -> _rAngle.value = value

                    // メイン基板データ
                    "H" -> _height.value = value
                    "I" -> _rpm.value = value

                    // 姿勢角データ
                    "O" -> _roll.value = value
                    "P" -> _pitch.value = value
                    "Q" -> _yaw.value = value

                    // スイッチデータ
                    "V" -> _sw1.value = value
                    "W" -> _sw2.value = value
                    "X" -> _sw3.value = value
                }
            }
        }

        // 受信データを保持
        _tbData.value = rawData.trim()
        _mainData.value = rawData.trim()
        _atiData.value = rawData.trim()
        _swData.value = rawData.trim()
    }


    //接続状態を監視する関数
    // 接続状態をチェックする関数
    fun isDeviceConnected(deviceAddress: String): Boolean {
        return _btClientManager.isConnected(deviceAddress)
    }

    // 接続状態を更新する関数
    fun updateConnectionStatus(status: String) {
        _connectionStatus.value = status
    }


    // Firebase匿名サインイン
    fun signInAnonymously() {
        FirebaseAuth.getInstance().signInAnonymously()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = FirebaseAuth.getInstance().currentUser
                    Log.d("FirebaseAuth", "Signed in anonymously: ${user?.uid}")
                } else {
                    Log.e("FirebaseAuth", "Authentication failed: ${task.exception?.message}")
                }
            }
    }

    /**
     * メールアドレスとパスワードでユーザー登録を行う
     * @param email ユーザーのメールアドレス
     * @param password ユーザーのパスワード
     */
    /**
     * メールアドレスとパスワードでユーザー登録を行う
     * @param email ユーザーのメールアドレス
     * @param password ユーザーのパスワード
     */
    fun createUserWithEmailAndPassword(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _currentUser.value = auth.currentUser
                    _loggedInEmail.value = auth.currentUser?.email
                    _errorMessage.value = null // エラーをクリア
                    Log.d("FirebaseAuth", "User created successfully: ${_currentUser.value?.uid}")
                    signInWithEmailAndPassword(email, password) // ユーザー登録成功時、ログインまで行う
                } else {
                    val exception = task.exception
                    val localizedMessage = getLocalizedErrorMessage(exception ?: Exception("不明なエラー"))
                    _resultText.value = ""
                    _errorMessage.value = localizedMessage
                    Log.e("FirebaseAuth", "User creation failed: $localizedMessage", exception)
                }
            }
    }


    /**
     * メールアドレスとパスワードでサインインを行う
     * @param email ユーザーのメールアドレス
     * @param password ユーザーのパスワード
     */
    fun signInWithEmailAndPassword(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _currentUser.value = auth.currentUser
                    _loggedInEmail.value = auth.currentUser?.email
                    _errorMessage.value = null // エラーをクリア
                    _resultText.value = "ログイン成功: ${auth.currentUser?.email}"
                    Log.d("FirebaseAuth", "Signed in successfully: ${_currentUser.value?.uid}")
                } else {
                    task.exception?.let { exception -> // ★ここを修正★
                        _errorMessage.value = getLocalizedErrorMessage(exception) // ★task.exception を引数として渡す★
                        _resultText.value = "" // エラー発生時は結果テキストをクリア
                        Log.e("FirebaseAuth", "Sign in failed: ${exception.message}")
                    } ?: run {
                        // exception が null の場合（通常は発生しないが念のため）
                        _errorMessage.value = "不明なエラーが発生しました。"
                        _resultText.value = ""
                        Log.e("FirebaseAuth", "Sign in failed with no exception message.")
                    }
                }
            }
    }

    // ★追加: Firebase Exception を日本語メッセージに変換するヘルパー関数
    private fun getLocalizedErrorMessage(exception: Exception): String {
        return when (exception) {
            is FirebaseAuthException -> {
                when (exception.errorCode) {
                    "ERROR_INVALID_EMAIL", "invalid-email" -> "メールアドレスの形式が正しくありません。\nご確認ください。"
                    "ERROR_WRONG_PASSWORD", "wrong-password",
                    "ERROR_INVALID_CREDENTIAL", "invalid-credential" ->
                        "パスワードが間違っているか、認証情報が無効です。\nもう一度お試しください。"
                    "ERROR_USER_DISABLED", "user-disabled" ->
                        "このアカウントは無効化されています。\n管理者にお問い合わせください。"
                    "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL", "account-exists-with-different-credential" ->
                        "このメールアドレスは他のログイン方法ですでに使われています。\nログイン方法をご確認ください。"
                    "ERROR_CREDENTIAL_ALREADY_IN_USE", "credential-already-in-use" ->
                        "この認証情報は他のアカウントで使用されています。"
                    "ERROR_USER_NOT_FOUND", "user-not-found" -> "このメールアドレスのユーザーは見つかりません。\n新規登録をご検討ください。"
                    "ERROR_EMAIL_ALREADY_IN_USE", "email-already-in-use" -> "このメールアドレスは既に登録されています。\nログインまたはパスワードをお忘れの場合にお進みください。"
                    "ERROR_WEAK_PASSWORD", "weak-password" -> "パスワードが弱すぎます。\n半角英数字を組み合わせて6文字以上で設定してください。"
                    "ERROR_OPERATION_NOT_ALLOWED", "operation-not-allowed" -> "この操作は現在許可されていません。\nシステム管理者にお問い合わせください。"
                    "ERROR_TOO_MANY_REQUESTS", "too-many-requests" -> "短時間でのアクセスが多すぎます。\nしばらく時間をおいてから再度お試しください。"
                    "ERROR_REQUIRES_RECENT_LOGIN", "requires-recent-login" -> "セキュリティ強化のため、再ログインが必要です。"
                    // その他のFirebase Authエラーコードに対応 (必要に応じて追加)
                    else -> "認証エラーが発生しました: [${exception.errorCode}] ${exception.message}"
                }
            }
            is FirebaseFunctionsException -> {
                // Functionsからのカスタムエラーコードと詳細を利用
                val details = exception.details as? Map<String, Any>
                when (exception.code) {
                    FirebaseFunctionsException.Code.INVALID_ARGUMENT -> {
                        val customMessage = details?.get("customMessage") as? String
                        customMessage ?: "入力内容に問題があります。\nご確認ください。"
                    }
                    FirebaseFunctionsException.Code.UNAUTHENTICATED -> "認証されていません。\nログインし直してください。"
                    FirebaseFunctionsException.Code.PERMISSION_DENIED -> "操作の権限がありません。"
                    FirebaseFunctionsException.Code.NOT_FOUND -> "対象が見つかりませんでした。"
                    FirebaseFunctionsException.Code.UNAVAILABLE -> "サービスが一時的に利用できません。\nしばらくしてから再試行してください。"
                    // Functionsのその他のエラーコードに対応
                    else -> "サーバー処理エラーが発生しました: [${exception.code.name}] ${exception.message}"
                }
            }
            is IOException -> {
                // ネットワーク接続エラーなど
                "ネットワークに接続できません。\nインターネット接続をご確認ください。"
            }
            is FirebaseException -> {
                // その他のFirebase関連の例外（例: FirebaseFirestoreExceptionなど）
                "Firebaseサービスエラーが発生しました: ${exception.message}"
            }
            else -> {
                // プログラム内で発生する一般的なエラー
                "予期せぬエラーが発生しました: ${exception.message ?: "詳細不明"}"
            }
        }
    }

    /**
     * サインアウトを行う
     */
    fun signOut() {
        _resultText.value = "サインアウト中・・・"
        auth.signOut()
        _currentUser.value = null
        _loggedInEmail.value = null
        _resultText.value = "サインアウトが完了しました"
        Log.d("FirebaseAuth", "Signed out")
    }

    /**
     * 登録コードを検証し、Firebase Cloud Function を介してユーザー登録を行う
     * @param email ユーザーのメールアドレス
     * @param password ユーザーのパスワード
     * @param registrationCode ユーザーが入力した登録コード
     */
    fun registerUserWithCode(email: String, password: String, registrationCode: String) {
        _errorMessage.value = null

        auth.signInAnonymously() // まず匿名でサインイン
            .addOnCompleteListener { anonymousSignInTask ->
                if (anonymousSignInTask.isSuccessful) {
                    Log.d("FirebaseAuth", "Signed in anonymously. Calling Cloud Function.")
                    // 匿名サインインが成功したら、Functionsを呼び出す
                    val data: Map<String, String> = hashMapOf(
                        "email" to email,
                        "password" to password,
                        "registrationCode" to registrationCode
                    )

                    functions
                        .getHttpsCallable("registerUserWithCode")
                        .call(data)
                        .continueWith { task ->
                            if (task.isSuccessful) {
                                val result = task.result?.data as? Map<String, Any>
                                val customToken = result?.get("customToken") as? String

                                if (customToken != null) {
                                    // Cloud Function からカスタムトークンが返されたら、そのトークンでサインイン
                                    // これにより、匿名ユーザーセッションは新しいユーザーに自動的に置き換わる
                                    auth.signInWithCustomToken(customToken)
                                        .addOnCompleteListener { signInTask ->
                                            if (signInTask.isSuccessful) {
                                                _currentUser.value = auth.currentUser
                                                _loggedInEmail.value = auth.currentUser?.email
                                                _errorMessage.value = null
                                                Log.d("FirebaseAuth", "Signed in with custom token: ${_currentUser.value?.uid}")
                                                // ここでユーザーに成功メッセージを表示するなど
                                            } else {
                                                _errorMessage.value = signInTask.exception?.message
                                                Log.e("FirebaseAuth", "Sign in with custom token failed: ${signInTask.exception?.message}")
                                                // ★ サインイン失敗時は、匿名ユーザーセッションに戻るべきか、ログアウトすべきか検討
                                                // この場合は、もう匿名ユーザーに留まる必要がないので、ログアウトしてクリーンな状態に戻す
                                                auth.signOut()
                                                Log.d("FirebaseAuth", "Signed out after custom token sign-in failure.")
                                            }
                                        }
                                } else {
                                    _errorMessage.value = "Cloud Function did not return a custom token."
                                    Log.e("FirebaseAuth", "Cloud Function did not return a custom token.")
                                    // ★ カスタムトークンが返ってこなかった場合も、匿名ユーザーセッションを終了する
                                    auth.signOut()
                                    Log.d("FirebaseAuth", "Signed out after no custom token.")
                                }
                            } else {
                                val e = task.exception
                                if (e is com.google.firebase.functions.FirebaseFunctionsException) {
                                    val code = e.code
                                    val details = e.details
                                    _errorMessage.value = "Registration failed: ${e.message} (Code: $code)"
                                    Log.e("FirebaseAuth", "Cloud Function error: $code - ${e.message} - $details")
                                } else {
                                    _errorMessage.value = e?.message ?: "An unknown error occurred during registration."
                                    Log.e("FirebaseAuth", "Unknown error during Cloud Function call: ${e?.message}")
                                }
                                // ★ Functions呼び出し自体が失敗した場合も、匿名ユーザーセッションを終了する
                                auth.signOut()
                                Log.d("FirebaseAuth", "Signed out after Cloud Function call failure.")
                            }
                        }
                } else {
                    _errorMessage.value = anonymousSignInTask.exception?.message
                    Log.e("FirebaseAuth", "Anonymous sign in failed: ${anonymousSignInTask.exception?.message}")
                }
            }
    }

    fun setErrorMessage(message: String?) {
        _errorMessage.value = message
        _resultText.value = ""
    }

    fun setResult(msg: String) {
        _resultText.value = msg
        _errorMessage.value = ""
    }

    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val ref: DatabaseReference = database.getReference("data")

    private val _firebaseData = MutableStateFlow("データなし")
    val firebaseData: StateFlow<String> = _firebaseData

    init {
        startFirebaseListener()
    }

    private fun startFirebaseListener() {
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newData = snapshot.getValue(String::class.java)
                if (newData != null) {
                    viewModelScope.launch {
                        _firebaseData.value = newData
                    }
                    Log.d("DeviceViewModel", "Received data: $newData")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("DeviceViewModel", "Failed to read value.", error.toException())
            }
        })
    }

    fun setSelectedValue(value: String) {
        _selectedValue.value = value
    }

    fun toggleSlider() {
        _isSliderVisible.value = !_isSliderVisible.value
    }

    /**
     * Cloud FunctionsのhelloWorld関数を呼び出し、名前を送信します。
     * @param name 送信する名前の文字列
     */
    fun callHelloWorldWithName(email: String, pass: String, code: String) {
        _errorMessage.value = null // エラーをクリア
        _resultText.value = "Functionsを呼び出し中..."
        Log.d("DeviceViewModel", "Attempting anonymous sign-in.")

        // メアド形式チェックはここで続行
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            setErrorMessage("メールアドレスの形式が正しくありません。")
            _resultText.value = ""
            Log.d("DeviceViewModel", "Email format invalid.")
            return
        }

        viewModelScope.launch {
            try {
                // 匿名認証
                // ここで認証エラーが発生する可能性も考慮
                try {
                    auth.signInAnonymously().await()
                    Log.d("DeviceViewModel", "Anonymous sign-in successful. UID: ${auth.currentUser?.uid}")
                } catch (e: Exception) {
                    // 匿名認証エラーをキャッチし、getLocalizedErrorMessageで処理
                    val authErrorMessage = getLocalizedErrorMessage(e)
                    setErrorMessage("匿名認証エラー: $authErrorMessage")
                    _resultText.value = "エラー"
                    Log.e("DeviceViewModel", "Anonymous sign-in failed: $authErrorMessage", e)
                    return@launch // ここで処理を終了
                }


                val dataToSend: MutableMap<String, Any> = HashMap()
                dataToSend["code"] = code
                Log.d("DeviceViewModel", "Prepared data to send: ${dataToSend}")

                // Functionsの呼び出し
                val taskResult = functions
                    .getHttpsCallable("helloWorld")
                    .call(dataToSend)
                    .await()

                val responseDataMap = taskResult?.data as? Map<String, Any>
                val message = responseDataMap?.get("message") as? String

                if (message != null) {
                    _resultText.value = "$message"
                    Log.d("DeviceViewModel", "Function call successful: $message")
                    if("$message" == "認証コードが確認されました。"){
                        _resultText.value = "ユーザー登録中です。"
                        createUserWithEmailAndPassword(email, pass)
                    }
                } else {
                    val fallbackMessage = "Functionsからの応答形式が予期せぬものです。\nサーバーのログをご確認ください。"
                    _resultText.value = "エラー"
                    Log.w("DeviceViewModel", "Unexpected response format. Raw data: ${taskResult?.data}")
                    setErrorMessage(fallbackMessage) // 予期せぬ形式もエラーとして表示
                }

            } catch (e: Exception) {
                // Functions呼び出し自体のエラー（ネットワーク、タイムアウト、Functions側の未捕捉エラーなど）
                val errorMessageText = getLocalizedErrorMessage(e)
                _resultText.value = "エラー"
                Log.e("DeviceViewModel", "Function call failed: $errorMessageText", e)
                setErrorMessage(errorMessageText)
            } finally {
                auth.signOut()
                Log.d("DeviceViewModel", "Signed out from anonymous session.")
            }
        }
    }

    fun sendPasswordResetEmailWithCheck(email: String) {
        _resetError.value = null // エラーをクリア
        _resetResult.value = "メールアドレスの確認中・・・"
        if (email.isEmpty()) {
            _resetError.value = "メールアドレスを入力してください。"
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _resetError.value = "有効なメールアドレスを入力してください。"
            return
        }

        viewModelScope.launch {
            val auth = FirebaseAuth.getInstance()
            var didAnonymousLogin = false

            try {
                // 匿名ログイン
                if (auth.currentUser == null) {
                    val result = auth.signInAnonymously().await()
                    didAnonymousLogin = result.user != null
                }

                val functions = Firebase.functions
                val result = functions
                    .getHttpsCallable("sendPasswordResetIfUserExists")
                    .call(hashMapOf("email" to email))
                    .await()

                val data = result.data as Map<*, *>
                val status = data["result"] as? String

                if (status == "確認済み") {
                    // メール送信
                    auth.sendPasswordResetEmail(email).await()
                    _resetResult.value = "パスワードリセットメールを送信しました。"
                    _resetError.value = ""
                } else {
                    _resetError.value = "不明なレスポンスが返されました。"
                }

            } catch (e: FirebaseFunctionsException) {
                val errorCode = e.code
                val errorMessage = e.message ?: "エラーが発生しました"
                _resetError.value = when (errorCode) {
                    FirebaseFunctionsException.Code.NOT_FOUND -> "登録されていないメールアドレスです。"
                    FirebaseFunctionsException.Code.INVALID_ARGUMENT -> "有効なメールアドレスを入力してください。"
                    else -> errorMessage
                }
                _resetResult.value = ""
            } catch (e: FirebaseAuthException) {
                _resetError.value = "メール送信に失敗しました: ${e.message}"
                _resetResult.value = ""
            } finally {
                if (didAnonymousLogin) {
                    auth.signOut()
                }
            }
        }
    }

    fun reauthenticateAndRun(password: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val user = auth.currentUser
        val email = user?.email
        _errorMessage.value = null // エラーをクリア
        _resultText.value = "パスワードの確認中..."

        if (user != null && email != null) {
            val credential = EmailAuthProvider.getCredential(email, password)
            user.reauthenticate(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onSuccess()
                        _resultText.value = "パスワード認証に成功しました"
                    } else {
                        val ex = task.exception
                        onFailure(getLocalizedErrorMessage(ex ?: Exception("再認証に失敗しました")))
                    }
                }
        } else {
            onFailure("ユーザーが見つかりません")
        }
    }



    fun changePassword(newPassword: String, onResult: (Boolean, String?) -> Unit) {
        _errorMessage.value = null // エラーをクリア
        _resultText.value = "パスワード変更中..."
        val user = auth.currentUser
        if (user != null) {
            user.updatePassword(newPassword)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onResult(true, null)
                        _resultText.value = "パスワード認証に成功しました"
                    } else {
                        val ex = task.exception
                        onResult(false, getLocalizedErrorMessage(ex ?: Exception("パスワード変更に失敗しました")))
                    }
                }
        } else {
            onResult(false, "ユーザーが見つかりません")
        }
    }

    fun deleteUser(onResult: (Boolean, String?) -> Unit) {
        _errorMessage.value = null // エラーをクリア
        _resultText.value = "アカウント削除中..."
        val user = auth.currentUser
        if (user != null) {
            user.delete()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onResult(true, null)
                        _resultText.value = "アカウント削除に成功しました"
                    } else {
                        val ex = task.exception
                        onResult(false, getLocalizedErrorMessage(ex ?: Exception("アカウント削除に失敗しました")))
                    }
                }
        } else {
            onResult(false, "ユーザーが見つかりません")
        }
    }



    fun setResetError(message: String) {
        _resetError.value = message
    }

    fun clearAll(){
        _resetError.value = null
        _resetResult.value = ""
        _errorMessage.value = null // エラーをクリア
        _resultText.value = ""
    }

}

