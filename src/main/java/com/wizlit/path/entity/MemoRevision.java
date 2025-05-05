package com.wizlit.path.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("memo_revision")
public class MemoRevision {
    @Id
    @Column("rev_id")
    private Long revId;

    @Column("rev_memo")
    private Long revMemo;

    @Column("rev_actor")
    private Long revActor;

    @Column("rev_timestamp")
    private Instant revTimestamp; // cannot be changed.

    @Column("rev_start_timestamp")
    private Instant revStartTimestamp; // cannot be changed.

    @Column("rev_end_timestamp")
    private Instant revEndTimestamp; // cannot be changed.

    @Column("rev_minor_edit")
    private Boolean revMinorEdit;

    @Column("rev_parent_id")
    private Long revParentId; // parent revision id. if null, it is the first revision 

    @Column("rev_summary")
    private String revSummary; // an ai summary of diff

    @Column("rev_content")
    private Long revContent; // revision content id. if content is reverted, it will be useful
}