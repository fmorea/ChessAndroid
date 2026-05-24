package com.fmorea.chess;

import java.util.ArrayList;

public interface ChessDelegate {
    Piece pieceAt(int col, int row);
    Boolean movePiece(int fromCol, int fromRow, int toCol, int toRow);
    Boolean blackPointOfView();
    Boolean autoRotate();
    void setOrientation(boolean orientation);
    ArrayList<Movement> getLegalMoves();
    
    // New methods for graphical info
    boolean isWhiteTurn();
    int getMaterialCount();
    String getNetworkStatus();
    boolean isConnected();
}
