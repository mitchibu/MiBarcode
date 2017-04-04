package jp.gr.java_conf.mitchibu.mibarcode.camera;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.ActivityCompat;
import android.util.Pair;
import android.util.Size;
import android.view.Surface;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
public class CameraSource {
	public static Pair<String, StreamConfigurationMap> findCamera(Context context, int facing) {
		CameraManager cm = (CameraManager)context.getSystemService(Context.CAMERA_SERVICE);
		try {
			for(String id : cm.getCameraIdList()) {
				CameraCharacteristics characteristics = cm.getCameraCharacteristics(id);
				if(Integer.valueOf(facing).equals(characteristics.get(CameraCharacteristics.LENS_FACING))) {
					return new Pair<>(id, characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP));
				}
			}
		} catch(CameraAccessException e) {
			e.printStackTrace();
		}
		return null;
	}

	@SuppressWarnings("SuspiciousNameCombination")
	public static Size chooseOptimalSize(Size[] sizes, int width, int height) {
		if(width < height) {
			int t = width;
			width = height;
			height = t;
		}

		List<Size> bigEnough = new ArrayList<>();
		List<Size> notBigEnough = new ArrayList<>();
		for(Size size : sizes) {
			if(size.getWidth() >= width && size.getHeight() >= height) {
				bigEnough.add(size);
			} else {
				notBigEnough.add(size);
			}
		}

		if(bigEnough.size() > 0) {
			return Collections.min(bigEnough, new CompareSizesByArea());
		} else if(notBigEnough.size() > 0) {
			return Collections.max(notBigEnough, new CompareSizesByArea());
		} else {
			return sizes[0];
		}
	}

	private final ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener() {
		@Override
		public void onImageAvailable(ImageReader imageReader) {
			Image image = null;
			try {
				image = reader.acquireLatestImage();
				if(image != null && listener != null) listener.OnImageAvailable(image);
			} finally {
				if(image != null) image.close();
			}
		}
	};
	private final CameraManager cm;
	private final Context context;
	private final OnImageAvailableListener listener;
	private Semaphore cameraOpenCloseLock = new Semaphore(1);

	private HandlerThread thread = null;
	private Handler handler = null;
	private ImageReader reader = null;
	private CameraDevice cameraDevice = null;
	private CameraCaptureSession cameraSession = null;

	public CameraSource(Context context, OnImageAvailableListener listener) {
		cm = (CameraManager)context.getSystemService(Context.CAMERA_SERVICE);
		this.context = context;
		this.listener = listener;

		thread = new HandlerThread(getClass().getSimpleName());
		thread.start();
		handler = new Handler(thread.getLooper());
	}

	public void start(Activity activity, final String id, int width, int height, final SurfaceTexture surfaceTexture, int requestCode) throws CameraAccessException, InterruptedException {
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if(ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
				ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CAMERA}, requestCode);
				return;
			}
		}
		if(surfaceTexture == null) return;

//		Size[] sizes = result.second.getOutputSizes(SurfaceTexture.class);
//		Size size = Camera2Helper.chooseOptimalSize(sizes, width, height);
//		android.util.Log.v("test", String.format("size: %dx%d %dx%d", width, height, size.getWidth(), size.getHeight()));
		surfaceTexture.setDefaultBufferSize(width, height);
		reader = ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, 2);
		reader.setOnImageAvailableListener(onImageAvailableListener, handler);

//		adjustPreview();
		handler.post(new Runnable() {
			@Override
			public void run() {
				try {
					openDevice(id, new Surface(surfaceTexture), reader.getSurface());
				} catch(CameraAccessException | InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
	}

	public void stop() {
		closeDevice();
		thread.quit();
	}

	private void openDevice(String id, final Surface... surfaces) throws CameraAccessException, InterruptedException {
		if(!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
			throw new RuntimeException("Time out waiting to lock camera opening.");
		}

		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if(ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) return;
		}

		cm.openCamera(id, new CameraDevice.StateCallback() {
			@SuppressWarnings("NullableProblems")
			@Override
			public void onOpened(CameraDevice camera) {
				cameraOpenCloseLock.release();
				cameraDevice = camera;
				try {
					createSession(camera, surfaces);
				} catch(CameraAccessException e) {
					e.printStackTrace();
				}
			}

			@SuppressWarnings("NullableProblems")
			@Override
			public void onDisconnected(CameraDevice camera) {
				cameraOpenCloseLock.release();
				closeDevice();
			}

			@SuppressWarnings("NullableProblems")
			@Override
			public void onError(CameraDevice camera, int error) {
				cameraOpenCloseLock.release();
				closeDevice();
			}
		}, null);
	}

	private void closeDevice() {
		try {
			cameraOpenCloseLock.acquire();
		} catch(InterruptedException e) {
			e.printStackTrace();
		}

		destroySession();
		if(cameraDevice != null) {
			cameraDevice.close();
			cameraDevice = null;
		}

		cameraOpenCloseLock.release();
	}

	private void createSession(CameraDevice camera, final Surface... surfaces) throws CameraAccessException {
		camera.createCaptureSession(Arrays.asList(surfaces), new CameraCaptureSession.StateCallback() {
			@SuppressWarnings("NullableProblems")
			@Override
			public void onConfigured(CameraCaptureSession session) {
				cameraSession = session;
				try {
					requestRepeating(session, surfaces);
				} catch(CameraAccessException e) {
					e.printStackTrace();
					closeDevice();
				}
			}

			@SuppressWarnings("NullableProblems")
			@Override
			public void onConfigureFailed(CameraCaptureSession session) {
				cameraSession = session;
				closeDevice();
			}
		}, null);
	}

	private void destroySession() {
		if(cameraSession != null) {
			try {
				cameraSession.stopRepeating();
			} catch(CameraAccessException e) {
				e.printStackTrace();
			}
			cameraSession.close();
			cameraSession = null;
		}
	}

	private void requestRepeating(CameraCaptureSession session, Surface... surfaces) throws CameraAccessException {
		final CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
		builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
		for(Surface surface : surfaces) builder.addTarget(surface);
		session.setRepeatingRequest(builder.build(), null, handler);
	}

	private static class CompareSizesByArea implements Comparator<Size> {
		@Override
		public int compare(Size lhs, Size rhs) {
			return Long.signum((long)lhs.getWidth() * lhs.getHeight() - (long)rhs.getWidth() * rhs.getHeight());
		}

	}

	@SuppressWarnings("WeakerAccess")
	public interface OnImageAvailableListener {
		void OnImageAvailable(Image image);
	}
}
