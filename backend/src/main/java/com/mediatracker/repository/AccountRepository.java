package com.mediatracker.repository;

import com.mediatracker.model.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<AccountEntity, String> {
    Optional<AccountEntity> findByProviderAndProviderAccountId(String provider, String providerAccountId);
    List<AccountEntity> findByUserId(UUID userId);
}
