package com.example.alumni.service;

import com.example.alumni.entity.Alumni;
import com.example.alumni.repository.AlumniRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Handles all transactional database writes for alumni profiles.
 *
 * Extracted into its own Spring bean so that the {@link Transactional}
 * annotation is honoured by Spring's AOP proxy. If this logic lived inside
 * {@link AlumniServiceImpl} and was called via {@code this.method()}, Spring's
 * proxy would not intercept the call and the annotation would be silently
 * ignored.
 *
 * This class has no business logic — it exists purely to own the transaction
 * boundary around the batch insert.
 */
@Service
public class AlumniPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(AlumniPersistenceService.class);

    private final AlumniRepository alumniRepository;

    public AlumniPersistenceService(AlumniRepository alumniRepository) {
        this.alumniRepository = alumniRepository;
    }

    /**
     * Persists a batch of new alumni entities in a single transaction.
     *
     * A failure rolls back all inserts in the batch, leaving the table in a
     * consistent state. Each call is independent — there is no enclosing
     * transaction from the caller (the PhantomBuster HTTP call happens before
     * this method is invoked, so no database connection is held open during
     * the network round-trip).
     *
     * @param alumni list of new, non-persisted entities
     * @return the saved entities with generated IDs
     */
    @Transactional
    public List<Alumni> saveAll(List<Alumni> alumni) {
        List<Alumni> saved = alumniRepository.saveAll(alumni);
        log.info("Persisted {} new alumni profile(s).", saved.size());
        return saved;
    }
}
