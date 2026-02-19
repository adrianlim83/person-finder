package com.persons.finder.data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class Person {

    private Long id;
    @NotBlank(message = "Name is required")
    @Size(max = 500, message = "Name must not exceed 500 characters")
    private final String name;
    @NotBlank(message = "Email is required")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private final String email;
    @NotBlank(message = "Job title is required")
    @Size(max = 500, message = "Job title must not exceed 500 characters")
    private final String jobTitle;
    @Size(max = 20, message = "Maximum 20 hobbies allowed")
    @NotEmpty(message = "Hobbies are required")
    private List<@NotBlank @Size(max = 500) String> hobbies;
    private final String bio;

}
