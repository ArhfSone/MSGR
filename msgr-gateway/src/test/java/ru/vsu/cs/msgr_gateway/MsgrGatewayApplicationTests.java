package ru.vsu.cs.msgr_gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("msgr-gateway unit tests")
class MsgrGatewayApplicationTests {

    @Nested
    @DisplayName("Application startup")
    @SpringBootTest
    class ApplicationStartupTests {

        @Autowired
        private ApplicationContext applicationContext;

        @Test
        @DisplayName("Spring context loads successfully")
        void contextLoads() {
            assertNotNull(applicationContext);
        }

        @Test
        @DisplayName("Gateway application bean is registered")
        void gatewayApplicationBeanExists() {
            assertTrue(applicationContext.containsBean("msgrGatewayApplication"));
        }

        @Test
        @DisplayName("Gateway route configuration is loaded")
        void gatewayRouteConfigurationLoaded() {
            assertEquals("auth-service", applicationContext.getEnvironment()
                    .getProperty("spring.cloud.gateway.routes[0].id"));
        }
    }
}
