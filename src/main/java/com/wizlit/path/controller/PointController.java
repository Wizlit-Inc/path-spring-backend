package com.wizlit.path.controller;

import com.wizlit.path.entity.Point;
import com.wizlit.path.model.*;
import com.wizlit.path.service.EdgeService;
import com.wizlit.path.service.LastUpdateService;
import com.wizlit.path.service.PointService;
import com.wizlit.path.temp.GoogleService;
import com.wizlit.path.utils.PrivateAccess;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/point")
public class PointController {

    /**
     * Controller 규칙:
     * 1. repository 직접 호출 X (service 만 호출)
     */

    private final PointService pointService;
    private final EdgeService edgeService;
    private final LastUpdateService lastUpdateService;
    private final GoogleService driveService;
    
    @Value("${app.googledrive.folderId}")
    private String googleDriveFolderId;

    @PostMapping
    @PrivateAccess
    @Transactional
    @Operation(
            summary = "Add a new point",
            description = "Adds a new point to the system. " +
                    "Returns a bad request status if invalid input is provided, or an internal server error status in case of processing errors.",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Successfully created a new point",
                            content = @Content(
                                    schema = @Schema(implementation = OutputPointDto.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request due to invalid input. Possible error codes:\n" +
                                    "- **NULL_PARAMETERS**: Input data contains null values\n" +
                                    "- **INVALID_NUMERIC_IDS**: Provided ID is invalid or non-numeric\n" +
                                    "- **SAME_POINTS**: Start point and end point cannot be the same\n" +
                                    "- **NON_EXISTENT_POINTS**: Either the startPoint or endPoint does not exist"
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "Conflict occurred while processing the request. Possible error codes:\n" +
                                    "- **BACKWARD_PATH**: A backward path exists from endPoint to startPoint within X edges\n"
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "An internal server error occurred while processing the request. Possible error codes:\n" +
                                    "- **ERR_INTERNAL**: An unexpected error occurred. Please try again later\n" +
                                    "- **ERR_UNKNOWN**: An unspecified error occurred"
                    )
            }
    )
    public Mono<ResponseEntity<OutputPointDto>> addPoint(
            @RequestAttribute("token") String token,
            @RequestBody AddPointDto addPointDto
    ) {

        Point newPoint = AddPointDto.toPoint(addPointDto);
        Mono<Point> pointMono;

        if (addPointDto.getOrigin() == null && addPointDto.getDestination() == null) {
            // Only adding a new point with no connections
            pointMono = pointService.createPoint(newPoint);

        } else if (addPointDto.getOrigin() == null || addPointDto.getDestination() == null) {
            // Connect point with one edge - validate existence of origin or destination
            Long existingPointId = Long.valueOf(addPointDto.getOrigin() != null
                    ? addPointDto.getOrigin()
                    : addPointDto.getDestination());

            pointMono = pointService.findExistingPoint(existingPointId)
                    .then(pointService.createPoint(newPoint))
                    .flatMap(savedPoint ->
                            edgeService.createEdge(
                                    addPointDto.getOrigin() != null ? Long.valueOf(addPointDto.getOrigin()) : savedPoint.getId(),
                                    addPointDto.getDestination() != null ? Long.valueOf(addPointDto.getDestination()) : savedPoint.getId()
                            ).thenReturn(savedPoint)
                    );

        } else {
            // Both origin and destination provided: split edge
            pointMono = pointService.convertPointsToLong(addPointDto.getOrigin(), addPointDto.getDestination())
                    .flatMap(tuple -> {
                        Long originId = tuple.getT1();
                        Long destinationId = tuple.getT2();
                        return edgeService.validateNotBackwardPath(originId, destinationId, 5)
                                .then(pointService.createPoint(newPoint))
                                .flatMap(savedMiddlePoint ->
                                        edgeService.splitEdge(originId, destinationId, savedMiddlePoint.getId())
                                                .then(Mono.just(savedMiddlePoint))
                                );
                    });
        }

        return pointMono
                .flatMap(savedPoint -> processDocument(token, savedPoint))
                .flatMap(_saved -> lastUpdateService.update("path").thenReturn(_saved))
                .map(updatedPoint -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(OutputPointDto.fromPoint(updatedPoint)));
    }

    private Mono<Point> processDocument(String token, Point savedPoint) {
        if (savedPoint.getDocument() == null) {
            return driveService.copyDocs(
                            token,
                            "16ENglpBm0RpyVEEPLxAJS7K3jmAzBbcn2LnzTTJDlMY",
                            googleDriveFolderId,
                            savedPoint.getId() + " // " + savedPoint.getTitle()
                    )
                    .flatMap(driveResponse -> {
                        savedPoint.setDocument("https://docs.google.com/document/d/" + driveResponse.getId());
                        return pointService.updatePoint(savedPoint);
                    });
        }
        return Mono.just(savedPoint);
    }

    @GetMapping("/{pointId}")
    @Operation(
            summary = "Get a point and its details",
            description = "Retrieve a point by its ID using edgeService. Converts the result into an OutputPointDto.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved the point",
                            content = @Content(
                                    schema = @Schema(implementation = OutputPointDto.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Point not found"
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "An internal server error occurred"
                    )
            }
    )
    public Mono<ResponseWithTimestamp<OutputPointDto>> getPoint(@PathVariable Long pointId) {
        return pointService.findExistingPoint(pointId)
                .map(point -> new ResponseWithTimestamp<>(OutputPointDto.fromPoint(point)));
    }
    
    @PutMapping("/{pointId}")
    @PrivateAccess
    @Transactional
    @Operation(
            summary = "Update a point",
            description = "Updates a point by its ID using pointService. Converts the result into an OutputPointDto.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully updated the point",
                            content = @Content(schema = @Schema(implementation = OutputPointDto.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Point not found"
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "An internal server error occurred"
                    )
            }
    )
    public Mono<ResponseWithTimestamp<OutputPointDto>> updatePoint(
            @RequestAttribute("token") String token,
            @PathVariable String pointId, 
            @RequestBody UpdatePointDto updatePointDto
    ) {
        Point point = updatePointDto.toPoint(pointId);

        return pointService.updatePoint(point)
                .flatMap(existingPoint -> {
                    String title = updatePointDto.getTitle();
                    String documentUrl = existingPoint.getDocument();

                    // If title is being updated and document exists, update Google Drive file name
                    if (title != null && documentUrl != null) {
                        // Extract file ID from Google Docs URL
                        String fileId = documentUrl.substring(documentUrl.lastIndexOf("/") + 1);
                        return driveService.updateFileName(token, fileId, pointId + " // " + title).thenReturn(existingPoint);
                    }
                    return Mono.just(existingPoint);
                })
                .flatMap(_saved -> lastUpdateService.update("path").thenReturn(_saved))
                .map(updatedPoint -> new ResponseWithTimestamp<>(OutputPointDto.fromPoint(updatedPoint)));
    }

}
