package com.wizlit.path.repository;

import com.wizlit.path.entity.LastUpdate;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LastUpdateRepository extends ReactiveCrudRepository<LastUpdate, String> {
}