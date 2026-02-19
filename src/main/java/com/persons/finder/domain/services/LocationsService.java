package com.persons.finder.domain.services;

import com.persons.finder.data.Location;
import java.util.List;

public interface LocationsService {
    void addLocation(Location location);
    void removeLocation(Long locationReferenceId);
    List<Location> findAround(Double latitude, Double longitude, Double radiusInKm);
}
