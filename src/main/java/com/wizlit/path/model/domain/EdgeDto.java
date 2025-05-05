package com.wizlit.path.model.domain;

import com.wizlit.path.entity.Edge;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EdgeDto {
    private String origin;
    private String destination;

// function: convert from Edge
public static EdgeDto fromEdge(Edge edge) {
    return EdgeDto.builder()
            .origin(edge.getOriginPoint().toString())
            .destination(edge.getDestinationPoint().toString())
            .build();
    }
}
