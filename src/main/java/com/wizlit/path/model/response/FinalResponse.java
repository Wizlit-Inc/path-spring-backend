package com.wizlit.path.model.response;

import com.wizlit.path.model.domain.*;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FinalResponse {
    private Long currentId;
    private List<ProjectDto> projects;
    private List<PointDto> points;
    private List<MemoDto> memos;
    private List<UserDto> users;
    private List<ReserveMemoDto> reserveMemos;
    private List<IdDataDto<Long, List<MemoRevisionDto>>> memoRevisions;
    private List<RevisionContentDto> memoRevisionContents;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class IdDataDto<A,T> {
        private Long id;
        private A additional;
        private T data;
    }

    public FinalResponse forGetProject(Long currentId, ProjectDto project, List<PointDto> points) {
        this.currentId = currentId;
        this.projects = List.of(project);
        this.points = points;
        return this;
    }

    public FinalResponse forGetPoint(Long currentId, PointDto point, List<MemoDto> memos, List<UserDto> users) {
        this.currentId = currentId;
        this.points = List.of(point);
        this.memos = memos;
        this.users = users;
        return this;
    }

    public FinalResponse forOnlyPoint(Long currentId, PointDto point) {
        this.currentId = currentId;
        this.points = List.of(point);
        return this;
    }

    public FinalResponse forGetMemo(Long currentId, MemoDto memo, List<UserDto> users, ReserveMemoDto reserveMemo) {
        this.currentId = currentId;
        this.memos = List.of(memo);
        this.users = users;
        if (reserveMemo != null) {
            this.reserveMemos = List.of(reserveMemo);
        }
        return this;
    }

    public FinalResponse forOnlyMemo(Long currentId, MemoDto memo, ReserveMemoDto reserveMemo) {
        this.currentId = currentId;
        this.memos = List.of(memo);
        if (reserveMemo != null) {
            this.reserveMemos = List.of(reserveMemo);
        }
        return this;
    }

    public FinalResponse forNewMemo(Long currentId, MemoDto memo, ReserveMemoDto reserveMemo, PointDto point, UserDto user) {
        this.currentId = currentId;
        this.memos = List.of(memo);
        if (reserveMemo != null) {
            this.reserveMemos = List.of(reserveMemo);
        }
        this.points = List.of(point);
        this.users = List.of(user);
        return this;
    }

    public FinalResponse forMemoRevisions(Long currentId, Long timestampAfter, List<MemoRevisionDto> memoRevisions, List<UserDto> users) {
        this.currentId = currentId;
        this.memoRevisions = List.of(new IdDataDto<>(currentId, timestampAfter, memoRevisions));
        this.users = users;
        return this;
    }

    public FinalResponse forMemoRevisionContent(Long currentId, RevisionContentDto memoRevisionContent) {
        this.currentId = currentId;
        this.memoRevisionContents = List.of(memoRevisionContent);
        return this;
    }
}


