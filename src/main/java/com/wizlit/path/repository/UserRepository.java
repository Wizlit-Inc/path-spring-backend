package com.wizlit.path.repository;

import com.wizlit.path.entity.User;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@Repository
public interface UserRepository extends ReactiveCrudRepository<User, Long> {
    Mono<User> findByUserEmail(String email);
    Flux<User> findByUserIdInAndUserUpdatedTimestampAfter(List<Long> userIds, Instant updatedAfter);
} 