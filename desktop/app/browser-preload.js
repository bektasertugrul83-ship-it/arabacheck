// İlan penceresi (sahibinden / arabam): yalnızca görünen yazıyı ArabaCheck'e gönderme köprüsü
const { contextBridge, ipcRenderer } = require("electron");
contextBridge.exposeInMainWorld("acBridge", {
  send: (text, url) => ipcRenderer.send("ac:listing", String(text || "").slice(0, 60000), String(url || "")),
});
