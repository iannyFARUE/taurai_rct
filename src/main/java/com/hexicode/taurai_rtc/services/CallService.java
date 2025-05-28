package com.hexicode.taurai_rtc.services;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import com.hexicode.taurai_rtc.domain.CallSession;

@Service
public class CallService {
    
    private final Map<String, CallSession> activeCalls = new ConcurrentHashMap<>();
    
    public CallSession initiateCall(String fromUserId, String toUserId) {
        String callId = UUID.randomUUID().toString();
        CallSession callSession = new CallSession(callId, fromUserId, toUserId, "INITIATED");
        activeCalls.put(callId, callSession);
        return callSession;
    }
    
    public List<CallSession> getActiveCallsForUser(String userId) {
        return activeCalls.values().stream()
                .filter(call -> call.getFromUserId().equals(userId) || call.getToUserId().equals(userId))
                .collect(Collectors.toList());
    }
    
    public void endCall(String callId) {
        CallSession call = activeCalls.get(callId);
        if (call != null) {
            call.setStatus("ENDED");
            activeCalls.remove(callId);
        }
    }
}