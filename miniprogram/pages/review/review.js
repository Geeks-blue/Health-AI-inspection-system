// 老师复核页：列表 + 通过/不通过 + 下拉刷新
const { request, gotoLogin } = require('../../utils/request.js');
const app = getApp();

Page({
  data: {
    items: [],
    loading: false,
    user: null
  },

  onLoad() {
    const user = app.globalData.user;
    if (!user) { gotoLogin(); return; }
    if (user.role !== 'TEACHER' && user.role !== 'ADMIN') {
      wx.showToast({ title: '需要老师权限', icon: 'none' });
      setTimeout(() => gotoLogin(), 800);
      return;
    }
    this.setData({ user });
  },

  onShow() {
    if (!app.globalData.user) { gotoLogin(); return; }
    this.refresh();
  },

  /** 下拉刷新 */
  onPullDownRefresh() {
    this.refresh().then(() => wx.stopPullDownRefresh());
  },

  async refresh() {
    this.setData({ loading: true });
    try {
      const list = await request({ url: '/api/cleaning/review/list' });
      this.setData({ items: Array.isArray(list) ? list : [] });
    } catch (e) {
      wx.showToast({ title: e.message || '加载失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  async submit(e) {
    const { id, result } = e.currentTarget.dataset;
    wx.showLoading({ title: '提交中', mask: true });
    try {
      await request({
        url: '/api/cleaning/review/submit',
        method: 'POST',
        data: { recordId: id, result }
      });
      wx.showToast({
        title: result === 'pass' ? '已通过' : '已驳回',
        icon: 'success'
      });
      this.refresh();
    } catch (e) {
      wx.showToast({ title: e.message || '提交失败', icon: 'none' });
    } finally {
      wx.hideLoading();
    }
  }
});
