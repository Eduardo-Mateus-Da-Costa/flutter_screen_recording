package com.isvisoft.flutter_screen_recording

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.WindowManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.getSystemService
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry
import io.flutter.plugin.common.PluginRegistry.Registrar
import java.io.File
import java.io.IOException
import android.media.MediaRecorder.AudioSource
import com.foregroundservice.ForegroundService

import android.media.AudioFormat
import android.media.AudioRecord
import java.io.FileOutputStream
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioAttributes

import com.arthenica.mobileffmpeg.ExecuteCallback;
import com.arthenica.mobileffmpeg.FFmpeg;
import com.arthenica.mobileffmpeg.FFmpegExecution;

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class FlutterScreenRecordingPlugin(
        private val registrar: Registrar
) : MethodCallHandler, PluginRegistry.ActivityResultListener{

    var mScreenDensity: Int = 0
    var mMediaRecorder: MediaRecorder? = null
    var mProjectionManager: MediaProjectionManager? = null
    var mMediaProjection: MediaProjection? = null
    var mMediaProjectionCallback: MediaProjectionCallback? = null
    var mVirtualDisplay: VirtualDisplay? = null
    var mDisplayWidth: Int = 1280
    var mDisplayHeight: Int = 800
    var videoName: String? = ""
    var mFileName: String? = ""
    var recordAudio: Boolean? = false;
    var recordInternalAudio: Boolean? = false;
    var isRecordingAudio: Boolean = false;
    var audioPath: String? = ""
    var audioRecord: AudioRecord? = null
    var intentData: Intent? = null
    private val SCREEN_RECORD_REQUEST_CODE = 333

    private lateinit var _result: MethodChannel.Result


    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val channel = MethodChannel(registrar.messenger(), "flutter_screen_recording")
            val plugin = FlutterScreenRecordingPlugin(registrar)
            channel.setMethodCallHandler(plugin)
            registrar.addActivityResultListener(plugin)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        intentData = data
        if (requestCode == SCREEN_RECORD_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                mMediaProjectionCallback = MediaProjectionCallback()
                mMediaProjection = mProjectionManager?.getMediaProjection(resultCode, data!!)
                mMediaProjection?.registerCallback(mMediaProjectionCallback, null)
                mVirtualDisplay = createVirtualDisplay()
                _result.success(true)
                return true
            } else {
                _result.success(false)
            }
        }
        return false
    }

    override fun  onMethodCall(call: MethodCall, result: Result) {
        if (call.method == "startRecordScreen") {
            try {
                _result = result
                var title = call.argument<String?>("title")
                var message = call.argument<String?>("message")
                if (title == null || title == "") {
                    title = "Your screen is being recorded";
                }
                if (message == null || message == "") {
                    message = "Your screen is being recorded"
                }
                ForegroundService.startService(registrar.context(), title, message)
                mProjectionManager = registrar.context().applicationContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager?

                val metrics = DisplayMetrics()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    mMediaRecorder = MediaRecorder(registrar.context().applicationContext)
                } else {
                    @Suppress("DEPRECATION")
                    mMediaRecorder = MediaRecorder()
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val display = registrar.activity()!!.display
                    display?.getRealMetrics(metrics)
                } else {
                    val defaultDisplay = registrar.context().applicationContext.getDisplay()
                    defaultDisplay?.getMetrics(metrics)
                }
                mScreenDensity = metrics.densityDpi
                calculeResolution(metrics)
                videoName = call.argument<String?>("name")
                recordAudio = call.argument<Boolean?>("audio")
                recordInternalAudio = call.argument<Boolean?>("internalaudio")
                prepareRecordScreen()

            } catch (e: Exception) {
                Log.d("Error onMethodCall startRecordScreen", e.message+"")
                e.printStackTrace()
                result.success(false)
            }
        } else if (call.method == "stopRecordScreen") {
            try {
                ForegroundService.stopService(registrar.context())
                if (mMediaRecorder != null) {
                    stopRecord()
                    result.success(mFileName)
                } else {
                    result.success("")
                }
            } catch (e: Exception) {
                Log.d("Error onMethodCall stopRecordScreen", e.message+"")
                e.printStackTrace()
                result.success("")
            }
        } else {
            result.notImplemented()
        }
    }

    private fun calculeResolution(metrics: DisplayMetrics) {

        mDisplayHeight = metrics.heightPixels
        mDisplayWidth = metrics.widthPixels

        var maxRes = 1280.0;
        if (metrics.scaledDensity >= 3.0f) {
            maxRes = 1920.0;
        }
        if (metrics.widthPixels > metrics.heightPixels) {
            var rate = metrics.widthPixels / maxRes

            if(rate > 1.5){
                rate = 1.5
            }
            mDisplayWidth = maxRes.toInt()
            mDisplayHeight = (metrics.heightPixels / rate).toInt()
        } else {
            var rate = metrics.heightPixels / maxRes
            if(rate > 1.5){
                rate = 1.5
            }
            mDisplayHeight = maxRes.toInt()
            mDisplayWidth = (metrics.widthPixels / rate).toInt()
        }
    }

    fun prepareRecordScreen() {
        try {
            try {
                mFileName = registrar.context().getExternalCacheDir()?.getAbsolutePath()
                mFileName += "/$videoName.mp4"
            } catch (e: IOException) {
                Log.d("PrepareRecordScreen Error", e.message+"")
                e.printStackTrace()
                throw e
            }
            mMediaRecorder?.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            if (recordAudio!!) {
                mMediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC);
                mMediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                mMediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            } else {
                mMediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            }
            mMediaRecorder?.setOutputFile(mFileName)
            mMediaRecorder?.setVideoSize(mDisplayWidth, mDisplayHeight)
            mMediaRecorder?.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            mMediaRecorder?.setVideoEncodingBitRate(5 * mDisplayWidth * mDisplayHeight)
            mMediaRecorder?.setVideoFrameRate(30)

            mMediaRecorder?.prepare()
        } catch (e: IOException) {
            Log.d("PrepareRecordScreen Error", e.message+"")
            e.printStackTrace()
            throw e
        }
        val permissionIntent = mProjectionManager?.createScreenCaptureIntent()
        ActivityCompat.startActivityForResult(registrar.activity()!!, permissionIntent!!, SCREEN_RECORD_REQUEST_CODE, null)
    }


    fun startRecord() {
        try {
            if (recordInternalAudio!!) {
                startRecordAudio()
            }
            mMediaRecorder?.start()
        }catch (e: Exception) {
            Log.d("Error startRecord", e.message+"")
            e.printStackTrace()
            throw e
        }
    }



    fun stopRecord() {
        try {
            if (recordInternalAudio!!) {
                stopRecordAudio()
            }
            mMediaRecorder?.stop()
            mMediaRecorder?.reset()
            if (recordInternalAudio!!) {
                joinFiles()
            }
        } catch (e: Exception) {
            Log.d("StopRecord Error", e.message+"")
            e.printStackTrace()
        } finally {
            stopScreenSharing()
        }
    }


    private fun joinFiles() {
        try {
            convertAudioToWav()
            var mMergeFileName = mFileName?.replace(".mp4", "_merge.mp4")
            var command = "-i $mFileName -i $audioPath -c:v copy -c:a aac -map 0:v:0 -map 1:a:0 -shortest $mMergeFileName"
            FFmpeg.execute(command)
            var fileMerge = File(mMergeFileName)
            var fileVideo = File(mFileName)
            var fileAudio = File(audioPath!!)
            if (fileAudio.exists()) {
                fileAudio.delete()
            }
            if (fileVideo.exists()) {
                fileVideo.delete()
            }
            if (fileMerge.exists()) {
                fileMerge.renameTo(File(mFileName))
            }else {
                throw Exception("Error merging files")
            }
        }catch (e: Exception) {
            Log.d("FFmpeg Error", e.message+"")
            e.printStackTrace()
            throw e
        }
    }



    fun convertAudioToWav() {
        try {
            var mWavFileName = audioPath?.replace(".pcm", ".wav")
            var file = File(mWavFileName)
            if (file.exists()) {
                file.delete()
            }
            var command = "-f s16le -ar 44.1k -ac 1 -i $audioPath $mWavFileName"
            FFmpeg.execute(command)
            var fileAudio = File(audioPath!!)
            var fileWav = File(mWavFileName)
            if (!fileWav.exists()) {
                throw Exception("Error converting audio to wav")
            }
            if (fileAudio.exists()) {
                fileAudio.delete()
            }
            audioPath = mWavFileName
        } catch (e: Exception) {
            Log.d("FFmpeg Error Converting to Wav", e.message+"")
            e.printStackTrace()
            throw e
        }
    }



    fun startRecordAudio() {
        try {
            if (mMediaProjection == null) {
                try{
                    mMediaProjection = mProjectionManager?.getMediaProjection(Activity.RESULT_OK, intentData!!)
                }catch (e: Exception) {
                    Log.d("Error getMediaProjection", e.message+"")
                    e.printStackTrace()
                    throw e
                }
            }
//            USAGE_ALARM
//            Usage value to use when the usage is an alarm (e.g. wake-up alarm).
//
//            int	USAGE_ASSISTANCE_ACCESSIBILITY
//                    Usage value to use when the usage is for accessibility, such as with a screen reader.
//
//            int	USAGE_ASSISTANCE_NAVIGATION_GUIDANCE
//                    Usage value to use when the usage is driving or navigation directions.
//
//            int	USAGE_ASSISTANCE_SONIFICATION
//                    Usage value to use when the usage is sonification, such as with user interface sounds.
//
//            int	USAGE_ASSISTANT
//                    Usage value to use for audio responses to user queries, audio instructions or help utterances.
//
//            int	USAGE_GAME
//                    Usage value to use when the usage is for game audio.
//
//            int	USAGE_MEDIA
//                    Usage value to use when the usage is media, such as music, or movie soundtracks.
//
//            int	USAGE_NOTIFICATION
//                    Usage value to use when the usage is notification.
//
//            int	USAGE_NOTIFICATION_COMMUNICATION_DELAYED
//            This constant was deprecated in API level 33. Use USAGE_NOTIFICATION which is handled the same way as this usage by the audio framework
//
//            int	USAGE_NOTIFICATION_COMMUNICATION_INSTANT
//                    This constant was deprecated in API level 33. Use USAGE_NOTIFICATION which is handled the same way as this usage by the audio framework
//
//            int	USAGE_NOTIFICATION_COMMUNICATION_REQUEST
//                    This constant was deprecated in API level 33. Use USAGE_NOTIFICATION which is handled the same way as this usage by the audio framework
//
//            int	USAGE_NOTIFICATION_EVENT
//                    Usage value to use when the usage is to attract the user's attention, such as a reminder or low battery warning.
//
//            int	USAGE_NOTIFICATION_RINGTONE
//                    Usage value to use when the usage is telephony ringtone.
//
//            int	USAGE_UNKNOWN
//                    Usage value to use when the usage is unknown.
//
//            int	USAGE_VOICE_COMMUNICATION
//            Usage value to use when the usage is voice communications, such as telephony or VoIP.
//
//            int	USAGE_VOICE_COMMUNICATION_SIGNALLING
//                    Usage value to use when the usage is in-call signalling, such as with a "busy" beep, or DTMF tones.
            var audioConfig = AudioPlaybackCaptureConfiguration.Builder(mMediaProjection!!)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                .addMatchingUsage(AudioAttributes.USAGE_ALARM)
                .addMatchingUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                .addMatchingUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                .addMatchingUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .addMatchingUsage(AudioAttributes.USAGE_ASSISTANT)
                .addMatchingUsage(AudioAttributes.USAGE_NOTIFICATION)
                .addMatchingUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_DELAYED)
                .addMatchingUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                .addMatchingUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_REQUEST)
                .addMatchingUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .addMatchingUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .addMatchingUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .addMatchingUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION_SIGNALLING)
                .build()
            var sampleRate = 44100
            var channelConfig = AudioFormat.CHANNEL_IN_MONO
            var audioFormat = AudioFormat.ENCODING_PCM_16BIT
            var minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
//            audioRecord = AudioRecord.Builder()
//                    .setAudioPlaybackCaptureConfig(audioConfig)
//                    .setAudioFormat(AudioFormat.Builder()
//                            .setEncoding(audioFormat)
//                            .setSampleRate(sampleRate)
//                            .setChannelMask(channelConfig)
//                            .build())
//                    .setBufferSizeInBytes(minBufferSize)
//                    .build()
            audioRecord = AudioRecord.Builder()
                    .setAudioSource(AudioSource.REMOTE_SUBMIX)
                    .setAudioFormat(AudioFormat.Builder()
                            .setEncoding(audioFormat)
                            .setSampleRate(sampleRate)
                            .setChannelMask(channelConfig)
                            .build())
                    .setBufferSizeInBytes(minBufferSize)
                    .build()
            var audioData = ByteArray(minBufferSize)
            audioPath = mFileName?.replace(".mp4", ".pcm")
            var file = File(audioPath!!)
            if (file.exists()) {
                file.delete()
            }
            file.createNewFile()
            var fileOutputStream = FileOutputStream(file)
            isRecordingAudio = true
            audioRecord!!.startRecording()
            Thread {
                while (isRecordingAudio) {
                    var numberOfReadBytes = audioRecord!!.read(audioData, 0, minBufferSize)
                    if (numberOfReadBytes < 0) {
                        val emptyBuffer = ByteArray(minBufferSize)
                        fileOutputStream.write(emptyBuffer)
                    }else {
                        fileOutputStream.write(audioData, 0, numberOfReadBytes)
                    }
                }
                fileOutputStream.close()
                audioRecord?.stop()
                audioRecord?.release()
            }.start()
        } catch (e: Exception) {
            Log.d("Error startRecordAudio", e.message+"")
            e.printStackTrace()
            throw e
        }
    }


    fun stopRecordAudio() {
        isRecordingAudio = false
    }


    private fun createVirtualDisplay(): VirtualDisplay? {
        try {
            startRecord()
            return mMediaProjection?.createVirtualDisplay(
                    "MainActivity", mDisplayWidth, mDisplayHeight, mScreenDensity,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mMediaRecorder?.surface, null, null
            )
        } catch (e: Exception) {
            Log.d("Error createVirtualDisplay", e.message+"")
            e.printStackTrace()
            return null
        }
    }

    private fun stopScreenSharing() {
        if (mVirtualDisplay != null) {
            mVirtualDisplay?.release()
            if (mMediaProjection != null) {
                mMediaProjection?.unregisterCallback(mMediaProjectionCallback)
                mMediaProjection?.stop()
                mMediaProjection = null
            }
            Log.d("TAG", "MediaProjection Stopped")
        }
    }

    inner class MediaProjectionCallback : MediaProjection.Callback() {
        override fun onStop() {
            isRecordingAudio = false
            audioRecord?.release()
            mMediaRecorder?.reset()
            mMediaProjection = null
            stopScreenSharing()
        }
    }
}