package com.hapticks.app.service

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.hapticks.app.HapticksApp
import com.hapticks.app.MainActivity
import com.hapticks.app.R
import com.hapticks.app.data.MusicHapticsSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.sqrt

class MusicHapticsService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var micRecord: AudioRecord? = null
    private var deviceRecord: AudioRecord? = null
    private var mediaProjection: MediaProjection? = null
    private var detectionJob: Job? = null
    private val sharedLastHapticMs = AtomicLong(0L)

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val source = intent?.getStringExtra(EXTRA_SOURCE)
            ?.let { runCatching { MusicHapticsSource.valueOf(it) }.getOrNull() }
            ?: lastSource

        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
            ?: Activity.RESULT_CANCELED
        val projData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(EXTRA_PROJECTION_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(EXTRA_PROJECTION_DATA)
        }

        lastSource = source

        // Obtain MediaProjection for device audio capture
        if (source != MusicHapticsSource.SURROUNDINGS &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            resultCode == Activity.RESULT_OK && projData != null
        ) {
            mediaProjection?.stop()
            val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = mgr.getMediaProjection(resultCode, projData)
        }

        startForegroundWithSource(source)
        startBeatDetection(source)
        return START_STICKY
    }

    private fun startForegroundWithSource(source: MusicHapticsSource) {
        val notification = buildNotification(source)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val type = when {
                source == MusicHapticsSource.SURROUNDINGS && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                source == MusicHapticsSource.DEVICE ->
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                source == MusicHapticsSource.BOTH && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                else ->
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            }
            startForeground(NOTIF_ID, notification, type)
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    override fun onDestroy() {
        detectionJob?.cancel()
        micRecord?.apply { runCatching { stop() }; release() }
        deviceRecord?.apply { runCatching { stop() }; release() }
        micRecord = null
        deviceRecord = null
        mediaProjection?.stop()
        mediaProjection = null
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startBeatDetection(source: MusicHapticsSource) {
        detectionJob?.cancel()
        sharedLastHapticMs.set(0L)
        detectionJob = serviceScope.launch {
            when (source) {
                MusicHapticsSource.SURROUNDINGS -> detectFromMic()
                MusicHapticsSource.DEVICE -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && mediaProjection != null) {
                        detectFromDevice()
                    } else {
                        // Fallback to mic if projection unavailable
                        detectFromMic()
                    }
                }
                MusicHapticsSource.BOTH -> {
                    launch { detectFromMic() }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && mediaProjection != null) {
                        detectFromDevice()
                    }
                }
            }
        }
    }

    private suspend fun detectFromMic() {
        val sampleRate = 44100
        val bufferSize = maxOf(AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT) * 2, 4096)
        val recorder = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize)
        if (recorder.state != AudioRecord.STATE_INITIALIZED) { recorder.release(); return }
        micRecord = recorder
        recorder.startRecording()
        runDetectionLoop(recorder, bufferSize)
        runCatching { recorder.stop() }
        recorder.release()
        micRecord = null
    }

    private suspend fun detectFromDevice() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val proj = mediaProjection ?: return
        val sampleRate = 44100
        val bufferSize = maxOf(AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT) * 2, 4096)
        val config = AudioPlaybackCaptureConfiguration.Builder(proj)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
            .build()
        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
            .build()
        val recorder = AudioRecord.Builder()
            .setAudioPlaybackCaptureConfig(config)
            .setAudioFormat(format)
            .setBufferSizeInBytes(bufferSize)
            .build()
        if (recorder.state != AudioRecord.STATE_INITIALIZED) { recorder.release(); return }
        deviceRecord = recorder
        recorder.startRecording()
        runDetectionLoop(recorder, bufferSize)
        runCatching { recorder.stop() }
        recorder.release()
        deviceRecord = null
    }

    private suspend fun runDetectionLoop(recorder: AudioRecord, bufferSize: Int) {
        val buffer = ShortArray(bufferSize / 2)
        var dynamicThreshold = 0.02f
        val smoothingAlpha = 0.05f
        val minHapticIntervalMs = 80L
        val prefs = (applicationContext as HapticksApp).preferences
        val loopScope = kotlinx.coroutines.currentCoroutineContext()

        while (loopScope.isActive) {
            val settings = prefs.settings.first()
            if (!settings.musicHapticsEnabled) { delay(200L); continue }

            val read = recorder.read(buffer, 0, buffer.size)
            if (read <= 0) continue

            var sumSq = 0.0
            for (i in 0 until read) { val s = buffer[i] / 32768f; sumSq += s * s }
            val rms = sqrt(sumSq / read).toFloat()

            dynamicThreshold = dynamicThreshold * (1f - smoothingAlpha) + rms * smoothingAlpha

            val detectionMultiplier = 3.0f - (settings.musicHapticsSensitivity * 1.7f)
            val beatThreshold = dynamicThreshold * detectionMultiplier

            val now = System.currentTimeMillis()
            val last = sharedLastHapticMs.get()
            if (rms > beatThreshold && rms > 0.005f && (now - last) >= minHapticIntervalMs) {
                if (sharedLastHapticMs.compareAndSet(last, now)) {
                    val normalizedIntensity = ((rms / beatThreshold).coerceIn(1f, 3f) - 1f) / 2f
                    val amplitude = (settings.musicHapticsStrength * normalizedIntensity * 255f).toInt().coerceIn(20, 255)
                    val durationMs = (40 + (normalizedIntensity * 60f)).toLong().coerceIn(30L, 100L)
                    vibrator.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Music Haptics", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Music & Sound Haptics beat detection"
                setShowBadge(false)
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(source: MusicHapticsSource = MusicHapticsSource.SURROUNDINGS): Notification {
        val text = when (source) {
            MusicHapticsSource.DEVICE -> "Detecting beats from in-device audio"
            MusicHapticsSource.SURROUNDINGS -> "Detecting beats from surroundings via microphone"
            MusicHapticsSource.BOTH -> "Detecting beats from device audio & microphone"
        }
        val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Music & Sound Haptics")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pi)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "music_haptics_service"
        const val NOTIF_ID = 1002
        const val EXTRA_SOURCE = "extra_source"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_PROJECTION_DATA = "extra_projection_data"

        var lastSource: MusicHapticsSource = MusicHapticsSource.Default

        fun start(
            context: Context,
            source: MusicHapticsSource = MusicHapticsSource.Default,
            resultCode: Int = Activity.RESULT_CANCELED,
            projectionData: Intent? = null,
        ) {
            val intent = Intent(context, MusicHapticsService::class.java).apply {
                putExtra(EXTRA_SOURCE, source.name)
                if (resultCode != Activity.RESULT_CANCELED) putExtra(EXTRA_RESULT_CODE, resultCode)
                if (projectionData != null) putExtra(EXTRA_PROJECTION_DATA, projectionData)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
            else context.startService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MusicHapticsService::class.java))
        }
    }
}
