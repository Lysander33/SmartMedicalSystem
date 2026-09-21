/* ============================================================
   auth.js —— 会话与鉴权层
   四个端原本各有一份逐字相同的 api()/clearAuth()/logout()，
   以及患者端一份行为略有差异的版本，这里用配置吸收差异。

   令牌与身份按角色分槽存放（token_patient / token_doctor ...），
   同一个浏览器同时登多个端互不覆盖。后缀必须是后端 Role 的英文枚举值。
   ============================================================ */
window.SM = window.SM || {};

(function (SM) {
  'use strict';

  // 旧版本遗留的 key，登录成功或退出时清掉，避免页面读到过期身份
  var LEGACY_KEYS = ['token', 'user', 'role', 'patient', 'doctor', 'doctor_token', 'staff'];

  SM.LEGACY_KEYS = LEGACY_KEYS;

  /**
   * 创建一个端的会话对象。
   *
   * cfg.role          localStorage key 后缀，必填（'patient'|'doctor'|'pharmacy'|'admin'）
   * cfg.redirect      令牌失效/退出后跳转目标
   * cfg.replaceOn401  401 时用 location.replace 而非 href（默认 false）
   *                   注意：只作用于 401 分支，logout 一律用 href —— 这是患者端原有的差异
   * cfg.alertOn401    401 时是否 alert（默认 true，患者端为 false）
   * cfg.throwOn401    401 之后是否抛异常（默认 true，患者端为 false，它会继续往下 return）
   * cfg.catchNetwork  是否把网络异常吞成 {code:-1}（默认 true，患者端为 false，它向上抛）
   */
  SM.createSession = function (cfg) {
    cfg = cfg || {};
    var role = cfg.role;
    var keyToken = 'token_' + role;
    var keyUser = 'user_' + role;
    var redirect = cfg.redirect || '/login.html';
    var replaceOn401 = !!cfg.replaceOn401;
    var alertOn401 = cfg.alertOn401 !== false;
    var throwOn401 = cfg.throwOn401 !== false;
    var catchNetwork = cfg.catchNetwork !== false;

    var S = {
      role: role,
      token: localStorage.getItem(keyToken) || '',
      user: null,

      hasAuth: function () { return !!(S.token && S.user); },

      clearAuth: function () {
        localStorage.removeItem(keyToken);
        localStorage.removeItem(keyUser);
        LEGACY_KEYS.forEach(function (k) { localStorage.removeItem(k); });
        S.token = '';
        S.user = null;
      },

      // 登录成功后调用：写分槽 key，并把身份对象挂到会话上
      save: function (token, profile) {
        S.token = token || '';
        S.user = profile || null;
        if (S.token) localStorage.setItem(keyToken, S.token);
        if (S.user) localStorage.setItem(keyUser, JSON.stringify(S.user));
        if (cfg.purgeOnSave) LEGACY_KEYS.forEach(function (k) { localStorage.removeItem(k); });
      },

      api: async function (url, opts) {
        opts = opts || {};
        var headers = Object.assign({}, opts.headers || {});
        //每次请求现读令牌，不能在闭包里缓存
        if (S.token) headers['X-Token'] = S.token;

        var r;
        if (catchNetwork) {
          try {
            var res = await fetch(url, Object.assign({}, opts, { headers }));
            r = await res.json();
          } catch (e) {
            return { code: -1, msg: '网络异常：' + e.message };
          }
        } else {
          var res2 = await fetch(url, Object.assign({}, opts, { headers }));
          r = await res2.json();
        }

        if (r && r.code === 401) {
          if (alertOn401) alert('登录状态已失效，请重新登录');
          S.clearAuth();
          if (replaceOn401) location.replace(redirect); else location.href = redirect;
          if (throwOn401) throw new Error('401');
        }
        return r;
      },

      logout: async function () {
        try { await S.api('/api/auth/logout', { method: 'POST' }); } catch (e) { /* 忽略，本地照样清 */ }
        S.clearAuth();
        location.href = redirect;
      }
    };

    if (S.token) S.user = JSON.parse(localStorage.getItem(keyUser) || 'null');
    return S;
  };

  /**
   * 登录页专用：role 由服务端响应给出（data.role），不是用户选的页签，
   * 所以不能挂在固定 role 的会话对象上。行为与登录页原实现一致 —— 写完清遗留 key。
   */
  SM.saveLogin = function (token, role, profile) {
    localStorage.setItem('token_' + role, token);
    localStorage.setItem('user_' + role, JSON.stringify(profile));
    LEGACY_KEYS.forEach(function (k) { localStorage.removeItem(k); });
  };
})(window.SM);
