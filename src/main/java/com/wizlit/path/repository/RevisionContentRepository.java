package com.wizlit.path.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.wizlit.path.entity.RevisionContent;

public interface RevisionContentRepository extends ReactiveCrudRepository<RevisionContent, Long> {

}
