package com.wizlit.path.service.impl;

import com.wizlit.path.exception.ApiException;
import com.wizlit.path.exception.ErrorCode;
import com.wizlit.path.service.ProjectService;
import com.wizlit.path.service.manager.EdgeManager;
import com.wizlit.path.service.manager.ProjectManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.wizlit.path.model.domain.ProjectDto;
import com.wizlit.path.model.domain.EdgeDto;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    /**
     * Service 규칙:
     * 1. repository 직접 호출 X (helper 를 통해서만 호출 - 동일한 helper 가 다른 곳에서도 쓰여도 됨)
     */

    private final ProjectManager projectManager;
    private final EdgeManager edgeManager;

    @Override
    public Mono<ProjectDto> getProjectById(Long projectId) {
        if (projectId == null) {
            return Mono.error(new ApiException(ErrorCode.NULL_INPUT, "projectId"));
        }
        
        return projectManager.getFullProjectById(projectId)
                .flatMap(projectDto -> edgeManager.findEdgesByPointIds(projectDto.getPointIds())
                    .map(EdgeDto::fromEdge)
                    .collectList()
                    .map(edges -> projectDto.append(projectDto.getPointIds(), edges)));
    }
}
