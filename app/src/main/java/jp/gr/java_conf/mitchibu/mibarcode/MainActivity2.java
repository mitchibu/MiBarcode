package jp.gr.java_conf.mitchibu.mibarcode;

import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import jp.gr.java_conf.mitchibu.glengine.GLEngine;

@SuppressWarnings("deprecation")
public class MainActivity2 extends AppCompatActivity {
	private Camera camera = null;
	private GLSurfaceView preview;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

//		camera = Camera.open();
		GLEngine engine = new GLEngine();
		engine.setScene(new BarcodeScene(this, camera));

		preview = new GLSurfaceView(this);
		preview.setPreserveEGLContextOnPause(true);
		preview.setKeepScreenOn(true);
		preview.setEGLContextClientVersion(2);
		preview.setRenderer(engine);
		setContentView(preview);
	}

	@Override
	protected void onResume() {
		super.onResume();
		preview.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
		preview.onPause();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(camera != null) camera.release();
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return;
		if(hasFocus) {
			getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
					| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
					| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
					| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
					| View.SYSTEM_UI_FLAG_FULLSCREEN
					| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
		}
	}
}
