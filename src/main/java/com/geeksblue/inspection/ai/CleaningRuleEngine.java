package com.geeksblue.inspection.ai;

import org.springframework.stereotype.Component;

/**
 * Applies the lenient classroom-cleaning rules from the design doc.
 *
 * <ul>
 *   <li>Obvious large trash on the floor → review</li>
 *   <li>Patches of trash or beverage bottles on desks → review</li>
 *   <li>Visible piles on the podium → review</li>
 *   <li>Overflowing trash bin → review</li>
 *   <li>Otherwise → pass</li>
 * </ul>
 */
@Component
public class CleaningRuleEngine {

    public static final String PASS = "pass";
    public static final String REVIEW = "review";

    public Judgement judge(DetectionResult detection) {
        StringBuilder reason = new StringBuilder();
        boolean review = false;

        if (!detection.getFloorBigTrash().isEmpty()) {
            reason.append("floor_big_trash, ");
            review = true;
        } else {
            reason.append("floor_ok, ");
        }

        if (!detection.getDeskTrash().isEmpty()) {
            reason.append("desk_trash, ");
            review = true;
        } else {
            reason.append("desk_ok, ");
        }

        if (!detection.getPodiumPiles().isEmpty()) {
            reason.append("podium_piles, ");
            review = true;
        } else {
            reason.append("podium_ok, ");
        }

        if (!detection.getBinOverflow().isEmpty()) {
            reason.append("bin_overflow");
            review = true;
        } else {
            reason.append("bin_ok");
        }

        return new Judgement(review ? REVIEW : PASS, reason.toString());
    }

    public record Judgement(String result, String reason) {}
}
