package com.persons.finder.presentation;

import com.persons.finder.data.Location;
import com.persons.finder.data.Person;
import com.persons.finder.domain.services.LocationsService;
import com.persons.finder.domain.services.PersonsService;
import com.persons.finder.exception.PersonNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * API tests for PersonController that start a real embedded HTTP server and
 * exercise the full request/response pipeline including serialization,
 * deserialization, validation, and exception handling.
 *
 * <p>These tests complement the unit tests in {@code PersonsServiceImplTest} and
 * {@code LocationsServiceImplTest} by verifying the HTTP layer end-to-end.
 * They run automatically as part of {@code mvn clean verify} and are included
 * in the "Build & run unit, integration & API tests" step of the CI/CD pipeline
 * defined in {@code .github/workflows/aws.yml}.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "ai.provider=mock",
                "ai.openai.api-key=test-api-key",
                "ai.prompt=Test bio for %s who loves %s"
        })
@DisplayName("PersonController API Tests")
class PersonControllerApiTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private PersonsService personsService;

    @MockBean
    private LocationsService locationsService;

    // ─── POST /api/v1/persons ────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/v1/persons - valid request returns 200 with created person")
    void createPerson_ValidRequest_Returns200() {
        Person request = new Person(null, "Alice Smith", "alice@example.com",
                "Engineer", Arrays.asList("Coding", "Reading"), null);
        Person saved = new Person("abc123", "Alice Smith", "alice@example.com",
                "Engineer", Arrays.asList("Coding", "Reading"), "Generated bio");
        when(personsService.save(any(Person.class))).thenReturn(saved);

        ResponseEntity<Person> response = restTemplate.postForEntity(
                "/api/v1/persons", request, Person.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo("abc123");
        assertThat(response.getBody().getName()).isEqualTo("Alice Smith");
    }

    @Test
    @DisplayName("POST /api/v1/persons - missing name returns 400")
    void createPerson_MissingName_Returns400() {
        Person request = new Person(null, "", "alice@example.com",
                "Engineer", Arrays.asList("Coding"), null);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/persons", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("POST /api/v1/persons - missing email returns 400")
    void createPerson_MissingEmail_Returns400() {
        Person request = new Person(null, "Alice Smith", "",
                "Engineer", Arrays.asList("Coding"), null);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/persons", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("POST /api/v1/persons - missing job title returns 400")
    void createPerson_MissingJobTitle_Returns400() {
        Person request = new Person(null, "Alice Smith", "alice@example.com",
                "", Arrays.asList("Coding"), null);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/persons", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("POST /api/v1/persons - missing hobbies returns 400")
    void createPerson_MissingHobbies_Returns400() {
        Person request = new Person(null, "Alice Smith", "alice@example.com",
                "Engineer", List.of(), null);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/persons", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ─── PUT /api/v1/persons/{id}/location ───────────────────────────────────

    @Test
    @DisplayName("PUT /api/v1/persons/{id}/location - valid request returns 204")
    void updateLocation_ValidRequest_Returns204() {
        doNothing().when(locationsService).addLocation(any(Location.class));
        Location request = new Location(null, 1.3521, 103.8198, null, null);

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/persons/abc123/location",
                HttpMethod.PUT,
                new HttpEntity<>(request),
                Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("PUT /api/v1/persons/{id}/location - missing latitude returns 400")
    void updateLocation_MissingLatitude_Returns400() {
        Location request = new Location(null, null, 103.8198, null, null);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/persons/abc123/location",
                HttpMethod.PUT,
                new HttpEntity<>(request),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("PUT /api/v1/persons/{id}/location - missing longitude returns 400")
    void updateLocation_MissingLongitude_Returns400() {
        Location request = new Location(null, 1.3521, null, null, null);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/persons/abc123/location",
                HttpMethod.PUT,
                new HttpEntity<>(request),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("PUT /api/v1/persons/{id}/location - person not found returns 404")
    void updateLocation_PersonNotFound_Returns404() {
        doThrow(new PersonNotFoundException("Person not found with id: unknown"))
                .when(locationsService).addLocation(any(Location.class));
        Location request = new Location(null, 1.3521, 103.8198, null, null);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/persons/unknown/location",
                HttpMethod.PUT,
                new HttpEntity<>(request),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ─── GET /api/v1/persons/nearby ──────────────────────────────────────────

    @Test
    @DisplayName("GET /api/v1/persons/nearby - valid request returns 200 with list")
    void findNearby_ValidRequest_Returns200() {
        List<Location> nearbyLocations = Arrays.asList(
                new Location("p1", 1.3521, 103.8198, 0.5, "Bio 1"),
                new Location("p2", 1.3600, 103.8300, 1.2, "Bio 2"));
        when(locationsService.findAround(1.3521, 103.8198, 5.0, null, null))
                .thenReturn(nearbyLocations);

        ResponseEntity<Location[]> response = restTemplate.getForEntity(
                "/api/v1/persons/nearby?lat=1.3521&lon=103.8198&radiusInKm=5.0",
                Location[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull().hasSize(2);
    }

    @Test
    @DisplayName("GET /api/v1/persons/nearby - latitude out of range returns 400")
    void findNearby_LatitudeOutOfRange_Returns400() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/persons/nearby?lat=91.0&lon=103.8198&radiusInKm=5.0",
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("GET /api/v1/persons/nearby - longitude out of range returns 400")
    void findNearby_LongitudeOutOfRange_Returns400() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/persons/nearby?lat=1.3521&lon=181.0&radiusInKm=5.0",
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("GET /api/v1/persons/nearby - radiusInKm out of range returns 400")
    void findNearby_RadiusOutOfRange_Returns400() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/persons/nearby?lat=1.3521&lon=103.8198&radiusInKm=25000.0",
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("GET /api/v1/persons/nearby - missing lat returns 400")
    void findNearby_MissingLat_Returns400() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/persons/nearby?lon=103.8198&radiusInKm=5.0",
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
