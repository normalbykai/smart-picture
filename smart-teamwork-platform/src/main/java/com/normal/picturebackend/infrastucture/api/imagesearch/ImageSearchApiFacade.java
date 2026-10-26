package com.normal.picturebackend.infrastucture.api.imagesearch;

import com.normal.picturebackend.infrastucture.api.imagesearch.model.ImageSearchResult;
import com.normal.picturebackend.infrastucture.api.imagesearch.sub.GetImageFirstUrlApi;
import com.normal.picturebackend.infrastucture.api.imagesearch.sub.GetImageListApi;
import com.normal.picturebackend.infrastucture.api.imagesearch.sub.GetImageUrlPageApi;

import java.util.List;

/**
 * 门面模式 外部只需要提供一个图片URL地址就可以实现所有步骤
 */
public class ImageSearchApiFacade {

    public static List<ImageSearchResult> searchImage(String url) {
        // 获取图片URL页面
        String imageUrlPage = GetImageUrlPageApi.getImageUrlPage(url);

        // 获取图片URL
        String imageUrl = GetImageFirstUrlApi.getImageFirstUrl(imageUrlPage);

        List<ImageSearchResult> imageSearchResults = GetImageListApi.getImageListApi(imageUrl);
        return imageSearchResults;
    }

    /**
     * 测试接口
     * @param args
     */
    public static void main(String[] args) {
        //List<ImageSearchResult> imageUrl = searchImage("https://normal-picture-1346305499.cos.ap-chongqing.myqcloud.com/space/1924655719764791298//2025-05-20_bkvzVwmU6gz6JbVr.jpeg");
        List<ImageSearchResult> imageUrl =searchImage("https://www.codefather.cn/logo.png");
        System.out.println(imageUrl);
    }
}
