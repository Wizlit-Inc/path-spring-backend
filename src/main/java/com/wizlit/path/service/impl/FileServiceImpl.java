package com.wizlit.path.service.impl;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.wizlit.path.model.domain.UserDto;
import com.wizlit.path.service.FileService;

import reactor.core.publisher.Mono;

public class FileServiceImpl implements FileService {

    /**
     * Service 규칙:
     * 1. repository 직접 호출 X (helper 를 통해서만 호출 - 동일한 helper 가 다른 곳에서도 쓰여도 됨)
     */
    
    @Transactional
    @Override
    public Mono<String> uploadFile(MultipartFile file, UserDto user) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'uploadFile'");
    }

}
