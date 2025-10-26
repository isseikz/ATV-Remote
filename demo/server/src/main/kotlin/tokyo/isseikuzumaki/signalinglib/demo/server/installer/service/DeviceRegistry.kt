package tokyo.isseikuzumaki.signalinglib.demo.server.installer.service

import tokyo.isseikuzumaki.signalinglib.demo.server.installer.model.RegisteredDevice
import java.util.concurrent.ConcurrentHashMap

/**
 * Repository for managing registered devices
 * In production, this should use a persistent database
 */
class DeviceRegistry {
    
    private val devices = ConcurrentHashMap<String, RegisteredDevice>()

    /**
     * Register or update a device
     */
    fun registerDevice(device: RegisteredDevice) {
        devices[device.deviceId] = device
    }

    /**
     * Get device by ID
     */
    fun getDevice(deviceId: String): RegisteredDevice? {
        return devices[deviceId]
    }

    /**
     * Get all registered devices
     */
    fun getAllDevices(): List<RegisteredDevice> {
        return devices.values.toList()
    }

    /**
     * Get devices by group (simple implementation)
     * In production, implement proper grouping logic
     */
    fun getDevicesByGroup(group: String?): List<RegisteredDevice> {
        return if (group == null) {
            getAllDevices()
        } else {
            // For now, return all devices - implement filtering in production
            getAllDevices()
        }
    }

    /**
     * Update device last seen timestamp
     */
    fun updateLastSeen(deviceId: String) {
        devices[deviceId]?.let { device ->
            devices[deviceId] = device.copy(lastSeen = System.currentTimeMillis())
        }
    }

    /**
     * Remove device
     */
    fun removeDevice(deviceId: String) {
        devices.remove(deviceId)
    }

    /**
     * Get device count
     */
    fun getDeviceCount(): Int {
        return devices.size
    }
}
