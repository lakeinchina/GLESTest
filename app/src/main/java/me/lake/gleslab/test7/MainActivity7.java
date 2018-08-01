package me.lake.gleslab.test7;

import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import me.lake.gleslab.HomeActivity;
import me.lake.gleslab.R;

public class MainActivity7 extends AppCompatActivity implements TextureView.SurfaceTextureListener {
    TextureView txv_image;
    RenderT renderT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main4);
        txv_image = findViewById(R.id.txv_image);
        txv_image.setSurfaceTextureListener(this);
        this.getWindow().getDecorView().setKeepScreenOn(true);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        renderT = new RenderT(new Surface(surface), width, height);
        renderT.start();
        Log.e("aa", "onSurfaceTextureAvailable");
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.e("aa", "onSurfaceTextureSizeChanged");
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.e("aa", "onSurfaceTextureDestroyed");
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        Log.e("aa", "onSurfaceTextureUpdated");
    }

    class RenderT extends Thread {
        Surface surface;
        int width;
        int height;

        public RenderT(Surface s, int w, int h) {
            surface = s;
            width = w;
            height = h;
        }

        @Override
        public void run() {
            //https://www.khronos.org/registry/EGL/extensions/KHR/EGL_KHR_image_base.txt
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            HomeActivity.runImageKHR(surface, width, height);
            try {
                sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
