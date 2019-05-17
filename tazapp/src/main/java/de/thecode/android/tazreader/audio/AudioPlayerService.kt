package de.thecode.android.tazreader.audio

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.IBinder
import android.os.Parcelable
import android.os.PowerManager
import android.widget.Toast
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

    enum class State {
        LOADING, PLAYING, PAUSED
    }

    companion object {
        val TAG = "AudioPLayerService"
//        const val ACTION_SERVICE_COMMUNICATION = "audioServiceDestroyedCommunication"
        const val EXTRA_AUDIO_ITEM = "audioExtra"
        const val ACTION_STATE_CHANGED = "serviceAudioStateChanged"
//        const val EXTRA_COMMUNICATION_MESSAGE = "messageExtra"
//        const val EXTRA_STATE = "state"

        const val MESSAGE_SERVICE_PREPARE_PLAYING = "servicePreparePlaying"
//        const val MESSAGE_SERVICE_DESTROYED = "serviceDestroyed"
//        const val MESSAGE_SERVICE_PLAYSTATE_CHANGED = "playstateChanged"

        var instance: AudioPlayerService? = null

        fun isServiceCreated(): Boolean {
            instance?.let {
                return it.ping()
            }
            return false
        }
    }

    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null

    var audioItem: AudioItem? = null
    var state: State = State.LOADING
        set(value) {
            d {"setState $value"}
            field = value
            val serviceIntent = Intent()
            serviceIntent.action = ACTION_STATE_CHANGED
            LocalBroadcastManager.getInstance(this)
                    .sendBroadcast(serviceIntent)
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
    }

    override fun onDestroy() {
        instance = null
        removeAudioFocus()
        state = State.LOADING
        super.onDestroy()
    }

    private fun ping(): Boolean {
        return true
    }

//    private fun sendLocalBroadcastMessage(message: String) {
//        val serviceIntent = Intent()
//        serviceIntent.action = ACTION_SERVICE_COMMUNICATION
//        serviceIntent.putExtra(EXTRA_COMMUNICATION_MESSAGE, message)
//        LocalBroadcastManager.getInstance(this)
//                .sendBroadcast(serviceIntent)
//    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        d { "onStartCommand $intent" }
        intent?.let { onStartIntent ->
            val audioItemExtra = onStartIntent.getParcelableExtra<AudioItem>(EXTRA_AUDIO_ITEM)

            audioItemExtra?.let {
                audioItem = it
                if (isPlaying()) {
                    mediaPlayer?.stop()
                    mediaPlayer?.release()
                    mediaPlayer = null
                }
                initForeground()
                //Request audio focus
                if (requestAudioFocus()) {

                    initMediaPlayer()
                } else {
                    Toast.makeText(this, R.string.audio_service_error_gain_focus, Toast.LENGTH_LONG)
                            .show()
                    //Could not gain focus
                    stopSelf()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun initForeground() {
        val builder = NotificationCompat.Builder(this, NotificationUtils.AUDIO_CHANNEL_ID)
        val title = StringBuilder()
        title.append(audioItem!!.title)
        builder.setContentTitle(title.toString())
                .setContentText(audioItem!!.source)
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
        builder.priority = NotificationCompat.PRIORITY_DEFAULT


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
        return try {
            mediaPlayer?.isPlaying ?: throw IllegalStateException() //mediaPlayer.isPlaying also throws IllegalstateException
        } catch (ex: IllegalStateException) {
            false
        }
    }

    private fun initMediaPlayer() {
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
        state = State.LOADING
//        sendLocalBroadcastMessage(MESSAGE_SERVICE_PREPARE_PLAYING)
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
        d { "pauseOrResumePlaying" }
        mediaPlayer?.let { mp ->
            if (mp.isPlaying) {
                mp.pause()
                state = State.PAUSED
//                sendLocalBroadcastMessage(MESSAGE_SERVICE_PLAYSTATE_CHANGED)
                audioItem?.let {
                    it.resumePosition = mp.currentPosition
                }

            } else {
                audioItem?.let {
                    mp.seekTo(it.resumePosition)
                }
                mp.start()
                state= State.PLAYING
//                sendLocalBroadcastMessage(MESSAGE_SERVICE_PLAYSTATE_CHANGED)
            }
            initForeground()
        }
    }

    private fun requestAudioFocus(): Boolean {
        val audioManagerService = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager = audioManagerService
        val result = audioManagerService.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        d { "requestAudioFocus $result" }
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun removeAudioFocus(): Boolean {
        d { "removeAudioFocus" }
        audioManager?.let {
            return AudioManager.AUDIOFOCUS_REQUEST_GRANTED == it.abandonAudioFocus(this)
        } ?: return false
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
        d { "onAudioFocusChange $focusChange" }
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (mediaPlayer == null) {
                    initMediaPlayer()
                } else if (!isPlaying()) {
                    pauseOrResumePlaying()
                }
                mediaPlayer?.setVolume(1F, 1F)
                // resume or start playback
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                // Lost focus for an unbounded amount of time: stop playback and release media player
                stopPlaying()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if (isPlaying()) pauseOrResumePlaying()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Lost focus for a short time, but it's ok to keep playing
                if (isPlaying()) mediaPlayer?.setVolume(0.1F, 0.1F)
            }

        }
    }


}

@Parcelize
data class AudioItem(val uri: String, val title: String, val source: String, val sourceId: String, var resumePosition: Int = 0) : Parcelable