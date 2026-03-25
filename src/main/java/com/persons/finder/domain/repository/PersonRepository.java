package com.persons.finder.domain.repository;

import com.persons.finder.domain.Person;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonRepository extends MongoRepository<Person, String> {
    Person findByEmail(String email);
}
