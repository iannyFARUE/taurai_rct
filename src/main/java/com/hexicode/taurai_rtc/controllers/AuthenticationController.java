package com.hexicode.taurai_rtc.controllers;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hexicode.taurai_rtc.domain.AuthenticationRequest;
import com.hexicode.taurai_rtc.domain.AuthenticationResponse;
import com.hexicode.taurai_rtc.domain.RegisterRequest;
import com.hexicode.taurai_rtc.domain.TokenValidationRequest;
import com.hexicode.taurai_rtc.domain.UserInfo;
import com.hexicode.taurai_rtc.security.JwtService;
import com.hexicode.taurai_rtc.services.AuthenticationService;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthenticationController {

  private final AuthenticationService service;

  @PostMapping("/register")
  public ResponseEntity<AuthenticationResponse> register(
      @RequestBody RegisterRequest request
  ) {
    return ResponseEntity.ok(service.register(request));
  }
  @PostMapping("/authenticate")
  public ResponseEntity<AuthenticationResponse> authenticate(
      @RequestBody AuthenticationRequest request
  ) {
    return ResponseEntity.ok(service.authenticate(request));
  }

  @PostMapping("/refresh-token")
  public void refreshToken(
      HttpServletRequest request,
      HttpServletResponse response
  ) throws IOException {
    service.refreshToken(request, response);
  }

      @PostMapping("/validate-token")
    public ResponseEntity<?> validateToken(@RequestBody TokenValidationRequest request) {
        AuthenticationResponse authenticationResponse = service.validateToken(request);
        if(authenticationResponse == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthenticationResponse(null,null,"Invalid or expired token", null)); 
        }else{
            return ResponseEntity.ok(authenticationResponse);
        }
    }

}