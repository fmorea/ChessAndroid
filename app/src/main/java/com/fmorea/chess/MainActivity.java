package com.fmorea.chess;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.fmorea.chess.databinding.ActivityMainBinding;
import java.util.Locale;

/**
 * MainActivity handles the UI layer of the Chess application.
 * Following Unix philosophy: it focuses on user interaction and display.
 * Networking roles are managed automatically by NetworkAutoManager.
 */
public class MainActivity extends AppCompatActivity implements ChessGameController.GameUI {

    private ActivityMainBinding binding;
    private final ChessModel model = new ChessModel();
    private NetworkHandler transport;
    private NetworkDiscovery discovery;
    private ChessGameController controller;
    private NetworkAutoManager autoManager;

    private static NetworkHandler sharedTransport;
    public static NetworkHandler getTransport() { return sharedTransport; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        transport = new NetworkHandler(50000);
        sharedTransport = transport;
        discovery = new NetworkDiscovery(50001);
        controller = new ChessGameController(model, transport, this);
        
        autoManager = new NetworkAutoManager(discovery, transport, controller);

        binding.chessView.setChessDelegate(controller);
        model.setChessDelegate(controller);

        setupUI();
        autoManager.start();
    }

    private void setupUI() {
        // Switch 1 was for hints, now enabled by default.
        binding.switch1.setVisibility(View.GONE);

        binding.switch2.setOnCheckedChangeListener((v, c) -> { if (c != model.isBlackPointOfView()) model.setBlackPointOfView(c); binding.chessView.invalidate(); });
        binding.switch3.setOnCheckedChangeListener((v, c) -> { model.setAutoRotate(c); binding.chessView.invalidate(); });
        
        binding.button.setOnClickListener(v -> controller.resetGame());
        
        binding.btnPen.setOnClickListener(v -> {
            boolean active = !v.isSelected();
            v.setSelected(active);
            binding.btnEraser.setSelected(false);
            binding.btnEraser.setBackgroundColor(0x00000000);
            binding.chessView.setPenMode(active);
            v.setBackgroundColor(active ? 0x33FF0000 : 0x00000000);
        });

        binding.btnEraser.setOnClickListener(v -> {
            boolean active = !v.isSelected();
            v.setSelected(active);
            binding.btnPen.setSelected(false);
            binding.btnPen.setBackgroundColor(0x00000000);
            binding.chessView.setEraserMode(active);
            v.setBackgroundColor(active ? 0x330000FF : 0x00000000);
        });

        binding.bottomAppBar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_chat) startActivity(new Intent(this, ChatActivity.class));
            else if (id == R.id.action_privacy_policy) startActivity(new Intent(this, PrivacyPolicy.class));
            else if (id == R.id.action_undo) controller.undo();
            else if (id == R.id.action_redo) controller.redo();
            else return false;
            return true;
        });
    }

    @Override public void refreshBoard() { 
        runOnUiThread(() -> {
            binding.chessView.invalidate();
            binding.switch2.setChecked(model.isBlackPointOfView());
        });
    }

    @Override
    public void updateStatus(int material, boolean inCheck, boolean whiteTurn, Movement lastMove) {
        runOnUiThread(() -> {
            String status = String.format(Locale.getDefault(), getString(R.string.material_val), material);
            if (inCheck) status += getString(R.string.check_label);
            status += (whiteTurn ? getString(R.string.white_turn) : getString(R.string.black_turn));
            binding.textView3.setText(status);

            if (lastMove != null && lastMove.getX0() != 0) {
                binding.textView2.setText(String.format(Locale.getDefault(), "[%s%d] -> [%s%d]", 
                    getLetter(lastMove.getX0()), lastMove.getY0(), getLetter(lastMove.getX()), lastMove.getY()));
            } else {
                binding.textView2.setText(R.string.new_game);
            }
        });
    }

    @Override
    public void onConnectionStateChanged(boolean connected) { }

    @Override
    public void updateNetworkInfo(String role, String status) {
        runOnUiThread(() -> {
            binding.textViewNetworkRole.setText(getString(R.string.net_role_prefix, role));
            binding.textViewNetworkStatus.setText(getString(R.string.net_status_prefix, status));
        });
    }

    @Override public void onMessage(String msg) { 
        runOnUiThread(() -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());
    }

    private String getLetter(int i) { return (i < 1 || i > 8) ? "" : String.valueOf((char)('a' + i - 1)); }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        autoManager.stop();
    }
}
