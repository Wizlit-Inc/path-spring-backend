package com.wizlit.path.model.request;

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
public class AddPointRequest {
    @Schema(description = "Project ID")
    private Long projectId;
    @Schema(description = "Title of the point")
    private String title;
    @Schema(description = "Origin point ID")
    private Long origin;
    @Schema(description = "Destination point ID")
    private Long destination;

    // function: convert to Point
    // public Point toPoint(Long userId) {
    //     return Point.builder()
    //             .pointTitle(this.title)
    //             .pointCreatedUser(userId)
    //             .build();
    // }
}
