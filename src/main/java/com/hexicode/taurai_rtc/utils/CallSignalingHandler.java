package com.hexicode.taurai_rtc.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Component
public class CallSignalingHandler extends TextWebSocketHandler {
    
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> userSessions = new ConcurrentHashMap<>();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId = getUserIdFromSession(session);
        sessions.put(session.getId(), session);
        userSessions.put(userId, session.getId());
        
        System.out.println("User " + userId + " connected");
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode messageNode = mapper.readTree(message.getPayload());
        
        String type = messageNode.get("type").asText();
        String targetUserId = messageNode.get("targetUserId").asText();
        String fromUserId = getUserIdFromSession(session);
        
        switch (type) {
            case "call-offer":
                handleCallOffer(messageNode, fromUserId, targetUserId);
                break;
            case "call-answer":
                handleCallAnswer(messageNode, fromUserId, targetUserId);
                break;
            case "ice-candidate":
                handleIceCandidate(messageNode, fromUserId, targetUserId);
                break;
            case "call-end":
                handleCallEnd(fromUserId, targetUserId);
                break;
        }
    }
    
    private void handleCallOffer(JsonNode message, String fromUserId, String targetUserId) throws Exception {
        String targetSessionId = userSessions.get(targetUserId);
        if (targetSessionId != null) {
            WebSocketSession targetSession = sessions.get(targetSessionId);
            if (targetSession != null && targetSession.isOpen()) {
                ObjectNode response = createMessage("call-offer", fromUserId);
                response.set("offer", message.get("offer"));
                targetSession.sendMessage(new TextMessage(response.toString()));
            }
        }
    }
    
    private void handleCallAnswer(JsonNode message, String fromUserId, String targetUserId) throws Exception {
        String targetSessionId = userSessions.get(targetUserId);
        if (targetSessionId != null) {
            WebSocketSession targetSession = sessions.get(targetSessionId);
            if (targetSession != null && targetSession.isOpen()) {
                ObjectNode response = createMessage("call-answer", fromUserId);
                response.set("answer", message.get("answer"));
                targetSession.sendMessage(new TextMessage(response.toString()));
            }
        }
    }
    
    private void handleIceCandidate(JsonNode message, String fromUserId, String targetUserId) throws Exception {
        String targetSessionId = userSessions.get(targetUserId);
        if (targetSessionId != null) {
            WebSocketSession targetSession = sessions.get(targetSessionId);
            if (targetSession != null && targetSession.isOpen()) {
                ObjectNode response = createMessage("ice-candidate", fromUserId);
                response.set("candidate", message.get("candidate"));
                targetSession.sendMessage(new TextMessage(response.toString()));
            }
        }
    }
    
    private void handleCallEnd(String fromUserId, String targetUserId) throws Exception {
        String targetSessionId = userSessions.get(targetUserId);
        if (targetSessionId != null) {
            WebSocketSession targetSession = sessions.get(targetSessionId);
            if (targetSession != null && targetSession.isOpen()) {
                ObjectNode response = createMessage("call-end", fromUserId);
                targetSession.sendMessage(new TextMessage(response.toString()));
            }
        }
    }
    
    private ObjectNode createMessage(String type, String fromUserId) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode message = mapper.createObjectNode();
        message.put("type", type);
        message.put("fromUserId", fromUserId);
        message.put("timestamp", System.currentTimeMillis());
        return message;
    }
    
    private String getUserIdFromSession(WebSocketSession session) {
        // Extract user ID from session attributes or JWT token
        // This is a simplified version - implement proper authentication
        return session.getAttributes().get("userId").toString();
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String userId = getUserIdFromSession(session);
        sessions.remove(session.getId());
        userSessions.remove(userId);
        
        System.out.println("User " + userId + " disconnected");
    }
}

