package com.api.service.interfaces;

import com.api.dto.AuthResponse;
import com.api.dto.LoginRequest;
import com.api.dto.RefreshResponse;
import com.api.dto.RegisterRequest;
import com.api.dto.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request, HttpServletResponse response);
    AuthResponse login(LoginRequest request, HttpServletResponse response);
    RefreshResponse refresh(HttpServletRequest request, HttpServletResponse response);
    void logout(HttpServletRequest request, HttpServletResponse response);
    UserResponse getCurrentUser(String email);
}
