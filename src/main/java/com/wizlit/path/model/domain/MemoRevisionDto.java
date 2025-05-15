package com.wizlit.path.model.domain;

import com.wizlit.path.entity.MemoRevision;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemoRevisionDto {
    private Long revisionId;
    private Long revisionMemo;
    private Long revisionActor;
    private Long revisionTimestamp;
    private Long revisionStartTimestamp;
    private Long revisionEndTimestamp;
    private Boolean revisionMinorEdit;
    private String revisionSummary;
    private Long revisionContentId;

    public static MemoRevisionDto from(MemoRevision revision) {
        return MemoRevisionDto.builder()
            .revisionId(revision.getRevId())
            .revisionMemo(revision.getRevMemo())
            .revisionActor(revision.getRevActor())
            .revisionTimestamp(revision.getRevTimestamp().toEpochMilli())
            .revisionStartTimestamp(revision.getRevStartTimestamp().toEpochMilli())
            .revisionEndTimestamp(revision.getRevEndTimestamp().toEpochMilli())
            .revisionMinorEdit(revision.getRevMinorEdit())
            .revisionSummary(revision.getRevSummary())
            .revisionContentId(revision.getRevContent())
            .build();
    }
}

