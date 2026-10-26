package com.normal.picturebackend.infrastucture.api.imagesearch.sub;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONUtil;
import com.normal.picturebackend.infrastucture.exception.BusinessException;
import com.normal.picturebackend.infrastucture.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 获取图片url分页地址 (step 1)
 */
@Slf4j
public class GetImageUrlPageApi {

    /**
     * 通过URL地址获取到相似URL分页地址
     */
    public static String getImageUrlPage(String url) {
        // 1. 拼接URL的时间戳 刷新缓存
        long timeMillis = System.currentTimeMillis();
        String requestUrl = "https://graph.baidu.com/upload?uptime=" + timeMillis;
        // 2. 添加请求头
        // https://normal-picture-1346305499.cos.ap-chongqing.myqcloud.com/space/1923347038406180865/2025-05-16_PWAlHU0atMkpJLFK.jpeg
        Map<String, Object> formData = new HashMap<>();
        formData.put("image", url);
        formData.put("tn", "pc");
        formData.put("from", "pc");
        formData.put("image_source", "PC_UPLOAD_URL");
        try {
            // 3. 发送识图请求
            HttpResponse response = HttpRequest.post(requestUrl)
                    .header("acs-token", RandomUtil.randomString(1))
                    .form(formData)
                    .timeout(5000)
                    .execute();
            // 校验返回结构
            if (response.getStatus() != HttpStatus.HTTP_OK) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到该资源");
            }
            // 4. 解析结果
            String body = response.body();
            Map<String, Object> bodyMap = JSONUtil.toBean(body, Map.class);
            if (bodyMap == null && !Integer.valueOf(0).equals(bodyMap.get("status"))) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到该资源");
            }

            // 拿到url地址
            Map<String, Object> data = (Map<String, Object>) bodyMap.get("data");

            // 为了防止编码格式错误 进行一次URL编译
            String searchResultUrl = URLUtil.decode((String) data.get("url"), StandardCharsets.UTF_8);
            // 返回搜索结果URL
            return searchResultUrl;
        } catch (Exception e) {
            log.error("搜索失败:{}", e.getMessage());
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "搜索失败");
        }
    }

    public static void main(String[] args) {
        String url = "https://www.codefather.cn/logo.png";
        String ulr2 = "https://normal-picture-1346305499.cos.ap-chongqing.myqcloud.com/space/1924655719764791298/2025-05-20_Bw1OfIRmvAX3Pmz6.jpg";
        String imageUrlPage = getImageUrlPage(ulr2);
        System.out.println("搜索成功， 结果URL:" + imageUrlPage);
    }
}
