package jp.gr.java_conf.mitchibu.mibarcode;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;

import java.util.Arrays;

public class BarcodeOverlayView extends View {
	private final SparseArray<Barcode> barcodeArray = new SparseArray<>();
	private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint textBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Path path = new Path();
	private final RectF bounds = new RectF();
	private final GestureDetector detector;

	private int width = 0;
	private int height = 0;

	private OnItemClickListener onItemClickListener = null;

	public BarcodeOverlayView(Context context, AttributeSet attrs) {
		super(context, attrs);

		int[] attrSet = {
				R.attr.pointColor,
				R.attr.textSize,
				R.attr.textColor,
				R.attr.textBgColor,
		};
		Arrays.sort(attrSet);

		TypedArray a = null;
		try {
			a = context.obtainStyledAttributes(attrs, R.styleable.BarcodeOverlayView);

			int color = a.getColor(Arrays.binarySearch(attrSet, R.attr.pointColor), Color.YELLOW);
			paint.setColor(color);
			paint.setStyle(Paint.Style.FILL);

			color = a.getColor(Arrays.binarySearch(attrSet, R.attr.textColor), Color.BLUE);
			textPaint.setColor(color);
			int size = a.getDimensionPixelSize(Arrays.binarySearch(attrSet, R.attr.textSize), 20);
			textPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, size, getResources().getDisplayMetrics()));

			color = a.getColor(Arrays.binarySearch(attrSet, R.attr.textBgColor), Color.argb(128, 0, 0, 0));
			textBgPaint.setColor(color);
			textBgPaint.setStyle(Paint.Style.FILL);
		} finally {
			if(a != null) a.recycle();
		}

		detector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
			@Override
			public boolean onDown(MotionEvent e) {
				return true;
			}

			@Override
			public boolean onSingleTapUp(MotionEvent e) {
				handleTap(e);
				return true;
			}
		});
	}

	public void setOnItemClickListener(OnItemClickListener listener) {
		onItemClickListener = listener;
	}

	public synchronized void update(Detector.Detections<Barcode> detections) {
		Frame.Metadata metadata = detections.getFrameMetadata();
		width = metadata.getWidth();
		height = metadata.getHeight();

		SparseArray<Barcode> items = detections.getDetectedItems();
		for(int i = 0, n = items.size(); i < n; ++ i) {
			barcodeArray.put(items.keyAt(i), items.valueAt(i));
		}
		postInvalidate();
	}

	public synchronized void reset(Detector.Detections<Barcode> detections) {
		if(detections == null) {
			barcodeArray.clear();
		} else {
			SparseArray<Barcode> items = detections.getDetectedItems();
			for(int i = 0, n = items.size(); i < n; ++ i) {
				barcodeArray.remove(items.keyAt(i));
			}
		}
		postInvalidate();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		boolean rc = detector.onTouchEvent(event);
		return rc || super.onTouchEvent(event);
	}

	@Override
	protected synchronized void onDraw(Canvas canvas) {
		if(width == 0 || height == 0 || barcodeArray.size() == 0) return;

		float sx = (float)canvas.getWidth() / width;
		float sy = (float)canvas.getHeight() / height;
		for(int i = 0, n = barcodeArray.size(); i < n; ++ i) {
			Barcode barcode = barcodeArray.valueAt(i);

			path.reset();
			for(Point point : barcode.cornerPoints) {
				path.addCircle(point.x, point.y, 5, Path.Direction.CCW);
			}

			canvas.save();
			canvas.scale(sx, sy);
			canvas.drawPath(path, paint);
			canvas.restore();

			if(TextUtils.isEmpty(barcode.rawValue)) continue;

			path.computeBounds(bounds, true);
			Paint.FontMetricsInt font = textPaint.getFontMetricsInt();
			float textWidth = textPaint.measureText(barcode.rawValue);
			float left = bounds.centerX() * sx - textWidth / 2;
			float top = bounds.bottom * sy;
			float right = bounds.centerX() * sx + textWidth / 2;
			float bottom = top + font.bottom - font.top;

			drawText(canvas, barcode.rawValue, font, left, top, right, bottom);
		}
	}

	private void drawText(Canvas canvas, String text, Paint.FontMetricsInt font, float left, float top, float right, float bottom) {
		canvas.drawRoundRect(left - 5, top - 5, right + 5, bottom + 5, 4, 4, textBgPaint);
		canvas.drawText(text, left, (bottom + top) / 2 - (font.ascent + font.descent) / 2, textPaint);
	}

	private synchronized void handleTap(MotionEvent event) {
		if(onItemClickListener == null) return;

		float sx = (float)getWidth() / width;
		float sy = (float)getHeight() / height;
		for(int i = 0, n = barcodeArray.size(); i < n; ++ i) {
			Barcode barcode = barcodeArray.valueAt(i);

			path.reset();
			for(Point point : barcode.cornerPoints) {
				path.addCircle(point.x, point.y, 5, Path.Direction.CCW);
			}
			path.computeBounds(bounds, true);

			if(bounds.contains(event.getX() / sx, event.getY() / sy)) {
				onItemClickListener.onItemClick(this, barcodeArray.keyAt(i), barcode);
			}
		}
	}

	public interface OnItemClickListener {
		void onItemClick(View view, int id, Barcode barcode);
	}
}
