package com.fmorea.chess;

public class ChessModel {
    private final GameLogic gameLogic = new GameLogic();
    private boolean showReachableSquares = false;
    private boolean blackPointOfView = false;
    private boolean autoRotate = false;

    private ChessDelegate chessDelegate;

    public ChessModel() {
        reset();
        gameLogic.print();
    }

    public boolean movePiece(int fromCol, int fromRow, int toCol, int toRow) {
        boolean hasMoved = gameLogic.move(fromRow, fromCol, toRow, toCol);
        if (hasMoved && chessDelegate != null && chessDelegate.autoRotate()) {
            chessDelegate.setOrientation(!chessDelegate.blackPointOfView());
        }
        return hasMoved;
    }

    public void reset() {
        gameLogic.createStandardChessboard();
    }

    public ChessPiece pieceAt(int col, int row) {
        if (gameLogic.getPezzo(row, col) == null) return null;
        char color = gameLogic.getColorePezzo(row, col);
        char type = gameLogic.getTipoPezzo(row, col);

        if (color == 'B') {
            switch (type) {
                case 'p': return new ChessPiece(col, row, ChessPlayer.WHITE, ChessRank.PAWN, R.drawable.pawn_white);
                case 'a': return new ChessPiece(col, row, ChessPlayer.WHITE, ChessRank.BISHOP, R.drawable.bishop_white);
                case 'c': return new ChessPiece(col, row, ChessPlayer.WHITE, ChessRank.KNIGHT, R.drawable.knight_white);
                case 'r': return new ChessPiece(col, row, ChessPlayer.WHITE, ChessRank.KING, R.drawable.king_white);
                case 'd': return new ChessPiece(col, row, ChessPlayer.WHITE, ChessRank.QUEEN, R.drawable.queen_white);
                case 't': return new ChessPiece(col, row, ChessPlayer.WHITE, ChessRank.ROOK, R.drawable.rook_white);
            }
        } else if (color == 'N') {
            switch (type) {
                case 'p': return new ChessPiece(col, row, ChessPlayer.BLACK, ChessRank.PAWN, R.drawable.pawn_black);
                case 'a': return new ChessPiece(col, row, ChessPlayer.BLACK, ChessRank.BISHOP, R.drawable.bishop_black);
                case 'c': return new ChessPiece(col, row, ChessPlayer.BLACK, ChessRank.KNIGHT, R.drawable.knight_black);
                case 'r': return new ChessPiece(col, row, ChessPlayer.BLACK, ChessRank.KING, R.drawable.king_black);
                case 'd': return new ChessPiece(col, row, ChessPlayer.BLACK, ChessRank.QUEEN, R.drawable.queen_black);
                case 't': return new ChessPiece(col, row, ChessPlayer.BLACK, ChessRank.ROOK, R.drawable.rook_black);
            }
        }
        return null;
    }

    public GameLogic getGameLogic() { return gameLogic; }
    public boolean isShowReachableSquares() { return showReachableSquares; }
    public void setShowReachableSquares(boolean showReachableSquares) { this.showReachableSquares = showReachableSquares; }
    public boolean isBlackPointOfView() { return blackPointOfView; }
    public void setBlackPointOfView(boolean blackPointOfView) { this.blackPointOfView = blackPointOfView; }
    public boolean isAutoRotate() { return autoRotate; }
    public void setAutoRotate(boolean autoRotate) { this.autoRotate = autoRotate; }
    //public ChessDelegate getChessDelegate() { return chessDelegate; }
    public void setChessDelegate(ChessDelegate chessDelegate) { this.chessDelegate = chessDelegate; }
}
