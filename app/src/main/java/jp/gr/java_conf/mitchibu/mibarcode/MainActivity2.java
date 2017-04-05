package jp.gr.java_conf.mitchibu.mibarcode;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Size;
import android.view.Surface;
import android.view.View;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.nio.ByteBuffer;

import jp.gr.java_conf.mitchibu.cameracompat.CameraCompat;
import jp.gr.java_conf.mitchibu.glengine.GLEngine;

public class MainActivity2 extends AppCompatActivity implements BarcodeScene.Callback {
	private static final int ACQUIRE_PERMISSION_REQUEST = 100;

	private BarcodeScene scene = null;
	private GLSurfaceView preview;
	private CameraCompat camera = null;
	private BarcodeDetector detector = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
				ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, ACQUIRE_PERMISSION_REQUEST);
				return;
			}
		}
		init();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if(preview != null) preview.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
		if(preview != null) preview.onPause();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(camera != null) camera.release();
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

	@SuppressWarnings("NullableProblems")
	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		if(requestCode == ACQUIRE_PERMISSION_REQUEST) {
			for(int result : grantResults) {
				if(result != PackageManager.PERMISSION_GRANTED) {
					// TODO
					finish();
					return;
				}
			}
			init();
		}
	}

	Size size;
	@Override
	public void onInitialized(SurfaceTexture surfaceTexture) {
		try {
			camera.startPreview(surfaceTexture);
			size = new Size(camera.getConfiguredPreviewWidth(), camera.getConfiguredPreviewHeight());
//			scene.setContentSize(size.getWidth(), size.getHeight());
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("SuspiciousNameCombination")
	@SuppressLint("SwitchIntDef")
	@Override
	public void onConfigured(int width, int height) {
		int contentWidth;
		int contentHeight;
		int rotate = getWindowManager().getDefaultDisplay().getRotation();
		switch(rotate) {
		case Surface.ROTATION_0:
		case Surface.ROTATION_180:
//			contentWidth = size.height;
//			contentHeight = size.width;
			contentWidth = size.getHeight();
			contentHeight = size.getWidth();
			break;
		case Surface.ROTATION_90:
		case Surface.ROTATION_270:
//			contentWidth = size.width;
//			contentHeight = size.height;
			contentWidth = size.getWidth();
			contentHeight = size.getHeight();
			break;
		default:
			throw new RuntimeException();
		}
		scene.setContentSize(contentWidth, contentHeight);
		scene.setRotate(rotate);
	}

	private void init() {
		CameraCompat.Builder builder = new CameraCompat.Builder(this, false, new CameraCompat.Callback() {
			@Override
			public void onReceive(ByteBuffer buffer, int width, int height) {
				int degrees = getWindowManager().getDefaultDisplay().getRotation() * 90;
				int angle;
//				if(cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
//					angle = (cameraInfo.orientation + degrees) % 360;
//				} else {  // back-facing
//					angle = (cameraInfo.orientation - degrees + 360) % 360;
				angle = (90 - degrees + 360) % 360;
//				}

				Frame.Builder builder = new Frame.Builder();
				builder.setImageData(buffer, width, height, ImageFormat.NV21);
				builder.setRotation(angle / 90);
				detector.receiveFrame(builder.build());
			}

			@Override
			public void onError(int code, Exception e) {
				throw new RuntimeException();
			}
		});
		builder.setFaceFront(false);
		builder.setPreviewWidth(800);
		builder.setPreviewHeight(600);
		builder.setEnableAF(true);
		camera = builder.build();

		detector = new BarcodeDetector.Builder(this).setBarcodeFormats(Barcode.ALL_FORMATS).build();
		BarcodeProcessorFactory factory = new BarcodeProcessorFactory();
		detector.setProcessor(new MultiProcessor.Builder<>(factory).build());

		GLEngine engine = new GLEngine();
		engine.setScene(scene = new BarcodeScene(this));

		preview = new GLSurfaceView(this);
		preview.setPreserveEGLContextOnPause(true);
		preview.setKeepScreenOn(true);
		preview.setEGLContextClientVersion(2);
		preview.setRenderer(engine);
		setContentView(preview);
	}

	private class BarcodeProcessorFactory implements MultiProcessor.Factory<Barcode> {
		@Override
		public Tracker<Barcode> create(Barcode barcode) {
			return new BarcodeTracker();
		}
	}

	private class BarcodeTracker extends Tracker<Barcode> {
		@Override
		public void onNewItem(final int id, final Barcode item) {
//			runOnUiThread(new Runnable() {
//				@Override
//				public void run() {
//					result.setVisibility(View.VISIBLE);
//					result.setText(item.rawValue);
//				}
//			});
		}

		@Override
		public void onUpdate(Detector.Detections<Barcode> detections, Barcode barcode) {
//			overlay.update(detections);
			scene.update(detections);
		}

		@Override
		public void onMissing(Detector.Detections<Barcode> detections) {
//			overlay.reset(detections);
			scene.reset(detections);
		}

		@Override
		public void onDone() {
//			overlay.reset(null);
			scene.reset(null);
		}
	}
}
