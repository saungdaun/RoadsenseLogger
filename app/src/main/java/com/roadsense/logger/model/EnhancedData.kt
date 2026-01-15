package com.roadsense.logger.model

data class EnhancedData(
    val currentSpeed: Float = 0f,
    val totalOdo: Float = 0f,
    val tripDistance: Float = 0f,
    val currentAccelZ: Float = 0f,
    val batteryVoltage: Float = 0f,
    val timeValid: Boolean = false,
    val hour: Int = 0,
    val minute: Int = 0,
    val btConnected: Boolean = false,
    val dataStreaming: Boolean = false,
    val packetCount: Int = 0,
    val systemState: Int = 0,
    val maxSpeed: Float = 0f,
    val avgSpeed: Float = 0f,
    val viewMode: Int = 0,
    val lastUpdateTime: Long = 0
)
