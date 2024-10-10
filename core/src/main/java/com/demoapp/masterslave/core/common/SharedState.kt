package com.demoapp.masterslave.core.common

import java.net.Socket

class SharedState {
    val connectedClients = mutableListOf<Socket>()
}