package de.thecode.android.tazreader.audio

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Parcelable
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.github.ajalt.timberkt.Timber.d
import de.thecode.android.tazreader.R
import de.thecode.android.tazreader.notifications.NotificationUtils
import kotlinx.android.parcel.Parcelize


class AudioPlayerService : Service() {

    companion object {
        val TAG = "AudioPLayerService"
        const val ACTION_SERVICE_CREATED = "audioServiceCreated"
        const val ACTION_PLAY_URI = "playUriNow"
        const val EXTRA_AUDIO_ITEM = "playUriNow"

        var instance: AudioPlayerService? = null

        fun isServiceCreated(): Boolean {
            instance?.let {
                return it.ping()
            }
            return false
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        TODO("not implemented")
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        d {
            "XXX SERVICE CREATED"
        }
        val serviceCreatedIntent = Intent()
        serviceCreatedIntent.action = ACTION_SERVICE_CREATED
        LocalBroadcastManager.getInstance(this)
                .sendBroadcast(serviceCreatedIntent)
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    private fun ping(): Boolean {
        return true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        d {
            "XXX SERVICE ONSTART COMMAND $intent ${intent!!.action}"
        }
        intent?.let {
            d {
                "XXX ICH BIN HIER ${intent.action}"
            }
        }
//        intent?.let {
//            {
//                d {
//                    "XXX HAVE INTENT $intent"
//                }
//                when (intent.action) {
//                    ACTION_PLAY_URI -> {
//                        val audioItem = intent.getParcelableExtra<AudioItem>(EXTRA_AUDIO_ITEM)
//                        d {
//                            "XXX ACTION PLAY $audioItem"
//                        }
//
//                        audioItem?.let {
//                            d {
//                                "XXX $it"
//                            }
//                        }
//                    }
//                    else -> {
//                    }
//                }
//            }
//        }

        initForeground()

        return super.onStartCommand(intent, flags, startId)
    }

    private fun initForeground() {
        // Create notification default intent.
        //val intent = Intent()
        //val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

        // Create notification builder.
        val builder = NotificationCompat.Builder(this, NotificationUtils.AUDIO_CHANNEL_ID)
        builder.setContentTitle("TITLE")
                .setContentText("MESSAGE")
        // Make notification show big text.
        builder.setWhen(System.currentTimeMillis())
        builder.setSmallIcon(R.drawable.ic_audio_notification)
        val drawable = AppCompatResources.getDrawable(this, R.drawable.ic_record_voice_over_black_32dp)
        drawable?.let {
            var wrappedDrawable = DrawableCompat.wrap(it)
            wrappedDrawable = wrappedDrawable.mutate()
            DrawableCompat.setTint(wrappedDrawable, ContextCompat.getColor(this, R.color.color_accent))
            builder.setLargeIcon(wrappedDrawable.toBitmap())
        }
        // Make the notification max priority.
        builder.priority = NotificationCompat.PRIORITY_DEFAULT
        // Make head-up notification.
        // builder.setFullScreenIntent(pendingIntent, true)

//        // Add Play button intent in notification.
//        val playIntent = Intent(this, MyForeGroundService::class.java)
//        playIntent.action = ACTION_PLAY
//        val pendingPlayIntent = PendingIntent.getService(this, 0, playIntent, 0)
//        val playAction = NotificationCompat.Action(android.R.drawable.ic_media_play, "Play", pendingPlayIntent)
//        builder.addAction(playAction)
//
//        // Add Pause button intent in notification.
//        val pauseIntent = Intent(this, MyForeGroundService::class.java)
//        pauseIntent.action = ACTION_PAUSE
//        val pendingPrevIntent = PendingIntent.getService(this, 0, pauseIntent, 0)
//        val prevAction = NotificationCompat.Action(android.R.drawable.ic_media_pause, "Pause", pendingPrevIntent)
//        builder.addAction(prevAction)

        // Build the notification.
        val notification = builder.build()

        // Start foreground service.
        startForeground(1, notification)
    }

}

@Parcelize
data class AudioItem(val uri: String, val title: String, val source: String) : Parcelable