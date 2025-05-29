package com.hexicode.taurai_rtc.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hexicode.taurai_rtc.security.JwtService;
import com.hexicode.taurai_rtc.services.UserService;

import lombok.extern.slf4j.Slf4j;

import com.hexicode.taurai_rtc.domain.UserInfo;
import com.hexicode.taurai_rtc.entity.User;

@Component
@Slf4j
public class CallSignalingHandler extends TextWebSocketHandler {
    
    
    private final JwtService jwtService;
    
    private final UserService userService;
    
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> userSessions = new ConcurrentHashMap<>();
    private final Map<String, UserInfo> connectedUsers = new ConcurrentHashMap<>();
    
    public CallSignalingHandler(JwtService jwtService, UserService userService){
        this.jwtService = jwtService;
        this.userService = userService;
    }
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("Inside connection",session);
        try {
            // Extract JWT token from query parameters
            String query = session.getUri().getQuery();
            String token = extractTokenFromQuery(query);
            
            if (token == null || jwtService.isTokenExpired(token)) {
                session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Invalid or missing authentication token"));
                return;
            }
            
            // Get user info from token
            String username = jwtService.extractUsername(token);
            String userId = jwtService.getUserIdFromToken(token);
            User user = userService.findByUsername(username);
            
            if (user == null || !user.isEnabled()) {
                session.close(CloseStatus.NOT_ACCEPTABLE.withReason("User not found or inactive"));
                return;
            }
            
            // Store user info in session
            session.getAttributes().put("userId", userId);
            session.getAttributes().put("username", username);
            session.getAttributes().put("token", token);
            
            // Add to tracking maps
            sessions.put(session.getId(), session);
            
            // Check if user is already connected (prevent multiple sessions)
            if (userSessions.containsKey(userId)) {
                // Optionally close previous session or reject new connection
                String oldSessionId = userSessions.get(userId);
                WebSocketSession oldSession = sessions.get(oldSessionId);
                if (oldSession != null && oldSession.isOpen()) {
                    oldSession.close(CloseStatus.NORMAL.withReason("New session established"));
                    sessions.remove(oldSessionId);
                }
            }
            
            userSessions.put(userId, session.getId());
            connectedUsers.put(userId, new UserInfo(userId, username, user.getFirstname(), user.getEmail()));
            
            System.out.println("Authenticated user connected: " + username + " (ID: " + userId + ")");
            
            // Send connection confirmation with user info
            ObjectNode response = createMessage("connection-established", "system");
            response.put("userId", userId);
            response.put("username", username);
            response.put("fullName", user.getFirstname());
            session.sendMessage(new TextMessage(response.toString()));
            
            // Broadcast updated user list to all connected clients
            broadcastConnectedUsers();
            
        } catch (Exception e) {
            System.err.println("Error during WebSocket authentication: " + e.getMessage());
            session.close(CloseStatus.SERVER_ERROR.withReason("Authentication error"));
        }
    }

        private String extractTokenFromQuery(String query) {
        if (query != null && query.contains("token=")) {
            return query.split("token=")[1].split("&")[0];
        }
        return null;
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Verify session is still authenticated
        String token = (String) session.getAttributes().get("token");
        if (token == null || jwtService.isTokenExpired(token)) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Token expired"));
            return;
        }
        
        ObjectMapper mapper = new ObjectMapper();
        JsonNode messageNode = mapper.readTree(message.getPayload());
        
        String type = messageNode.get("type").asText();
        String fromUserId = (String) session.getAttributes().get("userId");
        String fromUsername = (String) session.getAttributes().get("username");
        
        System.out.println("Received message type: " + type + " from user: " + fromUsername + " (" + fromUserId + ")");
      
        
        switch (type) {
            case "call-offer":
                    JsonNode targetUserIdNode = messageNode.get("targetUserId");
                    if (targetUserIdNode == null) {
                        sendErrorToSession(session, "Missing targetUserId in call-offer");
                        return;
                    }
                    String targetUserId = targetUserIdNode.asText();
                handleCallOffer(messageNode, fromUserId, fromUsername, targetUserId, session);
                break;
            case "call-answer":
                JsonNode callerUserIdNode = messageNode.get("targetUserId");
                if (callerUserIdNode == null) {
                    sendErrorToSession(session, "Missing targetUserId in call-answer");
                    return;
                }
                String callerUserId = callerUserIdNode.asText();
                handleCallAnswer(messageNode, fromUserId, fromUsername, callerUserId);
                break;
            case "ice-candidate":
                JsonNode candidateTargetUserIdNode = messageNode.get("targetUserId");
                if (candidateTargetUserIdNode == null) {
                    System.err.println("Missing targetUserId in ice-candidate message from " + fromUsername);
                    sendErrorToSession(session, "Missing targetUserId in ice-candidate");
                    return;
                }
                String candidateTargetUserId = candidateTargetUserIdNode.asText();
                System.out.println("ICE candidate directed to: " + candidateTargetUserId);
                handleIceCandidate(messageNode, fromUserId, candidateTargetUserId);
                break;
            case "call-end":
                JsonNode endTargetUserIdNode = messageNode.get("targetUserId");
                if (endTargetUserIdNode == null) {
                    sendErrorToSession(session, "Missing targetUserId in call-end");
                    return;
                }
                String endTargetUserId = endTargetUserIdNode.asText();
                handleCallEnd(fromUserId, endTargetUserId);
                break;
            case "get-online-users":
                sendOnlineUsers(session);
                break;
        }
    }
    
    private void handleCallOffer(JsonNode message, String fromUserId, String fromUsername, String targetUserId, WebSocketSession callerSession) throws Exception {
        System.out.println("Forwarding call offer from " + fromUsername + " to " + targetUserId);
        
        String targetSessionId = userSessions.get(targetUserId);
        if (targetSessionId != null) {
            WebSocketSession targetSession = sessions.get(targetSessionId);
            if (targetSession != null && targetSession.isOpen()) {
                ObjectNode response = createMessage("call-offer", fromUserId);
                response.set("offer", message.get("offer"));
                response.put("fromUserId", fromUserId);
                response.put("fromUsername", fromUsername);
                
                // Add caller's full info
                UserInfo callerInfo = connectedUsers.get(fromUserId);
                if (callerInfo != null) {
                    response.put("callerFullName", callerInfo.getFullName());
                }
                
                targetSession.sendMessage(new TextMessage(response.toString()));
                System.out.println("Call offer sent successfully");
            } else {
                sendErrorToSession(callerSession, "User is not available");
            }
        } else {
            sendErrorToSession(callerSession, "User is not online");
        }
    }

    private void sendErrorToSession(WebSocketSession session, String errorMessage) throws Exception {
        if (session != null && session.isOpen()) {
            ObjectNode errorResponse = createMessage("call-error", "system");
            errorResponse.put("message", errorMessage);
            session.sendMessage(new TextMessage(errorResponse.toString()));
        }
    }

        private void broadcastConnectedUsers() throws Exception {
        ObjectNode message = createMessage("online-users", "system");
        ArrayNode usersArray = message.putArray("users");
        
        for (UserInfo userInfo : connectedUsers.values()) {
            ObjectNode userNode = usersArray.addObject();
            userNode.put("userId", userInfo.getUserId());
            userNode.put("username", userInfo.getUsername());
            userNode.put("fullName", userInfo.getFullName());
        }
        
        String messageStr = message.toString();
        for (WebSocketSession session : sessions.values()) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(messageStr));
            }
        }
    }

        private void sendOnlineUsers(WebSocketSession session) throws Exception {
        ObjectNode message = createMessage("online-users", "system");
        ArrayNode usersArray = message.putArray("users");
        
        String currentUserId = (String) session.getAttributes().get("userId");
        
        for (UserInfo userInfo : connectedUsers.values()) {
            // Don't include the requesting user in the list
            if (!userInfo.getUserId().equals(currentUserId)) {
                ObjectNode userNode = usersArray.addObject();
                userNode.put("userId", userInfo.getUserId());
                userNode.put("username", userInfo.getUsername());
                userNode.put("fullName", userInfo.getFullName());
            }
        }
        
        session.sendMessage(new TextMessage(message.toString()));
    }
    
    private void handleCallAnswer(JsonNode message, String fromUserId,String fromFullname, String targetUserId) throws Exception {
        String targetSessionId = userSessions.get(targetUserId);
        if (targetSessionId != null) {
            WebSocketSession targetSession = sessions.get(targetSessionId);
            if (targetSession != null && targetSession.isOpen()) {
                ObjectNode response = createMessage("call-answer", fromUserId);
                response.set("answer", message.get("answer"));
                response.put("fromUserName",fromFullname);
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
    
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String userId = (String) session.getAttributes().get("userId");
        String username = (String) session.getAttributes().get("username");
        
        if (userId != null) {
            sessions.remove(session.getId());
            userSessions.remove(userId);
            connectedUsers.remove(userId);
            
            System.out.println("User disconnected: " + username + " (" + userId + ")");
            
            // Broadcast updated user list
            broadcastConnectedUsers();
        }
    }
}

