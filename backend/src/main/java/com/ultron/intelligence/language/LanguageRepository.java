package com.ultron.intelligence.language;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LanguageRepository extends JpaRepository<Language, UUID> {

    Optional<Language> findByLanguageCode(String languageCode);

    List<Language> findByEnabledTrueOrderByDisplayNameAsc();

    boolean existsByLanguageCode(String languageCode);
}
