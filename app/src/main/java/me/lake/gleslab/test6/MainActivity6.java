package me.lake.gleslab.test6;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.TextureView;

import java.io.IOException;

import me.lake.gleslab.R;

public class MainActivity6 extends AppCompatActivity implements TextureView.SurfaceTextureListener, SurfaceTexture.OnFrameAvailableListener {
    int w = 1280;
    int h = 720;
    TextureView txv_image;
    ScreenRenderThread6 screenRenderThread;
    int textureId;
    SurfaceTexture camTexture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main4);
        txv_image = (TextureView) findViewById(R.id.txv_image);
        txv_image.setSurfaceTextureListener(this);
        textureId = createTexture();
        this.getWindow().getDecorView().setKeepScreenOn(true);
        camTexture = new SurfaceTexture(textureId);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        startCamera(camTexture);
        screenRenderThread = new ScreenRenderThread6(this, textureId, camTexture, surface);
        screenRenderThread.setWH(width, height);
        screenRenderThread.start();
        Log.e("aa", "onSurfaceTextureAvailable");
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        screenRenderThread.setWH(width, height);
        Log.e("aa", "onSurfaceTextureSizeChanged");
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.e("aa", "onSurfaceTextureDestroyed");
        screenRenderThread.quit();
        stopCamera();
        try {
            screenRenderThread.join();
        } catch (InterruptedException ignored) {
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        Log.e("aa", "onSurfaceTextureUpdated");
    }

    Camera cam;

    private void startCamera(SurfaceTexture texture) {
        texture.setOnFrameAvailableListener(this);
        cam = Camera.open(0);
        try {
//            cam.setDisplayOrientation(90);
            cam.setPreviewTexture(texture);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Camera.Parameters parameters = cam.getParameters();
//        parameters.setFlashMode("off"); // 无闪光灯
//        parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
//        parameters.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        parameters.setPreviewSize(w, h);
        parameters.setPreviewFpsRange(30000,
                30000);
        cam.setParameters(parameters);
        cam.startPreview();
        texture.detachFromGLContext();
    }

    long lastT;

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        Log.e("aa", "onFrameAvailable" + (System.currentTimeMillis() - lastT));
        lastT = System.currentTimeMillis();
        screenRenderThread.queue();
    }

    private void stopCamera() {
        cam.stopPreview();
        cam.release();
    }

    public static int createTexture() {
        int[] textureHandle = new int[1];
        GLES20.glGenTextures(1, textureHandle, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureHandle[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        return textureHandle[0];
    }
}
