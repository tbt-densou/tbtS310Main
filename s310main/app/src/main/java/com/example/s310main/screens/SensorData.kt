package com.example.s310main.screens

import com.google.firebase.database.IgnoreExtraProperties
import androidx.annotation.Keep

@Keep
@IgnoreExtraProperties
data class SensorData(
    val sw1: Double? = null,
    val speed: Double? = null,
    val height: Double? = null,
    val rpm: Double? = null,
    val roll: Double? = null,
    val pitch: Double? = null,
    val yaw: Double? = null,
    val eAngle: Double? = null,
    val rAngle: Double? = null,
    val timestamp: Long? = null
)


