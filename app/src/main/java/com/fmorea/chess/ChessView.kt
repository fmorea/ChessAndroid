package com.fmorea.chess

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.min

class ChessView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
    private val scaleFactor = 1.09f
    private var originX = 0f
    private var originY = 0f
    private var cellSide = 130f
    private var lightColor = Color.parseColor("#2D942D")
    private var darkColor = Color.parseColor("#DEDFC4")
    private val imgResIDs = setOf(
        R.drawable.bishop_black,
        R.drawable.bishop_white,
        R.drawable.king_black,
        R.drawable.king_white,
        R.drawable.queen_black,
        R.drawable.queen_white,
        R.drawable.rook_black,
        R.drawable.rook_white,
        R.drawable.knight_black,
        R.drawable.knight_white,
        R.drawable.pawn_black,
        R.drawable.pawn_white,
    )
    private val bitmaps = mutableMapOf<Int, Bitmap>()
    private val paint = Paint()

    private var movingPieceBitmap: Bitmap? = null
    private var movingPiece: ChessPiece? = null
    private var fromCol: Int = -1
    private var fromRow: Int = -1
    private var movingPieceX = -1f
    private var movingPieceY = -1f
    var isMoving = false

    var chessDelegate: ChessDelegate? = null

    init {
        loadBitmaps()
    }

    override fun onDraw(canvas: Canvas?) {
        canvas ?: return

        val chessBoardSide = min(width, height) * scaleFactor
        cellSide = chessBoardSide / 9f
        originX = (width - chessBoardSide) / 2f + (1f/18f)*chessBoardSide
        originY = (height - chessBoardSide) / 5f

        drawChessboard(canvas)
        drawPieces(canvas)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event ?: return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                fromCol = ((event.x - originX) / cellSide).toInt()+1
                fromRow = 7 - ((event.y - originY) / cellSide).toInt()+1

                chessDelegate?.pieceAt(fromCol, fromRow)?.let {
                    movingPiece = it
                    movingPieceBitmap = bitmaps[it.resID]
                }
            }
            MotionEvent.ACTION_MOVE -> {
                isMoving = true
                movingPieceX = event.x
                movingPieceY = event.y
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                val col = ((event.x - originX) / cellSide).toInt()+1
                val row = 7 - ((event.y - originY) / cellSide).toInt()+1
                if(col in 1..8 && row in 1..8) {
                    chessDelegate?.movePiece(fromCol, fromRow, col, row)
                }
                if(col == fromCol && row == fromRow){
                    chessDelegate?.pieceAt(fromCol, fromRow)?.let {
                        movingPiece = it
                        movingPieceBitmap = bitmaps[it.resID]
                    }
                }
                else {
                    movingPiece = null
                    movingPieceBitmap = null
                }
                isMoving = false
                invalidate()

            }
        }
        return true
    }

    private fun drawPieces(canvas: Canvas) {
        for (row in 1..8) {
            for (col in 1..8) {
                chessDelegate?.pieceAt(col, row)?.let {
                    if ((!isMoving && it == movingPiece) || it != movingPiece) {
                        drawPieceAt(canvas, col, row , it.resID)
                    }
                }
            }
        }

        movingPieceBitmap?.let {
            canvas.drawBitmap(it, null, RectF(movingPieceX - cellSide/2, movingPieceY - cellSide/2,movingPieceX + cellSide/2,movingPieceY + cellSide/2), paint)
        }
    }

    private fun drawPieceAt(canvas: Canvas, col: Int, row: Int, resID: Int) {
        val bitmap = bitmaps[resID]!!
        canvas.drawBitmap(bitmap, null, RectF(originX + (col - 1) * cellSide,originY + (7 - (row-1)) * cellSide,originX + ((col-1) + 1) * cellSide,originY + ((7 - (row-1)) + 1) * cellSide), paint)
    }

    private fun loadBitmaps() {
        imgResIDs.forEach {
            bitmaps[it] = BitmapFactory.decodeResource(resources, it)
        }
    }

    private fun drawChessboard(canvas: Canvas) {

        for (row in 1..8) {
            for (col in 1..8) {
                drawSquareAt(canvas, col, row, (col + row) % 2 == 0)
            }
        }
    }

    private fun drawSquareAt(canvas: Canvas, col: Int, row: Int, isDark: Boolean) {
        var isReachable = false
        var isReachableFromTheMovingPiece = false

        for(mov in chessDelegate?.getLegalMoves()!!){
            if (movingPiece?.col == mov.x0 && movingPiece?.row == mov.y0 && col == mov.x && row == mov.y){
                isReachableFromTheMovingPiece = true
                break
            }
        }

        if (chessDelegate?.SwitchOn() == true){
            for(mov in chessDelegate?.getLegalMoves()!!){
                if (mov.x == col && mov.y  == row){
                    isReachable = true
                    break
                }
            }
        }

        if (isReachable){
            darkColor = Color.parseColor("#FFFF2D")
            lightColor = Color.parseColor("#CCCC00")
        }
        if(isReachableFromTheMovingPiece){
            darkColor = Color.parseColor("#E68A00")
            lightColor = Color.parseColor("#FFB84D")
        }
        paint.color = if (isDark) darkColor else lightColor
        canvas.drawRect(originX + (col-1) * cellSide, originY + ((9-row)-1) * cellSide, originX + col* cellSide, originY + (9-row) * cellSide, paint)
        if (isReachable || isReachableFromTheMovingPiece){
            lightColor = Color.parseColor("#2D942D")
            darkColor = Color.parseColor("#DEDFC4")
        }
    }
}
