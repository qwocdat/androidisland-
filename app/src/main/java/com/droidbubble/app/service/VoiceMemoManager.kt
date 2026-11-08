package com.droidbubble.app.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import com.droidbubble.app.island.IslandStateManager
import com.droidbubble.app.model.VoiceMemoState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sin

object VoiceMemoManager {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var recorder: MediaRecorder? = null
    private var timerJob: Job? = null
    private var startTimeMs: Long = 0L
    private var currentFile: File? = null

    fun isRecording(): Boolean = IslandStateManager.voiceMemoState.value.isRecording

    fun toggleRecording(context: Context) {
        if (isRecording()) {
            stopRecording()
        } else {
            startRecording(context)
        }
    }

    fun startRecording(context: Context) {
        if (isRecording()) return
        val appContext = context.applicationContext
        val hasMicPerm = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        var startedRealRecorder = false
        if (hasMicPerm) {
            runCatching {
                val musicDir = appContext.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: appContext.cacheDir
                val memoDir = File(musicDir, "VoiceMemos").apply { mkdirs() }
                val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val outFile = File(memoDir, "MEMO_$ts.m4a")
                currentFile = outFile

                val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaRecorder(appContext)
                } else {
                    @Suppress("DEPRECATION")
                    MediaRecorder()
                }
                mr.setAudioSource(MediaRecorder.AudioSource.MIC)
                mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                mr.setAudioEncodingBitRate(128000)
                mr.setAudioSamplingRate(44100)
                mr.setOutputFile(outFile.absolutePath)
                mr.prepare()
                mr.start()
                recorder = mr
                startedRealRecorder = true
            }
        }

        startTimeMs = System.currentTimeMillis()
        IslandStateManager.onStopVoiceMemo = { stopRecording() }
        IslandStateManager.updateVoiceMemoState(
            VoiceMemoState(
                isRecording = true,
                durationSeconds = 0L,
                amplitude = 0.5f,
                filePath = currentFile?.absolutePath
            )
        )

        timerJob?.cancel()
        timerJob = scope.launch {
            var tick = 0
            while (isActive) {
                delay(250L)
                tick++
                val elapsedSec = ((System.currentTimeMillis() - startTimeMs) / 1000L).coerceAtLeast(0L)
                val amp = if (startedRealRecorder && recorder != null) {
                    val raw = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)
                    (raw / 18000f).coerceIn(0.18f, 1.0f)
                } else {
                    (0.45f + 0.35f * sin(tick * 0.6f)).coerceIn(0.2f, 0.95f)
                }
                IslandStateManager.updateVoiceMemoState(
                    VoiceMemoState(
                        isRecording = true,
                        durationSeconds = elapsedSec,
                        amplitude = amp,
                        filePath = currentFile?.absolutePath
                    )
                )
            }
        }
    }

    fun stopRecording() {
        timerJob?.cancel()
        timerJob = null
        runCatching {
            recorder?.stop()
        }
        runCatching {
            recorder?.reset()
            recorder?.release()
        }
        recorder = null
        IslandStateManager.updateVoiceMemoState(
            VoiceMemoState(isRecording = false, durationSeconds = 0L, amplitude = 0f, filePath = currentFile?.absolutePath)
        )
    }
}
