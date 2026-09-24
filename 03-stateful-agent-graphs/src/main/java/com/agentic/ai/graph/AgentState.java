package com.agentic.ai.graph;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe, observable shared state passed between nodes in a StateGraph.
 */
public class AgentState {

    private final Map<String, Object> data = new ConcurrentHashMap<>();
    private final List<String> nodeExecutionHistory = new ArrayList<>();
    private boolean waitingForHuman = false;
    private String interruptedAtNode = null;

    public AgentState() {}

    public AgentState(Map<String, Object> initialData) {
        if (initialData != null) {
            this.data.putAll(initialData);
        }
    }

    public AgentState set(String key, Object value) {
        if (value == null) {
            data.remove(key);
        } else {
            data.put(key, value);
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) data.get(key);
    }

    public <T> T getOrDefault(String key, T defaultValue) {
        Object val = data.get(key);
        if (val == null) return defaultValue;
        return (T) val;
    }

    public boolean has(String key) {
        return data.containsKey(key);
    }

    public Map<String, Object> snapshotData() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(data));
    }

    public synchronized void recordStep(String nodeName) {
        nodeExecutionHistory.add(nodeName);
    }

    public synchronized List<String> getExecutionHistory() {
        return Collections.unmodifiableList(new ArrayList<>(nodeExecutionHistory));
    }

    public boolean isWaitingForHuman() {
        return waitingForHuman;
    }

    public void setWaitingForHuman(boolean waitingForHuman, String nodeName) {
        this.waitingForHuman = waitingForHuman;
        this.interruptedAtNode = nodeName;
    }

    public String getInterruptedAtNode() {
        return interruptedAtNode;
    }

    @Override
    public String toString() {
        return "AgentState[history=" + nodeExecutionHistory + ", data=" + data + "]";
    }
}
