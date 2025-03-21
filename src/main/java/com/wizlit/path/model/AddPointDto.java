package com.wizlit.path.model;

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
    private String description;
    private String origin;
    private String destination;
}
