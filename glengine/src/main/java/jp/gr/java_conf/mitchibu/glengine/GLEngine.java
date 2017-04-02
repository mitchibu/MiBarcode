package jp.gr.java_conf.mitchibu.glengine;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

@SuppressWarnings({"WeakerAccess", "unused"})
public class GLEngine implements GLSurfaceView.Renderer {
	private Scene scene = null;
	private Scene nextScene = null;
	private boolean isSurfaceChanged = true;
	private int width = 0;
	private int height = 0;

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public void setScene(final Scene newScene) {
		if(scene == newScene) return;
		nextScene = newScene;
	}

	@Override
	public void onSurfaceCreated(GL10 ignore, EGLConfig config) {
		GLES20.glClearColor(0f, 0f, 0f, 0f);
	}

	@Override
	public void onSurfaceChanged(GL10 ignore, int width, int height) {
		isSurfaceChanged = true;
		this.width = width;
		this.height = height;
	}

	@Override
	public void onDrawFrame(GL10 ignore) {
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

		boolean changeScene = nextScene != null;
		if(changeScene) {
			if(scene != null) scene.release(this);
			nextScene.initialize(this);
			scene = nextScene;
			nextScene = null;
		}

		if(scene != null) {
			if(changeScene || isSurfaceChanged) {
				scene.configure(this);
				isSurfaceChanged = false;
			}
			scene.draw(this);
		}
	}

	public static class Scene {
		private final List<Layer> layers = new ArrayList<>();

		private boolean initialized = false;

		public void addLayer(Layer layer) {
			synchronized(layers) {
				layers.add(layer);
			}
		}

		public void removeLayer(Layer layer) {
			synchronized(layers) {
				layers.remove(layer);
			}
		}

		private boolean initialized() {
			return initialized;
		}

		private void initialize(GLEngine engine) {
			if(initialized) return;
			onPreInitialize(engine);
			synchronized(layers) {
				for(Layer layer : layers) layer.onInitialize(engine);
			}
			initialized = true;
			onPostInitialize(engine);
		}

		private void release(GLEngine engine) {
			if(!initialized) return;
			onPreRelease(engine);
			synchronized(layers) {
				for(Layer layer : layers) layer.onRelease(engine);
			}
			initialized = false;
			onPostRelease(engine);
		}

		private void configure(GLEngine engine) {
			onPreConfigure(engine);
			synchronized(layers) {
				for(Layer layer : layers) layer.onConfigure(engine);
			}
			onPostConfigure(engine);
		}

		private void draw(GLEngine engine) {
			onPreDraw(engine);
			synchronized(layers) {
				for(Layer layer : layers) layer.onDraw(engine);
			}
			onPostDraw(engine);
		}

		protected void onPreInitialize(GLEngine engine) {
		}

		protected void onPostInitialize(GLEngine engine) {
		}

		protected void onPreRelease(GLEngine engine) {
		}

		protected void onPostRelease(GLEngine engine) {
		}

		protected void onPreConfigure(GLEngine engine) {
		}

		protected void onPostConfigure(GLEngine engine) {
		}

		protected void onPreDraw(GLEngine engine) {
		}

		protected void onPostDraw(GLEngine engine) {
		}
	}

	public static class Layer {
		protected void onInitialize(GLEngine engine) {
		}

		protected void onRelease(GLEngine engine) {
		}

		protected void onConfigure(GLEngine engine) {
		}

		protected void onDraw(GLEngine engine) {
		}
	}
}
