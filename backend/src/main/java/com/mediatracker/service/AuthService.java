package com.mediatracker.service;

import com.mediatracker.model.dto.AuthUserDto;
import com.mediatracker.model.dto.OAuthSyncRequest;
import com.mediatracker.model.entity.AccountEntity;
import com.mediatracker.model.entity.UserEntity;
import com.mediatracker.model.entity.UserPrivacySettingsEntity;
import com.mediatracker.repository.AccountRepository;
import com.mediatracker.repository.UserPrivacySettingsRepository;
import com.mediatracker.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final UserPrivacySettingsRepository userPrivacySettingsRepository;

    public AuthService(UserRepository userRepository,
                       AccountRepository accountRepository,
                       UserPrivacySettingsRepository userPrivacySettingsRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.userPrivacySettingsRepository = userPrivacySettingsRepository;
    }

    @Transactional
    public AuthUserDto syncOAuthUser(OAuthSyncRequest request) {
        String provider = request.provider();
        String providerAccountId = request.providerAccountId();

        if (provider == null || provider.isBlank() || providerAccountId == null || providerAccountId.isBlank()) {
            throw new IllegalArgumentException("provider and providerAccountId are required");
        }

        // 1. Look up existing Account record
        Optional<AccountEntity> accountOpt = accountRepository.findByProviderAndProviderAccountId(provider, providerAccountId);
        UserEntity user = null;

        if (accountOpt.isPresent()) {
            user = userRepository.findById(accountOpt.get().getUserId()).orElse(null);
            if (user != null) {
                log.info("Found existing user {} for provider {} / {}", user.getId(), provider, providerAccountId);
            }
        }

        // 2. If no user linked via Account, attempt matching by email
        if (user == null && request.email() != null && !request.email().isBlank()) {
            user = userRepository.findByEmail(request.email().trim().toLowerCase()).orElse(null);
            if (user != null) {
                log.info("Linked user {} by email {}", user.getId(), request.email());
            }
        }

        // 3. Fallback: match by avatar image containing providerAccountId (e.g. Discord CDN avatar URLs)
        if (user == null) {
            user = userRepository.findAll().stream()
                    .filter(u -> u.getImage() != null && u.getImage().contains(providerAccountId))
                    .findFirst()
                    .orElse(null);
            if (user != null) {
                log.info("Linked user {} by image containing providerAccountId {}", user.getId(), providerAccountId);
            }
        }

        // 4. If user still does not exist, provision a new user
        if (user == null) {
            user = new UserEntity();
            user.setName(request.name());
            if (request.email() != null && !request.email().isBlank()) {
                user.setEmail(request.email().trim().toLowerCase());
            }
            user.setImage(request.image());
            user.setRole("user");
            user.setCreatedAt(OffsetDateTime.now());
            user = userRepository.save(user);

            // Default privacy settings
            UserPrivacySettingsEntity privacy = new UserPrivacySettingsEntity();
            privacy.setUserId(user.getId());
            privacy.setUpdatedAt(OffsetDateTime.now());
            userPrivacySettingsRepository.save(privacy);

            log.info("Created new user {} via OAuth provider {}", user.getId(), provider);
        } else {
            // Update profile fields if missing
            boolean updated = false;
            if (user.getImage() == null && request.image() != null) {
                user.setImage(request.image());
                updated = true;
            }
            if (user.getName() == null && request.name() != null) {
                user.setName(request.name());
                updated = true;
            }
            if (user.getEmail() == null && request.email() != null && !request.email().isBlank()) {
                user.setEmail(request.email().trim().toLowerCase());
                updated = true;
            }
            if (updated) {
                user = userRepository.save(user);
            }
        }

        // 5. Ensure Account entity exists linking this provider and user
        if (accountOpt.isEmpty()) {
            AccountEntity account = new AccountEntity();
            account.setId(UUID.randomUUID().toString());
            account.setUserId(user.getId());
            account.setProvider(provider);
            account.setProviderAccountId(providerAccountId);
            account.setType("oauth");
            accountRepository.save(account);
            log.info("Created Account link for user {} to {} / {}", user.getId(), provider, providerAccountId);
        }

        return toDto(user);
    }

    @Transactional(readOnly = true)
    public Optional<AuthUserDto> getUserDto(UUID userId) {
        if (userId == null) return Optional.empty();
        return userRepository.findById(userId).map(this::toDto);
    }

    public AuthUserDto toDto(UserEntity user) {
        return new AuthUserDto(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getEmail(),
                user.getName(),
                user.getImage()
        );
    }
}
