package com.persons.finder.domain.services;

import com.persons.finder.config.LocationConfig;
import com.persons.finder.data.Location;
import com.persons.finder.domain.Person;
import com.persons.finder.domain.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.NearQuery;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationsServiceImpl implements LocationsService {
    private final LocationConfig locationConfig;
    private final PersonRepository personRepository;
    private final MongoTemplate mongoTemplate;

    @Override
    public void addLocation(Location location) {
        Person person = personRepository.findById(location.getReferenceId())
                .orElseThrow(() -> new IllegalArgumentException("Person not found with id: " + location.getReferenceId()));

        person.setLocation(new GeoJsonPoint(
                location.getLongitude(),
                location.getLatitude()
        ));

        Person updated = personRepository.save(person);
        log.info("Updated location for person with id: {}", updated.getId());
    }

    @Override
    public void removeLocation(Long locationReferenceId) {
        Person person = personRepository.findById(locationReferenceId)
                .orElseThrow(() -> new IllegalArgumentException("Person not found with id: " + locationReferenceId));

        person.setLocation(null);

        Person updated = personRepository.save(person);
        log.info("Removed location for person with id: {}", updated.getId());
    }

    @Override
    public List<Location> findAround(Double latitude, Double longitude, Double radiusInKm, Integer page, Integer limit) {
        // Mongo expects (longitude, latitude)
        Point point = new Point(longitude, latitude);

        int size = limit != null ? limit : locationConfig.getNearbyLimit();
        Pageable pageable = PageRequest.of(page != null ? page - 1 : 0, size);

        NearQuery nearQuery = NearQuery.near(point)
                .maxDistance(new Distance(radiusInKm, Metrics.KILOMETERS))
                .spherical(true)
                .with(pageable);

        GeoResults<Person> results = mongoTemplate.geoNear(nearQuery, Person.class);

        // Map results to DTO with distance
        return results.getContent().stream()
                .map(geoResult -> {
                    Person person = geoResult.getContent();
                    double distanceKm = geoResult.getDistance().getValue();
                    return new Location(person.getId(), person.getLocation().getX(), person.getLocation().getY(), distanceKm, person.getBio());
                })
                .collect(Collectors.toList());
    }

}
