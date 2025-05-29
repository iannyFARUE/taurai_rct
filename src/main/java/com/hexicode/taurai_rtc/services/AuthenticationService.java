package com.hexicode.taurai_rtc.services;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.hexicode.taurai_rtc.domain.AuthenticationRequest;
import com.hexicode.taurai_rtc.domain.AuthenticationResponse;
import com.hexicode.taurai_rtc.domain.RegisterRequest;
import com.hexicode.taurai_rtc.domain.TokenValidationRequest;
import com.hexicode.taurai_rtc.domain.UserInfo;
import com.hexicode.taurai_rtc.entity.Role;
import com.hexicode.taurai_rtc.entity.Token;
import com.hexicode.taurai_rtc.entity.TokenType;
import com.hexicode.taurai_rtc.entity.User;
import com.hexicode.taurai_rtc.repository.TokenRepository;
import com.hexicode.taurai_rtc.repository.UserRepository;
import com.hexicode.taurai_rtc.security.JwtService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
  private final UserRepository repository;
  private final TokenRepository tokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final AuthenticationManager authenticationManager;

  public AuthenticationResponse register(RegisterRequest request) {
    var user = User.builder()
        .firstname(request.getFirstname())
        .lastname(request.getLastname())
        .email(request.getEmail())
        .password(passwordEncoder.encode(request.getPassword()))
        .role(Role.ADMIN)
        .userId(UUID.randomUUID().toString())
        .build();
    var savedUser = repository.save(user);
    var jwtToken = jwtService.generateToken(user);
    var refreshToken = jwtService.generateRefreshToken(user);
    saveUserToken(savedUser, jwtToken);
return buildAuthenticationResponse(jwtToken,refreshToken,"Registration sucessful",savedUser);
  }

  private AuthenticationResponse buildAuthenticationResponse(String token,String message,String refreshToken, User user){
    return AuthenticationResponse.builder()
        .accessToken(token)
            .refreshToken(refreshToken)
            .message(message)
            .userInfo(new UserInfo(user.getUserId(), user.getUsername(), user.getFirstname()+" "+user.getLastname(), user.getEmail()))
        .build();
  }

  public AuthenticationResponse authenticate(AuthenticationRequest request) {
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
            request.getEmail(),
            request.getPassword()
        )
    );
    var user = repository.findByEmail(request.getEmail())
        .orElseThrow();
    var jwtToken = jwtService.generateToken(user);
    var refreshToken = jwtService.generateRefreshToken(user);
    revokeAllUserTokens(user);
    saveUserToken(user, jwtToken);
return buildAuthenticationResponse(jwtToken,refreshToken,"Login sucessful",user);

  }

  private void saveUserToken(User user, String jwtToken) {
    var token = Token.builder()
        .user(user)
        .token(jwtToken)
        .tokenType(TokenType.BEARER)
        .expired(false)
        .revoked(false)
        .build();
    tokenRepository.save(token);
  }

  private void revokeAllUserTokens(User user) {
    var validUserTokens = tokenRepository.findAllValidTokenByUser(user.getId());
    if (validUserTokens.isEmpty())
      return;
    validUserTokens.forEach(token -> {
      token.setExpired(true);
      token.setRevoked(true);
    });
    tokenRepository.saveAll(validUserTokens);
  }

  public void refreshToken(
          HttpServletRequest request,
          HttpServletResponse response
  ) throws IOException {
    final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
    final String refreshToken;
    final String userEmail;
    if (authHeader == null ||!authHeader.startsWith("Bearer ")) {
      return;
    }
    refreshToken = authHeader.substring(7);
    userEmail = jwtService.extractUsername(refreshToken);
    if (userEmail != null) {
      var user = this.repository.findByEmail(userEmail)
              .orElseThrow();
      if (jwtService.isTokenValid(refreshToken, user)) {
        var accessToken = jwtService.generateToken(user);
        revokeAllUserTokens(user);
        saveUserToken(user, accessToken);
        var authResponse = buildAuthenticationResponse(accessToken,"Token refreshed sucessfully",refreshToken,user);

        new ObjectMapper().writeValue(response.getOutputStream(), authResponse);
      }
    }
  }

  public AuthenticationResponse validateToken(TokenValidationRequest request) {
          try {
            if (!jwtService.isTokenExpired(request.token())) {
                String username = jwtService.extractUsername(request.token());
                User user = repository.findByEmail(username).orElseThrow(()->new UsernameNotFoundException("User not found"));
                return buildAuthenticationResponse(request.token(),"Token is Valid", null, user);
            } else {
                 return null;
            }
        } catch (Exception e) {
                 return null;
        }
  }
}
