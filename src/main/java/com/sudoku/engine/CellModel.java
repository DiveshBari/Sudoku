package com.sudoku.engine;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a single Sudoku cell's runtime state:
 * - entered value (0 = empty)
 * - given flag (pre-filled, not editable)
 * - wrong flag (highlighted red)
 * - pencil notes (candidate numbers shown as mini-grid)
 */
public class CellModel {

    private int value;
    private final boolean given;
    private boolean wrong;
    private final Set<Integer> notes = new HashSet<>();

    public CellModel(int value, boolean given) {
        this.value = value;
        this.given = given;
    }

    public int getValue()              { return value; }
    public void setValue(int value)    { this.value = value; }
    public boolean isGiven()           { return given; }
    public boolean isWrong()           { return wrong; }
    public void setWrong(boolean w)    { this.wrong = w; }
    public boolean isEmpty()           { return value == 0; }
    public Set<Integer> getNotes()     { return notes; }

    public void toggleNote(int n) {
        if (notes.contains(n)) notes.remove(n);
        else notes.add(n);
    }

    public void clearNotes() { notes.clear(); }
}
