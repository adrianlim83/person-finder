package com.persons.finder.domain.services;

import com.persons.finder.data.Person;

public interface PersonsService {
    Person getById(Long id);
    Person save(Person person);
}
