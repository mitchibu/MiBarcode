package jp.gr.java_conf.mitchibu.glengine.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

@SuppressWarnings({"WeakerAccess", "unused"})
public class BufferUtils {
	public static final int BYTES_SHORT = Short.SIZE / Byte.SIZE;
	public static final int BYTES_INT = Integer.SIZE / Byte.SIZE;
	public static final int BYTES_FLOAT = Float.SIZE / Byte.SIZE;

	public static ByteBuffer createByteBuffer(int size) {
		return ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
	}

	public static ByteBuffer wrapByteBuffer(byte[] array) {
		ByteBuffer b = createByteBuffer(array.length);
		b.put(array);
		b.position(0);
		return b;
	}

	public static ShortBuffer createShortBuffer(int size) {
		return ByteBuffer.allocateDirect(size * BYTES_SHORT).order(ByteOrder.nativeOrder()).asShortBuffer();
	}

	public static ShortBuffer wrapShortBuffer(short[] array) {
		ShortBuffer b = createShortBuffer(array.length);
		b.put(array);
		b.position(0);
		return b;
	}

	public static IntBuffer createIntBuffer(int size) {
		return ByteBuffer.allocateDirect(size * BYTES_INT).order(ByteOrder.nativeOrder()).asIntBuffer();
	}

	public static IntBuffer wrapIntBuffer(int[] array) {
		IntBuffer b = createIntBuffer(array.length);
		b.put(array);
		b.position(0);
		return b;
	}

	public static FloatBuffer createFloatBuffer(int size) {
		return ByteBuffer.allocateDirect(size * BYTES_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer();
	}

	public static FloatBuffer wrapFloatBuffer(float[] array) {
		FloatBuffer b = createFloatBuffer(array.length);
		b.put(array);
		b.position(0);
		return b;
	}
}
