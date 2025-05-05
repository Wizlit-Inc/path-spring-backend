package com.wizlit.path.service;

import com.wizlit.path.model.domain.ProjectDto;
import reactor.core.publisher.Mono;

public interface ProjectService {
    Mono<ProjectDto> getProjectById(Long projectId);
} 