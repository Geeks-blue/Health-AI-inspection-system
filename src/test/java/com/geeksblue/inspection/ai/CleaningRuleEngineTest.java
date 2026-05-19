package com.geeksblue.inspection.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 卫生判定规则引擎单元测试。
 */
class CleaningRuleEngineTest {

    private final CleaningRuleEngine engine = new CleaningRuleEngine();

    /** 全部干净 → 合格 */
    @Test
    void cleanRoomPasses() {
        DetectionResult d = new DetectionResult();
        var j = engine.judge(d);
        assertEquals(CleaningRuleEngine.PASS, j.result());
        assertEquals("floor_ok, desk_ok, podium_ok, bin_ok", j.reason());
    }

    /** 地面出现大件垃圾 → 待复核 */
    @Test
    void floorTrashTriggersReview() {
        DetectionResult d = new DetectionResult();
        d.getFloorBigTrash().add("box");
        var j = engine.judge(d);
        assertEquals(CleaningRuleEngine.REVIEW, j.result());
    }

    /** 垃圾桶溢出 → 待复核 */
    @Test
    void overflowingBinTriggersReview() {
        DetectionResult d = new DetectionResult();
        d.getBinOverflow().add("trashcan");
        var j = engine.judge(d);
        assertEquals(CleaningRuleEngine.REVIEW, j.result());
    }
}
