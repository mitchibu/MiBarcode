package jp.gr.java_conf.mitchibu.mibarcode;

import android.app.Activity;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.view.Surface;

import jp.gr.java_conf.mitchibu.glengine.GLEngine;

@SuppressWarnings({"deprecation", "WeakerAccess"})
public class BarcodeScene extends GLEngine.Scene {
	private final PreviewLayer previewLayer = new PreviewLayer();
	private final OverlayLayer overlayLayer = new OverlayLayer();
	private final Activity activity;
	private final Camera camera;

	public BarcodeScene(Activity activity, Camera camera) {
		this.activity = activity;
		this.camera = camera;

		if(camera != null) addLayer(previewLayer);
		addLayer(overlayLayer);
		addLayer(new OverlayLayer2());
	}

	@Override
	protected void onPostInitialize(GLEngine engine) {
		if(camera == null) return;
		try {
			camera.setPreviewTexture(previewLayer.getSurfaceTexture());
			Camera.Parameters params = camera.getParameters();
			Camera.Size size = params.getPreferredPreviewSizeForVideo();
			params.setPreviewSize(size.width, size.height);
			camera.setParameters(params);
			camera.startPreview();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("SuspiciousNameCombination")
	@Override
	protected void onPreConfigure(GLEngine engine) {
		if(camera == null) return;
		Camera.Parameters params = camera.getParameters();
		Camera.Size size = getContentSize();
		params.setPreviewSize(size.width, size.height);
		camera.setParameters(params);
		camera.startPreview();

		int contentWidth;
		int contentHeight;
		int rotate = activity.getWindowManager().getDefaultDisplay().getRotation();
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
//		GLES20.glViewport((engine.getWidth() - contentWidth) / 2, (engine.getHeight() - contentHeight) / 2, contentWidth, contentHeight);
		previewLayer.setRotate(rotate);
	}

	private Camera.Size getContentSize() {
		Camera.Parameters params = camera.getParameters();
//		List<Camera.Size> sizes = params.getSupportedPreviewSizes();
		return params.getPreferredPreviewSizeForVideo();
	}
}
