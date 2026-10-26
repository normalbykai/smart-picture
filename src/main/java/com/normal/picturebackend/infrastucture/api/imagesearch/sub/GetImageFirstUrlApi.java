package com.normal.picturebackend.infrastucture.api.imagesearch.sub;

import com.normal.picturebackend.infrastucture.exception.BusinessException;
import com.normal.picturebackend.infrastucture.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

/**
 * 获取图片第一个 (step 3)
 */
@Slf4j
public class GetImageFirstUrlApi {

    /**
     * 获得图片相似数据地址
     */
    public static String getImageFirstUrl(String url) {

        try {
            // 1. 获取url地址的DOM节点
            Document document = Jsoup.connect(url)
                    .timeout(5000)
                    .get();
            // 获取Script标签
            Elements scripts = document.select("script");
            for (Element script: scripts) {
                // 拿到script标签中 firstUrl的值
                String scriptContent = script.html();
                if (scriptContent.contains("firstUrl")) {
                    // 使用正则匹配 firstUrl的值
                    String s = extractFirstUrlFromScript(scriptContent);
                    if (s != null) {
                        return s;
                    }
                }
            }
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "解析未找到识图集合URL");

        }catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    private static String extractFirstUrlFromScript(String scriptContent) {
        // 使用正则表达式匹配JSON格式的firstUrl
        java.util.regex.Pattern pattern =
                java.util.regex.Pattern.compile("\"firstUrl\"\\s*:\\s*\"([^\"]+)\"");
        java.util.regex.Matcher matcher = pattern.matcher(scriptContent);

        if (matcher.find()) {
            String group = matcher.group(1);
            // 将group中的 \/ 所有替换为 /
            group = group.replaceAll("\\\\/", "/");
            return group;
        }
        return null;
    }

    public static void main(String[] args) {
        String imageUrlPage = GetImageUrlPageApi.getImageUrlPage("https://www.codefather.cn/logo.png");
        System.out.println(imageUrlPage);
        String imageFirstUrl = getImageFirstUrl(imageUrlPage);
        System.out.println(imageFirstUrl);
    }
}
