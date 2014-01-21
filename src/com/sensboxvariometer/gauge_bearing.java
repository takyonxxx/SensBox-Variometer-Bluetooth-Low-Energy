/* Copyright (C) Türkay Biliyor 
   turkaybiliyor@hotmail.com */
package com.sensboxvariometer;
import com.sensboxvariometer.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
public class gauge_bearing extends View {

	private static final String TAG = "BEARING";
		// drawing tools
	private float currentheading=0;
	private RectF rimRect;
	private Paint rimPaint;
	private Paint rimCirclePaint;
	
	private RectF faceRect;
	private Bitmap faceTexture;
	private Paint facePaint;
	private Paint rimShadowPaint;
	
	private Paint scalePaint;
	private RectF scaleRect;
	
	private Paint titlePaint;	
	private Path titlePath;

	private Paint logoPaint;
	private Bitmap logo;
	private Matrix logoMatrix;
	private float logoScale;
	
	private Paint handPaint;
	private Path handPath;
	private Paint handScrewPaint;
	
	private Paint backgroundPaint; 
	// end drawing tools
	
	private Bitmap background; // holds the cached static part
	
	// scale configuration
	private static final int totalNicks = 180;
	private static final float degreesPerNick = 360.0f / totalNicks;	
	private static final int centerDegree = 0; // the one in the top center (12 o'clock)
	private static final int minDegrees =-360;
	private static final int maxDegrees = 360;
	
	// hand dynamics -- all are angular expressed in F degrees
	private boolean handInitialized = false;
	private float handPosition = centerDegree;
	private float handTarget = centerDegree;
	private float handVelocity = 0.0f;
	private float handAcceleration = 0.0f;
	private long lastHandMoveTime = -1L;	
	private Canvas backgroundCanvas;
	private float roseangle=0;
	public gauge_bearing(Context context) {
		super(context);
		init();
	}

	public gauge_bearing(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public gauge_bearing(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}
	
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();		
		setHandTarget(0);
	}
	public void setval(float val) {		
		setHandTarget(val);	
		currentheading=val;
	}
	
	public void rotaterose(float val) {	
		roseangle=val;
	}
	@Override
	protected void onDetachedFromWindow() {		
		super.onDetachedFromWindow();
	}
	
	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		Bundle bundle = (Bundle) state;
		Parcelable superState = bundle.getParcelable("superState");
		super.onRestoreInstanceState(superState);
		
		handInitialized = bundle.getBoolean("handInitialized");
		handPosition = bundle.getFloat("handPosition");
		handTarget = bundle.getFloat("handTarget");
		handVelocity = bundle.getFloat("handVelocity");
		handAcceleration = bundle.getFloat("handAcceleration");
		lastHandMoveTime = bundle.getLong("lastHandMoveTime");
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		Parcelable superState = super.onSaveInstanceState();
		
		Bundle state = new Bundle();
		state.putParcelable("superState", superState);
		state.putBoolean("handInitialized", handInitialized);
		state.putFloat("handPosition", handPosition);
		state.putFloat("handTarget", handTarget);
		state.putFloat("handVelocity", handVelocity);
		state.putFloat("handAcceleration", handAcceleration);
		state.putLong("lastHandMoveTime", lastHandMoveTime);
		return state;
	}

	private void init() {
	
		initDrawingTools();
	}

	private String getTitle() {
		return "";
	}

		
	private void initDrawingTools() {
		rimRect = new RectF(0.1f, 0.1f, 0.9f, 0.9f);

		// the linear gradient is a bit skewed for realism
		rimPaint = new Paint();
		rimPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
		rimPaint.setShader(new LinearGradient(0.40f, 0.0f, 0.60f, 1.0f, 
										   Color.rgb(0xf0, 0xf5, 0xf0),
										   Color.rgb(0x30, 0x31, 0x30),
										   Shader.TileMode.CLAMP));		

		rimCirclePaint = new Paint();
		rimCirclePaint.setAntiAlias(true);
		rimCirclePaint.setStyle(Paint.Style.STROKE);
		rimCirclePaint.setColor(Color.argb(0x4f, 0x33, 0x36, 0x33));
		rimCirclePaint.setStrokeWidth(0.003f);

		float rimSize = 0.01f;
		faceRect = new RectF();
		faceRect.set(rimRect.left + rimSize, rimRect.top + rimSize, 
			     rimRect.right - rimSize, rimRect.bottom - rimSize);		

		faceTexture = BitmapFactory.decodeResource(getContext().getResources(), 
				   R.drawable.compass_rose);
		BitmapShader paperShader = new BitmapShader(faceTexture, 
												    Shader.TileMode.MIRROR, 
												    Shader.TileMode.MIRROR);
		Matrix paperMatrix = new Matrix();
		facePaint = new Paint();
		facePaint.setFilterBitmap(true);
		paperMatrix.setScale(1.0f / faceTexture.getWidth(), 
							 1.0f / faceTexture.getHeight());
		paperShader.setLocalMatrix(paperMatrix);
		facePaint.setStyle(Paint.Style.FILL);
		facePaint.setShader(paperShader);

		rimShadowPaint = new Paint();
		rimShadowPaint.setShader(new RadialGradient(0.5f, 0.5f, faceRect.width() / 2.0f, 
				   new int[] { 0x00000000, 0x00000500, 0x50000500 },
				   new float[] { 0.96f, 0.96f, 0.99f },
				   Shader.TileMode.MIRROR));
		rimShadowPaint.setStyle(Paint.Style.FILL);

		scalePaint = new Paint();
		scalePaint.setStyle(Paint.Style.STROKE);
		scalePaint.setColor(Color.BLACK);
		scalePaint.setStrokeWidth(0.000f);
		scalePaint.setAntiAlias(true);
		
		scalePaint.setTextSize(0.060f);
		scalePaint.setTypeface(Typeface.SANS_SERIF);
		scalePaint.setTextScaleX(0.8f);
		scalePaint.setTextAlign(Paint.Align.CENTER);	
		
		float scalePosition = 0.10f;
		scaleRect = new RectF();
		scaleRect.set(faceRect.left + scalePosition, faceRect.top + scalePosition,
					  faceRect.right - scalePosition, faceRect.bottom - scalePosition);

		titlePaint = new Paint();
		titlePaint.setColor(Color.DKGRAY);
		titlePaint.setAntiAlias(true);
		titlePaint.setTypeface(Typeface.DEFAULT_BOLD);
		titlePaint.setTextAlign(Paint.Align.CENTER);
		titlePaint.setTextSize(0.06f);
		titlePaint.setTextScaleX(0.9f);

		titlePath = new Path();
		titlePath.addArc(new RectF(0.24f, 0.24f, 0.76f, 0.76f), -180.0f, -180.0f);

		logoPaint = new Paint();
		logoPaint.setFilterBitmap(true);
		logo = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.logo);
		logoMatrix = new Matrix();
		logoScale = (1.0f / logo.getWidth()) * 0.1f;;
		logoMatrix.setScale(logoScale, logoScale);

		handPaint = new Paint();
		handPaint.setAntiAlias(true);
		//handPaint.setColor(0xff392f2c);	
		handPaint.setColor(Color.GREEN);	
		handPaint.setShadowLayer(0.01f, -0.005f, -0.005f, 0x7f000000);
		handPaint.setStyle(Paint.Style.FILL);			
				
		handPath = new Path();
		handPath.moveTo(0.5f, 0.5f);
		handPath.lineTo(0.5f - 0.020f, 0.5f );
		handPath.lineTo(0.5f - 0.005f, 0.5f - 0.35f);
		handPath.lineTo(0.5f + 0.005f, 0.5f - 0.35f);
		handPath.lineTo(0.5f + 0.020f, 0.5f);
		handPath.lineTo(0.5f, 0.5f);
		handPath.addCircle(0.5f, 0.5f, 0.040f, Path.Direction.CW);
		
		
		handScrewPaint = new Paint();
		handScrewPaint.setAntiAlias(true);
		handScrewPaint.setColor(0xff493f3c);
		handScrewPaint.setStyle(Paint.Style.FILL);
		
		backgroundPaint = new Paint();
		backgroundPaint.setFilterBitmap(true);
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		Log.d(TAG, "Width spec: " + MeasureSpec.toString(widthMeasureSpec));
		Log.d(TAG, "Height spec: " + MeasureSpec.toString(heightMeasureSpec));
		
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);
		
		int chosenWidth = chooseDimension(widthMode, widthSize);
		int chosenHeight = chooseDimension(heightMode, heightSize);
		
		int chosenDimension = Math.min(chosenWidth, chosenHeight);
		
		setMeasuredDimension(chosenDimension, chosenDimension);
	}
	
	private int chooseDimension(int mode, int size) {
		if (mode == MeasureSpec.AT_MOST || mode == MeasureSpec.EXACTLY) {
			return size;
		} else { // (mode == MeasureSpec.UNSPECIFIED)
			return getPreferredSize();
		} 
	}
	
	// in case there is no size specified
	private int getPreferredSize() {
		return 300;
	}

	private void drawRim(Canvas canvas) {
		// first, draw the metallic body
		canvas.drawOval(rimRect, rimPaint);
		// now the outer rim circle
		canvas.drawOval(rimRect, rimCirclePaint);
	}
	
	private void drawFace(Canvas canvas) {		
		canvas.drawOval(faceRect, facePaint);
		// draw the inner rim circle
		canvas.drawOval(faceRect, rimCirclePaint);
		// draw the rim shadow inside the face
		canvas.drawOval(faceRect, rimShadowPaint);
	}

	private void drawScale(Canvas canvas) {
		canvas.drawOval(scaleRect, scalePaint);

		canvas.save(Canvas.MATRIX_SAVE_FLAG);
		for (int i = 0; i < totalNicks; ++i) {			
			if(i==0 || i==45 || i==90 || i==135){
				float y1 = scaleRect.top;
				float y2 = y1 - 0.020f;			
				canvas.drawLine(0.5f, y1, 0.5f, y2, scalePaint);	
			//String valueString = Integer.toString(i*2);
			//canvas.drawText(valueString, 0.5f, y2 - 0.015f, scalePaint);	
			}
			canvas.rotate(degreesPerNick, 0.5f, 0.5f);
		}
		canvas.restore();		
	}
	
	private int nickToDegree(int nick) {
		int rawDegree = ((nick < totalNicks / 2) ? nick : (nick - totalNicks)) * 2;
		int shiftedDegree = rawDegree + centerDegree;
		return shiftedDegree;
	}
	
	private float degreeToAngle(float degree) {
		return (degree - centerDegree) / 2.0f * degreesPerNick;
	}
	
	private void drawTitle(Canvas canvas) {
		String title = getTitle();
		canvas.drawTextOnPath(title, titlePath, 0.0f,0.0f, titlePaint);				
	}
	
	private void drawLogo(Canvas canvas) {
		
	}

	private void drawHand(Canvas canvas) {
		if (handInitialized) {			
			float handAngle = degreeToAngle(handPosition);
			canvas.save(Canvas.MATRIX_SAVE_FLAG);
			canvas.rotate(handAngle, 0.5f, 0.5f);
			canvas.drawPath(handPath, handPaint);
			canvas.restore();			
			canvas.drawCircle(0.5f, 0.5f, 0.01f, handScrewPaint);
		}
	}

	private void drawBackground(Canvas canvas) {
		if (background == null) {
			Log.w(TAG, "Background not created");
		} else {
			canvas.drawBitmap(background, 0, 0, backgroundPaint);
		}
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		drawBackground(canvas);			
		float scale = (float) getWidth();		
		canvas.save(Canvas.MATRIX_SAVE_FLAG);
		canvas.scale(scale, scale);	
		
		drawLogo(canvas);
		drawHand(canvas);
		
		canvas.restore();
	
		if (handNeedsToMove()) {
			moveHand();
		}		
		regenerateBackground(roseangle);
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		Log.d(TAG, "Size changed to " + w + "x" + h);
		
		regenerateBackground(roseangle);
	}
	
	private void regenerateBackground(float angle) {
		// free the old bitmap
		if (background != null) {
			background.recycle();
		}
		float compassheight=findViewById(R.id.gauge_bearing).getHeight();
		float compasswidth=findViewById(R.id.gauge_bearing).getWidth();
		background = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
		backgroundCanvas = new Canvas(background);		
		float scale = (float) getWidth();	
		backgroundCanvas.rotate(angle, compassheight/2, compasswidth/2);	
		backgroundCanvas.scale(scale, scale);	
		drawRim(backgroundCanvas);
		drawFace(backgroundCanvas);
		drawScale(backgroundCanvas);
		drawTitle(backgroundCanvas);		
	}

	private boolean handNeedsToMove() {
		return Math.abs(handPosition - handTarget) > 0.01f;
	}
	
	private void moveHand() {
		if (! handNeedsToMove()) {
			return;
		}	
		
		if (lastHandMoveTime != -1L) {
			long currentTime = System.currentTimeMillis();
			float delta = (currentTime - lastHandMoveTime) / 1000.0f;

			float direction = Math.signum(handVelocity);
			if (Math.abs(handVelocity) < 90.0f) {
				handAcceleration = 5.0f * (handTarget - handPosition);
			} else {
				handAcceleration = 0.0f;
			}
			handPosition += handVelocity * delta;
			handVelocity += handAcceleration * delta;
			if ((handTarget - handPosition) * direction < 0.01f * direction) {
				handPosition = handTarget;
				handVelocity = 0.0f;
				handAcceleration = 0.0f;
				lastHandMoveTime = -1L;
			} else {
				lastHandMoveTime = System.currentTimeMillis();				
			}
			invalidate();
		} else {
			lastHandMoveTime = System.currentTimeMillis();
			moveHand();
		}
	}
		
	private float getRelativePosition() {
		if (handPosition < centerDegree) {
			return - (centerDegree - handPosition) / (float) (centerDegree - minDegrees);
		} else {
			return (handPosition - centerDegree) / (float) (maxDegrees - centerDegree);
		}
	}
	
	public void setHandTarget(float val) {
		if (val < minDegrees) {
			val = minDegrees;
		} else if (val > maxDegrees) {
			val = maxDegrees;
		}			
		handTarget = val;
		handInitialized = true;			
		invalidate();
	}

	public View getView() {
		// TODO Auto-generated method stub
		return this;
	}
}
