package com.persons.finder.domain.services;

import com.persons.finder.config.LocationConfig;
import com.persons.finder.data.Location;
import com.persons.finder.domain.Person;
import com.persons.finder.domain.repository.PersonRepository;
import com.persons.finder.exception.PersonNotFoundException;
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
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
    public void addLocation(Location location) {
        Person person = personRepository.findById(location.getReferenceId())
                .orElseThrow(() -> new PersonNotFoundException("Person not found with id: " + location.getReferenceId()));

        person.setLocation(new GeoJsonPoint(
                location.getLongitude(),
                location.getLatitude()
        ));

        personRepository.save(person);
        log.info("Location updated successfully");
    }

    @Override
    @Transactional
    public void removeLocation(String locationReferenceId) {
        Person person = personRepository.findById(locationReferenceId)
                .orElseThrow(() -> new PersonNotFoundException("Person not found with id: " + locationReferenceId));

        person.setLocation(null);

        personRepository.save(person);
        log.info("Location removed successfully");
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
        // GeoJsonPoint: getX() = longitude, getY() = latitude
        return results.getContent().stream()
                .map(geoResult -> {
                    Person person = geoResult.getContent();
                    double distanceKm = geoResult.getDistance().getValue();
                    return new Location(person.getId(), person.getLocation().getY(), person.getLocation().getX(), distanceKm, person.getBio());
                })
                .collect(Collectors.toList());
    }

}
