package com.fmorea.chess;

import java.util.ArrayList;

/**
 * ChessGameController coordinates the interaction between the Model, the Network, and the UI.
 * Following Unix philosophy: it handles the game logic state transitions and network sync.
 */
public class ChessGameController implements NetworkHandler.NetworkListener, ChessDelegate {
    private final ChessModel model;
    private final NetworkHandler transport;
    private final GameUI ui;
    private boolean isNetworkMove = false;
    private boolean isServer = false;

    public interface GameUI {
        void refreshBoard();
        void updateStatus(int material, boolean inCheck, boolean whiteTurn, Movement lastMove);
        void onConnectionStateChanged(boolean connected);
        void updateNetworkInfo(String role, String status);
        void onMessage(String msg);
    }

    public ChessGameController(ChessModel model, NetworkHandler transport, GameUI ui) {
        this.model = model;
        this.transport = transport;
        this.ui = ui;
        this.transport.addListener(this);
    }

    public void setServer(boolean server) {
        this.isServer = server;
        updateNetworkUI();
    }

    public void updateNetworkUI() {
        String role = isServer ? "Server" : "Client";
        String status = transport.getState().toString();
        ui.updateNetworkInfo(role, status);
    }

    public void setNetworkStatus(String status) {
        String role = isServer ? "Server" : "Client";
        ui.updateNetworkInfo(role, status);
    }

    public void resetGame() {
        model.reset();
        transport.send(ChessProtocol.formatReset());
        notifyUI();
    }

    public void undo() {
        model.getGameLogic().undo();
        transport.send(ChessProtocol.formatUndo());
        notifyUI();
    }

    public void redo() {
        model.getGameLogic().redo();
        transport.send(ChessProtocol.formatRedo());
        notifyUI();
    }

    private void notifyUI() {
        ui.refreshBoard();
        ui.updateStatus(
            model.getGameLogic().objectiveFunction(),
            model.getGameLogic().isInCheck(),
            model.getGameLogic().toccaAlBianco(),
            model.getGameLogic().getMov()
        );
    }

    // --- NetworkHandler.NetworkListener ---

    @Override
    public void onMessage(String raw) {
        ChessProtocol.MessageType type = ChessProtocol.getType(raw);
        switch (type) {
            case RESET:
                model.reset();
                notifyUI();
                break;
            case UNDO:
                model.getGameLogic().undo();
                notifyUI();
                break;
            case REDO:
                model.getGameLogic().redo();
                notifyUI();
                break;
            case MOVE:
                int[] m = ChessProtocol.parseMove(raw);
                if (m != null) {
                    isNetworkMove = true;
                    movePiece(m[0], m[1], m[2], m[3]);
                    isNetworkMove = false;
                }
                break;
            case BOARD:
                model.getGameLogic().deserializeBoard(ChessProtocol.parseBoard(raw));
                notifyUI();
                break;
            case CHAT:
                ui.onMessage(ChessProtocol.parseChat(raw));
                break;
        }
    }

    @Override
    public void onConnected() {
        if (isServer) {
            transport.send(ChessProtocol.formatBoard(model.getGameLogic().serializeBoard()));
        }
        ui.onConnectionStateChanged(true);
        updateNetworkUI();
    }

    @Override
    public void onDisconnected() {
        ui.onConnectionStateChanged(false);
        updateNetworkUI();
    }

    // --- ChessDelegate ---

    @Override
    public Piece pieceAt(int col, int row) {
        return model.pieceAt(col, row);
    }

    @Override
    public Boolean movePiece(int fC, int fR, int tC, int tR) {
        boolean moved = model.movePiece(fC, fR, tC, tR);
        if (moved) {
            if (!isNetworkMove) {
                transport.send(ChessProtocol.formatMove(fC, fR, tC, tR));
            }
            notifyUI();
        }
        return moved;
    }

    @Override public Boolean blackPointOfView() { return model.isBlackPointOfView(); }
    @Override public Boolean autoRotate() { return model.isAutoRotate(); }
    @Override public void setOrientation(boolean o) { model.setBlackPointOfView(o); }
    @Override public ArrayList<Movement> getLegalMoves() { return model.getGameLogic().getLegalMoves(); }
    
    @Override public boolean isWhiteTurn() { return model.getGameLogic().toccaAlBianco(); }
    @Override public int getMaterialCount() { return model.getGameLogic().objectiveFunction(); }
    @Override public String getNetworkStatus() { return transport.getState().toString(); }
    @Override public boolean isConnected() { return transport.isConnected(); }
}
