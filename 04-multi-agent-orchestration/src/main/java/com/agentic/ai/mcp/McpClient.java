package com.agentic.ai.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Model Context Protocol (MCP) Client.
 * 
 * Implements the official JSON-RPC 2.0 protocol for external tool discovery
 * and remote execution over standard MCP primitives (tools/list, tools/call).
 */
public class McpClient {

    private static final Logger log = LoggerFactory.getLogger(McpClient.class);

    private final AtomicInteger requestIdCounter = new AtomicInteger(1);
    private final Function<McpMessage.Request, McpMessage.Response> transportHandler;
    private final ObjectMapper mapper = new ObjectMapper();

    public McpClient(Function<McpMessage.Request, McpMessage.Response> transportHandler) {
        this.transportHandler = transportHandler;
    }

    /**
     * Discovers all tools exposed by the MCP Server via 'tools/list'.
     */
    @SuppressWarnings("unchecked")
    public List<McpToolDefinition> listTools() {
        String reqId = String.valueOf(requestIdCounter.getAndIncrement());
        McpMessage.Request req = McpMessage.Request.create(reqId, "tools/list", Map.of());

        log.info("[MCP Client] Sending 'tools/list' request (id: {})", reqId);
        McpMessage.Response res = transportHandler.apply(req);

        if (!res.isSuccess()) {
            throw new RuntimeException("MCP tools/list failed: " + res.error().message());
        }

        List<McpToolDefinition> definitions = new ArrayList<>();
        Map<String, Object> resultMap = (Map<String, Object>) res.result();
        List<Map<String, Object>> toolsList = (List<Map<String, Object>>) resultMap.get("tools");

        for (Map<String, Object> t : toolsList) {
            definitions.add(new McpToolDefinition(
                    (String) t.get("name"),
                    (String) t.get("description"),
                    (Map<String, Object>) t.get("inputSchema")
            ));
        }

        log.info("[MCP Client] Successfully discovered {} remote tools from MCP Server", definitions.size());
        return definitions;
    }

    /**
     * Executes a tool on the remote MCP Server via 'tools/call'.
     */
    public Object callTool(String toolName, Map<String, Object> arguments) {
        String reqId = String.valueOf(requestIdCounter.getAndIncrement());
        Map<String, Object> params = Map.of(
                "name", toolName,
                "arguments", arguments != null ? arguments : Map.of()
        );
        McpMessage.Request req = McpMessage.Request.create(reqId, "tools/call", params);

        log.info("[MCP Client] Invoking 'tools/call' for [{}] (id: {})", toolName, reqId);
        McpMessage.Response res = transportHandler.apply(req);

        if (!res.isSuccess()) {
            throw new RuntimeException("MCP tools/call failed: " + res.error().message());
        }

        return res.result();
    }
}
