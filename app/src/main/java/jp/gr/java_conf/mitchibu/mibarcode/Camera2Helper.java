package jp.gr.java_conf.mitchibu.mibarcode;

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.os.Handler;
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

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
@SuppressWarnings("unused")
public class Camera2Helper {
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

	private final CameraManager cm;
	private final Handler handler;
	private Semaphore cameraOpenCloseLock = new Semaphore(1);

	private CameraDevice cameraDevice = null;
	private CameraCaptureSession cameraSession = null;

	public Camera2Helper(Context context, Handler handler) {
		cm = (CameraManager)context.getSystemService(Context.CAMERA_SERVICE);
		this.handler = handler;
	}

	public Pair<String, StreamConfigurationMap> findCamera(int facing) {
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

	public void start(String id, Surface... surfaces) {
		try {
			openDevice(id, surfaces);
		} catch(CameraAccessException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void stop() {
		closeDevice();
	}

	@SuppressWarnings("MissingPermission")
	private void openDevice(String id, final Surface... surfaces) throws CameraAccessException, InterruptedException {
		if(!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
			throw new RuntimeException("Time out waiting to lock camera opening.");
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
}
