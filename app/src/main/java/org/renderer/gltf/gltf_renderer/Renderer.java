package org.renderer.gltf.gltf_renderer;

import android.graphics.SurfaceTexture;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.TextureView;
import android.view.View;

import org.renderer.gltf.gltf_renderer.SampleGLTFView;

import org.renderer.gltf.gltf_renderer.R;

public class Renderer extends AppCompatActivity {
    private SampleGLTFView glTFView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_renderer);
        glTFView = (SampleGLTFView) findViewById(R.id.gltf_view);
        init();
    }

    private void init() {
        glTFView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                glTFView.initRenderThread(surface, width, height);
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                glTFView.releaseResources();
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        });
        glTFView.setVisibility(View.VISIBLE);
    }
}
