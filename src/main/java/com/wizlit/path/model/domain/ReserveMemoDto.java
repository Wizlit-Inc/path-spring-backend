package com.wizlit.path.model.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.wizlit.path.entity.MemoReserve;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReserveMemoDto {
    private Long reserveMemoId;
    private String reserveMemoCode;
    private Long reserveExpireTime;

    public static ReserveMemoDto from(MemoReserve reserveMemo) {
        return ReserveMemoDto.builder()
            .reserveMemoId(reserveMemo.getReserveMemo())
            .reserveMemoCode(reserveMemo.getReserveCode())
            .reserveExpireTime(reserveMemo.getReserveExpireTimestamp().toEpochMilli())
            .build();
    }
}
