package com.example.demo.websocket;

import okhttp3.*;
import org.jetbrains.annotations.NotNull;

public class OkHttpWebSocketClient {
    public static void main(String[] args) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url("ws://localhost:8088/ws").build();
        client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
                webSocket.send("{\"destination\": \"/topic/client123\", \"message\": \"Test push notification!\"}");
            }

            @Override
            public void onMessage(@NotNull WebSocket webSocket, @NotNull String message) {
                System.out.println("Received: " + message);
            }
        });
    }
}