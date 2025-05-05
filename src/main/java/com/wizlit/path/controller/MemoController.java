package com.wizlit.path.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import com.wizlit.path.model.ResponseWithChange;
import com.wizlit.path.model.domain.MemoDto;
import com.wizlit.path.model.request.MemoRequest;
import com.wizlit.path.model.response.FinalResponse;
import com.wizlit.path.service.MemoService;
import com.wizlit.path.service.UserService;
import com.wizlit.path.utils.PrivateAccess;
import com.wizlit.path.exception.ErrorResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.time.Instant;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/memo")
@Tag(name = "Memo", description = "Memo management APIs")
public class MemoController {

    /**
     * Controller 규칙:
     * 1. service 만 호출
     */
    
    private final UserService userService;
    private final MemoService memoService;

    @Operation(
        summary = "Create a new memo",
        description = "Creates a new memo in the specified point with the given title and content"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Memo created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input parameters (ErrorCode: NULL_INPUT, NOT_EXTERNAL_MEMO, EMPTY)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access (ErrorCode: INVALID_TOKEN, EXPIRED_TOKEN)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required (ErrorCode: INACCESSIBLE_USER)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Point not found (ErrorCode: POINT_NOT_FOUND)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Point has reached maximum number of memos (ErrorCode: POINT_MAX_MEMOS_REACHED)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error (ErrorCode: INTERNAL_SERVER, UNKNOWN)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    @PrivateAccess
    public Mono<ResponseEntity<ResponseWithChange<FinalResponse>>> createMemo(
        @RequestAttribute("email") String email,
        @RequestAttribute("name") String name,
        @RequestAttribute("avatar") String avatar,
        @RequestParam Long pointId,
        @RequestBody MemoRequest memoRequest,
        @RequestParam(required = false) Boolean continueEditing
    ) {
        return userService.getUserByEmailAndCreateIfNotExists(email, name, avatar)
            .flatMap(user -> memoService.createMemo(
                pointId,
                user,
                memoRequest.getMemoTitle(),
                memoRequest.getMemoContent()
            ).flatMap(memo -> {
                if (Boolean.TRUE.equals(continueEditing)) {
                    return memoService.reserveMemo(memo.getMemoId(), user, null)
                        .map(newReserveCode -> Tuples.of(memo, newReserveCode));
                }
                return Mono.just(Tuples.of(memo, ""));
            }))
            .map(tuple -> new FinalResponse().forOnlyMemo(tuple.getT1().getMemoId(), tuple.getT1(), tuple.getT2()))
            .switchIfEmpty(Mono.just(new FinalResponse()))
            .map(ResponseWithChange::new)
            .map(responseWithChange -> responseWithChange.toResponseEntity(HttpStatus.CREATED));
    }

    @Operation(
        summary = "Get memo details",
        description = "Retrieves details of a specific memo including its content and contributors"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved memo details"),
        @ApiResponse(responseCode = "400", description = "Invalid input parameters (ErrorCode: NULL_INPUT, EMPTY)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access (ErrorCode: INVALID_TOKEN, EXPIRED_TOKEN)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Memo not found (ErrorCode: MEMO_NOT_FOUND)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error (ErrorCode: INTERNAL_SERVER, UNKNOWN)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{memoId}")
    public Mono<ResponseEntity<ResponseWithChange<FinalResponse>>> getMemo(
        @PathVariable Long memoId,
        @RequestParam(required = false) Long lastFetchTimestamp
    ) {
        Instant updatedAfter = lastFetchTimestamp != null ? Instant.ofEpochMilli(lastFetchTimestamp) : null;
        return memoService.getMemo(memoId, updatedAfter)
            .flatMap(memo -> userService.listUserByUserIds(memo.getContributorUserIds(), updatedAfter)
                .collectList()
                .map(users -> new FinalResponse().forGetMemo(memoId, memo, users, null)))
            .switchIfEmpty(Mono.just(new FinalResponse()))
            .map(ResponseWithChange::new)
            .map(responseWithChange -> responseWithChange.toResponseEntity(HttpStatus.OK));
    }

    @Operation(
        summary = "Save memo changes",
        description = "Updates an existing memo with new content"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Memo updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input parameters (ErrorCode: NULL_INPUT, NOT_EXTERNAL_MEMO, EMPTY, ABNORMAL_CONTENT_DELETION)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access (ErrorCode: INVALID_TOKEN, EXPIRED_TOKEN)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required (ErrorCode: INACCESSIBLE_USER)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Memo not found (ErrorCode: MEMO_NOT_FOUND)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Memo is reserved by another user (ErrorCode: MEMO_RESERVED)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error (ErrorCode: INTERNAL_SERVER, UNKNOWN)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{memoId}")
    @PrivateAccess
    public Mono<ResponseEntity<ResponseWithChange<FinalResponse>>> saveMemo(
        @PathVariable Long memoId,
        @RequestAttribute("email") String email,
        @RequestAttribute("name") String name,
        @RequestAttribute("avatar") String avatar,
        @RequestBody MemoRequest memoRequest,
        @RequestParam(required = false) String reserveCode,
        @RequestParam(required = false) Boolean continueEditing
    ) {
        return userService.getUserByEmailAndCreateIfNotExists(email, name, avatar)
                .flatMap(user -> memoService.updateMemo(
                    memoId,
                    user,
                    memoRequest.getMemoTitle(),
                    memoRequest.getMemoContent(),
                    reserveCode
                ).flatMap(memoDto -> {
                    if (Boolean.TRUE.equals(continueEditing)) {
                        return memoService.reserveMemo(memoDto.getMemoId(), user, null)
                            .map(newReserveCode -> Tuples.of(memoDto, newReserveCode));
                    }
                    return Mono.just(Tuples.of(memoDto, ""));
                }))
            .map(tuple -> new FinalResponse().forOnlyMemo(memoId, tuple.getT1(), tuple.getT2()))
            .switchIfEmpty(Mono.just(new FinalResponse()))
            .map(ResponseWithChange::new)
            .map(responseWithChange -> responseWithChange.toResponseEntity(HttpStatus.OK));
    }

    @Operation(
        summary = "Edit external memo",
        description = "Updates the title of an external memo"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "External memo updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input parameters (ErrorCode: NULL_INPUT, NOT_EXTERNAL_MEMO)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access (ErrorCode: INVALID_TOKEN, EXPIRED_TOKEN)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Memo not found (ErrorCode: MEMO_NOT_FOUND)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{memoId}/external")
    @PrivateAccess
    public Mono<ResponseEntity<ResponseWithChange<FinalResponse>>> editExternalMemo(
        @PathVariable Long memoId,
        @RequestAttribute("token") String token,
        @RequestParam String title
    ) {
        return memoService.updateExternalMemo(memoId, token, title)
            .map(memo -> new FinalResponse().forOnlyMemo(memoId, memo, null))
            .switchIfEmpty(Mono.just(new FinalResponse()))
            .map(ResponseWithChange::new)
            .map(responseWithChange -> responseWithChange.toResponseEntity(HttpStatus.OK));
    }

    @Operation(
        summary = "Reserve memo for editing",
        description = "Reserves a memo for editing by a specific user"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Memo reserved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input parameters (ErrorCode: NULL_INPUT)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access (ErrorCode: INVALID_TOKEN, EXPIRED_TOKEN)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Memo not found (ErrorCode: MEMO_NOT_FOUND)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Memo is already reserved (ErrorCode: MEMO_RESERVED)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{memoId}/reserve")
    @PrivateAccess
    public Mono<ResponseEntity<ResponseWithChange<FinalResponse>>> reserveMemo(
        @PathVariable Long memoId,
        @RequestAttribute("email") String email,
        @RequestAttribute("name") String name,
        @RequestAttribute("avatar") String avatar,
        @RequestParam(required = false) String reserveCode,
        @RequestParam(required = false) Long lastFetchTimestamp
    ) {
        Instant updatedAfter = lastFetchTimestamp != null ? Instant.ofEpochMilli(lastFetchTimestamp) : null;
        return userService.getUserByEmailAndCreateIfNotExists(email, name, avatar)
            .flatMap(user -> memoService.getMemo(memoId, updatedAfter)
                .switchIfEmpty(Mono.just(new MemoDto()))
                .flatMap(memoDto -> memoService.reserveMemo(memoId, user, reserveCode)
                    .map(newReserveCode -> Tuples.of(memoDto, newReserveCode)))
                .map(tuple -> new FinalResponse().forOnlyMemo(memoId, tuple.getT1(), tuple.getT2()))
                .switchIfEmpty(Mono.just(new FinalResponse()))
                .map(ResponseWithChange::new)
                .map(responseWithChange -> responseWithChange.toResponseEntity(HttpStatus.OK)));
    }

    @Operation(
        summary = "Cancel memo reservation",
        description = "Cancels the reservation of a memo"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Reservation cancelled successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input parameters (ErrorCode: NULL_INPUT)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access (ErrorCode: INVALID_TOKEN, EXPIRED_TOKEN)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Memo not found (ErrorCode: MEMO_NOT_FOUND)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{memoId}/reserve")
    @PrivateAccess
    public Mono<ResponseEntity<Void>> cancelReserve(
        @PathVariable Long memoId,
        @RequestAttribute("email") String email,
        @RequestAttribute("name") String name,
        @RequestAttribute("avatar") String avatar,
        @RequestParam(required = false) String reserveCode
    ) {
        return userService.getUserByEmailAndCreateIfNotExists(email, name, avatar)
            .flatMap(user -> memoService.cancelReserve(memoId, user, reserveCode))
            .then(Mono.just(ResponseEntity.ok().build()));
    }
    
    @Operation(
        summary = "Move memo to different point",
        description = "Moves a memo to a different point in the project"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Memo moved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input parameters (ErrorCode: NULL_INPUT)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access (ErrorCode: INVALID_TOKEN, EXPIRED_TOKEN)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Memo or target point not found (ErrorCode: MEMO_NOT_FOUND, POINT_NOT_FOUND)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{memoId}/move")
    @PrivateAccess
    public Mono<ResponseEntity<ResponseWithChange<Void>>> moveMemo(
        @PathVariable Long memoId,
        @RequestParam Long newPointId
    ) {
        return memoService.moveMemo(memoId, newPointId)
            .map(ResponseWithChange::new)
            .map(responseWithChange -> responseWithChange.toResponseEntity(HttpStatus.OK));
    }

    // reorder memo?

    // delete memo? (Admin only)

}
