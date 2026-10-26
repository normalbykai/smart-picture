package com.normal.picturebackend.shared.ws;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.normal.picturebackend.shared.ws.disruptor.PictureEditEvenerProducer;
import com.normal.picturebackend.shared.ws.model.PictureEditActionEnum;
import com.normal.picturebackend.shared.ws.model.PictureEditMessageTypeEnum;
import com.normal.picturebackend.shared.ws.model.PictureEditRequestMessage;
import com.normal.picturebackend.shared.ws.model.PictureEditResponseMessage;
import com.normal.picturebackend.domain.user.entity.User;
import com.normal.picturebackend.domain.user.service.UserDomainService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WS处理器， 连接成功，连接失败，接受客户端信息进行相应处理
 */
@Component
@Slf4j
public class PictureEditHandler extends TextWebSocketHandler {

    // 当前正在编辑的用户 pictureId -> userId
    public final Map<Long, Long> pictureEditUsers = new ConcurrentHashMap<>();

    // ws连接会话 pictureId -> sessions
    public final Map<Long, Set<WebSocketSession>> pictureEditSessions = new ConcurrentHashMap<>();

    @Resource
    private UserDomainService userDomainService;
    @Autowired
    private PictureEditEvenerProducer pictureEditEvenerProducer;


    /**
     * 建立WS连接
     *
     * @param session
     * @throws Exception
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        User user = (User) session.getAttributes().get("user");

        // 初次建立连接加入会话
        pictureEditSessions.putIfAbsent(pictureId, ConcurrentHashMap.newKeySet());
        pictureEditSessions.get(pictureId).add(session);

        // 构造响应结构
        PictureEditResponseMessage responseMessage = new PictureEditResponseMessage();
        responseMessage.setType(PictureEditMessageTypeEnum.ENTER_EDIT.getValue());

        // 返回正在编辑的用户信息：连接建立时可能还没有人进入编辑状态，必须判空
        Long editingUserId = pictureEditUsers.get(pictureId);
        User editingUser = editingUserId == null ? null : userDomainService.getById(editingUserId);
        String message = String.format("%s用户加入编辑", user.getUserName());
        if (editingUser != null) {
            message = String.format("%s用户加入编辑，当前编辑者：%s", user.getUserName(), editingUser.getUserName());
        }
        responseMessage.setUser(userDomainService.getUserVO(editingUser));
        responseMessage.setMessage(message);

        // 给同一张图片的用户 广播发送信息
        broadcastToPicture(pictureId, responseMessage);
    }


    /**
     * 处理客户端编辑操作信息
     *
     * @param session
     * @param message
     * @throws Exception
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 解析message为 PictureEditRequestMessage
        PictureEditRequestMessage pictureEditRequestMessage = JSONUtil.toBean(message.getPayload(), PictureEditRequestMessage.class);
        String type = pictureEditRequestMessage.getType();
        PictureEditMessageTypeEnum pictureEditMessageTypeEnum = PictureEditMessageTypeEnum.getEnumByValue(type);
        // 拿到公共资源
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        User user = (User) session.getAttributes().get("user");

        // 发布事件到队列等待处理
        pictureEditEvenerProducer.publishEvent(pictureEditRequestMessage, session, user, pictureId);
    }


    /**
     * 客户端断开连接回调
     *
     * @param session
     * @param status
     * @throws Exception
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // 拿到公共数据
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        User user = (User) session.getAttributes().get("user");

        // 移除当前用户编辑状态
        handleExitEditMessage(null, session, pictureId, user);

        // 删除会话
        Set<WebSocketSession> webSocketSessions = pictureEditSessions.get(pictureId);
        if (webSocketSessions != null) {
            webSocketSessions.remove(session);
            if (webSocketSessions.isEmpty()) {
                pictureEditSessions.remove(pictureId);
            }
        }

        // 响应
        String message = "用户" + user.getUserName() + "离开编辑";
        PictureEditResponseMessage responseMessage = new PictureEditResponseMessage(
                PictureEditMessageTypeEnum.INFO.getValue(),
                message,
                null,
                userDomainService.getUserVO(user));

        // 通知
        broadcastToPicture(pictureId, responseMessage);

    }

    /**
     * 处理编辑操作消息
     *
     * @param pictureEditRequestMessage
     * @param session
     * @param pictureId
     * @param user
     */
    public void handleEditActionMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, Long pictureId, User user) throws IOException {
        Long editingUserId = pictureEditUsers.get(pictureId);
        String editAction = pictureEditRequestMessage.getEditAction();
        PictureEditActionEnum actionEnum = PictureEditActionEnum.getEnumByValue(editAction);
        if (actionEnum == null) {
            // 无动作
            return;
        }

        // 确认是当前操作者
        if (editingUserId != null && editingUserId.equals(user.getId())) {
            // 开始编辑操作
            PictureEditResponseMessage responseMessage = new PictureEditResponseMessage();
            responseMessage.setType(PictureEditMessageTypeEnum.EDIT_ACTION.getValue());
            responseMessage.setEditAction(editAction);
            responseMessage.setUser(userDomainService.getUserVO(user));
            responseMessage.setMessage(actionEnum.getText());
            responseMessage.setEditAction(actionEnum.getValue());

            // 发起广播通知用户之外的其他用户
            broadcastToPicture(pictureId, responseMessage, session);
        }

    }

    /**
     * 处理编辑退出信息
     *
     * @param pictureEditRequestMessage
     * @param session
     * @param pictureId
     * @param user
     */
    public void handleExitEditMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, Long pictureId, User user) throws IOException {
        Long editingUserId = pictureEditUsers.get(pictureId);
        if (editingUserId != null && editingUserId.equals(user.getId())) {
            // 移除编辑中用户
            pictureEditUsers.remove(pictureId);

            // 构造响应，通知外部
            PictureEditResponseMessage responseMessage = new PictureEditResponseMessage();
            responseMessage.setType(PictureEditMessageTypeEnum.EXIT_EDIT.getValue());
            responseMessage.setUser(userDomainService.getUserVO(user));
            String message = "用户 " + user.getUserName() + " 退出编辑状态";
            responseMessage.setMessage(message);

            // 发送广播
            broadcastToPicture(pictureId, responseMessage);
        }

    }

    /**
     * 执行编辑进入
     *
     * @param pictureEditRequestMessage
     * @param session
     * @param pictureId
     * @param user
     */
    public void handleEnterEditMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, Long pictureId, User user) throws IOException {
        // 没有用户正在编辑才能进入
        if (!pictureEditUsers.containsKey(pictureId)) {
            pictureEditUsers.put(pictureId, user.getId());
            // 构建响应信息
            PictureEditResponseMessage responseMessage = new PictureEditResponseMessage();
            responseMessage.setType(PictureEditMessageTypeEnum.ENTER_EDIT.getValue());
            responseMessage.setMessage("进入编辑状态");
            responseMessage.setUser(userDomainService.getUserVO(user));
            String message = String.format("%s开始编辑图片", user.getUserName());
            responseMessage.setMessage(message);
            // 发送数据
            broadcastToPicture(pictureId, responseMessage);
        }

    }

    /**
     * 发起广播 (可以进行排除不发送的会话)
     *
     * @param pictureId
     * @param responseMessage
     * @param excludeSession  排除的会话
     */
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage responseMessage, WebSocketSession excludeSession) throws IOException {
        // 拿到当前图片协同的所有会话
        Set<WebSocketSession> webSocketSessions = pictureEditSessions.get(pictureId);

        // 配置序列化： 将Long类型转换为String类型, 解决精度丢失
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
        simpleModule.addSerializer(Long.TYPE, ToStringSerializer.instance); // 支持long基本类型
        objectMapper.registerModule(simpleModule);
        String messageJson = objectMapper.writeValueAsString(responseMessage);

        // 遍历所有会话并发送数据
        if (CollUtil.isNotEmpty(webSocketSessions)) {
            for (WebSocketSession webSocketSession : webSocketSessions) {
                // 排除不要发送的会话
                if (excludeSession != null && excludeSession.equals(webSocketSession)) {
                    continue;
                }
                // 跳过已经关闭的会话
                if (!webSocketSession.isOpen()) {
                    continue;
                }
                // 向连接到的会话都发送信息
                webSocketSession.sendMessage(new TextMessage(messageJson));
            }
        }
    }


    /**
     * 发起全部广播
     *
     * @param pictureId
     * @param responseMessage
     */
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage responseMessage) throws IOException {
        broadcastToPicture(pictureId, responseMessage, null);
    }

}


