package com.normal.picturebackend.infrastructure.utils;

import java.awt.*;

/**
 * 主色调相似计算工具
 */
public class MainColorSimilarUtils {

    /**
     * 计算相似度
     * @param color1
     * @param color2
     * @return
     */
    public static double calculateSimilarity(Color color1, Color color2) {
        // 计算欧氏差距离
        double distance =Math.sqrt(Math.pow(color1.getRed() - color2.getRed(), 2) +
                Math.pow(color1.getGreen() - color2.getGreen(), 2) +
                Math.pow(color1.getBlue() - color2.getBlue(), 2));
        // 计算相似度
        return 1 - distance / (255 * Math.sqrt(3));
    }
}
