package com.manchorati.www

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.File
import java.nio.ByteBuffer

/**
 * VideoTrimmerHelper: أداة لقص مقاطع الفيديو بدون إعادة ترميز (Lossless Stream Copy) وبسرعة فائقة.
 * تستخدم MediaExtractor و MediaMuxer لاستخراج الجزء المحدد بالثواني وحفظه كملف مستقل بحجم صغير جداً،
 * ثم حذف الفيديو الأصلي الضخم لحماية مساحة الهاتف ومنع بطء التطبيق.
 */
object VideoTrimmerHelper {

    private const val TAG = "VideoTrimmerHelper"

    /**
     * يقص مقطع من sourcePath بين startSec و endSec ويحفظه في destPath.
     * يعمل بشكل سريع جداً وبدون ضياع في جودة الفيديو.
     */
    fun trimVideo(
        sourcePath: String,
        destPath: String,
        startSec: Float,
        endSec: Float
    ): Boolean {
        var extractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null
        return try {
            val sourceFile = File(sourcePath)
            if (!sourceFile.exists() || sourceFile.length() <= 0) return false

            extractor = MediaExtractor().apply {
                setDataSource(sourcePath)
            }

            val trackCount = extractor.trackCount
            if (trackCount <= 0) return false

            muxer = MediaMuxer(destPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val indexMap = HashMap<Int, Int>()

            var hasVideo = false
            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                    val muxerTrack = muxer.addTrack(format)
                    indexMap[i] = muxerTrack
                    if (mime.startsWith("video/")) hasVideo = true
                }
            }

            if (!hasVideo) return false

            muxer.start()

            val startUs = (startSec * 1_000_000L).toLong().coerceAtLeast(0L)
            val endUs = (endSec * 1_000_000L).toLong()

            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            val maxBufferSize = 2 * 1024 * 1024 // 2MB buffer
            val buffer = ByteBuffer.allocateDirect(maxBufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            while (true) {
                val trackIndex = extractor.sampleTrackIndex
                if (trackIndex < 0) break

                val sampleTime = extractor.sampleTime
                if (sampleTime > endUs) break

                val muxerTrack = indexMap[trackIndex]
                if (muxerTrack != null) {
                    bufferInfo.offset = 0
                    bufferInfo.size = extractor.readSampleData(buffer, 0)
                    if (bufferInfo.size < 0) break

                    bufferInfo.presentationTimeUs = maxOf(0L, sampleTime - startUs)
                    bufferInfo.flags = extractor.sampleFlags
                    muxer.writeSampleData(muxerTrack, buffer, bufferInfo)
                }
                extractor.advance()
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error trimming video", e)
            false
        } finally {
            try {
                muxer?.stop()
                muxer?.release()
            } catch (_: Exception) {}
            try {
                extractor?.release()
            } catch (_: Exception) {}
        }
    }

    /**
     * ينظف الملفات المؤقتة القديمة في مجلد studio_custom_vbg لحماية ذاكرة الهاتف.
     */
    fun cleanOldCache(context: Context, keepFilePath: String? = null) {
        try {
            val cacheDir = File(context.cacheDir, "studio_custom_vbg")
            if (cacheDir.exists() && cacheDir.isDirectory) {
                cacheDir.listFiles()?.forEach { file ->
                    if (keepFilePath == null || file.absolutePath != keepFilePath) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error cleaning cache", e)
        }
    }
}
