package com.digitalocean.llmproxy.service;

import com.digitalocean.llmproxy.model.metrics.ComparisonBreakdown;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class ComparisonService {

    private static final Pattern JSON_FENCE =
            Pattern.compile("^```(?:json)?\\s*([\\s\\S]*?)\\s*```$", Pattern.CASE_INSENSITIVE);

    private final ObjectMapper objectMapper;

    public ComparisonService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Deterministic heuristic comparison:
     * 1. Both model outputs must be valid, parseable JSON objects.
     * 2. Extract {@code action} from each payload and compare for exact equality.
     */
    public ComparisonBreakdown compareResponses(String primaryContent, String candidateContent) {
        ParsedPayload primary = parsePayload(primaryContent);
        ParsedPayload candidate = parsePayload(candidateContent);

        boolean actionExactMatch = primary.validJson()
                && candidate.validJson()
                && primary.action() != null
                && primary.action().equals(candidate.action());

        return new ComparisonBreakdown(
                primary.validJson(),
                candidate.validJson(),
                primary.action(),
                candidate.action(),
                actionExactMatch);
    }

    private ParsedPayload parsePayload(String content) {
        if (content == null || content.isBlank()) {
            return ParsedPayload.invalid();
        }

        String jsonText = unwrapMarkdownFence(content.trim());
        try {
            JsonNode node = objectMapper.readTree(jsonText);
            if (!node.isObject()) {
                return ParsedPayload.invalid();
            }
            JsonNode actionNode = node.get("action");
            String action = (actionNode != null && actionNode.isTextual()) ? actionNode.asText() : null;
            return new ParsedPayload(true, action);
        } catch (Exception ex) {
            return ParsedPayload.invalid();
        }
    }

    private static String unwrapMarkdownFence(String content) {
        Matcher matcher = JSON_FENCE.matcher(content);
        if (matcher.matches()) {
            return matcher.group(1).trim();
        }
        return content;
    }

    private record ParsedPayload(boolean validJson, String action) {
        static ParsedPayload invalid() {
            return new ParsedPayload(false, null);
        }
    }
}
