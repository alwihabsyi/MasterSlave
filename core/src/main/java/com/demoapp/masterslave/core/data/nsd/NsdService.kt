package com.demoapp.masterslave.core.data.nsd

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.demoapp.masterslave.core.data.repository.ClientRepositoryImpl.Companion.SERVICE_TYPE
import com.demoapp.masterslave.core.data.repository.ClientRepositoryImpl.Companion.SOCKET_PORT
import com.demoapp.masterslave.core.data.repository.SlaveRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.IOException

@Suppress("DEPRECATION")
class NsdService(
    private val nsdManager: NsdManager
) {
    private var nsdServiceJob: Job? = null

    private var registrationListener: NsdManager.RegistrationListener? = null
    private var isServiceRegistered = false

    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var isListenerRegistered = false

    private val unregisterNsdListener: (NsdManager.RegistrationListener) -> Unit = {
        nsdManager.unregisterService(it)
        isServiceRegistered = false
    }

    fun registerNsdService(onRegistered: (String) -> Unit) {
        if (nsdServiceJob == null) nsdServiceJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val serviceInfo = NsdServiceInfo().apply {
                    serviceName = "MasterService"
                    serviceType = SERVICE_TYPE
                    port = SOCKET_PORT
                }

                registrationListener = nsdListener(onRegistered)
                nsdManager.registerService(
                    serviceInfo,
                    NsdManager.PROTOCOL_DNS_SD,
                    registrationListener
                )

                isServiceRegistered = true
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun nsdListener(onRegistered: ((String) -> Unit)? = null) =
        object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                onRegistered?.invoke(serviceInfo.serviceName)
                isServiceRegistered = true
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                isServiceRegistered = false
                Log.e("NSD", "Service registration failed: Error code $errorCode")
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                isServiceRegistered = false
                Log.i("NSD", "Service successfully unregistered: ${serviceInfo.serviceName}")
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                isServiceRegistered = false
                Log.e("NSD", "Service unregistration failed: Error code $errorCode")
            }
        }

    fun discoverMasterService(
        onDiscoveryStarted: () -> Unit,
        onServiceFound: (NsdServiceInfo) -> Unit,
        onError: (String, Boolean) -> Unit
    ) {
        discoveryListener = discoveryListener(
            onDiscoveryStarted,
            onServiceFound,
            onError
        )

        nsdManager.discoverServices(
            SlaveRepositoryImpl.SERVICE_TYPE,
            NsdManager.PROTOCOL_DNS_SD,
            discoveryListener
        )
    }

    private fun discoveryListener(
        onDiscoveryStarted: () -> Unit,
        onServiceFound: (NsdServiceInfo) -> Unit,
        onError: (String, Boolean) -> Unit
    ) =
        object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                onDiscoveryStarted.invoke()
                isListenerRegistered = true
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (serviceInfo.serviceType == SlaveRepositoryImpl.SERVICE_TYPE) {
                    nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                            onServiceFound.invoke(serviceInfo)
                        }

                        override fun onResolveFailed(
                            serviceInfo: NsdServiceInfo,
                            errorCode: Int
                        ) {
                            onError.invoke("Failed to resolve connection", false)
                        }
                    })
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                onError.invoke("Master service lost. Reconnecting...", true)
            }

            override fun onDiscoveryStopped(serviceType: String) {
                isListenerRegistered = false
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                isListenerRegistered = false
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                isListenerRegistered = true
            }
        }

    fun closeService() = try {
        registrationListener?.takeIf { isServiceRegistered }?.apply(unregisterNsdListener)
    } catch (e: IOException) {
        e.printStackTrace()
    }
}