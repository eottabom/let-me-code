// DevTools 를 연 브라우저 창을 그대로 캡처한다. macOS 전용 (screencapture, 화면 기록 권한 필요).
// page.screenshot() 은 페이지 영역만 찍고 DevTools 는 찍지 못해서, 창 단위로 찍는다.
//
// usage: node devshot.js <console|network> <caseIndex[,caseIndex...]> <out.png>
// env  : H=창 높이(기본 720), CHROME=브라우저 실행 파일 (기본 Playwright Chromium)
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');
const os = require('os');
const { execSync } = require('child_process');

const [panel, caseIndex, out] = process.argv.slice(2);
const executablePath = process.env.CHROME || chromium.executablePath();
const frontmostApp = execSync(`osascript -e 'tell application "System Events" to get bundle identifier of first application process whose frontmost is true'`).toString().trim();

(async () => {
  // DevTools 설정은 프로필 Preferences 로 미리 넣어둔다 (아래쪽 도킹, 패널 선택, 언어 안내 배너 끄기 등).
  const profile = fs.mkdtempSync(path.join(os.tmpdir(), 'cors-capture-'));
  fs.mkdirSync(path.join(profile, 'Default'), { recursive: true });
  fs.writeFileSync(path.join(profile, 'Default', 'Preferences'), JSON.stringify({
    devtools: {
      preferences: {
        'panel-selected-tab': JSON.stringify(panel),
        currentDockState: '"bottom"',
        'network-log.preserve-log': 'true',
        'network-log-show-overview': 'false',
        'disable-locale-info-bar': 'true',
      },
    },
  }));

  const context = await chromium.launchPersistentContext(profile, {
    executablePath,
    headless: false,
    viewport: null,
    args: ['--auto-open-devtools-for-tabs', '--window-position=40,40', `--window-size=1280,${process.env.H || 720}`, '--lang=en-US'],
  });
  const page = context.pages()[0] || await context.newPage();
  await page.goto(`http://localhost:5173/?case=${caseIndex}`);
  // 새 창이 키보드 포커스를 가져가면, 그 사이 입력한 글자가 주소창이나 DevTools 필터에 찍힌다.
  // 원래 앱으로 포커스를 돌려준다. screencapture -l 은 뒤에 있는 창도 찍을 수 있다.
  await page.waitForTimeout(1000);
  execSync(`osascript -e 'tell application id "${frontmostApp}" to activate'`);
  await page.waitForTimeout(1500);

  const buttons = await page.locator('button').count();
  for (let i = 0; i < buttons; i++) {
    await page.locator('button').nth(i).evaluate((button) => button.click());
    await page.waitForTimeout(400);
  }
  await page.waitForTimeout(2500);

  // 브라우저 창 번호를 찾아서 그 창만 찍는다 (다른 창이 위에 있어도 된다).
  const owner = path.basename(executablePath);
  const windowId = execSync(`${__dirname}/winid "${owner}"`).toString().trim().split('\n')[0].split(' ')[0];
  execSync(`screencapture -x -o -l ${windowId} ${out}`);
  await context.close();
})();
