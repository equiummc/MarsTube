// Validate challenge-only TCL warm calls against the unchanged A2 full request.
// Synthetic, deterministic tests. They do not establish signed YouTube output.
const fs=require('fs'),vm=require('vm'),assert=require('assert');
const script=fs.readFileSync('MediaServiceCore/youtubeapi/src/main/assets/nsigsolver/tcl.solver.js','utf8');
const context={meriyah:{},astring:{}};
vm.createContext(context);
vm.runInContext(script,context,{timeout:2000});
const full=(script,challenges)=>context.tclSolver({
  type:'preprocessed',preprocessed_player:script,challenges
});
const reuse=challenges=>context.tclSolver({type:'reuse',challenges});
const a="return function(x){return x.split('').reverse().join('')}";
const b="return function(x){return x + '!'}";
assert.strictEqual(reuse(['abc']).type,'error');
for(const [script,seed,again] of [
  [a,['one','two'],['three','four']],
  [b,['cat'],['dog','bird']],
  [a,['xyz'],['abc','abc']]
]){
  assert.strictEqual(full(script,seed).type,'result');
  const got=reuse(again);
  const expected=full(script,again);
  assert.deepStrictEqual(JSON.parse(JSON.stringify(got)),
                         JSON.parse(JSON.stringify(expected)));
}
console.log('MT-011AB JS warm challenge-only / full invocation equivalence: PASS');
