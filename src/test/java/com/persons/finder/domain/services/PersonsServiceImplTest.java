package com.persons.finder.domain.services;

import com.persons.finder.ai.AiBioService;
import com.persons.finder.data.Person;
import com.persons.finder.domain.repository.PersonRepository;
import com.persons.finder.exception.PersonNotFoundException;
import com.persons.finder.security.InputSanitizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PersonsServiceImpl following Domain-Driven Design principles.
 * 
 * This test class validates the domain logic encapsulated in PersonsService,
 * ensuring proper behavior of core domain operations such as:
 * - Person retrieval with proper exception handling
 * - Person creation with duplicate email detection
 * - Person updates with validation
 * - Integration with infrastructure services (sanitization, AI bio generation)
 * 
 * Following DDD principles:
 * - All dependencies are mocked (repositories, infrastructure services)
 * - Tests focus on domain behavior, not implementation details
 * - Edge cases and domain rules are explicitly tested
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PersonsService Domain Logic Tests")
class PersonsServiceImplTest {

    @Mock
    private PersonRepository personRepository;

    @Mock
    private AiBioService aiBioService;

    @Mock
    private InputSanitizer inputSanitizer;

    @InjectMocks
    private PersonsServiceImpl personsService;

    private com.persons.finder.domain.Person domainPerson;
    private Person dataPerson;
    private List<String> hobbies;

    @BeforeEach
    void setUp() {
        // Arrange - Set up common test data
        String personId = UUID.randomUUID().toString();
        hobbies = Arrays.asList("Reading", "Gaming", "Coding");
        
        domainPerson = com.persons.finder.domain.Person.builder()
                .id(personId)
                .name("John Doe")
                .email("john.doe@example.com")
                .jobTitle("Software Engineer")
                .hobbies(hobbies)
                .bio("Generated bio content")
                .build();

        dataPerson = new Person(
                personId,
                "John Doe",
                "john.doe@example.com",
                "Software Engineer",
                hobbies,
                "Generated bio content"
        );
    }

    @Test
    @DisplayName("getById should return person when person exists")
    void getById_WhenPersonExists_ShouldReturnPerson() {
        // Arrange
        when(personRepository.findById(dataPerson.getId())).thenReturn(Optional.of(domainPerson));

        // Act
        Person result = personsService.getById(dataPerson.getId());

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(domainPerson.getId());
        assertThat(result.getName()).isEqualTo(domainPerson.getName());
        assertThat(result.getEmail()).isEqualTo(domainPerson.getEmail());
        assertThat(result.getJobTitle()).isEqualTo(domainPerson.getJobTitle());
        assertThat(result.getHobbies()).containsExactlyElementsOf(domainPerson.getHobbies());
        assertThat(result.getBio()).isEqualTo(domainPerson.getBio());

        verify(personRepository).findById(domainPerson.getId());
    }

    @Test
    @DisplayName("getById should throw PersonNotFoundException when person does not exist")
    void getById_WhenPersonDoesNotExist_ShouldThrowException() {
        // Arrange
        String personIdNotExist = UUID.randomUUID().toString();
        when(personRepository.findById(personIdNotExist)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> personsService.getById(personIdNotExist))
                .isInstanceOf(PersonNotFoundException.class)
                .hasMessageContaining("Person not found with id: " + personIdNotExist);

        verify(personRepository).findById(personIdNotExist);
    }

    @Test
    @DisplayName("save should create new person when ID is null and email does not exist")
    void save_WhenNewPersonWithNoExistingEmail_ShouldCreatePerson() {
        // Arrange - New person without ID
        Person newPerson = new Person(
                null,
                "Jane Smith",
                "jane.smith@example.com",
                "Product Manager",
                Arrays.asList("Yoga", "Travel"),
                null
        );

        when(personRepository.findByEmail("jane.smith@example.com"))
                .thenReturn(null);
        when(inputSanitizer.sanitize("Jane Smith")).thenReturn("Jane Smith");
        when(inputSanitizer.sanitize("Product Manager")).thenReturn("Product Manager");
        when(inputSanitizer.sanitizeList(anyList())).thenReturn(Arrays.asList("Yoga", "Travel"));
        when(aiBioService.generateBio("Product Manager", Arrays.asList("Yoga", "Travel")))
                .thenReturn("AI generated bio for Jane");

        com.persons.finder.domain.Person savedDomainPerson = com.persons.finder.domain.Person.builder()
                .id(UUID.randomUUID().toString())
                .name("Jane Smith")
                .email("jane.smith@example.com")
                .jobTitle("Product Manager")
                .hobbies(Arrays.asList("Yoga", "Travel"))
                .bio("AI generated bio for Jane")
                .build();

        when(personRepository.save(any(com.persons.finder.domain.Person.class)))
                .thenReturn(savedDomainPerson);

        // Act
        Person result = personsService.save(newPerson);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(savedDomainPerson.getId());
        assertThat(result.getName()).isEqualTo(savedDomainPerson.getName());
        assertThat(result.getEmail()).isEqualTo(savedDomainPerson.getEmail());
        assertThat(result.getBio()).isEqualTo(savedDomainPerson.getBio());

        verify(personRepository).findByEmail(savedDomainPerson.getEmail());
        verify(inputSanitizer).sanitize(savedDomainPerson.getName());
        verify(inputSanitizer).sanitize(savedDomainPerson.getJobTitle());
        verify(inputSanitizer).sanitizeList(anyList());
        verify(aiBioService).generateBio(savedDomainPerson.getJobTitle(), savedDomainPerson.getHobbies());
        verify(personRepository).save(any(com.persons.finder.domain.Person.class));
    }

    @Test
    @DisplayName("save should update existing person when ID is null but email exists")
    void save_WhenNewPersonWithExistingEmail_ShouldUpdateExistingPerson() {
        // Arrange - New person data with existing email
        Person personData = new Person(
                null,
                "John Doe Updated",
                "john.doe@example.com",
                "Senior Software Engineer",
                Arrays.asList("Reading", "Running"),
                null
        );

        // Existing person in database
        com.persons.finder.domain.Person existingDomainPerson = com.persons.finder.domain.Person.builder()
                .id(UUID.randomUUID().toString())
                .name("John Doe")
                .email("john.doe@example.com")
                .jobTitle("Software Engineer")
                .hobbies(Arrays.asList("Reading"))
                .bio("Old bio")
                .build();

        when(personRepository.findByEmail(existingDomainPerson.getEmail()))
                .thenReturn(existingDomainPerson);
        when(inputSanitizer.sanitize(personData.getName())).thenReturn(personData.getName());
        when(inputSanitizer.sanitize(personData.getJobTitle())).thenReturn(personData.getJobTitle());
        when(inputSanitizer.sanitizeList(anyList())).thenReturn(personData.getHobbies());
        when(aiBioService.generateBio(personData.getJobTitle(), personData.getHobbies()))
                .thenReturn("Updated bio content");

        com.persons.finder.domain.Person updatedDomainPerson = com.persons.finder.domain.Person.builder()
                .id(existingDomainPerson.getId())
                .name("John Doe Updated")
                .email("john.doe@example.com")
                .jobTitle("Senior Software Engineer")
                .hobbies(Arrays.asList("Reading", "Running"))
                .bio("Updated bio content")
                .build();

        when(personRepository.save(any(com.persons.finder.domain.Person.class)))
                .thenReturn(updatedDomainPerson);

        // Act
        Person result = personsService.save(personData);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(existingDomainPerson.getId());
        assertThat(result.getName()).isEqualTo("John Doe Updated");
        assertThat(result.getJobTitle()).isEqualTo("Senior Software Engineer");
        assertThat(result.getBio()).isEqualTo("Updated bio content");

        verify(personRepository).findByEmail("john.doe@example.com");
        verify(personRepository).save(any(com.persons.finder.domain.Person.class));
    }

    @Test
    @DisplayName("save should update person when ID is provided and person exists")
    void save_WhenPersonWithIdExists_ShouldUpdatePerson() {
        // Arrange
        Person updatePerson = new Person(
                domainPerson.getId(),
                "John Doe Updated",
                "john.updated@example.com",
                "Tech Lead",
                Arrays.asList("Reading", "Teaching"),
                null
        );

        when(personRepository.findById(domainPerson.getId())).thenReturn(Optional.of(domainPerson));
        when(inputSanitizer.sanitize(updatePerson.getName())).thenReturn("John Doe Updated");
        when(inputSanitizer.sanitize(updatePerson.getJobTitle())).thenReturn("Tech Lead");
        when(inputSanitizer.sanitizeList(anyList())).thenReturn(Arrays.asList("Reading", "Teaching"));
        when(aiBioService.generateBio(updatePerson.getJobTitle(), updatePerson.getHobbies()))
                .thenReturn("Updated bio for tech lead");

        com.persons.finder.domain.Person updatedDomainPerson = com.persons.finder.domain.Person.builder()
                .id(domainPerson.getId())
                .name("John Doe Updated")
                .email("john.updated@example.com")
                .jobTitle("Tech Lead")
                .hobbies(Arrays.asList("Reading", "Teaching"))
                .bio("Updated bio for tech lead")
                .build();

        when(personRepository.save(any(com.persons.finder.domain.Person.class)))
                .thenReturn(updatedDomainPerson);

        // Act
        Person result = personsService.save(updatePerson);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(domainPerson.getId());
        assertThat(result.getName()).isEqualTo(updatedDomainPerson.getName());
        assertThat(result.getEmail()).isEqualTo(updatedDomainPerson.getEmail());
        assertThat(result.getJobTitle()).isEqualTo(updatedDomainPerson.getJobTitle());
        assertThat(result.getBio()).isEqualTo(updatedDomainPerson.getBio());

        verify(personRepository).findById(domainPerson.getId());
        verify(personRepository, never()).findByEmail(anyString());
        verify(personRepository).save(any(com.persons.finder.domain.Person.class));
    }

    @Test
    @DisplayName("save should throw PersonNotFoundException when updating non-existent person")
    void save_WhenPersonWithIdDoesNotExist_ShouldThrowException() {
        // Arrange
        String personIdNotExist = UUID.randomUUID().toString();
        Person updatePerson = new Person(
                personIdNotExist,
                "Non Existent",
                "nonexistent@example.com",
                "Developer",
                Arrays.asList("Coding"),
                null
        );

        when(personRepository.findById(personIdNotExist)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> personsService.save(updatePerson))
                .isInstanceOf(PersonNotFoundException.class)
                .hasMessageContaining("Person not found with id: " + personIdNotExist);

        verify(personRepository).findById(personIdNotExist);
        verify(personRepository, never()).save(any());
    }

    @Test
    @DisplayName("save should properly sanitize all input fields")
    void save_ShouldSanitizeInputFields() {
        // Arrange
        Person unsanitizedPerson = new Person(
                null,
                "User with <script>",
                "user@example.com",
                "Job with injection attempt",
                Arrays.asList("Hobby1", "Hobby2"),
                null
        );

        when(personRepository.findByEmail("user@example.com")).thenReturn(null);
        when(inputSanitizer.sanitize("User with <script>")).thenReturn("User with ");
        when(inputSanitizer.sanitize("Job with injection attempt")).thenReturn("Job with  attempt");
        when(inputSanitizer.sanitizeList(anyList())).thenReturn(Arrays.asList("Hobby1", "Hobby2"));
        when(aiBioService.generateBio(anyString(), anyList())).thenReturn("Safe bio");

        com.persons.finder.domain.Person savedPerson = com.persons.finder.domain.Person.builder()
                .id(UUID.randomUUID().toString())
                .name("User with ")
                .email("user@example.com")
                .jobTitle("Job with  attempt")
                .hobbies(Arrays.asList("Hobby1", "Hobby2"))
                .bio("Safe bio")
                .build();

        when(personRepository.save(any(com.persons.finder.domain.Person.class))).thenReturn(savedPerson);

        // Act
        Person result = personsService.save(unsanitizedPerson);

        // Assert
        assertThat(result.getName()).isEqualTo("User with ");
        assertThat(result.getJobTitle()).isEqualTo("Job with  attempt");

        verify(inputSanitizer).sanitize("User with <script>");
        verify(inputSanitizer).sanitize("Job with injection attempt");
        verify(inputSanitizer).sanitizeList(anyList());
    }

    @Test
    @DisplayName("save should normalize email to lowercase and trim whitespace")
    void save_ShouldNormalizeEmail() {
        // Arrange
        Person personWithUppercaseEmail = new Person(
                null,
                "Test User",
                "  TEST.User@EXAMPLE.COM  ",
                "Tester",
                Arrays.asList("Testing"),
                null
        );

        when(personRepository.findByEmail("test.user@example.com")).thenReturn(null);
        when(inputSanitizer.sanitize(anyString())).thenAnswer(i -> i.getArgument(0));
        when(inputSanitizer.sanitizeList(anyList())).thenAnswer(i -> i.getArgument(0));
        when(aiBioService.generateBio(anyString(), anyList())).thenReturn("Test bio");

        com.persons.finder.domain.Person savedPerson = com.persons.finder.domain.Person.builder()
                .id(UUID.randomUUID().toString())
                .name("Test User")
                .email("test.user@example.com")
                .jobTitle("Tester")
                .hobbies(Arrays.asList("Testing"))
                .bio("Test bio")
                .build();

        when(personRepository.save(any(com.persons.finder.domain.Person.class))).thenReturn(savedPerson);

        // Act
        Person result = personsService.save(personWithUppercaseEmail);

        // Assert
        assertThat(result.getEmail()).isEqualTo("test.user@example.com");
        verify(personRepository).findByEmail("test.user@example.com");
    }

    @Test
    @DisplayName("save should integrate with AI bio service to generate bio")
    void save_ShouldGenerateBioUsingAiService() {
        // Arrange
        Person newPerson = new Person(
                null,
                "AI Test User",
                "ai.test@example.com",
                "AI Researcher",
                Arrays.asList("AI", "ML", "Deep Learning"),
                null
        );

        when(personRepository.findByEmail("ai.test@example.com")).thenReturn(null);
        when(inputSanitizer.sanitize(anyString())).thenAnswer(i -> i.getArgument(0));
        when(inputSanitizer.sanitizeList(anyList())).thenAnswer(i -> i.getArgument(0));
        when(aiBioService.generateBio("AI Researcher", Arrays.asList("AI", "ML", "Deep Learning")))
                .thenReturn("Expert in AI, ML, and Deep Learning with passion for innovation");

        com.persons.finder.domain.Person savedPerson = com.persons.finder.domain.Person.builder()
                .id(UUID.randomUUID().toString())
                .name("AI Test User")
                .email("ai.test@example.com")
                .jobTitle("AI Researcher")
                .hobbies(Arrays.asList("AI", "ML", "Deep Learning"))
                .bio("Expert in AI, ML, and Deep Learning with passion for innovation")
                .build();

        when(personRepository.save(any(com.persons.finder.domain.Person.class))).thenReturn(savedPerson);

        // Act
        Person result = personsService.save(newPerson);

        // Assert
        assertThat(result.getBio()).isEqualTo("Expert in AI, ML, and Deep Learning with passion for innovation");
        verify(aiBioService).generateBio("AI Researcher", Arrays.asList("AI", "ML", "Deep Learning"));
    }
}
