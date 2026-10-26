package com.normal.picturebackend.infrastucture.api.imagesearch.sub;

import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.normal.picturebackend.infrastucture.api.imagesearch.model.ImageSearchResult;
import com.normal.picturebackend.infrastucture.exception.BusinessException;
import com.normal.picturebackend.infrastucture.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 获取识图图片集合接口 (step 2)
 */
@Slf4j
public class GetImageListApi {


    /**
     * 获取图片集合接口
     * @param apiUrl 后端数据返回集合
     * @return 包装后的图片集合
     */
    public static List<ImageSearchResult> getImageListApi(String apiUrl) {

        try {
            // 1. 发起get请求
            HttpResponse response = HttpUtil.createGet(apiUrl)
                    .execute();

            // 2. 获取处理响应
            int status = response.getStatus();
            String body = response.body();

            // 校验响应状态码
            if (status == 200) {
                // 解析响应结果
                return processResponse(body);
            }else {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
            }

        } catch (Exception e) {
            log.error("获取图片集合接口失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取图片列表失败");
        }
    }

    private static List<ImageSearchResult> processResponse(String body) {
        JSONObject jsonObject = new JSONObject(body);
        if (!jsonObject.containsKey("data")){
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未找到图片列表");
        }
        JSONObject data = jsonObject.getJSONObject("data");
        JSONArray list = data.getJSONArray("list");
        return JSONUtil.toList(list, ImageSearchResult.class);
    }

    public static void main(String[] args) {
        String url = "https://graph.baidu.com/ajax/pcsimi?carousel=503&entrance=GENERAL&extUiData%5BisLogoShow%5D=1&inspire=general_pc&limit=30&next=2&render_type=card&session_id=18250884991634806863&sign=126d9e97cd54acd88139901747670303&tk=f933d&tpl_from=pc";
        List<ImageSearchResult> imageListApi = getImageListApi(url);

        System.out.println(imageListApi);
    }


}
