const { request, BASE_URL } = require('../../utils/request');

Page({
  data: {
    sessionId: '',
    messages: [],
    inputValue: '',
    loading: false,
    scrollToView: ''
  },

  onLoad() {
    // 生成或获取sessionId
    let sessionId = wx.getStorageSync('ai_session_id');
    if (!sessionId) {
      sessionId = this.generateSessionId();
      wx.setStorageSync('ai_session_id', sessionId);
    }
    this.setData({ sessionId });

    // 加载历史对话
    this.loadChatHistory();

    // 添加欢迎消息
    if (this.data.messages.length === 0) {
      this.addMessage('system', '你好！我是AI电影助手，我可以为你推荐电影、解答电影相关问题。试试问我"推荐喜剧电影"吧！');
    }
  },

  // 生成sessionId
  generateSessionId() {
    const timestamp = Date.now();
    const random = Math.random().toString(36).substring(2, 8).toUpperCase();
    return `${timestamp}${random}`;
  },

  // 加载历史对话
  loadChatHistory() {
    const history = wx.getStorageSync('ai_chat_history') || [];
    this.setData({
      messages: history
    });
    this.scrollToBottom();
  },

  // 保存对话历史
  saveChatHistory() {
    wx.setStorageSync('ai_chat_history', this.data.messages);
  },

  // 添加消息
  addMessage(type, content) {
    const messages = this.data.messages;
    const message = {
      type: type,
      content: content,
      time: this.formatTime(new Date())
    };

    // 如果是AI消息，需要格式化渲染内容
    if (type === 'ai') {
      message.renderedContent = this.formatAIContent(content);
    }

    messages.push(message);
    this.setData({ messages });
    this.saveChatHistory();
    this.scrollToBottom();
  },

  // 格式化AI回复内容（支持Markdown基本语法）
  formatAIContent(content) {
    if (!content) return '';

    let formatted = content;

    // 先保护已有的HTML标签（表情符号等）
    const htmlTags = [];
    formatted = formatted.replace(/<[^>]+>/g, (match) => {
      const index = htmlTags.length;
      htmlTags.push(match);
      return `__HTML_TAG_${index}__`;
    });

    // 转义HTML特殊字符
    formatted = formatted
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/&/g, '&amp;');

    // 恢复HTML标签
    formatted = formatted.replace(/__HTML_TAG_(\d+)__/g, (match, index) => {
      return htmlTags[parseInt(index)];
    });

    // 处理加粗 **text**
    formatted = formatted.replace(/\*\*(.*?)\*\*/g, '<strong style="color: #FF5722; font-weight: bold;">$1</strong>');

    // 处理列表项
    formatted = formatted.replace(/^\* (.+)$/gm, '<p style="margin: 10rpx 0; padding-left: 30rpx;">• $1</p>');

    // 处理数字列表
    formatted = formatted.replace(/^\d+\. (.+)$/gm, '<p style="margin: 10rpx 0; padding-left: 30rpx;">$1</p>');

    // 处理段落（双换行）
    formatted = formatted.replace(/\n\n/g, '</p><p style="margin: 15rpx 0;">');

    // 用p标签包裹整个内容
    formatted = '<p style="margin: 15rpx 0;">' + formatted + '</p>';

    // 处理单个换行为br
    formatted = formatted.replace(/\n/g, '<br/>');

    return formatted;
  },

  // 发送消息
  async onSend() {
    const input = this.data.inputValue.trim();
    if (!input) {
      wx.showToast({ title: '请输入内容', icon: 'none' });
      return;
    }

    if (this.data.loading) {
      wx.showToast({ title: '正在回复中...', icon: 'none' });
      return;
    }

    // 添加用户消息
    this.addMessage('user', input);
    this.setData({ inputValue: '', loading: true });

    try {
      const res = await request({
        url: `${BASE_URL.AI}/ai/agent/chat`,
        method: 'POST',
        data: {
          sessionId: this.data.sessionId,
          message: input
        }
      });

      this.setData({ loading: false });

      if (res.code === 200 || res.code === "200") {
        const aiReply = res.data?.message || res.data || '抱歉，我没有理解您的问题，请重新表述。';
        this.addMessage('ai', aiReply);
      } else {
        this.addMessage('ai', res.msg || '抱歉，服务暂时不可用，请稍后再试。');
      }
    } catch (err) {
      this.setData({ loading: false });
      this.addMessage('ai', '抱歉，网络错误，请检查网络连接后重试。');
      console.error(err);
    }
  },

  // 输入框输入
  onInput(e) {
    this.setData({ inputValue: e.detail.value });
  },

  // 滚动到底部
  scrollToBottom() {
    const viewId = `msg-${this.data.messages.length - 1}`;
    this.setData({ scrollToView: viewId });
  },

  // 清空对话
  clearChat() {
    wx.showModal({
      title: '清空对话',
      content: '确定要清空所有对话记录吗？',
      success: (res) => {
        if (res.confirm) {
          this.setData({ messages: [] });
          this.saveChatHistory();
          wx.showToast({ title: '已清空', icon: 'success' });
        }
      }
    });
  },

  // 格式化时间
  formatTime(date) {
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${hours}:${minutes}`;
  }
});
