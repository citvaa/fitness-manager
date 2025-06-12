package com.example.demo.websocket;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class StompWebSocketClient {
    public static void main(String[] args) throws InterruptedException {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new StringMessageConverter());

        StompSessionHandler handler = new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(@NotNull StompSession session, @NotNull StompHeaders headers) {
                log.info("✅ STOMP Connected!");
                session.subscribe("/topic/trainer1", this);
                session.subscribe("/topic/client1", this);
                session.send("/app/sendNotification", "Test push notification!");
            }

            @Override
            public void handleFrame(@NotNull StompHeaders headers, Object payload) {
                log.info("📩 Received frame!");
                log.debug("Headers: {}", headers);
                log.debug("Payload: {}", payload);
            }

            @Override
            public void handleTransportError(@NotNull StompSession session, @NotNull Throwable exception) {
                log.error("❌ Transport Error: {}", exception.getMessage());
            }
        };

        CompletableFuture<StompSession> sessionFuture = stompClient.connectAsync("ws://localhost:8088/ws", handler);
        sessionFuture.thenAccept(session -> {
            log.info("🔥 Connection established!");
            session.subscribe("/topic/trainer1", handler);
            session.send("/app/sendNotification", "Test push notification!");
        });

        Thread.sleep(60000);
    }
}