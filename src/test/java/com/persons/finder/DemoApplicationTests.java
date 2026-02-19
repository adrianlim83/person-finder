package com.persons.finder;

import com.persons.finder.domain.services.LocationsService;
import com.persons.finder.domain.services.PersonsService;
import com.persons.finder.presentation.PersonController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PersonController.class)
public class DemoApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PersonsService personsService;

    @MockBean
    private LocationsService locationsService;

    @Test
    void contextLoads() {
    }

}
