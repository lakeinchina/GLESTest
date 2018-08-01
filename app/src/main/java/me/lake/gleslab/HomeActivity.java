package me.lake.gleslab;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Surface;
import android.view.View;

import java.io.IOException;
import java.io.InputStream;

import me.lake.gleslab.test1.MainActivity1;
import me.lake.gleslab.test2.MainActivity2;
import me.lake.gleslab.test3.MainActivity3;
import me.lake.gleslab.test4.MainActivity4;
import me.lake.gleslab.test5.MainActivity5;
import me.lake.gleslab.test6.MainActivity6;
import me.lake.gleslab.test7.MainActivity7;

/**
 * Created by lake on 16-5-16.
 */
public class HomeActivity extends AppCompatActivity {
    static  Context context;
    @Override
    @SuppressWarnings("all")
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        context = this.getApplicationContext();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        findViewById(R.id.btn_display).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, MainActivity1.class));
            }
        });
        findViewById(R.id.btn_display_pixelbuff).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, MainActivity2.class));
            }
        });
        findViewById(R.id.btn_display_pbuffer_pixelbuffobject).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, MainActivity3.class));
            }
        });
        findViewById(R.id.btn_display_texutreOES_mediacodec_surface_input).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, MainActivity4.class));
            }
        });
        findViewById(R.id.btn_display_texutreOES_mediacodec_complex).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, MainActivity5.class));
            }
        });
        findViewById(R.id.btn_framebuffer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, MainActivity6.class));
            }
        });
        findViewById(R.id.btn_EGL_KHR_IMAGE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, MainActivity7.class));
            }
        });
    }

    public static native void ndkdraw(Surface surface, byte[] pixels, int w, int h, int s);

    public static native void toYV12(byte[] src, byte[] dst, int w, int h);

    public static native void readPixel(byte[] pix, int foramt, int type, int offset);

    public static native int runImageKHR(Object surface,int height,int width);
    public static int loadTexture(){
        return GlUtil.createTexture(GLES20.GL_TEXTURE_2D,getImageFromAssetsFile(context,"test.jpg"));
    }
    public static Bitmap getImageFromAssetsFile(Context context, String fileName) {
        Bitmap image = null;
        AssetManager am = context.getResources().getAssets();
        try {
            InputStream is = am.open(fileName);
            image = BitmapFactory.decodeStream(is);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return image;
    }

    static {
        System.loadLibrary("nativewindow");
    }
}
