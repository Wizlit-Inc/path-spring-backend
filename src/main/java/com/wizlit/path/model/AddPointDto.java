package com.wizlit.path.model;

import com.wizlit.path.entity.Point;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddPointDto {
    @Schema(description = "Title of the point")
    private String title;
    private String objective;
    private String document;
    private String origin;
    private String destination;

    // function: convert to Point
    public static Point toPoint(AddPointDto addPointDto) {
        return Point.builder()
                .title(addPointDto.getTitle())
                .objective(addPointDto.getObjective())
                .document(addPointDto.getDocument())
                .build();
    }
}
