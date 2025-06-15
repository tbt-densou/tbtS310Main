package com.example.s310main.model

import java.sql.Timestamp

data class BTRecieveData(val deviceName: String?, val deviceAddress: String, val data: String, val timestamp: Timestamp)
