import { readFile } from "node:fs/promises";
import { resolve } from "node:path";

const checks = [
  {
    file: "index.html",
    mustInclude: "id=\"root\"",
    label: "index.html has root mount",
  },
  {
    file: "src/App.jsx",
    mustInclude: "export default function App",
    label: "App component exports",
  },
  {
    file: "src/index.css",
    mustInclude: "@tailwind utilities",
    label: "Tailwind utilities loaded",
  },
];

let failed = false;

for (const check of checks) {
  const fullPath = resolve(check.file);
  const contents = await readFile(fullPath, "utf8");

  if (!contents.includes(check.mustInclude)) {
    console.error(`FAILED: ${check.label}`);
    failed = true;
  } else {
    console.log(`OK: ${check.label}`);
  }
}

if (failed) {
  process.exit(1);
}

