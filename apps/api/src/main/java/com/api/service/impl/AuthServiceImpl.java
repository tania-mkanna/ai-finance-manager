package com.api.service.impl;

import com.api.dto.AuthResponse;
import com.api.dto.LoginRequest;
import com.api.dto.RefreshResponse;
import com.api.dto.RegisterRequest;
import com.api.dto.UserResponse;
import com.api.exception.ConflictException;
import com.api.exception.InvalidRequestException;
import com.api.exception.NotFoundException;
import com.api.model.RefreshToken;
import com.api.model.User;
import com.api.repository.RefreshTokenRepository;
import com.api.repository.UserRepository;
import com.api.security.JwtService;
import com.api.service.interfaces.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Currency;
import java.util.Locale;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    public AuthServiceImpl(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletResponse response) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already exists");
        }
        try {
            ZoneId.of(request.timezone());
        } catch (DateTimeException e) {
            throw new InvalidRequestException("Invalid timezone");
        }
        String currency = request.defaultCurrency().toUpperCase(Locale.ROOT);

        try {
            Currency.getInstance(currency);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Invalid currency code");
        }
        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setDefaultCurrency(currency);
        user.setTimezone(request.timezone());

        User savedUser = userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(savedUser.getEmail());
        String refreshToken = jwtService.generateRefreshToken(savedUser.getEmail());
        saveRefreshToken(savedUser, refreshToken, jwtService.getRefreshTokenExpiration());

        setAuthCookies(response, accessToken, refreshToken);

        return new AuthResponse(UserResponse.from(savedUser));
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletResponse response) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.email(),
                            request.password()
                    )
            );
        } catch (BadCredentialsException ex) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String accessToken = jwtService.generateAccessToken(user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());
        saveRefreshToken(user, refreshToken, jwtService.getRefreshTokenExpiration());

        setAuthCookies(response, accessToken, refreshToken);

        return new AuthResponse(UserResponse.from(user));
    }

    @Override
    @Transactional
    public RefreshResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshTokenValue = readCookie(request, "refresh_token");
        if (refreshTokenValue == null || !jwtService.isTokenValid(refreshTokenValue, "refresh")) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        String email = jwtService.extractSubject(refreshTokenValue);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));

        String tokenHash = hashToken(refreshTokenValue);

        RefreshToken currentToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .filter(token -> token.getUser().getId().equals(user.getId()))
                .filter(token -> token.getRevokedAt() == null)
                .filter(token -> token.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new BadCredentialsException("Refresh token is invalid or revoked"));

        currentToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(currentToken);

        String newAccessToken = jwtService.generateAccessToken(email);
        String newRefreshToken = jwtService.generateRefreshToken(email);
        saveRefreshToken(user, newRefreshToken, jwtService.getRefreshTokenExpiration());

        setAuthCookies(response, newAccessToken, newRefreshToken);

        return new RefreshResponse("Token refreshed successfully");
    }

    @Override
    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshTokenValue = readCookie(request, "refresh_token");

        if (refreshTokenValue != null) {
            String tokenHash = hashToken(refreshTokenValue);

            refreshTokenRepository.findByTokenHash(tokenHash)
                    .ifPresent(token -> {
                        token.setRevokedAt(LocalDateTime.now());
                        refreshTokenRepository.save(token);
                    });
        }

        clearAuthCookies(response);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return UserResponse.from(user);
    }

    private void setAuthCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        response.addCookie(createCookie(
                "access_token",
                accessToken,
                (int) (jwtService.getAccessTokenExpiration() / 1000)
        ));
        response.addCookie(createCookie(
                "refresh_token",
                refreshToken,
                (int) (jwtService.getRefreshTokenExpiration() / 1000)
        ));
    }

    private void clearAuthCookies(HttpServletResponse response) {
        response.addCookie(createCookie("access_token", "", 0));
        response.addCookie(createCookie("refresh_token", "", 0));
    }

    private Cookie createCookie(String name, String value, int maxAgeSeconds) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/");
        cookie.setMaxAge(maxAgeSeconds);
        cookie.setAttribute("SameSite", "Lax");
        return cookie;
    }

    private String readCookie(HttpServletRequest request, String cookieName) {
        if (request.getCookies() == null) {
            return null;
        }

        for (Cookie cookie : request.getCookies()) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }

    private void saveRefreshToken(User user, String rawToken, long expirationMs) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hashToken(rawToken));
        refreshToken.setExpiresAt(LocalDateTime.now().plusSeconds(expirationMs / 1000));
        refreshToken.setRevokedAt(null);
        refreshTokenRepository.save(refreshToken);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Unable to hash refresh token", ex);
        }
    }
}
