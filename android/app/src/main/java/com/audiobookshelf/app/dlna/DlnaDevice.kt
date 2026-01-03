package com.audiobookshelf.app.dlna

import com.fasterxml.jackson.annotation.JsonIgnore
import net.mm2d.upnp.Device
import net.mm2d.upnp.Service

data class DlnaDevice(
    val id: String,
    val name: String,
    val manufacturer: String?,
    val modelName: String?,
    val modelDescription: String?,
    val address: String,
    @JsonIgnore
    val device: Device? = null
) {
    private fun findServiceInDeviceTree(device: Device?, serviceId: String, serviceType: String): Service? {
        if (device == null) return null
        
        device.findServiceById(serviceId)?.let { return it }
        device.findServiceByType(serviceType)?.let { return it }
        
        for (embeddedDevice in device.deviceList) {
            findServiceInDeviceTree(embeddedDevice, serviceId, serviceType)?.let { return it }
        }
        
        return null
    }

    @get:JsonIgnore
    val avTransportService: Service?
        get() = findServiceInDeviceTree(
            device,
            "urn:upnp-org:serviceId:AVTransport",
            "urn:schemas-upnp-org:service:AVTransport:1"
        )

    @get:JsonIgnore
    val renderingControlService: Service?
        get() = findServiceInDeviceTree(
            device,
            "urn:upnp-org:serviceId:RenderingControl",
            "urn:schemas-upnp-org:service:RenderingControl:1"
        )

    @get:JsonIgnore
    val connectionManagerService: Service?
        get() = findServiceInDeviceTree(
            device,
            "urn:upnp-org:serviceId:ConnectionManager",
            "urn:schemas-upnp-org:service:ConnectionManager:1"
        )

    @get:JsonIgnore
    val hasAvTransport: Boolean
        get() = avTransportService != null

    @get:JsonIgnore
    val isValid: Boolean
        get() = hasAvTransport

    companion object {
        fun fromDevice(device: Device): DlnaDevice {
            return DlnaDevice(
                id = device.udn,
                name = device.friendlyName,
                manufacturer = device.manufacture,
                modelName = device.modelName,
                modelDescription = device.modelDescription,
                address = device.ipAddress,
                device = device
            )
        }
    }

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "name" to name,
            "manufacturer" to manufacturer,
            "modelName" to modelName,
            "modelDescription" to modelDescription,
            "address" to address
        )
    }
}
