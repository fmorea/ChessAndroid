package com.fmorea.chess;

public class Movement {
    private int x;
    private int y;

    private int x0;
    private int y0;

    public void set(int y0, int x0, int y, int x){
        this.y = y;
        this.x0 = x0;
        this.x = x;
        this.y0 = y0;
    }

    public int getX0() {
        return x0;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getY0() {
        return y0;
    }

    // Overriding toString() method of String class
    @Override
    public String toString() {
        return "["+x0+","+y0+"] --> ["+x+","+y+"]\n";
    }
}
