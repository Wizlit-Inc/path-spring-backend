package com.wizlit.path.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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
@Table("memo_reserve")
public class MemoReserve implements Persistable<Long> {

    /**
     * if memo is reserved, no one except the user with the reserve code can update memo draft.
     * - when save draft content is requested, it will tell whether user will extend reserve or not.
     * - only the user with the reserve code can extend reserve + request save draft content.
     * - when user stops extending reserve on save memo api, memo reserve will be deleted.
     */

    @Id
    @Column("reserve_memo")
    private Long reserveMemo;

    @Column("reserve_code")
    private String reserveCode; // only give code on reserve or extend reserve (cannot request to get code)

    @Column("reserve_editor")
    private Long reserveEditor; // user who reserved memo

    @Column("reserve_timestamp")
    private Instant reserveTimestamp;

    @Column("reserve_expire_timestamp")
    private Instant reserveExpireTimestamp; // after 5~15 minutes (=expire), other user can override

    public MemoReserve(Long reserveMemo, Long reserveEditor, long additionalExpireTime) {
        this.reserveMemo = reserveMemo;
        this.reserveEditor = reserveEditor;
        this.reserveExpireTimestamp = Instant.now().plusSeconds(additionalExpireTime);
    }

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    public Long getId() {
        return reserveMemo;
    }

    @Override
    @Transient
    public boolean isNew() {
        return this.isNew || this.reserveMemo == null;
    }
    
    public MemoReserve markAsExisting() {
        this.isNew = false;
        return this;
    }

    public MemoReserve markAsNew() {
        this.isNew = true;
        return this;
    }

    public MemoReserve updateFrom(MemoReserve reserve) {
        this.reserveCode = reserve.getReserveCode();
        this.reserveEditor = reserve.getReserveEditor();
        this.reserveExpireTimestamp = reserve.getReserveExpireTimestamp();
        return this;
    }
}
