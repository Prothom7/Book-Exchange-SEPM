package com.example.book_exchange_sepm;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.servlet.DispatcherServlet;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
class ServiceLayerSmokeIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void applicationContext_ShouldLoad() {
        assertNotNull(applicationContext);
    }

    @Test
    void dispatcherServlet_ShouldBeRegistered() {
        DispatcherServlet dispatcherServlet = applicationContext.getBean(DispatcherServlet.class);
        assertNotNull(dispatcherServlet);
    }
}
