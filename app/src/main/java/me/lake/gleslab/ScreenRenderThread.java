package me.lake.gleslab;

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


/**
 * Created by lake on 16-4-28.
 */
public class ScreenRenderThread extends Thread {

    int mCamTextureId;
    SurfaceTexture mCamTexture;
    SurfaceTexture mDrawTexture;
    Context mContext;
    boolean quit;

    int sw, sh;
    private final Object syncThread = new Object();
    private MediaRenderThread mediaRenderThread;

    public ScreenRenderThread(Context context, int camTextureId, SurfaceTexture camTexture, SurfaceTexture drawTexture) {
        mDrawTexture = drawTexture;
        mCamTexture = camTexture;
        mCamTextureId = camTextureId;
        mContext = context;
        quit = false;
        screenWapper = new ScreenWapper();
        mediaRenderThread = new MediaRenderThread(context,camTextureId,camTexture);
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
        mediaRenderThread.setWH(w,h);
    }

    public void queue() {
        synchronized (syncThread) {
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


    int camTexId;
    private void initScreenTexture() {
        GLES20.glEnable(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
        camTexId = MainActivity.createTexture();

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

    }

    private void drawFrame() {
        GLES20.glViewport(0, 0, sw, sh);
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

    @Override
    public void run() {

        /**
         * 初始化GLES环境
         */
        initScreenGLES();
        mediaRenderThread.sharedContext= screenWapper.mEglContext;
        mediaRenderThread.start();
        currentScreen();
        initVertex();
        screenWapper.mProgram = ProgramTools.createProgram(mContext, R.raw.vertexshader, R.raw.fragmentshader_grey);
        initScreenTexture();
        Log.e("aa","spro="+screenWapper.mProgram+"loc="+screenWapper.mTextureLoc);
        while (!quit) {
            synchronized (syncThread) {
                try {
                    syncThread.wait();
                } catch (InterruptedException ignored) {
                }
            }
            long b = System.currentTimeMillis();
            currentScreen();
            long a=System.currentTimeMillis();
            mCamTexture.detachFromGLContext();
            mCamTexture.attachToGLContext(camTexId);
            mCamTexture.updateTexImage();
            Log.e("aa","tttttttt2="+mCamTexture.getTimestamp());
            Log.e("aa","SupdateTexImage="+(System.currentTimeMillis()-a));
            a=System.currentTimeMillis();
            //screen
            GLES20.glUseProgram(screenWapper.mProgram);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, camTexId);
            GLES20.glUniform1i(screenWapper.mTextureLoc, 0);
            GLES20.glEnableVertexAttribArray(screenWapper.aPostionLocation);
            GLES20.glEnableVertexAttribArray(screenWapper.aTextureCoordLocation);
            Log.e("aa","S2="+(System.currentTimeMillis()-a));
            a=System.currentTimeMillis();
            drawFrame();
            Log.e("aa","S3="+(System.currentTimeMillis()-a));
            a=System.currentTimeMillis();
            GLES20.glDisableVertexAttribArray(screenWapper.aPostionLocation);
            GLES20.glDisableVertexAttribArray(screenWapper.aTextureCoordLocation);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
            GLES20.glUseProgram(0);
            Log.e("aa","S4="+(System.currentTimeMillis()-a));
            a=System.currentTimeMillis();
            if (!EGL14.eglSwapBuffers(screenWapper.mEglDisplay, screenWapper.mEglSurface)) {
                throw new RuntimeException("eglSwapBuffers,failed!");
            }
            Log.e("aa","S5="+(System.currentTimeMillis()-a));
            a=System.currentTimeMillis();
            mCamTexture.detachFromGLContext();
            Log.e("aa","S6="+(System.currentTimeMillis()-a));
            a=System.currentTimeMillis();
            mediaRenderThread.queue();
            Log.e("aa","S7="+(System.currentTimeMillis()-a));
            a=System.currentTimeMillis();
            mediaRenderThread.waitme();
            Log.e("aa","S8="+(System.currentTimeMillis()-a));
            a=System.currentTimeMillis();
            mCamTexture.attachToGLContext(camTexId);
            Log.e("aa","S9="+(System.currentTimeMillis()-a));
            Log.e("aa", "drawFrame");
            Log.e("bb","bb="+(System.currentTimeMillis()-b));
        }
        mediaRenderThread.quit();
    }

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
            0.0f, 1.0f,
            1.0f, 1.0f,
            1.0f, 0.0f,
            0.0f, 0.0f
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

}
