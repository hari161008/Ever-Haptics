package com.hapticks.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.hapticks.app.HapticksApp
import com.hapticks.app.MainActivity
import com.hapticks.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt

class MusicHapticsService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var audioRecord: AudioRecord? = null
    private var detectionJob: Job? = null

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
        startForeground(NOTIF_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startBeatDetection()
        return START_STICKY
    }

    override fun onDestroy() {
        detectionJob?.cancel()
        audioRecord?.apply { stop(); release() }
        audioRecord = null
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startBeatDetection() {
        detectionJob?.cancel()
        detectionJob = serviceScope.launch {
            val sampleRate = 44100
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val bufferSize = maxOf(
                AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 2,
                4096,
            )

            val recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize,
            )

            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                recorder.release()
                stopSelf()
                return@launch
            }

            audioRecord = recorder
            recorder.startRecording()

            val buffer = ShortArray(bufferSize / 2)

            // Dynamic threshold tracking
            var dynamicThreshold = 0.02f
            val smoothingAlpha = 0.05f
            var lastHapticMs = 0L
            val minHapticIntervalMs = 80L

            val prefs = (applicationContext as HapticksApp).preferences

            while (isActive) {
                val settings = prefs.settings.first()

                if (!settings.musicHapticsEnabled) {
                    kotlinx.coroutines.delay(200L)
                    continue
                }

                val read = recorder.read(buffer, 0, buffer.size)
                if (read <= 0) continue

                // Calculate RMS
                var sumSq = 0.0
                for (i in 0 until read) {
                    val sample = buffer[i] / 32768f
                    sumSq += (sample * sample)
                }
                val rms = sqrt(sumSq / read).toFloat()

                // Update dynamic threshold with exponential moving average
                dynamicThreshold = dynamicThreshold * (1f - smoothingAlpha) + rms * smoothingAlpha

                val sensitivity = settings.musicHapticsSensitivity
                val strength = settings.musicHapticsStrength

                // Beat detected when RMS significantly exceeds dynamic threshold
                // Sensitivity maps 0→1 to multiplier range 3.0→1.3
                val detectionMultiplier = 3.0f - (sensitivity * 1.7f)
                val beatThreshold = dynamicThreshold * detectionMultiplier

                val now = System.currentTimeMillis()
                if (rms > beatThreshold && rms > 0.005f && (now - lastHapticMs) >= minHapticIntervalMs) {
                    lastHapticMs = now
                    // Intensity = strength setting * normalized rms (relative to threshold)
                    val normalizedIntensity = ((rms / beatThreshold).coerceIn(1f, 3f) - 1f) / 2f
                    val amplitude = (strength * normalizedIntensity * 255f).toInt().coerceIn(20, 255)
                    val durationMs = (40 + (normalizedIntensity * 60f)).toLong().coerceIn(30L, 100L)
                    vibrator.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
                }
            }

            recorder.stop()
            recorder.release()
            audioRecord = null
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Haptics",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Music & Sound Haptics beat detection"
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Music & Sound Haptics")
            .setContentText("Detecting beats and vibrating in sync")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "music_haptics_service"
        const val NOTIF_ID = 1002

        fun start(context: Context) {
            val intent = Intent(context, MusicHapticsService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MusicHapticsService::class.java))
        }
    }
}
