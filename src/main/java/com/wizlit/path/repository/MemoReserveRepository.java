package com.wizlit.path.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.wizlit.path.entity.MemoReserve;

public interface MemoReserveRepository extends ReactiveCrudRepository<MemoReserve, Long> {

}
