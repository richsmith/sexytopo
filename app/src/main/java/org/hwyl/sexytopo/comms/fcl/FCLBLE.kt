/*
 * FCL Enhanced BLE Protocol v2.0 - Split Packet Implementation for SexyTopo
 * ========================================================================
 *
 * This Kotlin implementation handles communication with FCL cave survey instruments
 * using the enhanced split-packet protocol that provides comprehensive shot quality data
 * and environmental monitoring capabilities while overcoming MTU limitations.
 *
 * PROTOCOL FEATURES:
 * - Split packet reception: Primary (20 bytes) + Extended (14 bytes)
 * - State machine similar to BRIC4 for ordered packet handling
 * - Real-time shot quality assessment (0-100%)
 * - Environmental monitoring (temperature, magnetic field, interference)
 * - Full 3-axis orientation data (azimuth, inclination, roll)
 * - Battery and device health monitoring
 * - CRC16 packet integrity verification
 *
 * SPLIT PACKET RECEPTION:
 * - PRIMARY PACKET (20 bytes): Core measurement + quality + CRC
 * - EXTENDED PACKET (14 bytes): Environmental data + metadata
 * - State machine: IDLE → PRIMARY_RECEIVED → COMPLETE → IDLE
 * - Automatic timeout handling and error recovery
 *
 * ENHANCED PACKET RECONSTRUCTION:
 * The complete 34-byte equivalent data is reconstructed from:
 * - Primary: Header(2) + Status(1) + Battery(1) + Azimuth(4) + Inclination(4) + Distance(4) + Quality(2) + CRC(2)
 * - Extended: MagField(2) + ExpMagField(2) + MagDip(2) + ExpMagDip(2) + Temperature(2) + Roll(2) + ID(2)
 *
 * STATE MACHINE FLOW:
 * IDLE → (Primary packet) → PRIMARY_RECEIVED → (Extended packet) → COMPLETE → (Callback) → IDLE
 *                                 ↓ (timeout)
 *                               ERROR → IDLE
 */

package org.hwyl.sexytopo.comms.fcl

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

private val FCL_BLE_SERVICE = UUID.fromString("9cc8ffd8-1b11-4848-9026-529e47d4c500")
private val COMMAND_CHARACTERISTIC = UUID.fromString("9cc8ffd8-1b11-4848-9026-529e47d4c501")
private val PRIMARY_LEG_CHARACTERISTIC = UUID.fromString("9cc8ffd8-1b11-4848-9026-529e47d4c504")
private val EXTENDED_DATA_CHARACTERISTIC = UUID.fromString("9cc8ffd8-1b11-4848-9026-529e47d4c505")
private val NOTIFICATION_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

typealias LegCallback = (Float, Float, Float) -> Unit
typealias EnhancedLegCallback = (EnhancedLegData) -> Unit
typealias StatusCallback = (Int, String) -> Unit

private const val ACK = 0x55
private const val LASER_ON = 0x36
private const val LASER_OFF = 0x37
private const val DEVICE_OFF = 0x34
private const val TAKE_SHOT = 0x38

// Enhanced protocol constants
private const val PRIMARY_PACKET_SIZE = 20
private const val EXTENDED_PACKET_SIZE = 14
private const val PROTOCOL_VERSION = 2
private const val MAGIC_NIBBLE = 0xF

// Status flags
private const val FLAG_VERTICAL_SHOT = 0x01
private const val FLAG_HIGH_INTERFERENCE = 0x02
private const val FLAG_LOW_BATTERY = 0x04
private const val FLAG_TEMPERATURE_WARNING = 0x08
private const val FLAG_POOR_SHOT_QUALITY = 0x10
private const val FLAG_CALIBRATION_OLD = 0x20
private const val FLAG_EXTENDED_DATA = 0x40

// State machine states
private enum class PacketState {
    IDLE,
    PRIMARY_RECEIVED,
    COMPLETE,
    ERROR
}

// Data classes for packet fragments
private data class PrimaryPacketData(
    val sequenceNumber: Int,
    val statusFlags: Int,
    val batteryLevel: Int,
    val azimuth: Float,
    val inclination: Float,
    val distance: Float,
    val shotQuality: Float,
    val protocolVersion: Int,
    val isValid: Boolean
)

private data class ExtendedPacketData(
    val currentMagneticField: Float,
    val expectedMagneticField: Float,
    val currentMagneticDip: Float,
    val expectedMagneticDip: Float,
    val temperature: Float,
    val rollAngle: Float,
    val measurementId: Int
)

data class EnhancedLegData(
    val azimuth: Float,
    val inclination: Float,
    val distance: Float,
    val shotQuality: Float,           // 0.0 - 1.0
    val currentMagneticField: Float,  // µT (measured)
    val expectedMagneticField: Float, // µT (from config)
    val currentMagneticDip: Float,    // degrees (measured)
    val expectedMagneticDip: Float,   // degrees (from config)
    val temperature: Float,           // °C
    val rollAngle: Float,             // degrees (rotation around measurement axis)
    val batteryLevel: Int,            // 0-100%
    val measurementId: Int,           // Sequential counter
    val statusFlags: Int,             // Status flags
    val sequenceNumber: Int,          // Packet sequence
    val protocolVersion: Int,         // Protocol version
    val isValid: Boolean              // Packet validation result
) {
    // Convenience methods for checking flags
    fun hasVerticalWarning(): Boolean = (statusFlags and FLAG_VERTICAL_SHOT) != 0
    fun hasInterferenceWarning(): Boolean = (statusFlags and FLAG_HIGH_INTERFERENCE) != 0
    fun hasLowBattery(): Boolean = (statusFlags and FLAG_LOW_BATTERY) != 0
    fun hasTemperatureWarning(): Boolean = (statusFlags and FLAG_TEMPERATURE_WARNING) != 0
    fun hasPoorQuality(): Boolean = (statusFlags and FLAG_POOR_SHOT_QUALITY) != 0
    fun hasOldCalibration(): Boolean = (statusFlags and FLAG_CALIBRATION_OLD) != 0
    fun hasExtendedData(): Boolean = (statusFlags and FLAG_EXTENDED_DATA) != 0

    // Quality assessment
    fun getQualityDescription(): String = when {
        shotQuality >= 0.9f -> "Excellent"
        shotQuality >= 0.8f -> "Good"
        shotQuality >= 0.7f -> "Fair"
        shotQuality >= 0.5f -> "Poor"
        else -> "Very Poor"
    }

    // Magnetic field assessment
    fun getMagneticFieldDeviation(): Float = currentMagneticField - expectedMagneticField

    fun getMagneticFieldDescription(): String = when {
        kotlin.math.abs(getMagneticFieldDeviation()) < 2.0f -> "Normal"
        kotlin.math.abs(getMagneticFieldDeviation()) < 5.0f -> "Slight anomaly"
        kotlin.math.abs(getMagneticFieldDeviation()) < 10.0f -> "Moderate anomaly"
        else -> "Significant anomaly"
    }

    // Magnetic dip assessment
    fun getMagneticDipDeviation(): Float = currentMagneticDip - expectedMagneticDip

    fun getMagneticDipDescription(): String = when {
        kotlin.math.abs(getMagneticDipDeviation()) < 2.0f -> "Normal"
        kotlin.math.abs(getMagneticDipDeviation()) < 5.0f -> "Slight anomaly"
        kotlin.math.abs(getMagneticDipDeviation()) < 10.0f -> "Moderate anomaly"
        else -> "Significant anomaly"
    }

    // Status description
    fun getStatusDescription(): String = when {
        hasLowBattery() -> "Low Battery"
        hasTemperatureWarning() -> "Temperature Warning"
        hasInterferenceWarning() -> "Magnetic Interference"
        hasVerticalWarning() -> "Vertical Shot"
        hasPoorQuality() -> "Poor Quality"
        hasOldCalibration() -> "Calibration Old"
        else -> "OK"
    }
}

@SuppressLint("MissingPermission")
class FCLBLE(val device: BluetoothDevice,
             val context: Context,
             val leg_callback: LegCallback,
             val enhanced_leg_callback: EnhancedLegCallback? = null,
             val statusCallback: StatusCallback?) {

    companion object {
        const val CONNECTED = 1
        const val DISCONNECTED = 2
        const val CONNECTION_FAILED = 3
        private const val TAG = "FCLBLE"
        private const val PACKET_TIMEOUT_MS = 1000L // 1 second timeout between packets
    }

    private var command: BluetoothGattCharacteristic? = null
    private var primaryDataIn: BluetoothGattCharacteristic? = null
    private var extendedDataIn: BluetoothGattCharacteristic? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var expectedSequence = 0

    // State machine for split packet handling
    private var currentState = PacketState.IDLE
    private var primaryPacketData: PrimaryPacketData? = null
    private var extendedPacketData: ExtendedPacketData? = null
    private val handler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null

    // Add state tracking for descriptor setup
    private var setupState = 0 // 0=not started, 1=primary done, 2=both done

    private val callback = object: BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    bluetoothGatt = gatt
                    Handler(Looper.getMainLooper()).post {
                        bluetoothGatt?.discoverServices()
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.w(TAG, "Successfully disconnected from $deviceAddress")
                    gatt.close()
                    statusCallback?.invoke(DISCONNECTED, "Successful disconnection")
                }
            } else {
                val msg = "Error $status encountered for $deviceAddress! Disconnecting..."
                Log.w(TAG, msg)
                gatt.close()
                statusCallback?.invoke(CONNECTION_FAILED, msg)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            with(gatt) {
                Log.i(TAG, "Discovered ${services.size} services for ${device.address}")
                val service = getService(FCL_BLE_SERVICE)
                Log.i(TAG, "Service holds ${service.characteristics.size} characteristics")

                // Set up command characteristic
                command = service.getCharacteristic(COMMAND_CHARACTERISTIC)
                Log.i(TAG, "Command char: $command")
                command?.value = ByteArray(1)
                command?.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE

                // Set up primary data characteristic (primary packets)
                primaryDataIn = service.getCharacteristic(PRIMARY_LEG_CHARACTERISTIC)
                Log.i(TAG, "Primary characteristic found: ${primaryDataIn != null}")

                // Set up extended data characteristic (extended packets)
                extendedDataIn = service.getCharacteristic(EXTENDED_DATA_CHARACTERISTIC)
                Log.i(TAG, "Extended characteristic found: ${extendedDataIn != null}")

                var primaryNotificationEnabled = false
                var extendedNotificationEnabled = false

                // Setup primary characteristic notifications
                if (primaryDataIn != null) {
                    Log.i(TAG, "Setting up primary packet notifications")
                    primaryNotificationEnabled = setCharacteristicNotification(primaryDataIn, true)
                    Log.i(TAG, "Primary notification setup result: $primaryNotificationEnabled")

                    if (primaryNotificationEnabled) {
                        val descriptor = primaryDataIn?.getDescriptor(NOTIFICATION_DESCRIPTOR)
                        Log.i(TAG, "Primary descriptor: $descriptor")
                        descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        val descriptorWriteResult = writeDescriptor(descriptor)
                        Log.i(TAG, "Primary descriptor write result: $descriptorWriteResult")
                    }
                }

                // Setup extended characteristic notifications
                if (extendedDataIn != null) {
                    Log.i(TAG, "Setting up extended packet notifications")
                    extendedNotificationEnabled = setCharacteristicNotification(extendedDataIn, true)
                    Log.i(TAG, "Extended notification setup result: $extendedNotificationEnabled")

                    if (extendedNotificationEnabled) {
                        val descriptor = extendedDataIn?.getDescriptor(NOTIFICATION_DESCRIPTOR)
                        Log.i(TAG, "Extended descriptor: $descriptor")
                        descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        val descriptorWriteResult = writeDescriptor(descriptor)
                        Log.i(TAG, "Extended descriptor write result: $descriptorWriteResult")

                        // CRITICAL: Add delay between descriptor writes to prevent conflicts
                        Thread.sleep(100)
                    }
                } else {
                    Log.e(TAG, "Extended characteristic not found - UUID: $EXTENDED_DATA_CHARACTERISTIC")
                }

                // Check connection status - both characteristics are REQUIRED for split protocol
                val connectionOk = (command != null) && primaryNotificationEnabled && extendedNotificationEnabled

                if (connectionOk) {
                    Log.i(TAG, "Split protocol connection successful")
                    statusCallback?.invoke(CONNECTED, "Connected - Enhanced Split Protocol v2.0")
                } else {
                    Log.e(TAG, "Split protocol setup failed")
                    Log.e(TAG, "Command: ${command != null}, Primary: $primaryNotificationEnabled, Extended: $extendedNotificationEnabled")
                    statusCallback?.invoke(CONNECTION_FAILED, "Split protocol setup failed")
                }
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                when (descriptor.characteristic.uuid) {
                    PRIMARY_LEG_CHARACTERISTIC -> {
                        setupState = 1
                        setupExtendedCharacteristic(gatt)
                    }
                    EXTENDED_DATA_CHARACTERISTIC -> {
                        setupState = 2
                        finalizeBLEConnection()
                    }
                }
            } else {
                Log.e(TAG, "Descriptor write failed, status: $status")
                statusCallback?.invoke(CONNECTION_FAILED, "Descriptor write failed")
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            when (characteristic.uuid) {
                PRIMARY_LEG_CHARACTERISTIC -> {
                    handlePrimaryPacket(characteristic.value)
                }
                EXTENDED_DATA_CHARACTERISTIC -> {
                    handleExtendedPacket(characteristic.value)
                }
                else -> {
                    Log.w(TAG, "Unexpected characteristic: ${characteristic.uuid}")
                }
            }
        }
    }

    private fun setupExtendedCharacteristic(gatt: BluetoothGatt) {
        if (extendedDataIn != null) {
            val extendedNotificationEnabled = gatt.setCharacteristicNotification(extendedDataIn, true)

            if (extendedNotificationEnabled) {
                val descriptor = extendedDataIn?.getDescriptor(NOTIFICATION_DESCRIPTOR)
                if (descriptor != null) {
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    val descriptorWriteResult = gatt.writeDescriptor(descriptor)

                    if (!descriptorWriteResult) {
                        Log.e(TAG, "Extended descriptor write failed")
                        statusCallback?.invoke(CONNECTION_FAILED, "Extended descriptor write failed")
                    }
                } else {
                    Log.e(TAG, "Extended descriptor not found")
                    statusCallback?.invoke(CONNECTION_FAILED, "Extended descriptor not found")
                }
            } else {
                Log.e(TAG, "Extended notification setup failed")
                statusCallback?.invoke(CONNECTION_FAILED, "Extended notification setup failed")
            }
        } else {
            Log.e(TAG, "Extended characteristic not found")
            statusCallback?.invoke(CONNECTION_FAILED, "Extended characteristic not found")
        }
    }

    private fun finalizeBLEConnection() {
        if (command != null && setupState == 2) {
            Log.i(TAG, "FCL split protocol connected")
            statusCallback?.invoke(CONNECTED, "Connected - Enhanced Split Protocol v2.0")
        } else {
            Log.e(TAG, "Split protocol setup incomplete")
            statusCallback?.invoke(CONNECTION_FAILED, "Split protocol setup incomplete")
        }
    }

    private fun handlePrimaryPacket(data: ByteArray) {
        try {
            val primaryData = parsePrimaryPacket(data)
            if (primaryData != null && primaryData.isValid) {

                // Cancel any existing timeout
                timeoutRunnable?.let { handler.removeCallbacks(it) }

                // Update state machine
                currentState = PacketState.PRIMARY_RECEIVED
                primaryPacketData = primaryData

                // Set timeout for extended packet
                timeoutRunnable = Runnable {
                    Log.w(TAG, "Extended packet timeout")
                    handlePacketError("Packet timeout")
                }
                handler.postDelayed(timeoutRunnable!!, PACKET_TIMEOUT_MS)

            } else {
                handlePacketError("Primary packet parsing failed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Primary packet error: ${e.message}")
            handlePacketError("Primary packet error")
        }
    }

    private fun handleExtendedPacket(data: ByteArray) {
        try {
            if (currentState != PacketState.PRIMARY_RECEIVED) {
                handlePacketError("Extended packet out of sequence")
                return
            }

            val extendedData = parseExtendedPacket(data)
            if (extendedData != null) {

                // Cancel timeout
                timeoutRunnable?.let { handler.removeCallbacks(it) }

                // Update state machine
                currentState = PacketState.COMPLETE
                extendedPacketData = extendedData

                // Combine packets and process
                val primaryData = primaryPacketData
                if (primaryData != null) {
                    val enhancedData = combinePackets(primaryData, extendedData)
                    processCompleteMeasurement(enhancedData)
                }

                // Reset state machine
                resetStateMachine()

            } else {
                handlePacketError("Extended packet parsing failed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Extended packet error: ${e.message}")
            handlePacketError("Extended packet error")
        }
    }

    private fun handlePacketTimeout() {
        handlePacketError("Packet timeout")
    }

    private fun handlePacketError(error: String) {
        currentState = PacketState.ERROR
        resetStateMachine()
    }

    private fun resetStateMachine() {
        currentState = PacketState.IDLE
        primaryPacketData = null
        extendedPacketData = null
        timeoutRunnable?.let { handler.removeCallbacks(it) }
        timeoutRunnable = null
    }

    private fun parsePrimaryPacket(data: ByteArray): PrimaryPacketData? {
        if (data.size != PRIMARY_PACKET_SIZE) {
            return null
        }

        try {
            val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

            // Parse header (2 bytes)
            val header = buffer.getShort().toInt() and 0xFFFF
            val magic = (header shr 12) and 0xF
            val version = (header shr 8) and 0xF
            val sequence = header and 0xFF

            // Validate header
            if (magic != MAGIC_NIBBLE || version != PROTOCOL_VERSION) {
                return null
            }

            // Parse packet contents
            val statusFlags = buffer.get().toInt() and 0xFF
            val batteryLevel = buffer.get().toInt() and 0xFF
            val azimuth = buffer.getFloat()
            val inclination = buffer.getFloat()
            val distance = buffer.getFloat()
            val shotQuality = (buffer.getShort().toInt() and 0xFFFF) / 1000.0f

            // Verify CRC
            val receivedCrc = buffer.getShort().toInt() and 0xFFFF
            val calculatedCrc = calculateCRC16(data, PRIMARY_PACKET_SIZE - 2)

            val isValid = (receivedCrc == calculatedCrc) &&
                    validateMeasurementRanges(azimuth, inclination, distance, shotQuality)

            return PrimaryPacketData(
                sequenceNumber = sequence,
                statusFlags = statusFlags,
                batteryLevel = batteryLevel,
                azimuth = azimuth,
                inclination = inclination,
                distance = distance,
                shotQuality = shotQuality,
                protocolVersion = version,
                isValid = isValid
            )

        } catch (e: Exception) {
            return null
        }
    }

    private fun parseExtendedPacket(data: ByteArray): ExtendedPacketData? {
        if (data.size != EXTENDED_PACKET_SIZE) {
            return null
        }

        try {
            val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

            val currentMagneticField = (buffer.getShort().toInt() and 0xFFFF) / 10.0f
            val expectedMagneticField = (buffer.getShort().toInt() and 0xFFFF) / 10.0f
            val currentMagneticDip = buffer.getShort() / 100.0f
            val expectedMagneticDip = buffer.getShort() / 100.0f
            val temperature = buffer.getShort() / 100.0f
            val rollAngle = buffer.getShort() / 100.0f
            val measurementId = buffer.getShort().toInt() and 0xFFFF

            return ExtendedPacketData(
                currentMagneticField = currentMagneticField,
                expectedMagneticField = expectedMagneticField,
                currentMagneticDip = currentMagneticDip,
                expectedMagneticDip = expectedMagneticDip,
                temperature = temperature,
                rollAngle = rollAngle,
                measurementId = measurementId
            )

        } catch (e: Exception) {
            return null
        }
    }

    private fun combinePackets(primary: PrimaryPacketData, extended: ExtendedPacketData): EnhancedLegData {
        return EnhancedLegData(
            azimuth = primary.azimuth,
            inclination = primary.inclination,
            distance = primary.distance,
            shotQuality = primary.shotQuality,
            currentMagneticField = extended.currentMagneticField,
            expectedMagneticField = extended.expectedMagneticField,
            currentMagneticDip = extended.currentMagneticDip,
            expectedMagneticDip = extended.expectedMagneticDip,
            temperature = extended.temperature,
            rollAngle = extended.rollAngle,
            batteryLevel = primary.batteryLevel,
            measurementId = extended.measurementId,
            statusFlags = primary.statusFlags,
            sequenceNumber = primary.sequenceNumber,
            protocolVersion = primary.protocolVersion,
            isValid = primary.isValid
        )
    }

    private fun processCompleteMeasurement(enhancedData: EnhancedLegData) {
        // Send ACK for completed measurement
        sendAck(enhancedData.sequenceNumber)

        // Call both callbacks
        leg_callback(enhancedData.azimuth, enhancedData.inclination, enhancedData.distance)
        enhanced_leg_callback?.invoke(enhancedData)
    }

    private fun validateMeasurementRanges(azimuth: Float, inclination: Float,
                                          distance: Float, quality: Float): Boolean {
        return azimuth in 0.0f..360.0f &&
                inclination in -90.0f..90.0f &&
                distance in 0.0f..999.9f &&
                quality in 0.0f..1.0f
    }

    private fun calculateCRC16(data: ByteArray, length: Int): Int {
        var crc = 0xFFFF
        for (i in 0 until length) {
            crc = crc xor (data[i].toInt() and 0xFF shl 8)
            for (j in 0 until 8) {
                crc = if (crc and 0x8000 != 0) {
                    (crc shl 1) xor 0x1021
                } else {
                    crc shl 1
                }
                crc = crc and 0xFFFF
            }
        }
        return crc
    }

    private fun sendAck(sequenceNumber: Int) {
        sendCommand(ACK + (sequenceNumber and 0xFF))
    }

    fun connect() {
        device.connectGatt(context, false, callback)
    }

    fun disconnect() {
        // Clean up state machine
        resetStateMachine()
        bluetoothGatt?.disconnect()
    }

    fun isConnected(): Boolean {
        val bluetoothMgr: BluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val devs = bluetoothMgr.getConnectedDevices(BluetoothProfile.GATT)
        return (device in devs)
    }

    fun sendCommand(value: Int) {
        command?.let {
            it.setValue(value, BluetoothGattCharacteristic.FORMAT_UINT8, 0)
            bluetoothGatt?.writeCharacteristic(it)
        }
    }

    fun laserOn() {
        sendCommand(LASER_ON)
    }

    fun laserOff() {
        sendCommand(LASER_OFF)
    }

    fun takeShot() {
        sendCommand(TAKE_SHOT)
    }

    fun deviceOff() {
        sendCommand(DEVICE_OFF)
    }
}