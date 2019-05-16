package de.thecode.android.tazreader.audio

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.IBinder
import android.os.Parcelable
import android.os.PowerManager
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.github.ajalt.timberkt.Timber.d
import com.github.ajalt.timberkt.Timber.e
import com.github.ajalt.timberkt.Timber.i
import de.thecode.android.tazreader.R
import de.thecode.android.tazreader.notifications.NotificationUtils
import de.thecode.android.tazreader.start.StartActivity
import kotlinx.android.parcel.Parcelize
import java.io.IOException


class AudioPlayerService : Service(), MediaPlayer.OnCompletionListener, MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnInfoListener, AudioManager.OnAudioFocusChangeListener {

    companion object {
        val TAG = "AudioPLayerService"
        const val ACTION_SERVICE_COMMUNICATION = "audioServiceDestroyedCommunication"
        const val ACTION_PLAY_URI = "playUriNow"
        const val EXTRA_AUDIO_ITEM = "audioExtra"
        const val EXTRA_COMMUNICATION_MESSAGE = "messageExtra"

        const val MESSAGE_SERVICE_CREATED = "serviceCreated"
        const val MESSAGE_SERVICE_DESTROYED = "serviceDestroyed"
        const val MESSAGE_SERVICE_PLAYSTATE_CHANGED = "playstateChanged"

        var instance: AudioPlayerService? = null

        fun isServiceCreated(): Boolean {
            instance?.let {
                return it.ping()
            }
            return false
        }
    }

    var mediaPlayer: MediaPlayer? = null
    var audioItem: AudioItem? = null

    override fun onBind(intent: Intent?): IBinder? {
        TODO("not implemented")
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        d {
            "XXX SERVICE CREATED"
        }
    }

    override fun onDestroy() {
        instance = null
        sendLocalBroadcastMessage(MESSAGE_SERVICE_DESTROYED)
        super.onDestroy()
    }

    private fun ping(): Boolean {
        return true
    }

    private fun sendLocalBroadcastMessage(message: String) {
        val serviceIntent = Intent()
        serviceIntent.action = ACTION_SERVICE_COMMUNICATION
        serviceIntent.putExtra(EXTRA_COMMUNICATION_MESSAGE, message)
        LocalBroadcastManager.getInstance(this)
                .sendBroadcast(serviceIntent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                ACTION_PLAY_URI -> {
                    sendLocalBroadcastMessage(MESSAGE_SERVICE_CREATED)
                    audioItem = it.getParcelableExtra(EXTRA_AUDIO_ITEM)
                    audioItem?.let {
                        initForeground()
                        initMediaPlayer()
                    }
                }
                else -> {

                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun initForeground() {
        // Create notification default intent.
        //val intent = Intent()
        //val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

        // Create notification builder.
        val builder = NotificationCompat.Builder(this, NotificationUtils.AUDIO_CHANNEL_ID)
        val title = StringBuilder()
//        if (!isPlaying()) {
//            title.append("[Pausiert] ")
//        }
        title.append(audioItem!!.title)
        builder.setContentTitle(title.toString())
                .setContentText(audioItem!!.source)
        // Make notification show big text.
        builder.setWhen(System.currentTimeMillis())
        builder.setSmallIcon(R.drawable.ic_audio_notification)
        val drawableRes = if (isPlaying()) R.drawable.ic_record_voice_over_black_32dp else R.drawable.ic_pause_black_24dp
        val drawable = AppCompatResources.getDrawable(this, drawableRes)
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

        val intent = Intent(this, StartActivity::class.java)
        intent.putExtra(NotificationUtils.NOTIFICATION_EXTRA_BOOKID, audioItem!!.sourceId)
        intent.putExtra(NotificationUtils.NOTIFICATION_EXTRA_TYPE_ID, NotificationUtils.AUDIOSERVICE_NOTIFICATION_ID)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        val uniqueInt = (System.currentTimeMillis() and 0xfffffff).toInt()
        val contentIntent = PendingIntent.getActivity(this, uniqueInt, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        builder.setContentIntent(contentIntent)

        val notification = builder.build()

        // Start foreground service.
        startForeground(1, notification)
    }

    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying ?: false
    }

    private fun initMediaPlayer() {
        mediaPlayer?.release()
        val mp = MediaPlayer()
        mp.setOnBufferingUpdateListener(this)
        mp.setOnCompletionListener(this)
        mp.setOnErrorListener(this)
        mp.setOnPreparedListener(this)
        mp.setOnInfoListener(this)
        mp.reset()
        mp.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK)
        mp.setAudioStreamType(AudioManager.STREAM_MUSIC)
        try {
            mp.setDataSource(audioItem!!.uri)
        } catch (ex: IOException) {
            e(ex)
            stopSelf()
        }
        mp.prepareAsync()
        mediaPlayer = mp
    }

    fun stopPlaying() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        stopSelf()
    }


    fun pauseOrResumePlaying() {
        mediaPlayer?.let { mp ->
            if (mp.isPlaying) {
                mp.pause()
                sendLocalBroadcastMessage(MESSAGE_SERVICE_PLAYSTATE_CHANGED)
                audioItem?.let {
                    it.resumePosition = mp.currentPosition
                }

            } else {
                audioItem?.let {
                    mp.seekTo(it.resumePosition)
                }
                mp.start()
                sendLocalBroadcastMessage(MESSAGE_SERVICE_PLAYSTATE_CHANGED)
            }
            initForeground()
        }
    }

    override fun onBufferingUpdate(mp: MediaPlayer?, percent: Int) {
        d {
            "onBufferingUpdate $percent"
        }
    }

    override fun onPrepared(mp: MediaPlayer?) {
        d {
            "onPrepared"
        }
        pauseOrResumePlaying()
    }

    override fun onCompletion(mp: MediaPlayer?) {
        d {
            "onCompletion"
        }
        stopPlaying()
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        e {
            "onError what: $what extra: $extra"
        }
        return false
    }

    override fun onInfo(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        i {
            "onInfo what: $what extra: $extra"
        }
        return false
    }

    override fun onAudioFocusChange(focusChange: Int) {
        TODO("not implemented")
    }


}

@Parcelize
data class AudioItem(val uri: String, val title: String, val source: String, val sourceId: String, var resumePosition: Int = 0) : Parcelable