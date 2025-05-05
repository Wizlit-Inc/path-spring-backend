package com.wizlit.path.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.annotation.Transient;

import com.wizlit.path.entity.vo.MemoType;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("memo")
public class Memo {
    @Id
    @Column("memo_id")
    private Long memoId;

    @Column("memo_point")
    private Long memoPoint;

    @Column("memo_title")
    private String memoTitle;

    @Column("memo_type")
    private MemoType memoType; // type cannot be changed

    @Column("memo_summary")
    private String memoSummary; // ai summary of memo latest revision. update on memo memoUpdatedTimestamp n days ago

    @Column("memo_summary_timestamp")
    private Instant memoSummaryTimestamp; // if memo update was happened because of summary, update this together

    @Column("memo_created_timestamp")
    private Instant memoCreatedTimestamp;

    @Column("memo_updated_timestamp")
    private Instant memoUpdatedTimestamp; // (look for memo draft first) Memo updated

    @Column("memo_created_user")
    private Long memoCreatedUser;

    @Column("memo_latest_revision")
    private Long memoLatestRevision;

    @Column("memo_alt_content")
    private String memoAltContent; // google docs url, memo content draft => content is saved until same user stops editing. (max 12 hours)

    @Transient
    private Integer position;

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public Memo(Long pointId, String title, Long userId) {
        this.memoPoint = pointId;
        this.memoTitle = title;
        this.memoType = MemoType.TEXT;
        this.memoCreatedUser = userId;
        this.memoCreatedTimestamp = Instant.now();
        this.memoUpdatedTimestamp = Instant.now();
        this.memoCreatedUser = userId;
    }
}
