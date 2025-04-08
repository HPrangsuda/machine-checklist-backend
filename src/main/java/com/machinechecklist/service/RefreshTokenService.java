package com.machinechecklist.service;

import com.machinechecklist.constant.Constant;
import com.machinechecklist.model.RefreshToken;
import com.machinechecklist.repo.RefreshTokenRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepo refreshTokenRepo;
    private final Constant constant;


    public String getRefreshToken(Long userId) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(userId);
        refreshToken.setRefreshToken(UUID.randomUUID().toString());
        refreshToken.setExpireAt(Date.from(Instant.now().plusMillis(constant.getJwtSecretExp()*2)));
        RefreshToken getRef = refreshTokenRepo.save(refreshToken);
        return getRef.getRefreshToken();
    }
}
