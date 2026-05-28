package com.fmorea.chess;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;
import com.fmorea.chess.databinding.ActivityMainBinding;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * ChessHttpServer: Mobile-first web console with drag&drop, hints, and global chat.
 * Optimized for low-latency feedback and responsiveness.
 */
public class ChessHttpServer implements NetworkHandler.NetworkListener {
    private static final String TAG = "ChessHttpServer";
    private final ChessGameController controller;
    private final int port;
    private final Context context;
    private final ActivityMainBinding binding;
    private ServerSocket serverSocket;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile boolean running = false;
    private final List<String> chatMessages = Collections.synchronizedList(new ArrayList<>());
    private final NetworkHandler transport;

    public ChessHttpServer(Context context, ActivityMainBinding binding, ChessGameController controller, int port, NetworkHandler transport) {
        this.context = context;
        this.binding = binding;
        this.controller = controller;
        this.port = port;
        this.transport = transport;
        if (transport != null) transport.addListener(this);
    }

    public void start() {
        if (running) return;
        running = true;
        executor.execute(() -> {
            try {
                serverSocket = new ServerSocket(port);
                Log.i(TAG, "Server started on port " + port);
                while (running) {
                    try {
                        Socket client = serverSocket.accept();
                        handleClient(client);
                    } catch (IOException ignored) {}
                }
            } catch (IOException e) {
                Log.e(TAG, "Server error", e);
            }
        });
    }

    public void stop() {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) {}
    }

    private void handleClient(Socket client) {
        executor.execute(() -> {
            try (InputStream in = client.getInputStream();
                 OutputStream out = client.getOutputStream()) {
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                String line = reader.readLine();
                if (line == null) return;
                
                String[] parts = line.split(" ");
                if (parts.length < 2) return;
                String path = parts[1];

                if (path.startsWith("/move?")) {
                    handleMove(path);
                    sendResponse(out, "text/plain", "OK".getBytes(StandardCharsets.UTF_8));
                } else if (path.startsWith("/action/")) {
                    handleAction(path);
                    sendResponse(out, "text/plain", "OK".getBytes(StandardCharsets.UTF_8));
                } else if (path.startsWith("/chat?msg=")) {
                    handleChatFromWeb(path);
                    sendResponse(out, "text/plain", "OK".getBytes(StandardCharsets.UTF_8));
                } else if (path.startsWith("/res/")) {
                    handleResource(out, path);
                } else if (path.equals("/state")) {
                    sendResponse(out, "application/json", getFullStateJson().getBytes(StandardCharsets.UTF_8));
                } else {
                    sendResponse(out, "text/html", generateHtml().getBytes(StandardCharsets.UTF_8));
                }
            } catch (Exception e) {
                Log.e(TAG, "Client error", e);
            } finally {
                try { client.close(); } catch (IOException ignored) {}
            }
        });
    }

    private void sendResponse(OutputStream out, String contentType, byte[] body) throws IOException {
        String headers = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: " + contentType + "; charset=utf-8\r\n" +
                "Content-Length: " + body.length + "\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n\r\n";
        out.write(headers.getBytes(StandardCharsets.UTF_8));
        out.write(body);
        out.flush();
    }

    private void handleChatFromWeb(String path) {
        try {
            String msg = Uri.parse("http://localhost" + path).getQueryParameter("msg");
            if (msg != null) {
                String formatted = ChessProtocol.formatChat("Web: " + msg);
                if (transport != null) transport.send(formatted);
                mainHandler.post(() -> controller.onMessage(formatted));
            }
        } catch (Exception ignored) {}
    }

    private void handleAction(String path) {
        String actionId = path.substring(8);
        mainHandler.post(() -> {
            try {
                if (actionId.equals("action_undo")) controller.undo();
                else if (actionId.equals("action_redo")) controller.redo();
                else {
                    int id = context.getResources().getIdentifier(actionId, "id", context.getPackageName());
                    if (id != 0) {
                        View v = binding.getRoot().findViewById(id);
                        if (v != null) {
                            if (v instanceof CompoundButton) ((CompoundButton) v).toggle();
                            else v.performClick();
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Action error", e);
            }
        });
    }

    private void handleResource(OutputStream out, String path) throws IOException {
        String name = path.substring(5).replace(".png", "");
        int resId = context.getResources().getIdentifier(name, "drawable", context.getPackageName());
        if (resId == 0) return;
        Bitmap b = BitmapFactory.decodeResource(context.getResources(), resId);
        if (b == null) return;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        b.compress(Bitmap.CompressFormat.PNG, 100, baos);
        sendResponse(out, "image/png", baos.toByteArray());
    }

    private String getFullStateJson() {
        final FutureTask<String> task = new FutureTask<>(() -> {
            JSONObject json = new JSONObject();
            json.put("board", controller.getGameLogic().serializeBoard());
            json.put("ui", reflectView(binding.getRoot()));
            
            JSONArray moves = new JSONArray();
            ArrayList<Movement> legalMoves = controller.getGameLogic().getLegalMoves();
            if (legalMoves != null) {
                for (Movement m : legalMoves) {
                    JSONObject move = new JSONObject();
                    move.put("fC", m.getX0()); move.put("fR", m.getY0());
                    move.put("tC", m.getX()); move.put("tR", m.getY());
                    moves.put(move);
                }
            }
            json.put("legalMoves", moves);

            JSONArray chat = new JSONArray();
            synchronized (chatMessages) {
                for (String m : chatMessages) chat.put(m);
            }
            json.put("chat", chat);
            return json.toString();
        });
        mainHandler.post(task);
        try {
            return task.get(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            return "{}";
        }
    }

    private JSONObject reflectView(View v) {
        JSONObject res = new JSONObject();
        try {
            res.put("type", v.getClass().getSimpleName());
            try {
                int id = v.getId();
                if (id != View.NO_ID && id > 0) res.put("id", context.getResources().getResourceEntryName(id));
            } catch (Exception ignored) {}
            res.put("visible", v.getVisibility() == View.VISIBLE);
            if (v instanceof TextView) res.put("text", ((TextView) v).getText().toString());
            if (v instanceof CompoundButton) res.put("checked", ((CompoundButton) v).isChecked());
            if (v instanceof ViewGroup) {
                JSONArray children = new JSONArray();
                ViewGroup g = (ViewGroup) v;
                for (int i = 0; i < g.getChildCount(); i++) {
                    View child = g.getChildAt(i);
                    if (child != null) children.put(reflectView(child));
                }
                res.put("children", children);
            }
        } catch (Exception ignored) {}
        return res;
    }

    private void handleMove(String path) {
        try {
            Uri uri = Uri.parse("http://localhost" + path);
            int fC = Integer.parseInt(uri.getQueryParameter("fC")), fR = Integer.parseInt(uri.getQueryParameter("fR"));
            int tC = Integer.parseInt(uri.getQueryParameter("tC")), tR = Integer.parseInt(uri.getQueryParameter("tR"));
            mainHandler.post(() -> controller.movePiece(fC, fR, tC, tR));
        } catch (Exception ignored) {}
    }

    @Override
    public void onMessage(String text) {
        if (ChessProtocol.getType(text) == ChessProtocol.MessageType.CHAT) {
            chatMessages.add(ChessProtocol.parseChat(text));
            if (chatMessages.size() > 50) chatMessages.remove(0);
        }
    }

    @Override public void onConnected() { chatMessages.add("System: Link established."); }
    @Override public void onDisconnected() { chatMessages.add("System: Link lost."); }

    private String generateHtml() {
        return "<!DOCTYPE html><html><head><title>Chess Mobile Mirror</title>" +
               "<meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no'>" +
               "<style>" +
               "body { margin: 0; background: #000; color: #fff; font-family: -apple-system, sans-serif; display: flex; flex-direction: column; height: 100vh; overflow: hidden; }" +
               ".header { padding: 10px; background: #1a1a1a; display: flex; justify-content: space-between; font-size: 14px; font-weight: bold; border-bottom: 1px solid #333; color: #bb86fc; }" +
               ".board-wrapper { flex: 0 0 auto; width: 100vw; height: 100vw; display: flex; align-items: center; justify-content: center; background: #111; perspective: 1000px; }" +
               ".board { width: 95%; height: 95%; display: grid; grid-template-columns: repeat(8, 1fr); border: 8px solid #2c1e14; background: #2d942d; transition: transform 0.6s; }" +
               ".cell { aspect-ratio: 1/1; display: flex; align-items: center; justify-content: center; position: relative; cursor: pointer; }" +
               ".light { background: #dedfc4; } .dark { background: #2d942d; }" +
               ".piece { width: 90%; height: 90%; z-index: 10; cursor: grab; touch-action: none; -webkit-user-drag: none; }" +
               ".selected { background: rgba(255, 255, 0, 0.4) !important; }" +
               ".hint { width: 25%; height: 25%; background: rgba(0,0,0,0.15); border-radius: 50%; pointer-events: none; }" +
               ".hint.capture { width: 80%; height: 80%; border: 4px solid rgba(0,0,0,0.1); background: transparent; }" +
               ".content-area { flex: 1; display: flex; flex-direction: column; overflow: hidden; background: #1a1a1a; }" +
               ".tabs { display: flex; background: #222; }" +
               ".tab { flex: 1; padding: 12px; text-align: center; font-size: 12px; text-transform: uppercase; letter-spacing: 1px; cursor: pointer; border-bottom: 2px solid transparent; }" +
               ".tab.active { border-bottom: 2px solid #bb86fc; color: #bb86fc; }" +
               ".tab-content { flex: 1; overflow-y: auto; padding: 10px; display: none; }" +
               ".tab-content.active { display: flex; flex-direction: column; }" +
               ".controls-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }" +
               "button { background: #333; color: #fff; border: 1px solid #444; padding: 12px; border-radius: 6px; cursor: pointer; font-size: 13px; }" +
               ".switch-row { display: flex; justify-content: space-between; align-items: center; padding: 10px; background: #2c2c2c; border-radius: 6px; margin-bottom: 4px; }" +
               ".chat-messages { flex: 1; overflow-y: auto; display: flex; flex-direction: column; gap: 6px; padding-bottom: 50px; }" +
               ".msg { padding: 8px 12px; background: #333; border-radius: 12px; max-width: 85%; font-size: 13px; line-height: 1.4; border-left: 3px solid #bb86fc; }" +
               ".chat-input-fixed { position: absolute; bottom: 0; left: 0; right: 0; padding: 8px; background: #222; display: flex; gap: 8px; border-top: 1px solid #333; }" +
               ".chat-input-fixed input { flex: 1; background: #333; border: 1px solid #444; color: #fff; padding: 10px; border-radius: 20px; outline: none; }" +
               "</style></head><body>" +
               "<div class='header'><span id='turn-info'>...</span><span id='move-info'>...</span></div>" +
               "<div class='board-wrapper'><div class='board' id='board' style='transform: rotateX(0deg)'></div></div>" +
               "<div class='content-area'>" +
               " <div class='tabs'><div class='tab active' onclick='showTab(\"ctrl\")'>Comandi</div><div class='tab' onclick='showTab(\"chat\")'>Chat</div></div>" +
               " <div id='tab-ctrl' class='tab-content active'><div id='controls' class='controls-grid'></div><button onclick='toggle3D()' style='margin-top:15px; border-color:#bb86fc'>VISTA 2D/3D</button></div>" +
               " <div id='tab-chat' class='tab-content'><div class='chat-messages' id='chat-box'></div>" +
               "  <div class='chat-input-fixed'><input type='text' id='chat-in' placeholder='Scrivi...' onkeydown='if(event.key===\"Enter\")sendChat()'>" +
               "  <button style='width:auto; border-radius:50%' onclick='sendChat()'>></button></div></div>" +
               "</div>" +
               "<script>" +
               "let sel=null, legalMoves=[], is3D=false, lastBoardData='', lastChatLen=0;" +
               "function showTab(t){ " +
               " document.querySelectorAll('.tab, .tab-content').forEach(e=>e.classList.remove('active'));" +
               " document.querySelector(`.tab[onclick*=\"${t}\"]`).classList.add('active');" +
               " document.getElementById('tab-'+t).classList.add('active');" +
               "}" +
               "function getRes(c){ if(!c || c==='null') return ''; const t=c[0],isW=c[3]==='B'; " +
               " let p=''; switch(t){ case'p':p='pawn';break; case't':p='rook';break; case'c':p='knight';break; case'a':p='bishop';break; case'd':p='queen';break; case'r':p='king';break; } " +
               " return `/res/${p}_${isW?'white':'black'}.png`; }" +
               "function toggle3D(){ is3D=!is3D; document.getElementById('board').style.transform = is3D ? 'rotateX(25deg)' : 'rotateX(0deg)'; }" +
               "function renderNode(n){" +
               " if(!n || (!n.visible && n.type!=='ConstraintLayout')) return null; let e;" +
               " if(n.type==='ChessView' || n.type==='View') return null;" +
               " if(n.type.includes('Button')){ e=document.createElement('button'); e.innerText=n.text||n.id; e.onclick=()=>fetch('/action/'+n.id); }" +
               " else if(n.type.includes('Switch')){ e=document.createElement('div'); e.className='switch-row'; e.innerHTML=`<span>${n.text}</span><input type='checkbox' ${n.checked?'checked':''} onchange=\"fetch('/action/'+n.id)\">`; }" +
               " else if(n.type.includes('TextView') && n.id){" +
               "  if(n.id==='textView3') document.getElementById('turn-info').innerText=n.text; " +
               "  if(n.id==='textView2') document.getElementById('move-info').innerText=n.text; " +
               "  return null; " +
               " } else { e=document.createElement('div'); }" +
               " if(n.children) n.children.forEach(c=>{ const child=renderNode(c); if(child) e.appendChild(child); });" +
               " return (e.childNodes.length > 0 || e.tagName==='BUTTON') ? e : null;" +
               "}" +
               "function drawBoard(s){" +
               " if(!s) return; const p=s.split(','), b=document.getElementById('board'); b.innerHTML=''; " +
               " for(let r=7;r>=0;r--){ for(let c=0;c<8;c++){" +
               "  const v=p[r*8+c], e=document.createElement('div'), col=c+1, row=r+1; " +
               "  e.className='cell '+((r+c)%2!==0?'light':'dark'); if(sel && sel.c==col && sel.r==row) e.classList.add('selected');" +
               "  e.onclick=()=>tap(col,row); e.ondragover=ev=>ev.preventDefault(); e.ondrop=ev=>{ ev.preventDefault(); tap(col,row); };" +
               "  if(sel && legalMoves.some(m=>m.fC==sel.c && m.fR==sel.r && m.tC==col && m.tR==row)){" +
               "   const h=document.createElement('div'); h.className='hint' + (v!=='null'?' capture':''); e.appendChild(h);" +
               "  }" +
               "  if(v && v!=='null' && v.length > 3) { " +
               "   const img=document.createElement('img'); img.src=getRes(v); img.className='piece'; img.draggable=true;" +
               "   img.addEventListener('touchstart', (ev)=>{ sel={c:col,r:row}; drawBoard(lastBoardData); }, {passive:true});" +
               "   img.ondragstart=()=>{ sel={c:col,r:row}; drawBoard(lastBoardData); }; e.appendChild(img); " +
               "  } b.appendChild(e); " +
               " } }" +
               "}" +
               "function tap(c,r){" +
               " if(!sel){ sel={c,r}; drawBoard(lastBoardData); } " +
               " else {" +
               "  if(sel.c==c && sel.r==r){ sel=null; drawBoard(lastBoardData); } " +
               "  else { fetch(`/move?fC=${sel.c}&fR=${sel.r}&tC=${c}&tR=${r}`).then(()=>{sel=null;update();}); } " +
               " }" +
               "}" +
               "function sendChat(){ const i=document.getElementById('chat-in'); if(!i.value) return; fetch('/chat?msg='+encodeURIComponent(i.value)).then(()=>i.value=''); }" +
               "function update(){ fetch('/state').then(r=>r.json()).then(data=>{ " +
               " const ctrls=document.getElementById('controls'); ctrls.innerHTML=''; const newNode=renderNode(data.ui); if(newNode) ctrls.appendChild(newNode); " +
               " if(data.board !== lastBoardData || legalMoves.length !== data.legalMoves.length){" +
               "  lastBoardData=data.board; legalMoves=data.legalMoves; drawBoard(data.board);" +
               " }" +
               " const box=document.getElementById('chat-box'); if(data.chat.length !== lastChatLen){" +
               "  box.innerHTML=''; data.chat.forEach(m=>{ const d=document.createElement('div'); d.className='msg'; d.innerText=m; box.appendChild(d); }); " +
               "  box.scrollTop = box.scrollHeight; lastChatLen=data.chat.length; " +
               " } }).catch(err=>console.error(err)); }" +
               "setInterval(update, 500); update();" +
               "</script></body></html>";
    }
}
