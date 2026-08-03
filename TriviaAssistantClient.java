package com.example.triviaassistant;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TriviaAssistantClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("trivia-assistant");

    // Matches the "[!] Answer this question to win a reward: <question>" format.
    // Adjust this pattern if your server phrases it differently.
    private static final Pattern TRIVIA_PATTERN = Pattern.compile(
            "(?i)\\[?!\\]?\\s*Answer this question.*?:\\s*(.+)");

    private final TriviaDatabase database = new TriviaDatabase();

    @Override
    public void onInitializeClient() {
        database.load();

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return; // ignore action-bar messages, only full chat
            handleMessage(message.getString());
        });

        LOGGER.info("Trivia Assistant loaded with {} known answers", database.size());
    }

    private void handleMessage(String plainText) {
        Matcher m = TRIVIA_PATTERN.matcher(plainText);
        if (!m.find()) return;

        String question = m.group(1).trim();
        LOGGER.info("Detected trivia question: {}", question);

        String answer = solve(question);

        if (answer != null) {
            copyToClipboard(answer);
            notifyPlayer("Trivia answer copied to clipboard: " + answer);
        } else {
            notifyPlayer("Trivia Assistant: couldn't find an answer for \"" + question + "\"");
        }
    }

    /** Routes the question to the right solver: math, roman numerals, then the trivia database. */
    private String solve(String question) {
        String mathAnswer = MathSolver.trySolve(question);
        if (mathAnswer != null) return mathAnswer;

        String romanAnswer = RomanNumerals.trySolve(question);
        if (romanAnswer != null) return romanAnswer;

        return database.answer(question);
    }

    private void copyToClipboard(String text) {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> client.keyboardHandler.setClipboard(text));
    }

    private void notifyPlayer(String text) {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            if (client.player != null) {
                client.player.displayClientMessage(Component.literal("[TriviaAssistant] " + text), false);
            }
        });
    }

    /** Exposed so other classes (e.g. a future command) can add new Q&A pairs at runtime. */
    public TriviaDatabase getDatabase() {
        return database;
    }
}
