package com.wizlit.path.service.manager;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Component;

import com.wizlit.path.entity.Project;
import com.wizlit.path.entity.ProjectPoint;
import com.wizlit.path.exception.ApiException;
import com.wizlit.path.exception.ErrorCode;
import com.wizlit.path.model.domain.ProjectDto;
import com.wizlit.path.repository.ProjectPointRepository;
import com.wizlit.path.repository.ProjectRepository;
import com.wizlit.path.utils.Validator;

import lombok.RequiredArgsConstructor;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * ProjectManager is a component responsible for managing project-related operations.
 * It handles project retrieval, point management, and project updates in a reactive manner.
 * 
 * This manager follows strict rules for repository usage and error handling:
 * 1. Each repository should be used exclusively within this manager
 * 2. Repository operations should be called exactly once per method
 * 3. All repository operations must include error mapping using Validator
 * 4. No direct calls to other helpers or services are allowed
 * 5. Methods should not return DTOs directly
 * 
 * Manager 규칙:
 * 1. 동일한 repository 가 다른 곳에서도 쓰이면 안됨
 * 2. repository 의 각 기능은 반드시 한 번만 호출
 * 3. repository 기능에는 .onErrorMap(error -> Validator.from(error).toException()) 필수
 * 4. 다른 helper 나 service 호출 금지
 * 5. DTO 반환 금지
 */
@Component
@RequiredArgsConstructor
public class ProjectManager {

    private final ProjectRepository projectRepository;
    private final ProjectPointRepository projectPointRepository;

    /**
     * Checks if a project exists by its ID.
     *
     * @param projectId The ID of the project to check
     * @return A Mono containing true if the project exists, or false if it doesn't
     */
    public Mono<Boolean> isProjectExists(Long projectId, Boolean throwIfFalse) {
        return projectRepository.existsById(projectId)
            .onErrorMap(error -> Validator.from(error)
                .containsAllElseError(
                    new ApiException(ErrorCode.PROJECT_NOT_FOUND, projectId),
                    "foreign", "key", "project"
                )
                .toException())
            .flatMap(exists -> {
                if (exists) {
                    return Mono.just(true);
                } else {
                    return Mono.error(new ApiException(ErrorCode.PROJECT_NOT_FOUND, projectId));
                }
            });
    }

    /**
     * Retrieves a full project by its ID.
     *
     * @param projectId The ID of the project to find
     * @return A Mono containing the found ProjectDto, or an error if not found
     * @throws ApiException with ErrorCode.PROJECT_NOT_FOUND if the project doesn't exist
     */
    public Mono<ProjectDto> getFullProjectById(Long projectId) {
        return projectRepository.findFullProjectsByIds(List.of(projectId), null)
            .next()
            .switchIfEmpty(Mono.error(new ApiException(ErrorCode.PROJECT_NOT_FOUND, projectId)));
    }

    /**
     * Lists all point IDs associated with a project.
     *
     * @param projectId The ID of the project to list points for
     * @return A Flux of point IDs, or an empty Flux if projectId is null
     */
    public Flux<Long> findPointIdsByProjectId(Long projectId) {
        if (projectId == null) {
            return Flux.empty();
        }
        return projectPointRepository.findPointIdsByProjectId(projectId)
                .onErrorMap(error -> Validator.from(error)
                        .toException());
    }

    /**
     * Saves a project to the repository.
     *
     * @param project The project to save
     * @return A Mono containing the saved Project
     */
    public Mono<Project> saveProject(Project project) {
        project.setProjectUpdatedTimestamp(Instant.now());

        return projectRepository.save(project)
                .onErrorMap(error -> Validator.from(error)
                        .containsAllElseError(
                            new ApiException(ErrorCode.PROJECT_NOT_FOUND, project.getProjectId()),
                            "foreign", "key", "project"
                        )
                        .toException());
    }

    /**
     * Updates the project's timestamp to the current time.
     *
     * @param projectId The ID of the project to update
     * @return A Mono containing the updated Project
     */
    public Mono<Project> updateProject(Long projectId) {
        if (projectId == null) {
            return Mono.error(new ApiException(ErrorCode.NULL_INPUT, "projectId"));
        }

        return projectRepository.findById(projectId)
                .onErrorMap(error -> Validator.from(error)
                        .toException())
                .switchIfEmpty(Mono.error(new ApiException(ErrorCode.PROJECT_NOT_FOUND, projectId)))
                .flatMap(existingProject -> saveProject(existingProject));
    }

    /**
     * Adds a point to a project and updates the project's timestamp.
     *
     * @param projectId The ID of the project to add the point to
     * @param pointId The ID of the point to add
     * @return A Mono containing the created ProjectPoint
     */
    public Mono<ProjectPoint> addPointToProject(Long projectId, Long pointId) {
        ProjectPoint newProjectPoint = ProjectPoint.builder()
                .projectId(projectId)
                .pointId(pointId)
                .build();

        return projectPointRepository.save(newProjectPoint)
                .onErrorMap(error -> Validator.from(error)
                        .containsAllElseError(
                            new ApiException(ErrorCode.PROJECT_NOT_FOUND, projectId),
                            "foreign", "key", "project"
                        )
                        .containsAllElseError(
                            new ApiException(ErrorCode.POINT_NOT_FOUND, pointId),
                            "foreign", "key", "point"
                        )
                        .toException())
                .flatMap(projectPoint -> updateProject(projectId)
                        .thenReturn(projectPoint));
    }

}