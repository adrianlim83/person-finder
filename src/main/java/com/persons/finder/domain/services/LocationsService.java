package com.persons.finder.domain.services;

import com.persons.finder.data.Location;
import java.util.List;

public interface LocationsService {
    void addLocation(Location location);
    void removeLocation(String locationReferenceId);
    List<Location> findAround(Double latitude, Double longitude, Double radiusInKm, Integer page, Integer limit);
}
