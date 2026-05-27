package com.fmorea.chess;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
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

    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;
    
    // Animation Targets
    private float targetPitch = 0f; 
    private float targetYaw = 0f;
    private float targetRotation = 0f; 
    private float targetZoom = 1.0f;
    private float targetPanX = 0f;
    private float targetPanY = 0f;

    // Current Values for Rendering (Smooth interpolation)
    private float curPitch = 0f;
    private float curYaw = 0f;
    private float curRotation = 0f;
    private float curZoom = 1.0f;
    private float curPanX = 0f;
    private float curPanY = 0f;

    private static final float MIN_ZOOM = 0.2f;
    private static final float MAX_ZOOM = 8.0f;
    private static final float ANIM_SPEED = 0.18f; 

    private float lastTwoFingerY = 0f;
    private float lastTwoFingerX = 0f;
    private float lastTwoFingerAngle = 0f;
    private float lastTwoFingerDist = 0f;
    private boolean isTwoFingerDragging = false;

    private final Paint paint = new Paint();
    private final Paint hintPaint = new Paint();
    private final Paint touchPaint = new Paint();
    private final Paint selectionPaint = new Paint();
    private final Paint cursorPaint = new Paint();
    private final Paint infoPaint = new Paint();
    private final Paint overlayPaint = new Paint();
    private final Paint penPaint = new Paint();
    private final Paint tablePaint = new Paint();

    private final Map<Integer, Bitmap> bitmaps = new HashMap<>();
    private Piece movingPiece = null;
    private Bitmap movingPieceBitmap = null;
    private int fromCol = -1, fromRow = -1;
    private int selectedCol = -1, selectedRow = -1;
    private int cursorCol = 4, cursorRow = 4;
    private float movingPieceX, movingPieceY;
    private boolean isDraggingPiece = false;
    private float touchStartX, touchStartY;
    private static final float TAP_THRESHOLD = 25f;

    private float gravityPitch = 0f;
    private float gravityYaw = 0f;

    private boolean isPenMode = false;
    private boolean isEraserMode = false;
    private final List<Stroke> strokes = new ArrayList<>();

    private ChessDelegate chessDelegate = null;

    public ChessView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        loadBitmaps();
        initTools(context);
    }

    private void loadBitmaps() {
        int[] resIDs = {
                R.drawable.pawn_white, R.drawable.pawn_black,
                R.drawable.bishop_white, R.drawable.bishop_black,
                R.drawable.knight_white, R.drawable.knight_black,
                R.drawable.king_white, R.drawable.king_black,
                R.drawable.queen_white, R.drawable.queen_black,
                R.drawable.rook_white, R.drawable.rook_black
        };
        for (int resID : resIDs) {
            bitmaps.put(resID, BitmapFactory.decodeResource(getResources(), resID));
        }
    }

    private void initTools(Context context) {
        hintPaint.setColor(Color.argb(200, 255, 235, 59)); // Stronger yellow
        hintPaint.setStyle(Paint.Style.FILL);
        hintPaint.setAntiAlias(true);
        
        touchPaint.setColor(Color.argb(150, 255, 255, 255));
        selectionPaint.setColor(Color.argb(200, 255, 255, 255));
        selectionPaint.setStyle(Paint.Style.STROKE);
        selectionPaint.setStrokeWidth(10f);
        cursorPaint.setColor(Color.argb(180, 255, 152, 0));
        cursorPaint.setStyle(Paint.Style.STROKE);
        cursorPaint.setStrokeWidth(12f);
        infoPaint.setAntiAlias(true);
        infoPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        overlayPaint.setAntiAlias(true);
        overlayPaint.setTextSize(26f);
        overlayPaint.setTypeface(Typeface.MONOSPACE);

        penPaint.setColor(Color.RED);
        penPaint.setStyle(Paint.Style.STROKE);
        penPaint.setStrokeWidth(10f);
        penPaint.setStrokeCap(Paint.Cap.ROUND);
        penPaint.setAntiAlias(true);

        tablePaint.setColor(Color.parseColor("#F5F5F5")); 
        tablePaint.setAntiAlias(true);

        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) { return true; }
        });

        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) { return false; }
        });
    }

    private Matrix getFinalTransformMatrix() {
        return getFinalTransformMatrixAtZ(0);
    }

    private Matrix getFinalTransformMatrixAtZ(float z) {
        Matrix matrix = new Matrix();
        matrix.postScale(curZoom, curZoom, getWidth() / 2f, getHeight() / 2f);
        matrix.postTranslate(curPanX * curZoom, curPanY * curZoom);

        Camera camera = new Camera();
        camera.save();
        camera.translate(0, 0, z);
        camera.rotateZ(curRotation + curYaw);
        camera.rotateX(curPitch);
        Matrix m3d = new Matrix();
        camera.getMatrix(m3d);
        camera.restore();

        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        m3d.preTranslate(-centerX, -centerY);
        m3d.postTranslate(centerX, centerY);
        matrix.postConcat(m3d);
        return matrix;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        updateAnimations();
        updateBoardMetrics();
        
        drawTable(canvas);
        
        // Draw Z-thickness (sides) of the board frame
        drawBoardThickness(canvas);

        canvas.save();
        Matrix finalMatrix = getFinalTransformMatrix();
        canvas.concat(finalMatrix);
        
        drawBoardFrameTop(canvas);
        drawChessboard(canvas);
        drawNotation(canvas);
        drawSelection(canvas);
        drawCursor(canvas);
        drawHints(canvas);
        drawPieces(canvas);
        drawStrokes(canvas);
        canvas.restore();
        
        drawOverlays(canvas);
        
        if (isAnimating()) invalidate();
    }

    private void drawTable(Canvas canvas) {
        canvas.save();
        canvas.concat(getFinalTransformMatrixAtZ(-60)); // Table is below the board
        float size = cellSide * 200f; 
        canvas.drawRect(originX - size, originY - size, originX + size, originY + size, tablePaint);
        canvas.restore();
    }

    private void drawBoardThickness(Canvas canvas) {
        float frameSize = cellSide * 0.4f;
        float boardSize = cellSide * 8;
        RectF frame = new RectF(originX - frameSize, originY - frameSize, originX + boardSize + frameSize, originY + boardSize + frameSize);
        
        Paint depthPaint = new Paint();
        depthPaint.setColor(Color.parseColor("#1A0A08"));
        
        // Draw layers to simulate thickness
        for (float z = -40; z < 0; z += 2) {
            canvas.save();
            canvas.concat(getFinalTransformMatrixAtZ(z));
            canvas.drawRect(frame, depthPaint);
            canvas.restore();
        }
    }

    private void drawBoardFrameTop(Canvas canvas) {
        float frameSize = cellSide * 0.4f;
        float boardSize = cellSide * 8;
        RectF frame = new RectF(originX - frameSize, originY - frameSize, originX + boardSize + frameSize, originY + boardSize + frameSize);
        Paint framePaint = new Paint();
        framePaint.setShader(new LinearGradient(frame.left, frame.top, frame.right, frame.bottom, Color.parseColor("#4E342E"), Color.parseColor("#21110E"), Shader.TileMode.MIRROR));
        canvas.drawRect(frame, framePaint);
    }

    private boolean isAnimating() {
        return Math.abs(curPitch - targetPitch) > 0.01f || Math.abs(curYaw - targetYaw) > 0.01f ||
               Math.abs(curRotation - targetRotation) > 0.01f || Math.abs(curZoom - targetZoom) > 0.001f ||
               Math.abs(curPanX - targetPanX) > 0.1f || Math.abs(curPanY - targetPanY) > 0.1f;
    }

    private void updateAnimations() {
        curPitch += (targetPitch - curPitch) * ANIM_SPEED;
        curYaw += (targetYaw - curYaw) * ANIM_SPEED;
        float diff = targetRotation - curRotation;
        while (diff > 180) diff -= 360;
        while (diff < -180) diff += 360;
        curRotation += diff * ANIM_SPEED;
        curZoom += (targetZoom - curZoom) * ANIM_SPEED;
        curPanX += (targetPanX - curPanX) * ANIM_SPEED;
        curPanY += (targetPanY - curPanY) * ANIM_SPEED;
    }

    private void drawOverlays(Canvas canvas) {
        canvas.save();
        canvas.translate(160, getHeight() - 160);
        
        Camera compCamera = new Camera();
        compCamera.save();
        compCamera.rotateX(curPitch);
        compCamera.rotateZ(curRotation + curYaw);
        Matrix compMatrix = new Matrix();
        compCamera.getMatrix(compMatrix);
        compCamera.restore();
        
        overlayPaint.setColor(Color.BLACK);
        canvas.drawCircle(0, 0, 5, overlayPaint);

        // X Axis (Red)
        float[] ptsX = {0, 0, 70, 0};
        compMatrix.mapPoints(ptsX);
        overlayPaint.setColor(Color.RED);
        overlayPaint.setStrokeWidth(5f);
        canvas.drawLine(ptsX[0], ptsX[1], ptsX[2], ptsX[3], overlayPaint);
        canvas.drawText("X", ptsX[2] + 5, ptsX[3], overlayPaint);

        // Y Axis (Green)
        float[] ptsY = {0, 0, 0, 70};
        compMatrix.mapPoints(ptsY);
        overlayPaint.setColor(Color.GREEN);
        canvas.drawLine(ptsY[0], ptsY[1], ptsY[2], ptsY[3], overlayPaint);
        canvas.drawText("Y", ptsY[2], ptsY[3] + 25, overlayPaint);

        // Z Axis (Blue) 
        overlayPaint.setColor(Color.BLUE);
        float zLen = 70f * (float) Math.cos(Math.toRadians(curPitch));
        canvas.drawLine(0, 0, 0, -zLen, overlayPaint);
        canvas.drawText("Z", -10, -zLen - 10, overlayPaint);
        
        canvas.restore();
    }

    private void drawChessboard(Canvas canvas) {
        for (int r = 1; r <= 8; r++) {
            for (int c = 1; c <= 8; c++) {
                paint.setColor((r + c) % 2 == 1 ? darkColor : lightColor);
                canvas.drawRect(getScreenX(c), getScreenY(r), getScreenX(c) + cellSide, getScreenY(r) + cellSide, paint);
            }
        }
    }

    private void drawNotation(Canvas canvas) {
        infoPaint.setTextSize(cellSide * 0.22f); infoPaint.setColor(Color.GRAY);
        for (int i = 1; i <= 8; i++) {
            drawBillboardText(canvas, (char)('a' + i - 1) + "", getScreenX(i) + 5, getScreenY(1) + cellSide - 5);
            drawBillboardText(canvas, i + "", getScreenX(1) + 5, getScreenY(i) + 20);
        }
    }

    private void drawBillboardText(Canvas canvas, String text, float x, float y) {
        canvas.save(); canvas.translate(x, y);
        Camera c = new Camera(); c.save(); c.rotateX(-curPitch); c.rotateZ(-curRotation - curYaw);
        Matrix m = new Matrix(); c.getMatrix(m); c.restore();
        canvas.concat(m); canvas.drawText(text, 0, 0, infoPaint); canvas.restore();
    }

    private void drawSelection(Canvas canvas) {
        if (selectedCol != -1) {
            float x = getScreenX(selectedCol), y = getScreenY(selectedRow);
            canvas.drawRect(x + 5, y + 5, x + cellSide - 5, y + cellSide - 5, selectionPaint);
        }
    }

    private void drawCursor(Canvas canvas) {
        float x = getScreenX(cursorCol), y = getScreenY(cursorRow);
        canvas.drawRect(x + 10, y + 10, x + cellSide - 10, y + cellSide - 10, cursorPaint);
    }

    private void drawHints(Canvas canvas) {
        if (chessDelegate == null || selectedCol == -1) return;
        List<Movement> moves = chessDelegate.getLegalMoves();
        if (moves == null) return;
        for (Movement m : moves) {
            if (m.getX0() == selectedCol && m.getY0() == selectedRow) {
                float cx = getScreenX(m.getX()) + cellSide / 2;
                float cy = getScreenY(m.getY()) + cellSide / 2;
                if (chessDelegate.pieceAt(m.getX(), m.getY()) != null) {
                    // Ring for capture moves
                    hintPaint.setStyle(Paint.Style.STROKE);
                    hintPaint.setStrokeWidth(cellSide / 8f);
                    canvas.drawCircle(cx, cy, cellSide / 3.5f, hintPaint);
                    hintPaint.setStyle(Paint.Style.FILL);
                } else {
                    // Dot for empty squares
                    canvas.drawCircle(cx, cy, cellSide / 4.5f, hintPaint);
                }
            }
        }
    }

    private void drawPieces(Canvas canvas) {
        if (chessDelegate == null) return;
        List<PiecePos> pieceList = new ArrayList<>();
        Matrix finalMatrix = getFinalTransformMatrix();
        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                Piece piece = chessDelegate.pieceAt(col, row);
                if (piece != null && !(isDraggingPiece && col == fromCol && row == fromRow)) {
                    float[] pts = {getScreenX(col) + cellSide/2, getScreenY(row) + cellSide/2};
                    finalMatrix.mapPoints(pts);
                    pieceList.add(new PiecePos(piece, col, row, pts[1]));
                }
            }
        }
        Collections.sort(pieceList, (a, b) -> Float.compare(a.projY, b.projY));
        for (PiecePos pp : pieceList) {
            drawPieceBillboardAt(canvas, getScreenX(pp.col), getScreenY(pp.row), bitmaps.get(pp.piece.getResID()));
        }

        if (isDraggingPiece && movingPieceBitmap != null) {
            drawPieceBillboardAt(canvas, movingPieceX - cellSide/2, movingPieceY - cellSide/2, movingPieceBitmap);
        }
    }

    private void drawPieceBillboardAt(Canvas canvas, float x, float y, Bitmap b) {
        if (b == null) return;
        paint.setAlpha(60);
        paint.setColorFilter(new PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN));
        canvas.save();
        canvas.drawBitmap(b, null, new RectF(x + 8, y + 8, x + cellSide + 8, y + cellSide + 8), paint);
        canvas.restore();
        
        paint.setAlpha(255); paint.setColorFilter(null);
        canvas.save(); canvas.translate(x + cellSide/2, y + cellSide/2);
        Camera pieceCamera = new Camera(); pieceCamera.save();
        pieceCamera.rotateX(-curPitch + gravityPitch); pieceCamera.rotateY(gravityYaw);
        pieceCamera.rotateZ(-curRotation - curYaw);
        Matrix pieceMatrix = new Matrix(); pieceCamera.getMatrix(pieceMatrix); pieceCamera.restore();
        canvas.concat(pieceMatrix);
        canvas.drawBitmap(b, null, new RectF(-cellSide/2, -cellSide/2, cellSide/2, cellSide/2), paint);
        canvas.restore();
    }

    private void drawStrokes(Canvas canvas) {
        for (Stroke s : strokes) canvas.drawPath(s.path, s.paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getPointerCount() == 2) {
            float curX = (event.getX(0) + event.getX(1)) / 2f;
            float curY = (event.getY(0) + event.getY(1)) / 2f;
            float curDist = (float) Math.hypot(event.getX(0) - event.getX(1), event.getY(0) - event.getY(1));
            float curAngle = calculateAngle(event);

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_POINTER_DOWN:
                    lastTwoFingerX = curX; lastTwoFingerY = curY;
                    lastTwoFingerAngle = curAngle; lastTwoFingerDist = curDist;
                    isTwoFingerDragging = true;
                    isDraggingPiece = false;
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (isTwoFingerDragging) {
                        float dx = curX - lastTwoFingerX;
                        float dy = curY - lastTwoFingerY;
                        float da = calculateDeltaAngle(curAngle, lastTwoFingerAngle);
                        targetPanX += dx / curZoom;
                        targetPanY += dy / curZoom;
                        targetYaw -= da * 1.5f;
                        if (lastTwoFingerDist > 0) {
                            targetZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, targetZoom * (curDist / lastTwoFingerDist)));
                        }
                        if (Math.abs(dy) > Math.abs(dx) * 1.5f) {
                            targetPitch = Math.max(-20, Math.min(95, targetPitch + dy * 0.3f));
                        }
                        lastTwoFingerX = curX; lastTwoFingerY = curY;
                        lastTwoFingerAngle = curAngle; lastTwoFingerDist = curDist;
                        invalidate();
                    }
                    break;
                case MotionEvent.ACTION_POINTER_UP: 
                    isTwoFingerDragging = false;
                    break;
            }
            return true;
        }

        float rawX = event.getX();
        float rawY = event.getY();
        float[] pts = {rawX, rawY};
        Matrix inv = new Matrix(); 
        getFinalTransformMatrix().invert(inv); 
        inv.mapPoints(pts);
        float x = pts[0]; float y = pts[1];

        if (isPenMode || isEraserMode) {
             switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (isPenMode) {
                        Path path = new Path();
                        path.moveTo(x, y);
                        Stroke stroke = new Stroke(path, new Paint(penPaint));
                        stroke.addPoint(x, y);
                        strokes.add(stroke);
                    } else {
                        eraseStrokesAt(x, y);
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (isPenMode && !strokes.isEmpty()) {
                        Stroke last = strokes.get(strokes.size() - 1);
                        last.path.lineTo(x, y);
                        last.addPoint(x, y);
                    } else if (isEraserMode) {
                        eraseStrokesAt(x, y);
                    }
                    break;
            }
            invalidate();
            return true;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchStartX = rawX;
                touchStartY = rawY;
                fromCol = getColFromX(x); fromRow = getRowFromY(y);
                if (chessDelegate != null) {
                    movingPiece = chessDelegate.pieceAt(fromCol, fromRow);
                    if (movingPiece != null) {
                        isDraggingPiece = true;
                        movingPieceBitmap = bitmaps.get(movingPiece.getResID());
                        movingPieceX = x; movingPieceY = y;
                        // Select immediately for hints
                        selectedCol = fromCol; selectedRow = fromRow;
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (isDraggingPiece) {
                    movingPieceX = x; movingPieceY = y;
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                float dist = (float) Math.hypot(rawX - touchStartX, rawY - touchStartY);
                if (dist < TAP_THRESHOLD) {
                    isDraggingPiece = false;
                    handleTap(x, y);
                } else if (isDraggingPiece) {
                    if (chessDelegate != null) {
                        if (chessDelegate.movePiece(fromCol, fromRow, getColFromX(x), getRowFromY(y))) {
                            selectedCol = -1; selectedRow = -1;
                        }
                    }
                    isDraggingPiece = false;
                    movingPiece = null;
                }
                invalidate();
                break;
            case MotionEvent.ACTION_CANCEL:
                isDraggingPiece = false;
                movingPiece = null;
                invalidate();
                break;
        }
        return true;
    }

    private void eraseStrokesAt(float x, float y) {
        float threshold = cellSide / 2;
        Iterator<Stroke> it = strokes.iterator();
        while (it.hasNext()) {
            if (it.next().isNear(x, y, threshold)) {
                it.remove();
            }
        }
    }

    private float calculateAngle(MotionEvent event) {
        float dx = event.getX(0) - event.getX(1);
        float dy = event.getY(0) - event.getY(1);
        return (float) Math.toDegrees(Math.atan2(dy, dx));
    }

    private float calculateDeltaAngle(float cur, float last) {
        float delta = cur - last;
        while (delta > 180) delta -= 360;
        while (delta < -180) delta += 360;
        return delta;
    }

    private void updateBoardMetrics() {
        float boardSide = Math.min(getWidth(), getHeight()) * 0.85f;
        cellSide = boardSide / 8f;
        originX = (getWidth() - boardSide) / 2f; originY = (getHeight() - boardSide) / 2f;
    }

    private int getColFromX(float x) { return (int) ((x - originX) / cellSide) + 1; }
    private int getRowFromY(float y) { return 8 - (int) ((y - originY) / cellSide); }
    private float getScreenX(int col) { return originX + (col - 1) * cellSide; }
    private float getScreenY(int row) { return originY + (8 - row) * cellSide; }

    public void setChessDelegate(ChessDelegate delegate) { 
        this.chessDelegate = delegate; 
        if (delegate != null) setBoardOrientation(delegate.blackPointOfView());
    }
    
    public void moveCursor(int dx, int dy) {
        cursorCol = Math.max(1, Math.min(8, cursorCol + dx));
        cursorRow = Math.max(1, Math.min(8, cursorRow + dy));
        invalidate();
    }

    public void selectCursor() {
        float[] pts = {getScreenX(cursorCol) + cellSide/2, getScreenY(cursorRow) + cellSide/2};
        handleTap(pts[0], pts[1]);
    }

    public void recenter() {
        targetPitch = 0f; targetYaw = 0f; targetRotation = 0f; targetZoom = 1.0f; targetPanX = 0f; targetPanY = 0f;
        invalidate();
    }

    public void zoomIn() { targetZoom = Math.min(MAX_ZOOM, targetZoom * 1.3f); invalidate(); }
    public void zoomOut() { targetZoom = Math.max(MIN_ZOOM, targetZoom / 1.3f); invalidate(); }
    public void setBoardOrientation(boolean isBlack) { targetRotation = isBlack ? 180f : 0f; invalidate(); }
    public void setGravityTilt(float pitch, float yaw) { this.gravityPitch = pitch * 0.4f; this.gravityYaw = yaw * 0.4f; invalidate(); }
    public void setPenMode(boolean penMode) { this.isPenMode = penMode; if (penMode) this.isEraserMode = false; invalidate(); }
    public void setEraserMode(boolean eraserMode) { this.isEraserMode = eraserMode; if (eraserMode) this.isPenMode = false; invalidate(); }

    private void handleTap(float x, float y) {
        int col = getColFromX(x); int row = getRowFromY(y);
        if (col < 1 || col > 8 || row < 1 || row > 8) {
            selectedCol = -1; selectedRow = -1;
        } else {
            if (selectedCol == col && selectedRow == row) {
                selectedCol = -1; selectedRow = -1;
            } else if (selectedCol != -1 && chessDelegate != null && chessDelegate.movePiece(selectedCol, selectedRow, col, row)) {
                selectedCol = -1; selectedRow = -1;
            } else {
                Piece p = (chessDelegate != null) ? chessDelegate.pieceAt(col, row) : null;
                if (p != null) { selectedCol = col; selectedRow = row; }
                else { selectedCol = -1; selectedRow = -1; }
            }
        }
        invalidate();
    }

    private static class PiecePos {
        Piece piece; int col, row; float projY;
        PiecePos(Piece p, int c, int r, float py) { piece = p; col = c; row = r; projY = py; }
    }

    private static class Stroke {
        Path path; Paint paint; List<PointF> points = new ArrayList<>();
        Stroke(Path path, Paint paint) { this.path = path; this.paint = paint; }
        void addPoint(float x, float y) { points.add(new PointF(x, y)); }
        boolean isNear(float x, float y, float threshold) {
            for (PointF p : points) { if (Math.hypot(p.x - x, p.y - y) < threshold) return true; }
            return false;
        }
    }
}
