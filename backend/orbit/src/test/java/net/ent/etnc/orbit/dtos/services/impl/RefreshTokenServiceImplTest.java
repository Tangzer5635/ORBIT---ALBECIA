package net.ent.etnc.orbit.dtos.services.impl;

import net.ent.etnc.orbit.models.entities.RefreshToken;
import net.ent.etnc.orbit.repositories.RefreshTokenRepository;
import net.ent.etnc.orbit.services.RefreshTokenService;
import net.ent.etnc.orbit.services.commons.ServiceException;
import net.ent.etnc.orbit.services.impl.RefreshTokenServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    private static final long DUREE_MS = 4 * 60 * 60 * 1000L;

    @Mock
    private RefreshTokenRepository repository;

    private RefreshTokenServiceImpl service;

    @BeforeEach
    void setUp() {
        // construit à la main : le constructeur attend aussi la durée de validité (long)
        service = new RefreshTokenServiceImpl(repository, DUREE_MS);
    }

    private static String sha256(String brut) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(brut.getBytes(StandardCharsets.UTF_8)));
    }

    private RefreshToken token(String brut, boolean revoque, LocalDateTime expiration) throws Exception {
        RefreshToken t = new RefreshToken();
        t.setTokenHash(sha256(brut));
        t.setUsername("t.lebuhe");
        t.setFamily("famille-1");
        t.setRevoked(revoque);
        t.setExpiresAt(expiration);
        return t;
    }

    // ---------- createRefreshToken ----------

    @Test
    void createRefreshToken_doitStockerUniquementLeHash() throws Exception {
        String brut = service.createRefreshToken("t.lebuhe");

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(captor.capture());
        RefreshToken enregistre = captor.getValue();
        assertThat(brut).isNotBlank();
        assertThat(enregistre.getTokenHash()).isEqualTo(sha256(brut)).isNotEqualTo(brut).hasSize(64);
        assertThat(enregistre.getUsername()).isEqualTo("t.lebuhe");
        assertThat(enregistre.getFamily()).isNotBlank();
        assertThat(enregistre.isRevoked()).isFalse();
        assertThat(enregistre.getExpiresAt()).isAfter(LocalDateTime.now().plusHours(3));
    }

    @Test
    void createRefreshToken_doitGenererDesTokensDifferents() {
        assertThat(service.createRefreshToken("a")).isNotEqualTo(service.createRefreshToken("a"));
    }

    // ---------- rotateRefreshToken ----------

    @Test
    void rotate_doitRevoquerLAncienEtCreerUnNouveauDansLaMemeFamille() throws Exception {
        RefreshToken ancien = token("ancien", false, LocalDateTime.now().plusHours(1));
        when(repository.findByTokenHash(sha256("ancien"))).thenReturn(Optional.of(ancien));

        RefreshTokenService.RotateResult resultat = service.rotateRefreshToken("ancien");

        assertThat(ancien.isRevoked()).isTrue();
        assertThat(resultat.username()).isEqualTo("t.lebuhe");
        assertThat(resultat.rawToken()).isNotEqualTo("ancien");
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository, times(2)).save(captor.capture());
        RefreshToken nouveau = captor.getAllValues().get(1);
        assertThat(nouveau.getFamily()).isEqualTo("famille-1");
        assertThat(nouveau.getTokenHash()).isEqualTo(sha256(resultat.rawToken()));
    }

    @Test
    void rotate_doitLeverServiceException_RGSEC01_quandTokenInconnu() {
        when(repository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rotateRefreshToken("inconnu"))
                .isInstanceOf(ServiceException.class)
                .hasMessage("Refresh token invalide");
    }

    @Test
    void rotate_doitRevoquerToutLaFamille_quandTokenDejaRevoque() throws Exception {
        RefreshToken vole = token("vole", true, LocalDateTime.now().plusHours(1));
        RefreshToken frere = token("frere", false, LocalDateTime.now().plusHours(1));
        when(repository.findByTokenHash(sha256("vole"))).thenReturn(Optional.of(vole));
        when(repository.findAllByFamily("famille-1")).thenReturn(List.of(vole, frere));

        assertThatThrownBy(() -> service.rotateRefreshToken("vole"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("toutes vos sessions ont été fermées");
        assertThat(frere.isRevoked()).isTrue();
        verify(repository).saveAll(List.of(vole, frere));
        verify(repository, never()).save(any());
    }

    @Test
    void rotate_doitLeverServiceException_RGSEC03_etRevoquer_quandExpire() throws Exception {
        RefreshToken expire = token("expire", false, LocalDateTime.now().minusMinutes(1));
        when(repository.findByTokenHash(sha256("expire"))).thenReturn(Optional.of(expire));

        assertThatThrownBy(() -> service.rotateRefreshToken("expire"))
                .isInstanceOf(ServiceException.class)
                .hasMessage("Refresh token expiré");
        assertThat(expire.isRevoked()).isTrue();
        verify(repository).save(expire);
    }

    // ---------- revokeToken ----------

    @Test
    void revokeToken_doitRevoquerLeToken() throws Exception {
        RefreshToken t = token("courant", false, LocalDateTime.now().plusHours(1));
        when(repository.findByTokenHash(sha256("courant"))).thenReturn(Optional.of(t));

        service.revokeToken("courant");

        assertThat(t.isRevoked()).isTrue();
        verify(repository).save(t);
    }

    @Test
    void revokeToken_neDoitRienFaire_quandTokenInconnu() {
        when(repository.findByTokenHash(any())).thenReturn(Optional.empty());

        service.revokeToken("inconnu");

        verify(repository, never()).save(any());
    }

    // ---------- revokeAllForUser ----------

    @Test
    void revokeAllForUser_doitSupprimerTousLesTokensDeLUtilisateur() {
        service.revokeAllForUser("t.lebuhe");

        verify(repository).deleteAllByUsername("t.lebuhe");
    }
}
