package com.hexicode.taurai_rtc.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hexicode.taurai_rtc.domain.CallRequest;
import com.hexicode.taurai_rtc.domain.CallSession;
import com.hexicode.taurai_rtc.services.CallService;

// Call Controller for REST endpoints
@RestController
@RequestMapping("/api/calls")
public class CallController {
    
    @Autowired
    private CallService callService;
    
    @PostMapping("/initiate")
    public ResponseEntity<?> initiateCall(@RequestBody CallRequest request) {
        try {
            CallSession callSession = callService.initiateCall(request.getFromUserId(), request.getToUserId());
            return ResponseEntity.ok(callSession);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to initiate call: " + e.getMessage());
        }
    }
    
    @GetMapping("/active/{userId}")
    public ResponseEntity<?> getActiveCalls(@PathVariable String userId) {
        List<CallSession> activeCalls = callService.getActiveCallsForUser(userId);
        return ResponseEntity.ok(activeCalls);
    }
    
    @PutMapping("/end/{callId}")
    public ResponseEntity<?> endCall(@PathVariable String callId) {
        try {
            callService.endCall(callId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to end call: " + e.getMessage());
        }
    }
}
