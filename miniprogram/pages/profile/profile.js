// 我的页：展示用户信息 + 退出登录
const { gotoLogin, userInitial } = require('../../utils/request.js');
const app = getApp();

Page({
  data: {
    user: null,
    initial: '',
    roleLabel: ''
  },

  onShow() {
    const user = app.globalData.user;
    if (!user) { gotoLogin(); return; }
    const roleMap = { STUDENT: '学生', TEACHER: '老师', ADMIN: '管理员' };
    this.setData({
      user,
      initial: userInitial(user),
      roleLabel: roleMap[user.role] || user.role
    });
  },

  /** 退出登录 */
  logout() {
    wx.showModal({
      title: '退出登录',
      content: '确定要退出当前账号吗？',
      success: (res) => {
        if (!res.confirm) return;
        app.clearUser();
        gotoLogin();
      }
    });
  }
});
