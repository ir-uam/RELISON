const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const source = name => fs.readFileSync(path.join(__dirname, '../../main/resources/public', name), 'utf8');

function harness() {
    const frames = new Map(), listeners = new Map(), elements = new Map();
    let sequence = 0, observer, instance;
    const element = () => ({ hidden: false, style: {}, children: [], attributes: {}, textContent: '',
        clientWidth: 800, clientHeight: 600, offsetWidth: 800, offsetHeight: 600,
        setAttribute(k, v) { this.attributes[k] = v; }, getAttribute(k) { return this.attributes[k]; },
        appendChild(child) { this.children.push(child); }, replaceChildren() { this.children = []; },
        getBoundingClientRect: () => ({ left: 0, top: 0, width: 800, height: 600 }), querySelectorAll: () => [] });
    const document = { body: { classList: { contains: () => false } }, createElement: element,
        getElementById: id => { if (!elements.has(id)) elements.set(id, element()); return elements.get(id); } };
    const exports = { root: (w,h) => { const e=element();e.setAttribute('width',w);e.setAttribute('height',h);return e; },
        element: (tag,attrs,text) => ({tag,attrs,text}), legend: (...args) => exports.legendCall=args,
        credit: (...args) => exports.creditCall=args, exportImage: async build => build() };
    class Globe {
        constructor() {
            instance = this; this.data = {}; this.fits = []; this.distance = 350;
            this.pov = {lat:0,lng:0,altitude:2.5};this.paused = false;this.destroyed = false;
            this.webgl = { ratio:1,getPixelRatio() {return this.ratio;},setPixelRatio(v) {this.ratio=v;},render() {},
                domElement:{toDataURL:()=> 'data:image/png;base64,test'} };
            for (const method of ['polygonsData','polygonAltitude','polygonsTransitionDuration','showAtmosphere','showGraticules',
                'polygonCapCurvatureResolution','customThreeObject','customLayerLabel','onCustomLayerClick','htmlLat','htmlLng','htmlAltitude','htmlElement',
                'htmlTransitionDuration','htmlElementVisibilityModifier','onGlobeClick','onPolygonClick','onZoom','backgroundColor',
                'polygonCapColor','polygonSideColor','polygonStrokeColor','customLayerData','htmlElementsData','width','height']) {
                this[method] = value => { this.data[method]=value;return this; };
            }
        }
        camera() { return {position:{length:()=>this.distance},fov:50}; }
        getGlobeRadius() { return 100; }
        globeMaterial() { return {color:{set() {}}}; }
        pointOfView(value) { if(!value)return this.pov;this.pov=value;this.fits.push(value);return this; }
        pauseAnimation() {this.paused=true;}
        resumeAnimation() {this.paused=false;}
        _destructor() {this.destroyed=true;}
        renderer() {return this.webgl;}
        scene() {return {};}
    }
    const nodes = new Map([['a',{color:'#ff0000',size:3,label:'A'}],['b',{color:'#00ff00',size:7,label:'B'}]]);
    const edges = new Map([['ab',{source:'a',target:'b',color:'#0000ff',size:2,label:'AB'}]]);
    const graph = {type:'directed',nodes:()=>[...nodes.keys()],
        forEachNode: fn => nodes.forEach((attrs,id)=>fn(id,attrs)),
        forEachEdge: fn => edges.forEach((attrs,id)=>fn(id,attrs,attrs.source,attrs.target,{}, {},false)),
        on: (name,fn)=>{if(!listeners.has(name))listeners.set(name,new Set());listeners.get(name).add(fn);},
        off: (name,fn)=>listeners.get(name)?.delete(fn) };
    const context = vm.createContext({window:{relisonGeographicExport:exports},document,console,
        requestAnimationFrame: fn=>{const id=++sequence;frames.set(id,fn);return id;},cancelAnimationFrame:id=>frames.delete(id),
        getComputedStyle:()=>({getPropertyValue:()=> '#121212'}),
        ResizeObserver:class {constructor(fn){observer=fn;}observe(){}disconnect(){observer=null;}} });
    vm.runInContext(source('globe-map.js'),context);
    const clicks=[],errors=[];
    const settings = { coordinates:()=>({points:new Map([['a',[51.5,-.1]],['b',[40.4,-3.7]]]),center:-2}),
        nodeDisplay:(_id,attrs)=>attrs,edgeDisplay:(_id,attrs)=>attrs,
        labels:()=>({nodeShow:true,edgeShow:true,nodeColorMode:'node',edgeColorMode:'single',edgeColor:'#123456'}),
        onNodeClick:(...args)=>clicks.push(['node',...args]),onEdgeClick:id=>clicks.push(['edge',id]),
        onClearSelection:()=>clicks.push(['clear']),onError:message=>errors.push(message),overlays:()=>[],legend:()=>({type:'none'}) };
    const map=context.window.createRelisonGlobe('test',async()=>({Globe,three:{},countries:[]}));
    function flush() {const pending=[...frames.values()];frames.clear();pending.forEach(fn=>fn());}
    return {context,map,graph,nodes,edges,settings,clicks,errors,listeners,elements,exports,flush,
        get globe(){return instance;}, resize:()=>observer?.()};
}

test('great-circle arcs remain finite at the poles, date line, antipodes and coincident endpoints', () => {
    const h=harness(),{arcPosition,vector}=h.context.window.relisonGlobeGeometry;
    for(const [startLat,startLng,endLat,endLng] of [[0,179,0,-179],[90,0,-90,0],[0,0,0,180],[51,-3,51,-3],[0,0,0,0]]) {
        const edge={startLat,startLng,endLat,endLng,parallel:2};
        for(let i=0;i<=100;i++) {
            const p=arcPosition(edge,i/100);
            assert.ok(p.every(Number.isFinite));assert.ok(Math.hypot(...p)>=1-1e-10);
        }
        for(const [t,expected] of [[0,vector(startLat,startLng)],[1,vector(endLat,endLng)]]) {
            arcPosition(edge,t).forEach((value,i)=>assert.ok(Math.abs(value-expected[i])<1e-8));
        }
    }
    const midpoint=arcPosition({startLat:0,startLng:179,endLat:0,endLng:-179},.5);
    assert.ok(midpoint[2]<-1, 'date-line links use the short route above the back hemisphere');
});

test('shared display values, directed links, parallel curves and dashed overlays reach the globe',async()=>{
    const h=harness();h.edges.set('ab2',{source:'a',target:'b',size:4});
    h.settings.overlays=()=>[{source:'a',target:'b',dashed:true,color:'#abcdef',size:2}];
    await h.map.render(h.graph,h.settings);h.flush();
    const data=h.globe.data.customLayerData,nodes=data.filter(x=>x.kind==='node'),edges=data.filter(x=>x.kind==='edge');
    assert.equal(nodes[0].display.size,3);assert.equal(nodes[1].display.color,'#00ff00');
    assert.equal(edges.length,3);assert.equal(edges[0].directed,true);assert.equal(edges[0].dashed,undefined);
    assert.equal(edges[1].parallel,1);assert.equal(edges[2].parallel,2);assert.equal(edges[2].dashed,true);
    const labels=h.globe.data.htmlElementsData;
    assert.equal(labels.length,3);
    assert.equal(h.globe.data.htmlElement(labels.find(x=>x.kind==='node')).style.color,'#ff0000');
    assert.equal(h.globe.data.htmlElement(labels.find(x=>x.kind==='edge')).style.color,'#123456');
});

test('hidden endpoints hide all adjacent base and recommendation links',async()=>{
    const h=harness();h.nodes.get('b').hidden=true;
    h.settings.overlays=()=>[{source:'a',target:'b',dashed:true},{source:'a',target:'missing'}];
    await h.map.render(h.graph,h.settings);
    assert.equal(h.globe.data.customLayerData.length,1);
    assert.equal(h.globe.data.customLayerData[0].id,'a');
});

test('selection uses shared IDs and ignores transient recommendation links',async()=>{
    const h=harness();await h.map.render(h.graph,h.settings);
    const click=h.globe.data.onCustomLayerClick,event={ctrlKey:true};
    click({kind:'node',id:'a'},event);click({kind:'edge',id:'ab'},event);
    click({kind:'edge',id:'overlay-0',overlay:true},event);h.globe.data.onGlobeClick();
    assert.deepEqual(h.clicks,[['node','a',event],['edge','ab'],['clear']]);
});

test('appearance refresh preserves camera and rotation does not recreate geometry',async()=>{
    const h=harness();await h.map.render(h.graph,h.settings);h.flush();
    const fits=h.globe.fits.length,data=h.globe.data.customLayerData;
    h.globe.data.onZoom();h.flush();assert.equal(h.globe.data.customLayerData,data);
    h.nodes.get('a').size=20;h.map.refresh();h.flush();
    assert.equal(h.globe.fits.length,fits);assert.equal(h.globe.data.customLayerData.find(x=>x.id==='a').display.size,20);
    h.globe.distance=400;h.globe.data.onZoom();h.flush();assert.notEqual(h.globe.data.customLayerData,data);
    await h.map.render(h.graph,h.settings);h.flush();assert.equal(h.globe.fits.length,fits);
    for(const set of h.listeners.values())assert.equal(set.size,1,'render does not duplicate listeners');
});

test('hidden tabs pause the globe and destroy releases listeners and pending refreshes',async()=>{
    const h=harness();await h.map.render(h.graph,h.settings);h.flush();
    h.elements.get('test').offsetWidth=0;h.resize();assert.equal(h.globe.paused,true);
    h.elements.get('test').offsetWidth=800;h.resize();assert.equal(h.globe.paused,false);
    h.map.refresh();h.map.destroy();h.flush();
    assert.equal(h.globe.destroyed,true);assert.equal(h.elements.get('test').hidden,true);
    for(const set of h.listeners.values())assert.equal(set.size,0);
    assert.deepEqual(h.errors,[]);
});

test('stale renders do not initialize the globe',async()=>{
    const h=harness();await h.map.render(h.graph,{...h.settings,isCurrent:()=>false});assert.equal(h.globe,undefined);
});

test('exports embed a double-resolution globe image and preserve the live renderer pixel ratio',async()=>{
    const h=harness();await h.map.render(h.graph,h.settings);
    const svg=await h.map.exportSvg('globe.svg');
    assert.equal(svg.getAttribute('width'),1600);assert.equal(svg.getAttribute('height'),1200);
    assert.equal(svg.children[0].tag,'image');assert.match(svg.children[0].attrs.href,/^data:image\/png/);
    assert.ok(h.exports.legendCall);assert.ok(h.exports.creditCall);assert.equal(h.globe.renderer().getPixelRatio(),1);
    h.exports.legendCall=null;await h.map.exportPng('globe.png',{includeLegend:false});assert.equal(h.exports.legendCall,null);
});

test('Globe permits polar coordinates while Mercator retains its latitude restriction',()=>{
    const h=harness();vm.runInContext(source('geographic-map.js'),h.context);
    const read=h.context.window.relisonGeographic.readCoordinates;
    assert.equal(read(['a'],()=>90,()=>180,'globe').points.get('a')[0],90);
    assert.throws(()=>read(['a'],()=>90,()=>180,'mercator'),/latitude/);
    assert.throws(()=>read(['a'],()=>NaN,()=>0,'globe'),/latitude/);
    assert.throws(()=>read(['a'],()=>91,()=>0,'globe'),/latitude/);
    assert.throws(()=>read(['a'],()=>0,()=>181,'globe'),/longitude/);
});

test('the shared geographic adapter dispatches globe camera/export/refresh and releases it on switching',async()=>{
    const calls=[];
    const adapter=name=>Object.fromEntries(['readCoordinates','ensureLoaded','render','refresh','destroy','fitView','exportPng','exportSvg','zoomIn','zoomOut']
        .map(method=>[method,()=>{calls.push(name+'.'+method);return true;}]));
    const context=vm.createContext({window:{createRelisonMercator:()=>adapter('mercator'),createRelisonGlobe:()=>adapter('globe'),
        relisonMapSettings:{useOnlineTiles:()=>true,subscribe:()=>()=>{}}}});
    vm.runInContext(source('equal-earth-map.js'),context);
    const map=context.window.relisonGeographic;
    await map.render({}, {projection:'globe',isCurrent:()=>true});
    map.zoomIn();map.zoomOut();map.fitView();map.refresh();map.exportSvg();map.exportPng();
    assert.ok(calls.includes('globe.render'));
    for(const method of ['zoomIn','zoomOut','fitView','refresh','exportSvg','exportPng'])assert.ok(calls.includes('globe.'+method));
    await map.render({}, {projection:'mercator',isCurrent:()=>true});
    assert.ok(calls.includes('globe.destroy'));assert.ok(calls.includes('mercator.render'));
    calls.length=0;await map.render({}, {projection:'globe',isCurrent:()=>false});
    assert.deepEqual(calls,['globe.ensureLoaded']);
});

function appHarness() {
    const inputs=new Map(),positions=new Map([['a',{x:10,y:20,z:30,latitude:51,longitude:-3}]]),requests=[],renders=[];
    const $=id=>{
        if(!inputs.has(id)){
            const classes=new Set();
            inputs.set(id,{value:'',disabled:false,checked:false,hidden:false,
                classList:{contains:name=>classes.has(name),add:name=>classes.add(name),remove:name=>classes.delete(name)}});
        }
        return inputs.get(id);
    };
    $('layout-geographic-latitude').value='latitude';$('layout-geographic-longitude').value='longitude';
    $('layout-geographic-projection').value='globe';$('network-renderer').value='cosmograph';
    $('pane-network').classList.add('cosmograph-active');
    const graph={nodes:()=>[...positions.keys()],hasNode:id=>positions.has(id),getNodeAttribute:(id,key)=>positions.get(id)[key],
        mergeNodeAttributes:(id,attrs)=>Object.assign(positions.get(id),attrs)};
    const map={ensureLoaded:async()=>{},render:async(g,settings)=>renders.push({g,settings}),destroy(){},
        readCoordinates:(ids,lat,lon)=>({latitudes:Object.fromEntries(ids.map(id=>[id,lat(id)])),
            longitudes:Object.fromEntries(ids.map(id=>[id,lon(id)])),points:new Map(ids.map(id=>[id,[lat(id),lon(id)]])),center:0})};
    const state={graph,graphId:'graph',renderer:{refresh(){}}},statuses=[];
    const context=vm.createContext({$,state,window:{relisonGeographic:map},AbortController,
        stopLayout(){},updateLayoutButton(){},setEditMode(){},setAreaNodeSelection(){},toggleHidden:(id,hidden)=>{$(id).hidden=hidden;},
        setStatus:message=>statuses.push(message),isAbort:()=>false,jsonBody:body=>body,
        api:async(url,body)=>{requests.push({url,body});return {positions:{a:{x:500,y:600}}};},
        layoutCoordinateFeature:(key,id)=>positions.get(id)[key],handleNetworkPointClick(){},selectEdge(){},clearSelection(){} });
    const app=source('app.js'),section=app.slice(app.indexOf('function usingCosmograph()'),app.indexOf('// Recommendations are transient results'));
    const handlers=vm.runInContext(section+'\n({applyGeographicLayout,leaveGeographicView})',context);
    return {$,context,state,positions,requests,renders,statuses,...handlers};
}

test('app globe activation skips the planar layout API and preserves XY/Z coordinates',async()=>{
    const h=appHarness();await h.applyGeographicLayout();
    assert.equal(h.requests.length,0);assert.equal(h.renders[0].settings.projection,'globe');
    assert.equal(h.positions.get('a').x,10);assert.equal(h.positions.get('a').y,20);assert.equal(h.positions.get('a').z,30);
    assert.equal(h.$('network-renderer').value,'geographic');assert.equal(h.$('network-globe-help').hidden,false);
    assert.equal(h.$('drag-nodes').disabled,true);assert.equal(h.$('edit-mode').disabled,true);
    h.leaveGeographicView('sigma');assert.equal(h.positions.get('a').x,10);assert.equal(h.$('network-globe-help').hidden,true);
});

test('switching from a planar map to the globe restores the original graph coordinates',async()=>{
    const h=appHarness();h.$('layout-geographic-projection').value='equal-earth';
    await h.applyGeographicLayout();assert.equal(h.positions.get('a').x,500);
    h.$('layout-geographic-projection').value='globe';await h.applyGeographicLayout();
    assert.equal(h.requests.length,1);assert.equal(h.positions.get('a').x,10);assert.equal(h.positions.get('a').z,30);
    h.leaveGeographicView('cosmograph');assert.equal(h.positions.get('a').x,10);
});

test('Diffusion follows the active globe and also accepts an explicit globe choice',()=>{
    const inputs=new Map(),initialized=[],state={diffusion:{}};
    const $=id=>{
        if(!inputs.has(id))inputs.set(id,{hidden:false,value:'',classList:{toggle(){}}});
        return inputs.get(id);
    };
    const context=vm.createContext({state,$,geographicActiveProjection:'globe',usingGeographic:()=>true,
        initDiffGeographic:projection=>initialized.push(projection)});
    const app=source('app.js');
    const methods=vm.runInContext(app.slice(app.indexOf('function mainDiffRendererType()'),app.indexOf('function geographicDiffusionEdges()'))
        +'\n({setDiffRenderer,usingDiffGeographic})',context);
    methods.setDiffRenderer('network');assert.equal(state.diffusion.rendererType,'globe');
    assert.equal(state.diffusion.rendererPreference,'network');assert.equal(methods.usingDiffGeographic(),true);
    assert.equal($('diff-geographic-container').hidden,false);
    methods.setDiffRenderer('globe');assert.deepEqual(initialized,['globe','globe']);
});

