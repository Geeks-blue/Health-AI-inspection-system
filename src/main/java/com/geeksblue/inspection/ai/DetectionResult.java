package com.geeksblue.inspection.ai;

import java.util.ArrayList;
import java.util.List;

/**
 * 检测结果聚合容器。
 *
 * <p>由 {@link AliyunVisionService} 填充，由 {@link CleaningRuleEngine} 消费，
 * 按教室内的四个区域分组：地面 / 桌面 / 讲台 / 垃圾桶。
 */
public class DetectionResult {

    /** 地面出现的明显大件垃圾（箱子、大袋等） */
    private final List<String> floorBigTrash = new ArrayList<>();

    /** 桌面成片的垃圾或饮料瓶 */
    private final List<String> deskTrash = new ArrayList<>();

    /** 讲台明显堆积物 */
    private final List<String> podiumPiles = new ArrayList<>();

    /** 垃圾桶溢出物 */
    private final List<String> binOverflow = new ArrayList<>();

    public List<String> getFloorBigTrash() { return floorBigTrash; }
    public List<String> getDeskTrash() { return deskTrash; }
    public List<String> getPodiumPiles() { return podiumPiles; }
    public List<String> getBinOverflow() { return binOverflow; }

    /**
     * 把另一份检测结果并入当前对象（取并集）。
     * 用于把「学生拍的」和「监控抓的」两次 AI 结果合并到一起再交给规则引擎判定。
     *
     * @param other 另一份检测结果；null 直接忽略
     */
    public void mergeFrom(DetectionResult other) {
        if (other == null) return;
        this.floorBigTrash.addAll(other.floorBigTrash);
        this.deskTrash.addAll(other.deskTrash);
        this.podiumPiles.addAll(other.podiumPiles);
        this.binOverflow.addAll(other.binOverflow);
    }
}
