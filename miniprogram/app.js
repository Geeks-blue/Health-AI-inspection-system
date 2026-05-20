// 全局：后端地址、登录态、wx.login 接入点
App({
  globalData: {
    // 部署后改成真实的后端 HTTPS 域名，并在小程序后台「服务器域名」白名单里配置
    baseUrl: 'https://your-backend.example.com',
    user: null
  },

  onLaunch() {
    // 从本地读取上次登录状态
    try {
      const u = wx.getStorageSync('cleaning_user');
      if (u && u.id) this.globalData.user = u;
    } catch (e) { /* ignore */ }

    // 演示 wx.login 流程：拿到 code 后应去后端换取 openid + 角色
    wx.login({
      success: (res) => {
        if (!res.code) return;
        // TODO: 联调时打开下面的请求
        // wx.request({ url: this.globalData.baseUrl + '/api/auth/wx-login',
        //   method: 'POST', data: { code: res.code },
        //   success: (r) => { this.setUser(r.data); } });
      }
    });
  },

  /** 设置登录态并落盘 */
  setUser(user) {
    this.globalData.user = user;
    wx.setStorageSync('cleaning_user', user);
  },

  /** 退出登录 */
  clearUser() {
    this.globalData.user = null;
    wx.removeStorageSync('cleaning_user');
  }
});
