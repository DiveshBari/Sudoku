package com.sudoku.engine;

import java.util.*;

/**
 * Core Sudoku engine.
 * Handles puzzle generation, solution validation, and hint logic
 * for different grid sizes: 4x4, 6x6, 9x9.
 */
public class SudokuEngine {

    public enum Level {
        MINI("Mini (4×4)", 4, 2, 2),
        SAMURAI("Samurai (6×6)", 6, 2, 3),
        EASY("Easy (9×9)", 9, 3, 3),
        MEDIUM("Medium (9×9)", 9, 3, 3),
        HARD("Hard (9×9)", 9, 3, 3);

        public final String label;
        public final int size;
        public final int boxRows;
        public final int boxCols;

        Level(String label, int size, int boxRows, int boxCols) {
            this.label = label;
            this.size = size;
            this.boxRows = boxRows;
            this.boxCols = boxCols;
        }
    }

    private final int size;
    private final int boxRows;
    private final int boxCols;
    private final int[][] solution;
    private final int[][] puzzle;

    public SudokuEngine(Level level) {
        this.size = level.size;
        this.boxRows = level.boxRows;
        this.boxCols = level.boxCols;
        this.solution = new int[size][size];
        this.puzzle = new int[size][size];
        generate(level);
    }

    private void generate(Level level) {
        fillBoard(solution);
        copyBoard(solution, puzzle);
        double ratio = switch (level) {
            case EASY    -> 0.55;
            case MEDIUM  -> 0.40;
            case HARD    -> 0.28;
            case MINI    -> 0.50;
            case SAMURAI -> 0.45;
        };
        int toRemove = size * size - (int)(size * size * ratio);
        removeClues(puzzle, toRemove);
    }

    private boolean fillBoard(int[][] board) {
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                if (board[row][col] == 0) {
                    List<Integer> nums = new ArrayList<>();
                    for (int n = 1; n <= size; n++) nums.add(n);
                    Collections.shuffle(nums);
                    for (int num : nums) {
                        if (isValid(board, row, col, num)) {
                            board[row][col] = num;
                            if (fillBoard(board)) return true;
                            board[row][col] = 0;
                        }
                    }
                    return false;
                }
            }
        }
        return true;
    }

    private void removeClues(int[][] board, int count) {
        Random rand = new Random();
        int removed = 0;
        int attempts = 0;
        while (removed < count && attempts < count * 10) {
            int r = rand.nextInt(size);
            int c = rand.nextInt(size);
            if (board[r][c] != 0) {
                board[r][c] = 0;
                removed++;
            }
            attempts++;
        }
    }

    public boolean isValid(int[][] board, int row, int col, int num) {
        for (int c = 0; c < size; c++)
            if (board[row][c] == num) return false;
        for (int r = 0; r < size; r++)
            if (board[r][col] == num) return false;
        int startRow = (row / boxRows) * boxRows;
        int startCol = (col / boxCols) * boxCols;
        for (int r = startRow; r < startRow + boxRows; r++)
            for (int c = startCol; c < startCol + boxCols; c++)
                if (board[r][c] == num) return false;
        return true;
    }

    public boolean isCorrect(int row, int col, int value) {
        return solution[row][col] == value;
    }

    public boolean isGiven(int row, int col) { return puzzle[row][col] != 0; }
    public int getPuzzleValue(int row, int col) { return puzzle[row][col]; }
    public int getSolutionValue(int row, int col) { return solution[row][col]; }
    public int getSize() { return size; }
    public int getBoxRows() { return boxRows; }
    public int getBoxCols() { return boxCols; }

    private void copyBoard(int[][] src, int[][] dst) {
        for (int r = 0; r < size; r++) dst[r] = src[r].clone();
    }
}
