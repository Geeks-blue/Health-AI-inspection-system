// 登录页：选择身份 + 填用户 ID，落盘后跳到对应首页
const app = getApp();

Page({
  data: {
    userId: '',
    role: 'STUDENT',
    roles: [
      { key: 'STUDENT', label: '学生', desc: '上传教室照片',  icon: '📷' },
      { key: 'TEACHER', label: '老师', desc: '复核待审记录',  icon: '👩‍🏫' },
      { key: 'ADMIN',   label: '管理员', desc: '查看统计数据', icon: '📊' }
    ]
  },

  onLoad() {
    // 已登录则直接跳过登录页
    if (app.globalData.user && app.globalData.user.id) {
      this.gotoHome(app.globalData.user.role);
    }
  },

  onUserIdInput(e) { this.setData({ userId: e.detail.value }); },

  pickRole(e) { this.setData({ role: e.currentTarget.dataset.key }); },

  submit() {
    const id = this.data.userId.trim();
    if (!id) {
      wx.showToast({ title: '请填写用户 ID', icon: 'none' });
      return;
    }
    app.setUser({ id, role: this.data.role });
    this.gotoHome(this.data.role);
  },

  /** 按身份跳到默认首页 */
  gotoHome(role) {
    if (role === 'TEACHER') wx.switchTab({ url: '/pages/review/review' });
    else                   wx.switchTab({ url: '/pages/upload/upload' });
  }
});
