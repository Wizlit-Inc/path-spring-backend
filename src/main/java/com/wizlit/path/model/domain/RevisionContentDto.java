package com.wizlit.path.model.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.wizlit.path.entity.RevisionContent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RevisionContentDto {
    private Long contentId;
    private Long contentSize;
    private String content;

    public static RevisionContentDto from(RevisionContent revisionContent, String actualContent) {
        return RevisionContentDto.builder()
            .contentId(revisionContent.getContentId())
            .contentSize(revisionContent.getContentSize())
            .content(actualContent)
            .build();
    }
}