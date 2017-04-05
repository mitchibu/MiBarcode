package jp.gr.java_conf.mitchibu.cameracompat;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.view.SurfaceHolder;

import java.nio.ByteBuffer;

public abstract class CameraCompat {
	private static final String PARAMS_FACE_FRONT = CameraCompat.class.getName() + ".params.FACE_FRONT";
	private static final String PARAMS_PREVIEW_WIDTH = CameraCompat.class.getName() + ".params.PREVIEW_WIDTH";
	private static final String PARAMS_PREVIEW_HEIGHT = CameraCompat.class.getName() + ".params.PREVIEW_HEIGHT";
	private static final String PARAMS_ENABLE_AF = CameraCompat.class.getName() + ".params.ENABLE_AF";

	private final Context context;
	private final Bundle params;
	private final Callback callback;

	private int previewWidth = 0;
	private int previewHeight = 0;

	CameraCompat(Context context, Bundle params, Callback callback) {
		this.context = context;
		this.params = params;
		this.callback = callback;
	}

	public int getConfiguredPreviewWidth() {
		return previewWidth;
	}

	public int getConfiguredPreviewHeight() {
		return previewHeight;
	}

	public abstract void startPreview(SurfaceHolder holder) throws Exception;
	public abstract void startPreview(SurfaceTexture texture) throws Exception;
	public abstract void release();

	Context getContext() {
		return context;
	}

	boolean isFrontFace() {
		return params.getBoolean(PARAMS_FACE_FRONT, false);
	}

	int getPreviewWidth() {
		return params.getInt(PARAMS_PREVIEW_WIDTH, 0);
	}

	int getPreviewHeight() {
		return params.getInt(PARAMS_PREVIEW_HEIGHT, 0);
	}

	boolean isEnableAF() {
		return params.getBoolean(PARAMS_ENABLE_AF, false);
	}

	void setConfiguredPreviewSize(int width, int height) {
		previewWidth = width;
		previewHeight = height;
	}

	void fireOnReceive(ByteBuffer buffer, int width, int height) {
		if(callback == null) return;
		callback.onReceive(buffer, width, height);
	}

	void fireOnError(int code, Exception e) {
		if(callback == null) return;
		callback.onError(code, e);
	}

	public interface Callback {
		void onReceive(ByteBuffer buffer, int width, int height);
		void onError(int code, Exception e);
	}

	public static class Builder {
		private final Bundle params = new Bundle();
		private final Context context;
		private final boolean legacy;
		private final Callback callback;

		public Builder(Context context, boolean legacy, Callback callback) {
			this.context = context;
			this.legacy = legacy;
			this.callback = callback;
		}

		public Builder setFaceFront(boolean front) {
			params.putBoolean(PARAMS_FACE_FRONT, front);
			return this;
		}

		public Builder setPreviewWidth(int width) {
			params.putInt(PARAMS_PREVIEW_WIDTH, width);
			return this;
		}

		public Builder setPreviewHeight(int height) {
			params.putInt(PARAMS_PREVIEW_HEIGHT, height);
			return this;
		}

		public Builder setEnableAF(boolean enable) {
			params.putBoolean(PARAMS_ENABLE_AF, enable);
			return this;
		}

		public CameraCompat build() {
			return legacy ? new CameraLegacy(context, params, callback) : new CameraLollipop(context, params, callback);
		}
	}
}
