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
import java.io.IOException
import java.io.File

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
                println("Error onMethodCall startRecordScreen")
                println(e.message)
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

    fun prepareRecordScreen() {
        try {
            try {
                mFileName = registrar.context().getExternalCacheDir()?.getAbsolutePath()
                mFileName += "/$videoName.mp4"
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
            }
            if (recordInternalAudio!!) {
                println("Record Internal Audio")
//                mMediaRecorder?.setAudioSource(MediaRecorder.AudioSource.REMOTE_SUBMIX);
//                mMediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
//                mMediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            }
            if (!recordAudio!! && !recordInternalAudio!!) {
                mMediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            }
            println("Record Screen")
            mMediaRecorder?.setOutputFile(mFileName)
            mMediaRecorder?.setVideoSize(mDisplayWidth, mDisplayHeight)
            mMediaRecorder?.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            mMediaRecorder?.setVideoEncodingBitRate(5 * mDisplayWidth * mDisplayHeight)
            mMediaRecorder?.setVideoFrameRate(30)

            mMediaRecorder?.prepare()
        } catch (e: IOException) {
            Log.d("--INIT-RECORDER", e.message+"")
            println("Error prepareRecordScreen")
            e.printStackTrace()
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
            println("Error startRecord")
            println(e.message)
            e.printStackTrace()
            throw e
        }
    }



    fun stopRecord() {
        try {
            println("stopRecordScreen")
            if (recordInternalAudio!!) {
                stopRecordAudio()
            }
            mMediaRecorder?.stop()
            mMediaRecorder?.reset()
            if (recordInternalAudio!!) {
                joinFiles()
            }
            println("stopRecordScreen success")
        } catch (e: Exception) {
            Log.d("--INIT-RECORDER", e.message +"")
            println("stopRecordScreen error")
            e.printStackTrace()
        } finally {
            stopScreenSharing()
        }
    }


    private fun joinFiles() {
        Log.d("--FFmpeg", "Joining files")
        convertAudioToWav()
        var mMergeFileName = mFileName?.replace(".mp4", "_merge.mp4")
        var command = "-i $mFileName -i $audioPath -c:v copy -c:a aac -map 0:v:0 -map 1:a:0 -shortest $mMergeFileName"
        println(command)
        try {
            FFmpeg.executeAsync(command, object : ExecuteCallback {
                override fun apply(executionId: Long, returnCode: Int) {
                    if (returnCode == 1) {
//                        var file = File(mFileName)
//                        if (file.exists()) {
//                            file.delete()
//                        }
//                        var fileAudio = File(audioPath!!)
//                        if (fileAudio.exists()) {
//                            fileAudio.delete()
//                        }
                        var fileMerge = File(mMergeFileName)
                        if (fileMerge.exists()) {
                            fileMerge.renameTo(File(mFileName))
                        }else {
                            throw Exception("Error merging files")
                        }
                    } else {
                        throw Exception("Error merging files $returnCode")
                    }
                }
            })
        }catch (e: Exception) {
            Log.d("--FFmpeg Error", e.message!!)
            e.printStackTrace()
            throw e
        }
    }


    @kotlin.Throws(IOException::class)
    private fun rawToWave(rawFile: File, waveFile: File) {
        val rawData = ByteArray(rawFile.length() as Int)
        var input: DataInputStream? = null
        try {
            input = DataInputStream(FileInputStream(rawFile))
            input.read(rawData)
        } finally {
            if (input != null) {
                input.close()
            }
        }

        var output: DataOutputStream? = null
        try {
            output = DataOutputStream(FileOutputStream(waveFile))
            // WAVE header
            // see http://ccrma.stanford.edu/courses/422/projects/WaveFormat/
            writeString(output, "RIFF") // chunk id
            writeInt(output, 36 + rawData.size) // chunk size
            writeString(output, "WAVE") // format
            writeString(output, "fmt ") // subchunk 1 id
            writeInt(output, 16) // subchunk 1 size
            writeShort(output, 1.toShort()) // audio format (1 = PCM)
            writeShort(output, 1.toShort()) // number of channels
            writeInt(output, 44100) // sample rate
            writeInt(output, RECORDER_SAMPLERATE * 2) // byte rate
            writeShort(output, 2.toShort()) // block align
            writeShort(output, 16.toShort()) // bits per sample
            writeString(output, "data") // subchunk 2 id
            writeInt(output, rawData.size) // subchunk 2 size
            // Audio data (conversion big endian -> little endian)
            val shorts = ShortArray(rawData.size / 2)
            ByteBuffer.wrap(rawData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)
            val bytes: ByteBuffer = ByteBuffer.allocate(shorts.size * 2)
            for (s in shorts) {
                bytes.putShort(s)
            }

            output.write(fullyReadFileToBytes(rawFile))
        } finally {
            if (output != null) {
                output.close()
            }
        }
    }

    @kotlin.Throws(IOException::class)
    fun fullyReadFileToBytes(f: File): ByteArray {
        val size = f.length() as Int
        val bytes = ByteArray(size)
        val tmpBuff = ByteArray(size)
        val fis: FileInputStream = FileInputStream(f)
        try {
            var read: Int = fis.read(bytes, 0, size)
            if (read < size) {
                var remain = size - read
                while (remain > 0) {
                    read = fis.read(tmpBuff, 0, remain)
                    System.arraycopy(tmpBuff, 0, bytes, size - remain, read)
                    remain -= read
                }
            }
        } catch (e: IOException) {
            throw e
        } finally {
            fis.close()
        }

        return bytes
    }

    @kotlin.Throws(IOException::class)
    private fun writeInt(output: DataOutputStream?, value: Int) {
        output.write(value shr 0)
        output.write(value shr 8)
        output.write(value shr 16)
        output.write(value shr 24)
    }

    @kotlin.Throws(IOException::class)
    private fun writeShort(output: DataOutputStream?, value: Short) {
        output.write(value.toInt() shr 0)
        output.write(value.toInt() shr 8)
    }

    @kotlin.Throws(IOException::class)
    private fun writeString(output: DataOutputStream?, value: String) {
        for (i in 0 until value.length()) {
            output.write(value.charAt(i))
        }
    }


    private fun convertAudioToWav() {
        var mWavFileName = audioPath?.replace(".pcm", ".wav")
        var file = File(mWavFileName)
        if (file.exists()) {
            file.delete()
        }
        try {
            rawToWave(File(audioPath!!), file)
            var fileAudio = File(audioPath!!)
            if (fileAudio.exists()) {
                fileAudio.delete()
            }
            audioPath = mWavFileName
        } catch (e: Exception) {
            println("Error convertAudioToWav")
            println(e.message)
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
                    println("Error getMediaProjection")
                    println(e.message)
                    e.printStackTrace()
                    throw e
                }
            }
            var audioConfig = AudioPlaybackCaptureConfiguration.Builder(mMediaProjection!!).addMatchingUsage(AudioAttributes.USAGE_MEDIA).build();
            var sampleRate = 8000
            var channelConfig = AudioFormat.CHANNEL_IN_MONO
            var audioFormat = AudioFormat.ENCODING_PCM_16BIT
            var minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            audioRecord = AudioRecord.Builder()
                    .setAudioPlaybackCaptureConfig(audioConfig)
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
                    if (numberOfReadBytes > 0) {
                        fileOutputStream.write(audioData, 0, numberOfReadBytes)
                    }
                }
                fileOutputStream.close()
                audioRecord?.stop()
                audioRecord?.release()
            }.start()
        } catch (e: Exception) {
            println("Error startRecordAudio")
            println(e.message)
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
            audioRecord?.release()
            mMediaRecorder?.reset()
            mMediaProjection = null
            stopScreenSharing()
        }
    }
}