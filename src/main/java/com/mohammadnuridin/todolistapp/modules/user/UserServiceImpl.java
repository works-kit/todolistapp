package com.mohammadnuridin.todolistapp.modules.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mohammadnuridin.todolistapp.core.exception.AppException;
import com.mohammadnuridin.todolistapp.core.exception.ErrorCode;
import com.mohammadnuridin.todolistapp.core.util.SecurityUtil;
import com.mohammadnuridin.todolistapp.core.util.ValidationService;
import com.mohammadnuridin.todolistapp.modules.auth.dto.ChangePasswordRequest;
import com.mohammadnuridin.todolistapp.modules.auth.service.TokenBlacklistService;
import com.mohammadnuridin.todolistapp.modules.user.domain.User;
import com.mohammadnuridin.todolistapp.modules.user.dto.UpdateProfileRequest;
import com.mohammadnuridin.todolistapp.modules.user.dto.UserResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ValidationService validationService;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getProfile() {
        User user = getCurrentUserEntity();
        return UserResponse.from(user);
    }

    @Override
    @Transactional
    public UserResponse updateProfile(UpdateProfileRequest request) {

        if (request.isEmpty()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR);
        }

        validationService.validate(request);

        User user = getCurrentUserEntity();

        if (request.name() != null && !request.name().isBlank()) {
            user.setName(request.name().trim());
        }

        if (request.email() != null && !request.email().isBlank()) {
            String newEmail = request.email().trim().toLowerCase();

            if (!newEmail.equals(user.getEmail())) {
                if (userRepository.existsByEmail(newEmail)) {
                    throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
                }
                user.setEmail(newEmail);
            }
        }

        User saved = userRepository.save(user);
        log.info("Profile updated for user: {}", user.getId());
        return UserResponse.from(saved);
    }

    @Override
    public void deleteAccount() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteAccount'");
    }

    private User getCurrentUserEntity() {
        String currentUserId = SecurityUtil.getCurrentUserId();
        return userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

    }

}
