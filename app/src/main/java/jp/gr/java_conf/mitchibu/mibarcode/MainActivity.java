package jp.gr.java_conf.mitchibu.mibarcode;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Pair;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener, ImageReader.OnImageAvailableListener {
	private static final int ACQUIRE_PERMISSION_REQUEST = 100;

	private TextureView preview = null;
	private BarcodeOverlayView overlay = null;

	private HandlerThread thread = null;
	private Handler handler = null;
	private ImageReader reader = null;

	private Camera2Helper helper = null;

	private BarcodeDetector detector = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		overlay = (BarcodeOverlayView)findViewById(R.id.overlay);
		preview = (TextureView)findViewById(R.id.preview);
		preview.setSurfaceTextureListener(this);

		thread = new HandlerThread(getPackageName());
		thread.start();
		handler = new Handler(thread.getLooper());
		helper = new Camera2Helper(this, handler);

		detector = new BarcodeDetector.Builder(this).setBarcodeFormats(Barcode.QR_CODE).build();
		BarcodeProcessorFactory factory = new BarcodeProcessorFactory();
		detector.setProcessor(new MultiProcessor.Builder<>(factory).build());
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		stop();
		if(thread != null) {
			thread.quitSafely();
			thread = null;
		}
		detector.release();
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
			startIfReady();
		}
	}

	@Override
	public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
		startIfReady();
	}

	@Override
	public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
		adjustPreview();
	}

	@Override
	public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
		stop();
		return true;
	}

	@Override
	public void onSurfaceTextureUpdated(SurfaceTexture texture) {
	}

	@Override
	public void onImageAvailable(ImageReader reader) {
		Image image = null;
		try {
			image = reader.acquireLatestImage();
			if(image == null) return;

			int degrees;
			switch(getWindowManager().getDefaultDisplay().getRotation()) {
			case Surface.ROTATION_0:
				degrees = 0;
				break;
			case Surface.ROTATION_90:
				degrees = 90;
				break;
			case Surface.ROTATION_180:
				degrees = 180;
				break;
			case Surface.ROTATION_270:
				degrees = 270;
				break;
			default:
				throw new UnsupportedOperationException();
			}

			int angle;
//			if(cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
//				angle = (cameraInfo.orientation + degrees) % 360;
//			} else {  // back-facing
//				angle = (cameraInfo.orientation - degrees + 360) % 360;
				angle = (90 - degrees + 360) % 360;
//			}

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
		} finally {
			if(image != null) image.close();
		}
	}

	private void adjustPreview() {
		if(reader == null) return;

		final int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
		final float w = reader.getWidth();
		final float h = reader.getHeight();
		final float width = preview.getWidth();
		final float height = preview.getHeight();
		final int centerX = width / 2;
		final int centerY = height / 2;
		final float scale = Math.max(width / h, height / w);
		float scaleX = h / width * scale;
		float scaleY = w / height * scale;

		Matrix matrix = new Matrix();
		matrix.postScale(scaleX, scaleY, centerX, centerY);
		matrix.postRotate((360 - rotation * 90) % 360, centerX, centerY);
		preview.setTransform(matrix);
	}

	private void startIfReady() {
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
				ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, ACQUIRE_PERMISSION_REQUEST);
				return;
			}
		}
		if(!preview.isAvailable()) return;

		Pair<String, StreamConfigurationMap> result = helper.findCamera(CameraCharacteristics.LENS_FACING_BACK);
		if(result == null) {
			// TODO
			finish();
		} else {
			// TODO プレビューサイズ ＆ TextureView.setTransform
			int width = preview.getWidth();
			int height = preview.getHeight();
			Size[] sizes = result.second.getOutputSizes(SurfaceTexture.class);
			Size size = Camera2Helper.chooseOptimalSize(sizes, width, height);
			android.util.Log.v("test", String.format("size: %dx%d %dx%d", width, height, size.getWidth(), size.getHeight()));
			preview.getSurfaceTexture().setDefaultBufferSize(size.getWidth(), size.getHeight());
			reader = ImageReader.newInstance(size.getWidth(), size.getHeight(), ImageFormat.YUV_420_888, 2);
			reader.setOnImageAvailableListener(this, handler);

			adjustPreview();

			helper.start(result.first, new Surface(preview.getSurfaceTexture()), reader.getSurface());
		}
	}

	private void stop() {
		if(helper != null) {
			helper.stop();
		}
		if(reader != null) {
			reader.close();
			reader = null;
		}
	}

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
			overlay.update(detections);
		}

		@Override
		public void onMissing(Detector.Detections<Barcode> detections) {
			overlay.reset(detections);
		}

		@Override
		public void onDone() {
			overlay.reset(null);
		}
	}
}
