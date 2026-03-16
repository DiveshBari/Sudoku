package com.sudoku.views;

import com.sudoku.engine.CellModel;
import com.sudoku.engine.SudokuEngine;
import com.sudoku.engine.SudokuEngine.Level;

import com.webforj.component.Composite;
import com.webforj.component.button.Button;
import com.webforj.component.button.ButtonTheme;
import com.webforj.component.html.elements.Div;
import com.webforj.component.layout.flexlayout.FlexAlignment;
import com.webforj.component.layout.flexlayout.FlexDirection;
import com.webforj.component.layout.flexlayout.FlexLayout;
import com.webforj.component.text.Label;
import com.webforj.router.annotation.Route;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Responsive Sudoku Game View.
 *
 * Grid uses CSS min() + 1fr tracks so it always fits any screen
 * without horizontal scrolling.  All font sizes use clamp() for
 * fluid scaling between mobile and desktop.
 */
@Route("/")
public class SudokuView extends Composite<FlexLayout> {

    private final FlexLayout self = getBoundComponent();

    // ── Game state ─────────────────────────────────────────────────
    private SudokuEngine engine;
    private CellModel[][] cells;
    private Level currentLevel = Level.EASY;
    private int selectedRow = -1, selectedCol = -1;
    private boolean noteMode = false;
    private int elapsedSeconds = 0;
    private Timer gameTimer;
    private boolean gameWon = false;

    // ── UI refs ────────────────────────────────────────────────────
    private Div gridContainer;
    private Div[][] cellDivs;
    private Label timerLabel;
    private Label statusLabel;
    private Button noteModeBtn;
    private FlexLayout numPadLayout;

    // ──────────────────────────────────────────────────────────────

    public SudokuView() {
        self.setDirection(FlexDirection.COLUMN);
        self.setAlignment(FlexAlignment.CENTER);
        self.setStyle("padding",     "clamp(12px, 3vw, 28px)");
        self.setStyle("min-height",  "100vh");
        self.setStyle("width",       "100%");
        self.setStyle("background",  "#f0f4f8");
        self.setStyle("font-family", "'Segoe UI', system-ui, sans-serif");
        self.setStyle("overflow-x",  "hidden");

        buildLayout();
        startNewGame(Level.EASY);
    }

    // ── Layout ────────────────────────────────────────────────────

    private void buildLayout() {
        Label title = new Label("🎯 Sudoku");
        title.setStyle("font-size",     "clamp(1.6rem, 5vw, 2.4rem)");
        title.setStyle("font-weight",   "800");
        title.setStyle("color",         "#1a202c");
        title.setStyle("margin-bottom", "2px");

        timerLabel = new Label("⏱ 00:00");
        timerLabel.setStyle("font-size",   "clamp(1.1rem, 3.5vw, 1.5rem)");
        timerLabel.setStyle("font-weight", "700");
        timerLabel.setStyle("color",       "#2d3748");
        timerLabel.setStyle("margin",      "6px 0");

        statusLabel = new Label(" ");
        statusLabel.setStyle("font-size",     "clamp(0.85rem, 2.5vw, 1rem)");
        statusLabel.setStyle("font-weight",   "600");
        statusLabel.setStyle("min-height",    "26px");
        statusLabel.setStyle("color",         "#276749");
        statusLabel.setStyle("margin-bottom", "8px");
        statusLabel.setStyle("text-align",    "center");

        // ── Level buttons ──
        FlexLayout levelRow = new FlexLayout();
        levelRow.setDirection(FlexDirection.ROW);
        levelRow.setStyle("flex-wrap",       "wrap");
        levelRow.setStyle("gap",             "6px");
        levelRow.setStyle("justify-content", "center");
        levelRow.setStyle("margin-bottom",   "12px");
        levelRow.setStyle("width",           "100%");

        for (Level lvl : Level.values()) {
            Button b = new Button(lvl.label);
            b.setTheme(ButtonTheme.DEFAULT);
            b.setStyle("font-size",    "clamp(0.7rem, 2vw, 0.85rem)");
            b.setStyle("padding",      "5px 10px");
            b.setStyle("border-radius","6px");
            b.setStyle("white-space",  "nowrap");
            b.setStyle("cursor",       "pointer");
            b.addClickListener(e -> startNewGame(lvl));
            levelRow.add(b);
        }

        // ── Grid wrapper ──
        Div gridWrapper = new Div();
        gridWrapper.setStyle("width",           "100%");
        gridWrapper.setStyle("display",         "flex");
        gridWrapper.setStyle("justify-content", "center");
        gridWrapper.setStyle("margin",          "6px 0 14px 0");
        gridContainer = new Div();
        gridWrapper.add(gridContainer);

        // ── Controls ──
        FlexLayout controls = new FlexLayout();
        controls.setDirection(FlexDirection.ROW);
        controls.setAlignment(FlexAlignment.CENTER);
        controls.setStyle("flex-wrap",       "wrap");
        controls.setStyle("gap",             "8px");
        controls.setStyle("justify-content", "center");
        controls.setStyle("margin-bottom",   "12px");
        controls.setStyle("width",           "100%");

        noteModeBtn = new Button("✏️ Notes: OFF");
        noteModeBtn.setTheme(ButtonTheme.DEFAULT);
        styleCtrlBtn(noteModeBtn, false);
        noteModeBtn.addClickListener(e -> toggleNoteMode());

        Button eraseBtn = new Button("🗑 Erase");
        eraseBtn.setTheme(ButtonTheme.DEFAULT);
        styleCtrlBtn(eraseBtn, false);
        eraseBtn.addClickListener(e -> eraseSelected());

        Button newGameBtn = new Button("🔄 New Game");
        newGameBtn.setTheme(ButtonTheme.PRIMARY);
        styleCtrlBtn(newGameBtn, true);
        newGameBtn.addClickListener(e -> startNewGame(currentLevel));

        controls.add(noteModeBtn, eraseBtn, newGameBtn);

        // ── Number pad ──
        numPadLayout = new FlexLayout();
        numPadLayout.setDirection(FlexDirection.ROW);
        numPadLayout.setStyle("flex-wrap",       "wrap");
        numPadLayout.setStyle("gap",             "clamp(5px, 1.5vw, 10px)");
        numPadLayout.setStyle("justify-content", "center");
        numPadLayout.setStyle("width",           "100%");

        self.add(title, timerLabel, statusLabel, levelRow, gridWrapper, controls, numPadLayout);
    }

    // ── Game ──────────────────────────────────────────────────────

    private void startNewGame(Level level) {
        currentLevel = level;
        stopTimer();
        gameWon = false;
        elapsedSeconds = 0;
        selectedRow = selectedCol = -1;
        noteMode = false;
        updateNoteModeButton();
        statusLabel.setText(" ");

        engine = new SudokuEngine(level);
        int size = engine.getSize();
        cells = new CellModel[size][size];
        for (int r = 0; r < size; r++)
            for (int c = 0; c < size; c++)
                cells[r][c] = new CellModel(engine.getPuzzleValue(r, c), engine.isGiven(r, c));

        updateTimerLabel();
        renderGrid();
        buildNumPad();
        startTimer();
    }

    // ── Grid ──────────────────────────────────────────────────────

    private void renderGrid() {
        gridContainer.removeAll();

        int size    = engine.getSize();
        int boxRows = engine.getBoxRows();
        int boxCols = engine.getBoxCols();
        cellDivs    = new Div[size][size];

        // Grid fits viewport: base size capped by available width
        int basePx      = size <= 4 ? 320 : size <= 6 ? 396 : 486;
        String gridSize = "min(" + basePx + "px, calc(100vw - 32px))";

        // Fluid font: scales between a small floor and a larger cap
        String cellFont = size <= 6
            ? "clamp(0.9rem, 4vw, 1.4rem)"
            : "clamp(0.75rem, 3vw, 1.1rem)";

        Div grid = new Div();
        grid.setStyle("display",               "grid");
        grid.setStyle("grid-template-columns", "repeat(" + size + ", 1fr)");
        grid.setStyle("width",                 gridSize);
        grid.setStyle("height",                gridSize);   // square grid
        grid.setStyle("border",                "3px solid #2d3748");
        grid.setStyle("border-radius",         "10px");
        grid.setStyle("overflow",              "hidden");
        grid.setStyle("box-shadow",            "0 6px 24px rgba(0,0,0,0.15)");

        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                final int row = r, col = c;
                Div cell = new Div();
                cell.setStyle("display",         "flex");
                cell.setStyle("align-items",     "center");
                cell.setStyle("justify-content", "center");
                cell.setStyle("position",        "relative");
                cell.setStyle("user-select",     "none");
                cell.setStyle("transition",      "background 0.12s");
                cell.setStyle("cursor",          cells[r][c].isGiven() ? "default" : "pointer");
                cell.setStyle("font-size",       cellFont);
                cell.setStyle("font-weight",     cells[r][c].isGiven() ? "700" : "500");

                // Thick lines between boxes
                cell.setStyle("border-top",    (r > 0 && r % boxRows == 0) ? "2px solid #2d3748" : "1px solid #cbd5e0");
                cell.setStyle("border-left",   (c > 0 && c % boxCols == 0) ? "2px solid #2d3748" : "1px solid #cbd5e0");
                cell.setStyle("border-bottom", "none");
                cell.setStyle("border-right",  "none");

                applyCellBg(cell, cells[r][c], false);
                renderCellContent(cell, cells[r][c], size);
                cell.addClickListener(e -> onCellClick(row, col));

                cellDivs[r][c] = cell;
                grid.add(cell);
            }
        }
        gridContainer.add(grid);
    }

    private void applyCellBg(Div cell, CellModel m, boolean selected) {
        String bg, color;
        if (selected)       { bg = "#bee3f8"; color = "#2b6cb0"; }
        else if (m.isGiven())   { bg = "#edf2f7"; color = "#1a202c"; }
        else if (m.isWrong())   { bg = "#fed7d7"; color = "#c53030"; }
        else                    { bg = "#ffffff";  color = "#2b6cb0"; }
        cell.setStyle("background", bg);
        cell.setStyle("color",      color);
    }

    private void renderCellContent(Div cell, CellModel m, int size) {
        cell.removeAll();
        if (!m.isEmpty()) {
            Label lbl = new Label(String.valueOf(m.getValue()));
            lbl.setStyle("pointer-events", "none");
            cell.add(lbl);
        } else if (!m.getNotes().isEmpty()) {
            int cols = (int) Math.ceil(Math.sqrt(size));
            Div ng = new Div();
            ng.setStyle("display",               "grid");
            ng.setStyle("grid-template-columns", "repeat(" + cols + ", 1fr)");
            ng.setStyle("width",                 "90%");
            ng.setStyle("height",                "90%");
            ng.setStyle("pointer-events",        "none");

            for (int n = 1; n <= size; n++) {
                Label nl = new Label(m.getNotes().contains(n) ? String.valueOf(n) : "");
                nl.setStyle("display",         "flex");
                nl.setStyle("align-items",     "center");
                nl.setStyle("justify-content", "center");
                nl.setStyle("font-size",       "clamp(0.25rem, 1vw, 0.42rem)");
                nl.setStyle("color",           "#718096");
                nl.setStyle("line-height",     "1");
                ng.add(nl);
            }
            cell.add(ng);
        }
    }

    private void buildNumPad() {
        numPadLayout.removeAll();
        int size = engine.getSize();
        for (int n = 1; n <= size; n++) {
            final int num = n;
            Button btn = new Button(String.valueOf(n));
            btn.setTheme(ButtonTheme.DEFAULT);
            btn.setStyle("width",         "clamp(36px, 8vw, 50px)");
            btn.setStyle("height",        "clamp(36px, 8vw, 50px)");
            btn.setStyle("font-size",     "clamp(0.9rem, 3vw, 1.2rem)");
            btn.setStyle("font-weight",   "700");
            btn.setStyle("border-radius", "8px");
            btn.setStyle("border",        "2px solid #cbd5e0");
            btn.setStyle("background",    "#ffffff");
            btn.setStyle("cursor",        "pointer");
            btn.setStyle("transition",    "all 0.12s");
            btn.addClickListener(e -> enterNumber(num));
            numPadLayout.add(btn);
        }
    }

    // ── Interaction ───────────────────────────────────────────────

    private void onCellClick(int row, int col) {
        if (gameWon) return;
        if (cells[row][col].isGiven()) { selectedRow = selectedCol = -1; }
        else { selectedRow = row; selectedCol = col; }
        refreshAllCells();
    }

    private void enterNumber(int num) {
        if (gameWon || selectedRow < 0 || selectedCol < 0) return;
        CellModel cell = cells[selectedRow][selectedCol];
        if (cell.isGiven()) return;

        if (noteMode) {
            cell.setValue(0);
            cell.setWrong(false);
            cell.toggleNote(num);
        } else {
            cell.clearNotes();
            cell.setValue(num);
            cell.setWrong(!engine.isCorrect(selectedRow, selectedCol, num));
        }
        refreshCell(selectedRow, selectedCol);
        checkWin();
    }

    private void eraseSelected() {
        if (selectedRow < 0 || selectedCol < 0) return;
        CellModel cell = cells[selectedRow][selectedCol];
        if (cell.isGiven()) return;
        cell.setValue(0);
        cell.clearNotes();
        cell.setWrong(false);
        refreshCell(selectedRow, selectedCol);
    }

    private void toggleNoteMode() {
        noteMode = !noteMode;
        updateNoteModeButton();
    }

    private void updateNoteModeButton() {
        if (noteModeBtn == null) return;
        noteModeBtn.setText(noteMode ? "✏️ Notes: ON" : "✏️ Notes: OFF");
        noteModeBtn.setStyle("background",   noteMode ? "#ebf8ff" : "#ffffff");
        noteModeBtn.setStyle("border-color", noteMode ? "#3182ce" : "#cbd5e0");
        noteModeBtn.setStyle("color",        noteMode ? "#2b6cb0" : "#2d3748");
    }

    private void refreshAllCells() {
        if (cells == null || cellDivs == null) return;
        int size = engine.getSize();
        for (int r = 0; r < size; r++)
            for (int c = 0; c < size; c++)
                refreshCell(r, c);
    }

    private void refreshCell(int row, int col) {
        applyCellBg(cellDivs[row][col], cells[row][col], row == selectedRow && col == selectedCol);
        renderCellContent(cellDivs[row][col], cells[row][col], engine.getSize());
    }

    // ── Win check ─────────────────────────────────────────────────

    private void checkWin() {
        int size = engine.getSize();
        for (int r = 0; r < size; r++)
            for (int c = 0; c < size; c++) {
                CellModel cm = cells[r][c];
                if (cm.isEmpty() || cm.isWrong()) return;
                if (cm.getValue() != engine.getSolutionValue(r, c)) return;
            }
        gameWon = true;
        stopTimer();
        statusLabel.setText("🎉 Solved in " + formatTime(elapsedSeconds) + "! Well done!");
    }

    // ── Timer ─────────────────────────────────────────────────────

    private void startTimer() {
        gameTimer = new Timer(true);
        gameTimer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() {
                if (!gameWon) { elapsedSeconds++; updateTimerLabel(); }
            }
        }, 1000, 1000);
    }

    private void stopTimer() {
        if (gameTimer != null) { gameTimer.cancel(); gameTimer = null; }
    }

    private void updateTimerLabel() {
        if (timerLabel != null) timerLabel.setText("⏱ " + formatTime(elapsedSeconds));
    }

    private String formatTime(int s) {
        return String.format("%02d:%02d", s / 60, s % 60);
    }

    // ── Style helpers ─────────────────────────────────────────────

    private void styleCtrlBtn(Button b, boolean primary) {
        b.setStyle("font-size",     "clamp(0.8rem, 2.5vw, 0.95rem)");
        b.setStyle("padding",       "clamp(6px,1.5vw,9px) clamp(12px,3vw,20px)");
        b.setStyle("border-radius", "8px");
        b.setStyle("white-space",   "nowrap");
        b.setStyle("cursor",        "pointer");
        if (primary) {
            b.setStyle("background",   "#3182ce");
            b.setStyle("color",        "white");
            b.setStyle("border-color", "#3182ce");
        }
    }
}
