package com.example.alumni.service;

import com.example.alumni.client.PhantomBusterClient;
import com.example.alumni.dto.external.LinkedInProfileResult;
import com.example.alumni.dto.request.AlumniSearchRequest;
import com.example.alumni.dto.response.AlumniResponse;
import com.example.alumni.entity.Alumni;
import com.example.alumni.exception.PhantomBusterException;
import com.example.alumni.mapper.AlumniMapper;
import com.example.alumni.repository.AlumniRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AlumniServiceImpl unit tests")
class AlumniServiceImplTest {

    @Mock
    private PhantomBusterClient phantomBusterClient;

    @Mock
    private AlumniRepository alumniRepository;

    @Mock
    private AlumniMapper alumniMapper;

    @Mock
    private AlumniPersistenceService persistenceService;

    @InjectMocks
    private AlumniServiceImpl alumniService;

    private AlumniSearchRequest baseRequest;
    private LinkedInProfileResult sampleProfile;
    private Alumni sampleEntity;
    private AlumniResponse sampleResponse;

    @BeforeEach
    void setUp() {
        baseRequest = AlumniSearchRequest.builder()
                .university("University of XYZ")
                .designation("Software Engineer")
                .build();

        sampleProfile = new LinkedInProfileResult();
        sampleProfile.setFullName("John Doe");
        sampleProfile.setJob("Passionate Software Engineer at XYZ Corp");
        sampleProfile.setCurrentJob("Software Engineer");
        sampleProfile.setLocation("New York, NY");
        sampleProfile.setProfileUrl("https://www.linkedin.com/in/johndoe");

        sampleEntity = Alumni.builder()
                .id(1L)
                .name("John Doe")
                .currentRole("Software Engineer")
                .university("University of XYZ")
                .location("New York, NY")
                .linkedinHeadline("Passionate Software Engineer at XYZ Corp")
                .profileUrl("https://www.linkedin.com/in/johndoe")
                .build();

        sampleResponse = AlumniResponse.builder()
                .name("John Doe")
                .currentRole("Software Engineer")
                .university("University of XYZ")
                .location("New York, NY")
                .linkedinHeadline("Passionate Software Engineer at XYZ Corp")
                .build();
    }

    // -------------------------------------------------------------------------
    // searchAndSaveAlumni
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should return alumni profiles when PhantomBuster search succeeds")
    void shouldReturnAlumniProfilesWhenPhantomBusterSearchSucceeds() {
        // Arrange
        when(phantomBusterClient.searchAlumni(baseRequest))
                .thenReturn(List.of(sampleProfile));
        when(alumniRepository.findByProfileUrl("https://www.linkedin.com/in/johndoe"))
                .thenReturn(Optional.empty());
        when(alumniMapper.toEntity(sampleProfile, "University of XYZ", null))
                .thenReturn(sampleEntity);
        when(persistenceService.saveAll(anyList()))
                .thenReturn(List.of(sampleEntity));
        when(alumniMapper.toResponse(sampleEntity))
                .thenReturn(sampleResponse);

        // Act
        List<AlumniResponse> result = alumniService.searchAndSaveAlumni(baseRequest);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("John Doe");
        assertThat(result.get(0).getUniversity()).isEqualTo("University of XYZ");

        verify(phantomBusterClient).searchAlumni(baseRequest);
        verify(persistenceService).saveAll(anyList());
    }

    @Test
    @DisplayName("should persist and return multiple profiles when PhantomBuster returns multiple results")
    void shouldReturnMultipleProfilesWhenPhantomBusterReturnsMultipleResults() {
        // Arrange
        LinkedInProfileResult profile2 = new LinkedInProfileResult();
        profile2.setFullName("Jane Smith");
        profile2.setProfileUrl("https://www.linkedin.com/in/janesmith");

        Alumni entity2 = Alumni.builder().id(2L).name("Jane Smith").build();
        AlumniResponse response2 = AlumniResponse.builder().name("Jane Smith").build();

        when(phantomBusterClient.searchAlumni(baseRequest))
                .thenReturn(List.of(sampleProfile, profile2));
        when(alumniRepository.findByProfileUrl("https://www.linkedin.com/in/johndoe"))
                .thenReturn(Optional.empty());
        when(alumniRepository.findByProfileUrl("https://www.linkedin.com/in/janesmith"))
                .thenReturn(Optional.empty());
        when(alumniMapper.toEntity(eq(sampleProfile), any(), any())).thenReturn(sampleEntity);
        when(alumniMapper.toEntity(eq(profile2), any(), any())).thenReturn(entity2);
        when(persistenceService.saveAll(anyList())).thenReturn(List.of(sampleEntity, entity2));
        when(alumniMapper.toResponse(sampleEntity)).thenReturn(sampleResponse);
        when(alumniMapper.toResponse(entity2)).thenReturn(response2);

        // Act
        List<AlumniResponse> result = alumniService.searchAndSaveAlumni(baseRequest);

        // Assert
        assertThat(result).hasSize(2);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Alumni>> saveCaptor = ArgumentCaptor.forClass(List.class);
        verify(persistenceService).saveAll(saveCaptor.capture());
        assertThat(saveCaptor.getValue()).hasSize(2);
    }

    @Test
    @DisplayName("should return empty list when PhantomBuster returns no profiles")
    void shouldReturnEmptyListWhenPhantomBusterReturnsNoProfiles() {
        // Arrange
        when(phantomBusterClient.searchAlumni(baseRequest)).thenReturn(List.of());

        // Act
        List<AlumniResponse> result = alumniService.searchAndSaveAlumni(baseRequest);

        // Assert
        assertThat(result).isEmpty();
        verify(persistenceService, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("should propagate PhantomBusterException when client throws")
    void shouldPropagatePhantomBusterExceptionWhenClientThrows() {
        // Arrange
        when(phantomBusterClient.searchAlumni(baseRequest))
                .thenThrow(new PhantomBusterException("PhantomBuster timed out"));

        // Act & Assert
        assertThatThrownBy(() -> alumniService.searchAndSaveAlumni(baseRequest))
                .isInstanceOf(PhantomBusterException.class)
                .hasMessageContaining("timed out");

        verify(persistenceService, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("should skip duplicate profiles that already exist in the database")
    void shouldSkipDuplicateProfilesThatAlreadyExistInDatabase() {
        // Arrange — profile already exists in DB
        when(phantomBusterClient.searchAlumni(baseRequest))
                .thenReturn(List.of(sampleProfile));
        when(alumniRepository.findByProfileUrl("https://www.linkedin.com/in/johndoe"))
                .thenReturn(Optional.of(sampleEntity));

        // Act
        List<AlumniResponse> result = alumniService.searchAndSaveAlumni(baseRequest);

        // Assert — nothing saved, empty response
        assertThat(result).isEmpty();
        verify(persistenceService, never()).saveAll(anyList());
        verify(alumniMapper, never()).toEntity(any(), any(), any());
    }

    @Test
    @DisplayName("should persist profile without profileUrl without deduplication check")
    void shouldPersistProfileWithoutProfileUrlWithoutDeduplicationCheck() {
        // Arrange — profile has no URL (edge case)
        sampleProfile.setProfileUrl(null);

        when(phantomBusterClient.searchAlumni(baseRequest))
                .thenReturn(List.of(sampleProfile));
        when(alumniMapper.toEntity(sampleProfile, "University of XYZ", null))
                .thenReturn(sampleEntity);
        when(persistenceService.saveAll(anyList()))
                .thenReturn(List.of(sampleEntity));
        when(alumniMapper.toResponse(sampleEntity))
                .thenReturn(sampleResponse);

        // Act
        List<AlumniResponse> result = alumniService.searchAndSaveAlumni(baseRequest);

        // Assert — saved without hitting findByProfileUrl
        assertThat(result).hasSize(1);
        verify(alumniRepository, never()).findByProfileUrl(any());
        verify(persistenceService).saveAll(anyList());
    }

    @Test
    @DisplayName("should pass passoutYear to mapper when provided in request")
    void shouldPassPassoutYearToMapperWhenProvidedInRequest() {
        // Arrange
        AlumniSearchRequest requestWithYear = AlumniSearchRequest.builder()
                .university("University of XYZ")
                .designation("Software Engineer")
                .passoutYear(2020)
                .build();

        when(phantomBusterClient.searchAlumni(requestWithYear))
                .thenReturn(List.of(sampleProfile));
        when(alumniRepository.findByProfileUrl("https://www.linkedin.com/in/johndoe"))
                .thenReturn(Optional.empty());
        when(alumniMapper.toEntity(sampleProfile, "University of XYZ", 2020))
                .thenReturn(sampleEntity);
        when(persistenceService.saveAll(anyList()))
                .thenReturn(List.of(sampleEntity));
        when(alumniMapper.toResponse(sampleEntity))
                .thenReturn(sampleResponse);

        // Act
        alumniService.searchAndSaveAlumni(requestWithYear);

        // Assert — mapper receives the year from the request
        verify(alumniMapper).toEntity(sampleProfile, "University of XYZ", 2020);
    }

    // -------------------------------------------------------------------------
    // getAllAlumni
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should return all saved alumni profiles from the repository")
    void shouldReturnAllSavedAlumniProfilesFromRepository() {
        // Arrange
        when(alumniRepository.findAll()).thenReturn(List.of(sampleEntity));
        when(alumniMapper.toResponse(sampleEntity)).thenReturn(sampleResponse);

        // Act
        List<AlumniResponse> result = alumniService.getAllAlumni();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("John Doe");
        verify(alumniRepository).findAll();
    }

    @Test
    @DisplayName("should return empty list when no alumni are saved")
    void shouldReturnEmptyListWhenNoAlumniAreSaved() {
        // Arrange
        when(alumniRepository.findAll()).thenReturn(List.of());

        // Act
        List<AlumniResponse> result = alumniService.getAllAlumni();

        // Assert
        assertThat(result).isEmpty();
    }
}
