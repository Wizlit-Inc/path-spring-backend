package com.wizlit.path.model;

import com.wizlit.path.entity.Edge;
import com.wizlit.path.entity.Point;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutputPathDto {
    private Map<String, OutputPointDto> nodes;
    private List<OutputEdgeDto> edges;

    // function: convert from Edge and Point using OutputPointDto>fromPoint, OutputEdgeDto>fromPoint
    public static OutputPathDto fromEdgesAndPoints(List<Point> points, List<Edge> edges) {
        Map<String, OutputPointDto> nodes = points.stream()
                .map(OutputPointDto::fromPoint)
                .collect(Collectors.toMap(OutputPointDto::getId, pointDto -> pointDto));

        List<OutputEdgeDto> edgeDtos = edges.stream()
                .map(OutputEdgeDto::fromEdge)
                .toList();

        return OutputPathDto.builder()
                .nodes(nodes)
                .edges(edgeDtos)
                .build();
    }
}
