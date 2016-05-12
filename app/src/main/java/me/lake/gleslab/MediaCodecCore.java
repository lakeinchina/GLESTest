package me.lake.gleslab;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by lake on 16-5-10.
 */
public class MediaCodecCore {
    private static final long WAIT_TIME = 10000;//10ms;
    MediaCodec mediaEncoder;
    MediaMuxer mediaMuxer;
    private Surface inputSurface;
    private Thread workThread;

    public MediaCodecCore() {
    }

    public Surface init() {
        MediaFormat videoFormat = MediaFormat.createVideoFormat("video/avc", 1280, 720);
        videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, 2000000);
        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5);
        try {
            mediaEncoder = MediaCodec.createEncoderByType(videoFormat.getString(MediaFormat.KEY_MIME));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        mediaEncoder.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        inputSurface = mediaEncoder.createInputSurface();
        try {
            mediaMuxer = new MediaMuxer("/sdcard/out.mp4", MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return inputSurface;
    }

    boolean started = true;
    int videoindex;

    public void start() {
        started= true;
        mediaEncoder.start();
        workThread = new Thread() {
            private ByteBuffer[] dstVideoEncoderOBuffers;

            @Override
            public void run() {
                while (started) {
                    dstVideoEncoderOBuffers = mediaEncoder.getOutputBuffers();
                    MediaCodec.BufferInfo einfo = new MediaCodec.BufferInfo();
                    int eobIndex = mediaEncoder.dequeueOutputBuffer(einfo, WAIT_TIME);
                    switch (eobIndex) {
                        case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                            Log.e("aa", "dstVideoEncoder,INFO_OUTPUT_BUFFERS_CHANGED");
                            dstVideoEncoderOBuffers = mediaEncoder.getOutputBuffers();
                            break;
                        case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                            Log.e("aa", "dstVideoEncoder,INFO_OUTPUT_FORMAT_CHANGED again!");
                            videoindex = mediaMuxer.addTrack(mediaEncoder.getOutputFormat());
                            mediaMuxer.start();
                            break;
                        case MediaCodec.INFO_TRY_AGAIN_LATER:
                            Log.e("aa", "dstVideoEncoder,INFO_TRY_AGAIN_LATER");
                            break;
                        default:
                            Log.e("aa", "getframe,size=" + einfo.size + ";time=" + einfo.presentationTimeUs + ";flag=" + einfo.flags + ";offset=" + einfo.offset);
                            ByteBuffer encodedBuff = dstVideoEncoderOBuffers[eobIndex];
                            if (einfo.size != 0) {
                                encodedBuff.position(einfo.offset);
                                encodedBuff.limit(einfo.offset + einfo.size);
                                mediaMuxer.writeSampleData(videoindex, encodedBuff, einfo);
                            }
                            mediaEncoder.releaseOutputBuffer(eobIndex, false);
                            break;
                    }
                }
                mediaEncoder.stop();
                mediaEncoder.release();
                mediaMuxer.stop();
                mediaMuxer.release();
            }
        };
        workThread.start();
    }

    public void stop() {
        started = false;
    }
}