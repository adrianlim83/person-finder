package com.persons.finder.presentation;

import com.persons.finder.data.Location;
import com.persons.finder.data.Person;
import com.persons.finder.domain.services.LocationsService;
import com.persons.finder.domain.services.PersonsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/persons")
@RequiredArgsConstructor
@Validated
public class PersonController {

    private final PersonsService personsService;
    private final LocationsService locationsService;

    @PostMapping
    public ResponseEntity<Person> createOrUpdatePerson(@Valid @RequestBody Person request) {
        return ResponseEntity.ok(personsService.save(request));
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PutMapping("/{id}/location")
    public void updateLocation(
        @PathVariable String id,
        @Valid @RequestBody Location request
    ) {
        request.setReferenceId(id);
        locationsService.addLocation(request);
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<Location>> findNearby(
        @RequestParam @NotNull @Min(-90) @Max(90) Double lat,
        @RequestParam @NotNull @Min(-180) @Max(180) Double lon,
        @RequestParam @NotNull @Min(0) @Max(20000) Double radiusInKm,
        @RequestParam(required = false) @Min(1) Integer page,
        @RequestParam(required = false) @Min(1) @Max(1000) Integer limit
    ) {
        List<Location> locations = locationsService.findAround(lat, lon, radiusInKm, page, limit);
        return ResponseEntity.ok(locations);
    }
}
