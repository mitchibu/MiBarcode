package jp.gr.java_conf.mitchibu.cameracompat;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
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
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Pair;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@SuppressWarnings("NullableProblems")
class CameraLollipop extends CameraCompat implements ImageReader.OnImageAvailableListener {
	@SuppressWarnings("SuspiciousNameCombination")
	private static Size chooseOptimalSize(Size[] sizes, int width, int height) {
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
	private final HandlerThread thread;
	private final Handler handler;

	private CameraDevice cameraDevice = null;
	private CameraCaptureSession cameraSession = null;
	private CaptureRequest.Builder requestBuilder = null;
	private ImageReader reader = null;

	CameraLollipop(Context context, Bundle params, Callback callback) {
		super(context, params, callback);
		cm = (CameraManager)context.getSystemService(Context.CAMERA_SERVICE);

		thread = new HandlerThread(getClass().getSimpleName());
		thread.start();
		handler = new Handler(thread.getLooper());
	}

	@Override
	public float getMaxZoom() {
		try {
			CameraCharacteristics c = cm.getCameraCharacteristics(cameraDevice.getId());
			Float zoom = c.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
			if(zoom != null) return zoom;
		} catch(Exception e) {
			e.printStackTrace();
		}
		return 1.0f;
	}

	@Override
	public float setZoom(float zoom) {
		try {
			CameraCharacteristics c = cm.getCameraCharacteristics(cameraDevice.getId());
			Float max = c.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
			if(max == null) max = 1.0f;

			Rect region = c.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
			if(region != null) {
				if(zoom < 1.0f) zoom = 1.0f;
				if(zoom > max) zoom = max;
				float width = (float)region.width() / zoom;
				float height = (float)region.height() / zoom;
				region.right = region.left + (int)width;
				region.bottom = region.top + (int)height;
				CaptureRequest.Builder builder = requestBuilder;
				builder.set(CaptureRequest.SCALER_CROP_REGION, region);
				cameraSession.setRepeatingRequest(builder.build(), null, handler);
			}
		} catch(CameraAccessException e) {
			e.printStackTrace();
		}
		return zoom;
	}

	@Override
	public void startPreview(SurfaceHolder holder) throws Exception {
		Pair<String, Size> result = configure();
		if(result == null) {
			fireOnError(-1, null);
		} else {
			holder.setFixedSize(result.second.getWidth(), result.second.getHeight());
			startPreview(result.first, holder.getSurface(), reader.getSurface());
		}
	}

	@Override
	public void startPreview(SurfaceTexture texture) throws Exception {
		Pair<String, Size> result = configure();
		if(result == null) {
			fireOnError(-1, null);
		} else {
			texture.setDefaultBufferSize(result.second.getWidth(), result.second.getHeight());
			startPreview(result.first, new Surface(texture), reader.getSurface());
		}
	}

	@Override
	public void release() {
		android.util.Log.v("test", "release");
		if(cameraSession != null) {
			try {
				cameraSession.stopRepeating();
			} catch(CameraAccessException e) {
				e.printStackTrace();
			}
			cameraSession.close();
			cameraSession = null;
		}
		if(cameraDevice != null) {
			cameraDevice.close();
			cameraDevice = null;
		}
		thread.quit();
	}

	@Override
	public void onImageAvailable(ImageReader imageReader) {
		Image image = null;
		try {
			image = reader.acquireLatestImage();
			if(image == null) return;
			Image.Plane[] planes = image.getPlanes();
			if(planes == null || planes.length == 0) return;
			fireOnReceive(planes[0].getBuffer(), image.getWidth(), image.getHeight());
		} finally {
			if(image != null) image.close();
		}
	}

	private Pair<String, Size> configure() {
		Pair<String, StreamConfigurationMap> result = findCamera(isFrontFace() ? CameraCharacteristics.LENS_FACING_FRONT : CameraCharacteristics.LENS_FACING_BACK);
		if(result == null) return null;

		Size[] sizes = result.second.getOutputSizes(SurfaceTexture.class);
		Size size = chooseOptimalSize(sizes, getPreviewWidth(), getPreviewHeight());
		setConfiguredPreviewSize(size.getWidth(), size.getHeight());
		reader = ImageReader.newInstance(size.getWidth(), size.getHeight(), ImageFormat.YUV_420_888, 2);
		reader.setOnImageAvailableListener(this, handler);
		return new Pair<>(result.first, size);
	}

	private Pair<String, StreamConfigurationMap> findCamera(int facing) {
		try {
			for(String id : cm.getCameraIdList()) {
				CameraCharacteristics c = cm.getCameraCharacteristics(id);
				if(Integer.valueOf(facing).equals(c.get(CameraCharacteristics.LENS_FACING))) {
					return new Pair<>(id, c.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP));
				}
			}
		} catch(CameraAccessException e) {
			e.printStackTrace();
		}
		return null;
	}

	@SuppressWarnings("MissingPermission")
	private void startPreview(final String id, final Surface... surfaces) throws CameraAccessException {
		handler.post(new Runnable() {
			@Override
			public void run() {
				try {
					cm.openCamera(id, new CameraDevice.StateCallback() {
						@Override
						public void onOpened(CameraDevice camera) {
							cameraDevice = camera;
							try {
								camera.createCaptureSession(Arrays.asList(surfaces), new CameraCaptureSession.StateCallback() {
									@Override
									public void onConfigured(CameraCaptureSession session) {
										cameraSession = session;
										try {
											requestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
											if(isEnableAF()) requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
											for(Surface surface : surfaces) requestBuilder.addTarget(surface);
											session.setRepeatingRequest(requestBuilder.build(), null, handler);
										} catch(CameraAccessException e) {
											e.printStackTrace();
											release();
											fireOnError(-1, e);
										}
									}

									@Override
									public void onConfigureFailed(CameraCaptureSession session) {
										cameraSession = session;
										release();
										fireOnError(-1, null);
									}
								}, null);
							} catch(CameraAccessException e) {
								e.printStackTrace();
								release();
								fireOnError(-1, e);
							}
						}

						@Override
						public void onDisconnected(CameraDevice camera) {
							release();
						}

						@Override
						public void onError(CameraDevice camera, int error) {
							release();
							fireOnError(error, null);
						}
					}, null);
				} catch(CameraAccessException e) {
					e.printStackTrace();
					release();
					fireOnError(-1, e);
				}
			}
		});
	}

	private static class CompareSizesByArea implements Comparator<Size> {
		@Override
		public int compare(Size lhs, Size rhs) {
			return Long.signum((long)lhs.getWidth() * lhs.getHeight() - (long)rhs.getWidth() * rhs.getHeight());
		}
	}
}
