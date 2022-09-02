package com.fmorea.chess;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * La scacchiera è una matrice di stringhe 8x8
 * Gli elementi della matrice sono in gergo scacchistico chiamate "case"
 * Una casa può essere vuota (Stringa = null)
 * oppure può essere occupata da un pezzo,
 * per il nome dei pezzi si è scelto un nome di 4 lettere, la cui ultima
 * lettera maiuscola rappresenta il colore del pezzo (B per bianco, N per nero)
 * la prima lettera rappresenta il nome del pezzo in italiano (p,d,t,a,c)
 * le rimanenti 2 lettere centrali del nome possono essere utilizzate come meta-dati in casi particolari
 */
public class GameLogic {
    // Gli attributi privati rappresentano lo stato della partita
    private String[][] matrix; //rappresentazione della scacchiera
    private boolean toccaAlBianco = true; //rappresentazione del turno corrente
    private boolean lockTurn = false; //flag booleano per far rimanere il turno al giocatore corrente
    private String promotionB="donB"; //auto promozione a donna per il giocatore bianco
    private String promotionN="donN"; //auto promozione a donna per il giocatore nero
    private ArrayList<Movement> legalMoves; //lista di mosse legali nella situazione corrente
    private ArrayList<Movement> pseudoLegalMoves; //lista di mosse pseudo legali (scacco non considerato) nella posizione corrente
    private ArrayList<String[][]> history=null;
    private String[][] lastUndo = null;


    public GameLogic() {
        this.matrix = new String[8][8];
    }

    public void createStandardChessboard() {
        this.setPezzo(2, 1, "pedB");
        this.setPezzo(2, 2, "pedB");
        this.setPezzo(2, 3, "pedB");
        this.setPezzo(2, 4, "pedB");
        this.setPezzo(2, 5, "pedB");
        this.setPezzo(2, 6, "pedB");
        this.setPezzo(2, 7, "pedB");
        this.setPezzo(2, 8, "pedB");

        this.setPezzo(7, 1, "pedN");
        this.setPezzo(7, 2, "pedN");
        this.setPezzo(7, 3, "pedN");
        this.setPezzo(7, 4, "pedN");
        this.setPezzo(7, 5, "pedN");
        this.setPezzo(7, 6, "pedN");
        this.setPezzo(7, 7, "pedN");
        this.setPezzo(7, 8, "pedN");

        this.setPezzo(1, 1, "torB");
        this.setPezzo(1, 8, "torB");
        this.setPezzo(1, 2, "cavB");
        this.setPezzo(1, 7, "cavB");
        this.setPezzo(1, 3, "alfB");
        this.setPezzo(1, 6, "alfB");
        this.setPezzo(1, 4, "donB");
        this.setPezzo(1, 5, "re_B");

        this.setPezzo(8, 1, "torN");
        this.setPezzo(8, 8, "torN");
        this.setPezzo(8, 2, "cavN");
        this.setPezzo(8, 7, "cavN");
        this.setPezzo(8, 3, "alfN");
        this.setPezzo(8, 6, "alfN");
        this.setPezzo(8, 4, "donN");
        this.setPezzo(8, 5, "re_N");

        this.toccaAlBianco = true;
        updateLegalMoves(); // too slow on startup !!!
        history = new ArrayList<>();
    }

    public void setPezzo(int y, int x, String pezzo) {
        if (!isInsideChessBoard(y,x)) {
            return;
        }
        this.matrix[y - 1][x - 1] = pezzo;
    }

    public void setPezzo(char c, int x, String pezzo) {
        int y = c - 'a' + 1;
        setPezzo(y,x,pezzo);
    }

    public String getPezzo(int y, int x) {
        if (!isInsideChessBoard(y,x)) {
            return "outOfBound";
        }
        return matrix[y - 1][x - 1];
    }

    /**
     * Funzione non interessante, solo di debug per printare la matrice
     * Dovrà essere riscritta prima o poi utilizzando i metodi di alto
     * livello "geTipoPezzo","getColorePezzo".
     */
    public void print() {
        System.out.print("  a||1 ");
        System.out.print("b||2 ");
        System.out.print("c||3 ");
        System.out.print("d||4 ");
        System.out.print("e||5 ");
        System.out.print("f||6 ");
        System.out.print("g||7 ");
        System.out.print("h||8 ");
        System.out.println();
        for (int y = 7; y >= 0; y--) {
            for (int x = 0; x < 8; x++) {
                if (x == 0) {
                    y = y + 1;
                    System.out.print(y + " ");
                    y = y - 1;
                }
                if (matrix[y][x] != null) {
                    System.out.print(matrix[y][x]);
                } else {
                    System.out.print("|__|");
                }
                if (x == 7) {
                    y = y + 1;
                    System.out.print(" " + y);
                    y = y - 1;
                }
                System.out.print(" ");
            }
            System.out.println();
        }
        System.out.print("  a||1 ");
        System.out.print("b||2 ");
        System.out.print("c||3 ");
        System.out.print("d||4 ");
        System.out.print("e||5 ");
        System.out.print("f||6 ");
        System.out.print("g||7 ");
        System.out.print("h||8 ");
        System.out.println();
        System.out.println();
        System.out.println();
    }

    public boolean move(int y0, int x0, int y, int x) {
        Movement toCheck = new Movement();
        toCheck.set(y0,x0,y,x);
        if (getLegalMoves().contains(toCheck)) {
            String[][] backup = copy(matrix);
            history.add(backup);
            forceMove(y0, x0, y, x);
            if (!lockTurn) {
                toccaAlBianco = !toccaAlBianco;
            }
            updateLegalMoves();
            //print();
            return true;
        }
        else return false;
    }

    public void forceMove(int y0, int x0, int y, int x) {
        // hack promozione del pedone
        if(y0 == 7 && y == 8 && getTipoPezzo(y0,x0) == 'p'){
            this.setPezzo(y0,x0,promotionB);
        }
        if(y0 == 2 && y == 1 && getTipoPezzo(y0,x0) == 'p'){
            this.setPezzo(y0,x0,promotionN);
        }

        // hack arrocco
        if(y0 == 1 && x0 == 5 && getColorePezzo(y0,x0)=='B' && y==1 && x==7){
            if(getPezzo(1,5)=="re_B" && getPezzo(1,6)==null && getPezzo(1,7)==null && getPezzo(1,8)=="torB"){
                setPezzo(1,5,null);
                setPezzo(1,8,null);
                setPezzo(1,6,"to_B");
                setPezzo(1,7,"re_B");
                return;
            }
        }

        if(y0 == 1 && x0 == 5 && getColorePezzo(y0,x0)=='B' && y==1 && x==3){
            if(getPezzo(1,5)=="re_B" && getPezzo(1,4)==null && getPezzo(1,3)==null && getPezzo(1,2)==null && getPezzo(1,1)=="torB"){
                setPezzo(1,5,null);
                setPezzo(1,1,null);
                setPezzo(1,4,"to_B");
                setPezzo(1,3,"re_B");
                return;
            }
        }

        if(y0 == 8 && x0 == 5 && getColorePezzo(y0,x0)=='N' && y==8 && x==7){
            if(getPezzo(8,5)=="re_N" && getPezzo(8,6)==null && getPezzo(8,7)==null && getPezzo(8,8)=="torN"){
                setPezzo(8,5,null);
                setPezzo(8,8,null);
                setPezzo(8,6,"to_N");
                setPezzo(8,7,"re_N");
                return;
            }
        }

        if(y0 == 8 && x0 == 5 && getColorePezzo(y0,x0)=='N' && y==8 && x==3){
            if(getPezzo(8,5)=="re_N" && getPezzo(8,4)==null && getPezzo(8,3)==null && getPezzo(8,2)==null && getPezzo(8,1)=="torN"){
                setPezzo(8,5,null);
                setPezzo(8,1,null);
                setPezzo(8,4,"to_N");
                setPezzo(8,3,"re_N");
                return;
            }
        }

        // meccanismo di segnalazione per l'arrocco
        if(getTipoPezzo(y0,x0) == 't'){
            String torre = getPezzo(y0, x0);
            torre = torre.replace('r', '_');
            setPezzo(y0, x0, torre);
        }
        if(getTipoPezzo(y0,x0) == 'r'){
            String re = getPezzo(y0, x0);
            re = re.replace('e', '_');
            setPezzo(y0, x0, re);
        }

        // hack en passant
        if(getTipoPezzo(y0,x0) == 'p'){
            pawnLogic(y0,x0,y,x);
        }

        if(isInsideChessBoard(y0,x0,y,x)) {
            String temp = this.getPezzo(y0, x0);
            this.setPezzo(y0, x0, null);
            this.setPezzo(y, x, temp);
        }
    }

    public char getTipoPezzo(int y, int x) {
        if (this.getPezzo(y, x) != null) {
            return this.getPezzo(y, x).charAt(0);
        } else return '0';

    }

    public char getColorePezzo(int y, int x) {
        if (getPezzo(y, x) != null) {
            return this.getPezzo(y, x).charAt(3);
        } else return '0';

    }

    public boolean isEmpty(int y, int x) {
        return this.getPezzo(y, x) == null;
    }

    public boolean isBianco(int y, int x) {
        return getColorePezzo(y, x) == 'B';
    }

    public boolean isNero(int y, int x) {
        return getColorePezzo(y, x) == 'N';
    }

    public boolean isNotRe(int y, int x) {
        return getTipoPezzo(y, x) != 'r' && !isEmpty(y, x);
    }

    public boolean isNotNull(int y, int x) {
        return getPezzo(y,x)!=null;
    }

    public boolean thereIsAnEatablePieceByPawn(int y0, int x0, int y, int x) {
        boolean answer = false;
        if (getColorePezzo(y, x) != getColorePezzo(y0, x0) && getColorePezzo(y0, x0) != '0' && getColorePezzo(y, x) != '0') {
            if ((x == x0 - 1 && isInsideChessBoard(x0 - 1)) ||
                    (x == x0 + 1 && isInsideChessBoard(x0 + 1))) {
                if (isBianco(y0, x0)) {
                    if (y == y0 + 1) {
                        answer = true;
                    }
                } else if (isNero(y0, x0)) {
                    if (y == y0 - 1) {
                        answer = true;
                    }
                }
            }
        }
        return answer;
    }

    public boolean legalPawnMove(int y0, int x0, int y, int x) {
        boolean isLegalMove = false;
        //Segnala che è la prima volta che muovo il pezzo (en passant related)
        if (getPezzo(y0, x0).charAt(1) == 'e') {
            String pedone = getPezzo(y0, x0);
            pedone = pedone.replace('e', '1');
            setPezzo(y0, x0, pedone);
        } else {
            String pedone = getPezzo(y0, x0);
            pedone = pedone.replace('1', '_');
            setPezzo(y0, x0, pedone);
        }

        if (isNotNull(y, x)) {
            isLegalMove = thereIsAnEatablePieceByPawn(y0, x0, y, x);
        }

        if (isEmpty(y, x)) {
            if (x0 == x) {
                isLegalMove = true;
                // Segnala che il pedone si è mosso in avanti di uno (En passant related)
                if (y == y0 + 1 || y == y0 - 1) {
                    String pedone = getPezzo(y0, x0);
                    pedone = pedone.replace('d', '_');
                    setPezzo(y0, x0, pedone);
                }
            }
        }
        return isLegalMove;
    }

    public boolean isEnPassantAble(int y, int x) {
        return getTipoPezzo(y, x) == 'p' &&
                getPezzo(y, x).charAt(2) == 'd' &&
                getPezzo(y, x).charAt(1) == '1';
    }

    public boolean isInCheck(){
        Boolean isInCheck = false;
        for(int x=1; x<=8; x++){
            for(int y=1; y<=8; y++){
                if(getTipoPezzo(y,x) == 'r'){
                    if((getColorePezzo(y,x)=='B' && toccaAlBianco()) || (getColorePezzo(y,x)=='N' && toccaAlNero())){
                        String[][] backupMatrix =copy(matrix);
                        setPezzo(y,x,null);
                        // si passi alla vista dell'altro giocatore
                        jumpTurn();
                        ArrayList<Movement> positions = pseudoLegalMoves();
                        jumpTurn();

                        for (Movement mov : positions){
                            if (mov.getX() == x && mov.getY() == y){
                                // fix pedina che andando avanti crede di poter mettere in scacco il re
                                if(!(getTipoPezzo(mov.getY0(),mov.getX0()) == 'p' && x==mov.getX() && mov.getX()==mov.getX0())) {
                                    isInCheck = true;
                                }
                                break;
                            }
                        }
                        matrix=copy(backupMatrix);
                        jumpTurn();
                        if(isBianco(y,x)){
                            setPezzo(y,x,"p__B");
                        }
                        else{
                            setPezzo(y,x,"p__N");
                        }
                        positions = pseudoLegalMoves();
                        jumpTurn();

                        for (Movement mov : positions){
                            if (mov.getX() == x && mov.getY() == y){
                                // fix pedina che andando avanti crede di poter mettere in scacco il re
                                isInCheck = true;
                                break;
                            }
                        }
                        //restore
                        matrix=copy(backupMatrix);
                    }
                }
            }
        }
        return isInCheck;
    }


    public boolean pseudoLegalMove(int y0, int x0, int y, int x) {
        if (    isEmpty(y0, x0) || // se la casella di partenza è vuota
                !isInsideChessBoard(y0,x0,y,x) || //se i numeri inseriti non sono validi
                ((y0 == y) && (x0 == x)) ||  // se la casella di partenza è uguale a quella di destinazione
                (toccaAlBianco() && isNero(y0, x0)) || // se vuoi muovere un pezzo di colore diverso
                (toccaAlNero() && isBianco(y0, x0))  || // rispetto al colore del turno corrente
                (getColorePezzo(y,x) == getColorePezzo(y0,x0)) || // se vuoi muovere un pezzo su un altro pezzo dello stesso colore
                // condizioni per scartare subito un mucchio di caselle
                (getTipoPezzo(y0,x0) == 'p' && !((x == x0 || x == x0+1 || x == x0-1 ) && (y == y0 + 1 || y == y0 -1 || (y0 == 2 && y ==4) || (y0 == 7 && y==5)))) ||
                (getTipoPezzo(y0,x0) == 'r' && !((x<=x0+1 && y<=y0+1 && x>=x0-1 && y>=y0-1) ||
                        //fix arrocco
                        (y0==1 && y==1 && x0==5 && x==7) ||
                        (y0==1 && y==1 && x0==5 && x==3) ||
                        (y0==8 && y==8 && x0==5 && x==7) ||
                        (y0==8 && y==8 && x0==5 && x==3))) ||
                (getTipoPezzo(y0,x0) == 't' && !(x == x0 || y == y0) ||
                        (getTipoPezzo(y0,x0) == 'a' && !((x0 + y0) % 2 == (x + y) % 2)) ||
                        (getTipoPezzo(y0,x0) == 'a' && (y == y0 || x == x0)) ||
                        (getTipoPezzo(y0,x0) == 'c' && (x > x0 + 3 || x < x0 - 3 || y > y0 + 3 || y < y0 - 3)) )
        ) {
            return false;
        }

        boolean isLegalMove = false;
        switch (getTipoPezzo(y0, x0)) {
            case 'p': // pedina
                String[][] backupMatrix = copy(matrix);
                isLegalMove = pawnLogic(y0,x0,y,x);
                matrix = copy(backupMatrix);
                break;
            case 't':
                int distance, minimum;
                boolean thereIsAPieceInTheMiddle = false;
                if (y0 == y) {
                    distance = Math.max(x0, x) - Math.min(x0, x);
                    // se c'è un pezzo in mezzo la mossa è illegale
                    minimum = Math.min(x0, x);
                    for (int i = 1; i < distance; i++) {
                        if (!isEmpty(y, minimum + i)) {
                            thereIsAPieceInTheMiddle = true;
                            break;
                        }
                    }
                    isLegalMove = !thereIsAPieceInTheMiddle;
                }
                if (x0 == x) {
                    distance = Math.max(y0, y) - Math.min(y0, y);//2
                    // se c'è un pezzo in mezzo la mossa è illegale
                    minimum = Math.min(y0, y);//5
                    for (int i = 1; i < distance; i++) {
                        if (!isEmpty(minimum + i, x)) {
                            thereIsAPieceInTheMiddle = true;
                            break;
                        }
                    }
                    isLegalMove = !thereIsAPieceInTheMiddle;
                }
                break;
            case 'a':
                int m = 999;
                boolean pieceInTheMiddle = false;
                // Quando l'alfiere si muove non conserva ne' l'ascissa ne' l'ordinata
                if (x != x0 && y!=y0){
                    if (y-y0 == x - x0){
                        m = 1;
                    }
                    else if(y-y0 == x0 - x){
                        m = -1;
                    }
                }

                if ((m == 1 || m == -1) && // i due pezzi sono allineati in diagonale
                        ((isNotRe(y, x) && getColorePezzo(y, x) != getColorePezzo(y0, x0)) ||
                                isEmpty(y, x))) {
                    //System.out.println("Slope: " + m);
                    int dist = (int) Math.ceil(Math.abs(Math.sqrt(Math.pow(x0 - x, 2) + Math.pow(y0 - y, 2))) / Math.sqrt(2));
                    //System.out.println("Distance between the 2 points = " + dist);
                    int min = Math.min(y0, y);
                    int x_del_min;
                    if (min == y0) {
                        x_del_min = x0;
                    } else {
                        x_del_min = x;
                    }
                    int xx = x_del_min;
                    int yy = min;
                    if (m == 1) {
                        for (int i = 1; i < dist; i++) {
                            xx = xx + 1;
                            yy = yy + 1;
                            //System.out.println("Checking emptiness of (" + xx + "," + yy+")");
                            if (!isEmpty(yy, xx)) {
                                pieceInTheMiddle = true;
                                break;
                                //System.out.println("not empty");
                            } else {
                                //System.out.println("empty");
                            }
                        }
                        isLegalMove = !pieceInTheMiddle;

                    }
                    if (m == -1) {
                        for (int i = 1; i < dist; i++) {
                            xx = xx - 1;
                            yy = yy + 1;
                            //System.out.println("Checking emptiness of (" + xx + "," + yy+")");
                            if (!isEmpty(yy, xx)) {
                                pieceInTheMiddle = true;
                                //System.out.println("not empty");
                            } else {
                                //System.out.println("empty");
                            }
                        }
                        isLegalMove = !pieceInTheMiddle;
                    }

                }
                break;
            case 'd':
                String donna = getPezzo(y0,x0);
                String backup = donna;
                // la mossa è fattibile da una torre?
                donna = donna.replace('d', 't');
                setPezzo(y0, x0, donna);
                if (pseudoLegalMove(y0,x0,y,x)){
                    setPezzo(y0,x0,backup);
                    return true;
                }
                // la mossa è fattibile da un'alfiere?
                donna = donna.replace('t', 'a');
                setPezzo(y0, x0, donna);
                if (pseudoLegalMove(y0,x0,y,x)){
                    setPezzo(y0,x0,backup);
                    return true;
                }
                setPezzo(y0,x0,backup);
                break;
            case 'c':
                if(     (y == y0 + 2 && x == x0 + 1) ||
                        (y == y0 + 1 && x == x0 + 2) ||
                        (y == y0 - 2 && x == x0 - 1) ||
                        (y == y0 - 1 && x == x0 - 2) ||
                        (y == y0 - 2 && x == x0 + 1) ||
                        (y == y0 - 1 && x == x0 + 2) ||
                        (y == y0 + 2 && x == x0 - 1) ||
                        (y == y0 + 1 && x == x0 - 2) ){
                    isLegalMove = true;
                }
                break;
            case 'r':
                if (getTipoPezzo(y,x)=='r'){
                    return false;
                }

                // Occorre verificare se il re si vuole muovere "nelle vicinanze"
                if(x<=x0+1 && y<=y0+1 && x>=x0-1 && y>=y0-1){
                    isLegalMove = true;
                    break;
                }
                if(y0 == 1 && x0 == 5 && getColorePezzo(y0,x0)=='B' && y==1 && x==7){
                    if(getPezzo(1,5)=="re_B" && getPezzo(1,6)==null && getPezzo(1,7)==null && getPezzo(1,8)=="torB"){
                        isLegalMove = true;
                        break;
                    }
                }
                if(y0 == 1 && x0 == 5 && getColorePezzo(y0,x0)=='B' && y==1 && x==3){
                    if(getPezzo(1,5)=="re_B" && getPezzo(1,4)==null && getPezzo(1,3)==null && getPezzo(1,2)==null && getPezzo(1,1)=="torB"){
                        isLegalMove = true;
                        break;
                    }
                }
                if(y0 == 8 && x0 == 5 && getColorePezzo(y0,x0)=='N' && y==8 && x==7){
                    if(getPezzo(8,5)=="re_N" && getPezzo(8,6)==null && getPezzo(8,7)==null && getPezzo(8,8)=="torN"){
                        isLegalMove = true;
                        break;
                    }
                }
                if(y0 == 8 && x0 == 5 && getColorePezzo(y0,x0)=='N' && y==8 && x==3){
                    if(getPezzo(8,5)=="re_N" && getPezzo(8,4)==null && getPezzo(8,3)==null && getPezzo(8,2)==null && getPezzo(8,1)=="torN"){
                        isLegalMove = true;
                        break;
                    }
                }
                break;
            default:
                isLegalMove = false;
        }
        return isLegalMove;
    }

    public ArrayList<Movement> legalMoves(){
        ArrayList<Movement> pseudoLegalMoves = pseudoLegalMoves();
        ArrayList<Movement> legalMoves = new ArrayList<>();
        for(Movement p : pseudoLegalMoves){
            String[][] backupMatrix = copy(matrix);
            forceMove(p.getY0(),p.getX0(),p.getY(),p.getX());
            if(!isInCheck()){
                legalMoves.add(p);
            }
            matrix = copy(backupMatrix);
        }
        return legalMoves;
    }

    public ArrayList<Movement> pseudoLegalMoves(){
        Movement toTest;
        ArrayList pseudoLegalMoves= new ArrayList<>();

        for (int i=1; i<=8;i++){
            for (int j=1; j<=8;j++){
                if(!isEmpty(i,j) && !((toccaAlBianco() && isNero(i, j)) || (toccaAlNero() && isBianco(i, j))) ) {
                    for (int k = 1; k <= 8; k++) {
                        for (int l = 1; l <= 8; l++) {
                            if (pseudoLegalMove(i, j, k, l)) {
                                toTest = new Movement();
                                toTest.set(i, j, k, l);
                                pseudoLegalMoves.add(toTest);
                            }
                        }
                    }
                }
            }
        }
        return pseudoLegalMoves;
    }

    public void updateLegalMoves(){
        this.pseudoLegalMoves = pseudoLegalMoves();
        this.legalMoves = legalMoves();
    }

    public boolean isInsideChessBoard(int w) {
        return w > 0 && w <= 8;
    }

    public boolean isInsideChessBoard(int w,int e) {
        return w > 0 && w <= 8 &&
                e > 0 && e <= 8;
    }

    public boolean isInsideChessBoard(int w,int e, int r, int t) {
        return w > 0 && w <= 8 &&
                e > 0 && e <= 8 &&
                r > 0 && r <= 8 &&
                t > 0 && t <= 8;
    }


    public boolean toccaAlBianco() {
        return toccaAlBianco;
    }

    public boolean toccaAlNero() {
        return !toccaAlBianco;
    }

    public void lockTurn() {
        lockTurn = !lockTurn;
    }

    public void jumpTurn(){
        toccaAlBianco = !toccaAlBianco;
    }

    public void promotion(String s) {
        if(toccaAlBianco()){
            promotionB=s;
        }
        else {
            promotionN=s;
        }
    }

    public ArrayList<Movement> getLegalMoves() {
        return legalMoves;
    }

    public String[][] copy(String[][] src) {
        if (src == null) {
            return null;
        }
        String[][] copy = new String[src.length][];
        for (int i = 0; i < src.length; i++) {
            copy[i] = Arrays.copyOf(src[i], src[i].length);
        }
        return copy;
    }

    /** Funzione non pura, ha side-effects sulla matrice**/
    private Boolean pawnLogic(int y0,int x0, int y, int x){
        boolean toReturn = false;
        // En passant fix
        if (isEmpty(y, x)) {
            if (isBianco(y + 1, x) && y + 1 == 4 && y0 == 4 &&
                    getTipoPezzo(y + 1, x) == 'p' &&
                    isEnPassantAble(y + 1, x) &&
                    getColorePezzo(y + 1, x) != getColorePezzo(y0, x0)
            ) {
                forceMove(y + 1, x, y, x);
            }
            if (isNero(y - 1, x) && y - 1 == 5 && y0 == 5 &&
                    getTipoPezzo(y - 1, x) == 'p' &&
                    isEnPassantAble(y - 1, x) &&
                    getColorePezzo(y - 1, x) != getColorePezzo(y0, x0)
            ) {
                forceMove(y - 1, x, y, x);
            }
        }


        // Standard pawn logic
        if (isBianco(y0, x0)) {
            if ((y0 == 2 && y == 4) || (y == y0 + 1 && isInsideChessBoard(y0 + 1))) {
                toReturn = legalPawnMove(y0, x0, y, x);
            }
        } else if (isNero(y0, x0)) {
            if ((y0 == 7 && y == 5) || (y == y0 - 1 && isInsideChessBoard(y0 - 1))) {
                toReturn = legalPawnMove(y0, x0, y, x);
            }
        }
        return toReturn;
    }

    public void undo(){
        if(!history.isEmpty()) {
            lastUndo = copy(matrix);
            matrix = copy(history.get(history.size() - 1));
            history.remove(history.size()-1);
            toccaAlBianco = !toccaAlBianco;
            updateLegalMoves();
        }
    }

    public void redo(){
        if (lastUndo!=null){
            history.add(history.size(),copy(matrix));
            matrix=copy(lastUndo);
            toccaAlBianco=!toccaAlBianco;
            lastUndo=null;
            updateLegalMoves();
        }
    }
}