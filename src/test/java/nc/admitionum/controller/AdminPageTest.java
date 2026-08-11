package nc.admitionum.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminPageTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void adminPageShouldRequireAuthentication()
            throws Exception {

        mockMvc.perform(
            get("/admin/")
        )
            .andExpect(
                status().is3xxRedirection()
            )
            .andExpect(
                redirectedUrl("/login")
            );
    }

    @Test
    void adminShouldReachPanelRoute()
            throws Exception {

        mockMvc.perform(
            get("/admin/")
                .with(
                    user("test-admin")
                        .roles("ADMIN")
                )
        )
            .andExpect(status().isOk())
            .andExpect(
                forwardedUrl(
                    "/admin/index.html"
                )
            );
    }

    @Test
    void adminStaticPageShouldContainPanelElements()
            throws Exception {

        MvcResult result =
            mockMvc.perform(
                get("/admin/index.html")
                    .with(
                        user("test-admin")
                            .roles("ADMIN")
                    )
            )
                .andExpect(status().isOk())
                .andReturn();

        String html =
            result
                .getResponse()
                .getContentAsString(
                    StandardCharsets.UTF_8
                );

        assertThat(html)
            .contains(
                "Panel de administración"
            )
            .contains(
                "dashboard-total-invitations"
            )
            .contains(
                "responses-table-body"
            )
            .contains(
                "/js/admin-app.js"
            );
    }

    @Test
    void adminScriptShouldBeAvailable()
            throws Exception {

        mockMvc.perform(
            get("/js/admin-app.js")
        )
            .andExpect(status().isOk())
            .andExpect(
                content().string(
                    containsString(
                        "/api/admin/dashboard"
                    )
                )
            )
            .andExpect(
                content().string(
                    containsString(
                        "/api/admin/responses"
                    )
                )
            );
    }
}