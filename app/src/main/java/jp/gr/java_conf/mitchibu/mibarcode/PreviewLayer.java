package jp.gr.java_conf.mitchibu.mibarcode;

import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import java.nio.FloatBuffer;

import jp.gr.java_conf.mitchibu.glengine.GLEngine;
import jp.gr.java_conf.mitchibu.glengine.util.BufferUtils;

@SuppressWarnings("WeakerAccess")
public class PreviewLayer extends GLEngine.Layer implements SurfaceTexture.OnFrameAvailableListener {
	private static final String vss =
			"attribute vec2 vPosition;\n" +
					"attribute vec2 vTexCoord;\n" +
					"varying vec2 texCoord;\n" +
					"void main() {\n" +
					"  texCoord = vTexCoord;\n" +
					"  gl_Position = vec4 ( vPosition.x, vPosition.y, 0.0, 1.0 );\n" +
					"}";

	private static final String fss =
			"#extension GL_OES_EGL_image_external : require\n" +
					"precision mediump float;\n" +
					"uniform samplerExternalOES sTexture;\n" +
					"varying vec2 texCoord;\n" +
					"void main() {\n" +
					"  gl_FragColor = texture2D(sTexture,texCoord);\n" +
					"}";
	private static final int DIMENSION = 2;
	private static final int COUNT = 4;

	private final int[] texture = new int[1];
	private final int[] buffer = new int[1];

	private int program = 0;
	private SurfaceTexture surfaceTexture = null;
	private boolean isUpdateTexture = false;
	private int rotate = 0;

	public SurfaceTexture getSurfaceTexture() {
		return surfaceTexture;
	}

	public void setRotate(int rotate) {
		this.rotate = rotate;
	}

	@Override
	protected void onInitialize(GLEngine engine) {
		FloatBuffer fb = BufferUtils.wrapFloatBuffer(new float[] {
				// vertex
//				-1.0f, 1.0f,
//				-1.0f, -1.0f,
//				1.0f, 1.0f,
//				1.0f, -1.0f,
				-0.4f, 0.4f,
				-0.4f, -0.4f,
				0.4f, 0.4f,
				0.4f, -0.4f,

				// texcode
				// 0
				0.0f, 1.0f,
				1.0f, 1.0f,
				0.0f, 0.0f,
				1.0f, 0.0f,

				// 90
				0.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 0.0f,
				1.0f, 1.0f,

				// 180
				1.0f, 0.0f,
				0.0f, 0.0f,
				1.0f, 1.0f,
				0.0f, 1.0f,

				// 270
				1.0f, 1.0f,
				1.0f, 0.0f,
				0.0f, 1.0f,
				0.0f, 0.0f
		});
		GLES20.glGenBuffers(buffer.length, buffer, 0);
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffer[0]);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, fb.capacity() * BufferUtils.BYTES_FLOAT, fb, GLES20.GL_STATIC_DRAW);

		GLES20.glGenTextures(texture.length, texture, 0);
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0]);
		GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
		GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

		surfaceTexture = new SurfaceTexture(texture[0]);
		surfaceTexture.setOnFrameAvailableListener(this);

		int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vss);
		int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fss);
		program = GLES20.glCreateProgram();
		GLES20.glAttachShader(program, vertexShader);
		GLES20.glAttachShader(program, fragmentShader);
		GLES20.glLinkProgram(program);
	}

	@Override
	protected void onDraw(GLEngine engine) {
		synchronized(this) {
			if(surfaceTexture != null && isUpdateTexture) {
				surfaceTexture.updateTexImage();
				isUpdateTexture = false;
			}
		}

		GLES20.glUseProgram(program);

		int ph = GLES20.glGetAttribLocation(program, "vPosition");
		int tch = GLES20.glGetAttribLocation (program, "vTexCoord" );
		int th = GLES20.glGetUniformLocation (program, "sTexture" );

		GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0]);
		GLES20.glUniform1i(th, 0);

		GLES20.glEnableVertexAttribArray(ph);
		GLES20.glEnableVertexAttribArray(tch);
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffer[0]);
		GLES20.glVertexAttribPointer(ph, DIMENSION, GLES20.GL_FLOAT, false, 0, 0);
		GLES20.glVertexAttribPointer(tch, DIMENSION, GLES20.GL_FLOAT, false, 0, BufferUtils.BYTES_FLOAT * COUNT * DIMENSION * (rotate + 1));
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, COUNT);

		GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
	}

	@Override
	public void onFrameAvailable(SurfaceTexture surfaceTexture) {
		isUpdateTexture = true;
	}

	private static int loadShader(int mode, String code) {
		int shader = GLES20.glCreateShader(mode);
		GLES20.glShaderSource(shader, code);
		GLES20.glCompileShader(shader);
		int[] compiled = new int[1];
		GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
		if(compiled[0] == 0) {
			android.util.Log.e("Shader", "Could not compile shader:" + GLES20.glGetShaderInfoLog(shader));
			GLES20.glDeleteShader(shader);
			shader = 0;
		}
		return shader;
	}
}
