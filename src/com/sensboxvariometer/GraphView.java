/* Copyright (C) Türkay Biliyor 
   turkaybiliyor@hotmail.com */
package com.sensboxvariometer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class GraphView extends View {

	private Bitmap mBitmap;
	private Paint mPaint = new Paint();
	private Canvas mCanvas = new Canvas();

	private float mSpeed = 1.0f;
	private float mLastX;
	private float mScale;
	private float mLastValue;
	private float mYOffset;	
	private float mWidth;
	private float maxValue = 0;
	private boolean startpoint=false,refreshmaxalt=false;

	public GraphView(Context context) {
		super(context);
		init();
	}

	public GraphView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	private void init() {		
		mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);			
	}
	public float newX ,v; 
	public void addDataPoint(double alt,double max,float graphspeed) {		
		try{
		if(!refreshmaxalt || maxValue ==0){	
		mSpeed=graphspeed;		
		maxValue = (float) max;
		mScale = -(mYOffset * (1.0f / maxValue));	
		final float middle = mYOffset + (maxValue/2) * mScale;		
		mPaint.setStrokeWidth(0);
		mPaint.setColor(Color.WHITE);
		mCanvas.drawLine(0, middle, mWidth, middle, mPaint);	
		
		    float height1=mYOffset;
	   	    float height2=mYOffset + (maxValue * mScale);
	   	    float height=height2-height1;
		DrawGrid(mCanvas,mWidth,height,height1);
		
		refreshmaxalt=true;
		}
		final Paint paint = mPaint;
		if(alt < 6)
			paint.setColor(Color.RED);
		else 
			paint.setColor(Color.YELLOW);	
		paint.setStrokeWidth(2);
		newX = mLastX + mSpeed;
		v = (float) (mYOffset + alt * mScale);		
		if(!startpoint)
		{
			v = (float) (mYOffset + 6 * mScale);		
			mLastValue =v;			
			startpoint=true;
		}
	    mCanvas.drawLine(mLastX, mLastValue, newX, v, paint);
		mLastValue = v;
		mLastX += mSpeed;

		invalidate();
		}catch(Exception e){}
	}
	public void setSpeed(float speed) {
		mSpeed = speed;
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {	
		try{
		mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
		mCanvas.setBitmap(mBitmap);
		mCanvas.drawColor(Color.BLACK);
		mYOffset = h;
		mScale = -(mYOffset * (1.0f / maxValue));
		mWidth = w;
		mLastX = mWidth;			
		super.onSizeChanged(w, h, oldw, oldh);
		}catch(Exception e){}
	}
	public void DrawGrid(Canvas canvas,float width,float height,float startpoint)
	{
		try{
		Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);       
        int pass = 0;
        int xpos = 0;
        int ypos = 0;
        // make the entire canvas white
        paint.setColor(Color.TRANSPARENT);        
        canvas.drawPaint(paint);      
        xpos = (int) (width / 10);
        ypos = (int) (height/4);
        paint.setColor(Color.DKGRAY);
        for (int i = 0; i <10; i++) { 
            canvas.drawLine(xpos +(xpos*i), 0, xpos +(xpos*i), startpoint, paint);   
        }      
        for (int i = 0; i <4; i++) { 
             canvas.drawLine(0,startpoint+ ypos +(ypos*i),width,startpoint+ ypos +(ypos*i), paint);	
        }   
        canvas.drawLine(0,startpoint-1,width,startpoint-1, paint);	
		}catch(Exception e){}
       
	}
	@Override
	protected void onDraw(Canvas canvas) {
		synchronized (this) {
			try{
			if (mBitmap != null) {							
				if (mLastX >= mWidth) {					
					mLastX = 0;
					refreshmaxalt=false;
					final Canvas cavas = mCanvas;
					cavas.drawColor(Color.BLACK);	
				}
				canvas.drawBitmap(mBitmap, 0, 0, null);				
			}
		}catch(Exception e){}
		}
	}
}
