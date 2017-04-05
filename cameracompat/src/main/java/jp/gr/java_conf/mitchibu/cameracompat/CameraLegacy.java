package jp.gr.java_conf.mitchibu.cameracompat;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.SurfaceHolder;

import java.io.IOException;

@SuppressWarnings("deprecation")
class CameraLegacy extends CameraCompat {
	private Camera camera = null;

	@SuppressWarnings("UnusedParameters")
	CameraLegacy(Context context, Bundle params, Callback callback) {
		super(context, params, callback);
	}

	@Override
	public void startPreview(SurfaceHolder holder) throws Exception {
		try {
			configure();
			camera.setPreviewDisplay(holder);
			camera.startPreview();
		} catch(IOException e) {
			release();
			throw e;
		}
	}

	@Override
	public void startPreview(SurfaceTexture texture) throws Exception {
		try {
			configure();
			camera.setPreviewTexture(texture);
			camera.startPreview();
		} catch(IOException e) {
			release();
			throw e;
		}
	}

	@Override
	public void release() {
		if(camera == null) return;
		camera.release();
		camera = null;
	}

	private void configure() {
		camera = Camera.open();
		Camera.Parameters params = camera.getParameters();
		params.setPreviewSize(getPreviewWidth(), getPreviewHeight());
		camera.setParameters(params);
		setConfiguredPreviewSize(getPreviewWidth(), getPreviewHeight());
	}
}
