package com.example.dancemarathon;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.ImageView;

public class Round extends ImageView {

  public Round(Context context) {
       super(context);
  }

  public Round(Context context, AttributeSet attrs) {
       super(context, attrs);
  }

  public Round(Context context, AttributeSet attrs, int defStyle) {
       super(context, attrs, defStyle);
  }

  @Override
  protected void onDraw(Canvas canvas) {
       float radius = 90.0f; // angle of round corners
       Path clipPath = new Path();
       RectF rect = new RectF(0, 0, this.getWidth(), this.getHeight());
       clipPath.addRoundRect(rect, radius, radius, Path.Direction.CW);
       canvas.clipPath(clipPath);

       super.onDraw(canvas);
   }
}
