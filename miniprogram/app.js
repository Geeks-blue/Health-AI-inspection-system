// 全局配置：后端地址、登录态、统一请求封装
App({
  // 全局数据
  globalData: {
    // ⚠️ 部署后请改成真实的后端域名，并在小程序「开发」→「服务器域名」白名单里配置
    baseUrl: 'https://your-backend.example.com',

    // 当前登录用户信息（由 wx.login + 后端换取得到，这里给出最小可用占位）
    // 真实接入步骤见 docs/miniprogram-integration.md
    user: {
      id: 'demo-user',     // 替换为后端返回的稳定用户 ID（如 openid 映射）
      role: 'STUDENT'       // STUDENT / TEACHER / ADMIN
    }
  },

  onLaunch() {
    // 启动时调用 wx.login 获取临时 code，再用 code 去后端换取用户身份
    // 这里只演示流程，未真正联调，避免在 demo 状态下网络失败
    wx.login({
      success: (res) => {
        if (!res.code) return;
        // TODO: 联调后打开下面的请求，使用 /api/auth/wx-login 之类的后端接口
        // wx.request({ url: this.globalData.baseUrl + '/api/auth/wx-login',
        //   method: 'POST', data: { code: res.code },
        //   success: (r) => {
        //     this.globalData.user.id = r.data.userId;
        //     this.globalData.user.role = r.data.role;
        //   } });
      }
    });
  }
});
