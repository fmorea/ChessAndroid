package com.fmorea.chess;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.ScrollView;
import androidx.appcompat.app.AppCompatActivity;
import com.fmorea.chess.databinding.ActivityChatBinding;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * ChatActivity handles the Terminal-style chat UI.
 * Following Unix philosophy: it does one thing - providing a chat interface over the existing transport.
 */
public class ChatActivity extends AppCompatActivity implements NetworkHandler.NetworkListener {

    private ActivityChatBinding binding;
    private NetworkHandler transport;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        transport = MainActivity.getTransport();
        if (transport != null) {
            transport.addListener(this);
            transport.send(ChessProtocol.formatChat("User joined the terminal."));
        } else {
            appendLog("System: Transport layer not available.");
        }

        binding.btnSendChat.setOnClickListener(v -> sendMessage());

        binding.editChatMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND || 
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                sendMessage();
                return true;
            }
            return false;
        });

        appendLog("System: Terminal session initialized.");
    }

    private void sendMessage() {
        String msg = binding.editChatMessage.getText().toString().trim();
        if (!msg.isEmpty() && transport != null) {
            transport.send(ChessProtocol.formatChat(msg));
            appendLog("You: " + msg);
            binding.editChatMessage.setText("");
        }
    }

    private void appendLog(String text) {
        String time = timeFormat.format(new Date());
        String entry = String.format("[%s] %s\n", time, text);
        binding.chatLog.append(entry);
        
        // Auto-scroll to bottom
        binding.chatScrollView.post(() -> 
            binding.chatScrollView.fullScroll(ScrollView.FOCUS_DOWN)
        );
    }

    // --- NetworkHandler.NetworkListener implementation ---

    @Override
    public void onMessage(String raw) {
        if (ChessProtocol.getType(raw) == ChessProtocol.MessageType.CHAT) {
            String message = ChessProtocol.parseChat(raw);
            appendLog("Peer: " + message);
        }
    }

    @Override
    public void onConnected() {
        appendLog("System: Link established.");
    }

    @Override
    public void onDisconnected() {
        appendLog("System: Link lost.");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (transport != null) {
            transport.removeListener(this);
        }
    }
}
