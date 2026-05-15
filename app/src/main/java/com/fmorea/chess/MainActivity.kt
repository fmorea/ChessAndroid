package com.fmorea.chess

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import com.fmorea.chess.databinding.ActivityMainBinding

const val TAG = "MainActivity"

class MainActivity : AppCompatActivity(), ChessDelegate {

    var chessModel = ChessModel()
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.chessView.chessDelegate = this
        chessModel.chessDelegate = this

        binding.switch1.setOnCheckedChangeListener { _, isChecked ->
            chessModel.showReachableSquares = isChecked
            binding.chessView.invalidate()
        }
        binding.switch2.setOnCheckedChangeListener { _, isChecked ->
            chessModel.blackPointOfView = !chessModel.blackPointOfView
            binding.chessView.invalidate()
        }
        binding.switch3.setOnCheckedChangeListener { _, isChecked ->
            chessModel.autoRotate = !chessModel.autoRotate
            binding.chessView.invalidate()
        }
        binding.button.setOnClickListener {
            for (i in 1..8) {
                for (j in 1..8) {
                    chessModel.gameLogic.setPezzo(i, j, null)
                }
            }
            chessModel.gameLogic.createStandardChessboard()
            binding.textView3.text = ""
            binding.chessView.invalidate()
            binding.textView2.text = "Game Restarted"
        }

        binding.bottomAppBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_settings -> {
                    val intent = Intent(this, AboutActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.action_privacy_policy -> {
                    val intent = Intent(this, PrivacyPolicy::class.java)
                    startActivity(intent)
                    true
                }
                R.id.action_undo -> {
                    chessModel.gameLogic.undo()
                    binding.chessView.invalidate()
                    binding.chessView.movingPiece = null
                    updateTextViews(true)
                    movePiece(0, 0, 0, 0) //update statistics
                    binding.textView2.text = "Undo done"
                    true
                }
                R.id.back_button -> {
                    chessModel.gameLogic.undo()
                    binding.chessView.invalidate()
                    binding.chessView.movingPiece = null
                    updateTextViews(true)
                    movePiece(0, 0, 0, 0) //update statistics
                    binding.textView2.text = "Undo done"
                    true
                }
                R.id.action_redo -> {
                    chessModel.gameLogic.redo()
                    binding.chessView.invalidate()
                    binding.chessView.movingPiece = null
                    movePiece(0, 0, 0, 0) //update statistics
                    binding.textView2.text = "Redo done"
                    updateTextViews(true)
                    true
                }
                else -> false
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.bottomappbar_menu, menu)
        return true
    }

    override fun pieceAt(col: Int, row: Int): ChessPiece? {
        return chessModel.pieceAt(col, row)
    }

    override fun movePiece(fromCol: Int, fromRow: Int, toCol: Int, toRow: Int): Boolean? {
        var hasMoved = chessModel.movePiece(fromCol, fromRow, toCol, toRow)
        if (hasMoved) {
            var temp = "Material Value: " + chessModel.gameLogic.objectiveFunction().toString()
            if (chessModel.gameLogic.isInCheck) {
                temp = temp + "\nKing is in check !!"
            }
            if (chessModel.gameLogic.toccaAlBianco()) {
                temp = temp + "\nWhite turn"
            } else {
                temp = temp + "\nBlack turn"
            }

            binding.textView3.text = temp
            binding.scrollView.fullScroll(ScrollView.FOCUS_DOWN)
        }

        updateTextViews(hasMoved)
        return hasMoved
    }

    private fun updateTextViews(hasMoved: Boolean) {
        if (!hasMoved) {
            binding.textView2.text = "Invalid Move !!!"
        } else {
            binding.textView2.text = "[" + getLetter(chessModel.gameLogic.mov.x0) + chessModel.gameLogic.mov.y0 + "]" + "->" + "[" + getLetter(chessModel.gameLogic.mov.x) + chessModel.gameLogic.mov.y + "]"
        }

        if (chessModel.gameLogic.legalMoves.isEmpty()) {
            if (chessModel.gameLogic.isInCheck) {
                if (chessModel.gameLogic.toccaAlBianco()) {
                    binding.textView2.text = "Black Won"
                } else {
                    binding.textView2.text = "White Won"
                }
            } else {
                binding.textView2.text = "Stalemate"
            }
        }
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
        chessModel.blackPointOfView = orientation
    }

    override fun getLegalMoves(): ArrayList<Movement> {
        return chessModel.gameLogic.legalMoves
    }

    private fun getLetter(i: Int): String? {
        if (i == 1) return "a"
        if (i == 2) return "b"
        if (i == 3) return "c"
        if (i == 4) return "d"
        if (i == 5) return "e"
        if (i == 6) return "f"
        if (i == 7) return "g"
        if (i == 8) return "h"
        return ""
    }
}
