package com.fmorea.chess;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ChessView extends View {
    private final float scaleFactor = 1.09f;
    private float originX = 0f;
    private float originY = 0f;
    private float cellSide = 130f;
    private int lightColor = Color.parseColor("#2D942D");
    private int darkColor = Color.parseColor("#DEDFC4");

    private final Set<Integer> imgResIDs = new HashSet<Integer>() {{
        add(R.drawable.bishop_black);
        add(R.drawable.bishop_white);
        add(R.drawable.king_black);
        add(R.drawable.king_white);
        add(R.drawable.queen_black);
        add(R.drawable.queen_white);
        add(R.drawable.rook_black);
        add(R.drawable.rook_white);
        add(R.drawable.knight_black);
        add(R.drawable.knight_white);
        add(R.drawable.pawn_black);
        add(R.drawable.pawn_white);
    }};

    private final Map<Integer, Bitmap> bitmaps = new HashMap<>();
    private final Paint paint = new Paint();

    private Bitmap movingPieceBitmap = null;
    private ChessPiece movingPiece = null;
    private int fromCol = 9999;
    private int fromRow = 9999;
    private int fromColOld = 9999;
    private int fromRowOld = 9999;
    private float movingPieceX = 9999f;
    private float movingPieceY = 9999f;
    private boolean isMoving = false;

    private ChessDelegate chessDelegate = null;

    public ChessView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        loadBitmaps();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float chessBoardSide = Math.min(getWidth(), getHeight()) * scaleFactor;
        cellSide = chessBoardSide / 9f;
        originX = (getWidth() - chessBoardSide) / 2f + (1f / 18f) * chessBoardSide;
        originY = (getHeight() - chessBoardSide) / 5f;

        drawChessboard(canvas);
        drawPieces(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event == null) return false;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                fromColOld = fromCol;
                fromRowOld = fromRow;
                if (chessDelegate != null && Boolean.TRUE.equals(chessDelegate.blackPointOfView())) {
                    fromCol = 9 - (((int) ((event.getX() - originX) / cellSide)) + 1);
                    fromRow = 9 - (7 - ((int) ((event.getY() - originY) / cellSide)) + 1);
                } else {
                    fromCol = (int) ((event.getX() - originX) / cellSide) + 1;
                    fromRow = 7 - (int) ((event.getY() - originY) / cellSide) + 1;
                }

                if (chessDelegate != null) {
                    ChessPiece piece = chessDelegate.pieceAt(fromCol, fromRow);
                    if (piece != null) {
                        movingPiece = piece;
                        movingPieceBitmap = bitmaps.get(piece.getResID());
                    }
                }
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                isMoving = true;
                movingPieceX = event.getX();
                movingPieceY = event.getY();
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                movingPieceX = 99999f;
                movingPieceY = 99999f;
                isMoving = false;
                int col;
                int row;
                if (chessDelegate != null && Boolean.TRUE.equals(chessDelegate.blackPointOfView())) {
                    col = 9 - (((int) ((event.getX() - originX) / cellSide)) + 1);
                    row = 9 - (7 - ((int) ((event.getY() - originY) / cellSide)) + 1);
                } else {
                    col = (int) ((event.getX() - originX) / cellSide) + 1;
                    row = 7 - (int) ((event.getY() - originY) / cellSide) + 1;
                }

                if (col >= 1 && col <= 8 && row >= 1 && row <= 8) {
                    if (chessDelegate != null) {
                        chessDelegate.movePiece(fromCol, fromRow, col, row);
                    }
                }

                if (col == fromCol && row == fromRow) {
                    if (chessDelegate != null) {
                        ChessPiece piece = chessDelegate.pieceAt(fromCol, fromRow);
                        if (piece != null) {
                            movingPiece = piece;
                            movingPieceBitmap = bitmaps.get(piece.getResID());
                        }
                    }
                    if (fromColOld != 9999 && fromRowOld != 9999 && fromRowOld >= 1 && fromRowOld <= 8 && fromColOld >= 1 && fromColOld <= 8 && fromRow >= 1 && fromRow <= 8 && fromCol >= 1 && fromCol <= 8) {
                        if (chessDelegate != null) {
                            chessDelegate.movePiece(fromColOld, fromRowOld, fromCol, fromRow);
                        }
                    }
                } else {
                    movingPiece = null;
                    movingPieceBitmap = null;
                }
                invalidate();
                break;
        }
        return true;
    }

    private void drawPieces(Canvas canvas) {
        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                if (chessDelegate != null) {
                    ChessPiece piece = chessDelegate.pieceAt(col, row);
                    if (piece != null) {
                        if ((!isMoving && piece.equals(movingPiece)) || (!piece.equals(movingPiece))) {
                            drawPieceAt(canvas, col, row, piece.getResID());
                        }
                    }
                }
            }
        }

        if (movingPieceBitmap != null) {
            canvas.drawBitmap(movingPieceBitmap, null, new RectF(movingPieceX - cellSide / 2, movingPieceY - cellSide / 2, movingPieceX + cellSide / 2, movingPieceY + cellSide / 2), paint);
        }
    }

    private void drawPieceAt(Canvas canvas, int col, int row, int resID) {
        Bitmap bitmap = bitmaps.get(resID);
        if (bitmap != null && row >= 1 && row <= 8 && col >= 1 && col <= 8) {
            if (chessDelegate != null && Boolean.TRUE.equals(chessDelegate.blackPointOfView())) {
                canvas.drawBitmap(bitmap, null, new RectF(originX + ((9 - col) - 1) * cellSide, originY + (7 - ((9 - row) - 1)) * cellSide, originX + (((9 - col) - 1) + 1) * cellSide, originY + ((7 - ((9 - row) - 1)) + 1) * cellSide), paint);
            } else {
                canvas.drawBitmap(bitmap, null, new RectF(originX + (col - 1) * cellSide, originY + (7 - (row - 1)) * cellSide, originX + ((col - 1) + 1) * cellSide, originY + ((7 - (row - 1)) + 1) * cellSide), paint);
            }
        }
    }

    private void loadBitmaps() {
        for (int resID : imgResIDs) {
            bitmaps.put(resID, BitmapFactory.decodeResource(getResources(), resID));
        }
    }

    private void drawChessboard(Canvas canvas) {
        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                if (chessDelegate != null && Boolean.TRUE.equals(chessDelegate.blackPointOfView())) {
                    drawSquareAt(canvas, 9 - col, 9 - row, (col + row) % 2 == 1);
                } else {
                    drawSquareAt(canvas, col, row, (col + row) % 2 == 1);
                }
            }
        }
    }

    private void drawSquareAt(Canvas canvas, int col, int row, boolean isDark) {
        boolean isReachable = false;
        boolean isReachableFromTheMovingPiece = false;

        if (chessDelegate != null && chessDelegate.getLegalMoves() != null) {
            for (Movement mov : chessDelegate.getLegalMoves()) {
                if (movingPiece != null && movingPiece.getCol() == mov.getX0() && movingPiece.getRow() == mov.getY0() && col == mov.getX() && row == mov.getY()) {
                    isReachableFromTheMovingPiece = true;
                    break;
                }
            }

            if (Boolean.TRUE.equals(chessDelegate.showAllReachableSquares())) {
                for (Movement mov : chessDelegate.getLegalMoves()) {
                    if (mov.getX() == col && mov.getY() == row) {
                        isReachable = true;
                        break;
                    }
                }
            }
        }

        int tempDarkColor = darkColor;
        int tempLightColor = lightColor;

        if (isReachable) {
            tempDarkColor = Color.parseColor("#FFFF2D");
            tempLightColor = Color.parseColor("#CCCC00");
        }
        if (isReachableFromTheMovingPiece) {
            tempLightColor = Color.parseColor("#E68A00");
            tempDarkColor = Color.parseColor("#FFB84D");
        }
        paint.setColor(isDark ? tempDarkColor : tempLightColor);
        if (chessDelegate != null && Boolean.TRUE.equals(chessDelegate.blackPointOfView())) {
            canvas.drawRect(
                    originX + ((9 - col) - 1) * cellSide,
                    originY + ((row) - 1) * cellSide,
                    originX + (9 - col) * cellSide,
                    originY + (row) * cellSide,
                    paint
            );
        } else if (chessDelegate != null && Boolean.FALSE.equals(chessDelegate.blackPointOfView())) {
            canvas.drawRect(
                    originX + (col - 1) * cellSide,
                    originY + ((9 - row) - 1) * cellSide,
                    originX + col * cellSide,
                    originY + (9 - row) * cellSide,
                    paint
            );
        }
    }

    public void setChessDelegate(ChessDelegate chessDelegate) {
        this.chessDelegate = chessDelegate;
    }

    public ChessPiece getMovingPiece() {
        return movingPiece;
    }

    public void setMovingPiece(ChessPiece movingPiece) {
        this.movingPiece = movingPiece;
    }
}
