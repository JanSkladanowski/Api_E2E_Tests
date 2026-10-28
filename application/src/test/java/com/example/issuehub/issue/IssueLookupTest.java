package com.example.issuehub.issue;

import com.example.issuehub.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IssueLookupTest {

    private MockMvc mockMvc;

    private IssueService issueService;

    @BeforeEach
    void setUp() {
        issueService = new IssueService();
        mockMvc = MockMvcBuilders.standaloneSetup(new IssueController(issueService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void exactTitleLookupReturnsOneIssueAndRejectsAmbiguity() throws Exception {
        String title = "e2e lookup & " + UUID.randomUUID();
        Issue first = issueService.create(new CreateIssueRequest(title, "Lookup test", IssuePriority.HIGH));
        try {
            mockMvc.perform(get("/api/v1/issues/by-title").param("title", title))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(first.id()))
                    .andExpect(jsonPath("$.title").value(title));

            Issue second = issueService.create(new CreateIssueRequest(title, "Duplicate title", IssuePriority.LOW));
            try {
                mockMvc.perform(get("/api/v1/issues/by-title").param("title", title))
                        .andExpect(status().isConflict());
            } finally {
                issueService.delete(second.id());
            }
        } finally {
            issueService.delete(first.id());
        }

        mockMvc.perform(get("/api/v1/issues/by-title").param("title", title))
                .andExpect(status().isNotFound());
    }
}
