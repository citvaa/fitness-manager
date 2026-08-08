package com.example.demo.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiExceptionHandlerTest {

    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new ThrowingController())
            .setControllerAdvice(new ApiExceptionHandler())
            .build();

    @Test
    void accessDeniedIsReturnedAsForbiddenInsteadOfBadRequest() throws Exception {
        mvc.perform(get("/test/access-denied").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Forbidden appointment"));
    }

    @Test
    void otherRuntimeExceptionsRemainBadRequests() throws Exception {
        mvc.perform(get("/test/runtime").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid schedule"));
    }

    @RestController
    private static class ThrowingController {
        @GetMapping("/test/access-denied")
        void denied() {
            throw new AccessDeniedException("Forbidden appointment");
        }

        @GetMapping("/test/runtime")
        void runtime() {
            throw new IllegalArgumentException("Invalid schedule");
        }
    }
}
