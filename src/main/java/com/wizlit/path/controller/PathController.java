package com.wizlit.path.controller;

import com.wizlit.path.model.*;
import com.wizlit.path.service.EdgeService;
import com.wizlit.path.service.LastUpdateService;
import com.wizlit.path.service.PointService;
import com.wizlit.path.utils.PrivateAccess;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Instant;

@RestController
@AllArgsConstructor
@RequestMapping("/api/path")
public class PathController {

    /**
     * Controller 규칙:
     * 1. repository 직접 호출 X (service 만 호출)
     */

    private final PointService pointService;
    private final EdgeService edgeService;
    private final LastUpdateService lastUpdateService;

    /**
     * Retrieves all points and their associated edges from the system.
     * If no points are available, it returns a ResponseEntity with a no-content status.
     * In case of an error during the process, it returns an internal server error response.
     *
     * @return a Mono containing a ResponseEntity with an OutputPathDto object that includes all points and edges,
     *         or appropriate response statuses (e.g., no content or internal server error).
     */
    @GetMapping
    @Operation(
            summary = "Get all points and related edges",
            description = "Fetch all points along with their connected edges from the system. " +
                    "Returns a no-content status if no points are available, or an internal server error status in case of processing errors.",
            tags = {"Path Management"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved points and edges",
                            content = @Content(
                                    schema = @Schema(implementation = OutputPathDto.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "204",
                            description = "No points found in the system"
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "An internal server error occurred while processing the request. Possible error codes:\n" +
                                    "- **ERR_INTERNAL**: An unexpected error occurred. Please try again later\n" +
                                    "- **ERR_UNKNOWN**: An unspecified error occurred"
                    )
            }
    )
    public Mono<ResponseWithTimestamp<OutputPathDto>> getAllPointsAndEdges() {
        return pointService.getAllPoints()
                .collectList()
                .flatMap(points -> {
                    if (points.isEmpty()) {
                        return Mono.just(new ResponseWithTimestamp<>(OutputPathDto.builder().build()));
                    }
                    return edgeService.getAllEdgesByPoints(points)
                            .collectList()
                            .map(edges -> new ResponseWithTimestamp<>(OutputPathDto.fromEdgesAndPoints(points, edges)));
                });
    }

    /**
     * Connects two points by creating an edge between the specified origin and destination.
     * The connection is assigned a default weight of 5.
     *
     * @param origin      the starting point of the edge to be created
     * @param destination the ending point of the edge to be created
     * @return a Mono containing the ResponseEntity with the created OutputEdgeDto and a status of HTTP 201 (Created)
     */
    @PostMapping("/connect")
    @PrivateAccess
    @Transactional
    @Operation(
            summary = "Connect two points",
            description = "Creates a connection (edge) between two existing points in the system. " +
                    "Returns a bad request status if invalid input is provided, a conflict status if connection-related rules are violated, or an internal server error status for unexpected issues.",
            tags = {"Path Management"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully connected the two points",
                            content = @Content(
                                    schema = @Schema(implementation = OutputEdgeDto.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request due to invalid input. Possible error codes:\n" +
                                    "- **NULL_PARAMETERS**: One or more input parameters are null\n" +
                                    "- **INVALID_NUMERIC_IDS**: The provided point IDs are invalid or non-numeric\n" +
                                    "- **SAME_POINTS**: Origin and destination points cannot be the same\n" +
                                    "- **NON_EXISTENT_POINTS**: Either the origin or destination point does not exist in the system"
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "Conflict during processing. Possible error codes:\n" +
                                    "- **EDGE_ALREADY_EXISTS**: An edge already exists between the two points\n" +
                                    "- **BACKWARD_PATH**: A backward path exists from the destination to the origin"
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "An internal server error occurred while processing the request. Possible error codes:\n" +
                                    "- **ERR_INTERNAL**: An unexpected error occurred. Please try again later\n" +
                                    "- **ERR_UNKNOWN**: An unspecified error occurred\n" +
                                    "- **SAVE_FAILED**: Failed to save the edge in the database"
                    )
            }
    )
    public Mono<ResponseEntity<OutputEdgeDto>> connectTwoPoints(@RequestParam String origin, @RequestParam String destination) {
        return pointService.convertPointsToLong(origin, destination)
                .flatMap(_tuple -> {
                    Long originIdInLong = _tuple.getT1();
                    Long destinationIdInLong = _tuple.getT2();

                    return edgeService.validateEdgeExists(originIdInLong, destinationIdInLong)
                            .then(pointService.validatePointsExist(originIdInLong, destinationIdInLong))
                            .then(edgeService.validateNotBackwardPath(originIdInLong, destinationIdInLong, 5))
                            .then(edgeService.createEdge(originIdInLong, destinationIdInLong));
                })
                .flatMap(_saved -> lastUpdateService.update("path").thenReturn(_saved))
                .map(_edge -> ResponseEntity.status(HttpStatus.CREATED).body(OutputEdgeDto.fromEdge(_edge)));
    }

    @GetMapping("/changed")
    @Operation(
            summary = ""
    )
    public Mono<ResponseWithTimestamp<Boolean>> isChanged(
            @RequestParam Long timestamp
    ) {
        if (timestamp == 0) {
            return Mono.just(new ResponseWithTimestamp<>(false));
        }
        return lastUpdateService.hasUpdate("path", Instant.ofEpochMilli(timestamp))
                .map(ResponseWithTimestamp::new);
    }

}
