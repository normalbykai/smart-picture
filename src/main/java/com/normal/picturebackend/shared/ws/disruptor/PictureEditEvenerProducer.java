package com.normal.picturebackend.shared.ws.disruptor;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.normal.picturebackend.shared.ws.model.PictureEditRequestMessage;
import com.normal.picturebackend.domain.user.entity.User;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * 图片编辑事件生产者
 */
@Component
@Slf4j
public class PictureEditEvenerProducer {

    @Resource(name = "pictureEditDisruptor")
    private Disruptor<PictureEditEvent> pictureEditDisruptor;

    /**
     * 发布事件
     *
     * @param pictureEditRequestMessage
     * @param session
     * @param user
     * @param pictureId
     */
    public void publishEvent(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) {
        RingBuffer<PictureEditEvent> ringBuffer = pictureEditDisruptor.getRingBuffer();

        // 获取可以写入的位置
        long next = ringBuffer.next();

        // 拿到对应位置的事件进行设置
        PictureEditEvent pictureEditEvent = ringBuffer.get(next);
        pictureEditEvent.setPictureId(pictureId);
        pictureEditEvent.setUser(user);
        pictureEditEvent.setSession(session);
        pictureEditEvent.setMessage(pictureEditRequestMessage);

        // 发布事件到队列等待处理
        ringBuffer.publish(next);
    }

    /**
     * 优雅停机
     */
    @PreDestroy
    public void close() {
        pictureEditDisruptor.shutdown();
    }
}
