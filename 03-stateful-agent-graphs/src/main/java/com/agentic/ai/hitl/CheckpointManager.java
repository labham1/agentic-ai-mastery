package com.agentic.ai.hitl;

import com.agentic.ai.graph.AgentState;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages state snapshots and persistence for Time-Travel debugging and HITL pause/resume.
 */
public class CheckpointManager {

    public record Checkpoint(String id, long timestamp, Map<String, Object> stateSnapshot) {}

    private final Map<String, Checkpoint> checkpointStore = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public String saveCheckpoint(String checkpointId, AgentState state) {
        Checkpoint cp = new Checkpoint(checkpointId, System.currentTimeMillis(), state.snapshotData());
        checkpointStore.put(checkpointId, cp);
        return checkpointId;
    }

    public AgentState restoreCheckpoint(String checkpointId) {
        Checkpoint cp = checkpointStore.get(checkpointId);
        if (cp == null) {
            throw new IllegalArgumentException("Checkpoint not found: " + checkpointId);
        }
        return new AgentState(cp.stateSnapshot());
    }

    public String serializeCheckpoint(String checkpointId) throws Exception {
        Checkpoint cp = checkpointStore.get(checkpointId);
        if (cp == null) return null;
        return objectMapper.writeValueAsString(cp);
    }

    public int count() {
        return checkpointStore.size();
    }
}
