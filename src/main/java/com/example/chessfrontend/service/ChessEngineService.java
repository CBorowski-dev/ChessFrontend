package com.example.chessfrontend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import javax.annotation.PostConstruct;

import java.io.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class ChessEngineService {
    private Process engineProcess;
    private BufferedReader processReader;
    private BufferedWriter processWriter;

    @Value("${chess.engine.path}")
    private String enginePath;

    @PostConstruct
    public void init() throws IOException {
        startEngine(enginePath);
    }

    public void startEngine(String enginePath) throws IOException {
        engineProcess = new ProcessBuilder(enginePath)
                .redirectErrorStream(true)
                .start();
        
        processReader = new BufferedReader(new InputStreamReader(engineProcess.getInputStream()));
        processWriter = new BufferedWriter(new OutputStreamWriter(engineProcess.getOutputStream()));
        
        sendCommand("uci");
        sendCommand("isready");
        waitForResponse("readyok");
    }

    public void sendCommand(String command) throws IOException {
        System.out.println("Sending command: " + command);
        processWriter.write(command + "\n");
        processWriter.flush();
    }

    public String waitForResponse(String expected) throws IOException {
        System.out.println("Waiting for response: " + expected);
        String line;
        while ((line = processReader.readLine()) != null) {
            if (line.contains(expected)) {
                System.out.println("Received response: " + line);
                return line;
            }
        }
        System.out.println("No response received");
        return null;
    }

    @Async
    public CompletableFuture<String> getBestMoveAsync(String fen) {
        try {
            System.out.println("Getting best move for: " + fen);
            sendCommand("position fen " + fen);
            sendCommand("go movetime 100");
            String result = waitForResponse("bestmove");
            return CompletableFuture.completedFuture(result);
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async
    public CompletableFuture<String> getCurrentFenAsync() {
        try {
            sendCommand("d");
            String result = waitForResponse("Fen")
                .replaceAll(".*Fen\\s+", "")
                .split("\\s+", 2)[0];
            return CompletableFuture.completedFuture(result);
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public void stopEngine() {
        if (engineProcess != null) {
            engineProcess.destroy();
        }
    }

    public String getCurrentFen() throws IOException {
        sendCommand("d");
        return waitForResponse("fen") // Parse FEN from engine output
            .replaceAll(".*fen\\s+", "")
            .split("\\s+", 2)[0];
    }
} 