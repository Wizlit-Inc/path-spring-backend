package com.wizlit.path.model.response;

import com.wizlit.path.model.domain.ProjectDto;
import com.wizlit.path.model.domain.PointDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.wizlit.path.model.domain.MemoDto;
import com.wizlit.path.model.domain.UserDto;

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
    private List<ProjectDto> projects;
    private List<PointDto> points;
    private List<MemoDto> memos;
    private List<UserDto> users;
    private ReserveMemo reserveMemo;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ReserveMemo {
        private Long reserveMemoId;
        private String reserveMemoCode;
    }

    public FinalResponse forGetProject(ProjectDto project, List<PointDto> points) {
        this.projects = List.of(project);
        this.points = points;
        return this;
    }

    public FinalResponse forGetPoint(PointDto point, List<MemoDto> memos, List<UserDto> users) {
        this.points = List.of(point);
        this.memos = memos;
        this.users = users;
        return this;
    }

    public FinalResponse forOnlyPoint(PointDto point) {
        this.points = List.of(point);
        return this;
    }

    public FinalResponse forGetMemo(Long memoId, MemoDto memo, List<UserDto> users, String reserveCode) {
        this.memos = List.of(memo);
        this.users = users;
        if (reserveCode != null && reserveCode.length() > 0) {
            this.reserveMemo = ReserveMemo.builder()
                .reserveMemoId(memoId)
                .reserveMemoCode(reserveCode)
                .build();
        }
        return this;
    }

    public FinalResponse forOnlyMemo(Long memoId, MemoDto memo, String reserveCode) {
        this.memos = List.of(memo);
        if (reserveCode != null && reserveCode.length() > 0) {
            this.reserveMemo = ReserveMemo.builder()
                .reserveMemoId(memoId)
                .reserveMemoCode(reserveCode)
                .build();
        }
        return this;
    }
}


