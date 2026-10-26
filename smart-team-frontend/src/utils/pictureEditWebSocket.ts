

/**
 * 图片编辑WebSocke]t服务
 * 功能：管理后端WebSocket连接，处理图片编辑相关消息
 */
export class PictureEditWebSocket {
  private socket: WebSocket | null = null;                           // WebSocket实例

  private pictureId: number;

  private eventHandlers: any;


  constructor(pictureId: number) {
    this.pictureId = pictureId;
    this.socket = null // WebSocket实例
    this.eventHandlers = {};
  }

  /**
   * 建立WebSocket连接
   */
  public connect(): void {
    // 避免重复连接
    if (this.socket && (this.socket.readyState === WebSocket.OPEN || this.socket.readyState === WebSocket.CONNECTING)) {
      return;
    }
    // 支持两种配置：
    // 1. 完整地址（ws://host/api/ws/...），开发环境常用；
    // 2. 相对路径（/api/ws/...），生产环境按当前页面协议与域名自动拼接 wss/ws。
    const configuredUrl = import.meta.env.VITE_BASE_WS_API as string;
    const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const baseUrl = configuredUrl.startsWith('ws://') || configuredUrl.startsWith('wss://')
      ? configuredUrl
      : `${wsProtocol}//${window.location.host}${configuredUrl}`;
    const url = `${baseUrl}?pictureId=${this.pictureId}`;
    this.socket = new WebSocket(url);

    // 设置携带cookie
    this.socket.binaryType = 'blob';

    // 连接成功回调
    this.socket.onopen = () => {
      console.log('WebSocket连接成功');
      this.triggerEvent('open');
    };

    // 监听消息
    this.socket.onmessage = (event: MessageEvent) => {
      try {
        const message = JSON.parse(event.data);
        const type = message.type
        // 触发事件
        this.triggerEvent(type, message)
      } catch (error) {
        console.error('消息处理失败:', error);
      }
    };

    // 错误回调
    this.socket.onerror = (error) => {
      console.error('WebSocket错误:', error);
      this.triggerEvent('error', error);
    };

    // 连接关闭回调
    this.socket.onclose = (event) => {
      console.log('WebSocket连接关闭:', event.code, event.reason);
      this.triggerEvent('close', event)
    };



  }


  /**
   * 触发事件
   * @param type 事件类型
   * @param data 消息数据
   */
  triggerEvent(type: string, data?: any) {
    const handler = this.eventHandlers[type];
    if (handler) {
      handler.forEach((event: any) => event(data));    }
  }

  /**
   * 添加自定义事件监听
   */
  on(type: string, handler: (data?: any) => void) {
    if (!this.eventHandlers[type]) {
      this.eventHandlers[type] = [];
    }
    this.eventHandlers[type].push(handler);
  }

  /**
   * 手动断开 websocket 连接
   */
  disconnect() {
    if (this.socket) {
      this.socket.close();
      console.log('webSocket 连接已手动断开')
    }
  }

  /**
   * 发送消息
   */
  sendMessage(message: any) {
    if (this.socket && this.socket.readyState === WebSocket.OPEN) {
      this.socket.send(JSON.stringify(message));
    } else {
      console.error('WebSocket未连接， 无法发送消息:', message)
    }
  }
}








