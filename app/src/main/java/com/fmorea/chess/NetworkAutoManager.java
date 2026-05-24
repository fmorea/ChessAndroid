package com.fmorea.chess;

import android.os.Handler;
import android.os.Looper;

/**
 * Automatically manages network roles and handles server election.
 * Follows Unix philosophy: one thing, managing role transitions.
 */
public class NetworkAutoManager implements NetworkHandler.NetworkListener {
    private final NetworkDiscovery discovery;
    private final NetworkHandler transport;
    private final ChessGameController controller;
    private final Handler handler = new Handler(Looper.getMainLooper());
    
    private boolean isServer = false;
    private static final int SCAN_PERIOD = 4000;

    public NetworkAutoManager(NetworkDiscovery discovery, NetworkHandler transport, ChessGameController controller) {
        this.discovery = discovery;
        this.transport = transport;
        this.controller = controller;
        this.transport.addListener(this);
    }

    public void start() {
        isServer = false;
        controller.setNetworkStatus("Scanning network...");
        
        discovery.startListening((ip, peerId) -> {
            if (isServer) {
                // Server Election: If a peer with a smaller ID is found, I yield and become client.
                if (peerId < discovery.getMyId()) {
                    yieldToServer(ip);
                }
            } else if (transport.getState() == NetworkHandler.State.IDLE) {
                // If I'm not a server and not connected, connect to the found server.
                connectTo(ip);
            }
        });

        // Initial scan: wait to see if a server already exists.
        handler.postDelayed(() -> {
            if (transport.getState() == NetworkHandler.State.IDLE && !isServer) {
                becomeServer();
            }
        }, SCAN_PERIOD);
    }

    private void becomeServer() {
        isServer = true;
        controller.setServer(true);
        controller.setNetworkStatus("Hosting at " + transport.getMyAddress());
        transport.startServer();
        discovery.startBroadcast();
    }

    private void connectTo(String ip) {
        handler.removeCallbacksAndMessages(null);
        isServer = false;
        controller.setServer(false);
        controller.setNetworkStatus("Connecting to " + ip + "...");
        transport.connect(ip);
    }

    private void yieldToServer(String ip) {
        controller.setNetworkStatus("Conflict! Yielding to peer...");
        isServer = false;
        discovery.stopAll();
        transport.disconnect();
        // Give the winner some time to re-initialize its server socket if needed
        handler.postDelayed(() -> connectTo(ip), 500);
    }

    public void stop() {
        handler.removeCallbacksAndMessages(null);
        discovery.stopAll();
        transport.disconnect();
    }

    @Override public void onMessage(String text) {}
    @Override public void onConnected() { discovery.stopAll(); }
    @Override public void onDisconnected() {
        // If the peer leaves, restart the search cycle
        if (transport.getState() == NetworkHandler.State.IDLE) {
            handler.postDelayed(this::start, 2000);
        }
    }
}
