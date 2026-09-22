package com.agentic.ai.tools;

import com.agentic.ai.core.Tool;
import com.agentic.ai.core.ToolParam;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Practical system and utility tools exposed to the Agent.
 * Compatible with both our custom ToolRegistry and LangChain4j @dev.langchain4j.agent.tool.Tool.
 */
public class SystemTools {

    @Tool("Returns the current date and time formatted as yyyy-MM-dd HH:mm:ss")
    @dev.langchain4j.agent.tool.Tool("Returns the current date and time formatted as yyyy-MM-dd HH:mm:ss")
    public String getCurrentDateTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    @Tool("Calculates arithmetic operations: add, subtract, multiply, divide, power")
    @dev.langchain4j.agent.tool.Tool("Calculates arithmetic operations: add, subtract, multiply, divide, power")
    public double calculate(
            @ToolParam("First number")
            @dev.langchain4j.agent.tool.P("First number") double a,
            @ToolParam("Second number")
            @dev.langchain4j.agent.tool.P("Second number") double b,
            @ToolParam("Operation: add, subtract, multiply, divide, power")
            @dev.langchain4j.agent.tool.P("Operation: add, subtract, multiply, divide, power") String operation
    ) {
        return switch (operation.toLowerCase().trim()) {
            case "add", "+" -> a + b;
            case "subtract", "-" -> a - b;
            case "multiply", "*" -> a * b;
            case "divide", "/" -> {
                if (b == 0) throw new ArithmeticException("Division by zero is not allowed.");
                yield a / b;
            }
            case "power", "^" -> Math.pow(a, b);
            default -> throw new IllegalArgumentException("Unknown operator: " + operation);
        };
    }

    @Tool("Calculates the square root of a given non-negative number")
    @dev.langchain4j.agent.tool.Tool("Calculates the square root of a given non-negative number")
    public double calculateSquareRoot(
            @ToolParam("Non-negative number")
            @dev.langchain4j.agent.tool.P("Non-negative number") double number
    ) {
        if (number < 0) {
            throw new IllegalArgumentException("Cannot compute square root of negative number: " + number);
        }
        return Math.sqrt(number);
    }

    @Tool("Lists files and directories present at a given local path")
    @dev.langchain4j.agent.tool.Tool("Lists files and directories present at a given local path")
    public String listDirectory(
            @ToolParam("Absolute or relative directory path")
            @dev.langchain4j.agent.tool.P("Absolute or relative directory path") String path
    ) {
        File dir = new File(path);
        if (!dir.exists()) {
            return "Path does not exist: " + path;
        }
        if (!dir.isDirectory()) {
            return "Path is a file, not a directory: " + path;
        }

        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            return "Directory is empty.";
        }

        StringBuilder sb = new StringBuilder("Contents of ").append(path).append(":\n");
        int limit = Math.min(files.length, 25);
        for (int i = 0; i < limit; i++) {
            File f = files[i];
            sb.append(f.isDirectory() ? "[DIR]  " : "[FILE] ")
              .append(f.getName())
              .append(" (").append(f.length()).append(" bytes)\n");
        }
        if (files.length > limit) {
            sb.append("... and ").append(files.length - limit).append(" more items.");
        }
        return sb.toString();
    }

    @Tool("Fetches real-time weather information for a specified city")
    @dev.langchain4j.agent.tool.Tool("Fetches real-time weather information for a specified city")
    public String getWeather(
            @ToolParam("Name of the city, e.g. Tokyo, New York, London")
            @dev.langchain4j.agent.tool.P("Name of the city, e.g. Tokyo, New York, London") String city
    ) {
        // Mock weather data service for deterministic offline testing
        Map<String, String> weatherDb = Map.of(
                "tokyo", "Sunny, 21°C, Humidity 45%, Wind 5 km/h",
                "london", "Overcast with light rain, 14°C, Humidity 82%, Wind 18 km/h",
                "new york", "Clear, 18°C, Humidity 50%, Wind 12 km/h",
                "delhi", "Hazy sunshine, 31°C, Humidity 60%, Wind 8 km/h",
                "bengaluru", "Pleasant, 24°C, Humidity 55%, Wind 10 km/h"
        );

        String normalized = city.toLowerCase().trim();
        return weatherDb.getOrDefault(normalized, "Weather data for " + city + ": Partly Cloudy, 22°C, Humidity 50%");
    }
}
