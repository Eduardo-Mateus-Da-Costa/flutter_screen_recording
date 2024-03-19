package com.isvisoft.flutter_screen_recording

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
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
import android.media.AudioFormat
import android.media.AudioAttributes
import android.media.MediaRecorder.AudioSource
import java.io.FileOutputStream
import com.foregroundservice.ForegroundService

import com.arthenica.mobileffmpeg.ExecuteCallback;
import com.arthenica.mobileffmpeg.FFmpeg;
import com.arthenica.mobileffmpeg.FFmpegExecution;


class FlutterScreenRecordingPlugin(
        private val registrar: Registrar
) : MethodCallHandler, PluginRegistry.ActivityResultListener{

    var mScreenDensity: Int = 0
    var mMediaRecorder: MediaRecorder? = null
    var mProjectionManager: MediaProjectionManager? = null
    var mMediaProjection: MediaProjection? = null
    var mMediaProjectionCallback: MediaProjectionCallback? = null
    var mVirtualDisplay: VirtualDisplay? = null
    var mAudioRecord: AudioRecord? = null
    var mDisplayWidth: Int = 1280
    var mDisplayHeight: Int = 800
    var videoName: String? = ""
    var mFileName: String? = ""
    var mAudioFileName: String? = ""
    var recordAudio: Boolean? = false;
    var recordInternalAudio: Boolean? = false;
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
        if (requestCode == SCREEN_RECORD_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                mMediaProjectionCallback = MediaProjectionCallback()
                mMediaProjection = mProjectionManager?.getMediaProjection(resultCode, data!!)
                mMediaProjection?.registerCallback(mMediaProjectionCallback, null)
                mVirtualDisplay = createVirtualDisplay()
                _result.success(true)
                startRecord()
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
                startRecordScreen()

            } catch (e: Exception) {
                println("Error onMethodCall startRecordScreen")
                println(e.message)
                result.success(false)
            }
        } else if (call.method == "stopRecordScreen") {
            try {
                ForegroundService.stopService(registrar.context())
                if (mMediaRecorder != null) {
                    stopRecordScreen()
                    result.success(mFileName)
                } else {
                    result.success("")
                }
            } catch (e: Exception) {
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
            println("Rate : $rate")
        } else {
            var rate = metrics.heightPixels / maxRes
            if(rate > 1.5){
                rate = 1.5
            }
            mDisplayHeight = maxRes.toInt()
            mDisplayWidth = (metrics.widthPixels / rate).toInt()
            println("Rate : $rate")
        }

        println("Scaled Density")
        println(metrics.scaledDensity)
        println("Original Resolution ")
        println(metrics.widthPixels.toString() + " x " + metrics.heightPixels)
        println("Calcule Resolution ")
        println("$mDisplayWidth x $mDisplayHeight")
    }

    fun startRecord() {
        try {
            mFileName = registrar.context().getExternalCacheDir()?.getAbsolutePath()
            mFileName += "/$videoName.mp4"
            mAudioFileName = mFileName?.replace(".mp4", ".mp3")
        } catch (e: IOException) {
            println("Error creating name")
            return
        }
        mMediaRecorder?.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        if (recordAudio!!) {
            println("Record Audio")
            mMediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mMediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        } else {
            mMediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        }
        if (recordInternalAudio!!) {
            println("Record Internal Audio")
            mMediaRecorder?.setAudioSource(MediaRecorder.AudioSource.REMOTE_SUBMIX);
            mMediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mMediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
//            try{
//                var mAudioFormat = AudioFormat.Builder()
//                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
//                        .setSampleRate(8000)
//                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
//                        .build()
//                var mAudioPlaybackCaptureConfig = AudioPlaybackCaptureConfiguration.Builder(mMediaProjection!!)
//                        .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
//                        .build()
//                mAudioRecord = AudioRecord.Builder().setAudioFormat(mAudioFormat)
//                        .setAudioPlaybackCaptureConfig(mAudioPlaybackCaptureConfig)
//                        .build()
//            }catch (e: Exception){
//                println("Error AudioRecord")
//                println(e.message)
//            }
        }
        println("Record Screen")
        mMediaRecorder?.setOutputFile(mFileName)
        mMediaRecorder?.setVideoSize(mDisplayWidth, mDisplayHeight)
        mMediaRecorder?.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        mMediaRecorder?.setVideoEncodingBitRate(5 * mDisplayWidth * mDisplayHeight)
        mMediaRecorder?.setVideoFrameRate(30)

        mMediaRecorder?.prepare()
        mMediaRecorder?.start()
//        mAudioRecord?.startRecording()
    }


    fun startRecordScreen() {
        try {
            val permissionIntent = mProjectionManager?.createScreenCaptureIntent()
            ActivityCompat.startActivityForResult(registrar.activity()!!, permissionIntent!!, SCREEN_RECORD_REQUEST_CODE, null)

        } catch (e: Exception) {
            Log.e("--INIT-RECORDER", "Error starting screen capture intent: ${e.message}")
            return
        }
    }


    fun startAudioSave() {
        val file = File(mAudioFileName)
        if (file.exists()) {
            file.delete()
        }
        val buffer = ByteArray(1024)
        val fileOutputStream = FileOutputStream(mAudioFileName)
        while (mAudioRecord?.read(buffer, 0, buffer.size)!! > 0) {
            println("Buffer : $buffer")
            fileOutputStream.write(buffer)
        }
        println("startAudioSave success")
        fileOutputStream.close()
    }

    fun stopRecordScreen() {
        try {
            println("stopRecordScreen")
//            mAudioRecord?.stop()
            mMediaRecorder?.stop()
//            if (recordInternalAudio!!){
//                println("stopRecordScreen startAudioSave")
//                startAudioSave()
//                mAudioRecord?.release()
//            }
            mMediaRecorder?.reset()
//            joinFiles()
            println("stopRecordScreen success")

        } catch (e: Exception) {
            Log.d("--INIT-RECORDER", e.message +"")
            println("stopRecordScreen error")
            println(e.message)

        } finally {
            stopScreenSharing()
        }
    }

    fun joinFiles() {
        Log.d("--FFmpeg", "Joining files")
        println("Joining files")
        val mMergeFileName = mFileName?.replace(".mp4", "_merge.mp4")
        val command = "-i $mFileName -i $mAudioFileName -c:v copy -c:a aac -map 0:v:0 -map 1:a:0 -shortest $mMergeFileName"
        println(command)
        println("Executing FFmpeg command")
        FFmpeg.executeAsync(command, object : ExecuteCallback {
            override fun apply(executionId: Long, returnCode: Int) {
                if (returnCode == 1) {
                    val file = File(mFileName)
                    if (file.exists()) {
                        file.delete()
                    }
                    val fileAudio = File(mAudioFileName)
                    if (fileAudio.exists()) {
                        fileAudio.delete()
                    }
                    val fileMerge = File(mMergeFileName)
                    if (fileMerge.exists()) {
                        fileMerge.renameTo(File(mFileName))
                    }
                    println("Joining files success")
                } else {
                    println("Joining files error")
                    throw Exception("Error merging files $returnCode")
                }
            }
        })
    }

    private fun createVirtualDisplay(): VirtualDisplay? {
        try {
            return mMediaProjection?.createVirtualDisplay(
                    "MainActivity", mDisplayWidth, mDisplayHeight, mScreenDensity,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mMediaRecorder?.surface, null, null
            )
        } catch (e: Exception) {
            println("createVirtualDisplay err")
            println(e.message)
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
            mMediaRecorder?.reset()
//            mAudioRecord?.release()
            mMediaProjection = null
            stopScreenSharing()
        }
    }
}