package com.wizlit.path.model;

import com.wizlit.path.entity.Edge;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutputEdgeDto {
    private String source;
    private String target;
    private Boolean trimmed;

    // function: convert from Edge
    public static OutputEdgeDto fromEdge(Edge edge) {
        return OutputEdgeDto.builder()
                .source(edge.getStartPoint().toString())
                .target(edge.getEndPoint().toString())
                .trimmed(Boolean.FALSE) // Default value or modify as required
                .build();
    }
}
