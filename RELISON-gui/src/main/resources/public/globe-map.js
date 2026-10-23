/* Surface-anchored geographic networks using Globe.gl and Three.js. */
(() => {
    const EVENTS = ["nodeAdded", "nodeDropped", "edgeAdded", "edgeDropped", "cleared", "nodeAttributesUpdated",
        "edgeAttributesUpdated", "eachNodeAttributesUpdated", "eachEdgeAttributesUpdated"];
    let loading;
    function load() {
        if (!loading) loading = Promise.all([
            import("./vendor/esm/globe.js"),
            import("./vendor/esm/three.js"),
            import("./vendor/esm/topojson.js"),
            fetch("vendor/countries-110m.json").then(response => {
                if (!response.ok) throw new Error("Could not load the globe's country boundaries.");
                return response.json();
            }),
        ]).then(([module, three, topojson, world]) => ({ Globe: module.default, three,
            countries: topojson.feature(world, world.objects.countries).features }))
            .catch(error => { loading = null; throw new Error("Could not load Globe.gl: " + error.message); });
        return loading;
    }
    const dot = (a,b) => a.reduce((sum,value,index) => sum + value*b[index],0);
    const cross = (a,b) => [a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]];
    const unit = a => { const length = Math.hypot(...a); return a.map(value => value/(length || 1)); };
    function vector(lat,lng) {
        const p=lat*Math.PI/180, t=lng*Math.PI/180;
        return [Math.cos(p)*Math.sin(t),Math.sin(p),Math.cos(p)*Math.cos(t)];
    }
    // A deterministic great-circle arc, including coincident and antipodal endpoints.
    function arcPosition(edge,t) {
        const a=vector(edge.startLat,edge.startLng), b=vector(edge.endLat,edge.endLng);
        const angle=Math.atan2(Math.hypot(...cross(a,b)),Math.max(-1,Math.min(1,dot(a,b))));
        let axis=unit(cross(a,b));
        if (Math.hypot(...cross(a,b))<1e-8) axis=unit(cross(a,Math.abs(a[1])<.9?[0,1,0]:[1,0,0]));
        if (angle<1e-8) {
            const tangent=unit(cross(axis,a)), radius=.035+(edge.parallel || 0)*.015;
            return a.map((value,i) => value*(1+.03*Math.sin(Math.PI*t))
                + tangent[i]*radius*Math.sin(2*Math.PI*t) + axis[i]*radius*(1-Math.cos(2*Math.PI*t)));
        }
        const tangent=cross(axis,a), phase=angle*t;
        const height=.025 + angle*.12 + (edge.parallel || 0)*.035;
        return a.map((value,i) => (value*Math.cos(phase)+tangent[i]*Math.sin(phase))*(1+height*Math.sin(Math.PI*t)));
    }
    function geographic(point) {
        const length=Math.hypot(...point);
        return { lat: Math.asin(point[1]/length)*180/Math.PI, lng: Math.atan2(point[0],point[2])*180/Math.PI, altitude: length-1 };
    }
    function safeText(value) { const span=document.createElement("span"); span.textContent=String(value ?? ""); return span; }
    function create(containerId="geographic-container", loadLibraries=load) {
        let globe=null, graph=null, options=null, libraries=null, observer=null, frame=null, rotationFrame=null, rotationLastTime=0, rotationSettings={ enabled:false, axis:[0,1,0], speed:25, reverse:false }, shown=false;
        let visibleNodes=[], visibleEdges=[], labels=[], pixelScale=0;
        const container=() => document.getElementById(containerId);
        function labelColor(kind,display) {
            const settings=options.labels(), mode=settings[kind+"ColorMode"] || "default";
            return mode==="single" ? settings[kind+"Color"] : mode===kind ? display.color
                : document.body.classList.contains("light") ? "#000000" : "#ffffff";
        }
        function worldPerPixel() {
            const camera=globe.camera(), radius=globe.getGlobeRadius();
            const distance=Math.max(radius*.05,camera.position.length()-radius);
            return 2*distance*Math.tan((camera.fov || 50)*Math.PI/360)/Math.max(1,container().clientHeight);
        }
        function nodeObject(node) {
            const T=libraries.three, object=new T.Group(), radius=Math.max(1,Number(node.display.size) || 4)*worldPerPixel();
            const circle=new T.Mesh(new T.CircleGeometry(radius,24),new T.MeshBasicMaterial({ color:node.display.color || "#4f9dff", side:T.DoubleSide }));
            object.add(circle);
            const selected=options.selectedNode?.()===node.id, bordered=document.getElementById("node-border-on")?.checked;
            if (selected || bordered) {
                const color=selected ? "#ffcc66" : document.getElementById("node-border-color")?.value || "#1b1c1f";
                const width=selected ? 2 : Math.max(.5,Number(document.getElementById("node-border-width")?.value) || 1.5);
                object.add(new T.Mesh(new T.RingGeometry(radius,radius+width*worldPerPixel(),24),
                    new T.MeshBasicMaterial({ color,side:T.DoubleSide })));
            }
            const position=globe.getCoords(node.lat,node.lng,.002), normal=new T.Vector3(position.x,position.y,position.z).normalize();
            object.position.set(position.x,position.y,position.z);
            object.quaternion.setFromUnitVectors(new T.Vector3(0,0,1),normal);
            return object;
        }
        function edgeObject(edge) {
            const T=libraries.three, object=new T.Group(), earth=globe.getGlobeRadius();
            const thickness=Math.max(.25,Number(edge.display.size) || 1)*worldPerPixel();
            class Arc extends T.Curve {
                constructor(start=0,end=1) { super(); this.start=start; this.end=end; }
                getPoint(t,target=new T.Vector3()) { return target.fromArray(arcPosition(edge,this.start+t*(this.end-this.start))).multiplyScalar(earth); }
            }
            const material=new T.MeshBasicMaterial({ color:edge.display.color || "#64748b" });
            if (edge.dashed) {
                for(let i=0;i<12;i++) object.add(new T.Mesh(new T.TubeGeometry(new Arc(i/12,(i+.6)/12),6,thickness/2,5,false),material));
            } else object.add(new T.Mesh(new T.TubeGeometry(new Arc(),64,thickness/2,5,false),material));
            if (edge.directed) {
                const curve=new Arc(), t=.92, length=Math.max(thickness*4,worldPerPixel()*7);
                const arrow=new T.Mesh(new T.ConeGeometry(Math.max(thickness*1.6,worldPerPixel()*2.5),length,8),material);
                arrow.position.copy(curve.getPoint(t));
                arrow.quaternion.setFromUnitVectors(new T.Vector3(0,1,0),curve.getTangent(t).normalize());
                object.add(arrow);
            }
            return object;
        }
        function labelElement(label) {
            const element=document.createElement("span"), settings=options.labels(), kind=label.kind;
            element.className="relison-globe-label";
            element.textContent=label.text;
            const base=Number(settings[kind+"Size"]) || (kind==="node"?12:9);
            const size=settings[kind+"Prop"] ? Math.max(6,Number(label.display.size || 1)*base/(kind==="node"?8:2)) : base;
            element.style.fontFamily=settings[kind+"Font"] || "sans-serif";
            element.style.fontSize=size+"px"; element.style.color=labelColor(kind,label.display);
            return element;
        }
        function refreshNow() {
            if (!globe || !graph || !options) return;
            pixelScale=worldPerPixel();
            const coordinates=options.coordinates(), visible=new Map();
            graph.forEachNode((id,attrs) => {
                const display=options.nodeDisplay(id,attrs), p=coordinates.points.get(id);
                if (!display.hidden && p) visible.set(id,{ id,kind:"node",lat:p[0],lng:p[1],display });
            });
            const edges=[], pairs=new Map();
            function addEdge(edge) {
                const a=visible.get(edge.source), b=visible.get(edge.target);
                if (!a || !b || edge.display.hidden) return;
                const key=JSON.stringify([String(edge.source),String(edge.target)].sort()), parallel=pairs.get(key) || 0;
                pairs.set(key,parallel+1);
                edges.push({ ...edge,kind:"edge",startLat:a.lat,startLng:a.lng,endLat:b.lat,endLng:b.lng,parallel });
            }
            graph.forEachEdge((id,attrs,source,target,sa,ta,undirected) => addEdge({ id,source,target,directed:!undirected,
                display:options.edgeDisplay(id,attrs) }));
            const baseWidth=edges.length ? edges.reduce((sum,edge)=>sum+(Number(edge.display.size)||1),0)/edges.length : 1;
            for(const [index,edge] of (options.overlays?.() || []).entries()) addEdge({ ...edge,id:"overlay-"+index,overlay:true,
                directed:Boolean(edge.directed ?? graph.type==="directed"),display:{ color:edge.color,size:edge.size || baseWidth,label:edge.label } });
            visibleNodes=[...visible.values()]; visibleEdges=edges;
            labels=[];
            if (options.labels().nodeShow) for(const node of visibleNodes) labels.push({ ...node,text:node.display.label || String(node.id),altitude:.007 });
            if (options.labels().edgeShow) for(const edge of edges) if(edge.display.label)
                labels.push({ kind:"edge",text:edge.display.label,display:edge.display,...geographic(arcPosition(edge,.5)) });
            const light=document.body.classList.contains("light");
            globe.backgroundColor(getComputedStyle(document.body).getPropertyValue("--canvas-bg").trim() || "#18191c");
            globe.globeMaterial().color.set(light?"#c9e2f3":"#102a40");
            globe.polygonCapColor(() => light?"#e3e8dc":"#264e46").polygonSideColor(() => light?"#e3e8dc":"#264e46")
                .polygonStrokeColor(() => light?"#879b9d":"#52736d");
            globe.customLayerData([...edges,...visibleNodes]).htmlElementsData(labels);
        }
        function refresh() {
            if (!globe || frame!==null) return;
            frame=requestAnimationFrame(() => { frame=null; try { refreshNow(); } catch(error) { options?.onError(error.message); } });
        }
        function fitView() {
            if (!globe || !options) return;
            const points=[...options.coordinates().points.values()];
            const center=points.reduce((sum,p) => { const v=vector(p[0],p[1]);return sum.map((x,i)=>x+v[i]); },[0,0,0]);
            const target=Math.hypot(...center)>1e-8?geographic(center):{lat:0,lng:options.coordinates().center};
            globe.pointOfView({ lat:target.lat,lng:target.lng,altitude:2.5 },0);
            refresh();
        }
        function setContinuousRotation(settings, legacyOptions) {
            const config=typeof settings==="object" && settings!==null ? settings : { ...(legacyOptions||{}), enabled:Boolean(settings) };
            rotationSettings={
                enabled:Boolean(config.enabled),
                axis:Array.isArray(config.axis) && config.axis.length===3 ? config.axis.map(Number) : rotationSettings.axis,
                speed:Math.max(1,Number(config.speed)||25), reverse:Boolean(config.reverse),
            };
            const length=Math.hypot(...rotationSettings.axis);
            if(!Number.isFinite(length)||length<1e-8) { rotationSettings.enabled=false; return false; }
            rotationSettings.axis=rotationSettings.axis.map(value=>value/length);
            if(rotationFrame!==null) cancelAnimationFrame(rotationFrame);
            rotationFrame=null;rotationLastTime=0;
            if(rotationSettings.enabled && globe) rotationFrame=requestAnimationFrame(rotateFrame);
            return true;
        }
        function rotateFrame(timestamp) {
            rotationFrame=null;
            if(!rotationSettings.enabled||!globe) return;
            const element=container();
            if(!element.offsetWidth||!element.offsetHeight) { rotationSettings.enabled=false; return; }
            if(rotationLastTime) {
                const elapsed=Math.min(100,Math.max(0,timestamp-rotationLastTime));
                const camera=globe.camera(), target=globe.controls().target, axis=rotationSettings.axis;
                const offset=new libraries.three.Vector3().subVectors(camera.position,target);
                const angle=(rotationSettings.reverse?-1:1)*elapsed*rotationSettings.speed*Math.PI/180000;
                offset.applyAxisAngle(new libraries.three.Vector3(...axis),angle);
                camera.position.copy(target).add(offset);
                globe.controls().update();
            }
            rotationLastTime=timestamp;
            rotationFrame=requestAnimationFrame(rotateFrame);
        }
        function getRotationSnapshot() {
            if(!globe) return null;
            const camera=globe.camera(), target=globe.controls().target;
            return { position:camera.position.toArray(), target:target.toArray() };
        }
        function setRotationFrame(snapshot, axis, degrees) {
            if(!globe || !snapshot || !libraries?.three || !Array.isArray(axis) || axis.length!==3) return false;
            const T=libraries.three, target=new T.Vector3(...snapshot.target);
            const offset=new T.Vector3(...snapshot.position).sub(target);
            offset.applyAxisAngle(new T.Vector3(...axis).normalize(),Number(degrees)*Math.PI/180);
            globe.controls().target.copy(target);
            globe.camera().position.copy(target).add(offset);
            globe.controls().update();
            return true;
        }
        async function captureRotationFrame(_legend=null,{width=null,includeLegend=true}={}) {
            await new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve)));
            if(!globe) return null;
            const element=container(),size=window.relisonAnimationExport.frameSize(element.clientWidth,element.clientHeight,width||globe.renderer().domElement.width);
            const svg=await buildExportSvg(includeLegend,size.width/element.clientWidth);
            if(!svg) return null;
            return window.relisonAnimationExport.svgFrame(new XMLSerializer().serializeToString(svg),element.clientWidth,element.clientHeight,size.width);
        }
        function resize() {
            if (!globe) return;
            const element=container();
            if(element.clientWidth && element.clientHeight) globe.width(element.clientWidth).height(element.clientHeight);
            const visible=element.offsetWidth>0 && element.offsetHeight>0;
            if (visible!==shown) { shown=visible; if(visible) globe.resumeAnimation();else globe.pauseAnimation(); }
            if (visible) refresh();
        }
        function detach() {
            if(graph) for(const event of EVENTS) graph.off(event,refresh);
            if(frame!==null) cancelAnimationFrame(frame);frame=null;
        }
        async function render(input,settings) {
            const loaded=await loadLibraries();
            if(settings.isCurrent && !settings.isCurrent()) return;
            settings.coordinates();
            const changed=graph!==input;
            detach(); graph=input; options=settings; libraries=loaded;
            container().hidden=false;
            if(!globe) {
                const {Globe}=libraries;
                globe=new Globe(container(),{ animateIn:false,waitForGlobeReady:false,
                    rendererConfig:{ antialias:true,preserveDrawingBuffer:true } });
                // Follow the sphere curvature closely and lift country caps a
                // little above its opaque ocean mesh to avoid surface overlap.
                globe.polygonsData(libraries.countries).polygonAltitude(.001).polygonCapCurvatureResolution(2).polygonsTransitionDuration(0)
                    .showAtmosphere(true).showGraticules(true)
                    .customThreeObject(data => data.kind==="node"?nodeObject(data):edgeObject(data))
                    .customLayerLabel(data => safeText(data.display.label || (data.kind==="node"?data.id:data.source+(data.directed?" → ":" — ")+data.target)))
                    .onCustomLayerClick((data,event) => {
                        if(data.kind==="node") options.onNodeClick(data.id,event);
                        else if(!data.overlay) options.onEdgeClick(data.id);
                    })
                    .htmlLat("lat").htmlLng("lng").htmlAltitude("altitude").htmlElement(labelElement).htmlTransitionDuration(0)
                    .htmlElementVisibilityModifier((element,visible) => { element.hidden=!visible; })
                    .onGlobeClick(() => options.onClearSelection()).onPolygonClick(() => options.onClearSelection())
                    // Rotation alone does not change geometry. Rebuild pixel-sized
                    // markers only when the camera's distance changes materially.
                    .onZoom(() => { if(Math.abs(worldPerPixel()/pixelScale-1)>.01) refresh(); });
                const credit=document.createElement("a");
                credit.href="https://www.naturalearthdata.com/";credit.textContent="Natural Earth · Globe.gl";
                credit.className="equal-earth-credit";credit.target="_blank";credit.rel="noopener";
                container().appendChild(credit);
                observer=new ResizeObserver(resize);observer.observe(container());
            }
            resize();refreshNow(); if(changed) fitView();
            for(const event of EVENTS) graph.on(event,refresh);
        }
        function destroy() {
            detach();observer?.disconnect();observer=null;
            setContinuousRotation(false);
            if(globe) {
                globe.pauseAnimation();
                if(typeof globe._destructor==="function") globe._destructor();
                else { globe.controls().dispose();globe.renderer().dispose(); }
                container().replaceChildren();container().hidden=true;
            }
            globe=null;graph=null;options=null;shown=false;visibleNodes=[];visibleEdges=[];labels=[];pixelScale=0;
        }
        async function buildExportSvg(includeLegend=true,exportPixelRatio=2) {
            if(!globe) return null;
            const E=window.relisonGeographicExport, renderer=globe.renderer(), ratio=renderer.getPixelRatio();
            const width=container().clientWidth,height=container().clientHeight;
            if(!width || !height) throw new Error("Show the globe before exporting.");
            let image;
            try {
                renderer.setPixelRatio(exportPixelRatio);renderer.render(globe.scene(),globe.camera());
                image=renderer.domElement.toDataURL("image/png");
            } finally { renderer.setPixelRatio(ratio);renderer.render(globe.scene(),globe.camera()); }
            const svg=E.root(width,height);
            // WebGL geometry is an embedded high-resolution image; labels and legend remain vector text.
            svg.setAttribute("width",width*exportPixelRatio);svg.setAttribute("height",height*exportPixelRatio);
            svg.appendChild(E.element("image",{href:image,width,height}));
            const bounds=container().getBoundingClientRect();
            for(const element of container().querySelectorAll(".relison-globe-label")) {
                if(element.hidden) continue;
                const rect=element.getBoundingClientRect(),style=getComputedStyle(element);
                if(!rect.width || !rect.height) continue;
                svg.appendChild(E.element("text",{ x:rect.left-bounds.left+4,y:rect.top-bounds.top+3,
                    "dominant-baseline":"hanging","font-size":parseFloat(style.fontSize)||12,"font-family":style.fontFamily,fill:style.color },element.textContent));
            }
            if(includeLegend) E.legend(svg,width,height,options.legend?.());
            E.credit(svg,width,height,"Natural Earth · Globe.gl");
            return svg;
        }
        function zoom(factor) {
            if(!globe) return;
            const view=globe.pointOfView();globe.pointOfView({...view,altitude:Math.max(.08,Math.min(8,view.altitude*factor))},150);
        }
        return { ensureLoaded:loadLibraries,render,refresh,destroy,fitView,zoomIn:()=>zoom(.8),zoomOut:()=>zoom(1.25),setContinuousRotation,
            getRotationSnapshot,setRotationFrame,captureRotationFrame,
            exportPng:(name="network-globe.png",settings={}) => window.relisonGeographicExport.exportImage(()=>buildExportSvg(settings.includeLegend!==false),"png",name),
            exportSvg:(name="network-globe.svg",settings={}) => window.relisonGeographicExport.exportImage(()=>buildExportSvg(settings.includeLegend!==false),"svg",name) };
    }
    window.createRelisonGlobe=create;
    window.relisonGlobeGeometry={ arcPosition,vector };
})();
