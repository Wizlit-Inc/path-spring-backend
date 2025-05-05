package com.wizlit.path.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("memo_draft")
public class MemoDraft implements Persistable<Long> {

    /**
     * (triggered when attempts memo draft update using java)
     * memo draft will be deleted and saved in new memo revision when...
     * - not same user
     * - draft is 1~24 hours old (compare draft created timestamp with current time)
     * - draft content is same as last memo revision content
     * - from frontend, it will auto request save draft content every 5~15 minutes
     */
    
    @Id
    @Column("draft_memo_id")
    private Long draftMemoId;

    @Column("draft_editor")
    private Long draftEditor; // used for comparison and memo revision actor

    @Column("draft_created_timestamp")
    private Instant draftCreatedTimestamp;

    @Column("draft_updated_timestamp")
    private Instant draftUpdatedTimestamp; // draft last updated timestamp will be used in memo revision create timestamp

    @Column("draft_title")
    private String draftTitle;

    @Column("draft_content")
    private String draftContent;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    public Long getId() {
        return draftMemoId;
    }

    @Override
    @Transient
    public boolean isNew() {
        return this.isNew || this.draftMemoId == null;
    }

    public MemoDraft markAsExisting() {
        this.isNew = false;
        return this;
    }

    public MemoDraft markAsNew() {
        this.isNew = true;
        return this;
    }

    public MemoDraft updateFrom(MemoDraft draft) {
        this.draftEditor = draft.getDraftEditor();
        this.draftUpdatedTimestamp = draft.getDraftUpdatedTimestamp();
        this.draftTitle = draft.getDraftTitle();
        this.draftContent = draft.getDraftContent();
        return this;
    }

    private static String toJson(String title, String content) {
        return String.format("{'title':'%s','content':'%s'}", 
            title.replace("'", "\\'"), 
            content.replace("'", "\\'"));
    }

    public String toJson() {
        return toJson(draftTitle, draftContent);
    }

    public Boolean sameJsonAs(String compareJson) {
        String draftJson = toJson();
        return draftJson.equals(compareJson);
    }
}
