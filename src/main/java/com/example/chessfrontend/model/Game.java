package com.example.chessfrontend.model;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Data
public class Game {
    private String fen;
    private String lastMove;
    private boolean gameOver;
    private String result;
    private boolean playerTurn;
    
    // Piece position maps
    private Map<String, String> pieces = new HashMap<>();
    private static final Pattern MOVE_PATTERN = Pattern.compile("([KQRBN])?([a-h])?([1-8])?x?([a-h][1-8])(=[QRBN])?[+#]?");
    private static final Pattern UCI_PATTERN = Pattern.compile("([a-h][1-8])([a-h][1-8])");
    
    public Game() {
        this.fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        this.playerTurn = true;
        this.gameOver = false;
        initializePieces();
    }

    private void initializePieces() {
        pieces.clear(); // Clear existing pieces
        String[] ranks = fen.split(" ")[0].split("/");
        for (int rank = 7; rank >= 0; rank--) { // Start from rank 8 (index 7)
            int file = 0;
            for (char c : ranks[7 - rank].toCharArray()) {
                if (Character.isDigit(c)) {
                    file += Character.getNumericValue(c);
                } else {
                    String square = (char)('a' + file) + String.valueOf(rank + 1);
                    pieces.put(square, String.valueOf(c));
                    file++;
                }
            }
        }
        // System.out.println("Initialized pieces: " + pieces);
    }

    public void setLastMove(String move) {
        // System.out.println("Making move: " + move + " Current FEN: " + fen);
        // System.out.println("Current pieces before move: " + pieces);
        this.lastMove = move;
        updatePosition(move);
        // System.out.println("Pieces after move: " + pieces);
        // System.out.println("New FEN: " + fen);
    }

    private void updatePosition(String moveStr) {
        // First try UCI format (for engine moves)
        var uciMatcher = UCI_PATTERN.matcher(moveStr);
        if (uciMatcher.matches()) {
            String fromSquare = uciMatcher.group(1);
            String toSquare = uciMatcher.group(2);
            
            // Get the piece from the source square
            String movingPiece = pieces.get(fromSquare);
            if (movingPiece == null) {
                throw new IllegalStateException("No piece found at " + fromSquare);
            }
            
            // Move the piece
            pieces.remove(fromSquare);
            pieces.put(toSquare, movingPiece);
            
            // Switch turn and update FEN
            playerTurn = !playerTurn;
            updateFen();
            return;
        }

        // Handle algebraic notation (for human moves)
        var matcher = MOVE_PATTERN.matcher(moveStr);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid move format: " + moveStr);
        }

        String piece;
        String pieceType = matcher.group(1);
        String targetSquare = matcher.group(4);
        
        if (pieceType != null) {
            piece = playerTurn ? pieceType.toUpperCase() : pieceType.toLowerCase();
        } else {
            piece = playerTurn ? "P" : "p";
        }

        // System.out.println("Piece type determined: " + piece);

        String sourceSquare = findSourceSquare(piece, targetSquare, matcher.group(2), matcher.group(3));
        // System.out.println("Moving " + piece + " from " + sourceSquare + " to " + targetSquare);
        
        String movingPiece = pieces.remove(sourceSquare);
        if (movingPiece == null) {
            throw new IllegalStateException("No piece found at " + sourceSquare);
        }
        pieces.put(targetSquare, movingPiece);

        String promotion = matcher.group(5);
        if (promotion != null) {
            String promotedPiece = playerTurn ? promotion.substring(1).toUpperCase() : promotion.substring(1).toLowerCase();
            pieces.put(targetSquare, promotedPiece);
        }

        playerTurn = !playerTurn;
        updateFen();
    }

    private String findSourceSquare(String piece, String targetSquare, String sourceFile, String sourceRank) {
        // System.out.println("--> Finding source square for move: " + piece + " " + targetSquare + " " + sourceFile + " " + sourceRank);
        
        // For pawns without explicit source file, we know it must be on the same file as target
        if (piece.toUpperCase().equals("P") && sourceFile == null) {
            sourceFile = String.valueOf(targetSquare.charAt(0));
        }

        for (Map.Entry<String, String> entry : pieces.entrySet()) {
            String square = entry.getKey();
            String pieceOnSquare = entry.getValue();
            
            if (pieceOnSquare.equals(piece) && // Exact match including case
                (sourceFile == null || square.charAt(0) == sourceFile.charAt(0)) &&
                (sourceRank == null || square.charAt(1) == sourceRank.charAt(0)) &&
                isLegalMove(square, targetSquare, pieceOnSquare)) {
                return square;
            }
        }
        throw new IllegalStateException("No valid source square found for move: " + piece + " to " + targetSquare);
    }

    private boolean isLegalMove(String from, String to, String piece) {
        int fileFrom = from.charAt(0) - 'a';
        int rankFrom = from.charAt(1) - '1';
        int fileTo = to.charAt(0) - 'a';
        int rankTo = to.charAt(1) - '1';

        int fileDiff = Math.abs(fileTo - fileFrom);
        int rankDiff = Math.abs(rankTo - rankFrom);
        
        // For pawns, we need to check the direction of movement
        int rankDirection = Character.isUpperCase(piece.charAt(0)) ? 1 : -1;

        switch (piece.toUpperCase()) {
            case "P":
                if (Character.isUpperCase(piece.charAt(0))) {
                    // White pawn
                    return fileDiff <= 1 && ((rankTo - rankFrom) == 1 || 
                           (rankFrom == 1 && (rankTo - rankFrom) == 2));
                } else {
                    // Black pawn
                    return fileDiff <= 1 && ((rankFrom - rankTo) == 1 || 
                           (rankFrom == 6 && (rankFrom - rankTo) == 2));
                }
            case "N":
                return (fileDiff == 2 && rankDiff == 1) || (fileDiff == 1 && rankDiff == 2);
            case "B":
                return fileDiff == rankDiff;
            case "R":
                return fileDiff == 0 || rankDiff == 0;
            case "Q":
                return fileDiff == rankDiff || fileDiff == 0 || rankDiff == 0;
            case "K":
                return fileDiff <= 1 && rankDiff <= 1;
            default:
                return false;
        }
    }

    private void updateFen() {
        StringBuilder fenBuilder = new StringBuilder();
        
        // Board position
        for (int rank = 7; rank >= 0; rank--) {
            int emptyCount = 0;
            for (int file = 0; file < 8; file++) {
                String square = (char)('a' + file) + String.valueOf(rank + 1);
                String piece = pieces.get(square);
                
                if (piece == null) {
                    emptyCount++;
                } else {
                    if (emptyCount > 0) {
                        fenBuilder.append(emptyCount);
                        emptyCount = 0;
                    }
                    fenBuilder.append(piece);
                }
            }
            if (emptyCount > 0) {
                fenBuilder.append(emptyCount);
            }
            if (rank > 0) {
                fenBuilder.append('/');
            }
        }
        
        // Add turn, castling rights, en passant, and move counts
        // For simplicity, we're only updating the position and turn
        fenBuilder.append(playerTurn ? " w " : " b ");
        fenBuilder.append("KQkq - 0 1");
        
        this.fen = fenBuilder.toString();
    }
} 