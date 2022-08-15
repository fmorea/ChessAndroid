package com.fmorea.chess
import android.util.Log

class ChessModel {
    private var gameLogic = GameLogic();

    init {
        reset()
        gameLogic.print()
    }

    fun movePiece(fromCol: Int, fromRow: Int, toCol: Int, toRow: Int) : Boolean {
        return gameLogic.move(fromRow+1,fromCol+1,toRow+1,toCol+1)
    }

    private fun reset() {
        gameLogic.createStandardChessboard();
    }

    fun pieceAt(col: Int, row: Int) : ChessPiece? {
        if (gameLogic.getPezzo(row+1,col+1) == null) return null;
        if(gameLogic.getColorePezzo(row+1,col+1) == 'B'){
            when (gameLogic.getTipoPezzo(row+1,col+1)) {
                'p' -> return ChessPiece(col,row,ChessPlayer.WHITE,ChessRank.PAWN,R.drawable.pawn_white)
                'a' -> return ChessPiece(col,row,ChessPlayer.WHITE,ChessRank.BISHOP,R.drawable.bishop_white)
                'c' -> return ChessPiece(col,row,ChessPlayer.WHITE,ChessRank.KNIGHT,R.drawable.knight_white)
                'r' -> return ChessPiece(col,row,ChessPlayer.WHITE,ChessRank.KING,R.drawable.king_white)
                'd' -> return ChessPiece(col,row,ChessPlayer.WHITE,ChessRank.QUEEN,R.drawable.queen_white)
                't' -> return ChessPiece(col,row,ChessPlayer.WHITE,ChessRank.ROOK,R.drawable.rook_white)
            }
        }
        else if(gameLogic.getColorePezzo(row+1,col+1) == 'N'){
            when (gameLogic.getTipoPezzo(row+1,col+1)) {
                'p' -> return ChessPiece(col,row,ChessPlayer.BLACK,ChessRank.PAWN,R.drawable.pawn_black)
                'a' -> return ChessPiece(col,row,ChessPlayer.BLACK,ChessRank.BISHOP,R.drawable.bishop_black)
                'c' -> return ChessPiece(col,row,ChessPlayer.BLACK,ChessRank.KNIGHT,R.drawable.knight_black)
                'r' -> return ChessPiece(col,row,ChessPlayer.BLACK,ChessRank.KING,R.drawable.king_black)
                'd' -> return ChessPiece(col,row,ChessPlayer.BLACK,ChessRank.QUEEN,R.drawable.queen_black)
                't' -> return ChessPiece(col,row,ChessPlayer.BLACK,ChessRank.ROOK,R.drawable.rook_black)
            }
        }
        return null
    }

}