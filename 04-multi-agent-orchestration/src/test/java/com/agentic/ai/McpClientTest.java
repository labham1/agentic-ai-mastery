package com.agentic.ai;

import com.agentic.ai.mcp.McpClient;
import com.agentic.ai.mcp.McpMessage;
import com.agentic.ai.mcp.McpToolDefinition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class McpClientTest {

    @Test
    @DisplayName("Should discover tools via standard MCP tools/list protocol")
    void shouldDiscoverToolsViaMcp() {
        McpClient client = new McpClient(req -> {
            assertThat(req.method()).isEqualTo("tools/list");
            return new McpMessage.Response("2.0", req.id(), Map.of(
                    "tools", List.of(
                            Map.of(
                                    "name", "searchDocs",
                                    "description", "Searches internal engineering documentation",
                                    "inputSchema", Map.of("type", "object")
                            )
                    )
            ), null);
        });

        List<McpToolDefinition> tools = client.listTools();
        assertThat(tools).hasSize(1);
        assertThat(tools.get(0).name()).isEqualTo("searchDocs");
        assertThat(tools.get(0).description()).isEqualTo("Searches internal engineering documentation");
    }

    @Test
    @DisplayName("Should execute remote tool via MCP tools/call protocol")
    void shouldExecuteToolViaMcp() {
        McpClient client = new McpClient(req -> {
            assertThat(req.method()).isEqualTo("tools/call");
            assertThat(req.params().get("name")).isEqualTo("calculateHash");
            return new McpMessage.Response("2.0", req.id(), "hash_abc_123", null);
        });

        Object result = client.callTool("calculateHash", Map.of("input", "test"));
        assertThat(result).isEqualTo("hash_abc_123");
    }

    @Test
    @DisplayName("Should handle MCP error response gracefully")
    void shouldHandleMcpError() {
        McpClient client = new McpClient(req -> new McpMessage.Response(
                "2.0", req.id(), null, new McpMessage.Error(-32600, "Invalid Request", null)
        ));

        assertThatThrownBy(client::listTools)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("MCP tools/list failed: Invalid Request");
    }
}
