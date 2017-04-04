package jp.gr.java_conf.mitchibu.mibarcode;

import android.graphics.Point;
import android.opengl.GLES20;
import android.util.SparseArray;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;

import java.nio.FloatBuffer;
import java.util.ArrayList;

import jp.gr.java_conf.mitchibu.glengine.GLEngine;
import jp.gr.java_conf.mitchibu.glengine.util.BufferUtils;

@SuppressWarnings("WeakerAccess")
public class OverlayLayer extends GLEngine.Layer {
	private static final String vss =
			"attribute vec2 vPosition;\n" +
					"void main() {\n" +
					"  gl_Position = vec4( vPosition.x, vPosition.y, 0.0, 1.0 );\n" +
					"   gl_PointSize = 100.0;" +
					"}";

	private static final String fss =
					"precision mediump float;\n" +
					"void main() {\n" +
					"  gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0);\n" +
					"}";
	private static final int DIMENSION = 2;
	private static final int COUNT = 4;

	private final SparseArray<Barcode> barcodeArray = new SparseArray<>();

	private int program = 0;
	int width;
	int height;

	public synchronized void update(Detector.Detections<Barcode> detections) {
		synchronized(barcodeArray) {
			Frame.Metadata metadata = detections.getFrameMetadata();
			width = metadata.getWidth();
			height = metadata.getHeight();

			SparseArray<Barcode> items = detections.getDetectedItems();
			for(int i = 0, n = items.size(); i < n; ++i) {
				barcodeArray.put(items.keyAt(i), items.valueAt(i));
			}
		}
	}

	public synchronized void reset(Detector.Detections<Barcode> detections) {
		synchronized(barcodeArray) {
			if(detections == null) {
				barcodeArray.clear();
			} else {
				SparseArray<Barcode> items = detections.getDetectedItems();
				for(int i = 0, n = items.size(); i < n; ++i) {
					barcodeArray.remove(items.keyAt(i));
				}
			}
		}
	}

	@Override
	protected void onInitialize(GLEngine engine) {
		int vertexShader = GLEngine.loadShader(GLES20.GL_VERTEX_SHADER, vss);
		int fragmentShader = GLEngine.loadShader(GLES20.GL_FRAGMENT_SHADER, fss);
		program = GLES20.glCreateProgram();
		GLES20.glAttachShader(program, vertexShader);
		GLES20.glAttachShader(program, fragmentShader);
		GLES20.glLinkProgram(program);
	}

	FloatBuffer fb = BufferUtils.wrapFloatBuffer(new float[] {
			0, 0,
			-0.8f, 0.8f,
			-0.8f, -0.8f,
			0.8f, 0.8f,
			0.8f, -0.8f,
	});
	@Override
	protected void onDraw(GLEngine engine) {
//		GLES20.glViewport((engine.getWidth()- width) / 2, (engine.getHeight() - height) / 2, width, height);
//		GLES20.glViewport(0, 0, width, height);
		GLES20.glViewport(0, 0, engine.getWidth(), engine.getHeight());
		ArrayList<Float> array;
		synchronized(barcodeArray) {
			array = new ArrayList<>();
			float sx = (float)engine.getWidth() / width;
			float sy = (float)engine.getHeight() / height;
			for(int i = 0, n = barcodeArray.size(); i < n; ++i) {
				Barcode barcode = barcodeArray.valueAt(i);
				for(Point point : barcode.cornerPoints) {
					float x = (float)point.x * sx / width;
					float y = (float)point.y * sy / height;
					array.add(x - x / 2);
					array.add(-(y - y / 2));
				}
			}
		}
		fb = BufferUtils.createFloatBuffer(array.size());
		for(Float f : array) {
			fb.put(f);
		}
		fb.position(0);

		GLES20.glUseProgram(program);

		int ph = GLES20.glGetAttribLocation(program, "vPosition");
		GLES20.glEnableVertexAttribArray(ph);

		GLES20.glVertexAttribPointer(ph, DIMENSION, GLES20.GL_FLOAT, false, 0, fb);
//		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, COUNT);
		GLES20.glDrawArrays(GLES20.GL_POINTS, 0, fb.capacity() / 2);

		GLES20.glDisableVertexAttribArray(ph);
		GLES20.glUseProgram(0);
	}
}
