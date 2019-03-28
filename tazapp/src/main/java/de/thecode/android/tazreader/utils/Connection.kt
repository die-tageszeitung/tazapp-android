package de.thecode.android.tazreader.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.telephony.TelephonyManager
import com.github.ajalt.timberkt.i
import de.thecode.android.tazreader.R
import de.thecode.android.tazreader.app

class Connection {

    enum class Type(val readable: String) {
        NOT_AVAILABLE(app.getString(R.string.connection_not_available)), ROAMING(app.getString(R.string.connection_roaming)), MOBILE(app.getString(R.string.connection_mobile)), FAST(app.getString(R.string.connection_fast))
    }

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

            val type: Type = if (info == null) Type.NOT_AVAILABLE else
                when (info.type) {
                    ConnectivityManager.TYPE_WIFI, ConnectivityManager.TYPE_ETHERNET, ConnectivityManager.TYPE_BLUETOOTH -> {
                        Type.FAST
                    }
                    ConnectivityManager.TYPE_MOBILE -> {
                        val tmanager = app.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                        if (tmanager.isNetworkRoaming)
                            Type.ROAMING
                        else
                            Type.MOBILE
                    }
                    else -> Type.NOT_AVAILABLE
                }
            val result = ConnectionInfo(type, info?.isConnectedOrConnecting ?: false)
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

data class ConnectionInfo(val type: Connection.Type, val isConnectedOrConnecting: Boolean)

