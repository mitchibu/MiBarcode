package jp.gr.java_conf.mitchibu.mibarcode;

import android.graphics.SurfaceTexture;
import android.opengl.GLES20;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;

import jp.gr.java_conf.mitchibu.glengine.GLEngine;

@SuppressWarnings({"deprecation", "WeakerAccess"})
public class BarcodeScene extends GLEngine.Scene {
	private final PreviewLayer previewLayer = new PreviewLayer();
	private final OverlayLayer overlayLayer = new OverlayLayer();
	private final Callback callback;

	public BarcodeScene(Callback callback) {
		this.callback = callback;

		addLayer(previewLayer);
		addLayer(overlayLayer);
	}

	public void setViewport(int x, int y, int width, int height) {
		GLES20.glViewport(x, y, width, height);
	}

	public void setRotate(int rotate) {
		previewLayer.setRotate(rotate);
	}

	public void setContentSize(int width, int height) {
		previewLayer.setContentSize(width, height);
	}

	public synchronized void update(Detector.Detections<Barcode> detections) {
		overlayLayer.update(detections);
	}

	public synchronized void reset(Detector.Detections<Barcode> detections) {
		overlayLayer.reset(detections);
	}

		@Override
	protected void onPostInitialize(GLEngine engine) {
		if(callback != null) callback.onInitialized(previewLayer.getSurfaceTexture());
	}

	@SuppressWarnings("SuspiciousNameCombination")
	@Override
	protected void onPreConfigure(GLEngine engine) {
		if(callback != null) callback.onConfigured(engine.getWidth(), engine.getHeight());
	}

	public interface Callback {
		void onInitialized(SurfaceTexture surfaceTexture);
		void onConfigured(int width, int height);
	}
}
