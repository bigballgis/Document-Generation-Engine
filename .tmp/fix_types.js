const fs = require('fs');
const path = require('path');

// Map demo variable types to backend-supported VariableType enum values
const TYPE_MAP = {
  STRING: 'TEXT',
  TEXT: 'TEXT',
  MONEY: 'AMOUNT',
  INTEGER: 'NUMBER',
  NUMBER: 'NUMBER',
  DATE: 'DATE',
  BOOLEAN: 'BOOLEAN',
  LIST: 'LIST',
  OBJECT: 'OBJECT',
  ENUM: 'ENUM',
  COMPUTED: 'COMPUTED',
};

const demos = [
  'demo-credit-limit',
  'demo-mortgage',
  'demo-trade-lc',
  'demo-collection',
  'demo-annual-review',
  'demo-wealth',
];

let changed = 0;
for (const d of demos) {
  const vfile = path.join('deploy', d, 'config', `${d.replace('demo-','')}-variables.json`);
  if (!fs.existsSync(vfile)) continue;
  const j = JSON.parse(fs.readFileSync(vfile, 'utf8'));
  let demoChanged = false;
  for (const v of j.variables) {
    const mapped = TYPE_MAP[v.type] || v.type;
    if (mapped !== v.type) { v.type = mapped; demoChanged = true; changed++; }
  }
  if (demoChanged) {
    fs.writeFileSync(vfile, JSON.stringify(j, null, 2) + '\n');
    console.log('rewrote', vfile);
  }
}
console.log('total type changes:', changed);
