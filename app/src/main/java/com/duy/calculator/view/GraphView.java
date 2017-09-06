package com.duy.calculator.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.os.Build;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import com.duy.calculator.R;
import com.duy.math.Point;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class GraphView extends View {
    private static final boolean DEBUG = false;

    private static final int LINES = 1;
    private static final int DOTS = 2;
    private static final int CURVES = 3;

    private static final int GRID_WIDTH = 2;
    private static final int AXIS_WIDTH = 4;
    private static final int GRAPH_WIDTH = 6;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private static final int BOX_STROKE = 6;
    private final List<PanListener> mPanListeners = new ArrayList<>();
    private final List<ZoomListener> mZoomListeners = new ArrayList<>();
    private final Rect mTempRect = new Rect();
    private int mDrawingAlgorithm = LINES;
    private DecimalFormat mFormat = new DecimalFormat("#.#");
    private Paint mBackgroundPaint;
    private Paint mTextPaint;
    private Paint mAxisPaint;
    private Paint mGraphPaint;
    private Paint mDebugPaint;
    private int mOffsetX;
    private int mOffsetY;
    private int mLineMargin;
    private int mMinLineMargin;
    private int mTextPaintSize;
    private int mTextMargin;
    private float mZoomLevel = 1;
    private List<Graph> mData;

    private float mStartX;
    private float mStartY;
    private int mDragOffsetX;
    private int mDragOffsetY;
    private int mDragRemainderX;
    private int mDragRemainderY;
    private int mTouchSlop;

    private int mRemainderX;
    private int mRemainderY;
    private double mZoomInitDistance;
    private float mZoomInitLevel;
    private int mMode;
    private int mPointers;
    private boolean mShowGrid = true;
    private boolean mShowAxis = true;
    private boolean mShowOutline = true;
    private boolean mPanEnabled = true;
    private boolean mZoomEnabled = true;
    private boolean mInlineNumbers = false;

    private boolean mGraphIsCentered = true;
    private OnCenterListener mOnCenterListener;
    private List<Point> curveCachedData;
    private List<Point> curveCachedMutatedData;

    public GraphView(Context context) {
        super(context);
        setup(context, null);
    }

    public GraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup(context, attrs);
    }

    public GraphView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public GraphView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setup(context, attrs);
    }

    private void setup(Context context, AttributeSet attrs) {
        mBackgroundPaint = new Paint();
        mBackgroundPaint.setColor(Color.WHITE);
        mBackgroundPaint.setStyle(Style.FILL);

        mTextMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, getResources().getDisplayMetrics());
        mTextPaintSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16, getResources().getDisplayMetrics());
        mTextPaint = new Paint();
        mTextPaint.setColor(Color.BLACK);
        mTextPaint.setTextSize(mTextPaintSize);

        mAxisPaint = new Paint();
        mAxisPaint.setColor(Color.LTGRAY);
        mAxisPaint.setStyle(Style.STROKE);
        mAxisPaint.setStrokeWidth(GRID_WIDTH);

        mGraphPaint = new Paint();
        mGraphPaint.setColor(Color.CYAN);
        mGraphPaint.setStyle(Style.STROKE);
        mGraphPaint.setStrokeWidth(GRAPH_WIDTH);

        mDebugPaint = new Paint();
        mDebugPaint.setColor(Color.MAGENTA);
        mDebugPaint.setStyle(Style.STROKE);
        mDebugPaint.setStrokeWidth(GRAPH_WIDTH);

        ViewConfiguration vc = ViewConfiguration.get(getContext());
        mTouchSlop = vc.getScaledTouchSlop();

        mLineMargin = mMinLineMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 25, getResources().getDisplayMetrics());

        zoomReset();

        mData = new ArrayList();

        if (attrs != null) {
            final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.GraphView, 0, 0);
            setShowGrid(a.getBoolean(R.styleable.GraphView_showGrid, mShowGrid));
            setShowInlineNumbers(a.getBoolean(R.styleable.GraphView_showInlineNumbers, mInlineNumbers));
            setShowOutline(a.getBoolean(R.styleable.GraphView_showOutline, mShowOutline));
            setPanEnabled(a.getBoolean(R.styleable.GraphView_panEnabled, mPanEnabled));
            setZoomEnabled(a.getBoolean(R.styleable.GraphView_zoomEnabled, mZoomEnabled));
            setBackgroundColor(a.getColor(R.styleable.GraphView_backgroundColor, mBackgroundPaint.getColor()));
            setGridColor(a.getColor(R.styleable.GraphView_gridColor, mAxisPaint.getColor()));
            setGraphColor(a.getColor(R.styleable.GraphView_graphColor, mGraphPaint.getColor()));
            setTextColor(a.getColor(R.styleable.GraphView_numberTextColor, mTextPaint.getColor()));
            a.recycle();
        }
    }

    public void zoomReset() {
        setZoomLevel(1);

        // Zero everything out
        mRemainderX = mRemainderY = mOffsetX = mOffsetY = 0;

        mGraphIsCentered = true;

        onSizeChanged(getWidth(), getHeight(), 0, 0);
        postInvalidate();

        for (PanListener listener : mPanListeners) {
            listener.panApplied();
        }
        for (ZoomListener listener : mZoomListeners) {
            listener.zoomApplied(mZoomLevel);
        }
    }

    private Point average(Point... args) {
        float x = 0;
        float y = 0;
        for (Point p : args) {
            x += p.getX();
            y += p.getY();
        }
        return new Point(x / args.length, y / args.length);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mPanEnabled != true && mZoomEnabled != true) {
            return super.onTouchEvent(event);
        }

        // Update mode if pointer count changes
        if (mPointers != event.getPointerCount()) {
            setMode(event);
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                setMode(event);
                break;
            case MotionEvent.ACTION_UP:
                break;
            case MotionEvent.ACTION_MOVE:
                if (mMode == DRAG && mPanEnabled) {
                    float deltaX = event.getX() - mStartX;
                    float deltaY = event.getY() - mStartY;

                    // Cancel out the previous drag
                    mOffsetX += mDragOffsetX;
                    mOffsetY += mDragOffsetY;
                    mRemainderX -= mDragRemainderX;
                    mRemainderY -= mDragRemainderY;

                    // Calculate new drag
                    mDragOffsetX = (int) (deltaX / mLineMargin);
                    mDragOffsetY = (int) (deltaY / mLineMargin);
                    mDragRemainderX = (int) (deltaX) % mLineMargin;
                    mDragRemainderY = (int) (deltaY) % mLineMargin;

                    // Apply new drag
                    mOffsetX -= mDragOffsetX;
                    mOffsetY -= mDragOffsetY;
                    mRemainderX += mDragRemainderX;
                    mRemainderY += mDragRemainderY;

                    // Because we're summing the remainders, we can go above % line margin
                    mOffsetX -= mRemainderX / mLineMargin;
                    mRemainderX %= mLineMargin;
                    mOffsetY -= mRemainderY / mLineMargin;
                    mRemainderY %= mLineMargin;

                    // Notify listeners
                    for (PanListener listener : mPanListeners) {
                        listener.panApplied();
                    }
                    mGraphIsCentered = false;
                } else if (mMode == ZOOM && mZoomEnabled) {
                    double distance = getDistance(new Point(event.getX(0), event.getY(0)), new Point(event.getX(1), event.getY(1)));
                    double delta = mZoomInitDistance - distance;
                    float zoom = (float) (delta / mZoomInitDistance);
                    setZoomLevel(mZoomInitLevel + zoom);
                }
                break;
        }
        invalidate();
        return true;
    }

    @Override
    protected void onSizeChanged(int xNew, int yNew, int xOld, int yOld) {
        super.onSizeChanged(xNew, yNew, xOld, yOld);

        // If the graph was centered, recenter it. If it was panned, leave it alone.
        if (mGraphIsCentered) {
            // Lets calculate the min x and y values
            mOffsetX = (-xNew / mLineMargin) / 2;
            mOffsetY = (-yNew / mLineMargin) / 2;

            // But! The view will probably not perfectly match up to line margins.
            // So put half the remainder on top and half on bottom
            mRemainderX = (xNew % mLineMargin) / 2;
            mRemainderY = (yNew % mLineMargin) / 2;

            // Unfortunately, there's one gotcha left. An even number of line margins won't center
            // the axis! So push everything down by half a line margin, to center on the axis.
            if ((xNew / mLineMargin) % 2 == 1) {
                mRemainderX += mLineMargin / 2;
            }
            if ((yNew / mLineMargin) % 2 == 1) {
                mRemainderY += mLineMargin / 2;
            }

            if (mOnCenterListener != null) {
                mOnCenterListener.onCentered();
            }
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        canvas.drawPaint(mBackgroundPaint);

        // Draw bounding box
        mAxisPaint.setStrokeWidth(BOX_STROKE);
        if (mShowOutline) {
            canvas.drawRect(mLineMargin, mLineMargin,
                    getWidth() - BOX_STROKE / 2, getHeight() - BOX_STROKE / 2, mAxisPaint);
        }

        // Draw the grid lines
        Rect bounds = mTempRect;
        int previousLine = 0;
        boolean inlineNumbersDrawn = !mInlineNumbers;
        for (int i = mInlineNumbers ? 0 : 1, j = mOffsetX; i * mLineMargin < getWidth(); i++, j++) {
            // Draw vertical lines
            int x = i * mLineMargin + mRemainderX;
            if (!mInlineNumbers && (x < mLineMargin || x - previousLine < mMinLineMargin)) continue;
            previousLine = x;

            if (j == 0 && mShowAxis) {
                mAxisPaint.setStrokeWidth(AXIS_WIDTH);
                canvas.drawLine(x, mInlineNumbers ? 0 : mLineMargin, x, getHeight(), mAxisPaint);
            } else if (mShowGrid) {
                mAxisPaint.setStrokeWidth(GRID_WIDTH);
                canvas.drawLine(x, mInlineNumbers ? 0 : mLineMargin, x, getHeight(), mAxisPaint);
            }

            if (!mInlineNumbers) {
                // Draw label on top
                String text = mFormat.format(j * mZoomLevel);
                int textLength = ((text.startsWith("-") ? text.length() - 1 : text.length()) + 1) / 2;
                mTextPaint.setTextSize(mTextPaintSize / textLength);
                mTextPaint.getTextBounds(text, 0, text.length(), bounds);
                int textWidth = bounds.right - bounds.left;
                canvas.drawText(text, x - textWidth / 2, mLineMargin / 2 + mTextPaint.getTextSize() / 2, mTextPaint);
            } else if (j + 1 == 0) {
                // Draw the y min
                String text = mFormat.format(getYAxisMin());
                mTextPaint.getTextBounds(text, 0, text.length(), bounds);
                int textWidth = bounds.right - bounds.left;
                int xCord = x - textWidth;
                xCord = Math.min(getWidth() - 2 * mLineMargin, xCord);
                xCord = Math.max(2 * mLineMargin - textWidth, xCord);
                xCord = Math.max(mTextMargin, xCord); // Don't let the text go off the screen. Margin of mTextMargin
                xCord = Math.min(getWidth() - textWidth - mTextMargin, xCord); // Don't let the text go off the screen.
                canvas.drawText(text, xCord, getHeight() - mLineMargin + mTextPaintSize, mTextPaint);

                // Draw the y max
                text = mFormat.format(getYAxisMax());
                mTextPaint.getTextBounds(text, 0, text.length(), bounds);
                textWidth = bounds.right - bounds.left;
                xCord = x - textWidth;
                xCord = Math.min(getWidth() - 2 * mLineMargin, xCord);
                xCord = Math.max(2 * mLineMargin - textWidth, xCord);
                xCord = Math.max(mTextMargin, xCord); // Don't let the text go off the screen. Margin of mTextPaintSize
                xCord = Math.min(getWidth() - textWidth - mTextMargin, xCord); // Don't let the text go off the screen.
                canvas.drawText(text, xCord, mLineMargin, mTextPaint);

                inlineNumbersDrawn = true;
            }
        }
        if (!inlineNumbersDrawn) {
            boolean drawOnRightSide = getXAxisMin() + (getXAxisMax() - getXAxisMin()) / 2 < 0;

            // Draw the y min
            String text = mFormat.format(getYAxisMin());
            mTextPaint.getTextBounds(text, 0, text.length(), bounds);
            int textWidth = bounds.right - bounds.left;
            int xCord;
            if (drawOnRightSide) {
                xCord = getWidth() - 2 * mLineMargin;
            } else {
                xCord = 2 * mLineMargin - textWidth;
            }
            xCord = Math.max(mTextMargin, xCord); // Don't let the text go off the screen. Margin of mTextMargin
            xCord = Math.min(getWidth() - textWidth - mTextMargin, xCord); // Don't let the text go off the screen.
            int yCord = getHeight() - mLineMargin + mTextPaintSize;
            canvas.drawText(text, xCord, yCord, mTextPaint);

            // Draw the y max
            text = mFormat.format(getYAxisMax());
            mTextPaint.getTextBounds(text, 0, text.length(), bounds);
            textWidth = bounds.right - bounds.left;
            if (drawOnRightSide) {
                xCord = getWidth() - 2 * mLineMargin;
            } else {
                xCord = 2 * mLineMargin - textWidth;
            }
            xCord = Math.max(mTextMargin, xCord); // Don't let the text go off the screen. Margin of mTextMargin
            xCord = Math.min(getWidth() - textWidth - mTextMargin, xCord); // Don't let the text go off the screen.
            yCord = mLineMargin;
            canvas.drawText(text, xCord, yCord, mTextPaint);
        }
        previousLine = 0;
        inlineNumbersDrawn = !mInlineNumbers;
        for (int i = mInlineNumbers ? 0 : 1, j = mOffsetY; i * mLineMargin < getHeight(); i++, j++) {
            // Draw horizontal lines
            int y = i * mLineMargin + mRemainderY;
            if (!mInlineNumbers && (y < mLineMargin || y - previousLine < mMinLineMargin)) continue;
            previousLine = y;

            if (j == 0 && mShowAxis) {
                mAxisPaint.setStrokeWidth(AXIS_WIDTH);
                canvas.drawLine(mInlineNumbers ? 0 : mLineMargin, y, getWidth(), y, mAxisPaint);
            } else if (mShowGrid) {
                mAxisPaint.setStrokeWidth(GRID_WIDTH);
                canvas.drawLine(mInlineNumbers ? 0 : mLineMargin, y, getWidth(), y, mAxisPaint);
            }

            if (!mInlineNumbers) {
                // Draw label on left
                String text = mFormat.format(-j * mZoomLevel);
                int textLength = ((text.startsWith("-") ? text.length() - 1 : text.length()) + 1) / 2;
                mTextPaint.setTextSize(mTextPaintSize / textLength);
                mTextPaint.getTextBounds(text, 0, text.length(), bounds);
                int textHeight = bounds.bottom - bounds.top;
                int textWidth = bounds.right - bounds.left;
                canvas.drawText(text, mLineMargin / 2 - textWidth / 2, y + textHeight / 2, mTextPaint);
            } else if (j - 1 == 0) {
                // Draw the x min
                String text = mFormat.format(getXAxisMin());
                mTextPaint.getTextBounds(text, 0, text.length(), bounds);
                int textWidth = bounds.right - bounds.left;
                int xCord = mLineMargin - textWidth;
                xCord = Math.max(mTextMargin, xCord); // Don't let the text go off the screen. Margin of mTextMargin
                xCord = Math.min(getWidth() - textWidth - mTextMargin, xCord); // Don't let the text go off the screen.
                int yCord = y;
                yCord = Math.min(getHeight() - 2 * mLineMargin + mTextPaintSize, yCord);
                yCord = Math.max(2 * mLineMargin, yCord);
                canvas.drawText(text, xCord, yCord, mTextPaint);

                // Draw the x max
                text = mFormat.format(getXAxisMax());
                mTextPaint.getTextBounds(text, 0, text.length(), bounds);
                textWidth = bounds.right - bounds.left;
                xCord = getWidth() - mLineMargin;
                xCord = Math.max(mTextMargin, xCord); // Don't let the text go off the screen. Margin of mTextMargin
                xCord = Math.min(getWidth() - textWidth - mTextMargin, xCord); // Don't let the text go off the screen.
                canvas.drawText(text, xCord, yCord, mTextPaint);

                inlineNumbersDrawn = true;
            }
        }
        if (!inlineNumbersDrawn) {
            boolean drawOnBottom = getYAxisMin() + (getYAxisMax() - getYAxisMin()) / 2 > 0;

            // Draw the x min
            String text = mFormat.format(getXAxisMin());
            mTextPaint.getTextBounds(text, 0, text.length(), bounds);
            int textWidth = bounds.right - bounds.left;
            int xCord = mLineMargin - textWidth;
            xCord = Math.max(mTextMargin, xCord); // Don't let the text go off the screen. Margin of mTextMargin
            xCord = Math.min(getWidth() - textWidth - mTextMargin, xCord); // Don't let the text go off the screen.
            int yCord;
            if (drawOnBottom) {
                yCord = getHeight() - 2 * mLineMargin + mTextPaintSize;
            } else {
                yCord = 2 * mLineMargin;
            }
            canvas.drawText(text, xCord, yCord, mTextPaint);

            // Draw the x max
            text = mFormat.format(getXAxisMax());
            mTextPaint.getTextBounds(text, 0, text.length(), bounds);
            textWidth = bounds.right - bounds.left;
            xCord = getWidth() - mLineMargin;
            xCord = Math.max(mTextMargin, xCord); // Don't let the text go off the screen. Margin of mTextMargin
            xCord = Math.min(getWidth() - textWidth - mTextMargin, xCord); // Don't let the text go off the screen.
            if (drawOnBottom) {
                yCord = getHeight() - 2 * mLineMargin + mTextPaintSize;
            } else {
                yCord = 2 * mLineMargin;
            }
            canvas.drawText(text, xCord, yCord, mTextPaint);
        }

        // Restrict drawing the graph to the grid
        if (!mInlineNumbers) {
            canvas.clipRect(mLineMargin, mLineMargin,
                    getWidth() - BOX_STROKE, getHeight() - BOX_STROKE);
        }

        // Create a path to draw smooth arcs
        for (Graph graph : mData) {
            if (graph.visible && graph.data.size() != 0) {
                mGraphPaint.setColor(graph.color);
                if (mDrawingAlgorithm == LINES) {
                    drawWithStraightLines(graph.data, canvas, mGraphPaint);
                } else if (mDrawingAlgorithm == DOTS) {
                    drawDots(graph.data, canvas, mGraphPaint);
                } else if (mDrawingAlgorithm == CURVES) {
                    drawWithCurves(graph.data, canvas, mGraphPaint);
                }
            }
        }

        if (DEBUG) {
            canvas.drawLine(0, getHeight() / 2, getWidth(), getHeight() / 2, mDebugPaint);
            canvas.drawLine(getWidth() / 2, 0, getWidth() / 2, getHeight(), mDebugPaint);
        }
    }

    private void drawWithStraightLines(List<Point> data, Canvas canvas, Paint paint) {
        Point previousPoint = null;
        for (Point currentPoint : data) {
            if (previousPoint == null) {
                previousPoint = currentPoint;
                continue;
            }

            int aX = getRawX(previousPoint);
            int aY = getRawY(previousPoint);
            int bX = getRawX(currentPoint);
            int bY = getRawY(currentPoint);

            previousPoint = currentPoint;

            if (tooFar(aX, aY, bX, bY)) continue;

            canvas.drawLine(aX, aY, bX, bY, paint);
        }
    }

    private void drawDots(List<Point> data, Canvas canvas, Paint paint) {
        for (Point p : data) {
            canvas.drawPoint(getRawX(p), getRawY(p), paint);
        }
    }

    private void drawWithCurves(List<Point> data, Canvas canvas, Paint paint) {
        if (curveCachedData == data) {
            drawWithStraightLines(curveCachedMutatedData, canvas, paint);
            return;
        }

        float tension = 0.5f;
        int numOfSegments = 16;
        List<Point> mutatedData = new ArrayList<>(data);
        List<Point> newData = new ArrayList<>(data.size());

        // The algorithm require a previous and next point to the actual point array.
        // Duplicate first points to beginning, end points to end
        mutatedData.add(0, mutatedData.get(0));
        mutatedData.add(mutatedData.get(mutatedData.size() - 1));


        // ok, lets start..

        // 1. loop goes through point array
        // 2. loop goes through each segment between the 2 pts + 1e point before and after
        for (int i = 1; i < data.size() - 2; i++) {
            for (int t = 0; t <= numOfSegments; t++) {

                // calc tension vectors
                float t1x = (data.get(i + 1).getX() - data.get(i - 1).getX()) * tension;
                float t2x = (data.get(i + 2).getX() - data.get(i).getX()) * tension;

                float t1y = (data.get(i + 1).getY() - data.get(i - 1).getY()) * tension;
                float t2y = (data.get(i + 2).getY() - data.get(i).getY()) * tension;

                // calc step
                float st = t / numOfSegments;

                // calc cardinals
                double c1 = 2 * Math.pow(st, 3) - 3 * Math.pow(st, 2) + 1;
                double c2 = -(2 * Math.pow(st, 3)) + 3 * Math.pow(st, 2);
                double c3 = Math.pow(st, 3) - 2 * Math.pow(st, 2) + st;
                double c4 = Math.pow(st, 3) - Math.pow(st, 2);

                // calc x and y cords with common control vectors
                float x = (float) (c1 * data.get(i).getX() + c2 * data.get(i + 1).getX() + c3 * t1x + c4 * t2x);
                float y = (float) (c1 * data.get(i).getY() + c2 * data.get(i + 1).getY() + c3 * t1y + c4 * t2y);

                //store points in array
                newData.add(new Point(x, y));

            }
        }

        curveCachedData = data;
        curveCachedMutatedData = newData;

        drawWithStraightLines(newData, canvas, paint);
    }

    private int getRawX(Point p) {
        if (p == null || Double.isNaN(p.getX()) || Double.isInfinite(p.getX())) return -1;

        // The left line is at pos
        float leftLine = (mInlineNumbers ? 0 : mLineMargin) + mRemainderX;
        // And equals
        float val = mOffsetX * mZoomLevel;
        // And changes at a rate of
        float slope = mLineMargin / mZoomLevel;
        // Put it all together
        int pos = (int) (slope * (p.getX() - val) + leftLine);

        return pos;
    }

    private int getRawY(Point p) {
        if (p == null || Double.isNaN(p.getY()) || Double.isInfinite(p.getY())) return -1;

        // The top line is at pos
        float topLine = (mInlineNumbers ? 0 : mLineMargin) + mRemainderY;
        // And equals
        float val = -mOffsetY * mZoomLevel;
        // And changes at a rate of
        float slope = mLineMargin / mZoomLevel;
        // Put it all together
        int pos = (int) (-slope * (p.getY() - val) + topLine);

        return pos;
    }

    private boolean tooFar(float aX, float aY, float bX, float bY) {
        boolean outOfBounds = aX == -1 || aY == -1 || bX == -1 || bY == -1;
        if (outOfBounds) return true;

        boolean horzAsymptote = (aX > getXAxisMax() && bX < getXAxisMin()) || (aX < getXAxisMin() && bX > getXAxisMax());
        boolean vertAsymptote = (aY > getYAxisMax() && bY < getYAxisMin()) || (aY < getYAxisMin() && bY > getYAxisMax());
        return horzAsymptote || vertAsymptote;
    }

    public float getXAxisMin() {
        return (mOffsetX - 1) * mZoomLevel;
    }

    public float getXAxisMax() {
        int numOfHorizontalGridLines = getWidth() / mLineMargin + 1;
        return (numOfHorizontalGridLines + mOffsetX) * mZoomLevel;
    }

    public float getYAxisMin() {
        int numOfVerticalGridLines = getHeight() / mLineMargin + 1;
        return -1 * (numOfVerticalGridLines + mOffsetY) * mZoomLevel;
    }

    public float getYAxisMax() {
        return -1 * (mOffsetY - 1) * mZoomLevel;
    }

    @Override
    public void setBackgroundColor(int color) {
        mBackgroundPaint.setColor(color);
    }

    private void setMode(MotionEvent e) {
        mPointers = e.getPointerCount();
        switch (e.getPointerCount()) {
            case 1:
                // Drag
                setMode(DRAG, e);
                break;
            case 2:
                // Zoom
                setMode(ZOOM, e);
                break;
        }
    }

    private void setMode(int mode, MotionEvent e) {
        mMode = mode;
        switch (mode) {
            case DRAG:
                mStartX = e.getX();
                mStartY = e.getY();
                mDragOffsetX = 0;
                mDragOffsetY = 0;
                mDragRemainderX = 0;
                mDragRemainderY = 0;
                break;
            case ZOOM:
                mZoomInitDistance = getDistance(new Point(e.getX(0), e.getY(0)), new Point(e.getX(1), e.getY(1)));
                mZoomInitLevel = mZoomLevel;
                break;
        }
    }

    public float getZoomLevel() {
        return mZoomLevel;
    }

    public void setZoomLevel(float level) {
        mZoomLevel = level;
        invalidate();
        for (ZoomListener listener : mZoomListeners) {
            listener.zoomApplied(mZoomLevel);
        }
    }

    public void zoomIn() {
        setZoomLevel(mZoomLevel / 2);
    }

    public void zoomOut() {
        setZoomLevel(mZoomLevel * 2);
    }

    public void addGraph(Graph graph) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new RuntimeException("addGraph called from a thread other than the ui thread");
        }

        mData.add(graph);
        postInvalidate();
    }

    public void clearGraphs() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new RuntimeException("clearGraphs called from a thread other than the ui thread");
        }

        mData.clear();
        postInvalidate();
    }

    public void setOnCenterListener(OnCenterListener l) {
        mOnCenterListener = l;
    }

    public List<Graph> getGraphs() {
        return mData;
    }

    private double getDistance(Point a, Point b) {
        return Math.sqrt(square(a.getX() - b.getX()) + square(a.getY() - b.getY()));
    }

    private double square(double val) {
        return val * val;
    }

    public void setGridColor(int color) {
        mAxisPaint.setColor(color);
    }

    public void setTextColor(int color) {
        mTextPaint.setColor(color);
        invalidate();
    }

    public void setGraphColor(int color) {
        mGraphPaint.setColor(color);
    }

    public void addPanListener(PanListener l) {
        mPanListeners.add(l);
    }

    public void removePanListener(PanListener l) {
        mPanListeners.remove(l);
    }

    public void addZoomListener(ZoomListener l) {
        mZoomListeners.add(l);
    }

    public void removeZoomListener(ZoomListener l) {
        mZoomListeners.remove(l);
    }

    public boolean isGridShown() {
        return mShowGrid;
    }

    public void setShowGrid(boolean show) {
        mShowGrid = show;
    }

    public boolean isAxisShown() {
        return mShowAxis;
    }

    public void setShowAxis(boolean show) {
        mShowAxis = show;
    }

    public boolean isOutlineShown() {
        return mShowOutline;
    }

    public void setShowOutline(boolean show) {
        mShowOutline = show;
    }

    public boolean isPanEnabled() {
        return mPanEnabled;
    }

    public void setPanEnabled(boolean enabled) {
        mPanEnabled = enabled;
    }

    public boolean isZoomEnabled() {
        return mZoomEnabled;
    }

    public void setZoomEnabled(boolean enabled) {
        mZoomEnabled = enabled;
    }

    public boolean showInlineNumbers() {
        return mInlineNumbers;
    }

    public void setShowInlineNumbers(boolean show) {
        mInlineNumbers = show;
    }

    public void panBy(float x, float y) {
        mOffsetX -= (int) x / mLineMargin;
        mOffsetY -= (int) y / mLineMargin;
        mRemainderX += (int) x % mLineMargin;
        mRemainderY += (int) y % mLineMargin;
        invalidate();
    }

    public interface PanListener {
        void panApplied();
    }

    public interface ZoomListener {
        void zoomApplied(float level);
    }

    public interface OnCenterListener {
        void onCentered();
    }

    public static class Graph {
        private String formula;
        private int color;
        private List<Point> data;
        private boolean visible = true;

        public Graph(String formula, int color, List<Point> data) {
            this.formula = formula;
            this.color = color;
            this.data = data;
        }

        public String getFormula() {
            return formula;
        }

        public void setFormula(String formula) {
            this.formula = formula;
        }

        public int getColor() {
            return color;
        }

        public void setColor(int color) {
            this.color = color;
        }

        public List<Point> getData() {
            return data;
        }

        public void setData(List<Point> data) {
            this.data = data;
        }

        public boolean isVisible() {
            return visible;
        }

        public void setVisible(boolean visible) {
            this.visible = visible;
        }

        @Override
        public String toString() {
            return String.format("Graph{formula=%s}", formula);
        }
    }
}
