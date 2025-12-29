package com.mopl.api.domain.notification.service;

import com.mopl.api.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    // TODO 필요한 종속성
    private final NotificationRepository notificationRepository;

    public void tempFunction() {
        // TODO Service 구현 내용.....
    }
}
