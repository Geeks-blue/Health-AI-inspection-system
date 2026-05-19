// 学生上传页：选教室（互斥锁） + 拍照 + 提交
const { request, uploadFile, gotoLogin, userInitial } = require('../../utils/request.js');
const app = getApp();

Page({
  data: {
    user: null,
    userInitial: '',
    rooms: [],           // 教室列表
    claimedRoom: null,   // 当前用户已锁定的教室 ID
    claimedRoomName: '',
    photoPath: '',       // 本地图片临时路径
    submitting: false,   // 防重复提交
    result: null         // 上一次的判定结果
  },

  onLoad() {
    const user = app.globalData.user;
    if (!user) { gotoLogin(); return; }
    this.setData({ user, userInitial: userInitial(user) });
  },

  onShow() {
    if (!app.globalData.user) { gotoLogin(); return; }
    this.refreshRooms();
    // 每 3 秒轮询一次教室占用状态
    this.pollTimer = setInterval(() => this.refreshRooms(), 3000);
  },

  onHide()   { this.stopPolling(); this.releaseIfClaimed(); },
  onUnload() { this.stopPolling(); this.releaseIfClaimed(); },

  stopPolling() {
    if (this.pollTimer) { clearInterval(this.pollTimer); this.pollTimer = null; }
  },

  /** 给后端打个释放请求；忽略错误 */
  releaseIfClaimed() {
    const id = this.data.claimedRoom;
    if (!id) return;
    request({ url: `/api/classrooms/${id}/release`, method: 'POST' }).catch(() => {});
  },

  /** 拉教室列表，并标记自己持有的那间 */
  async refreshRooms() {
    try {
      const rooms = await request({ url: '/api/classrooms' });
      const meId = this.data.user.id;
      const decorated = rooms.map(r => ({
        ...r,
        isSelf: r.locked && r.lockedBy === meId,
        isOther: r.locked && r.lockedBy !== meId
      }));
      const mine = decorated.find(r => r.isSelf);
      this.setData({
        rooms: decorated,
        claimedRoom: mine ? mine.id : null,
        claimedRoomName: mine ? (mine.name || mine.id) : ''
      });
    } catch (e) {
      // 拉取失败一般是后端不通，避免反复弹 Toast
      console.error('refresh rooms failed', e);
    }
  },

  /** 点教室卡片 */
  async pickRoom(e) {
    const room = e.currentTarget.dataset.room;
    if (!room || room.isOther) return;
    if (this.data.claimedRoom === room.id) return;

    try {
      const dto = await request({
        url: `/api/classrooms/${room.id}/claim`,
        method: 'POST'
      });
      this.setData({
        claimedRoom: dto.id,
        claimedRoomName: dto.name || dto.id
      });
      this.refreshRooms();
    } catch (e) {
      if (e.statusCode === 409) {
        wx.showToast({ title: '已被占用，请换一间', icon: 'none' });
        this.refreshRooms();
      } else {
        wx.showToast({ title: e.message || '占用失败', icon: 'none' });
      }
    }
  },

  /** 取消已锁的教室 */
  cancelRoom() {
    const id = this.data.claimedRoom;
    if (!id) return;
    request({ url: `/api/classrooms/${id}/release`, method: 'POST' })
      .then(() => {
        this.setData({ claimedRoom: null, claimedRoomName: '' });
        this.refreshRooms();
      })
      .catch(e => wx.showToast({ title: e.message || '释放失败', icon: 'none' }));
  },

  /** 拍照或从相册选图 */
  chooseImage() {
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: ['camera', 'album'],
      sizeType: ['compressed'],
      success: (res) => {
        this.setData({ photoPath: res.tempFiles[0].tempFilePath });
      }
    });
  },

  removePhoto() { this.setData({ photoPath: '' }); },

  /** 提交：上传 → 后端判定 → 后端自动释放锁 */
  async submit() {
    if (!this.data.claimedRoom) {
      wx.showToast({ title: '请先选择教室', icon: 'none' });
      return;
    }
    if (!this.data.photoPath) {
      wx.showToast({ title: '请先拍照', icon: 'none' });
      return;
    }
    this.setData({ submitting: true });
    try {
      const body = await uploadFile({
        url: '/api/cleaning/check',
        filePath: this.data.photoPath,
        name: 'photo',
        formData: { classroomId: this.data.claimedRoom }
      });
      this.setData({
        result: body,
        photoPath: '',
        claimedRoom: null,
        claimedRoomName: ''
      });
      wx.showToast({
        title: body.result === 'pass' ? 'AI 判定合格' : '已转老师复核',
        icon: 'success'
      });
      this.refreshRooms();
    } catch (e) {
      wx.showToast({ title: e.message || '上传失败', icon: 'none' });
    } finally {
      this.setData({ submitting: false });
    }
  }
});
