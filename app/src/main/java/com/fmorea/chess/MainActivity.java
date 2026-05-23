package com.fmorea.chess;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.fmorea.chess.databinding.ActivityMainBinding;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements ChessDelegate {

    private final ChessModel chessModel = new ChessModel();
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.chessView.setChessDelegate(this);
        chessModel.setChessDelegate(this);

        binding.switch1.setOnCheckedChangeListener((buttonView, isChecked) -> {
            chessModel.setShowReachableSquares(isChecked);
            binding.chessView.invalidate();
        });
        binding.switch2.setOnCheckedChangeListener((buttonView, isChecked) -> {
            chessModel.setBlackPointOfView(!chessModel.isBlackPointOfView());
            binding.chessView.invalidate();
        });
        binding.switch3.setOnCheckedChangeListener((buttonView, isChecked) -> {
            chessModel.setAutoRotate(!chessModel.isAutoRotate());
            binding.chessView.invalidate();
        });
        binding.button.setOnClickListener(v -> {
            for (int i = 1; i <= 8; i++) {
                for (int j = 1; j <= 8; j++) {
                    chessModel.getGameLogic().setPezzo(i, j, null);
                }
            }
            chessModel.getGameLogic().createStandardChessboard();
            binding.textView3.setText("");
            binding.chessView.invalidate();
            binding.textView2.setText("Game Restarted");
        });

        binding.bottomAppBar.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_settings) {
                Intent intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.action_privacy_policy) {
                Intent intent = new Intent(this, PrivacyPolicy.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.action_undo || itemId == R.id.back_button) {
                chessModel.getGameLogic().undo();
                binding.chessView.invalidate();
                binding.chessView.setMovingPiece(null);
                updateTextViews(true);
                movePiece(0, 0, 0, 0); // update statistics
                binding.textView2.setText("Undo done");
                return true;
            } else if (itemId == R.id.action_redo) {
                chessModel.getGameLogic().redo();
                binding.chessView.invalidate();
                binding.chessView.setMovingPiece(null);
                movePiece(0, 0, 0, 0); // update statistics
                binding.textView2.setText("Redo done");
                updateTextViews(true);
                return true;
            }
            return false;
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.bottomappbar_menu, menu);
        return true;
    }

    @Override
    public ChessPiece pieceAt(int col, int row) {
        return chessModel.pieceAt(col, row);
    }

    @Override
    public Boolean movePiece(int fromCol, int fromRow, int toCol, int toRow) {
        boolean hasMoved = chessModel.movePiece(fromCol, fromRow, toCol, toRow);
        if (hasMoved) {
            String temp = "Material Value: " + chessModel.getGameLogic().objectiveFunction();
            if (chessModel.getGameLogic().isInCheck()) {
                temp += "\nKing is in check !!";
            }
            if (chessModel.getGameLogic().toccaAlBianco()) {
                temp += "\nWhite turn";
            } else {
                temp += "\nBlack turn";
            }

            binding.textView3.setText(temp);
            binding.scrollView.fullScroll(ScrollView.FOCUS_DOWN);
        }

        updateTextViews(hasMoved);
        return hasMoved;
    }

    private void updateTextViews(boolean hasMoved) {
        if (!hasMoved) {
            binding.textView2.setText("Invalid Move !!!");
        } else {
            binding.textView2.setText("[" + getLetter(chessModel.getGameLogic().getMov().getX0()) + chessModel.getGameLogic().getMov().getY0() + "]" + "->" + "[" + getLetter(chessModel.getGameLogic().getMov().getX()) + chessModel.getGameLogic().getMov().getY() + "]");
        }

        if (chessModel.getGameLogic().getLegalMoves().isEmpty()) {
            if (chessModel.getGameLogic().isInCheck()) {
                if (chessModel.getGameLogic().toccaAlBianco()) {
                    binding.textView2.setText("Black Won");
                } else {
                    binding.textView2.setText("White Won");
                }
            } else {
                binding.textView2.setText("Stalemate");
            }
        }
    }

    @Override
    public Boolean showAllReachableSquares() {
        return chessModel.isShowReachableSquares();
    }

    @Override
    public Boolean blackPointOfView() {
        return chessModel.isBlackPointOfView();
    }

    @Override
    public Boolean autoRotate() {
        return chessModel.isAutoRotate();
    }

    @Override
    public void setOrientation(boolean orientation) {
        chessModel.setBlackPointOfView(orientation);
    }

    @Override
    public ArrayList<Movement> getLegalMoves() {
        return chessModel.getGameLogic().getLegalMoves();
    }

    private String getLetter(int i) {
        return switch (i) {
            case 1 -> "a";
            case 2 -> "b";
            case 3 -> "c";
            case 4 -> "d";
            case 5 -> "e";
            case 6 -> "f";
            case 7 -> "g";
            case 8 -> "h";
            default -> "";
        };
    }
}
