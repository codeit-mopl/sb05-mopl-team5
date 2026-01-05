package com.mopl.api.domain.user.service;

import com.mopl.api.domain.user.dto.request.JwtInformation;
import com.mopl.api.domain.user.dto.request.ResetPasswordRequest;

public interface AuthService {

    JwtInformation refreshToken(String refreshToken);
    void resetPassword(ResetPasswordRequest request);



}
