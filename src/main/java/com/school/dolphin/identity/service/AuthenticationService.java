
package com.school.whale.identity.service;

import com.school.whale.common.exception.InvalidCredentialsException;
import com.school.whale.common.exception.InvalidRefreshTokenException;
import com.school.whale.identity.config.JwtProperties;
import com.school.whale.identity.dto.AccessContextResponse;
import com.school.whale.identity.dto.LoginRequest;
import com.school.whale.identity.dto.LoginResponse;
import com.school.whale.identity.dto.RefreshTokenRequest;
import com.school.whale.identity.entity.UserAccount;
import com.school.whale.identity.entity.UserRefreshToken;
import com.school.whale.identity.repository.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Locale;

@Service
public class AuthenticationService {

    private static final String TOKEN_TYPE = "Bearer";

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final LoginAttemptService loginAttemptService;
    private final AccessContextService accessContextService;
    private final RefreshTokenService refreshTokenService;

    /**
     * A valid encoded hash used only to equalize the work done for unknown
     * usernames, so response timing does not reveal whether an account exists.
     */
    private final String dummyPasswordHash;

    public AuthenticationService(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            JwtProperties jwtProperties,
            LoginAttemptService loginAttemptService,
            AccessContextService accessContextService,
            RefreshTokenService refreshTokenService
    ) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.loginAttemptService = loginAttemptService;
        this.accessContextService = accessContextService;
        this.refreshTokenService = refreshTokenService;
        this.dummyPasswordHash = passwordEncoder.encode(
                "whale-nonexistent-account-timing-guard");
    }

    @Transactional
    public LoginResponse authenticate(LoginRequest request) {

        String attemptKey = request.username() == null
                ? ""
                : request.username().trim().toLowerCase(Locale.ROOT);

        loginAttemptService.assertNotLocked(attemptKey);

        UserAccount user = userAccountRepository
                .findByUsername(request.username())
                .orElse(null);

        boolean passwordMatches = passwordEncoder.matches(
                request.password(),
                user != null ? user.getPasswordHash() : dummyPasswordHash);

        if (user == null || !user.isActive() || !passwordMatches) {
            loginAttemptService.recordFailure(attemptKey);

            // A single generic message prevents username/account enumeration.
            throw new InvalidCredentialsException(
                    "Invalid username or password");
        }

        loginAttemptService.reset(attemptKey);

        OffsetDateTime now = OffsetDateTime.now();

        AccessContextResponse accessContext =
                accessContextService.getAccessContext(user);

        String accessToken = jwtService.generateAccessToken(
                user.getId(),
                user.getUsername(),
                accessContext.roles(),
                accessContext.permissions()
        );

        RefreshTokenService.IssuedRefreshToken refreshToken =
                refreshTokenService.issueNewSession(user, now);

        return response(accessToken, refreshToken, user, accessContext);
    }

    /**
     * Rotates a refresh session and issues a new access token.
     *
     * The refresh row remains locked for the duration of the transaction.
     * Current account status and effective authorization data are reloaded
     * before issuing the replacement tokens.
     *
     * noRollbackFor ensures replay-detection family revocation remains
     * committed when this method rejects a reused refresh token.
     */
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public LoginResponse refresh(RefreshTokenRequest request) {

        OffsetDateTime now = OffsetDateTime.now();

        UserRefreshToken current = refreshTokenService
                .lockByRawToken(request.refreshToken())
                .orElseThrow(() -> new InvalidRefreshTokenException(
                        "Refresh token is invalid"));

        if (current.isRevoked()) {
            // A reused token may indicate theft. Revoke its entire family.
            refreshTokenService.revokeFamily(current.getFamilyId(), now);

            throw new InvalidRefreshTokenException(
                    "Refresh token has already been used");
        }

        if (current.isExpired(now)) {
            throw new InvalidRefreshTokenException(
                    "Refresh token has expired");
        }

        UserAccount user = current.getUser();

        if (!user.isActive()) {
            refreshTokenService.revokeAllForUser(user.getId(), now);

            throw new InvalidRefreshTokenException(
                    "Refresh session is no longer valid");
        }

        // Resolve current roles, permissions and scopes from persisted data.
        AccessContextResponse accessContext =
                accessContextService.getAccessContext(user);

        RefreshTokenService.IssuedRefreshToken rotated =
                refreshTokenService.rotate(current, now);

        String accessToken = jwtService.generateAccessToken(
                user.getId(),
                user.getUsername(),
                accessContext.roles(),
                accessContext.permissions()
        );

        return response(accessToken, rotated, user, accessContext);
    }

    /**
     * Revokes the session family that owns the supplied refresh token.
     * Unknown tokens are ignored to avoid exposing session validity.
     */
    @Transactional
    public void logout(String rawRefreshToken) {

        OffsetDateTime now = OffsetDateTime.now();

        refreshTokenService.lockByRawToken(rawRefreshToken)
                .ifPresent(token -> refreshTokenService.revokeFamily(
                        token.getFamilyId(), now));
    }

    private LoginResponse response(
            String accessToken,
            RefreshTokenService.IssuedRefreshToken refreshToken,
            UserAccount user,
            AccessContextResponse accessContext
    ) {
        return new LoginResponse(
                accessToken,
                TOKEN_TYPE,
                jwtProperties.expirationMs() / 1000,
                refreshToken.token(),
                jwtProperties.refreshExpirationMs() / 1000,
                user.getId(),
                user.getUsername(),
                accessContext
        );
    }
}
