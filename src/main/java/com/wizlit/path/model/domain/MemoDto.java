package com.wizlit.path.model.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.wizlit.path.entity.Memo;
import com.wizlit.path.entity.MemoDraft;
import com.wizlit.path.entity.MemoRevision;

import com.fasterxml.jackson.annotation.JsonInclude;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MemoDto {
    private Long memoId;
    private Long memoPoint;
    private String memoTitle;
    private String memoSummary;
    private Long memoSummaryTimestamp;
    private Long memoCreatedTimestamp;
    private Long memoCreatedUser;
    private Long memoUpdatedTimestamp; // memo draft updated timestamp || memo latest revision timestamp
    private Long memoUpdatedUser; // memo draft editor || memo latest revision actor
    private List<Long> contributorUserIds;
    private String memoEmbedContent;
    private String memoContent; // memo draft content || memo latest revision content

    public static MemoDto from(
        Memo memo,
        MemoDraft memoDraft,
        Long contributorUserId
    ) {
        return MemoDto.from(memo, memoDraft, null, null, new ArrayList<>(Arrays.asList(contributorUserId)));
    }

    public static MemoDto from(
        Memo memo,
        MemoDraft memoDraft,
        List<Long> contributorUserIds
    ) {
        return MemoDto.from(memo, memoDraft, null, null, new ArrayList<>(contributorUserIds));
    }

    public static MemoDto from(
        Memo memo,
        MemoRevision memoRevision,
        RevisionContentDto revisionContent,
        List<Long> contributorUserIds
    ) {
        return MemoDto.from(memo, null, memoRevision, revisionContent, new ArrayList<>(contributorUserIds));
    }

    private static MemoDto from(
        Memo memo,
        MemoDraft memoDraft,
        MemoRevision memoRevision,
        RevisionContentDto revisionContent,
        List<Long> contributorUserIds
    ) {
        MemoDto dto = MemoDto.builder()
                .memoId(memo.getMemoId())
                .memoPoint(memo.getMemoPoint())
                .memoTitle(memo.getMemoTitle())
                .memoSummary(memo.getMemoSummary())
                .memoSummaryTimestamp(memo.getMemoSummaryTimestamp() != null ? memo.getMemoSummaryTimestamp().toEpochMilli() : null)
                .memoCreatedTimestamp(memo.getMemoCreatedTimestamp() != null ? memo.getMemoCreatedTimestamp().toEpochMilli() : null)
                .memoCreatedUser(memo.getMemoCreatedUser())
                .memoEmbedContent(memo.getMemoEmbedContent())
                .contributorUserIds(contributorUserIds)
                .build();

        if (memoDraft != null && memoRevision != null) {
            // Compare timestamps to determine which is more recent
            if (memoDraft.getDraftUpdatedTimestamp().isAfter(memoRevision.getRevTimestamp())) {
                dto.setMemoUpdatedTimestamp(memoDraft.getDraftUpdatedTimestamp() != null ? memoDraft.getDraftUpdatedTimestamp().toEpochMilli() : null);
                dto.setMemoUpdatedUser(memoDraft.getDraftEditor());
                dto.setMemoContent(memoDraft.getDraftContent());
            } else {
                dto.setMemoUpdatedTimestamp(memoRevision.getRevTimestamp() != null ? memoRevision.getRevTimestamp().toEpochMilli() : null);
                dto.setMemoUpdatedUser(memoRevision.getRevActor());
                dto.setMemoContent(revisionContent.getContent());
            }
        } else if (memoDraft != null) {
            dto.setMemoUpdatedTimestamp(memoDraft.getDraftUpdatedTimestamp() != null ? memoDraft.getDraftUpdatedTimestamp().toEpochMilli() : null);
            dto.setMemoUpdatedUser(memoDraft.getDraftEditor());
            dto.setMemoContent(memoDraft.getDraftContent());
        } else if (memoRevision != null) {
            dto.setMemoUpdatedTimestamp(memoRevision.getRevTimestamp() != null ? memoRevision.getRevTimestamp().toEpochMilli() : null);
            dto.setMemoUpdatedUser(memoRevision.getRevActor());
            dto.setMemoContent(revisionContent.getContent());
        }
        
        return dto;
    }
} 