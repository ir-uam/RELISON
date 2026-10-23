const test=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),vm=require('node:vm');
const source=fs.readFileSync(path.join(__dirname,'../../main/resources/public/h3-navigation.js'),'utf8');
function setup(){const checkbox={checked:false},context={window:{},document:{getElementById:()=>checkbox}};vm.createContext(context);vm.runInContext(source,context);return {api:context.window.relisonH3,checkbox};}
function distance(a,b,r=100){const dot=(a.x*b.x+a.y*b.y+a.z*b.z)/(r*r),aa=(a.x*a.x+a.y*a.y+a.z*a.z)/(r*r),bb=(b.x*b.x+b.y*b.y+b.z*b.z)/(r*r);return Math.acosh((1-dot)/Math.sqrt((1-aa)*(1-bb)));}
test('Klein boost centers focus and preserves hyperbolic pair distances',()=>{
 const {api}=setup(),points={a:{x:20,y:10,z:-30},b:{x:50,y:-20,z:15},c:{x:-10,y:5,z:70}};
 const focused=api.recenter(points,100,'b');assert(Math.hypot(focused.b.x,focused.b.y,focused.b.z)<1e-12);
 for(const a of Object.keys(points))for(const b of Object.keys(points))if(a!==b)assert(Math.abs(distance(points[a],points[b])-distance(focused[a],focused[b]))<1e-12);
 assert.equal(JSON.stringify(api.recenter(points,100)),JSON.stringify(points));
});
test('canonical positions survive repeated focus and non-tree edge toggles',()=>{
 const {api,checkbox}=setup(),g={},points={a:{x:0,y:0,z:0},b:{x:20,y:30,z:40},c:{x:-10,y:3,z:5}};
 api.activate(g,points,100,{b:'a',c:'a'});const focused=api.focus(g,'b');api.focus(g,'c');assert.deepEqual(api.focus(g,'b'),focused);
 assert(api.edgeVisible(g,'a','b'));assert(!api.edgeVisible(g,'b','c'));checkbox.checked=true;assert(api.edgeVisible(g,'b','c'));
 assert(api.edgeVisible({},'b','c'));api.clear();assert(!api.active(g));assert.throws(()=>api.focus(g,'a'),/Apply H3/);
});
test('BFS tree follows roots and traversal direction with disconnected nodes',()=>{
 const {api}=setup(),out={a:['b'],b:['c'],c:[],d:[]},incoming={a:[],b:['a'],c:['b'],d:[]};
 const g={type:'directed',nodes:()=>Object.keys(out),hasNode:n=>n in out,outNeighbors:n=>out[n],inNeighbors:n=>incoming[n]};
 assert.equal(JSON.stringify(api.parents(g,'c','IN')),JSON.stringify({b:'c',a:'b'}));
 assert.equal(JSON.stringify(api.parents(g,'a','OUT')),JSON.stringify({b:'a',c:'b'}));
 assert.throws(()=>api.parents(g,'missing','OUT'),/root/);
});
test('tree-only view chooses one occurrence among parallel and reciprocal links',()=>{
 const {api}=setup(),g={forEachEdge:fn=>{fn('z',{},'a','b');fn('reverse',{},'b','a');fn('first',{},'a','b')}};
 api.activate(g,{a:{x:0,y:0,z:0},b:{x:1,y:0,z:0}},100,{b:'a'});
 assert(api.edgeVisible(g,'a','b','first'));assert(!api.edgeVisible(g,'a','b','z'));assert(!api.edgeVisible(g,'b','a','reverse'));
 assert(!api.edgeVisible(g,'a','b','__recommendation__0'));
});
