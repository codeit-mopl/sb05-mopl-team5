package com.mopl.api.domain.user.service;

import com.mopl.api.domain.user.dto.response.JwtDto;

public interface AuthService {

    JwtDto refreshToken(String refreshToken);

    JwtDto signIn(String username, String password);


}
