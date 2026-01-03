<template>
  <modals-modal v-model="show" :width="320" height="100%">
    <template #outer>
      <div class="absolute top-8 left-4 z-40">
        <p class="text-white text-2xl truncate">{{ $strings.HeaderSelectSpeaker || 'Select Speaker' }}</p>
      </div>
    </template>

    <div class="w-full h-full overflow-hidden absolute top-0 left-0 flex items-center justify-center" @click="show = false">
      <div class="w-full overflow-x-hidden overflow-y-auto bg-primary rounded-lg border border-border" style="max-height: 75%" @click.stop>
        <div v-if="isScanning" class="p-6 flex flex-col items-center justify-center">
          <div class="w-8 h-8 border-2 border-fg border-t-transparent rounded-full animate-spin mb-4"></div>
          <p class="text-fg text-center">{{ $strings.MessageScanningForDevices || 'Scanning for devices...' }}</p>
        </div>

        <div v-else-if="devices.length === 0" class="p-6 text-center">
          <span class="material-symbols text-4xl text-fg-muted mb-2">speaker_group</span>
          <p class="text-fg-muted">{{ $strings.MessageNoDevicesFound || 'No speakers found' }}</p>
          <ui-btn class="mt-4" small @click="startScan">{{ $strings.ButtonRescan || 'Scan Again' }}</ui-btn>
        </div>

        <div v-else>
          <ul class="w-full" role="listbox">
            <li
              v-for="device in devices"
              :key="device.id"
              class="text-fg select-none relative py-4 px-4 hover:bg-bg cursor-pointer border-b border-border last:border-b-0"
              :class="{ 'bg-success/20': connectedDeviceId === device.id }"
              role="option"
              @click="selectDevice(device)"
            >
              <div class="flex items-center">
                <span class="material-symbols text-2xl mr-3">speaker</span>
                <div class="flex-1 min-w-0">
                  <p class="font-medium truncate">{{ device.name }}</p>
                  <p v-if="device.manufacturer" class="text-xs text-fg-muted truncate">{{ device.manufacturer }}</p>
                </div>
                <span v-if="connectedDeviceId === device.id" class="material-symbols text-success">check_circle</span>
              </div>
            </li>
          </ul>

          <div v-if="connectedDeviceId" class="p-4 border-t border-border">
            <ui-btn class="w-full" color="error" small @click="disconnect">
              {{ $strings.ButtonDisconnect || 'Disconnect' }}
            </ui-btn>
          </div>
        </div>
      </div>
    </div>
  </modals-modal>
</template>

<script>
import { AbsAudioPlayer } from '@/plugins/capacitor'

export default {
  props: {
    value: Boolean
  },
  data() {
    return {
      isScanning: false,
      scanTimeout: null
    }
  },
  computed: {
    show: {
      get() {
        return this.value
      },
      set(val) {
        this.$emit('input', val)
      }
    },
    devices() {
      return this.$store.state.dlnaDevices || []
    },
    connectedDeviceId() {
      return this.$store.state.connectedDlnaDevice?.id || null
    }
  },
  methods: {
    async startScan() {
      this.isScanning = true
      await AbsAudioPlayer.startDlnaDiscovery()
      
      this.scanTimeout = setTimeout(() => {
        this.isScanning = false
      }, 5000)
    },
    async stopScan() {
      if (this.scanTimeout) {
        clearTimeout(this.scanTimeout)
        this.scanTimeout = null
      }
      await AbsAudioPlayer.stopDlnaDiscovery()
    },
    async selectDevice(device) {
      if (this.connectedDeviceId === device.id) {
        return
      }

      await this.$hapticsImpact()
      
      try {
        const result = await AbsAudioPlayer.connectDlnaDevice({ deviceId: device.id })
        if (result.success) {
          this.$store.commit('setConnectedDlnaDevice', device)
          this.show = false
        }
      } catch (error) {
        console.error('Failed to connect to device:', error)
        this.$toast.error('Failed to connect to speaker')
      }
    },
    async disconnect() {
      await this.$hapticsImpact()
      await AbsAudioPlayer.disconnectDlnaDevice()
      this.$store.commit('clearDlnaConnection')
    }
  },
  watch: {
    show(val) {
      if (val) {
        this.startScan()
      } else {
        this.stopScan()
      }
    }
  },
  beforeDestroy() {
    this.stopScan()
  }
}
</script>
