package com.wizlit.path.controller;

import com.wizlit.path.model.*;
import com.wizlit.path.model.domain.EdgeDto;
import com.wizlit.path.model.request.AddPointRequest;
import com.wizlit.path.model.request.UpdatePointRequest;
import com.wizlit.path.model.response.FinalResponse;
import com.wizlit.path.service.PointService;
import com.wizlit.path.service.MemoService;
import com.wizlit.path.service.UserService;
import com.wizlit.path.utils.PrivateAccess;
import com.wizlit.path.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Objects;

import com.wizlit.path.model.domain.PointDto;
import com.wizlit.path.model.domain.MemoDto;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/point")
@Tag(name = "Point", description = "Point management APIs")
public class PointController {

    /**
     * Controller 규칙:
     * 1. service 만 호출
     */

    private final PointService pointService;
    private final MemoService memoService;
    private final UserService userService;

    @Value("${app.googledrive.folderId}")
    private String googleDriveFolderId;

    @Operation(
        summary = "Add a new point",
        description = "Creates a new point in the project with the specified title and connections"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Point created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input parameters (ErrorCode: NULL_INPUT, NULL_POINTS, SAME_POINTS, INVALID_NUMERIC_IDS, EMPTY)",
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
        @ApiResponse(responseCode = "409", description = "Point title already exists (ErrorCode: POINT_NAME_DUPLICATED)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error (ErrorCode: INTERNAL_SERVER, UNKNOWN)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    @PrivateAccess
    public Mono<ResponseEntity<ResponseWithChange<FinalResponse>>> addPoint(
            @RequestAttribute("email") String email,
            @RequestAttribute("name") String name,
            @RequestAttribute("avatar") String avatar,
            @RequestBody AddPointRequest addPointRequest
    ) {
        return userService.getUserByEmailAndCreateIfNotExists(email, name, avatar)
            .flatMap(user -> pointService.createPoint(
                addPointRequest.getProjectId(),
                user,
                addPointRequest.getTitle(),
                addPointRequest.getOrigin(),
                addPointRequest.getDestination()
            ))
            .map(point -> new FinalResponse().forOnlyPoint(point.getPointId(), point))
            .map(ResponseWithChange::new)
            .map(responseWithChange -> responseWithChange.toResponseEntity(HttpStatus.CREATED));
    }

    @Operation(
        summary = "Get point details",
        description = "Retrieves details of a specific point including its memos and contributors"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved point details"),
        @ApiResponse(responseCode = "400", description = "Invalid input parameters (ErrorCode: NULL_INPUT, EMPTY)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access (ErrorCode: INVALID_TOKEN, EXPIRED_TOKEN)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Point not found (ErrorCode: POINT_NOT_FOUND)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error (ErrorCode: INTERNAL_SERVER, UNKNOWN)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{pointId}")
    public Mono<ResponseEntity<ResponseWithChange<FinalResponse>>> getPoint(
        @PathVariable Long pointId,
        @RequestParam(required = false) Long lastFetchTimestamp
    ) {
        Instant updatedAfter = lastFetchTimestamp != null ? Instant.ofEpochMilli(lastFetchTimestamp) : null;
        return Mono.zip(
            pointService.getPoint(pointId, updatedAfter),
            memoService.listMemosByPointId(pointId, updatedAfter).collectList()
        )
        .flatMap(tuple -> {
            PointDto point = tuple.getT1();
            List<MemoDto> memos = tuple.getT2();
            
            List<Long> allUserIds = Stream.concat(
                Stream.empty(),
                // Stream.of(point.getPointCreatedUser()),
                memos.stream()
                    .flatMap(memo -> Stream.concat(
                        Stream.of(memo.getMemoCreatedUser()),
                        memo.getContributorUserIds() != null ? 
                            memo.getContributorUserIds().stream() : 
                            Stream.empty()
                    ))
            ).filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
            
            return userService.listUserByUserIds(allUserIds, updatedAfter)
                .collectList()
                .map(users -> new FinalResponse().forGetPoint(point.getPointId(), point, memos, users));
        })
        .switchIfEmpty(Mono.just(new FinalResponse()))
        .map(ResponseWithChange::new)
        .map(response -> response.toResponseEntity(HttpStatus.OK));
    }
    
    @Operation(
        summary = "Update point title",
        description = "Updates the title of an existing point"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Point updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input parameters (ErrorCode: NULL_INPUT, EMPTY)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access (ErrorCode: INVALID_TOKEN, EXPIRED_TOKEN)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required (ErrorCode: INACCESSIBLE_USER)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Point not found (ErrorCode: POINT_NOT_FOUND)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Point title already exists (ErrorCode: POINT_NAME_DUPLICATED)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error (ErrorCode: INTERNAL_SERVER, UNKNOWN)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{pointId}")
    @PrivateAccess
    public Mono<ResponseEntity<ResponseWithChange<FinalResponse>>> updatePoint(
            @PathVariable Long pointId, 
            @RequestBody UpdatePointRequest updatePointRequest
    ) {
        return pointService.updatePoint(pointId, updatePointRequest.getTitle())
            .map(point -> new FinalResponse().forOnlyPoint(point.getPointId(), point))
            .map(ResponseWithChange::new)
            .map(responseWithChange -> responseWithChange.toResponseEntity(HttpStatus.OK));
    }

    @Operation(
        summary = "Delete point",
        description = "Deletes a point from the project (Admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Point deleted successfully"),
        @ApiResponse(responseCode = "400", description = "Point cannot be deleted (ErrorCode: POINT_NOT_DELETABLE, EMPTY)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access (ErrorCode: INVALID_TOKEN, EXPIRED_TOKEN)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required (ErrorCode: INACCESSIBLE_USER)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Point not found (ErrorCode: POINT_NOT_FOUND)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error (ErrorCode: INTERNAL_SERVER, UNKNOWN)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{pointId}")
    @PrivateAccess
    public Mono<ResponseEntity<ResponseWithChange<Void>>> deletePoint(
        @PathVariable Long pointId
    ) {
        return pointService.deletePoint(pointId)
            .map(ResponseWithChange::new)
            .map(responseWithChange -> responseWithChange.toResponseEntity(HttpStatus.OK));
    }

    /**
     * Connects two points by creating an edge between the specified origin and destination.
     * The connection is assigned a default weight of 5.
     *
     * @pathVariable origin      the starting point of the edge to be created
     * @pathVariable destination the ending point of the edge to be created
     * @return a Mono containing the ResponseEntity with the created OutputEdgeDto and a status of HTTP 201 (Created)
     */
    @Operation(
        summary = "Connect two points",
        description = "Creates an edge between two points with a default weight of 5"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Points connected successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input parameters (ErrorCode: NULL_INPUT, NULL_POINTS, SAME_POINTS, INVALID_NUMERIC_IDS, BACKWARD_PATH, EMPTY)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access (ErrorCode: INVALID_TOKEN, EXPIRED_TOKEN)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required (ErrorCode: INACCESSIBLE_USER)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "One or both points not found (ErrorCode: NON_EXISTENT_POINTS)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Edge already exists (ErrorCode: EDGE_ALREADY_EXISTS)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error (ErrorCode: INTERNAL_SERVER, UNKNOWN)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{originPointId}/point/{destinationPointId}")
    @PrivateAccess
    public Mono<ResponseEntity<ResponseWithChange<EdgeDto>>> connectTwoPoints(
        @PathVariable Long originPointId,
        @PathVariable Long destinationPointId
    ) {
        return pointService.connectPoints(originPointId, destinationPointId)
                .map(ResponseWithChange::new)
                .map(responseWithChange -> responseWithChange.toResponseEntity(HttpStatus.CREATED));
    }

    /**
     * Disconnects two points by deleting the edge between them.
     * 
     * @pathVariable origin      the starting point of the edge to be deleted
     * @pathVariable destination the ending point of the edge to be deleted
     * @return a Mono containing the ResponseEntity with the deleted OutputEdgeDto and a status of HTTP 200 (OK)
     */
    @Operation(
        summary = "Disconnect two points",
        description = "Removes the edge between two points"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Points disconnected successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input parameters (ErrorCode: NULL_INPUT, NULL_POINTS, SAME_POINTS, INVALID_NUMERIC_IDS, EMPTY)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access (ErrorCode: INVALID_TOKEN, EXPIRED_TOKEN)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required (ErrorCode: INACCESSIBLE_USER)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "One or both points not found (ErrorCode: NON_EXISTENT_POINTS)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error (ErrorCode: INTERNAL_SERVER, UNKNOWN)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{originPointId}/point/{destinationPointId}")
    @PrivateAccess
    public Mono<ResponseEntity<ResponseWithChange<Void>>> disconnectTwoPoints(
        @PathVariable Long originPointId,
        @PathVariable Long destinationPointId
    ) {
        return pointService.disconnectPoints(originPointId, destinationPointId)
            .map(ResponseWithChange::new)
            .map(responseWithChange -> responseWithChange.toResponseEntity(HttpStatus.OK));
    }

    
    // .flatMap(updatedPoint -> {
    //     // if updatedPoint type is googledocs, get altContent as documentUrl

    //     String title = updatePointDto.getTitle();
    //     String documentUrl = existingPoint.getDocument();

    //     // If title is being updated and document exists, update Google Drive file name
    //     if (title != null && documentUrl != null) {
    //         // Extract file ID from Google Docs URL
    //         String fileId = documentUrl.substring(documentUrl.lastIndexOf("/") + 1);
    //         return driveService.updateFileName(token, fileId, pointId + " // " + title).thenReturn(existingPoint);
    //     }
    //     return Mono.just(existingPoint);
    // })
    // .flatMap(_saved -> lastUpdateService.update("path").thenReturn(_saved))
    // .map(updatedPoint -> new ResponseWithChange<>(
    //         OutputPointDto.fromPoint(updatedPoint),
    //         true,
    //         updatedPoint.getUpdatedOn().getTime()
    // ));

}
