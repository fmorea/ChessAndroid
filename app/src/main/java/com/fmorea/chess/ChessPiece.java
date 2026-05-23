package com.fmorea.chess;

import java.util.Objects;

public class ChessPiece {
    private final int col;
    private final int row;
    private final ChessPlayer player;
    private final ChessRank rank;
    private final int resID;

    public ChessPiece(int col, int row, ChessPlayer player, ChessRank rank, int resID) {
        this.col = col;
        this.row = row;
        this.player = player;
        this.rank = rank;
        this.resID = resID;
    }

    public int getCol() { return col; }
    public int getRow() { return row; }
    public ChessPlayer getPlayer() { return player; }
    public ChessRank getRank() { return rank; }
    public int getResID() { return resID; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChessPiece that = (ChessPiece) o;
        return col == that.col && row == that.row && resID == that.resID && player == that.player && rank == that.rank;
    }

    @Override
    public int hashCode() {
        return Objects.hash(col, row, player, rank, resID);
    }
}
