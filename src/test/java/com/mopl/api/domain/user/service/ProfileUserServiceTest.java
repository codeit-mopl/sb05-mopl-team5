package com.mopl.api.domain.user.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mopl.api.domain.user.dto.request.UserUpdateRequest;
import com.mopl.api.domain.user.entity.User;
import com.mopl.api.domain.user.mapper.UserMapper;
import com.mopl.api.domain.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
public class ProfileUserServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    UserMapper userMapper;

    @Mock
    ProfileImageStorageService profileImageStorageService;

    @InjectMocks
    UserServiceImpl userService;


    @Test
    public void profileChange_rejectsWhenNotSelf() {
        UUID userId = UUID.randomUUID();
        UUID requester = UUID.randomUUID();

        assertThatThrownBy(() ->
            userService.profileChange(userId, requester, new UserUpdateRequest("newName"), null)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void profileChange_updatesNameAndImage() {
        UUID userId = UUID.randomUUID();
        UUID requester = userId;

        User user = mock(User.class);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        MultipartFile image = mock(MultipartFile.class);
        when(image.isEmpty()).thenReturn(false);
        when(profileImageStorageService.store(eq(userId), eq(image))).thenReturn("/profile-images/a.png");

        userService.profileChange(userId, requester, new UserUpdateRequest("alice"), image);

        verify(user).updateName("alice");
        verify(user).updateProfileImageUrl("/profile-images/a.png");
    }

    @Test
    public void profileChange_updatesOnlyNameWhenNoImage() {
        UUID userId = UUID.randomUUID();
        User user = mock(User.class);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.profileChange(userId, userId, new UserUpdateRequest("bob"), null);

        verify(user).updateName("bob");
        verify(user, never()).updateProfileImageUrl(any());
    }
}
