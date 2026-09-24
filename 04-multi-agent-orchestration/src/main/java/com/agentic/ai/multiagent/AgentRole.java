package com.agentic.ai.multiagent;

import com.agentic.ai.core.ToolRegistry;

/**
 * Definition of a specialized worker agent role in a Multi-Agent Squad.
 */
public record AgentRole(
        String roleName,
        String systemPersona,
        ToolRegistry availableTools
) {
    public AgentRole(String roleName, String systemPersona) {
        this(roleName, systemPersona, new ToolRegistry());
    }
}
