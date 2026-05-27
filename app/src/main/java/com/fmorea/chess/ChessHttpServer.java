package com.fmorea.chess;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

/**
 * A simple HTTP server to provide a web interface for the chess game.
 * Features dynamic updates, drag-and-drop, and a terminal-style chat.
 */
public class ChessHttpServer implements NetworkHandler.NetworkListener {
    private static final String TAG = "ChessHttpServer";
    private final ChessGameController controller;
    private final int port;
    private ServerSocket serverSocket;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile boolean running = false;
    private final List<String> chatMessages = Collections.synchronizedList(new ArrayList<>());

    public ChessHttpServer(ChessGameController controller, int port, NetworkHandler transport) {
        this.controller = controller;
        this.port = port;
        if (transport != null) {
            transport.addListener(this);
        }
    }

    public void start() {
        if (running) return;
        running = true;
        executor.execute(() -> {
            try {
                serverSocket = new ServerSocket(port);
                Log.d(TAG, "HTTP Server started on port " + port);
                while (running) {
                    try {
                        Socket client = serverSocket.accept();
                        handleClient(client);
                    } catch (IOException e) {
                        if (running) Log.e(TAG, "Error accepting client", e);
                    }
                }
            } catch (IOException e) {
                if (running) Log.e(TAG, "Could not start server on port " + port, e);
            }
        });
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Error closing server socket", e);
        }
    }

    private void handleClient(Socket client) {
        executor.execute(() -> {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), "UTF-8"));
                 OutputStream out = client.getOutputStream()) {
                
                String line = in.readLine();
                if (line == null) return;
                
                String[] parts = line.split(" ");
                if (parts.length < 2) return;
                
                String path = parts[1];

                if (path.startsWith("/move?")) {
                    handleMove(path);
                    sendRedirect(out, "/");
                } else if (path.startsWith("/chat?")) {
                    handleChat(path);
                    sendRedirect(out, "/");
                } else if (path.equals("/undo")) {
                    mainHandler.post(controller::undo);
                    sendRedirect(out, "/");
                } else if (path.equals("/reset")) {
                    mainHandler.post(controller::resetGame);
                    sendRedirect(out, "/");
                } else if (path.equals("/state")) {
                    sendTextResponse(out, controller.getGameLogic().serializeBoard() + "|" + getChatPayload());
                } else {
                    sendHtmlResponse(out, generateHtml());
                }
            } catch (IOException e) {
                Log.e(TAG, "Error handling client", e);
            } finally {
                try { client.close(); } catch (IOException ignored) {}
            }
        });
    }

    private void handleMove(String path) {
        try {
            Uri uri = Uri.parse("http://localhost" + path);
            int fC = Integer.parseInt(uri.getQueryParameter("fC"));
            int fR = Integer.parseInt(uri.getQueryParameter("fR"));
            int tC = Integer.parseInt(uri.getQueryParameter("tC"));
            int tR = Integer.parseInt(uri.getQueryParameter("tR"));
            mainHandler.post(() -> controller.movePiece(fC, fR, tC, tR));
        } catch (Exception ignored) {}
    }

    private void handleChat(String path) {
        try {
            Uri uri = Uri.parse("http://localhost" + path);
            String msg = uri.getQueryParameter("msg");
            if (msg != null && !msg.trim().isEmpty()) {
                String formatted = "Web: " + msg;
                chatMessages.add(formatted);
                // Send to Android peers via controller's transport
                mainHandler.post(() -> {
                    NetworkHandler transport = MainActivity.getTransport();
                    if (transport != null) {
                        transport.send(ChessProtocol.formatChat(msg));
                    }
                });
            }
        } catch (Exception ignored) {}
    }

    private String getChatPayload() {
        StringBuilder sb = new StringBuilder();
        synchronized (chatMessages) {
            int start = Math.max(0, chatMessages.size() - 20);
            for (int i = start; i < chatMessages.size(); i++) {
                sb.append(chatMessages.get(i)).append("\n");
            }
        }
        return sb.toString();
    }

    @Override
    public void onMessage(String raw) {
        if (ChessProtocol.getType(raw) == ChessProtocol.MessageType.CHAT) {
            chatMessages.add("Peer: " + ChessProtocol.parseChat(raw));
        }
    }
    @Override public void onConnected() { chatMessages.add("System: Link established."); }
    @Override public void onDisconnected() { chatMessages.add("System: Link lost."); }

    private void sendRedirect(OutputStream out, String location) throws IOException {
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, "UTF-8"));
        writer.println("HTTP/1.1 302 Found\r\nLocation: " + location + "\r\n\r\n");
        writer.flush();
    }

    private void sendTextResponse(OutputStream out, String text) throws IOException {
        byte[] bytes = text.getBytes("UTF-8");
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, "UTF-8"));
        writer.println("HTTP/1.1 200 OK\r\nContent-Type: text/plain; charset=utf-8\r\nContent-Length: " + bytes.length + "\r\nConnection: close\r\n\r\n");
        writer.print(text);
        writer.flush();
    }

    private void sendHtmlResponse(OutputStream out, String html) throws IOException {
        byte[] bytes = html.getBytes("UTF-8");
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, "UTF-8"));
        writer.println("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: " + bytes.length + "\r\nConnection: close\r\n\r\n");
        writer.print(html);
        writer.flush();
    }

    private String generateHtml() {
        GameLogic logic = controller.getGameLogic();
        String boardData = logic.serializeBoard();
        String[] parts = boardData.split(",");
        String turn = controller.isWhiteTurn() ? "White's turn" : "Black's turn";

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><title>Chess Web Remote</title>");
        sb.append("<meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no'>");
        sb.append("<style>");
        sb.append("body { margin: 0; font-family: monospace; background: #121212; color: #00FF00; display: flex; flex-direction: column; height: 100vh; overflow: hidden; }");
        sb.append(".topBar { background: #1a1a1a; padding: 10px; display: flex; gap: 10px; border-bottom: 1px solid #333; }");
        sb.append("button { background: #333; color: #0f0; border: 1px solid #0f0; padding: 5px 15px; cursor: pointer; font-family: inherit; }");
        sb.append(".main-content { flex: 1; display: flex; flex-wrap: wrap; overflow-y: auto; align-items: center; justify-content: center; padding: 10px; }");
        sb.append(".board { display: grid; grid-template-columns: repeat(8, 1fr); width: 90vw; max-width: 450px; aspect-ratio: 1/1; border: 4px solid #4E342E; background: #4E342E; margin: 10px; }");
        sb.append(".cell { aspect-ratio: 1/1; display: flex; align-items: center; justify-content: center; font-size: 36px; cursor: pointer; position: relative; }");
        sb.append(".light { background: #DEDFC4; } .dark { background: #2D942D; }");
        sb.append(".piece { filter: drop-shadow(0 2px 2px rgba(0,0,0,0.5)); transition: transform 0.1s; }");
        sb.append(".selected { box-shadow: inset 0 0 0 4px #fff; }");
        sb.append(".cursor { box-shadow: inset 0 0 0 5px #FF9800; }");
        
        sb.append(".chat-container { width: 90vw; max-width: 450px; height: 200px; display: flex; flex-direction: column; background: #000; border: 1px solid #0f0; margin: 10px; }");
        sb.append(".chat-log { flex: 1; padding: 5px; overflow-y: auto; font-size: 12px; white-space: pre-wrap; }");
        sb.append(".chat-input { display: flex; border-top: 1px solid #0f0; }");
        sb.append(".chat-input input { flex: 1; background: #000; color: #0f0; border: none; padding: 8px; outline: none; font-family: inherit; }");
        
        sb.append(".info { padding: 10px; background: #1a1a1a; font-size: 14px; text-align: center; }");
        sb.append("</style></head><body>");
        
        sb.append("<div class='topBar'><button onclick=\"location.href='/reset'\">RESET</button><button onclick=\"location.href='/undo'\">UNDO</button></div>");
        
        sb.append("<div class='main-content'>");
        sb.append("<div class='board' id='chessboard'>");
        for (int r = 7; r >= 0; r--) {
            for (int c = 0; c < 8; c++) {
                String p = parts[r * 8 + c];
                sb.append("<div class='cell ").append((r + c) % 2 != 0 ? "light" : "dark").append("' id='c-").append(c+1).append("-").append(r+1).append("' onclick='tap(").append(c+1).append(",").append(r+1).append(")'>");
                if (!p.equals("null")) sb.append("<span class='piece'>").append(getUnicodePiece(p)).append("</span>");
                sb.append("</div>");
            }
        }
        sb.append("</div>");
        
        sb.append("<div class='chat-container'>");
        sb.append("<div class='chat-log' id='chatLog'>").append(getChatPayload()).append("</div>");
        sb.append("<div class='chat-input'><input type='text' id='chatIn' placeholder='Type command...' onkeydown='if(event.key===\"Enter\") sendChat()'><button onclick='sendChat()'>SEND</button></div>");
        sb.append("</div></div>");

        sb.append("<div class='info' id='status'>").append(turn).append(logic.isInCheck()?" - CHECK!":"").append("</div>");

        sb.append("<script>let sel=null, cur={c:4,r:4}, lastBoard='").append(boardData).append("';");
        sb.append("function upCur(){document.querySelectorAll('.cell').forEach(e=>e.classList.remove('cursor'));document.getElementById(`c-${cur.c}-${cur.r}`).classList.add('cursor')}");
        sb.append("function tap(c,r){cur={c,r};upCur();if(!sel){let e=document.getElementById(`c-${c}-${r}`);if(e.innerHTML){sel={c,r};e.classList.add('selected')}}else{if(sel.c==c&&sel.r==r){document.getElementById(`c-${c}-${r}`).classList.remove('selected');sel=null}else{location.href=`/move?fC=${sel.c}&fR=${sel.r}&tC=${c}&tR=${r}`}}}");
        sb.append("function sendChat(){let i=document.getElementById('chatIn'); if(i.value) location.href='/chat?msg='+encodeURIComponent(i.value); }");
        
        sb.append("setInterval(()=>{fetch('/state').then(r=>r.text()).then(res=>{");
        sb.append("  const [board, chat] = res.split('|');");
        sb.append("  if(board !== lastBoard) location.reload();");
        sb.append("  const log = document.getElementById('chatLog'); if(log.innerText !== chat){ log.innerText = chat; log.scrollTop = log.scrollHeight; }");
        sb.append("});}, 1500); upCur();");
        sb.append("</script></body></html>");
        return sb.toString();
    }

    private String getUnicodePiece(String code) {
        char type = code.charAt(0);
        boolean isB = code.charAt(3) == 'B';
        switch (type) {
            case 'p': return isB ? "&#9817;" : "&#9823;";
            case 't': return isB ? "&#9814;" : "&#9820;";
            case 'c': return isB ? "&#9816;" : "&#9822;";
            case 'a': return isB ? "&#9815;" : "&#9821;";
            case 'd': return isB ? "&#9813;" : "&#9819;";
            case 'r': return isB ? "&#9812;" : "&#9818;";
            default: return "";
        }
    }
}
