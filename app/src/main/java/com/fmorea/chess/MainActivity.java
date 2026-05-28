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
import android.view.HapticFeedbackConstants;
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

    private ChessHttpServer httpServer;
    private final Handler joystickHandler = new Handler(Looper.getMainLooper());
    private Runnable joystickRunnable;
    private static final int INITIAL_DELAY = 400;
    private static final int REPEAT_INTERVAL = 100;

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
        
        // Start the Mirror-HTTP Server linked to UI binding
        httpServer = new ChessHttpServer(this, binding, controller, 8080, transport);
        httpServer.start();
        
        controller.notifyUI();
        
        String serverUrl = "http://" + transport.getMyAddress() + ":8080";
        binding.textViewWebAddress.setText(serverUrl);
        Toast.makeText(this, "Web interface at " + serverUrl, Toast.LENGTH_LONG).show();
    }

    private void setupSensors() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        if (gravitySensor == null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
    }

    private void setupUI() {
        binding.switch2.setOnCheckedChangeListener((v, c) -> { 
            if (c != model.isBlackPointOfView()) {
                model.setBlackPointOfView(c);
                binding.chessView.setBoardOrientation(c);
            }
        });
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
        binding.btnRecenter.setOnClickListener(v -> binding.chessView.recenter());

        // Enhanced Joystick Setup
        setupJoystickButton(binding.btnLeft, -1, 0);
        setupJoystickButton(binding.btnUp, 0, 1);
        setupJoystickButton(binding.btnDown, 0, -1);
        setupJoystickButton(binding.btnRight, 1, 0);
        
        binding.btnSelect.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            binding.chessView.selectCursor();
        });

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

    private void setupJoystickButton(View btn, final int dx, final int dy) {
        btn.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(50).start();
                    binding.chessView.moveCursor(dx, dy);
                    
                    joystickRunnable = new Runnable() {
                        @Override
                        public void run() {
                            binding.chessView.moveCursor(dx, dy);
                            v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                            joystickHandler.postDelayed(this, REPEAT_INTERVAL);
                        }
                    };
                    joystickHandler.postDelayed(joystickRunnable, INITIAL_DELAY);
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                    joystickHandler.removeCallbacks(joystickRunnable);
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
    protected void onDestroy() {
        super.onDestroy();
        if (httpServer != null) httpServer.stop();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_GRAVITY || event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];

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

            float multiplier = 3.2f;
            binding.chessView.setGravityTilt(-screenX * multiplier, screenY * multiplier);

            if (model.isAutoRotate()) {
                if (screenY < -4.5f && !model.isBlackPointOfView()) {
                    model.setBlackPointOfView(true);
                    binding.chessView.setBoardOrientation(true);
                } else if (screenY > 4.5f && model.isBlackPointOfView()) {
                    model.setBlackPointOfView(false);
                    binding.chessView.setBoardOrientation(false);
                }
                binding.chessView.invalidate();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void refreshBoard() {
        binding.chessView.invalidate();
    }

    @Override
    public void updateStatus(int mode, boolean isChecked, boolean whitesTurn, Movement lastMove, int lastMoveCount) {
        String turn = whitesTurn ? "White's turn" : "Black's turn";
        binding.textView3.setText(turn);
        
        if (lastMove != null) {
            binding.textView2.setText(String.format(Locale.getDefault(), "Move %d: %s", lastMoveCount, lastMove.toString()));
        } else {
            binding.textView2.setText("New game");
        }
    }

    @Override
    public void onConnectionStateChanged(boolean connected) {
        // UI updates for connection state
    }

    @Override
    public void updateNetworkInfo(String role, String status) {
        binding.textViewNetworkRole.setText(role);
        binding.textViewNetworkStatus.setText(status);
    }

    @Override
    public void onMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showPromotionDialog(int fromCol, int fromRow, int toCol, int toRow, boolean isWhite) {
        String[] items = {"Queen", "Rook", "Bishop", "Knight"};
        new AlertDialog.Builder(this)
                .setTitle("Select Promotion")
                .setItems(items, (dialog, which) -> {
                    String suffix = isWhite ? "B" : "N";
                    String promo;
                    switch (which) {
                        case 1: promo = "tor" + suffix; break;
                        case 2: promo = "alf" + suffix; break;
                        case 3: promo = "cav" + suffix; break;
                        default: promo = "don" + suffix; break;
                    }
                    controller.movePiece(fromCol, fromRow, toCol, toRow, promo);
                })
                .setCancelable(false)
                .show();
    }
}
