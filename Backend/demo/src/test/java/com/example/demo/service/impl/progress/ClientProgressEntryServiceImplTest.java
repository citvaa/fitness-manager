package com.example.demo.service.impl.progress;

import com.example.demo.dto.progress.ClientProgressEntryDTO;
import com.example.demo.mapper.progress.ClientProgressEntryMapper;
import com.example.demo.model.progress.ClientProgressEntry;
import com.example.demo.model.user.Client;
import com.example.demo.repository.progress.ClientProgressEntryRepository;
import com.example.demo.repository.user.ClientRepository;
import com.example.demo.security.TrainerClientAccessGuard;
import com.example.demo.service.params.request.progress.CreateProgressEntryRequest;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ClientProgressEntryServiceImpl} - trainer-ownership guarding on the
 * write/read-by-id paths (delegated to {@link TrainerClientAccessGuard}, verified here via a
 * mock rather than re-testing the guard's own logic) and the JWT-resolved getMine() path, which
 * deliberately does NOT go through the guard - see AGENTS.md ("Upgrade: service layer decisions").
 */
@ExtendWith(MockitoExtension.class)
class ClientProgressEntryServiceImplTest {

    @Mock
    private ClientProgressEntryRepository clientProgressEntryRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private ClientProgressEntryMapper clientProgressEntryMapper;
    @Mock
    private TrainerClientAccessGuard trainerClientAccessGuard;
    @Mock
    private CacheManager cacheManager;
    @Mock
    private Cache cache;

    private ClientProgressEntryServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(cacheManager.getCache(any())).thenReturn(cache);
        service = new ClientProgressEntryServiceImpl(clientProgressEntryRepository, clientRepository,
                clientProgressEntryMapper, trainerClientAccessGuard, cacheManager);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void create_delegatesOwnershipCheckToGuardBeforeSaving() {
        CreateProgressEntryRequest request = new CreateProgressEntryRequest();
        request.setClientId(1);
        request.setEntryDate(LocalDate.now());
        request.setWeightKg(new BigDecimal("80.5"));

        Client client = Client.builder().id(1).build();
        when(clientRepository.findById(1)).thenReturn(Optional.of(client));
        when(clientProgressEntryRepository.save(any(ClientProgressEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(clientProgressEntryMapper.toDto(any(ClientProgressEntry.class))).thenReturn(new ClientProgressEntryDTO());

        service.create(request);

        verify(trainerClientAccessGuard).assertCanAccessClient(1);

        ArgumentCaptor<ClientProgressEntry> captor = ArgumentCaptor.forClass(ClientProgressEntry.class);
        verify(clientProgressEntryRepository).save(captor.capture());
        assertThat(captor.getValue().getClient()).isSameAs(client);
        assertThat(captor.getValue().getWeightKg()).isEqualTo(new BigDecimal("80.5"));
    }

    @Test
    void create_propagatesAccessDeniedFromGuardAndNeverSaves() {
        CreateProgressEntryRequest request = new CreateProgressEntryRequest();
        request.setClientId(1);

        doThrow(new AccessDeniedException("You have never trained this client - access denied"))
                .when(trainerClientAccessGuard).assertCanAccessClient(1);

        assertThatThrownBy(() -> service.create(request)).isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(clientProgressEntryRepository);
        verifyNoInteractions(clientRepository);
    }

    @Test
    void create_throwsWhenClientNotFound() {
        CreateProgressEntryRequest request = new CreateProgressEntryRequest();
        request.setClientId(1);
        when(clientRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Client not found");
    }

    @Test
    void getForClient_delegatesOwnershipCheckToGuard() {
        when(clientProgressEntryRepository.findByClientIdOrderByEntryDateAsc(3)).thenReturn(List.of());
        when(clientProgressEntryMapper.toDto(List.<ClientProgressEntry>of())).thenReturn(List.of());

        service.getForClient(3);

        verify(trainerClientAccessGuard).assertCanAccessClient(3);
    }

    @Test
    void getForClient_propagatesAccessDenied() {
        doThrow(new AccessDeniedException("denied")).when(trainerClientAccessGuard).assertCanAccessClient(3);

        assertThatThrownBy(() -> service.getForClient(3)).isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(clientProgressEntryRepository);
    }

    @Test
    void getMine_resolvesClientFromJwtAndNeverCallsTheGuard() {
        Client client = Client.builder().id(42).build();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("email", "client@gym.com")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(900))
                .build();
        Authentication authentication = new UsernamePasswordAuthenticationToken(jwt, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(clientRepository.findByUserEmail("client@gym.com")).thenReturn(Optional.of(client));
        when(clientProgressEntryRepository.findByClientIdOrderByEntryDateAsc(42)).thenReturn(List.of());
        when(clientProgressEntryMapper.toDto(List.<ClientProgressEntry>of())).thenReturn(List.of());

        service.getMine();

        // The deliberate design point from AGENTS.md: getMine() must NOT route through the
        // trainer-ownership guard, since a CLIENT caller would be misread as a TRAINER.
        verifyNoInteractions(trainerClientAccessGuard);
        verify(clientProgressEntryRepository).findByClientIdOrderByEntryDateAsc(42);
    }

    @Test
    void getMine_throwsWhenUnauthenticated() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> service.getMine()).isInstanceOf(AccessDeniedException.class);
    }

    // ---------- update/delete (Faza 9 - correcting/removing an existing entry) ----------

    @Test
    void update_checksAccessAgainstTheEntrysOwnClientAndEvictsInsightCache() {
        Client client = Client.builder().id(7).build();
        ClientProgressEntry existing = ClientProgressEntry.builder().id(50).client(client)
                .entryDate(LocalDate.now().minusDays(3)).weightKg(new BigDecimal("70")).build();
        when(clientProgressEntryRepository.findById(50)).thenReturn(Optional.of(existing));
        when(clientProgressEntryRepository.save(existing)).thenReturn(existing);
        when(clientProgressEntryMapper.toDto(existing)).thenReturn(new ClientProgressEntryDTO());

        // clientId in the request is deliberately different from the entry's real client (7) -
        // it must be ignored for authorization, see AGENTS.md ("Upgrade: Faza 9 decisions").
        CreateProgressEntryRequest request = new CreateProgressEntryRequest();
        request.setClientId(999);
        request.setEntryDate(LocalDate.now());
        request.setWeightKg(new BigDecimal("71.5"));

        service.update(50, request);

        verify(trainerClientAccessGuard).assertCanAccessClient(7);
        assertThat(existing.getWeightKg()).isEqualTo(new BigDecimal("71.5"));
        verify(cache).evict(7);
    }

    @Test
    void update_throwsWhenEntryNotFound() {
        when(clientProgressEntryRepository.findById(50)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(50, new CreateProgressEntryRequest()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void update_propagatesAccessDeniedAndNeverSaves() {
        Client client = Client.builder().id(7).build();
        ClientProgressEntry existing = ClientProgressEntry.builder().id(50).client(client).build();
        when(clientProgressEntryRepository.findById(50)).thenReturn(Optional.of(existing));
        doThrow(new AccessDeniedException("denied")).when(trainerClientAccessGuard).assertCanAccessClient(7);

        assertThatThrownBy(() -> service.update(50, new CreateProgressEntryRequest()))
                .isInstanceOf(AccessDeniedException.class);

        verify(clientProgressEntryRepository, never()).save(any());
    }

    @Test
    void delete_checksAccessAndEvictsInsightCache() {
        Client client = Client.builder().id(7).build();
        ClientProgressEntry existing = ClientProgressEntry.builder().id(50).client(client).build();
        when(clientProgressEntryRepository.findById(50)).thenReturn(Optional.of(existing));

        service.delete(50);

        verify(trainerClientAccessGuard).assertCanAccessClient(7);
        verify(clientProgressEntryRepository).delete(existing);
        verify(cache).evict(7);
    }

    @Test
    void delete_throwsWhenEntryNotFound() {
        when(clientProgressEntryRepository.findById(50)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(50)).isInstanceOf(EntityNotFoundException.class);

        verify(clientProgressEntryRepository, never()).delete(any());
    }
}
