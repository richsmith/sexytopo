package org.hwyl.sexytopo.comms.sap6

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

private val CAVE_BLE_SERVICE = UUID.fromString("137c4435-8a64-4bcb-93f1-3792c6bdc965")
private val COMMAND_CHARACTERISTIC = UUID.fromString("137c4435-8a64-4bcb-93f1-3792c6bdc967")

private val LEG_CHARACTERISTIC = UUID.fromString("137c4435-8a64-4bcb-93f1-3792c6bdc968")
private val NOTIFICATION_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
typealias LegCallback = (Float, Float, Float, Float) -> Unit
typealias StatusCallback = (Int, String) -> Unit

private const val ACK = 0x55
private const val START_CAL = 0x31
private const val STOP_CAL = 0x30
private const val LASER_ON = 0x36
private const val LASER_OFF = 0x37
private const val DEVICE_OFF = 0x34
private const val TAKE_SHOT = 0x38

@SuppressLint("MissingPermission")
class CaveBLE(val device: BluetoothDevice,
              val context: Context,
              val leg_callback: LegCallback,
              val statusCallback: StatusCallback?) {
    companion object {
        const val CONNECTED = 1
        const val DISCONNECTED = 2
        const val CONNECTION_FAILED = 3
    }

    private var command: BluetoothGattCharacteristic? = null
    private var dataIn: BluetoothGattCharacteristic? = null
    private var bluetoothGatt: BluetoothGatt? = null
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
                    Log.w("BluetoothGattCallback", "Successfully disconnected from $deviceAddress")
                    gatt.close()
                    statusCallback?.invoke(DISCONNECTED, "Successful disconnection")
                }
            } else {
                val msg = "Error $status encountered for $deviceAddress! Disconnecting..."
                Log.w("BluetoothGattCallback", msg)
                gatt.close()
                statusCallback?.invoke(CONNECTION_FAILED, msg)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            with(gatt) {
                Log.w("BluetoothGattCallback", "Discovered ${services.size} services for ${device.address}")
                val service = getService(CAVE_BLE_SERVICE)
                Log.w("BluetoothGattCallback", "Service holds ${service.characteristics.size}")
                command = service.getCharacteristic(COMMAND_CHARACTERISTIC)
                Log.w("BluetoothGattCallback", "Cmd char: $command")
                command?.value = ByteArray(1)
                command?.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                dataIn = service.getCharacteristic(LEG_CHARACTERISTIC)
                val notified = setCharacteristicNotification(dataIn, true)
                if (notified) {
                    val descriptor = dataIn?.getDescriptor(NOTIFICATION_DESCRIPTOR)
                    descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    writeDescriptor(descriptor)
                }
                if (notified && (dataIn != null) && (command != null)) {
                    statusCallback?.invoke(CONNECTED, "Successfully connected")
                } else {
                    statusCallback?.invoke(CONNECTION_FAILED, "Could not connect")
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            Log.w("BluetoothGattCallback", "onChanged: $characteristic")
            if (characteristic == dataIn) {
                val buffer = ByteBuffer.wrap(characteristic.value).order(ByteOrder.LITTLE_ENDIAN)
                val ack_bit = buffer.get()
                val azimuth = buffer.getFloat()
                val inclination = buffer.getFloat()
                val roll = buffer.getFloat()
                val distance = buffer.getFloat()
                sendCommand(ACK + ack_bit)
                leg_callback(azimuth, inclination, roll, distance)
            }
        }
    }

    fun connect() {
        device.connectGatt(context,false,callback)
    }

    fun disconnect() {
        bluetoothGatt?.disconnect()
    }

    fun isConnected(): Boolean {
        val bluetoothMgr: BluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
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

    fun startCal() {
        sendCommand(START_CAL)
    }

    fun stopCal() {
        sendCommand(STOP_CAL)
    }
}

