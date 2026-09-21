/* ============================================================
   app.js —— 全站工具与渲染层
   把四个端重复的时间格式化、空态、状态徽章、顶栏渲染、分区路由
   收敛到这里。单页专属的业务逻辑仍留在各页 <script> 内。
   ============================================================ */
window.SM = window.SM || {};

(function (SM) {
  'use strict';

  SM.$ = function (id) { return document.getElementById(id); };

  /* ---------- 时间 ---------- */

  //后端返回的是 ISO(UTC) 时间，统一转成本地时间显示（直接截字符串会差8小时甚至差一天）
  SM.fmtTime = function (d) {
    if (!d) return '';
    var t = new Date(d);
    if (isNaN(t.getTime())) return String(d);
    var p = function (n) { return String(n).padStart(2, '0'); };
    return t.getFullYear() + '-' + p(t.getMonth() + 1) + '-' + p(t.getDate()) +
      ' ' + p(t.getHours()) + ':' + p(t.getMinutes());
  };

  SM.fmtDate = function (d) { return SM.fmtTime(d).substring(0, 10); };

  /* ---------- 图标 ---------- */

  //返回 <i data-lucide> 占位，由 lucide 在渲染后替换成 SVG
  SM.icon = function (name, cls) {
    return '<i data-lucide="' + name + '" class="ico' + (cls ? ' ' + cls : '') + '"></i>';
  };

  //lucide 只在被调用时替换当前已存在的占位元素，动态 innerHTML 渲染出来的不会自动生效。
  //用 MutationObserver 兜住，省得在每个渲染函数末尾手工补一次 createIcons()
  SM.initIcons = function () {
    if (!window.lucide) return;
    window.lucide.createIcons();
    var pending = false;
    new MutationObserver(function () {
      if (pending) return;
      pending = true;
      requestAnimationFrame(function () {
        pending = false;
        window.lucide.createIcons();
      });
    }).observe(document.body, { childList: true, subtree: true });
  };

  /* ---------- 空态 / 状态徽章 ---------- */

  SM.empty = function (text) { return '<div class="empty">' + text + '</div>'; };

  SM.tag = function (text, kind) {
    return '<span class="tag tag--' + kind + '">' + text + '</span>';
  };

  //各业务的状态→颜色表。四张表内容不同，不合并
  SM.STATUS_TAG = {
    reg:     { '待支付': 'warn', '已支付': 'success', '已取消': 'neutral', '已完成': 'info' },
    consult: { '待接诊': 'warn', '问诊中': 'info', '已结束': 'success' },
    rx:      { '待缴费': 'warn', '待发药': 'info', '已完成': 'success', '已作废': 'neutral' },
    pay:     { '待支付': 'warn', '已支付': 'success', '已退款': 'neutral', '已取消': 'neutral' }
  };

  SM.statusTag = function (kind, status) {
    var map = SM.STATUS_TAG[kind] || {};
    return SM.tag(status, map[status] || 'neutral');
  };

  /* ---------- 顶栏身份区 ---------- */

  /**
   * 顶栏右侧唯一的身份入口：身份 + 切换身份 + 退出。
   * 全站都要登录才进得来，所以这里不再有「未登录」分支，
   * 也不会再出现两个登录按钮（原来患者端是弹窗入口 + 医护登录链接各一个）。
   *
   * cfg.elId       容器 id（患者端 'userArea'，员工端 'staffArea'）
   * cfg.user       身份对象
   * cfg.icon       身份图标名
   * cfg.label      u => 显示文案
   * cfg.logoutFn   退出时 javascript: 调用的全局函数名
   * cfg.logoutText 退出按钮文案（患者端是「退出」，员工端是「退出登录」）
   */
  SM.renderTopbar = function (cfg) {
    var el = SM.$(cfg.elId);
    if (!el) return;
    el.innerHTML = '<span class="who">' + SM.icon(cfg.icon) + ' ' + cfg.label(cfg.user) +
      ' <a href="/login.html" title="登录其他身份的工作端">切换身份</a>' +
      ' <a href="javascript:' + cfg.logoutFn + '()">' + (cfg.logoutText || '退出') + '</a></span>';
  };

  /* ---------- 分区路由（患者端 / 管理端） ---------- */

  /**
   * cfg.sections     合法分区 key 数组
   * cfg.defaultKey   默认分区
   * cfg.navSelector  导航按钮选择器，如 '.nav button' / '.side button'
   * cfg.navId        导航容器 id，用于绑定点击
   * cfg.onLoad       s => 该分区的加载逻辑
   * 返回 activate(s)，供 goReg 之类的跨分区跳转复用
   */
  SM.router = function (cfg) {
    function activate(s) {
      if (cfg.sections.indexOf(s) === -1) s = cfg.defaultKey;
      document.querySelectorAll(cfg.navSelector).forEach(function (b) {
        b.classList.toggle('active', b.dataset.s === s);
      });
      document.querySelectorAll('.section').forEach(function (x) {
        x.classList.toggle('active', x.id === 's-' + s);
      });
      cfg.onLoad(s);
      //用 replaceState 而不是改 location.hash：不产生历史记录，也不会触发 hashchange 再切一次
      history.replaceState(null, '', s === cfg.defaultKey ? location.pathname : '#' + s);
    }

    if (cfg.navId) {
      SM.$(cfg.navId).addEventListener('click', function (e) {
        var btn = e.target.closest('button');
        if (!btn) return;
        activate(btn.dataset.s);
      });
    }
    return activate;
  };

  /* ---------- 弹层 ---------- */

  SM.mask = function (id, open) {
    var el = SM.$(id);
    if (el) el.style.display = open ? 'flex' : 'none';
  };
})(window.SM);
