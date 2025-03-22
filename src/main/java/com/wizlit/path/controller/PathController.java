package com.wizlit.path.controller;

import com.wizlit.path.model.AddPointDto;
import com.wizlit.path.entity.Point;
import com.wizlit.path.model.OutputEdgeDto;
import com.wizlit.path.model.OutputPointDto;
import com.wizlit.path.model.OutputPathDto;
import com.wizlit.path.service.EdgeService;
import com.wizlit.path.service.PointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@AllArgsConstructor
@RequestMapping("/path")
public class PathController {

    /**
     * Controller 규칙:
     * 1. repository 직접 호출 X (service 만 호출)
     */

    private final PointService pointService;
    private final EdgeService edgeService;

    private static final Logger log = LoggerFactory.getLogger(PathController.class);

    @GetMapping("/{pointId}")
    @Operation(
            summary = "Get a point and its details",
            description = "Retrieve a point by its ID using edgeService. Converts the result into an OutputPointDto.",
            tags = {"Path Management"},
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
    public Mono<ResponseEntity<OutputPointDto>> getPoint(@PathVariable Long pointId) {
        return pointService.findExistingPoint(pointId)
                .map(point -> ResponseEntity.ok(OutputPointDto.fromPoint(point)));
    }
    
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
    public Mono<ResponseEntity<OutputPathDto>> getAllPointsAndEdges() {
        return pointService.getAllPoints()
                .collectList()
                .flatMap(points -> {
                    if (points.isEmpty()) {
                        return Mono.just(ResponseEntity.ok(OutputPathDto.builder().build()));
                    }
                    return edgeService.getAllEdgesByPoints(points)
                            .collectList()
                            .map(edges -> ResponseEntity.ok(OutputPathDto.fromEdgesAndPoints(points, edges)));
                });
    }


    @PostMapping
    @Transactional
    @Operation(
            summary = "Add a new point",
            description = "Adds a new point to the system. " +
                    "Returns a bad request status if invalid input is provided, or an internal server error status in case of processing errors.",
            tags = {"Path Management"},
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
    public Mono<ResponseEntity<OutputPointDto>> addPoint(@RequestBody AddPointDto addPointDto) {

        Point newPoint = AddPointDto.toPoint(addPointDto);

        if (addPointDto.getOrigin() == null && addPointDto.getDestination() == null) {
            // Only adding a new point with no connections
            return pointService.createPoint(newPoint)
                    .map(_savedPoint -> ResponseEntity.status(HttpStatus.CREATED).body(OutputPointDto.fromPoint(_savedPoint)));

        } else if (addPointDto.getOrigin() == null || addPointDto.getDestination() == null) {
            // Connect point with one edge - validate existence of origin or destination
            Long existingPointId = Long.valueOf(addPointDto.getOrigin() != null
                    ? addPointDto.getOrigin()
                    : addPointDto.getDestination());

            return pointService.findExistingPoint(existingPointId)
                    .then(pointService.createPoint(newPoint))
                    .flatMap(_savedPoint ->
                            edgeService.createEdge(
                                    addPointDto.getOrigin() != null ? Long.valueOf(addPointDto.getOrigin()) : _savedPoint.getId(),
                                    addPointDto.getDestination() != null ? Long.valueOf(addPointDto.getDestination()) : _savedPoint.getId()
                            ).then(Mono.just(_savedPoint))
                    )
                    .flatMap(_savedPoint -> Mono.just(ResponseEntity.status(HttpStatus.CREATED).body(OutputPointDto.fromPoint(_savedPoint))));
        } else {
            return pointService.convertPointsToLong(addPointDto.getOrigin(), addPointDto.getDestination())
                    .flatMap(_tuple -> {
                        Long originIdInLong = _tuple.getT1();
                        Long destinationIdInLong = _tuple.getT2();

                        return edgeService.validateNotBackwardPath(originIdInLong, destinationIdInLong, 5)
                                .then(pointService.createPoint(newPoint))
                                .flatMap(_savedMiddlePoint ->
                                        edgeService.splitEdge(originIdInLong, destinationIdInLong, _savedMiddlePoint.getId())
                                                .then(Mono.just(_savedMiddlePoint))
                                );
                    })
                    .map(_savedPoint -> ResponseEntity.status(HttpStatus.CREATED).body(OutputPointDto.fromPoint(_savedPoint)));
        }
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
                .map(_edge -> ResponseEntity.status(HttpStatus.CREATED).body(OutputEdgeDto.fromEdge(_edge)));
    }
}
