package com.fmorea.chess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * GameLogic handles the core chess engine rules and evaluation.
 * Optimized for performance and advanced positional evaluation.
 */
public class GameLogic {
    private final String[][] matrix;
    private Movement mov = new Movement(0, 0, 0, 0);
    private boolean toccaAlBianco = true;
    private boolean lockTurn = false;
    private String promotionB = "donB";
    private String promotionN = "donN";
    private ArrayList<Movement> legalMoves;
    private final ArrayList<String[][]> history = new ArrayList<>();
    private final ArrayList<String[][]> redoHistory = new ArrayList<>();

    // --- Piece Evaluation Constants ---
    private static final int PAWN_VAL = 100;
    private static final int KNIGHT_VAL = 320;
    private static final int BISHOP_VAL = 330;
    private static final int ROOK_VAL = 500;
    private static final int QUEEN_VAL = 900;
    private static final int KING_VAL = 20000;

    // Piece-Square Tables (PST) - Values are centered for White.
    // They represent the positional value of each square for each piece type.
    private static final int[] PAWN_PST = {
        0,  0,  0,  0,  0,  0,  0,  0,
        50, 50, 50, 50, 50, 50, 50, 50,
        10, 10, 20, 30, 30, 20, 10, 10,
         5,  5, 10, 25, 25, 10,  5,  5,
         0,  0,  0, 20, 20,  0,  0,  0,
         5, -5,-10,  0,  0,-10, -5,  5,
         5, 10, 10,-20,-20, 10, 10,  5,
         0,  0,  0,  0,  0,  0,  0,  0
    };

    private static final int[] KNIGHT_PST = {
        -50,-40,-30,-30,-30,-30,-40,-50,
        -40,-20,  0,  0,  0,  0,-20,-40,
        -30,  0, 10, 15, 15, 10,  0,-30,
        -30,  5, 15, 20, 20, 15,  5,-30,
        -30,  0, 15, 20, 20, 15,  0,-30,
        -30,  5, 10, 15, 15, 10,  5,-30,
        -40,-20,  0,  5,  5,  0,-20,-40,
        -50,-40,-30,-30,-30,-30,-40,-50
    };

    private static final int[] BISHOP_PST = {
        -20,-10,-10,-10,-10,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  5, 10, 10,  5,  0,-10,
        -10,  5,  5, 10, 10,  5,  5,-10,
        -10,  0, 10, 10, 10, 10,  0,-10,
        -10, 10, 10, 10, 10, 10, 10,-10,
        -10,  5,  0,  0,  0,  0,  5,-10,
        -20,-10,-10,-10,-10,-10,-10,-20
    };

    private static final int[] ROOK_PST = {
          0,  0,  0,  0,  0,  0,  0,  0,
          5, 10, 10, 10, 10, 10, 10,  5,
         -5,  0,  0,  0,  0,  0,  0, -5,
         -5,  0,  0,  0,  0,  0,  0, -5,
         -5,  0,  0,  0,  0,  0,  0, -5,
         -5,  0,  0,  0,  0,  0,  0, -5,
         -5,  0,  0,  0,  0,  0,  0, -5,
          0,  0,  0,  5,  5,  0,  0,  0
    };

    private static final int[] QUEEN_PST = {
        -20,-10,-10, -5, -5,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  5,  5,  5,  5,  0,-10,
         -5,  0,  5,  5,  5,  5,  0, -5,
          0,  0,  5,  5,  5,  5,  0, -5,
        -10,  5,  5,  5,  5,  5,  0,-10,
        -10,  0,  5,  0,  0,  0,  0,-10,
        -20,-10,-10, -5, -5,-10,-10,-20
    };

    private static final int[] KING_PST = {
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -20,-30,-30,-40,-40,-30,-30,-20,
        -10,-20,-20,-20,-20,-20,-20,-10,
         20, 20,  0,  0,  0,  0, 20, 20,
         20, 30, 10,  0,  0, 10, 30, 20
    };

    private static final int[] KING_ENDGAME_PST = {
        -50,-40,-30,-20,-20,-30,-40,-50,
        -30,-20,-10,  0,  0,-10,-20,-30,
        -30,-10, 20, 30, 30, 20,-10,-30,
        -30,-10, 30, 40, 40, 30,-10,-30,
        -30,-10, 30, 40, 40, 30,-10,-30,
        -30,-10, 20, 30, 30, 20,-10,-30,
        -30,-30,  0,  0,  0,  0,-30,-30,
        -50,-30,-30,-30,-30,-30,-30,-50
    };

    private static final Map<String, ArrayList<Movement>> moveDatabase = new HashMap<>();

    public GameLogic() {
        this.matrix = new String[8][8];
    }

    public void createStandardChessboard() {
        for (int i = 0; i < 8; i++) Arrays.fill(matrix[i], null);
        for (int x = 1; x <= 8; x++) setPezzo(2, x, "pedB");
        setPezzo(1, 1, "torB"); setPezzo(1, 8, "torB");
        setPezzo(1, 2, "cavB"); setPezzo(1, 7, "cavB");
        setPezzo(1, 3, "alfB"); setPezzo(1, 6, "alfB");
        setPezzo(1, 4, "donB"); setPezzo(1, 5, "re_B");
        for (int x = 1; x <= 8; x++) setPezzo(7, x, "pedN");
        setPezzo(8, 1, "torN"); setPezzo(8, 8, "torN");
        setPezzo(8, 2, "cavN"); setPezzo(8, 7, "cavN");
        setPezzo(8, 3, "alfN"); setPezzo(8, 6, "alfN");
        setPezzo(8, 4, "donN"); setPezzo(8, 5, "re_N");

        this.toccaAlBianco = true;
        this.history.clear();
        this.redoHistory.clear();
        moveDatabase.clear();
        updateLegalMoves();
    }

    private String getBoardFingerprint() {
        StringBuilder sb = new StringBuilder(70);
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                String p = matrix[i][j];
                if (p == null) sb.append('.');
                else sb.append(p.charAt(0)).append(p.charAt(3));
            }
        }
        sb.append(toccaAlBianco ? 'W' : 'B');
        return sb.toString();
    }

    public void updateLegalMoves() {
        String fingerprint = getBoardFingerprint();
        if (moveDatabase.containsKey(fingerprint)) {
            this.legalMoves = new ArrayList<>(moveDatabase.get(fingerprint));
            return;
        }

        ArrayList<Movement> pseudo = calculateAllPseudoLegalMoves();
        this.legalMoves = filterLegalMoves(pseudo);
        moveDatabase.put(fingerprint, new ArrayList<>(this.legalMoves));
    }

    public boolean isInCheck() {
        char myColor = toccaAlBianco ? 'B' : 'N';
        int ky = -1, kx = -1;
        for (int y = 1; y <= 8; y++) {
            for (int x = 1; x <= 8; x++) {
                if (getTipoPezzo(y, x) == 'r' && getColorePezzo(y, x) == myColor) {
                    ky = y; kx = x; break;
                }
            }
            if (ky != -1) break;
        }
        return isSquareAttacked(ky, kx, toccaAlBianco ? 'N' : 'B');
    }

    private boolean isSquareAttacked(int ty, int tx, char attackerColor) {
        if (ty == -1) return false;
        int[][] knightMoves = {{2,1},{2,-1},{-2,1},{-2,-1},{1,2},{1,-2},{-1,2},{-1,-2}};
        for (int[] m : knightMoves) {
            int y = ty + m[0], x = tx + m[1];
            if (isInsideChessBoard(y, x) && getTipoPezzo(y, x) == 'c' && getColorePezzo(y, x) == attackerColor) return true;
        }
        int pDir = (attackerColor == 'B') ? -1 : 1;
        int[] pxAttacks = {tx - 1, tx + 1};
        for (int x : pxAttacks) {
            int y = ty + pDir;
            if (isInsideChessBoard(y, x) && getTipoPezzo(y, x) == 'p' && getColorePezzo(y, x) == attackerColor) return true;
        }
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}};
        for (int i = 0; i < 8; i++) {
            int y = ty + dirs[i][0], x = tx + dirs[i][1];
            if (isInsideChessBoard(y, x) && getTipoPezzo(y, x) == 'r' && getColorePezzo(y, x) == attackerColor) return true;
            while (isInsideChessBoard(y, x)) {
                String p = getPezzo(y, x);
                if (p != null) {
                    if (p.charAt(3) == attackerColor) {
                        char type = p.charAt(0);
                        if (i < 4 && (type == 't' || type == 'd')) return true;
                        if (i >= 4 && (type == 'a' || type == 'd')) return true;
                    }
                    break;
                }
                y += dirs[i][0]; x += dirs[i][1];
            }
        }
        return false;
    }

    private ArrayList<Movement> filterLegalMoves(ArrayList<Movement> pseudo) {
        ArrayList<Movement> legal = new ArrayList<>();
        for (Movement m : pseudo) {
            String captured = makeMove(m);
            if (!isInCheck()) legal.add(m);
            unmakeMove(m, captured);
        }
        return legal;
    }

    private String makeMove(Movement m) {
        String p = getPezzo(m.getY0(), m.getX0());
        String captured = getPezzo(m.getY(), m.getX());
        setPezzo(m.getY0(), m.getX0(), null);
        setPezzo(m.getY(), m.getX(), p);
        return captured;
    }

    private void unmakeMove(Movement m, String captured) {
        String p = getPezzo(m.getY(), m.getX());
        setPezzo(m.getY(), m.getX(), captured);
        setPezzo(m.getY0(), m.getX0(), p);
    }

    private ArrayList<Movement> calculateAllPseudoLegalMoves() {
        ArrayList<Movement> moves = new ArrayList<>();
        for (int y = 1; y <= 8; y++) {
            for (int x = 1; x <= 8; x++) {
                String p = getPezzo(y, x);
                if (p != null && p.charAt(3) == (toccaAlBianco ? 'B' : 'N')) {
                    generatePseudoMoves(y, x, moves);
                }
            }
        }
        return moves;
    }

    private void generatePseudoMoves(int y0, int x0, ArrayList<Movement> moves) {
        for (int y = 1; y <= 8; y++) {
            for (int x = 1; x <= 8; x++) {
                if (isPlausibleMove(y0, x0, y, x)) moves.add(new Movement(y0, x0, y, x));
            }
        }
    }

    private boolean isPlausibleMove(int y0, int x0, int y, int x) {
        if (y0 == y && x0 == x) return false;
        String target = getPezzo(y, x);
        if (target != null && target.charAt(3) == (toccaAlBianco ? 'B' : 'N')) return false;
        char type = getTipoPezzo(y0, x0);
        int dy = Math.abs(y - y0), dx = Math.abs(x - x0);
        switch (type) {
            case 'p':
                if (x == x0) {
                    if (target != null) return false;
                    if (toccaAlBianco) return (y == y0 + 1) || (y0 == 2 && y == 4 && isEmpty(3, x));
                    else return (y == y0 - 1) || (y0 == 7 && y == 5 && isEmpty(6, x));
                } else if (dx == 1) {
                    return target != null && (toccaAlBianco ? y == y0 + 1 : y == y0 - 1);
                }
                return false;
            case 'c': return (dy == 2 && dx == 1) || (dy == 1 && dx == 2);
            case 'r': return dy <= 1 && dx <= 1;
            case 't': if (y != y0 && x != x0) return false; break;
            case 'a': if (dy != dx) return false; break;
            case 'd': if (y != y0 && x != x0 && dy != dx) return false; break;
        }
        if (type == 't' || type == 'a' || type == 'd') {
            int stepY = Integer.compare(y, y0), stepX = Integer.compare(x, x0);
            int curY = y0 + stepY, curX = x0 + stepX;
            while (curY != y || curX != x) {
                if (!isEmpty(curY, curX)) return false;
                curY += stepY; curX += stepX;
            }
        }
        return true;
    }

    public boolean move(int y0, int x0, int y, int x) {
        if (y0 == 0) return true;
        Movement m = new Movement(y0, x0, y, x);
        if (legalMoves.contains(m)) {
            history.add(copy(matrix));
            redoHistory.clear();
            forceMove(y0, x0, y, x);
            if (!lockTurn) toccaAlBianco = !toccaAlBianco;
            updateLegalMoves();
            mov = m;
            return true;
        }
        return false;
    }

    public void forceMove(int y0, int x0, int y, int x) {
        String p = getPezzo(y0, x0);
        if (p == null) return;
        if (y0 == 7 && y == 8 && p.charAt(0) == 'p') p = promotionB;
        if (y0 == 2 && y == 1 && p.charAt(0) == 'p') p = promotionN;
        setPezzo(y0, x0, null);
        setPezzo(y, x, p);
    }

    public String getPezzo(int y, int x) { return isInsideChessBoard(y, x) ? matrix[y - 1][x - 1] : null; }
    public void setPezzo(int y, int x, String p) { if (isInsideChessBoard(y, x)) matrix[y - 1][x - 1] = p; }
    public char getTipoPezzo(int y, int x) { String p = getPezzo(y, x); return p != null ? p.charAt(0) : '0'; }
    public char getColorePezzo(int y, int x) { String p = getPezzo(y, x); return p != null ? p.charAt(3) : '0'; }
    public boolean isEmpty(int y, int x) { return getPezzo(y, x) == null; }
    public boolean isInsideChessBoard(int y, int x) { return y >= 1 && y <= 8 && x >= 1 && x <= 8; }
    public ArrayList<Movement> getLegalMoves() { return legalMoves; }
    public Movement getMov() { return mov; }
    public boolean toccaAlBianco() { return toccaAlBianco; }

    public void undo() {
        if (!history.isEmpty()) {
            redoHistory.add(copy(matrix));
            System.arraycopy(history.remove(history.size() - 1), 0, matrix, 0, 8);
            toccaAlBianco = !toccaAlBianco;
            updateLegalMoves();
        }
    }

    public void redo() {
        if (!redoHistory.isEmpty()) {
            history.add(copy(matrix));
            System.arraycopy(redoHistory.remove(redoHistory.size() - 1), 0, matrix, 0, 8);
            toccaAlBianco = !toccaAlBianco;
            updateLegalMoves();
        }
    }

    private String[][] copy(String[][] s) {
        String[][] d = new String[8][8];
        for (int i = 0; i < 8; i++) System.arraycopy(s[i], 0, d[i], 0, 8);
        return d;
    }

    public int objectiveFunction() {
        int score = 0;
        int whiteBishops = 0, blackBishops = 0;
        int totalMaterial = 0;
        int[] wPawnsFile = new int[10], bPawnsFile = new int[10];

        // 1. Initial scan: Material and structural data
        for (int y = 1; y <= 8; y++) {
            for (int x = 1; x <= 8; x++) {
                String p = getPezzo(y, x);
                if (p == null) continue;
                char type = p.charAt(0);
                char color = p.charAt(3);
                
                if (color == 'B') {
                    if (type == 'a') whiteBishops++;
                    if (type == 'p') wPawnsFile[x]++;
                } else {
                    if (type == 'a') blackBishops++;
                    if (type == 'p') bPawnsFile[x]++;
                }
                if (type != 'r') totalMaterial += getPieceValue(type);
            }
        }

        boolean isEndgame = totalMaterial < 1500;

        // 2. Comprehensive positional evaluation
        for (int y = 1; y <= 8; y++) {
            for (int x = 1; x <= 8; x++) {
                String p = getPezzo(y, x);
                if (p == null) continue;

                char type = p.charAt(0);
                char color = p.charAt(3);
                
                int val = getPieceValue(type);
                // Indice PST: invertito verticalmente per il Nero
                int pstIdx = (color == 'B') ? (8 - y) * 8 + (x - 1) : (y - 1) * 8 + (x - 1);

                switch (type) {
                    case 'p': val += PAWN_PST[pstIdx] + evalPawn(y, x, color, wPawnsFile, bPawnsFile); break;
                    case 'c': val += KNIGHT_PST[pstIdx]; break;
                    case 'a': val += BISHOP_PST[pstIdx]; break;
                    case 't': val += ROOK_PST[pstIdx] + evalRook(y, x, color); break;
                    case 'd': val += QUEEN_PST[pstIdx]; break;
                    case 'r': val += (isEndgame ? KING_ENDGAME_PST[pstIdx] : KING_PST[pstIdx]); break;
                }
                score += (color == 'B' ? val : -val);
            }
        }

        // Bishop pair bonus
        if (whiteBishops >= 2) score += 50;
        if (blackBishops >= 2) score -= 50;

        return score;
    }

    private int getPieceValue(char type) {
        switch (type) {
            case 'p': return PAWN_VAL;
            case 'c': return KNIGHT_VAL;
            case 'a': return BISHOP_VAL;
            case 't': return ROOK_VAL;
            case 'd': return QUEEN_VAL;
            case 'r': return KING_VAL;
            default: return 0;
        }
    }

    private int evalPawn(int y, int x, char color, int[] wFiles, int[] bFiles) {
        int bonus = 0;
        // Doubled pawns
        if (color == 'B' && wFiles[x] > 1) bonus -= 15;
        if (color == 'N' && bFiles[x] > 1) bonus -= 15;
        // Isolated pawn
        if (color == 'B' && wFiles[x-1] == 0 && wFiles[x+1] == 0) bonus -= 20;
        if (color == 'N' && bFiles[x-1] == 0 && bFiles[x+1] == 0) bonus -= 20;
        return bonus;
    }

    private int evalRook(int y, int x, char color) {
        // Bonus for open file
        boolean hasPawn = false;
        for (int r = 1; r <= 8; r++) {
            String p = getPezzo(r, x);
            if (p != null && p.charAt(0) == 'p') { hasPawn = true; break; }
        }
        return hasPawn ? 0 : 20;
    }

    public String serializeBoard() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) sb.append(matrix[i][j] == null ? "null" : matrix[i][j]).append(",");
        }
        sb.append(toccaAlBianco ? "W" : "B");
        return sb.toString();
    }

    public void deserializeBoard(String data) {
        String[] parts = data.split(",");
        if (parts.length < 65) return;
        for (int i = 0; i < 64; i++) matrix[i / 8][i % 8] = parts[i].equals("null") ? null : parts[i];
        toccaAlBianco = "W".equals(parts[64]);
        history.clear();
        redoHistory.clear();
        moveDatabase.clear();
        updateLegalMoves();
    }
}
