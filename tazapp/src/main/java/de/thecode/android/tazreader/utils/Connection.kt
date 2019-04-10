package de.thecode.android.tazreader.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import com.github.ajalt.timberkt.i
import de.thecode.android.tazreader.app

class Connection {

    companion object {

        private val systemReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val info = getConnectionInfo()
                listeners.forEach {
                    it.onNetworkConnectionChanged(info)
                }
            }
        }

        val listeners = arrayListOf<ConnectionChangeListener>()

        fun getConnectionInfo(): ConnectionInfo {
            val manager = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val info = manager.activeNetworkInfo
            val result = if (info != null) ConnectionInfo(info.isConnectedOrConnecting, manager.isActiveNetworkMetered, info.isRoaming)
                            else ConnectionInfo(false, false, false)
            i { "ConnectionInfo: $result" }
            return result
        }

        fun addListener(listener: ConnectionChangeListener) {
            listeners.add(listener)
            if (listeners.size > 0) {
                app.registerReceiver(systemReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
            }
        }

        fun removeListener(listener: ConnectionChangeListener) {
            listeners.remove(listener)
            if (listeners.size == 0) {
                app.unregisterReceiver(systemReceiver)
            }
        }
    }


    interface ConnectionChangeListener {
        fun onNetworkConnectionChanged(info: ConnectionInfo)
    }
}

data class ConnectionInfo(val connected: Boolean = false, val metered: Boolean = false, val roaming: Boolean = false)

