package com.fmorea.chess;

import java.util.ArrayList;

public interface ChessDelegate {
    ChessPiece pieceAt(int col, int row);
    Boolean movePiece(int fromCol, int fromRow, int toCol, int toRow);
    Boolean showAllReachableSquares();
    Boolean blackPointOfView();
    Boolean autoRotate();
    void setOrientation(boolean orientation);
    ArrayList<Movement> getLegalMoves();
}
