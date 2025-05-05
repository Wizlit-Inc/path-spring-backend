package com.wizlit.path.service;

import org.springframework.web.multipart.MultipartFile;

import com.wizlit.path.model.domain.UserDto;

import reactor.core.publisher.Mono;

public interface FileService {
    Mono<String> uploadFile(MultipartFile file, UserDto user);
}
