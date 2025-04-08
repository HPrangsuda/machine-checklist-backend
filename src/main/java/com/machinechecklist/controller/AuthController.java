package com.machinechecklist.controller;

import com.machinechecklist.constant.Constant;
import com.machinechecklist.model.LoginRequest;
import com.machinechecklist.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final Constant constant;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/encode")
    public String encodePassword(@RequestBody String password) {
        return passwordEncoder.encode(password);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        return ResponseEntity.ok(authService.signin(loginRequest, response));
    }


}