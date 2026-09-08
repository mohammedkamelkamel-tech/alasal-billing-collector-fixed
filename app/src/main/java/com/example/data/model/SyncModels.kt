package com.example.data.model

data class DiscoveredDevice(
    val id: String,
    val name: String,
    val ipAddress: String,
    val port: Int
)

data class SyncHistory(
    val id: String = java.util.UUID.randomUUID().toString(),
    val date: String,
    val deviceName: String,
    val status: String,
    val sentRecords: Int,
    val receivedRecords: Int
)
