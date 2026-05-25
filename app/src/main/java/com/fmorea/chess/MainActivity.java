package com.fmorea.chess;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.fmorea.chess.databinding.ActivityMainBinding;
import java.util.Locale;

/**
 * MainActivity handles the UI layer of the Chess application.
 * Following Unix philosophy: it focuses on user interaction and display.
 */
public class MainActivity extends AppCompatActivity implements ChessGameController.GameUI {

    private ActivityMainBinding binding;
    private final ChessModel model = new ChessModel();
    private NetworkHandler transport;
    private NetworkDiscovery discovery;
    private ChessGameController controller;
    private NetworkAutoManager autoManager;
    private final Handler handler = new Handler(Looper.getMainLooper());

    // Loopback for testing both client and server on the same device
    private NetworkHandler loopbackTransport;
    private ChessGameController loopbackController;
    private final ChessModel loopbackModel = new ChessModel();

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

        // Setup loopback components (acts as the "remote" peer on localhost)
        loopbackTransport = new NetworkHandler(50000);
        loopbackController = new ChessGameController(loopbackModel, loopbackTransport, new ChessGameController.GameUI() {
            @Override public void refreshBoard() {}
            @Override public void updateStatus(int m, boolean ic, boolean wt, Movement lm, int lmc) {}
            @Override public void onConnectionStateChanged(boolean c) {}
            @Override public void updateNetworkInfo(String r, String s) {}
            @Override public void onMessage(String msg) {}
            @Override public void showPromotionDialog(int fC, int fR, int tC, int tR, boolean isWhite) {}
        });
        loopbackController.setServer(true);

        binding.chessView.setChessDelegate(controller);
        model.setChessDelegate(controller);

        setupUI();
        autoManager.start();
        
        controller.notifyUI();
    }

    private void setupUI() {
        binding.switch2.setOnCheckedChangeListener((v, c) -> { if (c != model.isBlackPointOfView()) model.setBlackPointOfView(c); binding.chessView.invalidate(); });
        binding.switch3.setOnCheckedChangeListener((v, c) -> { model.setAutoRotate(c); binding.chessView.invalidate(); });
        
        binding.switchLoopback.setOnCheckedChangeListener((v, isChecked) -> {
            if (isChecked) {
                autoManager.stop();
                transport.disconnect();
                loopbackTransport.disconnect();
                
                // Start a server on localhost and connect to it as a client
                handler.postDelayed(() -> {
                    loopbackTransport.startServer();
                    handler.postDelayed(() -> transport.connect("127.0.0.1"), 500);
                }, 500);
                
                Toast.makeText(this, "Loopback Mode: ON", Toast.LENGTH_SHORT).show();
            } else {
                loopbackTransport.disconnect();
                transport.disconnect();
                autoManager.start();
                Toast.makeText(this, "Loopback Mode: OFF", Toast.LENGTH_SHORT).show();
            }
        });

        binding.button.setOnClickListener(v -> controller.resetGame());

        // Bottom Menu Observers
        binding.btnZoomOut.setOnClickListener(v -> binding.chessView.zoomOut());
        binding.btnZoomIn.setOnClickListener(v -> binding.chessView.zoomIn());
        
        binding.btnPenTool.setOnClickListener(v -> {
            boolean active = !binding.chessView.isPenMode();
            binding.chessView.setPenMode(active);
            v.setBackgroundColor(active ? 0x33FF0000 : 0x00000000);
            binding.btnEraserTool.setBackgroundColor(0x00000000);
            Toast.makeText(this, active ? "Pen Mode: ON" : "Pen Mode: OFF", Toast.LENGTH_SHORT).show();
        });

        binding.btnEraserTool.setOnClickListener(v -> {
            boolean active = !binding.chessView.isEraserMode();
            binding.chessView.setEraserMode(active);
            v.setBackgroundColor(active ? 0x330000FF : 0x00000000);
            binding.btnPenTool.setBackgroundColor(0x00000000);
            Toast.makeText(this, active ? "Eraser Mode: ON" : "Eraser Mode: OFF", Toast.LENGTH_SHORT).show();
        });

        binding.btnUndoTool.setOnClickListener(v -> controller.undo());
        binding.btnRedoTool.setOnClickListener(v -> controller.redo());

        // Over-menu items (Chat, Privacy) are still available via standard menu if needed, 
        // but for now we've moved the main tools to direct buttons.
    }

    @Override
    public void showPromotionDialog(int fC, int fR, int tC, int tR, boolean isWhite) {
        runOnUiThread(() -> {
            String suffix = isWhite ? "B" : "N";
            String[] options = {"Regina", "Torre", "Alfiere", "Cavallo"};
            String[] codes = {"don" + suffix, "tor" + suffix, "alf" + suffix, "cav" + suffix};

            new AlertDialog.Builder(this)
                .setTitle("Promozione Pedone")
                .setItems(options, (dialog, which) -> {
                    controller.movePiece(fC, fR, tC, tR, codes[which]);
                })
                .setCancelable(false)
                .show();
        });
    }

    @Override public void refreshBoard() { 
        runOnUiThread(() -> {
            binding.chessView.invalidate();
            binding.switch2.setChecked(model.isBlackPointOfView());
        });
    }

    @Override
    public void updateStatus(int material, boolean inCheck, boolean whiteTurn, Movement lastMove, int legalMovesCount) {
        runOnUiThread(() -> {
            StringBuilder sb = new StringBuilder();
            
            if (legalMovesCount == 0) {
                if (inCheck) {
                    sb.append(whiteTurn ? "SCACCO MATTO! Il Nero ha vinto" : "SCACCO MATTO! Il Bianco ha vinto");
                } else {
                    sb.append("PATTA PER STALLO");
                }
            } else {
                if (material > 400) sb.append("Il Bianco sta vincendo");
                else if (material > 150) sb.append("Bianco in netto vantaggio");
                else if (material > 50) sb.append("Lieve vantaggio Bianco");
                else if (material < -400) sb.append("Il Nero sta vincendo");
                else if (material < -150) sb.append("Nero in netto vantaggio");
                else if (material < -50) sb.append("Lieve vantaggio Nero");
                else sb.append("Partita equilibrata");

                if (inCheck) sb.append(" (SCACCO!)");
                
                sb.append("  •  ");
                sb.append(whiteTurn ? "Tocca al Bianco" : "Tocca al Nero");
            }
            
            binding.textView3.setText(sb.toString());

            if (lastMove != null && lastMove.getX0() != 0) {
                binding.textView2.setText(String.format(Locale.getDefault(), "Ultima mossa: [%s%d] -> [%s%d]", 
                    getLetter(lastMove.getX0()), lastMove.getY0(), getLetter(lastMove.getX()), lastMove.getY()));
            } else {
                binding.textView2.setText("Inizia una nuova partita");
            }
        });
    }

    @Override public void onConnectionStateChanged(boolean connected) { }

    @Override
    public void updateNetworkInfo(String role, String status) {
        runOnUiThread(() -> {
            if (binding.switchLoopback.isChecked()) {
                binding.textViewNetworkRole.setText(getString(R.string.loopback_active));
            } else {
                binding.textViewNetworkRole.setText(getString(R.string.net_role_prefix, role));
            }
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
        loopbackTransport.disconnect();
    }
}
