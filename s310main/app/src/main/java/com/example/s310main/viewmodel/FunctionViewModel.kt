package com.example.s310main.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.functions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FunctionViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val functions: FirebaseFunctions = Firebase.functions("us-central1") // リージョンが違うなら変更

    // 入力と結果の状態
    var value1 = mutableStateOf("")
    var value2 = mutableStateOf("")
    var value3 = mutableStateOf("")
    var resultText = mutableStateOf("")

    fun sendValuesToFunction() {
        viewModelScope.launch {
            resultText.value = "Signing in anonymously..."
            try {
                auth.signInAnonymously().await()

                val data = mapOf(
                    "value1" to value1.value,
                    "value2" to value2.value,
                    "value3" to value3.value
                )

                val response = functions
                    .getHttpsCallable("yourFunctionName") // Cloud Function 名をここに
                    .call(data)
                    .await()

                val resultMap = response.data as? Map<*, *>
                val message = resultMap?.get("message") as? String

                resultText.value = message ?: "No message received."
            } catch (e: Exception) {
                resultText.value = "Error: ${e.message}"
            } finally {
                auth.signOut()
            }
        }
    }
}
