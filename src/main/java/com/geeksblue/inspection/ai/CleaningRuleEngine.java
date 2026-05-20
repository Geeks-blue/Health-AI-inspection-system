package com.geeksblue.inspection.ai;

import org.springframework.stereotype.Component;

/**
 * 卫生情况判定规则引擎（宽松版）。
 *
 * <p>命中以下任一条件 → review；否则 → pass：
 * <ul>
 *   <li>地面出现明显大件垃圾</li>
 *   <li>桌面成片垃圾或饮料瓶</li>
 *   <li>讲台明显堆积</li>
 *   <li>垃圾桶溢出</li>
 * </ul>
 */
@Component
public class CleaningRuleEngine {

    /** 合格（无需老师复核） */
    public static final String PASS = "pass";
    /** 待复核（交由老师判断） */
    public static final String REVIEW = "review";

    /**
     * 根据检测结果进行判定。
     *
     * @param detection 视觉检测结果
     * @return 判定结论（含 result 与 reason 文本）
     */
    public Judgement judge(DetectionResult detection) {
        StringBuilder reason = new StringBuilder();
        boolean review = false;

        // 地面：出现明显大件垃圾 → 复核
        if (!detection.getFloorBigTrash().isEmpty()) {
            reason.append("floor_big_trash, ");
            review = true;
        } else {
            reason.append("floor_ok, ");
        }

        // 桌面：成片垃圾 / 饮料瓶 → 复核
        if (!detection.getDeskTrash().isEmpty()) {
            reason.append("desk_trash, ");
            review = true;
        } else {
            reason.append("desk_ok, ");
        }

        // 讲台：明显堆积 → 复核
        if (!detection.getPodiumPiles().isEmpty()) {
            reason.append("podium_piles, ");
            review = true;
        } else {
            reason.append("podium_ok, ");
        }

        // 垃圾桶：溢出 → 复核
        if (!detection.getBinOverflow().isEmpty()) {
            reason.append("bin_overflow");
            review = true;
        } else {
            reason.append("bin_ok");
        }

        return new Judgement(review ? REVIEW : PASS, reason.toString());
    }

    /** 判定结论：result = pass / review；reason = 人类可读原因 */
    public record Judgement(String result, String reason) {}
}
