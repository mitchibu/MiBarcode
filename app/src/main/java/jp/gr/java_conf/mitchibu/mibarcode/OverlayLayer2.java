package jp.gr.java_conf.mitchibu.mibarcode;

import android.opengl.GLES20;

import java.nio.FloatBuffer;

import jp.gr.java_conf.mitchibu.glengine.GLEngine;
import jp.gr.java_conf.mitchibu.glengine.util.BufferUtils;

@SuppressWarnings("WeakerAccess")
public class OverlayLayer2 extends GLEngine.Layer {
	private static final String vss =
			"attribute vec2 vPosition;\n" +
					"void main() {\n" +
					"  gl_Position = vec4( vPosition.x, vPosition.y, 0.0, 1.0 );\n" +
					"}";

	private static final String fss =
					"precision mediump float;\n" +
					"void main() {\n" +
					"  gl_FragColor = vec4(1.0, 1.0, 0.0, 1.0);\n" +
					"}";
	private static final int DIMENSION = 2;
	private static final int COUNT = 4;

	private int program = 0;

	@Override
	protected void onInitialize(GLEngine engine) {
		int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vss);
		int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fss);
		program = GLES20.glCreateProgram();
		GLES20.glAttachShader(program, vertexShader);
		GLES20.glAttachShader(program, fragmentShader);
		GLES20.glLinkProgram(program);
	}

	FloatBuffer fb = BufferUtils.wrapFloatBuffer(new float[] {
			-0.2f, 0.2f,
			-0.2f, -0.2f,
			0.2f, 0.2f,
			0.2f, -0.2f,
	});
	@Override
	protected void onDraw(GLEngine engine) {
		GLES20.glUseProgram(program);

		int ph = GLES20.glGetAttribLocation(program, "vPosition");
		GLES20.glEnableVertexAttribArray(ph);

		GLES20.glVertexAttribPointer(ph, DIMENSION, GLES20.GL_FLOAT, false, 0, fb);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, COUNT);

		GLES20.glDisableVertexAttribArray(ph);
		GLES20.glUseProgram(0);
	}

	@Override
	protected void onConfigure(GLEngine engine) {
//		GLES20.glViewport(0, 0, engine.getWidth(), engine.getHeight());
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
