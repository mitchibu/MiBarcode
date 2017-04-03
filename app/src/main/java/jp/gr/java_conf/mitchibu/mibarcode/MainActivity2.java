package jp.gr.java_conf.mitchibu.mibarcode;

import android.annotation.SuppressLint;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Surface;
import android.view.View;

import jp.gr.java_conf.mitchibu.glengine.GLEngine;

@SuppressWarnings("deprecation")
public class MainActivity2 extends AppCompatActivity implements BarcodeScene.Callback {
	private BarcodeScene scene = null;
	private GLSurfaceView preview;
	private Camera camera;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		GLEngine engine = new GLEngine();
		engine.setScene(scene = new BarcodeScene(this));
		camera = Camera.open();

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
		camera.release();
	}

	@SuppressLint("ObsoleteSdkInt")
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

	@Override
	public void onInitialized(SurfaceTexture surfaceTexture) {
		try {
			camera.setPreviewTexture(surfaceTexture);
			Camera.Parameters params = camera.getParameters();
			Camera.Size size = params.getPreferredPreviewSizeForVideo();
			params.setPreviewSize(size.width, size.height);
			camera.setParameters(params);
			camera.startPreview();
			scene.setContentSize(size.width, size.height);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("SuspiciousNameCombination")
	@SuppressLint("SwitchIntDef")
	@Override
	public void onConfigured(int width, int height) {
		Camera.Parameters params = camera.getParameters();
		Camera.Size size = getContentSize();
		params.setPreviewSize(size.width, size.height);
		camera.setParameters(params);
		camera.startPreview();

		int contentWidth;
		int contentHeight;
		int rotate = getWindowManager().getDefaultDisplay().getRotation();
		switch(rotate) {
		case Surface.ROTATION_0:
			contentWidth = size.height;
			contentHeight = size.width;
			break;
		case Surface.ROTATION_90:
			contentWidth = size.width;
			contentHeight = size.height;
			break;
		case Surface.ROTATION_180:
			contentWidth = size.height;
			contentHeight = size.width;
			break;
		case Surface.ROTATION_270:
			contentWidth = size.width;
			contentHeight = size.height;
			break;
		default:
			throw new RuntimeException();
		}
		scene.setRotate(rotate);
		scene.setViewport((width - contentWidth) / 2, (height - contentHeight) / 2, contentWidth, contentHeight);
	}

	private Camera.Size getContentSize() {
		Camera.Parameters params = camera.getParameters();
//		List<Camera.Size> sizes = params.getSupportedPreviewSizes();
		return params.getPreferredPreviewSizeForVideo();
	}
}
