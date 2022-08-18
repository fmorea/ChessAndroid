package com.fmorea.chess

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import android.widget.Toast

const val TAG = "MainActivity"

class MainActivity : AppCompatActivity(), ChessDelegate {

    var chessModel = ChessModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<ChessView>(R.id.chess_view).chessDelegate = this
        switch1?.setOnCheckedChangeListener { _, isChecked ->
            chessModel.switchOn = isChecked
            findViewById<ChessView>(R.id.chess_view).invalidate()
        }
        textView2.setOnClickListener {
            val toast = Toast
                .makeText(applicationContext,
                    chessModel.gameLogic.legalMoves().toString(),
                    Toast.LENGTH_LONG).show()
        }

    }

    override fun pieceAt(col: Int, row: Int): ChessPiece? {
        return chessModel.pieceAt(col, row)
    }

    override fun movePiece(fromCol: Int, fromRow: Int, toCol: Int, toRow: Int) {
        var hasMoved = chessModel.movePiece(fromCol, fromRow, toCol, toRow)
        if(chessModel.gameLogic.toccaAlBianco()){
            textView.text = "White moves"
        }
        else{
            textView.text = "Black moves"
        }
        if (!hasMoved){
            textView2.text="Please scroll the piece on the correct square (orange)"
        }
        else{
            textView2.text=""
        }
        if(chessModel.gameLogic.legalMoves.isEmpty()) {
            if (chessModel.gameLogic.toccaAlBianco()) {
                textView2.text = "Ha vinto il nero"
            } else {
                textView2.text = "Ha vinto il bianco"
            }
        }
        findViewById<ChessView>(R.id.chess_view).invalidate()
    }

    override fun SwitchOn(): Boolean? {
        return chessModel.switchOn
    }

    override fun getLegalMoves() : ArrayList<Movement>{
        return chessModel.gameLogic.legalMoves
    }
}