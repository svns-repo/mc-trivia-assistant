package com.example.triviaassistant;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Loads Q->A pairs from config/trivia-assistant/trivia.json (falls back to
 * the bundled starter file on first run) and answers questions with
 * normalization + fuzzy matching so slightly reworded questions still hit.
 */
public class TriviaDatabase {

    private static final Set<String> STOPWORDS = Set.of(
            "a", "an", "the", "is", "are", "was", "were", "in", "on", "of", "to",
            "who", "what", "which", "did", "do", "does", "as", "for", "this",
            "that", "answer", "question", "reward", "win"
    );

    private final Map<String, String> qaMap = new HashMap<>();
    private Path configFile;

    public void load() {
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("trivia-assistant");
        try {
            Files.createDirectories(configDir);
        } catch (IOException ignored) {}

        configFile = configDir.resolve("trivia.json");

        try {
            if (!Files.exists(configFile)) {
                // Copy the bundled starter database on first run
                try (Reader r = new InputStreamReader(
                        TriviaDatabase.class.getResourceAsStream("/trivia-starter.json"),
                        StandardCharsets.UTF_8)) {
                    Files.writeString(configFile, readAll(r));
                }
            }
            try (Reader r = new FileReader(configFile.toFile(), StandardCharsets.UTF_8)) {
                Type type = new TypeToken<Map<String, String>>() {}.getType();
                Map<String, String> raw = new Gson().fromJson(r, type);
                if (raw != null) {
                    for (var entry : raw.entrySet()) {
                        qaMap.put(normalize(entry.getKey()), entry.getValue());
                    }
                }
            }
        } catch (IOException e) {
            TriviaAssistantClient.LOGGER.error("Failed to load trivia.json", e);
        }

        TriviaAssistantClient.LOGGER.info("Loaded {} trivia entries", qaMap.size());
    }

    private static String readAll(Reader r) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[4096];
        int n;
        while ((n = r.read(buf)) != -1) sb.append(buf, 0, n);
        return sb.toString();
    }

    /** Lowercase, strip punctuation, collapse whitespace. */
    private static String normalize(String s) {
        return s.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static Set<String> keywordSet(String normalized) {
        Set<String> words = new HashSet<>();
        for (String w : normalized.split(" ")) {
            if (!w.isBlank() && !STOPWORDS.contains(w)) {
                words.add(w);
            }
        }
        return words;
    }

    /**
     * Tries an exact normalized match first, then falls back to keyword
     * overlap (Jaccard similarity) against every stored question, returning
     * the best match above a similarity threshold.
     */
    public String answer(String question) {
        String norm = normalize(question);

        String exact = qaMap.get(norm);
        if (exact != null) return exact;

        Set<String> qWords = keywordSet(norm);
        if (qWords.isEmpty()) return null;

        String bestAnswer = null;
        double bestScore = 0.0;

        for (var entry : qaMap.entrySet()) {
            Set<String> storedWords = keywordSet(entry.getKey());
            if (storedWords.isEmpty()) continue;

            Set<String> intersection = new HashSet<>(qWords);
            intersection.retainAll(storedWords);
            Set<String> union = new HashSet<>(qWords);
            union.addAll(storedWords);

            double score = (double) intersection.size() / union.size();
            if (score > bestScore) {
                bestScore = score;
                bestAnswer = entry.getValue();
            }
        }

        // Require a reasonably strong overlap before trusting a fuzzy match
        return (bestScore >= 0.6) ? bestAnswer : null;
    }

    /** Lets you add new entries at runtime (e.g. via a command) and persist them. */
    public void addAndSave(String question, String answer) {
        qaMap.put(normalize(question), answer);
        try {
            Files.writeString(configFile, new Gson().toJson(qaMap));
        } catch (IOException e) {
            TriviaAssistantClient.LOGGER.error("Failed to save trivia.json", e);
        }
    }

    public int size() {
        return qaMap.size();
    }
}
