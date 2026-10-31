// Compare the pinned upstream TCL solver and MT-010A2 warm-solver on synthetic
// pure n functions. No network, credentials, YouTube IDs or media requests.
// Real signed n challenges still require independent on-device acceptance.
const fs = require('fs');
const vm = require('vm');
const child = require('child_process');
const assert = require('assert');

const core = 'MediaServiceCore';
const jsPath = 'youtubeapi/src/main/assets/nsigsolver/tcl.solver.js';
const upstream = child.execFileSync('git', ['-C', core, 'show', 'HEAD:' + jsPath], { encoding: 'utf8' });
const experiment = fs.readFileSync(core + '/' + jsPath, 'utf8');

function build(script) {
  const context = { meriyah: {}, astring: {} };
  vm.createContext(context);
  vm.runInContext(script, context, { timeout: 2000 });
  assert.strictEqual(typeof context.tclSolver, 'function');
  return context.tclSolver;
}

const oldSolver = build(upstream);
const newSolver = build(experiment);
const scriptA = 'return function(x){return x.split(\'\').reverse().join(\'\')}';
const scriptB = 'return function(x){return x + \'!\'}';
const cases = [
  { type: 'preprocessed', preprocessed_player: scriptA, challenges: ['abc', 'xyz', 'abc'] },
  { type: 'preprocessed', preprocessed_player: scriptA, challenges: ['def', '123'] },
  { type: 'preprocessed', preprocessed_player: scriptB, challenges: ['abc', 'xyz'] },
  { type: 'preprocessed', preprocessed_player: scriptA, challenges: ['abc'] },
];

for (const testcase of cases) {
  const oldResult = oldSolver(testcase);
  const newResult = newSolver(testcase);
  assert.strictEqual(oldResult.type, 'result');
  assert.strictEqual(newResult.type, 'result');
  assert.deepStrictEqual(JSON.parse(JSON.stringify(newResult)),
      JSON.parse(JSON.stringify(oldResult)));
}
console.log('MT-010A2 synthetic compiled-function reuse equivalence: 4 cases green');
