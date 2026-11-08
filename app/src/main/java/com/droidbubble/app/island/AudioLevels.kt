package com.droidbubble.app.island

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.Visualizer
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.droidbubble.app.service.IslandAccessibilityService
import kotlin.math.hypot
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

private fun hasMicPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

/**
 * Battery Saver and screen state for the audio visualizers. With Battery Saver on they sample and
 * redraw at half rate; with the screen off (or always-on display) capture stops entirely and comes
 * back when the screen turns on.
 */
object AudioPowerPolicy {
    /** Battery Saver is on: half-rate sampling and redraws. */
    var lowPower by mutableStateOf(false)
        private set

    var screenOn by mutableStateOf(true)
        private set

    private var registered = false
    private val listeners = mutableListOf<() -> Unit>()

    fun ensure(context: Context) {
        if (registered) return
        registered = true
        val app = context.applicationContext
        val pm = app.getSystemService(Context.POWER_SERVICE) as? PowerManager
        lowPower = pm?.isPowerSaveMode == true
        screenOn = pm?.isInteractive != false
        val filter = IntentFilter().apply {
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        runCatching {
            ContextCompat.registerReceiver(app, object : BroadcastReceiver() {
                override fun onReceive(c: Context?, intent: Intent?) {
                    val wasLow = lowPower
                    val wasOn = screenOn
                    lowPower = pm?.isPowerSaveMode == true
                    screenOn = when (intent?.action) {
                        Intent.ACTION_SCREEN_OFF -> false
                        Intent.ACTION_SCREEN_ON -> true
                        else -> screenOn
                    }
                    if (wasLow != lowPower || wasOn != screenOn) listeners.forEach { it() }
                }
            }, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        }
    }

    fun addListener(listener: () -> Unit) {
        listeners += listener
    }
}

/**
 * Real-time spectrum of whatever the phone is playing (Spotify, YouTube...), read from the global
 * output mix with [Visualizer] on session 0. Needs the microphone permission (Android's rule for
 * any Visualizer), but never opens the mic. Runs only while an equalizer is on screen and media is
 * playing; [hasSignal] stays false when the device blocks output capture, so callers can fall back
 * to an animated placeholder.
 */
object OutputAudioBands {
    const val BANDS = 3
    private const val TAG = "OutputAudioBands"
    private const val MIN_PEAK = 6f

    // Bass / mids / treble edges in Hz
    private val BAND_EDGES_HZ = floatArrayOf(40f, 250f, 2000f, 9000f)

    private var visualizer: Visualizer? = null
    private var users = 0
    private val peaks = FloatArray(BANDS) { MIN_PEAK }
    private var appContext: Context? = null
    private var listening = false

    /** Latest band levels, 0..1. Written on the main thread by the capture listener. */
    val bands = FloatArray(BANDS)

    /** True once real audio data has arrived since capture started. */
    var hasSignal by mutableStateOf(false)
        private set

    fun acquire(context: Context) {
        appContext = context.applicationContext
        AudioPowerPolicy.ensure(context)
        if (!listening) {
            listening = true
            // Battery Saver toggled / screen on-off: restart at the new rate, or stay stopped
            AudioPowerPolicy.addListener {
                val ctx = appContext
                if (users > 0 && ctx != null) {
                    stop()
                    start(ctx)
                }
            }
        }
        users++
        if (users == 1) start(context.applicationContext)
    }

    fun release() {
        users = (users - 1).coerceAtLeast(0)
        if (users == 0) stop()
    }

    private fun start(context: Context) {
        if (visualizer != null || !hasMicPermission(context) || !AudioPowerPolicy.screenOn) return
        val rate = if (AudioPowerPolicy.lowPower) Visualizer.getMaxCaptureRate() / 2 else Visualizer.getMaxCaptureRate()
        visualizer = runCatching {
            Visualizer(0).apply {
                enabled = false
                val range = Visualizer.getCaptureSizeRange()
                captureSize = 512.coerceIn(range[0], range[1])
                scalingMode = Visualizer.SCALING_MODE_NORMALIZED
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(v: Visualizer?, waveform: ByteArray?, samplingRate: Int) {}
                    override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                        if (fft != null) onFft(fft, samplingRate)
                    }
                }, rate, false, true)
                enabled = true
            }
        }.onFailure { Log.w(TAG, "Output visualizer unavailable", it) }.getOrNull()
    }

    private fun stop() {
        visualizer?.let { v ->
            runCatching { v.enabled = false }
            runCatching { v.release() }
        }
        visualizer = null
        hasSignal = false
        bands.fill(0f)
        peaks.fill(MIN_PEAK)
    }

    /** [samplingRate] is in milliHertz. FFT layout: [0]=DC, [1]=Nyquist, then (re, im) pairs. */
    private fun onFft(fft: ByteArray, samplingRate: Int) {
        val bins = fft.size / 2
        if (bins < 4 || samplingRate <= 0) return
        val binHz = (samplingRate / 1000f) / fft.size
        var any = false
        for (b in 0 until BANDS) {
            val lo = (BAND_EDGES_HZ[b] / binHz).toInt().coerceIn(1, bins - 1)
            val hi = (BAND_EDGES_HZ[b + 1] / binHz).toInt().coerceIn(lo + 1, bins)
            var sum = 0f
            for (k in lo until hi) {
                sum += hypot(fft[2 * k].toFloat(), fft[2 * k + 1].toFloat())
            }
            val mag = sum / (hi - lo)
            if (mag > 0.5f) any = true
            // Per-band auto gain: quiet songs and loud songs both fill the bars
            peaks[b] = max(mag, max(MIN_PEAK, peaks[b] * 0.992f))
            bands[b] = sqrt((mag / peaks[b]).coerceIn(0f, 1f))
        }
        if (any && !hasSignal) hasSignal = true
    }
}

/**
 * Live microphone level for recorder cards, kept as a short history so the waveform scrolls like a
 * real recorder. Only runs while the accessibility service is connected: Android then lets the
 * service share the mic with the recording app instead of taking it over. If another app's capture
 * still gets silenced after we start, metering stops for good so a recording is never disturbed.
 */
object MicLevelMeter {
    const val HISTORY = 48
    private const val TAG = "MicLevelMeter"
    private const val SAMPLE_RATE = 16_000
    private const val CHUNK = 800 // 50 ms
    private const val CHUNK_LOW_POWER = 1600 // 100 ms on Battery Saver

    private val main = Handler(Looper.getMainLooper())
    private val history = FloatArray(HISTORY)
    private var head = 0
    private var users = 0
    // The capture thread that currently owns the meter; a stale thread sees it changed and exits
    @Volatile private var worker: Thread? = null
    @Volatile private var blocked = false
    private var appContext: Context? = null
    private var listening = false

    /** Bumps on every new sample; read it in a draw block to redraw. */
    var version by mutableIntStateOf(0)
        private set

    var hasSignal by mutableStateOf(false)
        private set

    /** Level 0..1 of the sample [ago] steps back (0 = newest). */
    fun levelAt(ago: Int): Float = history[(head - 1 - ago).mod(HISTORY)]

    fun acquire(context: Context) {
        appContext = context.applicationContext
        AudioPowerPolicy.ensure(context)
        if (!listening) {
            listening = true
            AudioPowerPolicy.addListener {
                val ctx = appContext
                if (users > 0 && ctx != null) {
                    stop()
                    start(ctx)
                }
            }
        }
        users++
        if (users == 1) start(context.applicationContext)
    }

    fun release() {
        users = (users - 1).coerceAtLeast(0)
        if (users == 0) stop()
    }

    private fun start(context: Context) {
        if (blocked || worker != null || !hasMicPermission(context) || !AudioPowerPolicy.screenOn) return
        if (!IslandAccessibilityService.isConnected.value) return
        val thread = Thread({ captureLoop(context) }, "island-mic-meter").apply { isDaemon = true }
        worker = thread
        thread.start()
    }

    private fun stop() {
        worker = null
        history.fill(0f)
        hasSignal = false
    }

    @Suppress("MissingPermission")
    private fun captureLoop(context: Context) {
        val me = Thread.currentThread()
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        // Sessions that were already silenced before we started aren't our doing
        val silencedBefore = runCatching {
            audioManager?.activeRecordingConfigurations
                ?.filter { it.isClientSilenced }
                ?.map { it.clientAudioSessionId }
                ?.toSet()
        }.getOrNull() ?: emptySet()

        val minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val record = runCatching {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                max(minBuf, CHUNK_LOW_POWER * 2)
            )
        }.getOrNull()
        if (record == null || record.state != AudioRecord.STATE_INITIALIZED) {
            record?.release()
            main.post { if (worker === me) worker = null }
            return
        }
        try {
            record.startRecording()
            val ourSession = record.audioSessionId
            val buf = ShortArray(if (AudioPowerPolicy.lowPower) CHUNK_LOW_POWER else CHUNK)
            var reads = 0
            var ownSilencedReads = 0
            while (worker === me) {
                val n = record.read(buf, 0, buf.size)
                if (n <= 0) break
                var sum = 0.0
                for (i in 0 until n) {
                    val s = buf[i].toDouble()
                    sum += s * s
                }
                val rms = sqrt(sum / n)
                val db = if (rms > 0.0) 20 * log10(rms / 32768.0) else -100.0
                val level = ((db + 58.0) / 46.0).coerceIn(0.0, 1.0).toFloat()
                val audible = rms > 2.0
                main.post {
                    if (worker !== me) return@post
                    history[head] = level
                    head = (head + 1) % HISTORY
                    if (audible && !hasSignal) hasSignal = true
                    version++
                }

                if (++reads % (if (buf.size > CHUNK) 5 else 10) == 0) {
                    val configs = runCatching { audioManager?.activeRecordingConfigurations }.getOrNull().orEmpty()
                    val hurtOther = configs.any {
                        it.clientAudioSessionId != ourSession && it.isClientSilenced && it.clientAudioSessionId !in silencedBefore
                    }
                    if (hurtOther) {
                        Log.w(TAG, "Another recorder got silenced; disabling mic metering")
                        blocked = true
                        break
                    }
                    // We're the one being silenced (a privacy-sensitive recorder): nothing to show
                    val ownSilenced = runCatching { record.activeRecordingConfiguration?.isClientSilenced }.getOrNull() == true
                    ownSilencedReads = if (ownSilenced) ownSilencedReads + 1 else 0
                    if (ownSilencedReads >= 2) break
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Mic metering failed", e)
        } finally {
            runCatching { record.stop() }
            record.release()
            main.post {
                if (worker === me) worker = null
                if (worker == null) hasSignal = false
            }
        }
    }
}

/** Keeps the output spectrum running while [active] and this composable is on screen. */
@Composable
fun OutputAudioBandsEffect(active: Boolean) {
    val context = LocalContext.current
    DisposableEffect(active) {
        if (!active) return@DisposableEffect onDispose {}
        OutputAudioBands.acquire(context)
        onDispose { OutputAudioBands.release() }
    }
}

/** Keeps the mic meter running while [active] and this composable is on screen. */
@Composable
fun MicLevelEffect(active: Boolean) {
    val context = LocalContext.current
    DisposableEffect(active) {
        if (!active) return@DisposableEffect onDispose {}
        MicLevelMeter.acquire(context)
        onDispose { MicLevelMeter.release() }
    }
}
