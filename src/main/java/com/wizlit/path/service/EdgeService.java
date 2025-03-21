package com.wizlit.path.service;

import com.wizlit.path.entity.Edge;
import com.wizlit.path.entity.Point;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface EdgeService {

    Flux<Edge> getAllEdgesByPoints(List<Point> points);
    
    /**
     * Check if there is a path between two points within a certain depth.
     *
     * @param startPoint The ID of the starting point.
     * @param endPoint   The ID of the destination point.
     * @param depth      The maximum depth of the path.
     * @return A Mono containing a Boolean indicating if the path exists.
     */
    Mono<Boolean> isPathWithinDepth(String startPoint, String endPoint, int depth);

    /**
     * Establishes a connection between two points by creating an edge.
     * Ensures there is no backward path within the defined depth before creating the edge.
     *
     * @param startPoint the starting point of the edge
     * @param endPoint   the ending point of the edge
     * @param depth      the maximum depth to check for potential backward paths
     * @return a {@code Mono<Edge>} containing the created edge, or an error if a backward path exists
     */
    Mono<Edge> connectEdge(String startPoint, String endPoint, int depth);

}