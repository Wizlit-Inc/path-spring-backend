package com.wizlit.path.controller;

import com.wizlit.path.model.AddPointDto;
import com.wizlit.path.entity.Edge;
import com.wizlit.path.entity.Point;
import com.wizlit.path.model.OutputEdgeDto;
import com.wizlit.path.model.OutputPointDto;
import com.wizlit.path.model.OutputPathDto;
import com.wizlit.path.repository.EdgeRepository;
import com.wizlit.path.repository.PointRepository;
import com.wizlit.path.service.EdgeService;
import com.wizlit.path.service.PointService;
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

import java.util.stream.Collectors;

@RestController
@AllArgsConstructor
@RequestMapping("/path")
public class PathController {

    private final PointService pointService;
    private final EdgeService edgeService;
    private final PointRepository pointRepository;
    private final EdgeRepository edgeRepository;

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
                .flatMap(points -> {
                    // Check if points exist
                    if (points.isEmpty()) {
                        return Mono.just(ResponseEntity.noContent().<OutputPathDto>build());
                    }
                    return edgeService.getAllEdgesByPoints(points)
                            .collectList()
                            .map(edges -> {
                                // Use the static method fromEdgesAndPoints
                                return ResponseEntity.ok(OutputPathDto.fromEdgesAndPoints(edges, points));
                            });
                })
                // Handle potential errors in the pipeline
                .onErrorResume(ex -> {
                    // Log the error (logging not shown for brevity)
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null));
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

        Point newPoint = Point.builder()
                .title(addPointDto.getTitle())
                .description(addPointDto.getDescription())
                .build();

        if (addPointDto.getOrigin() == null && addPointDto.getDestination() == null) {
            // Only adding a new point with no connections
            return pointService.createPoint(newPoint)
                    .map(_savedPoint -> ResponseEntity.status(HttpStatus.CREATED).body(
                            OutputPointDto.fromPoint(_savedPoint))
                    );

        } else if (addPointDto.getOrigin() == null || addPointDto.getDestination() == null) {
            // Connect point with one edge - validate existence of origin or destination
            Long existingPointId = Long.valueOf(addPointDto.getOrigin() != null
                    ? addPointDto.getOrigin()
                    : addPointDto.getDestination());

            return pointRepository.findById(existingPointId)
                    .switchIfEmpty(Mono.error(new IllegalArgumentException("Referenced point does not exist")))
                    .flatMap(_ -> {
                        return pointRepository.save(newPoint).flatMap(savedPoint -> {
                            Edge newEdge = Edge.builder()
                                    .startPoint(addPointDto.getOrigin() != null ? Long.valueOf(addPointDto.getOrigin()) : savedPoint.getId())
                                    .endPoint(addPointDto.getDestination() != null ? Long.valueOf(addPointDto.getDestination()) : savedPoint.getId())
                                    .build();

                            return edgeRepository.save(newEdge).then(
                                    Mono.just(ResponseEntity.status(HttpStatus.CREATED).body(
                                            OutputPointDto.fromPoint(savedPoint)))
                            );
                        });
                    });
        } else {
            return pointService.addMiddlePoint(addPointDto.getOrigin(), addPointDto.getDestination(), newPoint, 5)
                    .map(_savedPoint -> ResponseEntity.status(HttpStatus.CREATED).body(
                            OutputPointDto.fromPoint(_savedPoint))
                    );
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
        return edgeService.connectEdge(origin, destination, 5)
                .map(edge -> ResponseEntity.status(HttpStatus.CREATED).body(OutputEdgeDto.fromEdge(edge)));
    }

}
