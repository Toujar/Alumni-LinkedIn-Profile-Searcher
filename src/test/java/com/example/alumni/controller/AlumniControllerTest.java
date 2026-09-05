package com.example.alumni.controller;

import com.example.alumni.dto.response.AlumniResponse;
import com.example.alumni.exception.PhantomBusterException;
import com.example.alumni.service.AlumniService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller slice tests using MockMvc.
 *
 * These tests verify HTTP behaviour — routing, validation, status codes, and
 * response shape — without starting a full application context or touching
 * the database. The service is mocked via @MockitoBean.
 */
@WebMvcTest(AlumniController.class)
@DisplayName("AlumniController MockMvc tests")
class AlumniControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AlumniService alumniService;

    @Autowired
    private ObjectMapper objectMapper;

    // -------------------------------------------------------------------------
    // POST /api/alumni/search — happy path
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should return 200 OK with alumni data on valid search request")
    void shouldReturn200WithAlumniDataOnValidSearchRequest() throws Exception {
        // Arrange
        AlumniResponse response = AlumniResponse.builder()
                .name("John Doe")
                .currentRole("Software Engineer")
                .university("University of XYZ")
                .location("New York, NY")
                .linkedinHeadline("Passionate Software Engineer at XYZ Corp")
                .passoutYear(2020)
                .build();

        when(alumniService.searchAndSaveAlumni(any())).thenReturn(List.of(response));

        String requestBody = """
                {
                    "university": "University of XYZ",
                    "designation": "Software Engineer",
                    "passoutYear": 2020
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/alumni/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("John Doe"))
                .andExpect(jsonPath("$.data[0].university").value("University of XYZ"))
                .andExpect(jsonPath("$.data[0].passoutYear").value(2020));
    }

    @Test
    @DisplayName("should return 200 OK with empty data list when PhantomBuster finds no profiles")
    void shouldReturn200WithEmptyDataWhenNoProfilesFound() throws Exception {
        // Arrange
        when(alumniService.searchAndSaveAlumni(any())).thenReturn(List.of());

        String requestBody = """
                {
                    "university": "Unknown University",
                    "designation": "Rare Role"
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/alumni/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    // -------------------------------------------------------------------------
    // POST /api/alumni/search — validation failures
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should return 400 Bad Request when university is missing")
    void shouldReturn400WhenUniversityIsMissing() throws Exception {
        String requestBody = """
                {
                    "designation": "Software Engineer"
                }
                """;

        mockMvc.perform(post("/api/alumni/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(containsString("University name is required")));
    }

    @Test
    @DisplayName("should return 400 Bad Request when designation is missing")
    void shouldReturn400WhenDesignationIsMissing() throws Exception {
        String requestBody = """
                {
                    "university": "University of XYZ"
                }
                """;

        mockMvc.perform(post("/api/alumni/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(containsString("Designation is required")));
    }

    @Test
    @DisplayName("should return 400 Bad Request when university is blank")
    void shouldReturn400WhenUniversityIsBlank() throws Exception {
        String requestBody = """
                {
                    "university": "   ",
                    "designation": "Software Engineer"
                }
                """;

        mockMvc.perform(post("/api/alumni/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    @DisplayName("should return 400 Bad Request when passoutYear is below minimum (1950)")
    void shouldReturn400WhenPassoutYearIsBelowMinimum() throws Exception {
        String requestBody = """
                {
                    "university": "University of XYZ",
                    "designation": "Software Engineer",
                    "passoutYear": 1900
                }
                """;

        mockMvc.perform(post("/api/alumni/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(containsString("Pass-out year must be 1950 or later")));
    }

    @Test
    @DisplayName("should return 400 Bad Request when passoutYear exceeds maximum (2100)")
    void shouldReturn400WhenPassoutYearExceedsMaximum() throws Exception {
        String requestBody = """
                {
                    "university": "University of XYZ",
                    "designation": "Software Engineer",
                    "passoutYear": 2200
                }
                """;

        mockMvc.perform(post("/api/alumni/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(containsString("Pass-out year must be 2100 or earlier")));
    }

    @Test
    @DisplayName("should accept request without passoutYear (optional field)")
    void shouldAcceptRequestWithoutPassoutYear() throws Exception {
        // Arrange
        when(alumniService.searchAndSaveAlumni(any())).thenReturn(List.of());

        String requestBody = """
                {
                    "university": "University of XYZ",
                    "designation": "Software Engineer"
                }
                """;

        // Act & Assert — passoutYear omitted → still valid
        mockMvc.perform(post("/api/alumni/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    // -------------------------------------------------------------------------
    // POST /api/alumni/search — upstream failure
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should return 502 Bad Gateway when PhantomBuster client throws")
    void shouldReturn502WhenPhantomBusterClientThrows() throws Exception {
        // Arrange
        when(alumniService.searchAndSaveAlumni(any()))
                .thenThrow(new PhantomBusterException("Connection refused"));

        String requestBody = """
                {
                    "university": "University of XYZ",
                    "designation": "Software Engineer"
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/alumni/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(containsString("Connection refused")));
    }

    // -------------------------------------------------------------------------
    // GET /api/alumni/all
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should return 200 OK with all saved alumni profiles")
    void shouldReturn200WithAllSavedAlumniProfiles() throws Exception {
        // Arrange
        AlumniResponse response = AlumniResponse.builder()
                .name("John Doe")
                .university("University of XYZ")
                .currentRole("Software Engineer")
                .build();

        when(alumniService.getAllAlumni()).thenReturn(List.of(response));

        // Act & Assert
        mockMvc.perform(get("/api/alumni/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("John Doe"));
    }

    @Test
    @DisplayName("should return 200 OK with empty list when no alumni are saved")
    void shouldReturn200WithEmptyListWhenNoAlumniSaved() throws Exception {
        // Arrange
        when(alumniService.getAllAlumni()).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/alumni/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }
}
