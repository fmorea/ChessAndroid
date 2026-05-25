package com.fmorea.chess;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.fmorea.chess.databinding.ActivityMainBinding;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements ChessGameController.GameUI, SensorEventListener {

    private ActivityMainBinding binding;
    private final ChessModel model = new ChessModel();
    private NetworkHandler transport;
    private NetworkDiscovery discovery;
    private ChessGameController controller;
    private NetworkAutoManager autoManager;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private NetworkHandler loopbackTransport;
    private ChessGameController loopbackController;
    private final ChessModel loopbackModel = new ChessModel();

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gravitySensor;
    private int drawMode = 0; // 0: OFF, 1: PEN, 2: ERASER

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
        makeNavControlsDraggable();
        setupSensors();
        
        autoManager.start();
        controller.notifyUI();
    }

    private void setupSensors() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        if (gravitySensor == null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
    }

    private void setupUI() {
        binding.switch2.setOnCheckedChangeListener((v, c) -> { if (c != model.isBlackPointOfView()) model.setBlackPointOfView(c); binding.chessView.invalidate(); });
        binding.switch3.setOnCheckedChangeListener((v, c) -> { model.setAutoRotate(c); binding.chessView.invalidate(); });
        
        binding.switchLoopback.setOnCheckedChangeListener((v, isChecked) -> {
            if (isChecked) {
                autoManager.stop();
                transport.disconnect();
                loopbackTransport.disconnect();
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

        // WASD Listeners
        binding.btnUp.setOnClickListener(v -> binding.chessView.moveCursor(0, 1));
        binding.btnDown.setOnClickListener(v -> binding.chessView.moveCursor(0, -1));
        binding.btnLeft.setOnClickListener(v -> binding.chessView.moveCursor(-1, 0));
        binding.btnRight.setOnClickListener(v -> binding.chessView.moveCursor(1, 0));
        binding.btnSelect.setOnClickListener(v -> binding.chessView.selectCursor());

        // Bottom Menu handling
        binding.bottomAppBar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_zoom_in) {
                binding.chessView.zoomIn();
                return true;
            } else if (id == R.id.action_zoom_out) {
                binding.chessView.zoomOut();
                return true;
            } else if (id == R.id.action_draw_tool) {
                drawMode = (drawMode + 1) % 3;
                if (drawMode == 0) {
                    binding.chessView.setPenMode(false);
                    binding.chessView.setEraserMode(false);
                    Toast.makeText(this, "Disegno: OFF", Toast.LENGTH_SHORT).show();
                    item.setIcon(android.R.drawable.ic_menu_edit);
                } else if (drawMode == 1) {
                    binding.chessView.setPenMode(true);
                    Toast.makeText(this, "Modalità PENNA", Toast.LENGTH_SHORT).show();
                    item.setIcon(android.R.drawable.ic_menu_edit);
                } else {
                    binding.chessView.setEraserMode(true);
                    Toast.makeText(this, "Modalità GOMMA", Toast.LENGTH_SHORT).show();
                    item.setIcon(android.R.drawable.ic_menu_delete);
                }
                return true;
            } else if (id == R.id.action_undo) {
                controller.undo();
                return true;
            } else if (id == R.id.action_redo) {
                controller.redo();
                return true;
            } else if (id == R.id.action_chat) {
                startActivity(new Intent(this, ChatActivity.class));
                return true;
            } else if (id == R.id.action_privacy_policy) {
                startActivity(new Intent(this, PrivacyPolicy.class));
                return true;
            }
            return false;
        });
    }

    private void makeNavControlsDraggable() {
        binding.navDragHandle.setOnTouchListener(new View.OnTouchListener() {
            float dX, dY;
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = binding.navControls.getX() - event.getRawX();
                        dY = binding.navControls.getY() - event.getRawY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        binding.navControls.setX(event.getRawX() + dX);
                        binding.navControls.setY(event.getRawY() + dY);
                        break;
                    default:
                        return false;
                }
                return true;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gravitySensor != null) {
            sensorManager.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_GAME);
        } else if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_GRAVITY || event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];

            // Ottieni la rotazione del display per mappare correttamente gli assi
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            float screenX, screenY;

            switch (rotation) {
                case Surface.ROTATION_90:
                    screenX = -y;
                    screenY = x;
                    break;
                case Surface.ROTATION_180:
                    screenX = -x;
                    screenY = -y;
                    break;
                case Surface.ROTATION_270:
                    screenX = y;
                    screenY = -x;
                    break;
                case Surface.ROTATION_0:
                default:
                    screenX = x;
                    screenY = y;
                    break;
            }

            // Calcola il tilt per l'effetto parallasse (gravità visiva)
            float multiplier = 3.2f;
            binding.chessView.setTilt(-screenX * multiplier, screenY * multiplier);

            // Auto-rotazione della scacchiera basata sulla gravità reale (alto/basso)
            if (model.isAutoRotate()) {
                if (screenY < -4.5f && !model.isBlackPointOfView()) {
                    model.setBlackPointOfView(true);
                    refreshBoard();
                } else if (screenY > 4.5f && model.isBlackPointOfView()) {
                    model.setBlackPointOfView(false);
                    refreshBoard();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

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
