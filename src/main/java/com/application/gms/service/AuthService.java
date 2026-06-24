package com.application.gms.service;

import com.application.gms.dto.TokenPair;
import com.application.gms.dto.request.LoginRequest;
import com.application.gms.dto.request.RegistrationRequest;
import com.application.gms.dto.response.AuthenticationResponse;
import com.application.gms.model.User;
import com.application.gms.repository.TokenRepository;
import com.application.gms.repository.UserRepository;
import com.application.gms.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    @Value("${jwt.accessExpiration}")
    private long accessTokenExpirationMS;

    @Value("${jwt.refreshExpiration}")
    private long refreshTokenExpirationMS;


    public AuthService(AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider, UserDetailsService userDetailsService, TokenRepository tokenRepository, PasswordEncoder passwordEncoder, UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    public AuthenticationResponse register(RegistrationRequest registraion) {

        // First check if the user already exist
        userRepository.findByUserName(registraion.getUsername())
                .ifPresent(user -> {
                    throw new RuntimeException("Username is already in use");
                });

        // Create new user
        User user = new User();
        user.setUserName(registraion.getUsername());
        user.setPasswordHash(passwordEncoder.encode(registraion.getPassword()));
        user.setRoleEnum(registraion.getRoleEnum());

        userRepository.save(user);

        return authenticateUser(registraion.getUsername(), registraion.getPassword());

    }

    public AuthenticationResponse login(LoginRequest loginRequest) {
        return authenticateUser(loginRequest.getUsername(), loginRequest.getPassword());
    }

    private AuthenticationResponse authenticateUser(String username, String password) {
        // Authenticate the user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        username,
                        password
                )
        );

        // Set authentication in security context
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Generate JWT token pair (access + refresh)
        TokenPair tokenPair = jwtTokenProvider.generateTokenPair(authentication);

        // Store token in Redis
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        assert userDetails != null;
        tokenRepository.storeTokens(
                userDetails.getUsername(),
                tokenPair.getAccessToken(),
                tokenPair.getRefreshToken(),
                tokenPair.getAccessTokenExpirationMs(),
                tokenPair.getRefreshTokenExpirationMs()
        );

        return new AuthenticationResponse(
                tokenPair.getAccessToken(),
                tokenPair.getRefreshToken(),
                userDetails.getUsername(),
                userDetails.getAuthorities()
        );
    }

    public void logout() {
        // Get Current authenticated User
        var userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // Remove all tokens for this user
        tokenRepository.removeAllTokens(userDetails.getUsername());
    }

    public ResponseEntity<?> refreshToken(String refreshToken) {
        // Validate the refresh token
        if(!jwtTokenProvider.validateToken(refreshToken)) {
            return ResponseEntity.badRequest()
                    .body("Invalid refresh token");
        }

        // Check if token is blacklisted
        if(tokenRepository.isRefreshTokenBlacklisted(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("ERROR: Refresh token is blacklisted");
        }


        // Extract the username from refresh token
        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);


        // Verify token matches stored token for user
        String storedRefreshToken = tokenRepository.getRefreshToken(username);

        if(storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("ERROR: Invalid refresh token");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        // Create new authentication object
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());


        String newAccessToken = jwtTokenProvider.generateAccessToken(authToken);

        // Update access token in Redis
        tokenRepository.removeAccessToken(username);
        tokenRepository.storeTokens(
                username,
                newAccessToken,
                refreshToken,
                accessTokenExpirationMS,
                refreshTokenExpirationMS
        );

        return ResponseEntity.ok(new AuthenticationResponse(
                newAccessToken,
                refreshToken,
                username,
                userDetails.getAuthorities()
        ));
    }
}
