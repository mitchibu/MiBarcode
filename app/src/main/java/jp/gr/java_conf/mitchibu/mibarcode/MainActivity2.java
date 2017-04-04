package jp.gr.java_conf.mitchibu.mibarcode;

import android.annotation.SuppressLint;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Pair;
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

import jp.gr.java_conf.mitchibu.glengine.GLEngine;
import jp.gr.java_conf.mitchibu.mibarcode.camera.CameraSource;

@SuppressWarnings("deprecation")
public class MainActivity2 extends AppCompatActivity implements BarcodeScene.Callback {
	private BarcodeScene scene = null;
	private GLSurfaceView preview;
//	private Camera camera;
	private CameraSource source;
	private BarcodeDetector detector = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		detector = new BarcodeDetector.Builder(this).setBarcodeFormats(Barcode.ALL_FORMATS).build();
		BarcodeProcessorFactory factory = new BarcodeProcessorFactory();
		detector.setProcessor(new MultiProcessor.Builder<>(factory).build());
		source = new CameraSource(this, new CameraSource.OnImageAvailableListener() {
			@Override
			public void OnImageAvailable(Image image) {
				int degrees = getWindowManager().getDefaultDisplay().getRotation() * 90;
				int angle;
//				if(cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
//					angle = (cameraInfo.orientation + degrees) % 360;
// 				} else {  // back-facing
//					angle = (cameraInfo.orientation - degrees + 360) % 360;
					angle = (90 - degrees + 360) % 360;
//				}

				Image.Plane[] planes = image.getPlanes();
				if(planes == null || planes.length == 0) return;
				ByteBuffer bb = planes[0].getBuffer();
				Frame outputFrame = new Frame.Builder()
						.setImageData(bb, image.getWidth(), image.getHeight(), ImageFormat.NV21)
//					.setId(mPendingFrameId)
//					.setTimestampMillis(mPendingTimeMillis)
						.setRotation(angle / 90)
						.build();
				detector.receiveFrame(outputFrame);
			}
		});

		GLEngine engine = new GLEngine();
		engine.setScene(scene = new BarcodeScene(this));
//		camera = Camera.open();

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
//		camera.release();
		source.stop();
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

	Size size;
	@Override
	public void onInitialized(SurfaceTexture surfaceTexture) {
		Pair<String, StreamConfigurationMap> result = CameraSource.findCamera(this, CameraCharacteristics.LENS_FACING_BACK);
		Size[] sizes = result.second.getOutputSizes(SurfaceTexture.class);
//		size = CameraSource.chooseOptimalSize(sizes, preview.getHeight(), preview.getWidth());
		size = CameraSource.chooseOptimalSize(sizes, 640, 480);

		if(result != null) {
			try {
				source.start(this, result.first, size.getWidth(), size.getHeight(), surfaceTexture, 100);
//				scene.setContentSize(size.getWidth(), size.getHeight());
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
//		try {
//			camera.setPreviewTexture(surfaceTexture);
//			Camera.Parameters params = camera.getParameters();
//			Camera.Size size = params.getPreferredPreviewSizeForVideo();
//			params.setPreviewSize(size.width, size.height);
//			camera.setParameters(params);
//			camera.startPreview();
//			scene.setContentSize(size.width, size.height);
//		} catch(Exception e) {
//			e.printStackTrace();
//		}
	}

	@SuppressWarnings("SuspiciousNameCombination")
	@SuppressLint("SwitchIntDef")
	@Override
	public void onConfigured(int width, int height) {
//		Camera.Parameters params = camera.getParameters();
//		Camera.Size size = getContentSize();
//		params.setPreviewSize(size.width, size.height);
//		camera.setParameters(params);
//		camera.startPreview();

		int contentWidth;
		int contentHeight;
		int rotate = getWindowManager().getDefaultDisplay().getRotation();
		switch(rotate) {
		case Surface.ROTATION_0:
//			contentWidth = size.height;
//			contentHeight = size.width;
			contentWidth = size.getHeight();
			contentHeight = size.getWidth();
			break;
		case Surface.ROTATION_90:
//			contentWidth = size.width;
//			contentHeight = size.height;
			contentWidth = size.getWidth();
			contentHeight = size.getHeight();
			break;
		case Surface.ROTATION_180:
//			contentWidth = size.height;
//			contentHeight = size.width;
			contentWidth = size.getHeight();
			contentHeight = size.getWidth();
			break;
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
//		scene.setViewport((width - contentWidth) / 2, (height - contentHeight) / 2, contentWidth, contentHeight);
		float scale = 1;//Math.min((float)width / contentWidth, (float)height / contentHeight);
		contentWidth = (int)((float)contentWidth * scale);
		contentHeight = (int)((float)contentHeight * scale);
//		scene.setViewport((width - contentWidth) / 2, 100, contentWidth, contentHeight);
//		scene.setViewport((width - contentWidth) / 2, (height - contentHeight) / 2, contentWidth, contentHeight);
	}

//	private Camera.Size getContentSize() {
//		Camera.Parameters params = camera.getParameters();
//		List<Camera.Size> sizes = params.getSupportedPreviewSizes();
//		return params.getPreferredPreviewSizeForVideo();
//	}

	public class BarcodeProcessorFactory implements MultiProcessor.Factory<Barcode> {
		@Override
		public Tracker<Barcode> create(Barcode barcode) {
			return new BarcodeTracker();
		}
	}

	public class BarcodeTracker extends Tracker<Barcode> {
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
