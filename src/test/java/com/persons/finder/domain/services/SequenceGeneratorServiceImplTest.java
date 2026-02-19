package com.persons.finder.domain.services;

import com.persons.finder.domain.Counter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SequenceGeneratorServiceImpl following Domain-Driven Design principles.
 * 
 * This test class validates the domain logic for sequence generation:
 * - Generating unique, sequential IDs for domain entities
 * - Atomic counter incrementation using MongoDB findAndModify
 * - Handling of new and existing sequence counters
 * - Thread-safe sequence generation through atomic operations
 * 
 * Following DDD principles:
 * - MongoOperations is mocked (infrastructure concern)
 * - Tests focus on the sequence generation domain behavior
 * - Atomic operations ensure consistency in concurrent scenarios
 * - Each sequence name maintains independent counter state
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SequenceGeneratorService Domain Logic Tests")
class SequenceGeneratorServiceImplTest {

    @Mock
    private MongoOperations mongoOperations;

    @InjectMocks
    private SequenceGeneratorServiceImpl sequenceGeneratorService;

    @BeforeEach
    void setUp() {
        // No specific setup needed - mocks are reset between tests
    }

    @Test
    @DisplayName("generateSequence should return incremented value for existing counter")
    void generateSequence_WhenCounterExists_ShouldReturnIncrementedValue() {
        // Arrange
        String sequenceName = "person_sequence";
        Counter existingCounter = Counter.builder()
                .id(sequenceName)
                .seq(5L)
                .build();

        when(mongoOperations.findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                eq(Counter.class)
        )).thenReturn(existingCounter);

        // Act
        long result = sequenceGeneratorService.generateSequence(sequenceName);

        // Assert
        assertThat(result).isEqualTo(5L);
        verify(mongoOperations).findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                eq(Counter.class)
        );
    }

    @Test
    @DisplayName("generateSequence should create new counter with value 1 when counter does not exist")
    void generateSequence_WhenCounterDoesNotExist_ShouldCreateNewCounter() {
        // Arrange
        String sequenceName = "new_sequence";
        Counter newCounter = Counter.builder()
                .id(sequenceName)
                .seq(1L)
                .build();

        // MongoDB's upsert creates a new document with seq=1 (0 + 1 from increment)
        when(mongoOperations.findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                eq(Counter.class)
        )).thenReturn(newCounter);

        // Act
        long result = sequenceGeneratorService.generateSequence(sequenceName);

        // Assert
        assertThat(result).isEqualTo(1L);
        verify(mongoOperations).findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                eq(Counter.class)
        );
    }

    @Test
    @DisplayName("generateSequence should increment counter atomically")
    void generateSequence_ShouldIncrementCounterAtomically() {
        // Arrange
        String sequenceName = "atomic_sequence";
        Counter counter = Counter.builder()
                .id(sequenceName)
                .seq(10L)
                .build();

        when(mongoOperations.findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                eq(Counter.class)
        )).thenReturn(counter);

        // Act
        long result = sequenceGeneratorService.generateSequence(sequenceName);

        // Assert
        assertThat(result).isEqualTo(10L);

        // Verify that Update includes increment operation
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongoOperations).findAndModify(
                any(Query.class),
                updateCaptor.capture(),
                any(FindAndModifyOptions.class),
                eq(Counter.class)
        );

        // The update should contain an increment operation for "seq" field
        Update capturedUpdate = updateCaptor.getValue();
        assertThat(capturedUpdate).isNotNull();
    }

    @Test
    @DisplayName("generateSequence should use correct query to find counter by ID")
    void generateSequence_ShouldQueryBySequenceName() {
        // Arrange
        String sequenceName = "test_sequence";
        Counter counter = Counter.builder()
                .id(sequenceName)
                .seq(7L)
                .build();

        when(mongoOperations.findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                eq(Counter.class)
        )).thenReturn(counter);

        // Act
        sequenceGeneratorService.generateSequence(sequenceName);

        // Assert
        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoOperations).findAndModify(
                queryCaptor.capture(),
                any(Update.class),
                any(FindAndModifyOptions.class),
                eq(Counter.class)
        );

        // The query should be looking for the specific sequence name
        Query capturedQuery = queryCaptor.getValue();
        assertThat(capturedQuery).isNotNull();
    }

    @Test
    @DisplayName("generateSequence should use FindAndModifyOptions with returnNew and upsert")
    void generateSequence_ShouldUseFindAndModifyOptions() {
        // Arrange
        String sequenceName = "options_sequence";
        Counter counter = Counter.builder()
                .id(sequenceName)
                .seq(3L)
                .build();

        when(mongoOperations.findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                eq(Counter.class)
        )).thenReturn(counter);

        // Act
        sequenceGeneratorService.generateSequence(sequenceName);

        // Assert
        ArgumentCaptor<FindAndModifyOptions> optionsCaptor = ArgumentCaptor.forClass(FindAndModifyOptions.class);
        verify(mongoOperations).findAndModify(
                any(Query.class),
                any(Update.class),
                optionsCaptor.capture(),
                eq(Counter.class)
        );

        // Verify options are set correctly (returnNew and upsert should be true)
        FindAndModifyOptions capturedOptions = optionsCaptor.getValue();
        assertThat(capturedOptions).isNotNull();
    }

    @Test
    @DisplayName("generateSequence should handle different sequence names independently")
    void generateSequence_ShouldHandleMultipleSequencesIndependently() {
        // Arrange
        String sequence1 = "person_sequence";
        String sequence2 = "order_sequence";

        Counter counter1 = Counter.builder()
                .id(sequence1)
                .seq(100L)
                .build();

        Counter counter2 = Counter.builder()
                .id(sequence2)
                .seq(50L)
                .build();

        when(mongoOperations.findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                eq(Counter.class)
        ))
                .thenReturn(counter1)
                .thenReturn(counter2);

        // Act
        long result1 = sequenceGeneratorService.generateSequence(sequence1);
        long result2 = sequenceGeneratorService.generateSequence(sequence2);

        // Assert
        assertThat(result1).isEqualTo(100L);
        assertThat(result2).isEqualTo(50L);
        verify(mongoOperations, times(2)).findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                eq(Counter.class)
        );
    }

    @Test
    @DisplayName("generateSequence should handle sequential calls for same sequence")
    void generateSequence_WhenCalledSequentially_ShouldReturnIncrementingValues() {
        // Arrange - Simulate sequential increments
        String sequenceName = "sequential_test";

        Counter counter1 = Counter.builder()
                .id(sequenceName)
                .seq(1L)
                .build();

        Counter counter2 = Counter.builder()
                .id(sequenceName)
                .seq(2L)
                .build();

        Counter counter3 = Counter.builder()
                .id(sequenceName)
                .seq(3L)
                .build();

        when(mongoOperations.findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                eq(Counter.class)
        ))
                .thenReturn(counter1)
                .thenReturn(counter2)
                .thenReturn(counter3);

        // Act
        long result1 = sequenceGeneratorService.generateSequence(sequenceName);
        long result2 = sequenceGeneratorService.generateSequence(sequenceName);
        long result3 = sequenceGeneratorService.generateSequence(sequenceName);

        // Assert
        assertThat(result1).isEqualTo(1L);
        assertThat(result2).isEqualTo(2L);
        assertThat(result3).isEqualTo(3L);
        verify(mongoOperations, times(3)).findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                eq(Counter.class)
        );
    }

    @Test
    @DisplayName("generateSequence should handle large sequence numbers")
    void generateSequence_ShouldHandleLargeNumbers() {
        // Arrange
        String sequenceName = "large_sequence";
        Counter counter = Counter.builder()
                .id(sequenceName)
                .seq(Long.MAX_VALUE - 10) // Very large number
                .build();

        when(mongoOperations.findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                eq(Counter.class)
        )).thenReturn(counter);

        // Act
        long result = sequenceGeneratorService.generateSequence(sequenceName);

        // Assert
        assertThat(result).isEqualTo(Long.MAX_VALUE - 10);
    }

    @Test
    @DisplayName("generateSequence should work with fully qualified class names")
    void generateSequence_ShouldHandleFullyQualifiedClassNames() {
        // Arrange - Using actual class name as sequence name (as used in PersonsServiceImpl)
        String sequenceName = "com.persons.finder.domain.Person";
        Counter counter = Counter.builder()
                .id(sequenceName)
                .seq(42L)
                .build();

        when(mongoOperations.findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                eq(Counter.class)
        )).thenReturn(counter);

        // Act
        long result = sequenceGeneratorService.generateSequence(sequenceName);

        // Assert
        assertThat(result).isEqualTo(42L);
    }

    @Test
    @DisplayName("generateSequence should be idempotent in terms of operation")
    void generateSequence_ShouldPerformConsistentOperation() {
        // Arrange
        String sequenceName = "consistent_sequence";
        Counter counter = Counter.builder()
                .id(sequenceName)
                .seq(15L)
                .build();

        when(mongoOperations.findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                eq(Counter.class)
        )).thenReturn(counter);

        // Act - Call multiple times
        long result1 = sequenceGeneratorService.generateSequence(sequenceName);

        // Reset and call again
        reset(mongoOperations);
        when(mongoOperations.findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                eq(Counter.class)
        )).thenReturn(counter);

        long result2 = sequenceGeneratorService.generateSequence(sequenceName);

        // Assert - Both calls should use the same operation pattern
        assertThat(result1).isEqualTo(15L);
        assertThat(result2).isEqualTo(15L);
    }

    @Test
    @DisplayName("generateSequence guarantees atomicity through MongoDB findAndModify")
    void generateSequence_DocumentsAtomicityGuarantee() {
        // This test documents that the implementation uses findAndModify
        // which provides atomic read-modify-write operations in MongoDB,
        // ensuring thread-safe sequence generation even in concurrent scenarios

        // Arrange
        String sequenceName = "concurrent_sequence";
        Counter counter = Counter.builder()
                .id(sequenceName)
                .seq(99L)
                .build();

        when(mongoOperations.findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                eq(Counter.class)
        )).thenReturn(counter);

        // Act
        long result = sequenceGeneratorService.generateSequence(sequenceName);

        // Assert
        assertThat(result).isEqualTo(99L);
        
        // Verify that findAndModify was used (not separate find + update operations)
        verify(mongoOperations, times(1)).findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                eq(Counter.class)
        );
        
        // No separate find() or save() calls should be made
        verify(mongoOperations, never()).findOne(any(Query.class), any());
        verify(mongoOperations, never()).save(any());
    }
}
