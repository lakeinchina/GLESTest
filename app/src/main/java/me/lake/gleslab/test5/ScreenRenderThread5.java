package me.lake.gleslab.test5;

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

import me.lake.gleslab.ProgramTools;
import me.lake.gleslab.R;


/**
 * Created by lake on 16-4-28.
 */
public class ScreenRenderThread5 extends Thread {

    int mCamTextureId;
    SurfaceTexture mCamTexture;
    SurfaceTexture mDrawTexture;
    Surface mediaInputSurface;
    Context mContext;
    boolean quit;

    int sw, sh;
    private final Object syncThread = new Object();
    MediaCodecCore mediaCodecCore;

    public ScreenRenderThread5(Context context, int camTextureId, SurfaceTexture camTexture, SurfaceTexture drawTexture) {
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
        mTextureVerticesBuffer = ByteBuffer.allocateDirect(FLOAT_SIZE_BYTES * textureVertices.length).
                order(ByteOrder.nativeOrder()).
                asFloatBuffer();
        mTextureVerticesBuffer.put(textureVertices);
        mTextureVerticesBuffer.position(0);
        mDrawIndicesBuffer = ByteBuffer.allocateDirect(SHORT_SIZE_BYTES * drawIndices.length).
                order(ByteOrder.nativeOrder()).
                asShortBuffer();
        mDrawIndicesBuffer.put(drawIndices);
        mDrawIndicesBuffer.position(0);
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


    int camSTexId;
    int camMTexId;

    private void initScreenTexture() {
        GLES20.glEnable(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
        camSTexId = MainActivity5.createTexture();

        GLES20.glUseProgram(screenWapper.mProgram);
        screenWapper.mTextureLoc = GLES20.glGetUniformLocation(screenWapper.mProgram, "uTexture");

        screenWapper.aPostionLocation = GLES20.glGetAttribLocation(screenWapper.mProgram, "aPosition");
        screenWapper.aTextureCoordLocation = GLES20.glGetAttribLocation(screenWapper.mProgram, "aTextureCoord");
        GLES20.glEnableVertexAttribArray(screenWapper.aPostionLocation);
        GLES20.glVertexAttribPointer(screenWapper.aPostionLocation, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                COORDS_PER_VERTEX * 4, mSquareVerticesBuffer);
        GLES20.glEnableVertexAttribArray(screenWapper.aTextureCoordLocation);
        GLES20.glVertexAttribPointer(screenWapper.aTextureCoordLocation, TEXTURE_COORS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                TEXTURE_COORS_PER_VERTEX * 4, mTextureVerticesBuffer);
        int muTexelWidthOffset = GLES20.glGetUniformLocation(screenWapper.mProgram, "uTexelWidthOffset");
        int muTexelHeightOffset = GLES20.glGetUniformLocation(screenWapper.mProgram, "uTexelHeightOffset");
            GLES20.glUniform1f(muTexelWidthOffset,
                    (float) (20.0/sw));
            GLES20.glUniform1f(muTexelHeightOffset,
                    (float) (20.0/sh));
    }

    private void initMediaTexture() {
        GLES20.glEnable(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
        camMTexId = MainActivity5.createTexture();

        GLES20.glUseProgram(mediaWapper.mProgram);
        mediaWapper.mTextureLoc = GLES20.glGetUniformLocation(mediaWapper.mProgram, "uTexture");

        mediaWapper.aPostionLocation = GLES20.glGetAttribLocation(mediaWapper.mProgram, "aPosition");
        mediaWapper.aTextureCoordLocation = GLES20.glGetAttribLocation(mediaWapper.mProgram, "aTextureCoord");
        GLES20.glEnableVertexAttribArray(mediaWapper.aPostionLocation);
        GLES20.glVertexAttribPointer(mediaWapper.aPostionLocation, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                COORDS_PER_VERTEX * 4, mSquareVerticesBuffer);
        GLES20.glEnableVertexAttribArray(mediaWapper.aTextureCoordLocation);
        GLES20.glVertexAttribPointer(mediaWapper.aTextureCoordLocation, TEXTURE_COORS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                TEXTURE_COORS_PER_VERTEX * 4, mTextureVerticesBuffer);

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
        screenWapper.mProgram = ProgramTools.createProgram(mContext, R.raw.vertexshader_face, R.raw.fragmentshader_face);
        initScreenTexture();
        currentMedia();
        mediaWapper.mProgram = ProgramTools.createProgram(mContext, R.raw.vertexshader_face, R.raw.fragmentshader_face);
        initMediaTexture();
        currentScreen();
        mCamTexture.attachToGLContext(camSTexId);
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
            //screen
            GLES20.glUseProgram(screenWapper.mProgram);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, camSTexId);
            GLES20.glUniform1i(screenWapper.mTextureLoc, 0);
            GLES20.glEnableVertexAttribArray(screenWapper.aPostionLocation);
            GLES20.glEnableVertexAttribArray(screenWapper.aTextureCoordLocation);
            GLES20.glViewport(0, 0, sw, sh);
            long a=System.currentTimeMillis();
            drawFrame();
            Log.e("aa","drawtime="+(System.currentTimeMillis()-a));
//            mCamTexture.detachFromGLContext();
            GLES20.glDisableVertexAttribArray(screenWapper.aPostionLocation);
            GLES20.glDisableVertexAttribArray(screenWapper.aTextureCoordLocation);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
            GLES20.glUseProgram(0);
            if (!EGL14.eglSwapBuffers(screenWapper.mEglDisplay, screenWapper.mEglSurface)) {
                throw new RuntimeException("eglSwapBuffers,failed!");
            }
            //media
            currentMedia();
//            mCamTexture.attachToGLContext(camSTexId);
//            mCamTexture.updateTexImage();
            GLES20.glUseProgram(mediaWapper.mProgram);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, camSTexId);
            GLES20.glUniform1i(mediaWapper.mTextureLoc, 0);
            GLES20.glEnableVertexAttribArray(mediaWapper.aPostionLocation);
            GLES20.glEnableVertexAttribArray(mediaWapper.aTextureCoordLocation);
            drawFrame();
            GLES20.glDisableVertexAttribArray(mediaWapper.aPostionLocation);
            GLES20.glDisableVertexAttribArray(mediaWapper.aTextureCoordLocation);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
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

    long t;

    //形状顶点
    private FloatBuffer mSquareVerticesBuffer;
    private static float squareVertices[] = {
            -1.0f, 1.0f, 0.0f,
            -1.0f, -1.0f, 0.0f,
            1.0f, -1.0f, 0.0f,
            1.0f, 1.0f, 0.0f
    };
    //纹理对应顶点
    private FloatBuffer mTextureVerticesBuffer;
    private static float textureVertices[] = {
            1.0f, 0.0f,
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f
    };
    //绘制顶点顺序
    private ShortBuffer mDrawIndicesBuffer;
    private static short drawIndices[] = {0, 1, 2, 0, 2, 3};

    private static int FLOAT_SIZE_BYTES = 4;
    private static int SHORT_SIZE_BYTES = 2;
    private static final int COORDS_PER_VERTEX = 3;
    private static final int TEXTURE_COORS_PER_VERTEX = 2;

    private ScreenWapper screenWapper;

    public class ScreenWapper {
        EGLDisplay mEglDisplay;
        EGLConfig mEglConfig;
        EGLSurface mEglSurface;
        EGLContext mEglContext;
        int mProgram;
        int mTextureLoc;
        int aPostionLocation;
        int aTextureCoordLocation;
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
