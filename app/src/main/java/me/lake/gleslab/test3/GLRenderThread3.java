package me.lake.gleslab.test3;

import android.content.Context;
import android.opengl.EGL14;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.util.Log;
import android.view.Surface;

import java.nio.Buffer;
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
public class GLRenderThread3 extends Thread {
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

    public GLRenderThread3(Surface surface, Context context) {
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
                EGL10.EGL_ALPHA_SIZE,8,
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
//                EGL10.EGL_LARGEST_PBUFFER, EGL14.EGL_TRUE,
                EGL10.EGL_NONE
        };
        mEglSurface = mEgl.eglCreatePbufferSurface(mEglDisplay, mEglConfig, attr);
        if (null == mEglSurface || EGL10.EGL_NO_SURFACE == mEglSurface) {
            throw new RuntimeException("eglCreateWindowSurface,failed:" + GLUtils.getEGLErrorString(mEgl.eglGetError()));
        }
        int contextSpec[] = new int[]{
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 3,
                EGL14.EGL_NONE
        };
        mEglContext = mEgl.eglCreateContext(mEglDisplay, mEglConfig, EGL10.EGL_NO_CONTEXT, contextSpec);
        if (EGL10.EGL_NO_CONTEXT == mEglContext) {
            throw new RuntimeException("eglCreateContext,failed:" + GLUtils.getEGLErrorString(mEgl.eglGetError()));
        }
        if (!mEgl.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)) {
            throw new RuntimeException("eglMakeCurrent,failed:" + GLUtils.getEGLErrorString(mEgl.eglGetError()));
        }
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);
        GLES30.glDisable(GLES30.GL_CULL_FACE);
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
        GLES30.glGenTextures(1, texture, 0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture[0]);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, format, width, height, 0, format, GLES30.GL_UNSIGNED_BYTE, null);
    }

    private void initTexture() {
        GLES30.glEnable(GLES30.GL_TEXTURE_2D);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE2);

        createTexture(mWidth, mHeight, GLES30.GL_LUMINANCE, yTexture);
        createTexture(mWidth >> 1, mHeight >> 1, GLES30.GL_LUMINANCE, uTexture);
        createTexture(mWidth >> 1, mHeight >> 1, GLES30.GL_LUMINANCE, vTexture);

        GLES30.glUseProgram(mProgram);
        sampleYLoaction = GLES30.glGetUniformLocation(mProgram, "samplerY");
        sampleULoaction = GLES30.glGetUniformLocation(mProgram, "samplerU");
        sampleVLoaction = GLES30.glGetUniformLocation(mProgram, "samplerV");
        GLES30.glUniform1i(sampleYLoaction, 0);
        GLES30.glUniform1i(sampleULoaction, 1);
        GLES30.glUniform1i(sampleVLoaction, 2);
        int aPostionLocation = GLES30.glGetAttribLocation(mProgram, "aPosition");
        int aTextureCoordLocation = GLES30.glGetAttribLocation(mProgram, "aTextureCoord");
        GLES30.glEnableVertexAttribArray(aPostionLocation);
        GLES30.glVertexAttribPointer(aPostionLocation, COORDS_PER_VERTEX,
                GLES30.GL_FLOAT, false,
                COORDS_PER_VERTEX * 4, mSquareVerticesBuffer);
        GLES30.glEnableVertexAttribArray(aTextureCoordLocation);
        GLES30.glVertexAttribPointer(aTextureCoordLocation, TEXTURE_COORS_PER_VERTEX,
                GLES30.GL_FLOAT, false,
                TEXTURE_COORS_PER_VERTEX * 4, mTextureVerticesBuffer);

    }

    private void drawFrame() {
//        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
//        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);

        GLES30.glUseProgram(mProgram);

        //=================================
        synchronized (syncBuff) {
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, yTexture[0]);
            GLES30.glTexSubImage2D(GLES30.GL_TEXTURE_2D, 0, 0, 0,
                    mWidth,
                    mHeight,
                    GLES30.GL_LUMINANCE,
                    GLES30.GL_UNSIGNED_BYTE,
                    yBuf);
            GLES30.glActiveTexture(GLES30.GL_TEXTURE1);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, uTexture[0]);
            GLES30.glTexSubImage2D(GLES30.GL_TEXTURE_2D, 0, 0, 0,
                    mWidth >> 1,
                    mHeight >> 1,
                    GLES30.GL_LUMINANCE,
                    GLES30.GL_UNSIGNED_BYTE,
                    uBuf);
            GLES30.glActiveTexture(GLES30.GL_TEXTURE2);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, vTexture[0]);
            GLES30.glTexSubImage2D(GLES30.GL_TEXTURE_2D, 0, 0, 0,
                    mWidth >> 1,
                    mHeight >> 1,
                    GLES30.GL_LUMINANCE,
                    GLES30.GL_UNSIGNED_BYTE,
                    vBuf);
        }
        //=================================
//        GLES30.glGenBuffers(1, i);
//        pbo = i.array()[0];
//        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, pbo);
//        GLES30.glBufferData(GLES30.GL_PIXEL_PACK_BUFFER, ySize, null, GLES30.GL_DYNAMIC_READ);
//        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, 0);
        long a = System.currentTimeMillis();
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, drawIndices.length, GLES30.GL_UNSIGNED_SHORT, mDrawIndicesBuffer);
        GLES30.glFinish();
        Log.e("aa", "drawt=" + (System.currentTimeMillis() - a));
//        GLES30.glDisableVertexAttribArray(aPostionLocation);
//        GLES30.glDisableVertexAttribArray(aTextureCoordLocation);
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
        IntBuffer i = IntBuffer.allocate(1);
        GLES30.glGenBuffers(1, i);
        int pbo0 = i.array()[0];
        i = IntBuffer.allocate(1);
        GLES30.glGenBuffers(1, i);
        int pbo1 = i.array()[0];
        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, pbo0);
        GLES30.glBufferData(GLES30.GL_PIXEL_PACK_BUFFER, 4*ySize, null, GLES30.GL_DYNAMIC_READ);
        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, pbo1);
        GLES30.glBufferData(GLES30.GL_PIXEL_PACK_BUFFER, 4*ySize, null, GLES30.GL_DYNAMIC_READ);
        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, 0);
        while (!quit) {
            /**
             * 绘制
             */
            GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, 0);
            drawFrame();
            final IntBuffer pixelBuffer = IntBuffer.allocate(mWidth * mHeight);
            pixelBuffer.position(0);
            /**
             * ~30ms
             */
            int pw, pr;
            if (index%2 == 0) {
                pw = pbo0;
                pr = pbo1;
            } else {
                pw = pbo1;
                pr = pbo0;
            }
            index++;
            GLES30.glPixelStorei(GLES30.GL_PACK_ALIGNMENT,1);
            GLES30.glReadBuffer(GLES30.GL_BACK);
            GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, pbo0);
            byte[] pixelArray = new byte[4*ySize];
            HomeActivity.readPixel(pixelArray, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, 0);
            GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, pbo0);
            Buffer buf = GLES30.glMapBufferRange(GLES30.GL_PIXEL_PACK_BUFFER, 0, 4*ySize, GLES30.GL_MAP_READ_BIT);
            ByteBuffer b = ((ByteBuffer) buf).order(ByteOrder.nativeOrder());
            long a = System.currentTimeMillis();
            b.get(pixelArray, 0, 4*ySize);
            Log.e("aa", "tttreadPixel=" + (System.currentTimeMillis() - a));
            HomeActivity.toYV12(pixelArray, yuvpix, mWidth, mHeight);
            HomeActivity.ndkdraw(mSurface, yuvpix, mWidth, mHeight, size);
            GLES30.glUnmapBuffer(GLES30.GL_PIXEL_PACK_BUFFER);
            GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, 0);
            synchronized (syncThread) {
                try {
                    syncThread.wait();
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    int index = 0;

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