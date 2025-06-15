import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import android.util.Log
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.FirebaseAuth

object GoogleSheetsHelper {
    private const val SCRIPT_URL = "ここにスプレッドシートのURLが入ります"

    fun writeToSheet(rowData: List<Any?>) {
        Thread {
            try {
                val url = URL(SCRIPT_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")

                val jsonPayload = JSONObject()
                jsonPayload.put("value", rowData.joinToString(","))

                val outputStream = connection.outputStream
                outputStream.write(jsonPayload.toString().toByteArray())
                outputStream.flush()
                outputStream.close()

                val responseCode = connection.responseCode
                Log.d("GoogleSheetsHelper", "Response: $responseCode")
            } catch (e: Exception) {
                Log.e("GoogleSheetsHelper", "Error: ${e.message}", e)
                e.printStackTrace()
            }
        }.start()
    }
}

object FirebaseHelper {
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val dataRef: DatabaseReference = database.getReference("sensorData")

    fun writeToFirebase(rowData: List<Any?>, selectedValue: String?, currentSelectedItem:String?) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            if (rowData.any { it != null }) {
                val dataMap = mutableMapOf<String, Any?>()

                rowData.getOrNull(0)?.let { dataMap["sw1"] = it }
                rowData.getOrNull(1)?.let { dataMap["speed"] = it }
                rowData.getOrNull(2)?.let { dataMap["height"] = it }
                rowData.getOrNull(3)?.let { dataMap["rpm"] = it }
                rowData.getOrNull(4)?.let { dataMap["roll"] = it }
                rowData.getOrNull(5)?.let { dataMap["pitch"] = it }
                rowData.getOrNull(6)?.let { dataMap["yaw"] = it }
                rowData.getOrNull(7)?.let { dataMap["eAngle"] = it }
                rowData.getOrNull(8)?.let { dataMap["rAngle"] = it }
                rowData.getOrNull(9)?.let { dataMap["latitude"] = it }
                rowData.getOrNull(10)?.let { dataMap["longitude"] = it }

                dataMap["timestamp"] = System.currentTimeMillis()

                dataRef.push().setValue(dataMap)
                    .addOnSuccessListener {
                        Log.d("FirebaseHelper", "🔥 Firebase にデータ送信成功！")
                    }
                    .addOnFailureListener { e ->
                        Log.e("FirebaseHelper", "⚠️ Firebase 送信エラー: ${e.message}")
                    }

                // ここ！selectedValueがnullや空じゃなければ保存
                selectedValue?.takeIf { it.isNotEmpty() }?.let { key ->
                    val validKeys = listOf(
                        "デバック",
                        "全体接合",
                        "1st走行",
                        "2nd走行",
                        "1stTF",
                        "2ndTF",
                        "3rdTF",
                        "4thTF",
                        "5thTF",
                        "6thTF",
                        "7thTF",
                        "最終TF"
                    )

                    val safeKey = if (key in validKeys) key else "other"

                    currentSelectedItem?.takeIf { it.isNotEmpty() }?.let { key ->
                        val flightKeys = listOf(
                            "1本目",
                            "2本目",
                            "3本目",
                            "4本目",
                            "5本目",
                            "6本目",
                            "7本目",
                            "8本目",
                            "9本目",
                            "10本目",
                            "11本目",
                            "12本目",
                            "13本目",
                            "14本目",
                            "15本目"
                        )

                        val fliKey = if (key in flightKeys) key else "other"

                        val sortedRef = database.getReference(safeKey).child(fliKey)
                        sortedRef.push().setValue(dataMap)
                            .addOnSuccessListener {
                                Log.d("FirebaseHelper", "✅ sortedData/$safeKey に保存成功！")
                            }
                            .addOnFailureListener { e ->
                                Log.e(
                                    "FirebaseHelper",
                                    "⚠️ sortedData/$safeKey 保存失敗: ${e.message}"
                                )
                            }
                    }
                }

            } else {
                Log.e("FirebaseHelper", "⚠️ 有効なデータがありません。保存をスキップします。")
            }
        } else {
            Log.e("FirebaseHelper", "⚠️ ユーザーが認証されていません。")
        }
    }
}
