package com.fmorea.chess;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ChessView extends View {
    private float cellSide = 130f;
    private float originX = 0f;
    private float originY = 0f;
    private final int lightColor = Color.parseColor("#DEDFC4");
    private final int darkColor = Color.parseColor("#2D942D");

    private final Matrix transformMatrix = new Matrix();
    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;

    private boolean isPenMode = false;
    private boolean isEraserMode = false;
    private final List<Stroke> strokes = new ArrayList<>();
    private Stroke currentStroke;
    private final Paint penPaint = new Paint();
    private final Paint hintPaint = new Paint();
    private final Paint touchPaint = new Paint();
    private final Paint selectionPaint = new Paint();
    private final Paint infoPaint = new Paint();

    private final Map<Integer, Bitmap> bitmaps = new HashMap<>();
    private final Paint paint = new Paint();
    private Bitmap movingPieceBitmap = null;
    private Piece movingPiece = null;
    private int fromCol = -1, fromRow = -1;
    private int selectedCol = -1, selectedRow = -1;
    private float movingPieceX, movingPieceY;
    private boolean isDraggingPiece = false;

    private ChessDelegate chessDelegate = null;

    public ChessView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        loadBitmaps();
        initTools(context);
    }

    private void initTools(Context context) {
        penPaint.setColor(Color.RED);
        penPaint.setStyle(Paint.Style.STROKE);
        penPaint.setStrokeWidth(10f);
        penPaint.setStrokeCap(Paint.Cap.ROUND);
        penPaint.setAntiAlias(true);

        hintPaint.setColor(Color.argb(128, 255, 255, 0));
        hintPaint.setStyle(Paint.Style.FILL);

        touchPaint.setColor(Color.argb(150, 255, 255, 255));
        touchPaint.setStyle(Paint.Style.FILL);

        selectionPaint.setColor(Color.argb(100, 255, 255, 255));
        selectionPaint.setStyle(Paint.Style.STROKE);
        selectionPaint.setStrokeWidth(6f);

        infoPaint.setAntiAlias(true);
        infoPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float scaleFactor = detector.getScaleFactor();
                transformMatrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
                invalidate();
                return true;
            }
        });

        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (!isPenMode && !isEraserMode && e2.getPointerCount() > 1) {
                    transformMatrix.postTranslate(-distanceX, -distanceY);
                    invalidate();
                    return true;
                }
                return false;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                if (isPenMode || isEraserMode) return false;
                
                float[] pts = {e.getX(), e.getY()};
                Matrix inverse = new Matrix();
                transformMatrix.invert(inverse);
                inverse.mapPoints(pts);
                
                handleTap(pts[0], pts[1]);
                return true;
            }
        });
    }

    private void handleTap(float x, float y) {
        int col = getColFromX(x);
        int row = getRowFromY(y);

        if (col < 1 || col > 8 || row < 1 || row > 8) {
            selectedCol = -1;
            selectedRow = -1;
            invalidate();
            return;
        }

        if (selectedCol != -1 && selectedRow != -1) {
            if (selectedCol == col && selectedRow == row) {
                selectedCol = -1;
                selectedRow = -1;
            } else if (chessDelegate != null && chessDelegate.movePiece(selectedCol, selectedRow, col, row)) {
                selectedCol = -1;
                selectedRow = -1;
            } else {
                Piece p = (chessDelegate != null) ? chessDelegate.pieceAt(col, row) : null;
                if (p != null) {
                    selectedCol = col;
                    selectedRow = row;
                } else {
                    selectedCol = -1;
                    selectedRow = -1;
                }
            }
        } else {
            Piece p = (chessDelegate != null) ? chessDelegate.pieceAt(col, row) : null;
            if (p != null) {
                selectedCol = col;
                selectedRow = row;
            }
        }
        invalidate();
    }

    public void setPenMode(boolean enabled) {
        this.isPenMode = enabled;
        if (enabled) this.isEraserMode = false;
        invalidate();
    }

    public void setEraserMode(boolean enabled) {
        this.isEraserMode = enabled;
        if (enabled) this.isPenMode = false;
        invalidate();
    }

    private void updateBoardMetrics() {
        float chessBoardSide = Math.min(getWidth(), getHeight()) * 0.9f;
        cellSide = chessBoardSide / 8f;
        originX = (getWidth() - chessBoardSide) / 2f;
        originY = (getHeight() - chessBoardSide) / 2f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        updateBoardMetrics();
        canvas.save();
        canvas.concat(transformMatrix);

        drawInfiniteGrid(canvas);
        drawChessboard(canvas);
        drawNotation(canvas);
        drawSelection(canvas);
        drawHints(canvas);
        drawPieces(canvas);
        drawStrokes(canvas);
        drawInfo(canvas);

        if (isDraggingPiece) {
            canvas.drawCircle(movingPieceX, movingPieceY, cellSide / 6, touchPaint);
            if (movingPieceBitmap != null) {
                float offset = cellSide;
                RectF dest = new RectF(movingPieceX - cellSide/2, movingPieceY - cellSide/2 - offset,
                        movingPieceX + cellSide/2, movingPieceY + cellSide/2 - offset);
                canvas.drawBitmap(movingPieceBitmap, null, dest, paint);
            }
        }

        canvas.restore();
    }

    private void drawInfiniteGrid(Canvas canvas) {
        Paint gridPaint = new Paint();
        gridPaint.setColor(Color.parseColor("#EEEEEE"));
        gridPaint.setStrokeWidth(1f);
        float step = 50f;
        for (float x = -2000; x < 4000; x += step) canvas.drawLine(x, -2000, x, 4000, gridPaint);
        for (float y = -2000; y < 4000; y += step) canvas.drawLine(-2000, y, 4000, y, gridPaint);
    }

    private void drawNotation(Canvas canvas) {
        infoPaint.setTextSize(cellSide * 0.25f);
        infoPaint.setColor(Color.GRAY);
        
        // Columns a-h (Bottom and Top)
        infoPaint.setTextAlign(Paint.Align.CENTER);
        for (int col = 1; col <= 8; col++) {
            String label = String.valueOf((char) ('a' + col - 1));
            float x = getScreenX(col) + cellSide / 2;
            
            // Bottom
            float yBottom = originY + 8 * cellSide + infoPaint.getTextSize() * 1.2f;
            canvas.drawText(label, x, yBottom, infoPaint);
            
            // Top
            float yTop = originY - infoPaint.getTextSize() * 0.5f;
            canvas.drawText(label, x, yTop, infoPaint);
        }

        // Rows 1-8 (Left and Right)
        for (int row = 1; row <= 8; row++) {
            String label = String.valueOf(row);
            float y = getScreenY(row) + cellSide / 2 + infoPaint.getTextSize() / 2 - 5f;
            
            // Left
            infoPaint.setTextAlign(Paint.Align.RIGHT);
            float xLeft = originX - cellSide * 0.15f;
            canvas.drawText(label, xLeft, y, infoPaint);
            
            // Right
            infoPaint.setTextAlign(Paint.Align.LEFT);
            float xRight = originX + 8 * cellSide + cellSide * 0.15f;
            canvas.drawText(label, xRight, y, infoPaint);
        }
    }

    private void drawInfo(Canvas canvas) {
        if (chessDelegate == null) return;

        float infoX = originX;
        float infoY = originY + 8 * cellSide + cellSide * 0.8f;
        float textSize = cellSide * 0.35f;

        infoPaint.setTextSize(textSize);
        
        // Turn indicator
        float turnCircleRadius = textSize / 2;
        infoPaint.setColor(chessDelegate.isWhiteTurn() ? Color.WHITE : Color.BLACK);
        infoPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(infoX + turnCircleRadius, infoY - turnCircleRadius/2, turnCircleRadius, infoPaint);
        
        infoPaint.setColor(Color.GRAY);
        infoPaint.setStyle(Paint.Style.STROKE);
        infoPaint.setStrokeWidth(2f);
        canvas.drawCircle(infoX + turnCircleRadius, infoY - turnCircleRadius/2, turnCircleRadius, infoPaint);

        infoPaint.setStyle(Paint.Style.FILL);
        infoPaint.setColor(Color.DKGRAY);
        infoPaint.setTextAlign(Paint.Align.LEFT);
        String turnStr = (chessDelegate.isWhiteTurn() ? "White" : "Black") + "'s Turn";
        canvas.drawText(turnStr, infoX + turnCircleRadius * 3.0f, infoY, infoPaint);

        // Material Count
        int material = chessDelegate.getMaterialCount();
        String matStr = "Score Advantage: " + (material > 0 ? "+" + material : material);
        canvas.drawText(matStr, infoX, infoY + textSize * 1.5f, infoPaint);

        // Connection Status
        String netStr = "Network: " + chessDelegate.getNetworkStatus();
        if (chessDelegate.isConnected()) {
            infoPaint.setColor(Color.parseColor("#2E7D32")); // Material Green 800
        } else {
            infoPaint.setColor(Color.parseColor("#C62828")); // Material Red 800
        }
        canvas.drawText(netStr, infoX, infoY + textSize * 3.0f, infoPaint);
        
        infoPaint.setColor(Color.BLACK); // Reset
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);

        float[] pts = {event.getX(), event.getY()};
        Matrix inverse = new Matrix();
        transformMatrix.invert(inverse);
        inverse.mapPoints(pts);
        float x = pts[0];
        float y = pts[1];

        if (isPenMode) {
            handlePenTouch(event, x, y);
            return true;
        }
        if (isEraserMode) {
            handleEraserTouch(event, x, y);
            return true;
        }

        if (event.getPointerCount() > 1) {
            if (isDraggingPiece) {
                isDraggingPiece = false;
                movingPiece = null;
                invalidate();
            }
            return true;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                fromCol = getColFromX(x);
                fromRow = getRowFromY(y);
                if (chessDelegate != null) {
                    movingPiece = chessDelegate.pieceAt(fromCol, fromRow);
                    if (movingPiece != null) {
                        isDraggingPiece = true;
                        movingPieceBitmap = bitmaps.get(movingPiece.getResID());
                        movingPieceX = x;
                        movingPieceY = y;
                        selectedCol = -1;
                        selectedRow = -1;
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (isDraggingPiece) {
                    movingPieceX = x;
                    movingPieceY = y;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (isDraggingPiece) {
                    int toCol = getColFromX(x);
                    int toRow = getRowFromY(y);
                    if (chessDelegate != null) chessDelegate.movePiece(fromCol, fromRow, toCol, toRow);
                    isDraggingPiece = false;
                    movingPiece = null;
                }
                break;
        }
        invalidate();
        return true;
    }

    private int getColFromX(float x) {
        int c = (int) ((x - originX) / cellSide) + 1;
        if (chessDelegate != null && chessDelegate.blackPointOfView()) return 9 - c;
        return c;
    }

    private int getRowFromY(float y) {
        int r = 8 - (int) ((y - originY) / cellSide);
        if (chessDelegate != null && chessDelegate.blackPointOfView()) return 9 - r;
        return r;
    }

    private float getScreenX(int col) {
        int c = (chessDelegate != null && chessDelegate.blackPointOfView()) ? 9 - col : col;
        return originX + (c - 1) * cellSide;
    }

    private float getScreenY(int row) {
        int r = (chessDelegate != null && chessDelegate.blackPointOfView()) ? 9 - row : row;
        return originY + (8 - r) * cellSide;
    }

    private void handlePenTouch(MotionEvent event, float x, float y) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Path path = new Path();
                path.moveTo(x, y);
                currentStroke = new Stroke(path, new Paint(penPaint));
                currentStroke.addPoint(x, y);
                strokes.add(currentStroke);
                break;
            case MotionEvent.ACTION_MOVE:
                if (currentStroke != null) {
                    currentStroke.path.lineTo(x, y);
                    currentStroke.addPoint(x, y);
                }
                break;
            case MotionEvent.ACTION_UP:
                currentStroke = null;
                break;
        }
        invalidate();
    }

    private void handleEraserTouch(MotionEvent event, float x, float y) {
        if (event.getAction() == MotionEvent.ACTION_MOVE || event.getAction() == MotionEvent.ACTION_DOWN) {
            float threshold = cellSide / 2;
            Iterator<Stroke> it = strokes.iterator();
            while (it.hasNext()) {
                if (it.next().isNear(x, y, threshold)) {
                    it.remove();
                }
            }
            invalidate();
        }
    }

    private void drawStrokes(Canvas canvas) {
        for (Stroke s : strokes) canvas.drawPath(s.path, s.paint);
    }

    private void drawChessboard(Canvas canvas) {
        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                drawSquareAt(canvas, col, row, (col + row) % 2 == 1);
            }
        }
    }

    private void drawSquareAt(Canvas canvas, int col, int row, boolean isDark) {
        paint.setColor(isDark ? darkColor : lightColor);
        float x = getScreenX(col);
        float y = getScreenY(row);
        canvas.drawRect(x, y, x + cellSide, y + cellSide, paint);
    }

    private void drawSelection(Canvas canvas) {
        if (selectedCol != -1 && selectedRow != -1) {
            float x = getScreenX(selectedCol);
            float y = getScreenY(selectedRow);
            canvas.drawRect(x + 5, y + 5, x + cellSide - 5, y + cellSide - 5, selectionPaint);
        }
    }

    private void drawHints(Canvas canvas) {
        if (chessDelegate == null) return;
        
        int activeCol = isDraggingPiece ? fromCol : selectedCol;
        int activeRow = isDraggingPiece ? fromRow : selectedRow;
        
        if (activeCol == -1 || activeRow == -1) return;
        
        List<Movement> moves = chessDelegate.getLegalMoves();
        if (moves == null) return;
        for (Movement m : moves) {
            if (m.getX0() == activeCol && m.getY0() == activeRow) {
                float x = getScreenX(m.getX());
                float y = getScreenY(m.getY());
                canvas.drawCircle(x + cellSide/2, y + cellSide/2, cellSide/5, hintPaint);
            }
        }
    }

    private void drawPieces(Canvas canvas) {
        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                if (chessDelegate != null) {
                    Piece piece = chessDelegate.pieceAt(col, row);
                    if (piece != null && piece != movingPiece) drawPieceAt(canvas, col, row, piece.getResID());
                }
            }
        }
    }

    private void drawPieceAt(Canvas canvas, int col, int row, int resID) {
        Bitmap b = bitmaps.get(resID);
        if (b != null) {
            float x = getScreenX(col);
            float y = getScreenY(row);
            canvas.drawBitmap(b, null, new RectF(x, y, x + cellSide, y + cellSide), paint);
        }
    }

    private void loadBitmaps() {
        int[] resIDs = {R.drawable.bishop_black, R.drawable.bishop_white, R.drawable.king_black, R.drawable.king_white,
                R.drawable.queen_black, R.drawable.queen_white, R.drawable.rook_black, R.drawable.rook_white,
                R.drawable.knight_black, R.drawable.knight_white, R.drawable.pawn_black, R.drawable.pawn_white};
        for (int id : resIDs) {
            Bitmap b = BitmapFactory.decodeResource(getResources(), id);
            if (b != null) bitmaps.put(id, b);
        }
    }

    public void setChessDelegate(ChessDelegate delegate) { this.chessDelegate = delegate; }

    private static class Stroke {
        Path path;
        Paint paint;
        List<PointF> points = new ArrayList<>();

        Stroke(Path path, Paint paint) { this.path = path; this.paint = paint; }
        void addPoint(float x, float y) { points.add(new PointF(x, y)); }
        boolean isNear(float x, float y, float threshold) {
            for (PointF p : points) {
                if (Math.hypot(p.x - x, p.y - y) < threshold) return true;
            }
            return false;
        }
    }
}
