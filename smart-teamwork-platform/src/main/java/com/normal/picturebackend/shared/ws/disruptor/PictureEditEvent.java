package com.normal.picturebackend.shared.ws.disruptor;

import com.normal.picturebackend.shared.ws.model.PictureEditRequestMessage;
import com.normal.picturebackend.domain.user.entity.User;
import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

/**
 * 图片编辑事件
 *
 */
@Data
public class PictureEditEvent {

    /**
     * 消息
     */
    private PictureEditRequestMessage message;

    /**
     * 当前用户session
     */
    private WebSocketSession session;

    /**
     * 当前用户
     */
    private User user;

    /**
     * 图片ID
     */
    private Long pictureId;
}
