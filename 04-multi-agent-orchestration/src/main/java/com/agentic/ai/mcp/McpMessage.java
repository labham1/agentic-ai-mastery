package com.agentic.ai.mcp;

import java.util.Map;

/**
 * Standard JSON-RPC 2.0 messages used by the Model Context Protocol (MCP).
 */
public class McpMessage {

    public record Request(
            String jsonrpc,
            String id,
            String method,
            Map<String, Object> params
    ) {
        public static Request create(String id, String method, Map<String, Object> params) {
            return new Request("2.0", id, method, params);
        }
    }

    public record Response(
            String jsonrpc,
            String id,
            Object result,
            Error error
    ) {
        public boolean isSuccess() {
            return error == null;
        }
    }

    public record Error(
            int code,
            String message,
            Object data
    ) {}
}
