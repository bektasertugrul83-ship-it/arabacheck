// Ana pencere: sayfaya yalnızca "ilanı uygulama içinde aç" köprüsünü verir
const { contextBridge, ipcRenderer } = require("electron");
contextBridge.exposeInMainWorld("ArabaCheckDesktop", {
  openBrowser: (url, auto) => ipcRenderer.send("ac:open-browser", String(url || ""), !!auto),
});
