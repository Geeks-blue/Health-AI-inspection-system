// 统一封装 wx.request / wx.uploadFile，带鉴权头 + 统一错误处理
const app = getApp();

function fullUrl(path) {
  return app.globalData.baseUrl + path;
}

function authHeader() {
  const u = app.globalData.user || {};
  return {
    'X-User-Id': u.id || 'anonymous',
    'X-User-Role': u.role || 'STUDENT'
  };
}

/** 普通 JSON 请求 */
function request({ url, method = 'GET', data }) {
  return new Promise((resolve, reject) => {
    wx.request({
      url: fullUrl(url),
      method,
      data,
      header: { 'content-type': 'application/json', ...authHeader() },
      success: (res) => {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          resolve(res.data);
        } else {
          const msg = (res.data && res.data.error) || ('HTTP ' + res.statusCode);
          const err = new Error(msg);
          err.statusCode = res.statusCode;
          err.body = res.data;
          reject(err);
        }
      },
      fail: (e) => reject(new Error(e.errMsg || '网络错误'))
    });
  });
}

/** 文件上传 (multipart/form-data) */
function uploadFile({ url, filePath, name, formData }) {
  return new Promise((resolve, reject) => {
    wx.uploadFile({
      url: fullUrl(url),
      filePath,
      name,
      formData,
      header: authHeader(),
      success: (res) => {
        let body = {};
        try { body = res.data ? JSON.parse(res.data) : {}; } catch (e) { /* ignore */ }
        if (res.statusCode >= 200 && res.statusCode < 300) {
          resolve(body);
        } else {
          const err = new Error((body && body.error) || ('HTTP ' + res.statusCode));
          err.statusCode = res.statusCode;
          err.body = body;
          reject(err);
        }
      },
      fail: (e) => reject(new Error(e.errMsg || '上传失败'))
    });
  });
}

/** 跳到登录页（公用方法） */
function gotoLogin() {
  wx.reLaunch({ url: '/pages/login/login' });
}

/** 取出用户首字符，用作头像占位 */
function userInitial(user) {
  if (!user || !user.id) return '?';
  const s = String(user.id);
  return s.slice(0, 2).toUpperCase();
}

module.exports = { request, uploadFile, gotoLogin, userInitial };
