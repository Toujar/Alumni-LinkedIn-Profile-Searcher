package com.example.alumni.exception;

import com.example.alumni.controller.AlumniController;
import com.example.alumni.service.AlumniService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that {@link GlobalExceptionHandler} translates exceptions into the
 * correct HTTP status codes and response body structure.
 *
 * The same {@link AlumniController} slice is reused so we test the full
 * filter → controller → advice pipeline without a real service.
 */
@WebMvcTest(AlumniController.class)
@DisplayName("GlobalExceptionHandler tests")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AlumniService alumniService;

    private static final String VALID_SEARCH_BODY = """
            {
                "university": "University of XYZ",
                "designation": "Software Engineer"
            }
            """;

    // -------------------------------------------------------------------------
    // Validation → 400 Bad Request
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("MethodArgumentNotValidException maps to 400 with error body")
    void validationExceptionMapsto400() throws Exception {
        // Sending an empty body triggers validation failure
        mockMvc.perform(post("/api/alumni/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("validation error response contains timestamp field")
    void validationErrorResponseContainsTimestamp() throws Exception {
        mockMvc.perform(post("/api/alumni/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // -------------------------------------------------------------------------
    // PhantomBusterException → 502 Bad Gateway
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("PhantomBusterException maps to 502 with error body")
    void phantomBusterExceptionMapsto502() throws Exception {
        when(alumniService.searchAndSaveAlumni(any()))
                .thenThrow(new PhantomBusterException("Upstream timeout"));

        mockMvc.perform(post("/api/alumni/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_SEARCH_BODY))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(containsString("Upstream timeout")));
    }

    @Test
    @DisplayName("PhantomBusterException response does not expose stack trace")
    void phantomBusterExceptionResponseDoesNotExposeStackTrace() throws Exception {
        when(alumniService.searchAndSaveAlumni(any()))
                .thenThrow(new PhantomBusterException("Some internal detail"));

        mockMvc.perform(post("/api/alumni/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_SEARCH_BODY))
                .andExpect(status().isBadGateway())
                // stackTrace field must not be present
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    // -------------------------------------------------------------------------
    // AlumniDataException → 500 Internal Server Error
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("AlumniDataException maps to 500 with error body")
    void alumniDataExceptionMapsto500() throws Exception {
        when(alumniService.searchAndSaveAlumni(any()))
                .thenThrow(new AlumniDataException("Data processing failed"));

        mockMvc.perform(post("/api/alumni/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_SEARCH_BODY))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(containsString("Data processing failed")));
    }

    // -------------------------------------------------------------------------
    // DataAccessException → 500 Internal Server Error
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("DataAccessException maps to 500 with generic message")
    void dataAccessExceptionMapsto500WithGenericMessage() throws Exception {
        when(alumniService.getAllAlumni())
                .thenThrow(new DataIntegrityViolationException("constraint violation"));

        mockMvc.perform(get("/api/alumni/all"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("error"))
                // Sensitive DB details must not be forwarded to the client
                .andExpect(jsonPath("$.message").value(containsString("database error")));
    }

    // -------------------------------------------------------------------------
    // Wrong HTTP method → 405 Method Not Allowed
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET on a POST-only endpoint maps to 405 Method Not Allowed")
    void getOnPostOnlyEndpointMapsto405() throws Exception {
        // GET /api/alumni/search is not a valid route — only POST is
        mockMvc.perform(get("/api/alumni/search"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value("error"));
    }

    // -------------------------------------------------------------------------
    // Unexpected exception → 500 Internal Server Error
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("unexpected RuntimeException maps to 500 with generic message")
    void unexpectedRuntimeExceptionMapsto500() throws Exception {
        when(alumniService.searchAndSaveAlumni(any()))
                .thenThrow(new RuntimeException("Something unexpected"));

        mockMvc.perform(post("/api/alumni/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_SEARCH_BODY))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(containsString("unexpected error")));
    }
}
