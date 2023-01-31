package com.example.fintech75.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.nio.channels.IllegalBlockingModeException

object InternetCheck {

    suspend fun isNetworkAvailable(): Boolean = coroutineScope {
        return@coroutineScope try {
            val sock = Socket()
            val socketAddress = InetSocketAddress("8.8.8.8", 53)
            withContext(Dispatchers.IO) {
                sock.connect(socketAddress, 2000)
                sock.close()
            }

            true
        } catch (e: IOException) {
            false
        } catch (e: SocketTimeoutException) {
            false
        } catch (e: IllegalBlockingModeException) {
            false
        } catch (e: IllegalArgumentException ) {
            false
        }

    }
}