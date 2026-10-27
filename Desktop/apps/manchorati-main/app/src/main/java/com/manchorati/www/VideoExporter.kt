package com.manchorati.www

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executors

object VideoExporter {

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    private const val VIDEO_MIME = MediaFormat.MIMETYPE_VIDEO_AVC
    private const val AUDIO_MIME = MediaFormat.MIMETYPE_AUDIO_AAC
    private const val FRAME_RATE = 30
    private const val I_FRAME_INTERVAL = 1
    private const val BIT_RATE_VIDEO = 3_500_000 // 3.5 Mbps for smooth HD
    private const val BIT_RATE_AUDIO = 128_000
    private const val SAMPLE_RATE_AUDIO = 44100
    private const val TIMEOUT_USEC = 10_000L

    fun exportVideo(
        context: Context,
        config: VideoMotionRenderer.VideoConfig,
        customAudioUri: Uri? = null,
        onProgress: (Int) -> Unit,
        onSuccess: (File) -> Unit,
        onError: (Exception) -> Unit
    ) {
        executor.execute {
            var muxer: MediaMuxer? = null
            var videoCodec: MediaCodec? = null
            var inputSurface: CodecInputSurface? = null
            var bitmap: Bitmap? = null

            try {
                val isStory = config.aspectRatio == "9:16"
                val width = 720
                val height = if (isStory) 1280 else 720

                val cacheDir = File(context.cacheDir, "exported_videos").apply { mkdirs() }
                val outputFile = File(cacheDir, "reel_${System.currentTimeMillis()}.mp4")

                muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

                // 1. Configure Video Encoder
                val videoFormat = MediaFormat.createVideoFormat(VIDEO_MIME, width, height).apply {
                    setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                    setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE_VIDEO)
                    setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
                    setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)
                }

                videoCodec = MediaCodec.createEncoderByType(VIDEO_MIME)
                videoCodec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                inputSurface = CodecInputSurface(videoCodec.createInputSurface())
                videoCodec.start()

                // 2. Prepare Audio PCM Data
                val totalDurationSec = config.durationSec
                val pcmData = getAudioPcmBytes(context, config.audioPreset, customAudioUri, totalDurationSec)

                // 3. Encode Video Frames
                val totalFrames = totalDurationSec * FRAME_RATE
                bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)

                var videoTrackIndex = -1
                var audioTrackIndex = -1
                var muxerStarted = false

                val bufferInfo = MediaCodec.BufferInfo()

                for (frame in 0 until totalFrames) {
                    val timeMs = (frame * 1000L) / FRAME_RATE

                    // Draw onto bitmap
                    VideoMotionRenderer.renderFrame(context, canvas, width, height, timeMs, config)

                    // Draw to hardware surface
                    inputSurface.makeCurrent()
                    inputSurface.drawBitmap(bitmap, width, height)
                    val ptsNanos = frame * (1_000_000_000L / FRAME_RATE)
                    inputSurface.setPresentationTime(ptsNanos)
                    inputSurface.swapBuffers()

                    // Drain video encoder
                    while (true) {
                        val status = videoCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC)
                        if (status == MediaCodec.INFO_TRY_AGAIN_LATER) {
                            break
                        } else if (status == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                            if (!muxerStarted) {
                                videoTrackIndex = muxer.addTrack(videoCodec.outputFormat)
                                // Add audio track if available
                                audioTrackIndex = setupAudioTrack(muxer)
                                muxer.start()
                                muxerStarted = true
                            }
                        } else if (status >= 0) {
                            val encodedData = videoCodec.getOutputBuffer(status)
                            if (encodedData != null && muxerStarted && (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                                if (bufferInfo.size != 0) {
                                    encodedData.position(bufferInfo.offset)
                                    encodedData.limit(bufferInfo.offset + bufferInfo.size)
                                    muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                                }
                            }
                            videoCodec.releaseOutputBuffer(status, false)
                        }
                    }

                    // Progress reporting (0% to 75% for video rendering)
                    val progress = ((frame.toFloat() / totalFrames) * 75f).toInt()
                    mainHandler.post { onProgress(progress) }
                }

                // Signal End of Video Stream
                videoCodec.signalEndOfInputStream()
                var endOfStream = false
                while (!endOfStream) {
                    val status = videoCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC)
                    if (status >= 0) {
                        val encodedData = videoCodec.getOutputBuffer(status)
                        if (encodedData != null && muxerStarted && (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                            if (bufferInfo.size != 0) {
                                encodedData.position(bufferInfo.offset)
                                encodedData.limit(bufferInfo.offset + bufferInfo.size)
                                muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                            }
                        }
                        endOfStream = (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
                        videoCodec.releaseOutputBuffer(status, false)
                    } else if (status == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        endOfStream = true
                    }
                }

                // 4. Encode & Mux Audio Track (75% to 95%)
                if (muxerStarted && audioTrackIndex >= 0 && pcmData != null && pcmData.isNotEmpty()) {
                    encodeAudioToMuxer(pcmData, muxer, audioTrackIndex) { audioProg ->
                        val combined = 75 + ((audioProg * 20) / 100)
                        mainHandler.post { onProgress(combined) }
                    }
                }

                // Finish Muxer
                if (muxerStarted) {
                    try {
                        muxer.stop()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                mainHandler.post {
                    onProgress(100)
                    onSuccess(outputFile)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                mainHandler.post { onError(e) }
            } finally {
                try { inputSurface?.release() } catch (_: Exception) {}
                try { videoCodec?.stop(); videoCodec?.release() } catch (_: Exception) {}
                try { muxer?.release() } catch (_: Exception) {}
                try { bitmap?.recycle() } catch (_: Exception) {}
            }
        }
    }

    private fun setupAudioTrack(muxer: MediaMuxer): Int {
        val audioFormat = MediaFormat.createAudioFormat(AUDIO_MIME, SAMPLE_RATE_AUDIO, 1).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE_AUDIO)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
        }
        val encoder = MediaCodec.createEncoderByType(AUDIO_MIME)
        encoder.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        encoder.start()

        val bufferInfo = MediaCodec.BufferInfo()
        var trackIndex = -1
        while (trackIndex < 0) {
            val status = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC)
            if (status == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                trackIndex = muxer.addTrack(encoder.outputFormat)
            } else if (status >= 0) {
                encoder.releaseOutputBuffer(status, false)
            } else {
                break
            }
        }
        try { encoder.stop(); encoder.release() } catch (_: Exception) {}
        return trackIndex
    }

    private fun encodeAudioToMuxer(
        pcmData: ByteArray,
        muxer: MediaMuxer,
        audioTrackIndex: Int,
        onProgress: (Int) -> Unit
    ) {
        val audioFormat = MediaFormat.createAudioFormat(AUDIO_MIME, SAMPLE_RATE_AUDIO, 1).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE_AUDIO)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
        }
        val encoder = MediaCodec.createEncoderByType(AUDIO_MIME)
        encoder.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        encoder.start()

        val bufferInfo = MediaCodec.BufferInfo()
        var pcmOffset = 0
        var presentationTimeUs = 0L
        var isEndOfInputStream = false

        while (!isEndOfInputStream || bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM == 0) {
            // Feed input
            if (!isEndOfInputStream) {
                val inputIndex = encoder.dequeueInputBuffer(TIMEOUT_USEC)
                if (inputIndex >= 0) {
                    val inputBuffer = encoder.getInputBuffer(inputIndex)
                    if (inputBuffer != null) {
                        inputBuffer.clear()
                        val bytesToRead = minOf(inputBuffer.remaining(), pcmData.size - pcmOffset)
                        if (bytesToRead > 0) {
                            inputBuffer.put(pcmData, pcmOffset, bytesToRead)
                            encoder.queueInputBuffer(inputIndex, 0, bytesToRead, presentationTimeUs, 0)
                            pcmOffset += bytesToRead
                            // Samples sent = bytes / 2 (16-bit mono)
                            val samples = bytesToRead / 2
                            presentationTimeUs += (samples * 1_000_000L) / SAMPLE_RATE_AUDIO

                            val prog = ((pcmOffset.toFloat() / pcmData.size) * 100f).toInt()
                            onProgress(prog)
                        } else {
                            encoder.queueInputBuffer(inputIndex, 0, 0, presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            isEndOfInputStream = true
                        }
                    }
                }
            }

            // Drain output
            val outputIndex = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC)
            if (outputIndex >= 0) {
                val encodedData = encoder.getOutputBuffer(outputIndex)
                if (encodedData != null && (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0 && bufferInfo.size > 0) {
                    encodedData.position(bufferInfo.offset)
                    encodedData.limit(bufferInfo.offset + bufferInfo.size)
                    muxer.writeSampleData(audioTrackIndex, encodedData, bufferInfo)
                }
                val isEOS = (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
                encoder.releaseOutputBuffer(outputIndex, false)
                if (isEOS) break
            } else if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER && isEndOfInputStream) {
                break
            }
        }

        try { encoder.stop(); encoder.release() } catch (_: Exception) {}
    }

    private fun getAudioPcmBytes(
        context: Context,
        audioPreset: String,
        customAudioUri: Uri?,
        durationSec: Int
    ): ByteArray? {
        val totalBytesNeeded = durationSec * SAMPLE_RATE_AUDIO * 2 // 16-bit mono

        // Custom Audio handling
        if (customAudioUri != null) {
            val decoded = decodeAudioUriToPcm(context, customAudioUri, totalBytesNeeded)
            if (decoded != null && decoded.isNotEmpty()) return decoded
        }

        // Mute or Unknown
        if (audioPreset == "mute") {
            return ByteArray(totalBytesNeeded) // Silence
        }

        val rawResId = when (audioPreset.lowercase()) {
            "rain" -> R.raw.ambient_rain
            "oud" -> R.raw.ambient_oud
            "nature" -> R.raw.ambient_nature
            else -> R.raw.ambient_piano
        }

        return try {
            val inputStream: InputStream = context.resources.openRawResource(rawResId)
            val fullBytes = inputStream.readBytes()
            inputStream.close()

            // Skip 44 bytes WAV header
            val pcmOnly = if (fullBytes.size > 44) fullBytes.copyOfRange(44, fullBytes.size) else fullBytes
            val output = ByteArray(totalBytesNeeded)
            var written = 0
            while (written < totalBytesNeeded) {
                val chunk = minOf(pcmOnly.size, totalBytesNeeded - written)
                System.arraycopy(pcmOnly, 0, output, written, chunk)
                written += chunk
            }
            output
        } catch (e: Exception) {
            e.printStackTrace()
            ByteArray(totalBytesNeeded)
        }
    }

    private fun decodeAudioUriToPcm(context: Context, uri: Uri, maxBytesNeeded: Int): ByteArray? {
        var extractor: MediaExtractor? = null
        var decoder: MediaCodec? = null
        return try {
            extractor = MediaExtractor().apply {
                setDataSource(context, uri, null)
            }
            var audioTrack = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                val mime = f.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrack = i
                    format = f
                    break
                }
            }
            if (audioTrack < 0 || format == null) return null

            extractor.selectTrack(audioTrack)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return null
            decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(format, null, null, 0)
            decoder.start()

            val outputStream = java.io.ByteArrayOutputStream(maxBytesNeeded)
            val bufferInfo = MediaCodec.BufferInfo()
            var isEOS = false

            while (!isEOS && outputStream.size() < maxBytesNeeded) {
                val inIndex = decoder.dequeueInputBuffer(TIMEOUT_USEC)
                if (inIndex >= 0) {
                    val inBuffer = decoder.getInputBuffer(inIndex)
                    if (inBuffer != null) {
                        val sampleSize = extractor.readSampleData(inBuffer, 0)
                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            isEOS = true
                        } else {
                            decoder.queueInputBuffer(inIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                val outIndex = decoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC)
                if (outIndex >= 0) {
                    val outBuffer = decoder.getOutputBuffer(outIndex)
                    if (outBuffer != null && bufferInfo.size > 0) {
                        val chunk = ByteArray(bufferInfo.size)
                        outBuffer.position(bufferInfo.offset)
                        outBuffer.get(chunk)
                        outputStream.write(chunk)
                    }
                    decoder.releaseOutputBuffer(outIndex, false)
                }
            }
            outputStream.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            try { decoder?.stop(); decoder?.release() } catch (_: Exception) {}
            try { extractor?.release() } catch (_: Exception) {}
        }
    }

    /**
     * حفظ الفيديو المصدَّر في المعرض (Gallery) ضمن مجلد Movies/Manchorati
     */
    fun saveVideoToGallery(context: Context, videoFile: File, title: String): Uri? {
        val fileName = "manchorati_reel_${System.currentTimeMillis()}.mp4"
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Video.Media.TITLE, title)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/Manchorati")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { os ->
                    FileInputStream(videoFile).use { `is` ->
                        `is`.copyTo(os)
                    }
                }
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            uri
        } else {
            @Suppress("DEPRECATION")
            val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            val appDir = File(moviesDir, "Manchorati").apply { mkdirs() }
            val destFile = File(appDir, fileName)
            FileInputStream(videoFile).use { `is` ->
                FileOutputStream(destFile).use { os ->
                    `is`.copyTo(os)
                }
            }
            MediaScannerConnection.scanFile(context, arrayOf(destFile.absolutePath), arrayOf("video/mp4"), null)
            Uri.fromFile(destFile)
        }
    }

    /**
     * مشاركة الفيديو مباشرة في حالة واتساب (WhatsApp Status)
     */
    fun shareVideoToWhatsApp(context: Context, videoFile: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", videoFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            `package` = "com.whatsapp"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            shareVideoGeneral(context, videoFile)
        }
    }

    /**
     * مشاركة الفيديو عبر التطبيقات (انستغرام ريلز، تيك توك، فيسبوك، تيليغرام...)
     */
    fun shareVideoGeneral(context: Context, videoFile: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", videoFile)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "مشاركة الفيديو عبر..."))
    }
}
