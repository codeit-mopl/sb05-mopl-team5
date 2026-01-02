package com.mopl.api.domain.user.service;

import com.mopl.api.domain.user.dto.request.JwtInformation;
import com.mopl.api.domain.user.dto.response.JwtDto;

public interface AuthService {

    JwtInformation refreshToken(String refreshToken);



}
