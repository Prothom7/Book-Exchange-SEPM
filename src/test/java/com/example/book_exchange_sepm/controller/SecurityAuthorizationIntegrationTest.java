package com.example.book_exchange_sepm.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void availableBooksEndpoint_ShouldReturnUnauthorized_ForAnonymousUser() throws Exception {
        mockMvc.perform(get("/api/books/available"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "regular_user", roles = {"USER"})
    void userDashboard_ShouldBeAccessible_ForAuthenticatedUserRole() throws Exception {
        mockMvc.perform(get("/api/user/dashboard"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "regular_user", roles = {"USER"})
    void adminStatsEndpoint_ShouldBeForbidden_ForNonAdminUser() throws Exception {
        mockMvc.perform(get("/admin/api/stats"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void adminStatsEndpoint_ShouldBeAccessible_ForAdmin() throws Exception {
        mockMvc.perform(get("/admin/api/stats"))
            .andExpect(status().isOk());
    }
}

