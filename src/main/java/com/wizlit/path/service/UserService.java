package com.wizlit.path.service;

import java.time.Instant;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.wizlit.path.model.domain.UserDto;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserService {
    Flux<UserDto> listUserByUserIds(List<Long> userIds, Instant updatedAfter);
    Mono<UserDto> getUserByEmail(String email);
    Mono<UserDto> getUserByEmailAndCreateIfNotExists(String email, String name, String avatar);
    Mono<UserDto> updateUser(Long userId, String name, String email);
    Mono<Void> updateUserAvatar(Long userId, MultipartFile avatar);
    Mono<Void> deleteUserAvatar(Long userId);
}
