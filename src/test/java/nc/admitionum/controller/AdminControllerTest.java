package nc.admitionum.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import nc.admitionum.config.SecurityConfig;
import nc.admitionum.dto.admin.AdminDashboardResponse;
import nc.admitionum.dto.admin.AdminRsvpResponse;
import nc.admitionum.service.AdminService;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminService adminService;

    @Test
    void dashboardShouldRequireAuthentication()
            throws Exception {

        mockMvc.perform(
            get("/api/admin/dashboard")
        )
            .andExpect(
                status().is3xxRedirection()
            )
            .andExpect(
                redirectedUrl("/login")
            );
    }

    @Test
    void responsesShouldRequireAuthentication()
            throws Exception {

        mockMvc.perform(
            get("/api/admin/responses")
        )
            .andExpect(
                status().is3xxRedirection()
            )
            .andExpect(
                redirectedUrl("/login")
            );
    }

    @Test
    void adminShouldAccessDashboard()
            throws Exception {

        AdminDashboardResponse response =
            new AdminDashboardResponse(
                4,
                3,
                1,
                2,
                1,
                4,
                1
            );

        given(
            adminService.getDashboard()
        ).willReturn(response);

        mockMvc.perform(
            get("/api/admin/dashboard")
                .with(
                    user("test-admin")
                        .roles("ADMIN")
                )
        )
            .andExpect(status().isOk())
            .andExpect(
                jsonPath(
                    "$.totalInvitations"
                ).value(4)
            )
            .andExpect(
                jsonPath(
                    "$.answeredInvitations"
                ).value(3)
            )
            .andExpect(
                jsonPath(
                    "$.pendingInvitations"
                ).value(1)
            )
            .andExpect(
                jsonPath(
                    "$.confirmedInvitations"
                ).value(2)
            )
            .andExpect(
                jsonPath(
                    "$.declinedInvitations"
                ).value(1)
            )
            .andExpect(
                jsonPath(
                    "$.confirmedAttendees"
                ).value(4)
            )
            .andExpect(
                jsonPath(
                    "$.responsesWithIntolerances"
                ).value(1)
            );
    }

    @Test
    void adminShouldAccessResponses()
            throws Exception {

        AdminRsvpResponse response =
            new AdminRsvpResponse(
                1,
                "Familia García",
                4,
                "CONFIRMED",
                "Ana García",
                "ana@example.com",
                3,
                "Intolerancia a la lactosa",
                "Llegaremos el viernes",
                LocalDateTime.of(
                    2026,
                    8,
                    11,
                    15,
                    30
                )
            );

        given(
            adminService.getResponses()
        ).willReturn(
            List.of(response)
        );

        mockMvc.perform(
            get("/api/admin/responses")
                .with(
                    user("test-admin")
                        .roles("ADMIN")
                )
        )
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$[0].invitationId")
                    .value(1)
            )
            .andExpect(
                jsonPath("$[0].displayName")
                    .value("Familia García")
            )
            .andExpect(
                jsonPath("$[0].status")
                    .value("CONFIRMED")
            )
            .andExpect(
                jsonPath("$[0].guestName")
                    .value("Ana García")
            )
            .andExpect(
                jsonPath("$[0].attendeeCount")
                    .value(3)
            );
    }

    @Test
    void nonAdminShouldBeForbidden()
            throws Exception {

        mockMvc.perform(
            get("/api/admin/dashboard")
                .with(
                    user("normal-user")
                        .roles("USER")
                )
        )
            .andExpect(
                status().isForbidden()
            );
    }
}