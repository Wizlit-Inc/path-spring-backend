package com.wizlit.path.controller;

import com.wizlit.path.model.*;
import com.wizlit.path.model.response.FinalResponse;
import com.wizlit.path.service.PointService;
import com.wizlit.path.service.ProjectService;
import com.wizlit.path.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Instant;

@RestController
@AllArgsConstructor
@RequestMapping("/api/project")
@Tag(name = "Project", description = "Project management APIs")
public class ProjectController {

    /**
     * Controller 규칙:
     * 1. service 만 호출
     */

    private final PointService pointService;
    private final ProjectService projectService;

    /**
     * Retrieves all points and their associated edges from the system.
     * If no points are available, it returns a ResponseEntity with a no-content status.
     * In case of an error during the process, it returns an internal server error response.
     *
     * @return a Mono containing a ResponseEntity with an OutputPathDto object that includes all points and edges,
     *         or appropriate response statuses (e.g., no content or internal server error).
     */
    @Operation(
        summary = "Get project details",
        description = "Retrieves details of a specific project including its points and memos"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved project details"),
        @ApiResponse(responseCode = "400", description = "Invalid input parameters (ErrorCode: NULL_INPUT, EMPTY)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access (ErrorCode: INVALID_TOKEN, EXPIRED_TOKEN)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required (ErrorCode: INACCESSIBLE_USER)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Project not found (ErrorCode: PROJECT_NOT_FOUND)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error (ErrorCode: INTERNAL_SERVER, UNKNOWN)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{projectId}")
    public Mono<ResponseEntity<ResponseWithChange<FinalResponse>>> getProject(
        @PathVariable Long projectId,
        @RequestParam(required = false) Long lastFetchTimestamp
    ) {
        Instant updatedAfter = lastFetchTimestamp != null ? Instant.ofEpochMilli(lastFetchTimestamp) : null;
        
        return projectService.getProjectById(projectId)
            .flatMap(project -> pointService.listPointsByIds(project.getAllPointIds(), updatedAfter)
                .collectList()
                .map(points -> new FinalResponse().forGetProject(project.getProjectId(), project, points)))
            .switchIfEmpty(Mono.just(new FinalResponse()))
            .map(ResponseWithChange::new)
            .map(response -> response.toResponseEntity(HttpStatus.OK));
    }

}
