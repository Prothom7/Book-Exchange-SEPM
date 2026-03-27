package com.example.book_exchange_sepm;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ServiceLayerSmokeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void landingPage_ShouldLoadWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/landingpage"))
            .andExpect(status().isOk());
    }

    @Test
    void browsePage_ShouldRedirectWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/browse"))
            .andExpect(status().is3xxRedirection());
    }
}
