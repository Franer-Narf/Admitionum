package nc.admitionum.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(
    SecurityConfigTest.ProtectedTestController.class
)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicHealthShouldRemainAccessibleWithoutLogin()
            throws Exception {

        mockMvc.perform(
            get("/api/public/health")
        )
            .andExpect(status().isOk());
    }

    @Test
    void publicRsvpPutShouldNotRequireCsrfToken()
            throws Exception {

        String requestBody = """
            {
              "guestName": "Ana García",
              "contact": "ana@example.com",
              "attendanceConfirmed": true,
              "attendeeCount": 1,
              "intolerances": "",
              "additionalComment": ""
            }
            """;

        mockMvc.perform(
            put(
                "/api/public/invitations/"
                    + "UNKNOWN-CODE/response"
            )
                .contentType(
                    MediaType.APPLICATION_JSON
                )
                .content(requestBody)
        )
            .andExpect(status().isNotFound());
    }

    @Test
    void adminPageShouldRequireAuthentication()
            throws Exception {

        mockMvc.perform(
            get("/admin/security-test")
        )
            .andExpect(
                status().is3xxRedirection()
            )
            .andExpect(
                redirectedUrl("/login")
            );
    }

    @Test
    void adminApiShouldRequireAuthentication()
            throws Exception {

        mockMvc.perform(
            get("/api/admin/security-test")
        )
            .andExpect(
                status().is3xxRedirection()
            )
            .andExpect(
               redirectedUrl("/login")
            );
    }

    @Test
    void adminRoleShouldAccessProtectedArea()
            throws Exception {

        mockMvc.perform(
            get("/admin/security-test")
                .with(
                    user("test-admin")
                        .roles("ADMIN")
                )
        )
            .andExpect(status().isOk());
    }

    @Test
    void nonAdminRoleShouldBeForbidden()
            throws Exception {

        mockMvc.perform(
            get("/admin/security-test")
                .with(
                    user("normal-user")
                        .roles("USER")
                )
        )
            .andExpect(status().isForbidden());
    }

    @Test
    void validAdminCredentialsShouldAuthenticate()
            throws Exception {

        mockMvc.perform(
            formLogin()
                .user("test-admin")
                .password("test-password")
        )
            .andExpect(
                status().is3xxRedirection()
            )
            .andExpect(
                authenticated()
                    .withUsername("test-admin")
            );
    }

    @Test
    void invalidAdminPasswordShouldFail()
            throws Exception {

        mockMvc.perform(
            formLogin()
                .user("test-admin")
                .password("wrong-password")
        )
            .andExpect(
                status().is3xxRedirection()
            )
            .andExpect(unauthenticated());
    }

    @RestController
    static class ProtectedTestController {

        @GetMapping("/admin/security-test")
        public Map<String, String> adminPage() {
            return Map.of(
                "status",
                "admin-ok"
            );
        }

        @GetMapping("/api/admin/security-test")
        public Map<String, String> adminApi() {
            return Map.of(
                "status",
                "admin-api-ok"
            );
        }
    }
}