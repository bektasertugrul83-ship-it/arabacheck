// Web sayfasını uygulamaya kopyalar ve Android simgesinden Windows .ico üretir.
const fs = require("fs"), path = require("path");
const root = path.join(__dirname, "..");
fs.copyFileSync(path.join(root, "..", "index.html"), path.join(root, "app", "index.html"));
const png = fs.readFileSync(path.join(root, "..", "android", "icon-512.png"));
fs.copyFileSync(path.join(root, "..", "android", "icon-512.png"), path.join(root, "app", "icon.png"));
// Birden çok boyutta PNG gömülü ICO (Windows Vista+)
const sizes = [256, 48, 32, 16];
const pngs = sizes.map(n => fs.readFileSync(path.join(__dirname, `icon-${n}.png`)));
const header = Buffer.alloc(6); header.writeUInt16LE(0, 0); header.writeUInt16LE(1, 2); header.writeUInt16LE(sizes.length, 4);
let offset = 6 + 16 * sizes.length;
const entries = sizes.map((n, i) => {
  const e = Buffer.alloc(16);
  e.writeUInt8(n >= 256 ? 0 : n, 0); e.writeUInt8(n >= 256 ? 0 : n, 1);
  e.writeUInt16LE(1, 4); e.writeUInt16LE(32, 6); e.writeUInt32LE(pngs[i].length, 8); e.writeUInt32LE(offset, 12);
  offset += pngs[i].length; return e;
});
fs.writeFileSync(path.join(__dirname, "icon.ico"), Buffer.concat([header, ...entries, ...pngs]));
console.log("index.html ve simgeler hazır");
