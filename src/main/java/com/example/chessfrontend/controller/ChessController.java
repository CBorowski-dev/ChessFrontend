package com.example.chessfrontend.controller;

import com.example.chessfrontend.model.Game;
import com.example.chessfrontend.service.ChessEngineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.concurrent.CompletableFuture;

@Controller
@RequiredArgsConstructor
public class ChessController {
    private final ChessEngineService chessEngineService;
    private Game currentGame = new Game();

    @GetMapping("/")
    public String showChessboard(Model model) {
        model.addAttribute("game", currentGame);
        return "chess";
    }

    @PostMapping("/move")
    @ResponseBody
    public CompletableFuture<Game> makeMove(@RequestParam String move) {
        if (!currentGame.isGameOver() && currentGame.isPlayerTurn()) {
            currentGame.setLastMove(move);
            System.out.println("--> Player Fen: " + currentGame.getFen());
            
            return chessEngineService.getBestMoveAsync(currentGame.getFen())
                .thenApply(engineResponse -> {
                    String engineMove = engineResponse.split(" ")[1];
                    System.out.println("--> Engine move: " + engineMove);
                    currentGame.setLastMove(engineMove);  // Apply engine's move to update FEN
                    return currentGame;
                });
        }
        return CompletableFuture.completedFuture(currentGame);
    }

    @PostMapping("/newgame")
    @ResponseBody
    public Game newGame() {
        currentGame = new Game();
        return currentGame;
    }
} 