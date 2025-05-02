package com.machinechecklist.service;

import com.machinechecklist.constant.Constant;
import com.machinechecklist.model.LoginRequest;
import com.machinechecklist.model.User;
import com.machinechecklist.repo.UserRepo;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepo userRepo;
    private final RefreshTokenService refreshTokenService;
    private final Constant constant;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    public ResponseEntity<?> signIn(LoginRequest loginRequest, HttpServletResponse httpResponse) {
        Map<String, Object> response = new HashMap<>();
        try{
            System.out.println(loginRequest.getUsername());
            if(!userRepo.existsByUsername(loginRequest.getUsername())) {
                response.put("msg", "User with username " + loginRequest.getUsername() + " not exists");
                response.put("code", 1301);
            }
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );
            User getUSer = userRepo.findByUsername(loginRequest.getUsername())
                    .orElseThrow(() -> new RuntimeException("User with username '" + loginRequest.getUsername() + "' not found"));
            if(authentication.isAuthenticated()){
                String token = Jwts.builder()
                        .setSubject(getUSer.getUsername())
                        .setIssuedAt(Date.from(Instant.now()))
                        .setExpiration(Date.from(Instant.now().plusMillis(constant.getJwtSecretExp())))
                        .signWith(SignatureAlgorithm.HS512, constant.getJwtSecret())
                        .compact();
                String refreshToken = refreshTokenService.getRefreshToken(getUSer.getId());
                setAccessTokenCookie(token, httpResponse);
                setAccessRefreshCookie(refreshToken, httpResponse);
                setUsername(getUSer.getUsername(), httpResponse);
                response.put("code", 200);
                response.put("accessToken", token);
                response.put("refreshToken", refreshToken);
                response.put("username", loginRequest.getUsername());
                response.put("fullName", getUSer.getFirstName()+" "+getUSer.getLastName() );
            }else{
                response.put("msg", "User authentication failed");
                response.put("code", 1302);
            }

        }catch (Exception e){
            response.put("msg", e.getMessage());
            response.put("code", 1303);
            logger.error(e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "admin";
        String encodedPassword = encoder.encode(rawPassword);
        System.out.println("Encoded password for 'admin': " + encodedPassword);
    }

    private void setAccessTokenCookie(String accessToken, HttpServletResponse response) {
        Cookie cookie = new Cookie("authorise", accessToken);
        cookie.setHttpOnly(false); // ควรพิจารณาใช้ true เพื่อความปลอดภัย
        cookie.setSecure(false); // ใช้กับ HTTPS เท่านั้น
        cookie.setPath("/");
        cookie.setValue(accessToken);
        cookie.setMaxAge(60 * constant.getJwtSecretExpMin());
        response.addCookie(cookie);
    }

    private void setAccessRefreshCookie(String sessionId, HttpServletResponse response) {
        Cookie cookie = new Cookie("sessionId", sessionId);
        cookie.setHttpOnly(false); // ควรพิจารณาใช้ true เพื่อความปลอดภัย
        cookie.setSecure(false); // ใช้กับ HTTPS เท่านั้น
        cookie.setPath("/");
        cookie.setMaxAge(60 * constant.getJwtSecretExpMin());
        response.addCookie(cookie);
    }

    private void setUsername(String username, HttpServletResponse response) {
        Cookie cookie = new Cookie("username", username);
        cookie.setHttpOnly(false); // ควรพิจารณาใช้ true เพื่อความปลอดภัย
        cookie.setSecure(false); // ใช้กับ HTTPS เท่านั้น
        cookie.setPath("/");
        cookie.setMaxAge(60 * constant.getJwtSecretExpMin());
        response.addCookie(cookie);
    }
}

