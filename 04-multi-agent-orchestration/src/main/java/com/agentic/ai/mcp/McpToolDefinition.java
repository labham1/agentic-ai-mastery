package com.agentic.ai.mcp;

import java.util.Map;

/**
 * Tool metadata schema compliant with the Model Context Protocol (MCP) standard.
 */
public record McpToolDefinition(
        String name,
        String description,
        Map<String, Object> inputSchema
) {}
