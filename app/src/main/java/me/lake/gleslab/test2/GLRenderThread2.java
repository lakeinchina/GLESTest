package me.lake.gleslab.test2;
import android.content.Context;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;
import android.view.Surface;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

import me.lake.gleslab.HomeActivity;
import me.lake.gleslab.ProgramTools;
import me.lake.gleslab.R;

/**
 * Created by lake on 16-5-16.
 */
public class GLRenderThread2 extends Thread {
    EGL10 mEgl;
    EGLDisplay mEglDisplay;
    EGLConfig mEglConfig;
    EGLSurface mEglSurface;
    EGLContext mEglContext;
    int mProgram;


    Context mContext;
    boolean quit;
    int mWidth = 1280;
    int mHeight = 720;
    int ySize = mWidth * mHeight;
    int size = ySize + (ySize >> 1);

    int sw, sh;
    private final Object syncThread = new Object();
    private Surface mSurface;
    private byte[] yuvpix = new byte[size];

    public GLRenderThread2(Surface surface, Context context) {
        mContext = context;
        mSurface = surface;
        quit = false;
        yBuf = ByteBuffer.allocateDirect(ySize);
        uBuf = ByteBuffer.allocateDirect(ySize >> 2);
        vBuf = ByteBuffer.allocateDirect(ySize >> 2);
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
        Log.e("aa", "sw=" + sw + "sh" + sh);
    }

    public void queueYUV(byte[] y, byte[] u, byte[] v) {
        synchronized (syncBuff) {
            yBuf.position(0);
            yBuf.put(y).position(0);
            uBuf.position(0);
            uBuf.put(u).position(0);
            vBuf.position(0);
            vBuf.put(v).position(0);
        }
        synchronized (syncThread) {
            syncThread.notify();
        }
    }

    private void initGLES() {
        mEgl = (EGL10) EGLContext.getEGL();
        mEglDisplay = mEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        if (EGL10.EGL_NO_DISPLAY == mEglDisplay) {
            throw new RuntimeException("eglGetDisplay,failed:" + GLUtils.getEGLErrorString(mEgl.eglGetError()));
        }
        int versions[] = new int[2];
        if (!mEgl.eglInitialize(mEglDisplay, versions)) {
            throw new RuntimeException("eglInitialize,failed:" + GLUtils.getEGLErrorString(mEgl.eglGetError()));
        }
        int configsCount[] = new int[1];
        EGLConfig configs[] = new EGLConfig[1];
        int configSpec[] = new int[]{
                EGL10.EGL_SURFACE_TYPE, EGL10.EGL_PBUFFER_BIT,
                EGL10.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL10.EGL_RED_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_DEPTH_SIZE, 0,
                EGL10.EGL_STENCIL_SIZE, 0,
                EGL10.EGL_NONE
        };
        mEgl.eglChooseConfig(mEglDisplay, configSpec, configs, 1, configsCount);
        if (configsCount[0] <= 0) {
            throw new RuntimeException("eglChooseConfig,failed:" + GLUtils.getEGLErrorString(mEgl.eglGetError()));
        }
        mEglConfig = configs[0];
        int attr[] = new int[]{
                EGL10.EGL_WIDTH, mWidth,
                EGL10.EGL_HEIGHT, mHeight,
                EGL10.EGL_LARGEST_PBUFFER,EGL14.EGL_TRUE,
                EGL10.EGL_NONE
        };
        mEglSurface = mEgl.eglCreatePbufferSurface(mEglDisplay, mEglConfig, attr);
        if (null == mEglSurface || EGL10.EGL_NO_SURFACE == mEglSurface) {
            throw new RuntimeException("eglCreateWindowSurface,failed:" + GLUtils.getEGLErrorString(mEgl.eglGetError()));
        }
        int contextSpec[] = new int[]{
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
        };
        mEglContext = mEgl.eglCreateContext(mEglDisplay, mEglConfig, EGL10.EGL_NO_CONTEXT, contextSpec);
        if (EGL10.EGL_NO_CONTEXT == mEglContext) {
            throw new RuntimeException("eglCreateContext,failed:" + GLUtils.getEGLErrorString(mEgl.eglGetError()));
        }
        if (!mEgl.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)) {
            throw new RuntimeException("eglMakeCurrent,failed:" + GLUtils.getEGLErrorString(mEgl.eglGetError()));
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

    private void initTexture() {
        GLES20.glEnable(GLES20.GL_TEXTURE_2D);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE2);

        createTexture(mWidth, mHeight, GLES20.GL_LUMINANCE, yTexture);
        createTexture(mWidth >> 1, mHeight >> 1, GLES20.GL_LUMINANCE, uTexture);
        createTexture(mWidth >> 1, mHeight >> 1, GLES20.GL_LUMINANCE, vTexture);

        GLES20.glUseProgram(mProgram);
        sampleYLoaction = GLES20.glGetUniformLocation(mProgram, "samplerY");
        sampleULoaction = GLES20.glGetUniformLocation(mProgram, "samplerU");
        sampleVLoaction = GLES20.glGetUniformLocation(mProgram, "samplerV");
        GLES20.glUniform1i(sampleYLoaction, 0);
        GLES20.glUniform1i(sampleULoaction, 1);
        GLES20.glUniform1i(sampleVLoaction, 2);
        int aPostionLocation = GLES20.glGetAttribLocation(mProgram, "aPosition");
        int aTextureCoordLocation = GLES20.glGetAttribLocation(mProgram, "aTextureCoord");
        GLES20.glEnableVertexAttribArray(aPostionLocation);
        GLES20.glVertexAttribPointer(aPostionLocation, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                COORDS_PER_VERTEX * 4, mSquareVerticesBuffer);
        GLES20.glEnableVertexAttribArray(aTextureCoordLocation);
        GLES20.glVertexAttribPointer(aTextureCoordLocation, TEXTURE_COORS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                TEXTURE_COORS_PER_VERTEX * 4, mTextureVerticesBuffer);

    }

    private void drawFrame() {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glUseProgram(mProgram);

        //=================================
        synchronized (syncBuff) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yTexture[0]);
            GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0,
                    mWidth,
                    mHeight,
                    GLES20.GL_LUMINANCE,
                    GLES20.GL_UNSIGNED_BYTE,
                    yBuf);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, uTexture[0]);
            GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0,
                    mWidth >> 1,
                    mHeight >> 1,
                    GLES20.GL_LUMINANCE,
                    GLES20.GL_UNSIGNED_BYTE,
                    uBuf);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, vTexture[0]);
            GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0,
                    mWidth >> 1,
                    mHeight >> 1,
                    GLES20.GL_LUMINANCE,
                    GLES20.GL_UNSIGNED_BYTE,
                    vBuf);

        }
        //=================================
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawIndices.length, GLES20.GL_UNSIGNED_SHORT, mDrawIndicesBuffer);
        GLES20.glFinish();
//        GLES20.glDisableVertexAttribArray(aPostionLocation);
//        GLES20.glDisableVertexAttribArray(aTextureCoordLocation);
    }

    @Override
    public void run() {
        /**
         * 初始化GLES环境
         */
        initGLES();
        /**
         * 创建program
         */
        mProgram = ProgramTools.createProgram(mContext, R.raw.vertexshader, R.raw.fragmentshader_yuv);
        /**
         * 创建顶点Buff
         */
        initVertex();
        /**
         * 创建YUV纹理
         */
        initTexture();
        while (!quit) {
            /**
             * 绘制
             */
            drawFrame();
            final IntBuffer pixelBuffer = IntBuffer.allocate(mWidth * mHeight);
            pixelBuffer.position(0);
            long a=System.currentTimeMillis();
            /**
             * toslow
             */
            GLES20.glReadPixels(0, 0, mWidth, mHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixelBuffer);
            Log.e("aa", "ttttttt="+(System.currentTimeMillis()-a));
            int[] pixelArray = pixelBuffer.array();
            MainActivity2.toYV12(pixelArray,yuvpix,mWidth,mHeight);
            HomeActivity.ndkdraw(mSurface, yuvpix, mWidth, mHeight, size);
            synchronized (syncThread) {
                try {
                    syncThread.wait();
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    //像素Buff
    private final Object syncBuff = new Object();
    private ByteBuffer yBuf;
    private ByteBuffer uBuf;
    private ByteBuffer vBuf;

    //纹理
    private int[] yTexture = new int[1];
    private int[] uTexture = new int[1];
    private int[] vTexture = new int[1];
    private int sampleYLoaction;
    private int sampleULoaction;
    private int sampleVLoaction;

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
}