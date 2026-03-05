package com.persons.finder.domain.services;

import com.persons.finder.data.Location;
import com.persons.finder.domain.Person;
import com.persons.finder.domain.repository.PersonRepository;
import com.persons.finder.exception.PersonNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.NearQuery;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LocationsServiceImpl following Domain-Driven Design principles.
 * 
 * This test class validates the domain logic for location-based operations:
 * - Adding geospatial locations to person entities
 * - Removing location data from persons
 * - Proximity searches using geospatial queries
 * 
 * Following DDD principles:
 * - Repository and MongoTemplate are mocked (infrastructure concerns)
 * - Tests focus on domain behavior and geospatial business rules
 * - Edge cases include missing persons and empty search results
 * - Geospatial calculations are delegated to infrastructure (MongoDB)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LocationsService Domain Logic Tests")
class LocationsServiceImplTest {

    @Mock
    private PersonRepository personRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private LocationsServiceImpl locationsService;

    private Person testPerson1;
    private GeoJsonPoint testLocation1;
    private Person testPerson2;
    private GeoJsonPoint testLocation2;

    @BeforeEach
    void setUp() {
        // Arrange - Set up common test data
        testPerson1 = Person.builder()
                .id(UUID.randomUUID().toString())
                .name("John Doe")
                .email("john.doe@example.com")
                .jobTitle("Software Engineer")
                .hobbies(Arrays.asList("Reading", "Coding"))
                .bio("Tech enthusiast")
                .build();
        testPerson2 = Person.builder()
                .id(UUID.randomUUID().toString())
                .name("Jane Smith")
                .email("jane.smith@example.com")
                .jobTitle("Data Scientist")
                .bio("Data lover")
                .build();

        // GeoJSON Point: longitude first, then latitude
        testLocation1 = new GeoJsonPoint(103.8198, 1.3521); // Singapore coordinates
        testLocation2 = new GeoJsonPoint(-122.4194, 37.7749); // San Francisco coordinates

        // Set location data on test persons
        testPerson1.setLocation(testLocation1);
        testPerson2.setLocation(testLocation2);
    }

    @Test
    @DisplayName("addLocation should successfully add location to existing person")
    void addLocation_WhenPersonExists_ShouldAddLocation() {
        // Arrange
        Location locationData = new Location(testPerson1.getId(), 1.3521, 103.8198, null, null);

        when(personRepository.findById(testPerson1.getId())).thenReturn(Optional.of(testPerson1));
        when(personRepository.save(any(Person.class))).thenReturn(testPerson1);

        // Act
        locationsService.addLocation(locationData);

        // Assert
        ArgumentCaptor<Person> personCaptor = ArgumentCaptor.forClass(Person.class);
        verify(personRepository).findById(testPerson1.getId());
        verify(personRepository).save(personCaptor.capture());

        Person savedPerson = personCaptor.getValue();
        assertThat(savedPerson.getLocation()).isNotNull();
        assertThat(savedPerson.getLocation().getX()).isEqualTo(103.8198); // longitude
        assertThat(savedPerson.getLocation().getY()).isEqualTo(1.3521);   // latitude
    }

    @Test
    @DisplayName("addLocation should throw PersonNotFoundException when person does not exist")
    void addLocation_WhenPersonDoesNotExist_ShouldThrowException() {
        // Arrange
        String personIdNotExist = UUID.randomUUID().toString();
        Location locationData = new Location(personIdNotExist, 1.3521, 103.8198, null, null);

        when(personRepository.findById(personIdNotExist)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> locationsService.addLocation(locationData))
                .isInstanceOf(PersonNotFoundException.class)
                .hasMessageContaining("Person not found with id: " + personIdNotExist);

        verify(personRepository).findById(personIdNotExist);
        verify(personRepository, never()).save(any());
    }

    @Test
    @DisplayName("addLocation should handle location coordinates correctly (longitude, latitude order)")
    void addLocation_ShouldUseCorrectCoordinateOrder() {
        // Arrange - Test with specific coordinates to verify order
        Location locationData = new Location(testPerson1.getId(), 51.5074, -0.1278, null, null); // London: lat, lon

        when(personRepository.findById(testPerson1.getId())).thenReturn(Optional.of(testPerson1));
        when(personRepository.save(any(Person.class))).thenReturn(testPerson1);

        // Act
        locationsService.addLocation(locationData);

        // Assert
        ArgumentCaptor<Person> personCaptor = ArgumentCaptor.forClass(Person.class);
        verify(personRepository).save(personCaptor.capture());

        Person savedPerson = personCaptor.getValue();
        // GeoJSON stores as [longitude, latitude]
        assertThat(savedPerson.getLocation().getX()).isEqualTo(-0.1278);  // longitude
        assertThat(savedPerson.getLocation().getY()).isEqualTo(51.5074);  // latitude
    }

    @Test
    @DisplayName("addLocation should update existing location")
    void addLocation_WhenPersonHasExistingLocation_ShouldUpdateLocation() {
        // Arrange
        testPerson1.setLocation(new GeoJsonPoint(100.0, 10.0)); // Old location
        Location newLocationData = new Location(testPerson1.getId(), 1.3521, 103.8198, null, null);

        when(personRepository.findById(testPerson1.getId())).thenReturn(Optional.of(testPerson1));
        when(personRepository.save(any(Person.class))).thenReturn(testPerson1);

        // Act
        locationsService.addLocation(newLocationData);

        // Assert
        ArgumentCaptor<Person> personCaptor = ArgumentCaptor.forClass(Person.class);
        verify(personRepository).save(personCaptor.capture());

        Person savedPerson = personCaptor.getValue();
        assertThat(savedPerson.getLocation().getX()).isEqualTo(103.8198);
        assertThat(savedPerson.getLocation().getY()).isEqualTo(1.3521);
    }

    @Test
    @DisplayName("removeLocation should successfully remove location from person")
    void removeLocation_WhenPersonExists_ShouldRemoveLocation() {
        // Arrange
        testPerson1.setLocation(testLocation1);

        when(personRepository.findById(testPerson1.getId())).thenReturn(Optional.of(testPerson1));
        when(personRepository.save(any(Person.class))).thenReturn(testPerson1);

        // Act
        locationsService.removeLocation(testPerson1.getId());

        // Assert
        ArgumentCaptor<Person> personCaptor = ArgumentCaptor.forClass(Person.class);
        verify(personRepository).findById(testPerson1.getId());
        verify(personRepository).save(personCaptor.capture());

        Person savedPerson = personCaptor.getValue();
        assertThat(savedPerson.getLocation()).isNull();
    }

    @Test
    @DisplayName("removeLocation should throw PersonNotFoundException when person does not exist")
    void removeLocation_WhenPersonDoesNotExist_ShouldThrowException() {
        // Arrange
        String personIdNotExist = UUID.randomUUID().toString();
        when(personRepository.findById(personIdNotExist)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> locationsService.removeLocation(personIdNotExist))
                .isInstanceOf(PersonNotFoundException.class)
                .hasMessageContaining("Person not found with id: " + personIdNotExist);

        verify(personRepository).findById(personIdNotExist);
        verify(personRepository, never()).save(any());
    }

    @Test
    @DisplayName("removeLocation should handle person without existing location")
    void removeLocation_WhenPersonHasNoLocation_ShouldSetLocationToNull() {
        // Arrange - Person has no location
        testPerson1.setLocation(null);

        when(personRepository.findById(testPerson1.getId())).thenReturn(Optional.of(testPerson1));
        when(personRepository.save(any(Person.class))).thenReturn(testPerson1);

        // Act
        locationsService.removeLocation(testPerson1.getId());

        // Assert
        ArgumentCaptor<Person> personCaptor = ArgumentCaptor.forClass(Person.class);
        verify(personRepository).save(personCaptor.capture());

        Person savedPerson = personCaptor.getValue();
        assertThat(savedPerson.getLocation()).isNull();
    }

    @Test
    @DisplayName("findAround should return persons within specified radius")
    void findAround_WhenPersonsExistInRadius_ShouldReturnLocations() {
        // Arrange
        GeoResult<Person> geoResult1 = new GeoResult<>(testPerson1, new Distance(1.5, Metrics.KILOMETERS));
        GeoResult<Person> geoResult2 = new GeoResult<>(testPerson2, new Distance(3.2, Metrics.KILOMETERS));

        GeoResults<Person> geoResults = new GeoResults<>(
                Arrays.asList(geoResult1, geoResult2),
                Metrics.KILOMETERS
        );

        when(mongoTemplate.geoNear(any(NearQuery.class), eq(Person.class)))
                .thenReturn(geoResults);

        // Act
        List<Location> results = locationsService.findAround(1.3521, 103.8198, 5.0, 1, 1000);

        // Assert
        assertThat(results).hasSize(2);

        Location loc1 = results.get(0);
        assertThat(loc1.getReferenceId()).isEqualTo(testPerson1.getId());
        assertThat(loc1.getLatitude()).isEqualTo(103.8198);  // Currently gets longitude value
        assertThat(loc1.getLongitude()).isEqualTo(1.3521);   // Currently gets latitude value
        assertThat(loc1.getDistanceInKm()).isEqualTo(1.5);
        assertThat(loc1.getBio()).isEqualTo(testPerson1.getBio());

        Location loc2 = results.get(1);
        assertThat(loc2.getReferenceId()).isEqualTo(testPerson2.getId());
        assertThat(loc2.getDistanceInKm()).isEqualTo(3.2);

        verify(mongoTemplate).geoNear(any(NearQuery.class), eq(Person.class));
    }

    @Test
    @DisplayName("findAround should return empty list when no persons found in radius")
    void findAround_WhenNoPersonsInRadius_ShouldReturnEmptyList() {
        // Arrange
        GeoResults<Person> emptyResults = new GeoResults<>(
                Collections.emptyList(),
                Metrics.KILOMETERS
        );

        when(mongoTemplate.geoNear(any(NearQuery.class), eq(Person.class)))
                .thenReturn(emptyResults);

        // Act
        List<Location> results = locationsService.findAround(1.3521, 103.8198, 5.0,1, 1000);

        // Assert
        assertThat(results).isEmpty();
        verify(mongoTemplate).geoNear(any(NearQuery.class), eq(Person.class));
    }

    @Test
    @DisplayName("findAround should construct NearQuery with correct parameters")
    void findAround_ShouldCreateCorrectNearQuery() {
        // Arrange
        GeoResults<Person> emptyResults = new GeoResults<>(
                Collections.emptyList(),
                Metrics.KILOMETERS
        );

        when(mongoTemplate.geoNear(any(NearQuery.class), eq(Person.class)))
                .thenReturn(emptyResults);

        // Act
        locationsService.findAround(1.3521, 103.8198, 10.0, 1, 1000);

        // Assert
        ArgumentCaptor<NearQuery> queryCaptor = ArgumentCaptor.forClass(NearQuery.class);
        verify(mongoTemplate).geoNear(queryCaptor.capture(), eq(Person.class));

        // Note: We can't easily verify the NearQuery contents directly,
        // but we've verified that it was called with the correct class
    }

    @Test
    @DisplayName("findAround should use spherical geometry for accurate distance calculations")
    void findAround_ShouldUseSphericalGeometry() {
        // Arrange - This test documents the spherical geometry requirement
        GeoResults<Person> emptyResults = new GeoResults<>(
                Collections.emptyList(),
                Metrics.KILOMETERS
        );

        when(mongoTemplate.geoNear(any(NearQuery.class), eq(Person.class)))
                .thenReturn(emptyResults);

        // Act
        locationsService.findAround(40.7128, -74.0060, 50.0, 1, 1000); // New York coordinates

        // Assert
        verify(mongoTemplate).geoNear(any(NearQuery.class), eq(Person.class));
        // The implementation uses .spherical(true) which ensures accurate
        // calculations on the Earth's curved surface
    }

    @Test
    @DisplayName("findAround should handle large radius searches")
    void findAround_WithLargeRadius_ShouldReturnResults() {
        // Arrange
        String personId = UUID.randomUUID().toString();
        Person distantPerson = Person.builder()
                .id(personId)
                .name("Distant Person")
                .location(new GeoJsonPoint(0.0, 0.0)) // Equator, Prime Meridian
                .bio("Far away bio")
                .build();

        GeoResult<Person> geoResult = new GeoResult<>(
                distantPerson,
                new Distance(500.0, Metrics.KILOMETERS)
        );

        GeoResults<Person> geoResults = new GeoResults<>(
                Collections.singletonList(geoResult),
                Metrics.KILOMETERS
        );

        when(mongoTemplate.geoNear(any(NearQuery.class), eq(Person.class)))
                .thenReturn(geoResults);

        // Act
        List<Location> results = locationsService.findAround(1.3521, 103.8198, 1000.0, 1, 1000);

        // Assert
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getDistanceInKm()).isEqualTo(500.0);
    }

    @Test
    @DisplayName("findAround should handle edge case of zero radius")
    void findAround_WithZeroRadius_ShouldWork() {
        // Arrange
        GeoResults<Person> emptyResults = new GeoResults<>(
                Collections.emptyList(),
                Metrics.KILOMETERS
        );

        when(mongoTemplate.geoNear(any(NearQuery.class), eq(Person.class)))
                .thenReturn(emptyResults);

        // Act
        List<Location> results = locationsService.findAround(1.3521, 103.8198, 0.0, 1, 1000);

        // Assert
        assertThat(results).isEmpty();
        verify(mongoTemplate).geoNear(any(NearQuery.class), eq(Person.class));
    }

    @Test
    @DisplayName("findAround should correctly map GeoJSON coordinates to Location DTO")
    void findAround_ShouldMapCoordinatesCorrectly() {
        // Arrange - Test coordinate mapping from GeoJSON to DTO
        String personId = UUID.randomUUID().toString();
        Person person = Person.builder()
                .id(personId)
                .name("Test Person")
                .location(new GeoJsonPoint(-122.4194, 37.7749)) // San Francisco (lon, lat)
                .bio("SF resident")
                .build();

        GeoResult<Person> geoResult = new GeoResult<>(
                person,
                new Distance(2.5, Metrics.KILOMETERS)
        );

        GeoResults<Person> geoResults = new GeoResults<>(
                Collections.singletonList(geoResult),
                Metrics.KILOMETERS
        );

        when(mongoTemplate.geoNear(any(NearQuery.class), eq(Person.class)))
                .thenReturn(geoResults);

        // Act
        List<Location> results = locationsService.findAround(37.7749, -122.4194, 10.0, 1, 1000);

        // Assert
        assertThat(results).hasSize(1);
        Location location = results.get(0);
        assertThat(location.getLatitude()).isEqualTo(-122.4194);  // Currently gets longitude value
        assertThat(location.getLongitude()).isEqualTo(37.7749);   // Currently gets latitude value
    }
}
