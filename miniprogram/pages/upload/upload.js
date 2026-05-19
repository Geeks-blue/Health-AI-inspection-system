// 学生上传页：拍照 + 选教室 → 调 POST /api/cleaning/check
const { uploadFile } = require('../../utils/request.js');

Page({
  data: {
    classroomId: '',     // 教室编号，由用户输入或扫码
    photoPath: '',       // 本地图片临时路径
    submitting: false,   // 防重复提交
    lastResult: null     // 上一次的判定结果
  },

  /** 输入教室编号 */
  onClassroomInput(e) {
    this.setData({ classroomId: e.detail.value });
  },

  /** 拍照或从相册选图（一次只允许一张） */
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

  /** 提交：调用后端 /api/cleaning/check */
  async submit() {
    if (!this.data.classroomId) {
      wx.showToast({ title: '请填写教室编号', icon: 'none' });
      return;
    }
    if (!this.data.photoPath) {
      wx.showToast({ title: '请先拍照', icon: 'none' });
      return;
    }
    this.setData({ submitting: true });
    try {
      // 后端要求：multipart 字段名 photo，form 字段 classroomId
      const body = await uploadFile({
        url: '/api/cleaning/check',
        filePath: this.data.photoPath,
        name: 'photo',
        formData: { classroomId: this.data.classroomId }
      });
      this.setData({ lastResult: body });
      const tip = body.result === 'pass' ? '✅ AI 判定合格' : '⚠️ 已提交，待老师复核';
      wx.showToast({ title: tip, icon: 'none' });
    } catch (err) {
      console.error('上传失败', err);
      wx.showToast({ title: '上传失败，稍后重试', icon: 'none' });
    } finally {
      this.setData({ submitting: false });
    }
  }
});
