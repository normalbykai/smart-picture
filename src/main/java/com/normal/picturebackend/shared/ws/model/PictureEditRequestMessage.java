package com.normal.picturebackend.shared.ws.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图片编辑请求参数
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class PictureEditRequestMessage {

    /**
     * 消息类型， "ENTER_EDIT" "EXIT_EDIT" , "EDIT_ACTION"
     */
    private String type;

    /**
     * 执行编辑动作
     */
    private String editAction;
}
