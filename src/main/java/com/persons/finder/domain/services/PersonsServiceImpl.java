package com.persons.finder.domain.services;

import com.persons.finder.ai.AiBioService;
import com.persons.finder.data.Person;
import com.persons.finder.exception.PersonNotFoundException;
import com.persons.finder.domain.repository.PersonRepository;
import com.persons.finder.security.InputSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PersonsServiceImpl implements PersonsService {

    private final SequenceGeneratorService sequenceGeneratorService;
    private final PersonRepository personRepository;
    private final AiBioService aiBioService;
    private final InputSanitizer inputSanitizer;

    @Override
    public Person getById(Long id) {
        com.persons.finder.domain.Person person = personRepository.findById(id)
                .orElseThrow(() -> new PersonNotFoundException("Person not found with id: " + id));
        return new Person(person.getId(), person.getName(), person.getEmail(), person.getJobTitle(), person.getHobbies(), person.getBio());
    }

    @Override
    public Person save(Person person) {

        com.persons.finder.domain.Person domainPerson;

        if (person.getId() == null) {

            com.persons.finder.domain.Person existingPerson = personRepository.findByEmail(person.getEmail().trim().toLowerCase());
            if (existingPerson != null) {
                domainPerson = existingPerson;
            } else {
                long newId = sequenceGeneratorService.generateSequence(com.persons.finder.domain.Person.class.getSimpleName());
                domainPerson = com.persons.finder.domain.Person.builder()
                        .id(newId)
                        .build();
            }

        } else {
            domainPerson = personRepository.findById(person.getId())
                    .orElseThrow(() -> new PersonNotFoundException("Person not found with id: " + person.getId()));
        }

        domainPerson.setName(inputSanitizer.sanitize(person.getName()));
        domainPerson.setEmail(person.getEmail().trim().toLowerCase());
        domainPerson.setJobTitle(inputSanitizer.sanitize(person.getJobTitle()));
        domainPerson.setHobbies(inputSanitizer.sanitizeList(person.getHobbies()));
        domainPerson.setBio(aiBioService.generateBio(person.getJobTitle(), person.getHobbies()));

        domainPerson = personRepository.save(domainPerson);

        return new Person(domainPerson.getId(), domainPerson.getName(), domainPerson.getEmail(), domainPerson.getJobTitle(), domainPerson.getHobbies(), domainPerson.getBio());
    }
}
