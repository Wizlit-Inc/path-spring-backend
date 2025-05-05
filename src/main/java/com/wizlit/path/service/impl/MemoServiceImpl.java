package com.wizlit.path.service.impl;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import com.wizlit.path.model.domain.MemoDto;
import com.wizlit.path.model.domain.UserDto;
import com.wizlit.path.service.MemoService;
import com.wizlit.path.service.manager.MemoManager;
import com.wizlit.path.entity.Memo;
import com.wizlit.path.entity.MemoDraft;
import com.wizlit.path.entity.MemoReserve;
import com.wizlit.path.service.manager.PointManager;
import com.wizlit.path.temp.GoogleService;
import com.wizlit.path.entity.vo.MemoType;
import com.wizlit.path.exception.ApiException;
import com.wizlit.path.exception.ErrorCode;

@Service
@RequiredArgsConstructor
public class MemoServiceImpl implements MemoService {

    /**
     * Service 규칙:
     * 1. repository 직접 호출 X (helper 를 통해서만 호출 - 동일한 helper 가 다른 곳에서도 쓰여도 됨)
     */
    
    private final MemoManager memoManager;
    private final PointManager pointManager;
    private final GoogleService driveService;

    @Override
    public Flux<MemoDto> listMemosByPointId(Long pointId, Instant updatedAfter) {
        return memoManager.listMemosByPointId(pointId, updatedAfter);
    }

    @Override
    public Mono<MemoDto> getMemo(Long memoId, Instant updatedAfter) {
        return memoManager.getFullMemo(memoId, updatedAfter);
    }

    @Transactional
    @Override
    public Mono<MemoDto> createMemo(Long pointId, UserDto user, String title, String content) {        
        return memoManager.createMemoWithDraft(pointId, user.getUserId(), title, content)
            .flatMap(tuple -> {
                Memo savedMemo = tuple.getT1();
                MemoDraft savedDraft = tuple.getT2();

                return pointManager.addMemoToPoint(pointId, savedMemo.getMemoId())
                    .then(Mono.defer(() -> {
                        MemoDto dto = MemoDto.from(
                            pointId,
                            savedMemo,
                            savedDraft,
                            user.getUserId()
                        );

                        return Mono.just(dto);
                    }));
            });
    }

    @Transactional
    @Override
    public Mono<MemoDto> updateMemo( Long memoId, UserDto user, String title, String content, String reserveCode) {
        return memoManager.updateDraft(memoId, user.getUserId(), title, content, reserveCode)
            .flatMap(draftDto -> memoManager.getFullMemo(memoId));
    }

    @Transactional
    @Override
    public Mono<MemoDto> updateExternalMemo(
        Long memoId,
        String externalKey,
        String title
    ) {
        return memoManager.findMemoById(memoId)
            .<Memo>flatMap(memo -> {
                if (memo.getMemoType() == MemoType.GOOGLE_DOCS) {
                    String documentUrl = memo.getMemoAltContent();

                    // If title is being updated and document exists, update Google Drive file name
                    if (title != null && documentUrl != null) {
                        // Extract file ID from Google Docs URL
                        String fileId = documentUrl.substring(documentUrl.lastIndexOf("/") + 1);
                        return driveService.updateFileName(externalKey, fileId, memoId + " // " + title)
                            .then(memoManager.updateExternalMemo(memo, title));
                    }
                }
                throw new ApiException(ErrorCode.NOT_EXTERNAL_MEMO, memoId);
            })
            .flatMap(memo -> memoManager.getFullMemo(memoId));
    }

    // private Mono<Point> processDocument(String token, Point savedPoint) {
    //     if (savedPoint.getDocument() == null) {
    //         return driveService.copyDocs(
    //                         token,
    //                         "16ENglpBm0RpyVEEPLxAJS7K3jmAzBbcn2LnzTTJDlMY",
    //                         googleDriveFolderId,
    //                         savedPoint.getId() + " // " + savedPoint.getTitle()
    //                 )
    //                 .flatMap(driveResponse -> {
    //                     savedPoint.setDocument("https://docs.google.com/document/d/" + driveResponse.getId());
    //                     return pointService.updatePoint(savedPoint);
    //                 });
    //     }
    //     return Mono.just(savedPoint);
    // }

    @Transactional
    @Override
    public Mono<String> reserveMemo(Long memoId, UserDto user, String currentReserveCode) {
        return memoManager.createReserve(memoId, user.getUserId(), currentReserveCode)
            .map(MemoReserve::getReserveCode);
    }

    @Transactional
    @Override
    public Mono<Void> cancelReserve(Long memoId, UserDto user, String reserveCode) {
        return memoManager.deleteReserve(memoId, user.getUserId(), reserveCode);
    }

    @Transactional
    @Override
    public Mono<Void> moveMemo(Long memoId, Long newPointId) {
        return Mono.zip(
            pointManager.moveMemoToPoint(newPointId, memoId),
            memoManager.moveMemo(memoId, newPointId)
        ).then();
    }
}
