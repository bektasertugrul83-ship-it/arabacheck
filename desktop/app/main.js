const { app, BrowserWindow, shell, Menu, ipcMain } = require("electron");
const path = require("path");
let mainWin = null, listingWin = null;

// İlan sayfası mı? (otomatik okuma için)
const looksLikeListing = t => !!t && t.length > 300 && /(TL|₺)/.test(t) && /(İlan No|İlan Bilgileri|Kilometre|KM)/.test(t);
const BTN_JS = `(() => { if (document.getElementById("ac-analyze")) return;
  const b = document.createElement("button"); b.id = "ac-analyze"; b.textContent = "Bu ilanı ArabaCheck ile analiz et";
  b.style.cssText = "position:fixed;right:20px;bottom:20px;z-index:2147483647;height:52px;padding:0 22px;border:0;border-radius:14px;background:#081530;color:#fff;font:700 15px system-ui,sans-serif;box-shadow:0 10px 30px rgba(8,21,48,.45);cursor:pointer";
  b.onclick = () => window.acBridge && window.acBridge.send(document.body ? document.body.innerText : "", location.href);
  document.documentElement.appendChild(b); })()`;

function openListingWindow(url, auto){
  if (!/^https?:/i.test(url)) url = "https://www.sahibinden.com/kategori/otomobil";
  if (listingWin && !listingWin.isDestroyed()) listingWin.close();
  const w = listingWin = new BrowserWindow({
    width: 1180, height: 880, parent: mainWin || undefined, title: "İlan — ArabaCheck", backgroundColor: "#ffffff", autoHideMenuBar: true,
    webPreferences: { preload: path.join(__dirname, "browser-preload.js"), contextIsolation: true, sandbox: true, spellcheck: false },
  });
  let sent = false, tries = 0;
  const tryAuto = async () => {
    if (sent || w.isDestroyed()) return;
    tries++;
    try {
      const text = await w.webContents.executeJavaScript("document.body ? document.body.innerText : ''");
      if (looksLikeListing(text)) { sent = true; deliver(text, w.webContents.getURL()); return; }
    } catch {}
    if (tries < 6) setTimeout(tryAuto, 1500);
  };
  w.webContents.on("did-finish-load", () => {
    w.webContents.executeJavaScript(BTN_JS).catch(() => {});
    if (auto && !sent) { tries = 0; setTimeout(tryAuto, 1500); }
  });
  // İlan penceresindeki yeni sekme istekleri aynı pencerede açılsın; uygulama yönlendirmeleri yok sayılsın
  w.webContents.setWindowOpenHandler(({ url }) => { if (/^https?:/i.test(url)) w.loadURL(url); return { action: "deny" }; });
  w.loadURL(url);
}
function deliver(text, url){
  if (!mainWin || mainWin.isDestroyed()) return;
  mainWin.webContents.executeJavaScript(`window.acReceiveListing && acReceiveListing(${JSON.stringify(text)}, ${JSON.stringify(url || "")})`).catch(() => {});
  if (mainWin.isMinimized()) mainWin.restore();
  mainWin.focus();
  if (listingWin && !listingWin.isDestroyed()) listingWin.close();
}
ipcMain.on("ac:open-browser", (_e, url, auto) => openListingWindow(url, auto));
ipcMain.on("ac:listing", (e, text, url) => { if (listingWin && e.sender === listingWin.webContents) deliver(text, url); });

function createWindow() {
  const win = new BrowserWindow({
    width: 1280,
    height: 860,
    minWidth: 380,
    minHeight: 600,
    title: "ArabaCheck",
    backgroundColor: "#081530",
    icon: path.join(__dirname, "icon.png"),
    autoHideMenuBar: true,
    show: false,
    webPreferences: { preload: path.join(__dirname, "preload.js"), contextIsolation: true, sandbox: true, spellcheck: false },
  });
  mainWin = win;
  win.once("ready-to-show", () => win.show());
  // Uygulama görünümü (sekmeler, geçmiş) + masaüstü düzeni
  win.loadFile(path.join(__dirname, "index.html"), { query: { app: "1", desktop: "1" } });

  // İlan siteleri (sahibinden, arabam…) varsayılan tarayıcıda açılsın.
  const isExternal = (url) => /^https?:/i.test(url);
  win.webContents.setWindowOpenHandler(({ url }) => {
    if (isExternal(url)) shell.openExternal(url);
    return { action: "deny" };
  });
  win.webContents.on("will-navigate", (e, url) => {
    if (isExternal(url)) { e.preventDefault(); shell.openExternal(url); }
  });
}

Menu.setApplicationMenu(Menu.buildFromTemplate([
  { label: "ArabaCheck", submenu: [
    { label: "Yeni analiz", accelerator: "CmdOrCtrl+N", click: (_, w) => w && w.webContents.executeJavaScript('document.querySelector("#ab-new") && (resetForm(), goHome())') },
    { label: "Geçmiş", accelerator: "CmdOrCtrl+H", click: (_, w) => w && w.webContents.executeJavaScript('document.querySelector("[data-tab=history]").click()') },
    { type: "separator" },
    { label: "Yeniden yükle", role: "reload" },
    { label: "Tam ekran", role: "togglefullscreen" },
    { type: "separator" },
    { label: "Yakınlaştır", role: "zoomIn" },
    { label: "Uzaklaştır", role: "zoomOut" },
    { label: "Gerçek boyut", role: "resetZoom" },
    { type: "separator" },
    { label: "Çıkış", role: "quit" },
  ]},
  { label: "Düzen", submenu: [
    { label: "Geri al", role: "undo" }, { label: "Yinele", role: "redo" }, { type: "separator" },
    { label: "Kes", role: "cut" }, { label: "Kopyala", role: "copy" }, { label: "Yapıştır", role: "paste" },
    { label: "Tümünü seç", role: "selectAll" },
  ]},
]));

if (!app.requestSingleInstanceLock()) app.quit();
app.on("second-instance", () => {
  const w = BrowserWindow.getAllWindows()[0];
  if (w) { if (w.isMinimized()) w.restore(); w.focus(); }
});
app.whenReady().then(createWindow);
app.on("window-all-closed", () => { if (process.platform !== "darwin") app.quit(); });
app.on("activate", () => { if (BrowserWindow.getAllWindows().length === 0) createWindow(); });
