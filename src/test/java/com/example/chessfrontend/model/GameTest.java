package com.example.chessfrontend.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class GameTest {
    private Game game;

    @BeforeEach
    void setUp() {
        game = new Game();
    }

    @Test
    void testInitialPosition() {
        assertEquals("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", game.getFen());
        assertTrue(game.isPlayerTurn());
        assertFalse(game.isGameOver());
    }

    @ParameterizedTest
    @CsvSource({
        "e4,e2,e4,P",    // White pawn move
        //"e5,e7,e5,p",    // Black pawn move
        "Nf3,g1,f3,N"   // White knight move
        //"Nf6,g8,f6,n"    // Black knight move
    })
    void testValidMoves(String moveNotation, String expectedFrom, String expectedTo, String piece) {
        if (!moveNotation.startsWith("N")) {
            assertEquals(piece, game.getPieces().get(expectedFrom));
        }
        System.out.println("Moving " + piece + " from " + expectedFrom + " to " + expectedTo + " " + moveNotation);
        game.setLastMove(moveNotation);
        
        // Verify piece moved
        assertNull(game.getPieces().get(expectedFrom));
        assertEquals(piece, game.getPieces().get(expectedTo));
        
        // Verify turn switched
        assertEquals(moveNotation.equals("e4") || moveNotation.equals("Nf3"), 
                    !game.isPlayerTurn());
    }

    @Test
    void testInvalidPawnMove() {
        assertThrows(IllegalStateException.class, () -> game.setLastMove("e5"));
    }

    @Test
    void testInvalidKnightMove() {
        assertThrows(IllegalStateException.class, () -> game.setLastMove("Ng3"));
    }

    @Test
    void testUCIMove() {
        game.setLastMove("e2e4"); // UCI format
        assertNull(game.getPieces().get("e2"));
        assertEquals("P", game.getPieces().get("e4"));
        assertFalse(game.isPlayerTurn());
    }

    @Test
    void testMultipleMoves() {
        // White pawn to e4
        game.setLastMove("e4");
        assertEquals("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1", game.getFen());
        
        // Black pawn to e5
        game.setLastMove("e5");
        assertEquals("rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 1", game.getFen());
        
        // White knight to f3
        game.setLastMove("Nf3");
        assertEquals("rnbqkbnr/pppp1ppp/8/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 0 1", game.getFen());
    }

    @Test
    void testNewGame() {
        // Make some moves
        game.setLastMove("e4");
        game.setLastMove("e5");
        
        // Create new game
        game = new Game();
        
        // Verify initial position
        assertEquals("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", game.getFen());
        assertTrue(game.isPlayerTurn());
        assertFalse(game.isGameOver());
    }

    @Test
    void testPawnCapture() {
        // Setup position for capture
        game.setLastMove("e4");
        game.setLastMove("d5");
        
        // Capture
        game.setLastMove("exd5");
        
        // Verify capture
        assertNull(game.getPieces().get("e4"));
        assertEquals("P", game.getPieces().get("d5"));
        assertFalse(game.isPlayerTurn());
    }

    @Test
    void testInvalidFormat() {
        assertThrows(IllegalArgumentException.class, () -> game.setLastMove("invalid"));
    }
} 