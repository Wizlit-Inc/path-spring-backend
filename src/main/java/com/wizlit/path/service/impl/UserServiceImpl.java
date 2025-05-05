package com.wizlit.path.service.impl;

import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import com.wizlit.path.model.domain.UserDto;
import com.wizlit.path.service.UserService;
import com.wizlit.path.service.manager.UserManager;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    /**
     * Service 규칙:
     * 1. repository 직접 호출 X (helper 를 통해서만 호출 - 동일한 helper 가 다른 곳에서도 쓰여도 됨)
     */

    private final UserManager userManager;
    
    @Override
    public Flux<UserDto> listUserByUserIds(List<Long> userIds, Instant updatedAfter) {
        if (userIds == null || userIds.isEmpty()) {
            return Flux.empty();
        }
        return userManager.findByUserIds(userIds, updatedAfter)
            .map(UserDto::from);
    }
    
    @Override
    public Mono<UserDto> getUserByEmail(String email) {
        return userManager.findByEmail(email)
                .map(UserDto::from);
    }

    @Override
    public Mono<UserDto> getUserByEmailAndCreateIfNotExists(String email, String name, String avatar) {
        return userManager.findByEmail(email, false)
                .map(UserDto::from)
                .switchIfEmpty(userManager.createUser(email, name, avatar)
                    .map(UserDto::from));
    }

    @Override
    public Mono<UserDto> updateUser(Long userId, String name, String email) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'updateUser'");
    }

    @Override
    public Mono<Void> updateUserAvatar(Long userId, MultipartFile avatar) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'updateUserAvatar'");
    }

    @Override
    public Mono<Void> deleteUserAvatar(Long userId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteUserAvatar'");
    }
    
}
