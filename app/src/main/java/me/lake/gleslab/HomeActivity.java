package me.lake.gleslab;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Surface;
import android.view.View;

import me.lake.gleslab.test1.MainActivity1;
import me.lake.gleslab.test2.MainActivity2;
import me.lake.gleslab.test3.MainActivity3;
import me.lake.gleslab.test4.MainActivity4;

/**
 * Created by lake on 16-5-16.
 */
public class HomeActivity extends AppCompatActivity {
    @Override
    @SuppressWarnings("all")
    protected void onCreate(@Nullable Bundle savedInstanceState) {
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
    }
    public static native void ndkdraw(Surface surface, byte[] pixels, int w, int h, int s);
    public static native void toYV12(byte[] src, byte[] dst, int w, int h);
    public static native void readPixel(byte[] pix,int foramt,int type,int offset);
    static {
        System.loadLibrary("nativewindow");
    }
}
