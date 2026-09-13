// 勇者测试 前端浏览器端到端联调脚本（playwright-core + 已安装的 Chrome）
const { chromium } = require('playwright-core');

const BASE = process.env.E2E_BASE || 'http://localhost:5173';
const SHOTS = 'C:/Users/31201/WorkBuddy/2026-09-12-17-46-34/shots/';
const results = [];
const ok = (name, cond, detail = '') =>
  results.push((cond ? '[PASS] ' : '[FAIL] ') + name + (detail ? '  ' + detail : ''));

(async () => {
  const browser = await chromium.launch({
    executablePath: 'C:\\Users\\31201\\.agent-browser\\browsers\\chrome-153.0.8010.36\\chrome.exe',
    headless: true,
  });
  const page = await browser.newPage({ viewport: { width: 1366, height: 860 } });
  const errors = [];
  const badUrls = [];
  page.on('pageerror', (e) => errors.push('pageerror: ' + e.message.slice(0, 120)));
  page.on('console', (m) => {
    if (m.type() === 'error') errors.push('console: ' + m.text().slice(0, 120));
  });
  page.on('response', (r) => {
    if (r.status() >= 400) badUrls.push(r.status() + ' ' + r.url());
  });
  const user = 'webhero' + Date.now() % 100000;

  // 1. 登录页
  await page.goto(BASE + '/login', { waitUntil: 'networkidle' }).catch(() => {});
  await page.waitForTimeout(800);
  ok('登录页渲染', (await page.textContent('.title')).includes('勇者测试'));
  await page.screenshot({ path: SHOTS + '01-login.png', fullPage: true });

  // 2. 注册
  await page.click('#tab-register');
  await page.fill('#pane-register .el-form-item:nth-child(1) input', user);
  await page.fill('#pane-register .el-form-item:nth-child(2) input', 'WebHero');
  await page.fill('#pane-register .el-form-item:nth-child(3) input', 'test123');
  await page.click('#pane-register .el-button');
  await page.waitForTimeout(1500);
  ok('注册反馈', (await page.textContent('.el-message').catch(() => '')).includes('觉醒'));

  // 3. 登录 -> 任务大厅
  await page.fill('#pane-login .el-form-item:nth-child(1) input', user);
  await page.fill('#pane-login .el-form-item:nth-child(2) input', 'test123');
  await page.click('#pane-login .el-button');
  await page.waitForURL('**/tasks', { timeout: 8000 }).catch(() => {});
  await page.waitForTimeout(1200);
  ok('登录跳转任务大厅', page.url().includes('/tasks'), page.url());
  const taskBody = await page.textContent('body');
  ok('任务大厅有行者善事任务',
     ['照看独居老人', '清理公园', '社区送温暖'].some((k) => taskBody.includes(k)));
  await page.screenshot({ path: SHOTS + '02-tasks.png', fullPage: true });

  // 4. 冒险剧情：进入序章第一个节点
  await page.goto(BASE + '/story', { waitUntil: 'networkidle' }).catch(() => {});
  await page.waitForTimeout(1200);
  const storyBody = await page.textContent('body');
  ok('剧情页章节中文正常', storyBody.includes('灰色的城') && !storyBody.includes('ç'));
  await page.click('.el-button--primary').catch(() => {});
  await page.waitForTimeout(1500);
  const nodeBody = await page.textContent('body');
  ok('进入章节可见剧情节点', !nodeBody.includes('ç¬') && (nodeBody.includes('选择') || nodeBody.includes('你')));
  await page.screenshot({ path: SHOTS + '03-story.png', fullPage: true });

  // 5. 修身养成（自我激励系统）：真实创建一次自我任务
  await page.goto(BASE + '/self', { waitUntil: 'networkidle' }).catch(() => {});
  await page.waitForTimeout(1000);
  await page.fill('input[placeholder*="跑步"]', 'E2E 冒烟任务').catch(() => {});
  await page.click('.el-button--primary').catch(() => {});
  await page.waitForTimeout(1200);
  const testBody = await page.textContent('body');
  ok('修身养成页可用（自我任务/技能树）',
     testBody.includes('自我任务') && testBody.includes('技能树') && testBody.includes('修行币'));
  await page.screenshot({ path: SHOTS + '04-test.png', fullPage: true });

  // 6. 商城
  await page.goto(BASE + '/shop', { waitUntil: 'networkidle' }).catch(() => {});
  await page.waitForTimeout(1000);
  const shopBody = await page.textContent('body');
  ok('商城可见净化剂', shopBody.includes('净化剂'));
  await page.screenshot({ path: SHOTS + '05-shop.png', fullPage: true });

  // 7. 知识宝库
  await page.goto(BASE + '/knowledge', { waitUntil: 'networkidle' }).catch(() => {});
  await page.waitForTimeout(1000);
  await page.screenshot({ path: SHOTS + '06-knowledge.png', fullPage: true });

  // 8. 我的档案
  await page.goto(BASE + '/profile', { waitUntil: 'networkidle' }).catch(() => {});
  await page.waitForTimeout(1500);
  const profBody = await page.textContent('body');
  ok('档案显示D级', profBody.includes('D'));
  ok('档案有勇者榜/流水', profBody.includes('勇者榜') && profBody.includes('流水'));
  await page.screenshot({ path: SHOTS + '07-profile.png', fullPage: true });

  const realErrors = errors.filter((e) => !e.includes('favicon'));
  ok('无前端运行时错误', realErrors.length === 0, realErrors.slice(0, 3).join(' | '));
  const api404 = badUrls.filter((u) => u.includes('/api/'));
  ok('无API 404', api404.length === 0, api404.slice(0, 3).join(' | '));

  console.log(results.join('\n'));
  console.log(realErrors.length ? 'JS_ERRORS:\n' + realErrors.join('\n') : 'NO_JS_ERRORS');
  if (badUrls.length) console.log('BAD_URLS:\n' + badUrls.slice(0, 5).join('\n'));
  await browser.close();
})().catch((e) => {
  console.error('SCRIPT_ERR', e.message);
  process.exit(1);
});
