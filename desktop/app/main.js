const { app, BrowserWindow, shell, Menu } = require("electron");
const path = require("path");

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
    webPreferences: { contextIsolation: true, sandbox: true, spellcheck: false },
  });
  win.once("ready-to-show", () => win.show());
  win.loadFile(path.join(__dirname, "index.html"));

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
