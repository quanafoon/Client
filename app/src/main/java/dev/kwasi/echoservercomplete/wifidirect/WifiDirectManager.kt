package dev.kwasi.echoservercomplete.wifidirect

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice

import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.ActionListener

import android.os.Build
import android.util.Log

/// This [WifiDirectManager] class is a [BroadcastReceiver] that listens for events fired from the
/// operating system and relays the relevant information using the [WiFiDirectInterface] to the
/// [CommunicationActivity]
class WifiDirectManager(
    private val manager: WifiP2pManager,
    private val channel: WifiP2pManager.Channel,
    private val iFaceImpl: WifiDirectInterface
):BroadcastReceiver() {
    var groupInfo: WifiP2pGroup? = null


    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {

        val extras = intent.extras
        if (extras != null) {
            Log.d("WifiDirectReceiver", "Extras: $extras")
        } else {
            Log.e("WifiDirectReceiver", "No extras found in intent")
        }
        try {
            when (intent.action) {
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                    val isWifiP2pEnabled = state == WifiP2pManager.WIFI_P2P_STATE_ENABLED
                    iFaceImpl.onWiFiDirectStateChanged(isWifiP2pEnabled)
                    Log.e(
                        "WFDManager",
                        "The WiFi direct adapter state has changed to $isWifiP2pEnabled"
                    )
                }


                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    val wifiP2pInfo = when {
                        Build.VERSION.SDK_INT >= 33 -> intent.getParcelableExtra(
                            WifiP2pManager.EXTRA_WIFI_P2P_INFO,
                            WifiP2pInfo::class.java
                        )!!

                        else -> @Suppress("DEPRECATION") intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO)!!
                    }

                    if (!wifiP2pInfo.groupFormed) {
                        Log.e("WFDManager", "No group formed or wifiP2pInfo is null wifiInfo = $wifiP2pInfo, groupInfo : ${wifiP2pInfo.groupFormed}")
                        // Clean up or return early
                        iFaceImpl.onGroupStatusChanged(null)
                        return  // Return from the enclosing function
                    }


                    val tmpGroupInfo = when {
                        !(wifiP2pInfo.groupFormed) -> null
                        Build.VERSION.SDK_INT >= 33 -> intent.getParcelableExtra(
                            WifiP2pManager.EXTRA_WIFI_P2P_GROUP,
                            WifiP2pGroup::class.java
                        )!!

                        else -> @Suppress("DEPRECATION") intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_GROUP)!!
                    }

                    if (tmpGroupInfo == null) {
                        Log.e("WFDManager", "tmpGroupInfo is null, returning early")
                        return
                    }
                    if (groupInfo != tmpGroupInfo) {
                        groupInfo = tmpGroupInfo
                        Log.e("WFDManager", "The group status has changed")
                        iFaceImpl.onGroupStatusChanged(groupInfo)
                    }


                }

                WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                    val thisDevice = when {
                        Build.VERSION.SDK_INT >= 33 -> intent.getParcelableExtra(
                            WifiP2pManager.EXTRA_WIFI_P2P_DEVICE,
                            WifiP2pDevice::class.java
                        )!!

                        else -> @Suppress("DEPRECATION") intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)!!
                    }
                    Log.e("WFDManager", "The device status has changed")

                    iFaceImpl.onDeviceStatusChanged(thisDevice)
                }
            }
        }catch (e: Exception) { Log.e("WfdManager","Error processing connection change ${e.message}")}
    }
    @SuppressLint("MissingPermission")
    fun createGroup() {
        disconnect {
            manager.createGroup(channel, object : ActionListener {
                override fun onSuccess() {
                    Log.d("WFDManager", "Successfully created a group with myself as the GO")
                    // Request the group info immediately after successful group creation
                    manager.requestGroupInfo(channel) { group ->
                        if (group != null) {
                            groupInfo = group
                            Log.d("WFDManager", "Group info retrieved successfully: $groupInfo")

                            iFaceImpl.onGroupStatusChanged(groupInfo)

                        } else {
                            Log.e("WFDManager", "Group info is null after creation")
                            iFaceImpl.onGroupStatusChanged(null)
                        }
                    }
                }

                override fun onFailure(reason: Int) {
                    val errorMessage = when (reason) {
                        WifiP2pManager.BUSY -> "Wi-Fi P2P is busy"
                        WifiP2pManager.ERROR -> "An internal error occurred"
                        WifiP2pManager.P2P_UNSUPPORTED -> "Wi-Fi P2P is not supported"
                        else -> "Unknown error"
                    }
                    Log.e("WFDManager", "An error occurred while trying to create a group: $errorMessage")
                    iFaceImpl.onGroupStatusChanged(null)
                }

            })
        }
    }



    @SuppressLint("MissingPermission")
    fun connectToPeer(peer: WifiP2pDevice) {
        val config = WifiP2pConfig()
        config.deviceAddress = peer.deviceAddress
        manager.connect(channel, config, object : ActionListener {
            override fun onSuccess() {
                Log.e("WFDManager","Successfully attempted to connect to a peer '${peer.deviceName}'")
            }

            override fun onFailure(reason: Int) {
                Log.e("WFDManager","An error occurred while trying to connect to a peer '${peer.deviceName}'")
            }

        })
    }

    @SuppressLint("MissingPermission")
    fun discoverPeers(){
        manager.discoverPeers(channel, object : ActionListener {
            override fun onSuccess() {
                Log.e("WFDManager","Successfully attempted to discover peers")
            }

            override fun onFailure(reason: Int) {
                Log.e("WFDManager","An error occurred while trying to discover peers")
            }
        })
    }

    fun disconnect(callback: () -> Unit){
        manager.removeGroup(channel, object : ActionListener {
            override fun onSuccess() {
                Log.e("WFDManager","Successfully disconnected from the group")
                callback()
            }
            override fun onFailure(reason: Int) {
                Log.e("WFDManager","An error occurred while trying to disconnect from the group")
                callback()
            }

        })
    }
}