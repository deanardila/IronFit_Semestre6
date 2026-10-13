package com.ironfit.backendmongo.repositorio.test;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.ironfit.backendmongo.modelo.test.TestDoc;

public interface TestRepository extends MongoRepository<TestDoc, String> {

}
