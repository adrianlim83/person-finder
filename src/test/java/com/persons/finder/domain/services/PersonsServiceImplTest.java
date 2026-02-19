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
    private SequenceGeneratorService sequenceGeneratorService;

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
        hobbies = Arrays.asList("Reading", "Gaming", "Coding");
        
        domainPerson = com.persons.finder.domain.Person.builder()
                .id(1L)
                .name("John Doe")
                .email("john.doe@example.com")
                .jobTitle("Software Engineer")
                .hobbies(hobbies)
                .bio("Generated bio content")
                .build();

        dataPerson = new Person(
                1L,
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
        when(personRepository.findById(1L)).thenReturn(Optional.of(domainPerson));

        // Act
        Person result = personsService.getById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("John Doe");
        assertThat(result.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(result.getJobTitle()).isEqualTo("Software Engineer");
        assertThat(result.getHobbies()).containsExactlyElementsOf(hobbies);
        assertThat(result.getBio()).isEqualTo("Generated bio content");

        verify(personRepository).findById(1L);
    }

    @Test
    @DisplayName("getById should throw PersonNotFoundException when person does not exist")
    void getById_WhenPersonDoesNotExist_ShouldThrowException() {
        // Arrange
        when(personRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> personsService.getById(999L))
                .isInstanceOf(PersonNotFoundException.class)
                .hasMessageContaining("Person not found with id: 999");

        verify(personRepository).findById(999L);
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
        when(sequenceGeneratorService.generateSequence(com.persons.finder.domain.Person.class.getSimpleName()))
                .thenReturn(2L);
        when(inputSanitizer.sanitize("Jane Smith")).thenReturn("Jane Smith");
        when(inputSanitizer.sanitize("Product Manager")).thenReturn("Product Manager");
        when(inputSanitizer.sanitizeList(anyList())).thenReturn(Arrays.asList("Yoga", "Travel"));
        when(aiBioService.generateBio("Jane Smith", Arrays.asList("Yoga", "Travel")))
                .thenReturn("AI generated bio for Jane");

        com.persons.finder.domain.Person savedDomainPerson = com.persons.finder.domain.Person.builder()
                .id(2L)
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
        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getName()).isEqualTo("Jane Smith");
        assertThat(result.getEmail()).isEqualTo("jane.smith@example.com");
        assertThat(result.getBio()).isEqualTo("AI generated bio for Jane");

        verify(personRepository).findByEmail("jane.smith@example.com");
        verify(sequenceGeneratorService).generateSequence(com.persons.finder.domain.Person.class.getSimpleName());
        verify(inputSanitizer).sanitize("Jane Smith");
        verify(inputSanitizer).sanitize("Product Manager");
        verify(inputSanitizer).sanitizeList(anyList());
        verify(aiBioService).generateBio("Jane Smith", Arrays.asList("Yoga", "Travel"));
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
                .id(1L)
                .name("John Doe")
                .email("john.doe@example.com")
                .jobTitle("Software Engineer")
                .hobbies(Arrays.asList("Reading"))
                .bio("Old bio")
                .build();

        when(personRepository.findByEmail("john.doe@example.com"))
                .thenReturn(existingDomainPerson);
        when(inputSanitizer.sanitize("John Doe Updated")).thenReturn("John Doe Updated");
        when(inputSanitizer.sanitize("Senior Software Engineer")).thenReturn("Senior Software Engineer");
        when(inputSanitizer.sanitizeList(anyList())).thenReturn(Arrays.asList("Reading", "Running"));
        when(aiBioService.generateBio("John Doe Updated", Arrays.asList("Reading", "Running")))
                .thenReturn("Updated bio content");

        com.persons.finder.domain.Person updatedDomainPerson = com.persons.finder.domain.Person.builder()
                .id(1L)
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
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("John Doe Updated");
        assertThat(result.getJobTitle()).isEqualTo("Senior Software Engineer");
        assertThat(result.getBio()).isEqualTo("Updated bio content");

        verify(personRepository).findByEmail("john.doe@example.com");
        verify(sequenceGeneratorService, never()).generateSequence(anyString());
        verify(personRepository).save(any(com.persons.finder.domain.Person.class));
    }

    @Test
    @DisplayName("save should update person when ID is provided and person exists")
    void save_WhenPersonWithIdExists_ShouldUpdatePerson() {
        // Arrange
        Person updatePerson = new Person(
                1L,
                "John Doe Updated",
                "john.updated@example.com",
                "Tech Lead",
                Arrays.asList("Reading", "Teaching"),
                null
        );

        when(personRepository.findById(1L)).thenReturn(Optional.of(domainPerson));
        when(inputSanitizer.sanitize("John Doe Updated")).thenReturn("John Doe Updated");
        when(inputSanitizer.sanitize("Tech Lead")).thenReturn("Tech Lead");
        when(inputSanitizer.sanitizeList(anyList())).thenReturn(Arrays.asList("Reading", "Teaching"));
        when(aiBioService.generateBio("John Doe Updated", Arrays.asList("Reading", "Teaching")))
                .thenReturn("Updated bio for tech lead");

        com.persons.finder.domain.Person updatedDomainPerson = com.persons.finder.domain.Person.builder()
                .id(1L)
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
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("John Doe Updated");
        assertThat(result.getEmail()).isEqualTo("john.updated@example.com");
        assertThat(result.getJobTitle()).isEqualTo("Tech Lead");
        assertThat(result.getBio()).isEqualTo("Updated bio for tech lead");

        verify(personRepository).findById(1L);
        verify(personRepository, never()).findByEmail(anyString());
        verify(sequenceGeneratorService, never()).generateSequence(anyString());
        verify(personRepository).save(any(com.persons.finder.domain.Person.class));
    }

    @Test
    @DisplayName("save should throw PersonNotFoundException when updating non-existent person")
    void save_WhenPersonWithIdDoesNotExist_ShouldThrowException() {
        // Arrange
        Person updatePerson = new Person(
                999L,
                "Non Existent",
                "nonexistent@example.com",
                "Developer",
                Arrays.asList("Coding"),
                null
        );

        when(personRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> personsService.save(updatePerson))
                .isInstanceOf(PersonNotFoundException.class)
                .hasMessageContaining("Person not found with id: 999");

        verify(personRepository).findById(999L);
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
        when(sequenceGeneratorService.generateSequence(anyString())).thenReturn(3L);
        when(inputSanitizer.sanitize("User with <script>")).thenReturn("User with ");
        when(inputSanitizer.sanitize("Job with injection attempt")).thenReturn("Job with  attempt");
        when(inputSanitizer.sanitizeList(anyList())).thenReturn(Arrays.asList("Hobby1", "Hobby2"));
        when(aiBioService.generateBio(anyString(), anyList())).thenReturn("Safe bio");

        com.persons.finder.domain.Person savedPerson = com.persons.finder.domain.Person.builder()
                .id(3L)
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
        when(sequenceGeneratorService.generateSequence(anyString())).thenReturn(4L);
        when(inputSanitizer.sanitize(anyString())).thenAnswer(i -> i.getArgument(0));
        when(inputSanitizer.sanitizeList(anyList())).thenAnswer(i -> i.getArgument(0));
        when(aiBioService.generateBio(anyString(), anyList())).thenReturn("Test bio");

        com.persons.finder.domain.Person savedPerson = com.persons.finder.domain.Person.builder()
                .id(4L)
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
        when(sequenceGeneratorService.generateSequence(anyString())).thenReturn(5L);
        when(inputSanitizer.sanitize(anyString())).thenAnswer(i -> i.getArgument(0));
        when(inputSanitizer.sanitizeList(anyList())).thenAnswer(i -> i.getArgument(0));
        when(aiBioService.generateBio("AI Test User", Arrays.asList("AI", "ML", "Deep Learning")))
                .thenReturn("Expert in AI, ML, and Deep Learning with passion for innovation");

        com.persons.finder.domain.Person savedPerson = com.persons.finder.domain.Person.builder()
                .id(5L)
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
        verify(aiBioService).generateBio("AI Test User", Arrays.asList("AI", "ML", "Deep Learning"));
    }
}
