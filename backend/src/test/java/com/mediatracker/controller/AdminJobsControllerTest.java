package com.mediatracker.controller;

import com.mediatracker.worker.JobQueueManager;
import com.mediatracker.worker.JobScheduler;
import com.mediatracker.model.entity.UserEntity;
import com.mediatracker.model.enums.MediaType;
import com.mediatracker.repository.UserRepository;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminJobsControllerTest {
    @Mock private JobScheduler scheduler;
    @Mock private JobQueueManager queue;
    @Mock private UserRepository users;
    @Mock private com.mediatracker.repository.BackgroundJobRepository jobs;
    @InjectMocks private AdminController controller;

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void jobFiltersAndPageAreForwardedTogether() throws Exception {
        var page = org.springframework.data.domain.PageRequest.of(1, 10);
        when(jobs.findAdminJobs("pending", "award_badges", "badge:", "user", "timeout", page))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(java.util.List.of(), page, 11));
        MockMvcBuilders.standaloneSetup(controller).build().perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/admin/jobs")
                        .param("status", " PENDING ").param("type", " award_badges ")
                        .param("dedupeKey", "badge:").param("userId", "user").param("q", "timeout")
                        .param("page", "2").param("limit", "10"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(11));
        verify(jobs).findAdminJobs("pending", "award_badges", "badge:", "user", "timeout", page);
    }

    @Test
    void invalidJobPaginationNeverQueriesRepository() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(controller).build();
        for (String[] values : new String[][] { { "0", "10" }, { "-1", "10" }, { "1", "0" },
                { "1", "101" }, { "2147483647", "100" }, { "abc", "10" } }) {
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/admin/jobs")
                    .param("page", values[0]).param("limit", values[1])).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(jobs);
    }

    @Test
    void defaultBatchPreservesUiResponseContract() throws Exception {
        when(scheduler.processBatch(10)).thenReturn(new JobScheduler.BatchResult(true, "worker", 3, 1, 1, 1));
        MockMvcBuilders.standaloneSetup(controller).build().perform(post("/api/admin/jobs/process"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.workerId").value("worker"))
                .andExpect(jsonPath("$.processed").value(3))
                .andExpect(jsonPath("$.completed").value(1))
                .andExpect(jsonPath("$.retried").value(1))
                .andExpect(jsonPath("$.failed").value(1));
    }

    @Test
    void invalidBatchIsRejectedBeforeProcessing() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(controller).build();
        for (String size : new String[] { "0", "101", "abc" }) {
            mvc.perform(post("/api/admin/jobs/process").param("batchSize", size))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(scheduler);
    }

    private AdminController securedController() {
        var factory = new ProxyFactory(controller);
        factory.setProxyTargetClass(true);
        factory.addAdvisor(AuthorizationManagerBeforeMethodInterceptor.preAuthorize());
        return (AdminController) factory.getProxy();
    }

    private void authenticate(String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "test", "unused", AuthorityUtils.createAuthorityList("ROLE_" + role)));
    }

    @Test
    void batchAndSummaryRequireAdminNotGatewayUserOrSystem() {
        var secured = securedController();
        for (String role : new String[] { "GATEWAY", "USER", "SYSTEM" }) {
            authenticate(role);
            assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> secured.processJobs(10));
            assertThrows(org.springframework.security.access.AccessDeniedException.class, secured::getJobSummary);
        }
        verifyNoInteractions(scheduler, queue);
    }

    @Test
    void anonymousCannotProcessBatch() {
        assertThrows(org.springframework.security.authentication.AuthenticationCredentialsNotFoundException.class,
                () -> securedController().processJobs(10));
        verifyNoInteractions(scheduler);
    }

    @Test
    void adminCanProcessBatch() {
        authenticate("ADMIN");
        when(scheduler.processBatch(1)).thenReturn(new JobScheduler.BatchResult(true, "worker", 0, 0, 0, 0));
        assertEquals(200, securedController().processJobs(1).getStatusCode().value());
        verify(scheduler).processBatch(1);
    }

    @Test
    void recalculateStatsQueuesEverySupportedTypeForResolvedUser() {
        var user = new UserEntity();
        user.setId(UUID.randomUUID());
        when(users.findByUsernameIgnoreCase("test-user")).thenReturn(Optional.of(user));

        var response = controller.handleUserAction("test-user",
                new AdminController.UserActionRequest("recalculate-stats", null));

        assertEquals(200, response.getStatusCode().value());
        assertEquals(Map.of("success", true, "queuedTypes",
                Arrays.stream(MediaType.values()).map(Enum::name).toList()), response.getBody());
        for (MediaType type : MediaType.values()) {
            verify(queue).enqueueJob("update_user_stats",
                    Map.of("userId", user.getId().toString(), "mediaType", type.name()),
                    "update_user_stats:" + user.getId() + ":" + type.name(), null, null);
        }
        verifyNoMoreInteractions(queue);
    }

    @Test
    void recalculateStatsDoesNotQueueForMissingUser() {
        UUID id = UUID.randomUUID();
        when(users.findById(id)).thenReturn(Optional.empty());
        assertEquals(404, controller.handleUserAction(id.toString(),
                new AdminController.UserActionRequest("recalculate-stats", null)).getStatusCode().value());
        verifyNoInteractions(queue);
    }

    @Test
    void recalculateStatsDoesNotReportSuccessWhenEnqueueFails() {
        var user = new UserEntity();
        user.setId(UUID.randomUUID());
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        when(queue.enqueueJob(org.mockito.ArgumentMatchers.eq("update_user_stats"),
                org.mockito.ArgumentMatchers.anyMap(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull()))
                .thenThrow(new IllegalStateException("database unavailable"));
        assertThrows(IllegalStateException.class, () -> controller.handleUserAction(user.getId().toString(),
                new AdminController.UserActionRequest("recalculate-stats", null)));
    }
}
