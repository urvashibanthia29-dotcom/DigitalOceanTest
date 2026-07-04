package com.digitalocean.llmproxy.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.digitalocean.llmproxy.model.metrics.ComparisonBreakdown;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ComparisonServiceTest {

    private final ComparisonService comparisonService = new ComparisonService(new ObjectMapper());

    @Test
    void compareResponsesMatchingAction() {
        String primary = "{\"action\":\"book_flight\",\"destination\":\"Paris\"}";
        String candidate = "{\"action\":\"book_flight\",\"destination\":\"Lyon\"}";

        ComparisonBreakdown result = comparisonService.compareResponses(primary, candidate);

        assertTrue(result.isPrimaryValidJson());
        assertTrue(result.isCandidateValidJson());
        assertEquals("book_flight", result.getPrimaryAction());
        assertEquals("book_flight", result.getCandidateAction());
        assertTrue(result.isActionExactMatch());
    }

    @Test
    void compareResponsesMismatchedAction() {
        ComparisonBreakdown result = comparisonService.compareResponses(
                "{\"action\":\"search\"}",
                "{\"action\":\"cancel\"}");

        assertTrue(result.isPrimaryValidJson());
        assertTrue(result.isCandidateValidJson());
        assertFalse(result.isActionExactMatch());
    }

    @Test
    void compareResponsesInvalidJson() {
        ComparisonBreakdown result = comparisonService.compareResponses(
                "not json",
                "{\"action\":\"search\"}");

        assertFalse(result.isPrimaryValidJson());
        assertTrue(result.isCandidateValidJson());
        assertFalse(result.isActionExactMatch());
    }

    @Test
    void compareResponsesParsesMarkdownFence() {
        String primary = """
                ```json
                {"action":"refund"}
                ```
                """;
        String candidate = "{\"action\":\"refund\"}";

        ComparisonBreakdown result = comparisonService.compareResponses(primary, candidate);

        assertTrue(result.isPrimaryValidJson());
        assertTrue(result.isCandidateValidJson());
        assertTrue(result.isActionExactMatch());
    }

    @Test
    void compareResponsesDeterministic() {
        String primary = "{\"action\":\"answer\",\"text\":\"hello\"}";
        String candidate = "{\"action\":\"answer\",\"text\":\"world\"}";

        ComparisonBreakdown first = comparisonService.compareResponses(primary, candidate);
        ComparisonBreakdown second = comparisonService.compareResponses(primary, candidate);

        assertEquals(first, second);
        assertTrue(first.isActionExactMatch());
    }
}
