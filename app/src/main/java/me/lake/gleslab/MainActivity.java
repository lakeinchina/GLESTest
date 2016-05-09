package me.lake.gleslab;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.widget.Button;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener {
    int w = 1280;
    int h = 720;
    int ySize = w * h;
    int uvSize = ySize >> 1;
    int uSize = uvSize >> 1;
    int size = ySize + uvSize;
    byte[] pixBuff;
    byte[] y, u, v;
    Button btn_process;
    TextureView txv_image;
    SurfaceView sv_image;
    GLRenderThread glRenderThread;
    Surface mSurface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txv_image = (TextureView) findViewById(R.id.txv_image);
        this.getWindow().getDecorView().setKeepScreenOn(true);
        sv_image = (SurfaceView) findViewById(R.id.sv_image);
        pixBuff = new byte[size];
        y = new byte[ySize];
        u = new byte[uSize];
        v = new byte[uSize];
        txv_image.setSurfaceTextureListener(this);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        startCamera();
        mSurface = new Surface(surface);
        glRenderThread = new GLRenderThread(mSurface,this);
        glRenderThread.setWH(width, height);
        glRenderThread.start();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        glRenderThread.setWH(width, height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        glRenderThread.quit();
        stopCamera();
        try {
            glRenderThread.join();
        } catch (InterruptedException ignored) {
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    Camera cam;
    SurfaceTexture texture;

    private void startCamera() {
        cam = Camera.open(0);
        try {
            texture = new SurfaceTexture(10);
            cam.setPreviewTexture(texture);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Camera.Parameters parameters = cam.getParameters();
//        parameters.setFlashMode("off"); // 无闪光灯
//        parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
//        parameters.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        parameters.setPreviewFormat(ImageFormat.YV12);
        parameters.setPreviewSize(w, h);
        parameters.setPreviewFpsRange(30000,
                30000);
//        parameters.setPreviewFpsRange(30000,
//                30000);
//        parameters.setPreviewFrameRate(30);
//        parameters.setPictureSize(VideoWidth, VideoHeight);
        //这两个属性 如果这两个属性设置的和真实手机的不一样时，就会报错
        cam.setParameters(parameters);
        cam.addCallbackBuffer(new byte[(w * h * 3) / 2]);
        cam.addCallbackBuffer(new byte[(w * h * 3) / 2]);
        cam.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                System.arraycopy(data, 0, y, 0, ySize);
                System.arraycopy(data, ySize, v, 0, uSize);
                System.arraycopy(data, ySize + uSize, u, 0, uSize);
                glRenderThread.queueYUV(y, u, v);
//                ndkdraw(sv_image.getHolder().getSurface(), data, w, h, data.length);
                camera.addCallbackBuffer(data);
            }
        });
//        cam.startPreview();
    }

    private void stopCamera() {
        cam.stopPreview();
        cam.release();
    }

    static {
        System.loadLibrary("nativewindow");
    }

    public static native void ndkdraw(Surface surface, byte[] pixels, int w, int h, int s);
    public static native void toYV12(byte[] src, byte[] dst, int w, int h);

    public static native void readPixel(byte[] pix,int foramt,int type,int offset);
}
