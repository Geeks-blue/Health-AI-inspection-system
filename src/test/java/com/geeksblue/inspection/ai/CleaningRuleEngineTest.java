package com.geeksblue.inspection.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CleaningRuleEngineTest {

    private final CleaningRuleEngine engine = new CleaningRuleEngine();

    @Test
    void cleanRoomPasses() {
        DetectionResult d = new DetectionResult();
        var j = engine.judge(d);
        assertEquals(CleaningRuleEngine.PASS, j.result());
        assertEquals("floor_ok, desk_ok, podium_ok, bin_ok", j.reason());
    }

    @Test
    void floorTrashTriggersReview() {
        DetectionResult d = new DetectionResult();
        d.getFloorBigTrash().add("box");
        var j = engine.judge(d);
        assertEquals(CleaningRuleEngine.REVIEW, j.result());
    }

    @Test
    void overflowingBinTriggersReview() {
        DetectionResult d = new DetectionResult();
        d.getBinOverflow().add("trashcan");
        var j = engine.judge(d);
        assertEquals(CleaningRuleEngine.REVIEW, j.result());
    }
}
