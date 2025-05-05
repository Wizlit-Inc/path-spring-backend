package com.wizlit.path.service.manager;

import org.springframework.stereotype.Component;

import com.wizlit.path.entity.User;
import com.wizlit.path.exception.ApiException;
import com.wizlit.path.exception.ErrorCode;
import com.wizlit.path.repository.UserRepository;
import com.wizlit.path.utils.Validator;

import lombok.RequiredArgsConstructor;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class UserManager {

    /**
     * Helper 규칙:
     * 1. 동일한 repository 가 다른 곳에서도 쓰이면 안됨
     * 2. repository 의 각 기능은 반드시 한 번만 호출
     * 3. repository 기능에는 .onErrorMap(error -> Validator.from(error).toException()) 필수
     * 4. 다른 helper 나 service 호출 금지
     * 5. DTO 반환 금지
     */

    private final UserRepository userRepository;

    /**
     * Finds a user by email.
     *
     * @param email The email of the user to find
     * @param throwException Whether to throw an exception if the user is not found
     * @return A Mono containing the found User, or an error if not found
     */
    public Mono<User> findByEmail(String email, Boolean throwException) {
        return userRepository.findByUserEmail(email)
            .onErrorMap(error -> Validator.from(error)
                .containsAllElseError(
                    new ApiException(ErrorCode.USER_NOT_FOUND, email),
                    "foreign", "key", "user"
                )
                .toException())
            .flatMap(existingUser -> {
                if (existingUser != null) {
                    if (throwException) {
                        return Mono.error(new ApiException(ErrorCode.USER_NOT_FOUND, email));
                    } else {
                        return Mono.just(existingUser);
                    }
                }
                return Mono.empty();
            });
    }

    public Mono<User> findByEmail(String email) {
        return findByEmail(email, true);
    }
    
    /**
     * Creates a new user.
     *
     * @param email The email of the user
     * @param name The name of the user
     * @param avatar The avatar of the user
     * @return A Mono containing the created User
     */
    public Mono<User> createUser(String email, String name, String avatar) {
        return userRepository.save(User.builder()
                .userEmail(email)
                .userName(name)
                .userAvatar(avatar)
                .build())
                .onErrorMap(error -> Validator.from(error)
                        .containsAllElseError(
                            new ApiException(ErrorCode.USER_ALREADY_EXISTS, email),
                            "unique", "key", "user_email"
                        )
                        .toException());
    }

    /**
     * Finds users by their IDs.
     *
     * @param userIds The IDs of the users to find
     * @param updatedAfter The timestamp to check against
     * @return A Flux containing the found Users
     */
    public Flux<User> findByUserIds(List<Long> userIds, Instant updatedAfter) {
        if (userIds == null || userIds.isEmpty()) {
            return Flux.empty();
        }

        if (updatedAfter == null) {
            return userRepository.findAllById(userIds)
                .onErrorMap(error -> Validator.from(error)
                    .containsAllElseError(
                        new ApiException(ErrorCode.USER_NOT_FOUND, userIds),
                        "foreign", "key", "user"
                    )
                    .toException());
        }

        return userRepository.findByUserIdInAndUserUpdatedTimestampAfter(userIds, updatedAfter)
            .onErrorMap(error -> Validator.from(error)
                .containsAllElseError(
                    new ApiException(ErrorCode.USER_NOT_FOUND, userIds),
                    "foreign", "key", "user"
                )
                .toException());
    }
}
