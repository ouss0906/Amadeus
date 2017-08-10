package com.suneku.amadeus;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Shader;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import java.lang.reflect.Field;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

class DrawLaunchActivityCanvas extends SurfaceView implements Runnable {

    Thread thread, loader;
    SurfaceHolder surfaceHolder;
    volatile boolean running = false;
    private Paint mPaint;
    private int mWidth, mHeight;
    private Resources res;
    private BlockingQueue<Bitmap> queue = new LinkedBlockingQueue<>();

    private int getResId(String resName) {
        try {
            Class res = R.drawable.class;
            Field field = res.getField(resName);
            return field.getInt(null);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    private Bitmap loadFrameFromDrawable(int id) {
        String imgName = "logo" + Integer.toString(id);
        return BitmapFactory.decodeResource(res, getResId(imgName));
    }

    public DrawLaunchActivityCanvas(Context context) {
        super(context);
        this.res = context.getResources();

        surfaceHolder = getHolder();

        loader = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 1; i < 40; i++) {
                    try {
                        while (queue.size() > 3) {
                            TimeUnit.MILLISECONDS.sleep(15);
                        }
                        queue.add(loadFrameFromDrawable(i));
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        });
        thread = new Thread(this);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setShader(new LinearGradient(0, 0, 0, mHeight + 600, Color.parseColor("#11110c"), Color.parseColor("#231e00"), Shader.TileMode.CLAMP));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        mWidth = View.MeasureSpec.getSize(widthMeasureSpec);
        mHeight = View.MeasureSpec.getSize(heightMeasureSpec);

        setMeasuredDimension(mWidth, mHeight);
    }

    public void onResumeMySurfaceView(){
        running = true;
        queue = new LinkedBlockingQueue<>();
        counter = 0;
        loader.start();
        thread.start();
    }

    public void onPauseMySurfaceView() {
        boolean retry = true;
        running = false;
        while (retry) {
            try {
                thread.join();
                loader.interrupt();
                retry = false;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private int counter = 0;

    public void run() {
        while (running) {
            try {
                TimeUnit.MILLISECONDS.sleep(25);
            } catch (InterruptedException e) {
                running = false;
                return;
            }
            if (surfaceHolder.getSurface().isValid()) {
                surfaceHolder.setFormat(PixelFormat.TRANSPARENT);
                Canvas canvas = surfaceHolder.lockCanvas();

                canvas.save();
                canvas.rotate(135);
                canvas.drawPaint(mPaint);
                canvas.restore();

                Bitmap logo;

                try {
                    logo = queue.take();
                } catch (InterruptedException e) {
                    running = false;
                    return;
                }

                int cx = (mWidth - logo.getWidth()) >> 1;

                canvas.drawBitmap(logo, cx, 50, null);

                surfaceHolder.unlockCanvasAndPost(canvas);

                counter++;

                if (counter == 39) {
                    running = false;
                    counter = 0;
                }

            }
        }
    }
}