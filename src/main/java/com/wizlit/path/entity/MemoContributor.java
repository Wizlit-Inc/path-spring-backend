package com.wizlit.path.entity;

import lombok.*;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("memo_contributor")
public class MemoContributor {
    @Column("memo_id")
    private Long memoId;

    @Column("user_id")
    private Long userId;

    // Composite primary key
    public static class MemoContributorId {
        private Long memoId;
        private Long userId;
    }
} 