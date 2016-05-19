package me.lake.gleslab.test6;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;
import android.view.Surface;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import me.lake.gleslab.GlUtil;
import me.lake.gleslab.ProgramTools;
import me.lake.gleslab.R;


/**
 * Created by lake on 16-4-28.
 */
public class ScreenRenderThread6 extends Thread {
    int width = 1280, height = 720;

    int mCamTextureId;
    SurfaceTexture mCamTexture;
    SurfaceTexture mDrawTexture;
    Surface mediaInputSurface;
    Context mContext;
    boolean quit;

    int sw, sh;
    private final Object syncThread = new Object();
    MediaCodecCore mediaCodecCore;

    public ScreenRenderThread6(Context context, int camTextureId, SurfaceTexture camTexture, SurfaceTexture drawTexture) {
        mDrawTexture = drawTexture;
        mCamTexture = camTexture;
        mCamTextureId = camTextureId;
        mContext = context;
        quit = false;
        screenWapper = new ScreenWapper();
        mediaWapper = new MediaWapper();
        mediaCodecCore = new MediaCodecCore();
        mediaInputSurface = mediaCodecCore.init();
        mediaWapper = new MediaWapper();
    }

    public void quit() {
        quit = true;
        synchronized (syncThread) {
            syncThread.notify();
        }
    }

    public void setWH(int w, int h) {
        sw = w;
        sh = h;
    }

    int frameNum = 0;

    public void queue() {
        synchronized (syncThread) {
            frameNum++;
            syncThread.notify();
        }
    }


    private void initScreenGLES() {
        screenWapper.mEglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (EGL14.EGL_NO_DISPLAY == screenWapper.mEglDisplay) {
            throw new RuntimeException("eglGetDisplay,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
        }
        int versions[] = new int[2];
        if (!EGL14.eglInitialize(screenWapper.mEglDisplay, versions, 0, versions, 1)) {
            throw new RuntimeException("eglInitialize,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
        }
        int configsCount[] = new int[1];
        EGLConfig configs[] = new EGLConfig[1];
        int configSpec[] = new int[]{
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_DEPTH_SIZE, 0,
                EGL14.EGL_STENCIL_SIZE, 0,
                EGL14.EGL_NONE
        };
        EGL14.eglChooseConfig(screenWapper.mEglDisplay, configSpec, 0, configs, 0, 1, configsCount, 0);
        if (configsCount[0] <= 0) {
            throw new RuntimeException("eglChooseConfig,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
        }
        screenWapper.mEglConfig = configs[0];
        int[] surfaceAttribs = {
                EGL14.EGL_NONE
        };
        int contextSpec[] = new int[]{
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
        };
        screenWapper.mEglContext = EGL14.eglCreateContext(screenWapper.mEglDisplay, screenWapper.mEglConfig, EGL14.EGL_NO_CONTEXT, contextSpec, 0);
        if (EGL14.EGL_NO_CONTEXT == screenWapper.mEglContext) {
            throw new RuntimeException("eglCreateContext,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
        }
        int[] values = new int[1];
        EGL14.eglQueryContext(screenWapper.mEglDisplay, screenWapper.mEglContext, EGL14.EGL_CONTEXT_CLIENT_VERSION, values, 0);
        Log.d("AA", "screenWapper,EGLContext created, client version " + values[0]);
        screenWapper.mEglSurface = EGL14.eglCreateWindowSurface(screenWapper.mEglDisplay, screenWapper.mEglConfig, mDrawTexture, surfaceAttribs, 0);
        if (null == screenWapper.mEglSurface || EGL14.EGL_NO_SURFACE == screenWapper.mEglSurface) {
            throw new RuntimeException("eglCreateWindowSurface,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
        }
    }

    private void initMediaGLES() {
        mediaWapper.mEglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (EGL14.EGL_NO_DISPLAY == mediaWapper.mEglDisplay) {
            throw new RuntimeException("eglGetDisplay,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
        }
        int versions[] = new int[2];
        if (!EGL14.eglInitialize(mediaWapper.mEglDisplay, versions, 0, versions, 1)) {
            throw new RuntimeException("eglInitialize,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
        }
        int configsCount[] = new int[1];
        EGLConfig configs[] = new EGLConfig[1];
        int configSpec[] = new int[]{
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                0x3142, 1,
                EGL14.EGL_DEPTH_SIZE, 0,
                EGL14.EGL_STENCIL_SIZE, 0,
                EGL14.EGL_NONE
        };
        EGL14.eglChooseConfig(mediaWapper.mEglDisplay, configSpec, 0, configs, 0, 1, configsCount, 0);
        if (configsCount[0] <= 0) {
            throw new RuntimeException("eglChooseConfig,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
        }
        mediaWapper.mEglConfig = configs[0];
        int[] surfaceAttribs = {
                EGL14.EGL_NONE
        };
        int contextSpec[] = new int[]{
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
        };
        mediaWapper.mEglContext = EGL14.eglCreateContext(mediaWapper.mEglDisplay, mediaWapper.mEglConfig, screenWapper.mEglContext, contextSpec, 0);
        if (EGL14.EGL_NO_CONTEXT == mediaWapper.mEglContext) {
            throw new RuntimeException("eglCreateContext,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
        }
        int[] values = new int[1];
        EGL14.eglQueryContext(mediaWapper.mEglDisplay, mediaWapper.mEglContext, EGL14.EGL_CONTEXT_CLIENT_VERSION, values, 0);
        Log.d("AA", "mediaWapper,EGLContext created, client version " + values[0]);
        mediaWapper.mEglSurface = EGL14.eglCreateWindowSurface(mediaWapper.mEglDisplay, mediaWapper.mEglConfig, mediaInputSurface, surfaceAttribs, 0);
        if (null == mediaWapper.mEglSurface || EGL14.EGL_NO_SURFACE == mediaWapper.mEglSurface) {
            throw new RuntimeException("eglCreateWindowSurface,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
        }
    }

    private void initVertex() {
        mSquareVerticesBuffer = ByteBuffer.allocateDirect(FLOAT_SIZE_BYTES * squareVertices.length).
                order(ByteOrder.nativeOrder()).
                asFloatBuffer();
        mSquareVerticesBuffer.put(squareVertices);
        mSquareVerticesBuffer.position(0);
        mCamTextureVerticesBuffer = ByteBuffer.allocateDirect(FLOAT_SIZE_BYTES * camTextureVertices.length).
                order(ByteOrder.nativeOrder()).
                asFloatBuffer();
        mCamTextureVerticesBuffer.put(camTextureVertices);
        mCamTextureVerticesBuffer.position(0);
        mDrawIndicesBuffer = ByteBuffer.allocateDirect(SHORT_SIZE_BYTES * drawIndices.length).
                order(ByteOrder.nativeOrder()).
                asShortBuffer();
        mDrawIndicesBuffer.put(drawIndices);
        mDrawIndicesBuffer.position(0);

        mScreenTextureVerticesBuffer = ByteBuffer.allocateDirect(FLOAT_SIZE_BYTES * screenTextureVertices.length).
                order(ByteOrder.nativeOrder()).
                asFloatBuffer();
        mScreenTextureVerticesBuffer.put(screenTextureVertices);
        mScreenTextureVerticesBuffer.position(0);
        mMediaTextureVerticesBuffer = ByteBuffer.allocateDirect(FLOAT_SIZE_BYTES * mediaTextureVertices.length).
                order(ByteOrder.nativeOrder()).
                asFloatBuffer();
        mMediaTextureVerticesBuffer.put(mediaTextureVertices);
        mMediaTextureVerticesBuffer.position(0);
    }

    private void createTexture(int width, int height, int format, int[] texture) {
        GLES20.glGenTextures(1, texture, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, format, width, height, 0, format, GLES20.GL_UNSIGNED_BYTE, null);
    }

    private void initCamTexture() {
        GLES20.glEnable(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);

        GLES20.glUseProgram(screenWapper.mCamProgram);
        screenWapper.mCamTextureLoc = GLES20.glGetUniformLocation(screenWapper.mCamProgram, "uTexture");
        screenWapper.aCamPostionLocation = GLES20.glGetAttribLocation(screenWapper.mCamProgram, "aPosition");
        screenWapper.aCamTextureCoordLocation = GLES20.glGetAttribLocation(screenWapper.mCamProgram, "aTextureCoord");
        GLES20.glEnableVertexAttribArray(screenWapper.aCamPostionLocation);
        mSquareVerticesBuffer.position(0);
        GLES20.glVertexAttribPointer(screenWapper.aPostionLocation, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                COORDS_PER_VERTEX * 4, mSquareVerticesBuffer);
        GLES20.glEnableVertexAttribArray(screenWapper.aCamTextureCoordLocation);
        GLES20.glVertexAttribPointer(screenWapper.aCamTextureCoordLocation, TEXTURE_COORS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                TEXTURE_COORS_PER_VERTEX * 4, mCamTextureVerticesBuffer);
        GLES20.glDisableVertexAttribArray(screenWapper.aCamPostionLocation);
        GLES20.glDisableVertexAttribArray(screenWapper.aCamTextureCoordLocation);
    }

    private void initScreenTexture() {
        GLES20.glEnable(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);

        GLES20.glUseProgram(screenWapper.mProgram);
        screenWapper.mTextureLoc = GLES20.glGetUniformLocation(screenWapper.mProgram, "uTexture");

        screenWapper.aPostionLocation = GLES20.glGetAttribLocation(screenWapper.mProgram, "aPosition");
        screenWapper.aTextureCoordLocation = GLES20.glGetAttribLocation(screenWapper.mProgram, "aTextureCoord");
        GLES20.glEnableVertexAttribArray(screenWapper.aPostionLocation);
        mSquareVerticesBuffer.position(0);
        GLES20.glVertexAttribPointer(screenWapper.aPostionLocation, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                COORDS_PER_VERTEX * 4, mSquareVerticesBuffer);
        GLES20.glEnableVertexAttribArray(screenWapper.aTextureCoordLocation);
        GLES20.glVertexAttribPointer(screenWapper.aTextureCoordLocation, TEXTURE_COORS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                TEXTURE_COORS_PER_VERTEX * 4, mScreenTextureVerticesBuffer);
        GLES20.glDisableVertexAttribArray(screenWapper.aPostionLocation);
        GLES20.glDisableVertexAttribArray(screenWapper.aTextureCoordLocation);
    }

    private void initMediaTexture() {
        GLES20.glEnable(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);

        GLES20.glUseProgram(mediaWapper.mProgram);
        mediaWapper.mTextureLoc = GLES20.glGetUniformLocation(mediaWapper.mProgram, "uTexture");

        mediaWapper.aPostionLocation = GLES20.glGetAttribLocation(mediaWapper.mProgram, "aPosition");
        mediaWapper.aTextureCoordLocation = GLES20.glGetAttribLocation(mediaWapper.mProgram, "aTextureCoord");
        mSquareVerticesBuffer.position(0);
        GLES20.glEnableVertexAttribArray(mediaWapper.aPostionLocation);
        GLES20.glVertexAttribPointer(mediaWapper.aPostionLocation, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                COORDS_PER_VERTEX * 4, mSquareVerticesBuffer);
        GLES20.glEnableVertexAttribArray(mediaWapper.aTextureCoordLocation);
        GLES20.glVertexAttribPointer(mediaWapper.aTextureCoordLocation, TEXTURE_COORS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                TEXTURE_COORS_PER_VERTEX * 4, mMediaTextureVerticesBuffer);
        GLES20.glDisableVertexAttribArray(mediaWapper.aPostionLocation);
        GLES20.glDisableVertexAttribArray(mediaWapper.aTextureCoordLocation);
    }

    private void drawFrame() {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        //=================================
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawIndices.length, GLES20.GL_UNSIGNED_SHORT, mDrawIndicesBuffer);
        GLES20.glFinish();
    }

    private void currentScreen() {
        if (!EGL14.eglMakeCurrent(screenWapper.mEglDisplay, screenWapper.mEglSurface, screenWapper.mEglSurface, screenWapper.mEglContext)) {
            throw new RuntimeException("eglMakeCurrent,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
        }
    }

    private void currentMedia() {
        if (!EGL14.eglMakeCurrent(mediaWapper.mEglDisplay, mediaWapper.mEglSurface, mediaWapper.mEglSurface, mediaWapper.mEglContext)) {
            throw new RuntimeException("eglMakeCurrent,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
        }
    }

    private int framebuffer;
    private int frameBufferTexture;

    private void initFrameBuffer() {
        int[] mFrameBuffer;
        int[] mFrameBufferTexture;
        mFrameBuffer = new int[1];
        mFrameBufferTexture = new int[1];
        GLES20.glGenFramebuffers(1, mFrameBuffer, 0);
        GLES20.glGenTextures(1, mFrameBufferTexture, 0);
        framebuffer = mFrameBuffer[0];
        frameBufferTexture = mFrameBufferTexture[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, frameBufferTexture);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebuffer);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, frameBufferTexture, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GlUtil.checkGlError("initFrameBuffer");
    }

    int camTex;

    @Override
    public void run() {
        mediaCodecCore.start();
        /**
         * 初始化GLES环境
         */
        initScreenGLES();
        initMediaGLES();
        currentScreen();
        initVertex();
        initFrameBuffer();
        screenWapper.mCamProgram = ProgramTools.createProgram(mContext, R.raw.vertexshader, R.raw.fragmentshader);
        screenWapper.mProgram = ProgramTools.createProgram(mContext, R.raw.vertexshader_y, R.raw.fragmentshader_2d);
        initCamTexture();
        initScreenTexture();
        currentMedia();
        mediaWapper.mProgram = ProgramTools.createProgram(mContext, R.raw.vertexshader_y, R.raw.fragmentshader_2d);
        initMediaTexture();
//        GLES20.glFrontFace(GLES20.GL_CW);
//        GLES20.glCullFace(GLES20.GL_BACK);
//        GLES20.glEnable(GLES20.GL_CULL_FACE);
        currentScreen();
        camTex = MainActivity6.createTexture();
        mCamTexture.attachToGLContext(camTex);
//        GLES20.glFrontFace(GLES20.GL_CW);
//        GLES20.glCullFace(GLES20.GL_BACK);
//        GLES20.glEnable(GLES20.GL_CULL_FACE);
        while (!quit) {
            synchronized (syncThread) {
                if (frameNum == 0) {
                    try {
                        syncThread.wait();
                    } catch (InterruptedException ignored) {
                    }
                } else if (frameNum >= 2) {
//                    mCamTexture.attachToGLContext(camSTexId);
                    for (int i = 0; i < (frameNum - 1); i++) {
                        mCamTexture.updateTexImage();
                    }
//                    mCamTexture.detachFromGLContext();
                    --frameNum;
                }
            }
            long t = System.currentTimeMillis();
            currentScreen();
            mCamTexture.updateTexImage();
            //cam
            GLES20.glUseProgram(screenWapper.mCamProgram);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebuffer);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, camTex);
            GLES20.glUniform1i(screenWapper.mCamTextureLoc, 0);
            GLES20.glEnableVertexAttribArray(screenWapper.aCamPostionLocation);
            GLES20.glEnableVertexAttribArray(screenWapper.aCamTextureCoordLocation);
            mSquareVerticesBuffer.position(0);
            GLES20.glVertexAttribPointer(screenWapper.aCamPostionLocation, COORDS_PER_VERTEX,
                    GLES20.GL_FLOAT, false,
                    COORDS_PER_VERTEX * 4, mSquareVerticesBuffer);
            mCamTextureVerticesBuffer.position(0);
            GLES20.glVertexAttribPointer(screenWapper.aCamTextureCoordLocation, TEXTURE_COORS_PER_VERTEX,
                    GLES20.GL_FLOAT, false,
                    TEXTURE_COORS_PER_VERTEX * 4, mCamTextureVerticesBuffer);
            GLES20.glViewport(0, 0, width, height);
            long a = System.currentTimeMillis();
            drawFrame();
            Log.e("aa", "drawcam=" + (System.currentTimeMillis() - a));
            GLES20.glDisableVertexAttribArray(screenWapper.aCamPostionLocation);
            GLES20.glDisableVertexAttribArray(screenWapper.aCamTextureCoordLocation);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
            GLES20.glUseProgram(0);
            //screen
            GLES20.glUseProgram(screenWapper.mProgram);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, frameBufferTexture);
            GLES20.glUniform1i(screenWapper.mTextureLoc, 0);
            GLES20.glEnableVertexAttribArray(screenWapper.aPostionLocation);
            GLES20.glEnableVertexAttribArray(screenWapper.aTextureCoordLocation);
            mSquareVerticesBuffer.position(0);
            GLES20.glVertexAttribPointer(screenWapper.aPostionLocation, COORDS_PER_VERTEX,
                    GLES20.GL_FLOAT, false,
                    COORDS_PER_VERTEX * 4, mSquareVerticesBuffer);
            mScreenTextureVerticesBuffer.position(0);
            GLES20.glVertexAttribPointer(screenWapper.aTextureCoordLocation, TEXTURE_COORS_PER_VERTEX,
                    GLES20.GL_FLOAT, false,
                    TEXTURE_COORS_PER_VERTEX * 4, mScreenTextureVerticesBuffer);
            GLES20.glViewport(0, 0, sw, sh);
            a = System.currentTimeMillis();
            drawFrame();
            Log.e("aa", "drawscreen=" + (System.currentTimeMillis() - a));
            GLES20.glDisableVertexAttribArray(screenWapper.aPostionLocation);
            GLES20.glDisableVertexAttribArray(screenWapper.aTextureCoordLocation);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
            GLES20.glUseProgram(0);
            if (!EGL14.eglSwapBuffers(screenWapper.mEglDisplay, screenWapper.mEglSurface)) {
                throw new RuntimeException("eglSwapBuffers,failed!");
            }
            //media
            currentMedia();
            GLES20.glUseProgram(mediaWapper.mProgram);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, frameBufferTexture);
            GLES20.glUniform1i(mediaWapper.mTextureLoc, 0);
            GLES20.glEnableVertexAttribArray(mediaWapper.aPostionLocation);
            GLES20.glEnableVertexAttribArray(mediaWapper.aTextureCoordLocation);
            mSquareVerticesBuffer.position(0);
            GLES20.glVertexAttribPointer(mediaWapper.aPostionLocation, COORDS_PER_VERTEX,
                    GLES20.GL_FLOAT, false,
                    COORDS_PER_VERTEX * 4, mSquareVerticesBuffer);
            mMediaTextureVerticesBuffer.position(0);
            GLES20.glVertexAttribPointer(mediaWapper.aTextureCoordLocation, TEXTURE_COORS_PER_VERTEX,
                    GLES20.GL_FLOAT, false,
                    TEXTURE_COORS_PER_VERTEX * 4, mMediaTextureVerticesBuffer);
            a = System.currentTimeMillis();
            drawFrame();
            Log.e("aa", "drawmedia=" + (System.currentTimeMillis() - a));
            GLES20.glDisableVertexAttribArray(mediaWapper.aPostionLocation);
            GLES20.glDisableVertexAttribArray(mediaWapper.aTextureCoordLocation);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
            GLES20.glUseProgram(0);
            EGLExt.eglPresentationTimeANDROID(mediaWapper.mEglDisplay, mediaWapper.mEglSurface, mCamTexture.getTimestamp());
            if (!EGL14.eglSwapBuffers(mediaWapper.mEglDisplay, mediaWapper.mEglSurface)) {
                throw new RuntimeException("eglSwapBuffers,failed!");
            }
            synchronized (syncThread) {
                frameNum--;
            }
            Log.e("aa", "sdrawFrame=" + (System.currentTimeMillis() - t));
        }
        currentScreen();
        mCamTexture.detachFromGLContext();
        mediaCodecCore.stop();
    }


    //形状顶点
    private FloatBuffer mSquareVerticesBuffer;
    private static float squareVertices[] = {
            -1.0f, 1.0f,
            -1.0f, -1.0f,
            1.0f, -1.0f,
            1.0f, 1.0f
    };
    //纹理对应顶点
    private FloatBuffer mScreenTextureVerticesBuffer;
    private static float screenTextureVertices[] = {
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f
    };
    private FloatBuffer mMediaTextureVerticesBuffer;
    private static float mediaTextureVertices[] = {
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f
    };
    private FloatBuffer mCamTextureVerticesBuffer;
    private static float camTextureVertices[] = {
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f,
            1.0f, 0.0f
    };
    //绘制顶点顺序
    private ShortBuffer mDrawIndicesBuffer;
    private static short drawIndices[] = {0, 1, 2, 0, 2, 3};

    private static int FLOAT_SIZE_BYTES = 4;
    private static int SHORT_SIZE_BYTES = 2;
    private static final int COORDS_PER_VERTEX = 2;
    private static final int TEXTURE_COORS_PER_VERTEX = 2;

    private ScreenWapper screenWapper;

    public class ScreenWapper {
        EGLDisplay mEglDisplay;
        EGLConfig mEglConfig;
        EGLSurface mEglSurface;
        EGLContext mEglContext;
        int mProgram;
        int mCamProgram;
        int mTextureLoc;
        int aPostionLocation;
        int aTextureCoordLocation;

        int mCamTextureLoc;
        int aCamPostionLocation;
        int aCamTextureCoordLocation;
    }

    private MediaWapper mediaWapper;

    public class MediaWapper {
        EGLDisplay mEglDisplay;
        EGLConfig mEglConfig;
        EGLSurface mEglSurface;
        EGLContext mEglContext;
        int mProgram;
        int mTextureLoc;
        int aPostionLocation;
        int aTextureCoordLocation;
    }

}
