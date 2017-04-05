package jp.gr.java_conf.mitchibu.mibarcode;

import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import java.nio.FloatBuffer;

import jp.gr.java_conf.mitchibu.glengine.GLEngine;
import jp.gr.java_conf.mitchibu.glengine.util.BufferUtils;
import jp.gr.java_conf.mitchibu.mibarcode.test.GLES20FramebufferObject;
import jp.gr.java_conf.mitchibu.mibarcode.test.GLES20Shader;

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

	private final int[] texture = new int[2];
	private final int[] buffer = new int[1];

	private int program = 0;
	private SurfaceTexture surfaceTexture = null;
	private boolean isUpdateTexture = false;
	private int rotate = 0;
	private int width = 0;
	private int height = 0;

	private GLES20FramebufferObject mFramebufferObject = null;
	private GLES20Shader mShader;

	public SurfaceTexture getSurfaceTexture() {
		return surfaceTexture;
	}

	public void setRotate(int rotate) {
		this.rotate = rotate;
	}

	public void setContentSize(int width, int height) {
		this.width = width;
		this.height = height;
	}

	@Override
	protected void onInitialize(GLEngine engine) {
		mFramebufferObject = new GLES20FramebufferObject();
		mShader = new GLES20Shader();
		mShader.setup();

		FloatBuffer fb = BufferUtils.wrapFloatBuffer(new float[] {
				// vertex
				-1.0f, 1.0f,
				-1.0f, -1.0f,
				1.0f, 1.0f,
				1.0f, -1.0f,
//				-0.4f, 0.4f,
//				-0.4f, -0.4f,
//				0.4f, 0.4f,
//				0.4f, -0.4f,

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
		GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
		GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0]);
		GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
		GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

		surfaceTexture = new SurfaceTexture(texture[0]);
		surfaceTexture.setOnFrameAvailableListener(this);

		int vertexShader = GLEngine.loadShader(GLES20.GL_VERTEX_SHADER, vss);
		int fragmentShader = GLEngine.loadShader(GLES20.GL_FRAGMENT_SHADER, fss);
		program = GLES20.glCreateProgram();
		GLES20.glAttachShader(program, vertexShader);
		GLES20.glAttachShader(program, fragmentShader);
		GLES20.glLinkProgram(program);
	}

	int[] frameBuffer = new int[1];
	int[] renderBuffer = new int[1];
	@Override
	protected void onConfigure(GLEngine engine) {
		int width = this.width;//engine.getWidth();
		int height = this.height;//engine.getHeight();
//		float scale = Math.min((float)engine.getWidth() / this.width, (float)engine.getHeight() / this.height);
//		int width = (int)((float)this.width * scale);
//		int height = (int)((float)this.height * scale);
		if(mFramebufferObject != null) {
			mFramebufferObject.setup(width, height);
		} else {
			GLES20.glGenFramebuffers(frameBuffer.length, frameBuffer, 0);
			GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer[0]);

			GLES20.glGenRenderbuffers(renderBuffer.length, renderBuffer, 0);
			GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, renderBuffer[0]);
			// レンダーバッファの幅と高さを指定します。
			GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, width, height);
			// フレームバッファのアタッチメントとしてレンダーバッファをアタッチします。
			GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, renderBuffer[0]);

			GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[1]);
//			GLES20Utils.setupSampler(GLES20.GL_TEXTURE_2D, GLES20.GL_LINEAR, GLES20.GL_NEAREST);
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
			GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
			// フレームバッファのアタッチメントとして 2D テクスチャをアタッチします。
			GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, texture[1], 0);

			// フレームバッファが完全かどうかチェックします。
			final int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
			if(status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
				throw new RuntimeException("Failed to initialize framebuffer object " + status);
			}
		}
		// 何もやってない
//		mShader.setFrameSize(width, height);
	}

	@Override
	protected void onDraw(GLEngine engine) {
		GLES20.glViewport(0, 0, width, height);
//		adjustViewport(engine.getWidth(), engine.getHeight());
//		GLES20.glViewport(0, 0, engine.getWidth(), engine.getHeight());
		if(mFramebufferObject != null) {
			mFramebufferObject.enable();
		} else {
			GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer[0]);
		}
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
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

		GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
		GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0]);
		GLES20.glUniform1i(th, 0);

		GLES20.glEnableVertexAttribArray(ph);
		GLES20.glEnableVertexAttribArray(tch);
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffer[0]);
		GLES20.glVertexAttribPointer(ph, DIMENSION, GLES20.GL_FLOAT, false, 0, 0);
		GLES20.glVertexAttribPointer(tch, DIMENSION, GLES20.GL_FLOAT, false, 0, BufferUtils.BYTES_FLOAT * COUNT * DIMENSION * (rotate + 1));
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, COUNT);

		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
		adjustViewport(engine.getWidth(), engine.getHeight());
//		GLES20.glViewport(0, 0, engine.getWidth(), engine.getHeight());
//		GLES20.glViewport(0, 0, width, height);
		if(mFramebufferObject != null) {
			mShader.draw(mFramebufferObject.getTexName());
		} else {
			mShader.draw(texture[1]);
		}
	}

	@Override
	public void onFrameAvailable(SurfaceTexture surfaceTexture) {
		isUpdateTexture = true;
	}

	private void adjustViewport(int width, int height) {
		float scale = Math.max((float)width / this.width, (float)height / this.height);
		int contentWidth = (int)((float)this.width * scale);
		int contentHeight = (int)((float)this.height * scale);
		GLES20.glViewport((width - contentWidth) / 2, (height - contentHeight) / 2, contentWidth, contentHeight);
//		GLES20.glViewport(0, 0, contentWidth, contentHeight);
	}
}
