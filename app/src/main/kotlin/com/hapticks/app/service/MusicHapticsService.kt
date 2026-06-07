package com.hapticks.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.Visualizer
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
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.sqrt

class MusicHapticsService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var visualizer: Visualizer? = null
    private var micRecord: AudioRecord? = null
    private var micJob: Job? = null
    private val sharedLastHapticMs = AtomicLong(0L)

    @Volatile private var currentSensitivity = 0.5f
    @Volatile private var currentStrength = 0.7f
    @Volatile private var currentEnabled = true

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
        lastSource = source

        startForegroundCompat(source)
        startSettingsWatcher()
        startBeatDetection(source)
        return START_STICKY
    }

    override fun onDestroy() {
        stopAllDetection()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startSettingsWatcher() {
        serviceScope.launch {
            (applicationContext as HapticksApp).preferences.settings.collect { s ->
                currentSensitivity = s.musicHapticsSensitivity
                currentStrength = s.musicHapticsStrength
                currentEnabled = s.musicHapticsEnabled
            }
        }
    }

    private fun startBeatDetection(source: MusicHapticsSource) {
        stopAllDetection()
        sharedLastHapticMs.set(0L)
        when (source) {
            MusicHapticsSource.DEVICE -> startVisualizer(fallbackToMic = false)
            MusicHapticsSource.SURROUNDINGS -> micJob = serviceScope.launch { detectFromMic() }
            MusicHapticsSource.BOTH -> {
                startVisualizer(fallbackToMic = false)
                micJob = serviceScope.launch { detectFromMic() }
            }
        }
    }

    private fun stopAllDetection() {
        micJob?.cancel()
        micJob = null
        micRecord?.apply { runCatching { stop() }; release() }
        micRecord = null
        visualizer?.apply { runCatching { enabled = false }; runCatching { release() } }
        visualizer = null
    }

    // ── Device audio via Visualizer (no MediaProjection needed) ──────────────

    private fun startVisualizer(fallbackToMic: Boolean = false) {
        try {
            val vis = Visualizer(0)
            vis.captureSize = Visualizer.getCaptureSizeRange()[1].coerceAtLeast(1024)
            val dynThreshold = floatArrayOf(0.02f)
            val smoothAlpha = 0.06f
            val minHapticMs = 80L
            vis.setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(v: Visualizer, waveform: ByteArray, samplingRate: Int) {
                    if (!currentEnabled) return
                    var sumSq = 0.0
                    for (b in waveform) { val s = ((b.toInt() and 0xFF) - 128) / 128f; sumSq += s * s }
                    val rms = sqrt(sumSq / waveform.size).toFloat()
                    dynThreshold[0] = dynThreshold[0] * (1f - smoothAlpha) + rms * smoothAlpha
                    fireBeatIfDetected(rms, dynThreshold[0], minHapticMs)
                }
                override fun onFftDataCapture(v: Visualizer, fft: ByteArray, samplingRate: Int) {}
            }, Visualizer.getMaxCaptureRate() / 2, true, false)
            vis.enabled = true
            visualizer = vis
        } catch (_: Exception) {
            if (fallbackToMic) micJob = serviceScope.launch { detectFromMic() }
        }
    }

    // ── Surroundings audio via microphone ─────────────────────────────────────

    private suspend fun detectFromMic() {
        val sampleRate = 44100
        val bufferSize = maxOf(
            AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT) * 2,
            4096,
        )
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize,
        )
        if (recorder.state != AudioRecord.STATE_INITIALIZED) { recorder.release(); return }
        micRecord = recorder
        recorder.startRecording()

        val buffer = ShortArray(bufferSize / 2)
        var dynThreshold = 0.02f
        val smoothAlpha = 0.05f
        val minHapticMs = 80L

        val loopCtx = currentCoroutineContext()
        while (loopCtx.isActive) {
            if (!currentEnabled) { delay(200L); continue }
            val read = recorder.read(buffer, 0, buffer.size)
            if (read <= 0) continue
            var sumSq = 0.0
            for (i in 0 until read) { val s = buffer[i] / 32768f; sumSq += s * s }
            val rms = sqrt(sumSq / read).toFloat()
            dynThreshold = dynThreshold * (1f - smoothAlpha) + rms * smoothAlpha
            fireBeatIfDetected(rms, dynThreshold, minHapticMs)
        }
        runCatching { recorder.stop() }
        recorder.release()
        micRecord = null
    }

    // ── Shared beat-fire logic ────────────────────────────────────────────────

    private fun fireBeatIfDetected(rms: Float, dynThreshold: Float, minHapticMs: Long) {
        val detectionMultiplier = 3.0f - (currentSensitivity * 1.7f)
        val beatThreshold = dynThreshold * detectionMultiplier
        val now = System.currentTimeMillis()
        val last = sharedLastHapticMs.get()
        if (rms > beatThreshold && rms > 0.005f && (now - last) >= minHapticMs) {
            if (sharedLastHapticMs.compareAndSet(last, now)) {
                val norm = ((rms / beatThreshold).coerceIn(1f, 3f) - 1f) / 2f
                val amplitude = (currentStrength * norm * 255f).toInt().coerceIn(20, 255)
                val durationMs = (40 + norm * 60f).toLong().coerceIn(30L, 100L)
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
            }
        }
    }

    // ── Notification / foreground ─────────────────────────────────────────────

    private fun startForegroundCompat(source: MusicHapticsSource) {
        val notification = buildNotification(source)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIF_ID, notification)
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

    private fun buildNotification(source: MusicHapticsSource): Notification {
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
        var lastSource: MusicHapticsSource = MusicHapticsSource.Default

        fun start(context: Context, source: MusicHapticsSource = MusicHapticsSource.Default) {
            lastSource = source
            val intent = Intent(context, MusicHapticsService::class.java).apply {
                putExtra(EXTRA_SOURCE, source.name)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
            else context.startService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MusicHapticsService::class.java))
        }
    }
}
