package com.geeksblue.inspection.ai;

import java.util.ArrayList;
import java.util.List;

/**
 * Container of detection results aggregated by zone in the photo.
 * Populated by the AI service and consumed by {@link CleaningRuleEngine}.
 */
public class DetectionResult {

    /** Large items of trash on the floor (e.g. boxes, big bags). */
    private final List<String> floorBigTrash = new ArrayList<>();

    /** Trash or beverage bottles spread across desks. */
    private final List<String> deskTrash = new ArrayList<>();

    /** Visible piles on the teacher's podium. */
    private final List<String> podiumPiles = new ArrayList<>();

    /** Trash bin contents overflowing the rim. */
    private final List<String> binOverflow = new ArrayList<>();

    public List<String> getFloorBigTrash() { return floorBigTrash; }
    public List<String> getDeskTrash() { return deskTrash; }
    public List<String> getPodiumPiles() { return podiumPiles; }
    public List<String> getBinOverflow() { return binOverflow; }
}
