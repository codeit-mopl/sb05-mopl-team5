package com.mopl.api.domain.user.service;

import com.mopl.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    // TODO 필요한 종속성
    private final UserRepository userRepository;

    public void tempFunction() {
        // TODO Service 구현 내용.....
    }
}
