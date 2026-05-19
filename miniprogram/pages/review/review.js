// 老师复核页：拉列表 + 逐条 pass/fail
const { request } = require('../../utils/request.js');

Page({
  data: {
    list: [],          // 待复核列表
    loading: false
  },

  onShow() {
    this.refresh();
  },

  /** 拉取待复核列表 */
  async refresh() {
    this.setData({ loading: true });
    try {
      const list = await request({ url: '/api/cleaning/review/list' });
      this.setData({ list: Array.isArray(list) ? list : [] });
    } catch (err) {
      console.error('拉列表失败', err);
      wx.showToast({ title: '加载失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  /** 提交复核结果：data-id 与 data-result 通过 wx:for 在 wxml 中绑定 */
  async submit(e) {
    const { id, result } = e.currentTarget.dataset;
    try {
      await request({
        url: '/api/cleaning/review/submit',
        method: 'POST',
        data: { recordId: id, result }
      });
      wx.showToast({ title: '已提交', icon: 'success' });
      this.refresh();
    } catch (err) {
      console.error('提交失败', err);
      wx.showToast({ title: '提交失败', icon: 'none' });
    }
  }
});
