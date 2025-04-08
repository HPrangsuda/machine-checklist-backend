package com.machinechecklist.constant;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Data
@Component
public class Constant {
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.secrete-exp}")
    private Long jwtSecretExp;

    @Value("${jwt.secrete-exp-min}")
    private int jwtSecretExpMin;
}
