package com.normal.picturebackend.shared.ws.disruptor;

import cn.hutool.json.JSONUtil;
import com.lmax.disruptor.WorkHandler;
import com.normal.picturebackend.shared.ws.PictureEditHandler;
import com.normal.picturebackend.shared.ws.model.PictureEditMessageTypeEnum;
import com.normal.picturebackend.shared.ws.model.PictureEditRequestMessage;
import com.normal.picturebackend.shared.ws.model.PictureEditResponseMessage;
import com.normal.picturebackend.domain.user.entity.User;
import com.normal.picturebackend.domain.user.service.UserDomainService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * 图片编辑事件消费者处理
 */
@Component
public class PictureEditEventConsumer implements WorkHandler<PictureEditEvent> {


    @Resource
    private PictureEditHandler pictureEditHandler;
    @Autowired
    private UserDomainService userDomainService;

    /**
     * 处理事件
     * @param pictureEditEvent
     * @throws Exception
     */
    @Override
    public void onEvent(PictureEditEvent pictureEditEvent) throws Exception {
        // 1. 解析图片编辑事件中的具体参数
        PictureEditRequestMessage pictureEditRequestMessage = pictureEditEvent.getMessage();
        WebSocketSession session = pictureEditEvent.getSession(); // 当前用户的会话
        User user = pictureEditEvent.getUser(); // 发送消息的用户信息
        Long pictureId = pictureEditEvent.getPictureId(); // 图片的id

        // 2. 获取图片编辑的类型, 根据图片编辑类型执行不同的操作
        String type = pictureEditRequestMessage.getType();
        PictureEditMessageTypeEnum pictureEditMessageTypeEnum = PictureEditMessageTypeEnum.getEnumByValue(type);
        switch (pictureEditMessageTypeEnum) {
            case EDIT_ACTION:
                // 执行编辑操作
                pictureEditHandler.handleEditActionMessage(pictureEditRequestMessage, session, pictureId, user);
                break;
            case ENTER_EDIT:
                // 执行进入编辑信息
                pictureEditHandler.handleEnterEditMessage(pictureEditRequestMessage, session, pictureId, user);
                break;
            case EXIT_EDIT:
                // 执行退出编辑信息
                pictureEditHandler.handleExitEditMessage(pictureEditRequestMessage, session, pictureId, user);
                break;
            default:
                // 请求处理消息错误
                // 定义消息响应
                PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage(
                        PictureEditMessageTypeEnum.ERROR.getValue(),
                        "未知消息类型",
                        null,
                        userDomainService.getUserVO(user) // todo 但是好像前端用不到这个 用户id为long 会有精度缺失问题
                );
                // 对当前会话发送该条消息
                session.sendMessage(new TextMessage(JSONUtil.toJsonStr(pictureEditResponseMessage)));
        }
    }
}
