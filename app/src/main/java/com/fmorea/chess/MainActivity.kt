package com.fmorea.chess

import android.os.Bundle
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*


const val TAG = "MainActivity"

class MainActivity : AppCompatActivity(), ChessDelegate {

    var chessModel = ChessModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<ChessView>(R.id.chess_view).chessDelegate = this
        chessModel.chessDelegate=this

        switch1?.setOnCheckedChangeListener { _, isChecked ->
            chessModel.showReachableSquares = isChecked
            findViewById<ChessView>(R.id.chess_view).invalidate()
        }
        switch2?.setOnCheckedChangeListener { _, isChecked ->
            chessModel.blackPointOfView = !chessModel.blackPointOfView
            findViewById<ChessView>(R.id.chess_view).invalidate()
        }
        switch3?.setOnCheckedChangeListener { _, isChecked ->
            chessModel.autoRotate = !chessModel.autoRotate
            findViewById<ChessView>(R.id.chess_view).invalidate()
        }
        button.setOnClickListener {
            for (i in 1..8) {
                for (j in 1..8) {
                    chessModel.gameLogic.setPezzo(i, j, null)
                }
            }
            chessModel.gameLogic.createStandardChessboard();
            textView3.text="MOVE LOG";
            findViewById<ChessView>(R.id.chess_view).invalidate()
            val toast = Toast
                .makeText(applicationContext,
                    "Game Restared",
                    Toast.LENGTH_SHORT).show()
        }

    }

    override fun pieceAt(col: Int, row: Int): ChessPiece? {
        return chessModel.pieceAt(col, row)
    }

    override fun movePiece(fromCol: Int, fromRow: Int, toCol: Int, toRow: Int) : Boolean?{
        var hasMoved = chessModel.movePiece(fromCol, fromRow, toCol, toRow)
        if(chessModel.gameLogic.toccaAlBianco()){
            textView.text = "White moves"
        }
        else{
            textView.text = "Black moves"
        }
        if (!hasMoved){
            textView2.text="Invalid Move"
        }
        else{
            textView2.text="Move a piece"
            if(chessModel.gameLogic.toccaAlBianco()){
                textView3.append("\nBLACK: ");
            }
            else{
                textView3.append("\nWHITE: ");
            }
            textView3.append(getLetter(fromCol) + fromRow + " " + getLetter(toCol)+toRow);
            scrollView.fullScroll(ScrollView.FOCUS_DOWN)
        }
        if(chessModel.gameLogic.legalMoves.isEmpty()) {
            if(chessModel.gameLogic.isInCheck) {
                if (chessModel.gameLogic.toccaAlBianco()) {
                    textView2.text = "Black Won"
                } else {
                    textView2.text = "White Won"
                }
            }
            else{
                textView2.text = "Stalemate"
            }
        }
        return hasMoved
    }

    override fun showAllReachableSquares(): Boolean? {
        return chessModel.showReachableSquares
    }

    override fun blackPointOfView(): Boolean? {
        return chessModel.blackPointOfView
    }

    override fun autoRotate(): Boolean? {
        return chessModel.autoRotate
    }

    override fun setOrientation(orientation: Boolean) {
        chessModel.blackPointOfView=orientation
    }

    override fun getLegalMoves() : ArrayList<Movement>{
        return chessModel.gameLogic.legalMoves
    }

    private fun getLetter(i: Int): String? {
        if (i==1) return "a"
        if (i==2) return "b"
        if (i==3) return "c"
        if (i==4) return "d"
        if (i==5) return "e"
        if (i==6) return "f"
        if (i==7) return "g"
        if (i==8) return "h"
        return ""
    }
}