import{a as Ar,c as Sr}from"./chunks/chunk-7NCEHU6W.js";var ur=Ar((Zn,sr)=>{sr.exports=function(e){return e!==null&&typeof e=="object"&&typeof e.addUndirectedEdgeWithKey=="function"&&typeof e.dropNode=="function"&&typeof e.multi=="boolean"}});function Cr(r,e){if(typeof r!="object"||!r)return r;var n=r[Symbol.toPrimitive];if(n!==void 0){var t=n.call(r,e||"default");if(typeof t!="object")return t;throw new TypeError("@@toPrimitive must return a primitive value.")}return(e==="string"?String:Number)(r)}function ce(r){var e=Cr(r,"string");return typeof e=="symbol"?e:e+""}function w(r,e){if(!(r instanceof e))throw new TypeError("Cannot call a class as a function")}function Le(r,e){for(var n=0;n<e.length;n++){var t=e[n];t.enumerable=t.enumerable||!1,t.configurable=!0,"value"in t&&(t.writable=!0),Object.defineProperty(r,ce(t.key),t)}}function F(r,e,n){return e&&Le(r.prototype,e),n&&Le(r,n),Object.defineProperty(r,"prototype",{writable:!1}),r}function D(r){return D=Object.setPrototypeOf?Object.getPrototypeOf.bind():function(e){return e.__proto__||Object.getPrototypeOf(e)},D(r)}function De(){try{var r=!Boolean.prototype.valueOf.call(Reflect.construct(Boolean,[],function(){}))}catch{}return(De=function(){return!!r})()}function wr(r){if(r===void 0)throw new ReferenceError("this hasn't been initialised - super() hasn't been called");return r}function Fr(r,e){if(e&&(typeof e=="object"||typeof e=="function"))return e;if(e!==void 0)throw new TypeError("Derived constructors may only return object or undefined");return wr(r)}function N(r,e,n){return e=D(e),Fr(r,De()?Reflect.construct(e,n||[],D(r).constructor):e.apply(r,n))}function ue(r,e){return ue=Object.setPrototypeOf?Object.setPrototypeOf.bind():function(n,t){return n.__proto__=t,n},ue(r,e)}function x(r,e){if(typeof e!="function"&&e!==null)throw new TypeError("Super expression must either be null or a function");r.prototype=Object.create(e&&e.prototype,{constructor:{value:r,writable:!0,configurable:!0}}),Object.defineProperty(r,"prototype",{writable:!1}),e&&ue(r,e)}var K={black:"#000000",silver:"#C0C0C0",gray:"#808080",grey:"#808080",white:"#FFFFFF",maroon:"#800000",red:"#FF0000",purple:"#800080",fuchsia:"#FF00FF",green:"#008000",lime:"#00FF00",olive:"#808000",yellow:"#FFFF00",navy:"#000080",blue:"#0000FF",teal:"#008080",aqua:"#00FFFF",darkblue:"#00008B",mediumblue:"#0000CD",darkgreen:"#006400",darkcyan:"#008B8B",deepskyblue:"#00BFFF",darkturquoise:"#00CED1",mediumspringgreen:"#00FA9A",springgreen:"#00FF7F",cyan:"#00FFFF",midnightblue:"#191970",dodgerblue:"#1E90FF",lightseagreen:"#20B2AA",forestgreen:"#228B22",seagreen:"#2E8B57",darkslategray:"#2F4F4F",darkslategrey:"#2F4F4F",limegreen:"#32CD32",mediumseagreen:"#3CB371",turquoise:"#40E0D0",royalblue:"#4169E1",steelblue:"#4682B4",darkslateblue:"#483D8B",mediumturquoise:"#48D1CC",indigo:"#4B0082",darkolivegreen:"#556B2F",cadetblue:"#5F9EA0",cornflowerblue:"#6495ED",rebeccapurple:"#663399",mediumaquamarine:"#66CDAA",dimgray:"#696969",dimgrey:"#696969",slateblue:"#6A5ACD",olivedrab:"#6B8E23",slategray:"#708090",slategrey:"#708090",lightslategray:"#778899",lightslategrey:"#778899",mediumslateblue:"#7B68EE",lawngreen:"#7CFC00",chartreuse:"#7FFF00",aquamarine:"#7FFFD4",skyblue:"#87CEEB",lightskyblue:"#87CEFA",blueviolet:"#8A2BE2",darkred:"#8B0000",darkmagenta:"#8B008B",saddlebrown:"#8B4513",darkseagreen:"#8FBC8F",lightgreen:"#90EE90",mediumpurple:"#9370DB",darkviolet:"#9400D3",palegreen:"#98FB98",darkorchid:"#9932CC",yellowgreen:"#9ACD32",sienna:"#A0522D",brown:"#A52A2A",darkgray:"#A9A9A9",darkgrey:"#A9A9A9",lightblue:"#ADD8E6",greenyellow:"#ADFF2F",paleturquoise:"#AFEEEE",lightsteelblue:"#B0C4DE",powderblue:"#B0E0E6",firebrick:"#B22222",darkgoldenrod:"#B8860B",mediumorchid:"#BA55D3",rosybrown:"#BC8F8F",darkkhaki:"#BDB76B",mediumvioletred:"#C71585",indianred:"#CD5C5C",peru:"#CD853F",chocolate:"#D2691E",tan:"#D2B48C",lightgray:"#D3D3D3",lightgrey:"#D3D3D3",thistle:"#D8BFD8",orchid:"#DA70D6",goldenrod:"#DAA520",palevioletred:"#DB7093",crimson:"#DC143C",gainsboro:"#DCDCDC",plum:"#DDA0DD",burlywood:"#DEB887",lightcyan:"#E0FFFF",lavender:"#E6E6FA",darksalmon:"#E9967A",violet:"#EE82EE",palegoldenrod:"#EEE8AA",lightcoral:"#F08080",khaki:"#F0E68C",aliceblue:"#F0F8FF",honeydew:"#F0FFF0",azure:"#F0FFFF",sandybrown:"#F4A460",wheat:"#F5DEB3",beige:"#F5F5DC",whitesmoke:"#F5F5F5",mintcream:"#F5FFFA",ghostwhite:"#F8F8FF",salmon:"#FA8072",antiquewhite:"#FAEBD7",linen:"#FAF0E6",lightgoldenrodyellow:"#FAFAD2",oldlace:"#FDF5E6",magenta:"#FF00FF",deeppink:"#FF1493",orangered:"#FF4500",tomato:"#FF6347",hotpink:"#FF69B4",coral:"#FF7F50",darkorange:"#FF8C00",lightsalmon:"#FFA07A",orange:"#FFA500",lightpink:"#FFB6C1",pink:"#FFC0CB",gold:"#FFD700",peachpuff:"#FFDAB9",navajowhite:"#FFDEAD",moccasin:"#FFE4B5",bisque:"#FFE4C4",mistyrose:"#FFE4E1",blanchedalmond:"#FFEBCD",papayawhip:"#FFEFD5",lavenderblush:"#FFF0F5",seashell:"#FFF5EE",cornsilk:"#FFF8DC",lemonchiffon:"#FFFACD",floralwhite:"#FFFAF0",snow:"#FFFAFA",lightyellow:"#FFFFE0",ivory:"#FFFFF0"};var ze=new Int8Array(4),le=new Int32Array(ze.buffer,0,1),kr=new Float32Array(ze.buffer,0,1),Or=/^\s*rgba?\s*\(/,Nr=/^\s*rgba?\s*\(\s*([0-9]*)\s*,\s*([0-9]*)\s*,\s*([0-9]*)(?:\s*,\s*(.*)?)?\)\s*$/;function Ie(r){var e=0,n=0,t=0,i=1;if(r[0]==="#")r.length===4?(e=parseInt(r.charAt(1)+r.charAt(1),16),n=parseInt(r.charAt(2)+r.charAt(2),16),t=parseInt(r.charAt(3)+r.charAt(3),16)):(e=parseInt(r.charAt(1)+r.charAt(2),16),n=parseInt(r.charAt(3)+r.charAt(4),16),t=parseInt(r.charAt(5)+r.charAt(6),16)),r.length===9&&(i=parseInt(r.charAt(7)+r.charAt(8),16)/255);else if(Or.test(r)){var o=r.match(Nr);o&&(e=+o[1],n=+o[2],t=+o[3],o[4]&&(i=+o[4]))}return{r:e,g:n,b:t,a:i}}var H={};for($ in K)H[$]=k(K[$]),H[K[$]]=H[$];var $;function ve(r,e,n,t,i){return le[0]=t<<24|n<<16|e<<8|r,i&&(le[0]=le[0]&4278190079),kr[0]}function k(r){if(r=r.toLowerCase(),typeof H[r]<"u")return H[r];var e=Ie(r),n=e.r,t=e.g,i=e.b,o=e.a;o=o*255|0;var a=ve(n,t,i,o,!0);return H[r]=a,a}var fe={};function Q(r){if(typeof fe[r]<"u")return fe[r];var e=(r&16711680)>>>16,n=(r&65280)>>>8,t=r&255,i=255,o=ve(e,n,t,i,!0);return fe[r]=o,o}function y(r,e,n){return(e=ce(e))in r?Object.defineProperty(r,e,{value:n,enumerable:!0,configurable:!0,writable:!0}):r[e]=n,r}function Ge(r,e){var n=Object.keys(r);if(Object.getOwnPropertySymbols){var t=Object.getOwnPropertySymbols(r);e&&(t=t.filter(function(i){return Object.getOwnPropertyDescriptor(r,i).enumerable})),n.push.apply(n,t)}return n}function C(r){for(var e=1;e<arguments.length;e++){var n=arguments[e]!=null?arguments[e]:{};e%2?Ge(Object(n),!0).forEach(function(t){y(r,t,n[t])}):Object.getOwnPropertyDescriptors?Object.defineProperties(r,Object.getOwnPropertyDescriptors(n)):Ge(Object(n)).forEach(function(t){Object.defineProperty(r,t,Object.getOwnPropertyDescriptor(n,t))})}return r}function xr(r,e){for(;!{}.hasOwnProperty.call(r,e)&&(r=D(r))!==null;);return r}function me(){return me=typeof Reflect<"u"&&Reflect.get?Reflect.get.bind():function(r,e,n){var t=xr(r,e);if(t){var i=Object.getOwnPropertyDescriptor(t,e);return i.get?i.get.call(arguments.length<3?r:n):i.value}},me.apply(null,arguments)}function Ve(r,e,n,t){var i=me(D(1&t?r.prototype:r),e,n);return 2&t&&typeof i=="function"?function(o){return i.apply(n,o)}:i}function je(r){return r.normalized?1:r.size}function J(r){var e=0;return r.forEach(function(n){return e+=je(n)}),e}function We(r,e,n){var t=r==="VERTEX"?e.VERTEX_SHADER:e.FRAGMENT_SHADER,i=e.createShader(t);if(i===null)throw new Error("loadShader: error while creating the shader");e.shaderSource(i,n),e.compileShader(i);var o=e.getShaderParameter(i,e.COMPILE_STATUS);if(!o){var a=e.getShaderInfoLog(i);throw e.deleteShader(i),new Error(`loadShader: error while compiling the shader:
`.concat(a,`
`).concat(n))}return i}function $e(r,e){return We("VERTEX",r,e)}function Ye(r,e){return We("FRAGMENT",r,e)}function Xe(r,e){var n=r.createProgram();if(n===null)throw new Error("loadProgram: error while creating the program.");var t,i;for(t=0,i=e.length;t<i;t++)r.attachShader(n,e[t]);r.linkProgram(n);var o=r.getProgramParameter(n,r.LINK_STATUS);if(!o){var a=r.getProgramInfoLog(n);throw r.deleteProgram(n),new Error("loadProgram: error while linking the program: ".concat(a))}return n}function de(r){var e=r.gl,n=r.buffer,t=r.program,i=r.vertexShader,o=r.fragmentShader;e.deleteShader(i),e.deleteShader(o),e.deleteProgram(t),e.deleteBuffer(n)}var Ue=`#define PICKING_MODE
`,Pr=y(y(y(y(y(y(y(y({},WebGL2RenderingContext.BOOL,1),WebGL2RenderingContext.BYTE,1),WebGL2RenderingContext.UNSIGNED_BYTE,1),WebGL2RenderingContext.SHORT,2),WebGL2RenderingContext.UNSIGNED_SHORT,2),WebGL2RenderingContext.INT,4),WebGL2RenderingContext.UNSIGNED_INT,4),WebGL2RenderingContext.FLOAT,4);var he=(function(){function r(e,n,t){w(this,r),y(this,"array",new Float32Array),y(this,"constantArray",new Float32Array),y(this,"capacity",0),y(this,"verticesCount",0);var i=this.getDefinition();if(this.VERTICES=i.VERTICES,this.VERTEX_SHADER_SOURCE=i.VERTEX_SHADER_SOURCE,this.FRAGMENT_SHADER_SOURCE=i.FRAGMENT_SHADER_SOURCE,this.UNIFORMS=i.UNIFORMS,this.ATTRIBUTES=i.ATTRIBUTES,this.METHOD=i.METHOD,this.CONSTANT_ATTRIBUTES="CONSTANT_ATTRIBUTES"in i?i.CONSTANT_ATTRIBUTES:[],this.CONSTANT_DATA="CONSTANT_DATA"in i?i.CONSTANT_DATA:[],this.isInstanced="CONSTANT_ATTRIBUTES"in i,this.ATTRIBUTES_ITEMS_COUNT=J(this.ATTRIBUTES),this.STRIDE=this.VERTICES*this.ATTRIBUTES_ITEMS_COUNT,this.renderer=t,this.normalProgram=this.getProgramInfo("normal",e,i.VERTEX_SHADER_SOURCE,i.FRAGMENT_SHADER_SOURCE,null),this.pickProgram=n?this.getProgramInfo("pick",e,Ue+i.VERTEX_SHADER_SOURCE,Ue+i.FRAGMENT_SHADER_SOURCE,n):null,this.isInstanced){var o=J(this.CONSTANT_ATTRIBUTES);if(this.CONSTANT_DATA.length!==this.VERTICES)throw new Error("Program: error while getting constant data (expected ".concat(this.VERTICES," items, received ").concat(this.CONSTANT_DATA.length," instead)"));this.constantArray=new Float32Array(this.CONSTANT_DATA.length*o);for(var a=0;a<this.CONSTANT_DATA.length;a++){var s=this.CONSTANT_DATA[a];if(s.length!==o)throw new Error("Program: error while getting constant data (one vector has ".concat(s.length," items instead of ").concat(o,")"));for(var u=0;u<s.length;u++)this.constantArray[a*o+u]=s[u]}this.STRIDE=this.ATTRIBUTES_ITEMS_COUNT}}return F(r,[{key:"kill",value:function(){de(this.normalProgram),this.pickProgram&&(de(this.pickProgram),this.pickProgram=null)}},{key:"getProgramInfo",value:function(n,t,i,o,a){var s=this.getDefinition(),u=t.createBuffer();if(u===null)throw new Error("Program: error while creating the WebGL buffer.");var v=$e(t,i),h=Ye(t,o),m=Xe(t,[v,h]),_={};s.UNIFORMS.forEach(function(c){var d=t.getUniformLocation(m,c);d&&(_[c]=d)});var f={};s.ATTRIBUTES.forEach(function(c){f[c.name]=t.getAttribLocation(m,c.name)});var l;if("CONSTANT_ATTRIBUTES"in s&&(s.CONSTANT_ATTRIBUTES.forEach(function(c){f[c.name]=t.getAttribLocation(m,c.name)}),l=t.createBuffer(),l===null))throw new Error("Program: error while creating the WebGL constant buffer.");return{name:n,program:m,gl:t,frameBuffer:a,buffer:u,constantBuffer:l||{},uniformLocations:_,attributeLocations:f,isPicking:n==="pick",vertexShader:v,fragmentShader:h}}},{key:"bindProgram",value:function(n){var t=this,i=0,o=n.gl,a=n.buffer;this.isInstanced?(o.bindBuffer(o.ARRAY_BUFFER,n.constantBuffer),i=0,this.CONSTANT_ATTRIBUTES.forEach(function(s){return i+=t.bindAttribute(s,n,i,!1)}),o.bufferData(o.ARRAY_BUFFER,this.constantArray,o.STATIC_DRAW),o.bindBuffer(o.ARRAY_BUFFER,n.buffer),i=0,this.ATTRIBUTES.forEach(function(s){return i+=t.bindAttribute(s,n,i,!0)}),o.bufferData(o.ARRAY_BUFFER,this.array,o.DYNAMIC_DRAW)):(o.bindBuffer(o.ARRAY_BUFFER,a),i=0,this.ATTRIBUTES.forEach(function(s){return i+=t.bindAttribute(s,n,i)}),o.bufferData(o.ARRAY_BUFFER,this.array,o.DYNAMIC_DRAW)),o.bindBuffer(o.ARRAY_BUFFER,null)}},{key:"unbindProgram",value:function(n){var t=this;this.isInstanced?(this.CONSTANT_ATTRIBUTES.forEach(function(i){return t.unbindAttribute(i,n,!1)}),this.ATTRIBUTES.forEach(function(i){return t.unbindAttribute(i,n,!0)})):this.ATTRIBUTES.forEach(function(i){return t.unbindAttribute(i,n)})}},{key:"bindAttribute",value:function(n,t,i,o){var a=Pr[n.type];if(typeof a!="number")throw new Error('Program.bind: yet unsupported attribute type "'.concat(n.type,'"'));var s=t.attributeLocations[n.name],u=t.gl;if(s!==-1){u.enableVertexAttribArray(s);var v=this.isInstanced?(o?this.ATTRIBUTES_ITEMS_COUNT:J(this.CONSTANT_ATTRIBUTES))*Float32Array.BYTES_PER_ELEMENT:this.ATTRIBUTES_ITEMS_COUNT*Float32Array.BYTES_PER_ELEMENT;if(u.vertexAttribPointer(s,n.size,n.type,n.normalized||!1,v,i),this.isInstanced&&o)if(u instanceof WebGL2RenderingContext)u.vertexAttribDivisor(s,1);else{var h=u.getExtension("ANGLE_instanced_arrays");h&&h.vertexAttribDivisorANGLE(s,1)}}return n.size*a}},{key:"unbindAttribute",value:function(n,t,i){var o=t.attributeLocations[n.name],a=t.gl;if(o!==-1&&(a.disableVertexAttribArray(o),this.isInstanced&&i))if(a instanceof WebGL2RenderingContext)a.vertexAttribDivisor(o,0);else{var s=a.getExtension("ANGLE_instanced_arrays");s&&s.vertexAttribDivisorANGLE(o,0)}}},{key:"reallocate",value:function(n){n!==this.capacity&&(this.capacity=n,this.verticesCount=this.VERTICES*n,this.array=new Float32Array(this.isInstanced?this.capacity*this.ATTRIBUTES_ITEMS_COUNT:this.verticesCount*this.ATTRIBUTES_ITEMS_COUNT))}},{key:"hasNothingToRender",value:function(){return this.verticesCount===0}},{key:"renderProgram",value:function(n,t){var i=t.gl,o=t.program;i.enable(i.BLEND),i.useProgram(o),this.setUniforms(n,t),this.drawWebGL(this.METHOD,t)}},{key:"render",value:function(n){this.hasNothingToRender()||(this.pickProgram&&(this.pickProgram.gl.viewport(0,0,n.width*n.pixelRatio/n.downSizingRatio,n.height*n.pixelRatio/n.downSizingRatio),this.bindProgram(this.pickProgram),this.renderProgram(C(C({},n),{},{pixelRatio:n.pixelRatio/n.downSizingRatio}),this.pickProgram),this.unbindProgram(this.pickProgram)),this.normalProgram.gl.viewport(0,0,n.width*n.pixelRatio,n.height*n.pixelRatio),this.bindProgram(this.normalProgram),this.renderProgram(n,this.normalProgram),this.unbindProgram(this.normalProgram))}},{key:"drawWebGL",value:function(n,t){var i=t.gl,o=t.frameBuffer;if(i.bindFramebuffer(i.FRAMEBUFFER,o),!this.isInstanced)i.drawArrays(n,0,this.verticesCount);else if(i instanceof WebGL2RenderingContext)i.drawArraysInstanced(n,0,this.VERTICES,this.capacity);else{var a=i.getExtension("ANGLE_instanced_arrays");a&&a.drawArraysInstancedANGLE(n,0,this.VERTICES,this.capacity)}}}])})();var ge=(function(r){function e(){return w(this,e),N(this,e,arguments)}return x(e,r),F(e,[{key:"kill",value:function(){Ve(e,"kill",this,3)([])}},{key:"process",value:function(t,i,o){var a=i*this.STRIDE;if(o.hidden){for(var s=a+this.STRIDE;a<s;a++)this.array[a]=0;return}return this.processVisibleItem(Q(t),a,o)}}])})(he);var I=(function(r){function e(){var n;w(this,e);for(var t=arguments.length,i=new Array(t),o=0;o<t;o++)i[o]=arguments[o];return n=N(this,e,[].concat(i)),y(n,"drawLabel",void 0),n}return x(e,r),F(e,[{key:"kill",value:function(){Ve(e,"kill",this,3)([])}},{key:"process",value:function(t,i,o,a,s){var u=i*this.STRIDE;if(s.hidden||o.hidden||a.hidden){for(var v=u+this.STRIDE;u<v;u++)this.array[u]=0;return}return this.processVisibleItem(Q(t),u,o,a,s)}}])})(he);function ee(r,e){return(function(){function n(t,i,o){w(this,n),y(this,"drawLabel",e),this.programs=r.map(function(a){return new a(t,i,o)})}return F(n,[{key:"reallocate",value:function(i){this.programs.forEach(function(o){return o.reallocate(i)})}},{key:"process",value:function(i,o,a,s,u){this.programs.forEach(function(v){return v.process(i,o,a,s,u)})}},{key:"render",value:function(i){this.programs.forEach(function(o){return o.render(i)})}},{key:"kill",value:function(){this.programs.forEach(function(i){return i.kill()})}}])})()}var Lr=`
precision highp float;

varying vec4 v_color;
varying vec2 v_diffVector;
varying float v_radius;

uniform float u_correctionRatio;

const vec4 transparent = vec4(0.0, 0.0, 0.0, 0.0);

void main(void) {
  float border = u_correctionRatio * 2.0;
  float dist = length(v_diffVector) - v_radius + border;

  // No antialiasing for picking mode:
  #ifdef PICKING_MODE
  if (dist > border)
    gl_FragColor = transparent;
  else
    gl_FragColor = v_color;

  #else
  float t = 0.0;
  if (dist > border)
    t = 1.0;
  else if (dist > 0.0)
    t = dist / border;

  gl_FragColor = mix(v_color, transparent, t);
  #endif
}
`,Dr=Lr,zr=`
attribute vec4 a_id;
attribute vec4 a_color;
attribute vec2 a_position;
attribute float a_size;
attribute float a_angle;

uniform mat3 u_matrix;
uniform float u_sizeRatio;
uniform float u_correctionRatio;

varying vec4 v_color;
varying vec2 v_diffVector;
varying float v_radius;
varying float v_border;

const float bias = 255.0 / 254.0;

void main() {
  float size = a_size * u_correctionRatio / u_sizeRatio * 4.0;
  vec2 diffVector = size * vec2(cos(a_angle), sin(a_angle));
  vec2 position = a_position + diffVector;
  gl_Position = vec4(
    (u_matrix * vec3(position, 1)).xy,
    0,
    1
  );

  v_diffVector = diffVector;
  v_radius = size / 2.0;

  #ifdef PICKING_MODE
  // For picking mode, we use the ID as the color:
  v_color = a_id;
  #else
  // For normal mode, we use the color:
  v_color = a_color;
  #endif

  v_color.a *= bias;
}
`,Ir=zr,qe=WebGLRenderingContext,Me=qe.UNSIGNED_BYTE,_e=qe.FLOAT,Gr=["u_sizeRatio","u_correctionRatio","u_matrix"],re=(function(r){function e(){return w(this,e),N(this,e,arguments)}return x(e,r),F(e,[{key:"getDefinition",value:function(){return{VERTICES:3,VERTEX_SHADER_SOURCE:Ir,FRAGMENT_SHADER_SOURCE:Dr,METHOD:WebGLRenderingContext.TRIANGLES,UNIFORMS:Gr,ATTRIBUTES:[{name:"a_position",size:2,type:_e},{name:"a_size",size:1,type:_e},{name:"a_color",size:4,type:Me,normalized:!0},{name:"a_id",size:4,type:Me,normalized:!0}],CONSTANT_ATTRIBUTES:[{name:"a_angle",size:1,type:_e}],CONSTANT_DATA:[[e.ANGLE_1],[e.ANGLE_2],[e.ANGLE_3]]}}},{key:"processVisibleItem",value:function(t,i,o){var a=this.array,s=k(o.color);a[i++]=o.x,a[i++]=o.y,a[i++]=o.size,a[i++]=s,a[i++]=t}},{key:"setUniforms",value:function(t,i){var o=i.gl,a=i.uniformLocations,s=a.u_sizeRatio,u=a.u_correctionRatio,v=a.u_matrix;o.uniform1f(u,t.correctionRatio),o.uniform1f(s,t.sizeRatio),o.uniformMatrix3fv(v,!1,t.matrix)}}])})(ge);y(re,"ANGLE_1",0);y(re,"ANGLE_2",2*Math.PI/3);y(re,"ANGLE_3",4*Math.PI/3);var Ur=`
precision mediump float;

varying vec4 v_color;

void main(void) {
  gl_FragColor = v_color;
}
`,Mr=Ur,Br=`
attribute vec2 a_position;
attribute vec2 a_normal;
attribute float a_radius;
attribute vec3 a_barycentric;

#ifdef PICKING_MODE
attribute vec4 a_id;
#else
attribute vec4 a_color;
#endif

uniform mat3 u_matrix;
uniform float u_sizeRatio;
uniform float u_correctionRatio;
uniform float u_minEdgeThickness;
uniform float u_lengthToThicknessRatio;
uniform float u_widenessToThicknessRatio;

varying vec4 v_color;

const float bias = 255.0 / 254.0;

void main() {
  float minThickness = u_minEdgeThickness;

  float normalLength = length(a_normal);
  vec2 unitNormal = a_normal / normalLength;

  // These first computations are taken from edge.vert.glsl and
  // edge.clamped.vert.glsl. Please read it to get better comments on what's
  // happening:
  float pixelsThickness = max(normalLength / u_sizeRatio, minThickness);
  float webGLThickness = pixelsThickness * u_correctionRatio;
  float webGLNodeRadius = a_radius * 2.0 * u_correctionRatio / u_sizeRatio;
  float webGLArrowHeadLength = webGLThickness * u_lengthToThicknessRatio * 2.0;
  float webGLArrowHeadThickness = webGLThickness * u_widenessToThicknessRatio;

  float da = a_barycentric.x;
  float db = a_barycentric.y;
  float dc = a_barycentric.z;

  vec2 delta = vec2(
      da * (webGLNodeRadius * unitNormal.y)
    + db * ((webGLNodeRadius + webGLArrowHeadLength) * unitNormal.y + webGLArrowHeadThickness * unitNormal.x)
    + dc * ((webGLNodeRadius + webGLArrowHeadLength) * unitNormal.y - webGLArrowHeadThickness * unitNormal.x),

      da * (-webGLNodeRadius * unitNormal.x)
    + db * (-(webGLNodeRadius + webGLArrowHeadLength) * unitNormal.x + webGLArrowHeadThickness * unitNormal.y)
    + dc * (-(webGLNodeRadius + webGLArrowHeadLength) * unitNormal.x - webGLArrowHeadThickness * unitNormal.y)
  );

  vec2 position = (u_matrix * vec3(a_position + delta, 1)).xy;

  gl_Position = vec4(position, 0, 1);

  #ifdef PICKING_MODE
  // For picking mode, we use the ID as the color:
  v_color = a_id;
  #else
  // For normal mode, we use the color:
  v_color = a_color;
  #endif

  v_color.a *= bias;
}
`,Hr=Br,Ke=WebGLRenderingContext,Be=Ke.UNSIGNED_BYTE,Z=Ke.FLOAT,Vr=["u_matrix","u_sizeRatio","u_correctionRatio","u_minEdgeThickness","u_lengthToThicknessRatio","u_widenessToThicknessRatio"],P={extremity:"target",lengthToThicknessRatio:2.5,widenessToThicknessRatio:2};function V(r){var e=C(C({},P),r||{});return(function(n){function t(){return w(this,t),N(this,t,arguments)}return x(t,n),F(t,[{key:"getDefinition",value:function(){return{VERTICES:3,VERTEX_SHADER_SOURCE:Hr,FRAGMENT_SHADER_SOURCE:Mr,METHOD:WebGLRenderingContext.TRIANGLES,UNIFORMS:Vr,ATTRIBUTES:[{name:"a_position",size:2,type:Z},{name:"a_normal",size:2,type:Z},{name:"a_radius",size:1,type:Z},{name:"a_color",size:4,type:Be,normalized:!0},{name:"a_id",size:4,type:Be,normalized:!0}],CONSTANT_ATTRIBUTES:[{name:"a_barycentric",size:3,type:Z}],CONSTANT_DATA:[[1,0,0],[0,1,0],[0,0,1]]}}},{key:"processVisibleItem",value:function(o,a,s,u,v){if(e.extremity==="source"){var h=[u,s];s=h[0],u=h[1]}var m=v.size||1,_=u.size||1,f=s.x,l=s.y,c=u.x,d=u.y,E=k(v.color),b=c-f,R=d-l,T=b*b+R*R,A=0,p=0;T&&(T=1/Math.sqrt(T),A=-R*T*m,p=b*T*m);var g=this.array;g[a++]=c,g[a++]=d,g[a++]=-A,g[a++]=-p,g[a++]=_,g[a++]=E,g[a++]=o}},{key:"setUniforms",value:function(o,a){var s=a.gl,u=a.uniformLocations,v=u.u_matrix,h=u.u_sizeRatio,m=u.u_correctionRatio,_=u.u_minEdgeThickness,f=u.u_lengthToThicknessRatio,l=u.u_widenessToThicknessRatio;s.uniformMatrix3fv(v,!1,o.matrix),s.uniform1f(h,o.sizeRatio),s.uniform1f(m,o.correctionRatio),s.uniform1f(_,o.minEdgeThickness),s.uniform1f(f,e.lengthToThicknessRatio),s.uniform1f(l,e.widenessToThicknessRatio)}}])})(I)}var pn=V();var jr=`
precision mediump float;

varying vec4 v_color;
varying vec2 v_normal;
varying float v_thickness;
varying float v_feather;

const vec4 transparent = vec4(0.0, 0.0, 0.0, 0.0);

void main(void) {
  // We only handle antialiasing for normal mode:
  #ifdef PICKING_MODE
  gl_FragColor = v_color;
  #else
  float dist = length(v_normal) * v_thickness;

  float t = smoothstep(
    v_thickness - v_feather,
    v_thickness,
    dist
  );

  gl_FragColor = mix(v_color, transparent, t);
  #endif
}
`,Te=jr,Wr=`
attribute vec4 a_id;
attribute vec4 a_color;
attribute vec2 a_normal;
attribute float a_normalCoef;
attribute vec2 a_positionStart;
attribute vec2 a_positionEnd;
attribute float a_positionCoef;
attribute float a_radius;
attribute float a_radiusCoef;

uniform mat3 u_matrix;
uniform float u_zoomRatio;
uniform float u_sizeRatio;
uniform float u_pixelRatio;
uniform float u_correctionRatio;
uniform float u_minEdgeThickness;
uniform float u_lengthToThicknessRatio;
uniform float u_feather;

varying vec4 v_color;
varying vec2 v_normal;
varying float v_thickness;
varying float v_feather;

const float bias = 255.0 / 254.0;

void main() {
  float minThickness = u_minEdgeThickness;

  float radius = a_radius * a_radiusCoef;
  vec2 normal = a_normal * a_normalCoef;
  vec2 position = a_positionStart * (1.0 - a_positionCoef) + a_positionEnd * a_positionCoef;

  float normalLength = length(normal);
  vec2 unitNormal = normal / normalLength;

  // These first computations are taken from edge.vert.glsl. Please read it to
  // get better comments on what's happening:
  float pixelsThickness = max(normalLength, minThickness * u_sizeRatio);
  float webGLThickness = pixelsThickness * u_correctionRatio / u_sizeRatio;

  // Here, we move the point to leave space for the arrow head:
  float direction = sign(radius);
  float webGLNodeRadius = direction * radius * 2.0 * u_correctionRatio / u_sizeRatio;
  float webGLArrowHeadLength = webGLThickness * u_lengthToThicknessRatio * 2.0;

  vec2 compensationVector = vec2(-direction * unitNormal.y, direction * unitNormal.x) * (webGLNodeRadius + webGLArrowHeadLength);

  // Here is the proper position of the vertex
  gl_Position = vec4((u_matrix * vec3(position + unitNormal * webGLThickness + compensationVector, 1)).xy, 0, 1);

  v_thickness = webGLThickness / u_zoomRatio;

  v_normal = unitNormal;

  v_feather = u_feather * u_correctionRatio / u_zoomRatio / u_pixelRatio * 2.0;

  #ifdef PICKING_MODE
  // For picking mode, we use the ID as the color:
  v_color = a_id;
  #else
  // For normal mode, we use the color:
  v_color = a_color;
  #endif

  v_color.a *= bias;
}
`,$r=Wr,Qe=WebGLRenderingContext,He=Qe.UNSIGNED_BYTE,z=Qe.FLOAT,Yr=["u_matrix","u_zoomRatio","u_sizeRatio","u_correctionRatio","u_pixelRatio","u_feather","u_minEdgeThickness","u_lengthToThicknessRatio"],Ze={lengthToThicknessRatio:P.lengthToThicknessRatio};function Ee(r){var e=C(C({},Ze),r||{});return(function(n){function t(){return w(this,t),N(this,t,arguments)}return x(t,n),F(t,[{key:"getDefinition",value:function(){return{VERTICES:6,VERTEX_SHADER_SOURCE:$r,FRAGMENT_SHADER_SOURCE:Te,METHOD:WebGLRenderingContext.TRIANGLES,UNIFORMS:Yr,ATTRIBUTES:[{name:"a_positionStart",size:2,type:z},{name:"a_positionEnd",size:2,type:z},{name:"a_normal",size:2,type:z},{name:"a_color",size:4,type:He,normalized:!0},{name:"a_id",size:4,type:He,normalized:!0},{name:"a_radius",size:1,type:z}],CONSTANT_ATTRIBUTES:[{name:"a_positionCoef",size:1,type:z},{name:"a_normalCoef",size:1,type:z},{name:"a_radiusCoef",size:1,type:z}],CONSTANT_DATA:[[0,1,0],[0,-1,0],[1,1,1],[1,1,1],[0,-1,0],[1,-1,-1]]}}},{key:"processVisibleItem",value:function(o,a,s,u,v){var h=v.size||1,m=s.x,_=s.y,f=u.x,l=u.y,c=k(v.color),d=f-m,E=l-_,b=u.size||1,R=d*d+E*E,T=0,A=0;R&&(R=1/Math.sqrt(R),T=-E*R*h,A=d*R*h);var p=this.array;p[a++]=m,p[a++]=_,p[a++]=f,p[a++]=l,p[a++]=T,p[a++]=A,p[a++]=c,p[a++]=o,p[a++]=b}},{key:"setUniforms",value:function(o,a){var s=a.gl,u=a.uniformLocations,v=u.u_matrix,h=u.u_zoomRatio,m=u.u_feather,_=u.u_pixelRatio,f=u.u_correctionRatio,l=u.u_sizeRatio,c=u.u_minEdgeThickness,d=u.u_lengthToThicknessRatio;s.uniformMatrix3fv(v,!1,o.matrix),s.uniform1f(h,o.zoomRatio),s.uniform1f(l,o.sizeRatio),s.uniform1f(f,o.correctionRatio),s.uniform1f(_,o.pixelRatio),s.uniform1f(m,o.antiAliasingFeather),s.uniform1f(c,o.minEdgeThickness),s.uniform1f(d,e.lengthToThicknessRatio)}}])})(I)}var bn=Ee();function Je(r){return ee([Ee(r),V(r)])}var yn=Je();var er=WebGLRenderingContext,An=er.UNSIGNED_BYTE,Sn=er.FLOAT;var nr=WebGLRenderingContext,Hn=nr.UNSIGNED_BYTE,Vn=nr.FLOAT;var Xr=`
attribute vec4 a_id;
attribute vec4 a_color;
attribute vec2 a_normal;
attribute float a_normalCoef;
attribute vec2 a_positionStart;
attribute vec2 a_positionEnd;
attribute float a_positionCoef;
attribute float a_sourceRadius;
attribute float a_targetRadius;
attribute float a_sourceRadiusCoef;
attribute float a_targetRadiusCoef;

uniform mat3 u_matrix;
uniform float u_zoomRatio;
uniform float u_sizeRatio;
uniform float u_pixelRatio;
uniform float u_correctionRatio;
uniform float u_minEdgeThickness;
uniform float u_lengthToThicknessRatio;
uniform float u_feather;

varying vec4 v_color;
varying vec2 v_normal;
varying float v_thickness;
varying float v_feather;

const float bias = 255.0 / 254.0;

void main() {
  float minThickness = u_minEdgeThickness;

  vec2 normal = a_normal * a_normalCoef;
  vec2 position = a_positionStart * (1.0 - a_positionCoef) + a_positionEnd * a_positionCoef;

  float normalLength = length(normal);
  vec2 unitNormal = normal / normalLength;

  // These first computations are taken from edge.vert.glsl. Please read it to
  // get better comments on what's happening:
  float pixelsThickness = max(normalLength, minThickness * u_sizeRatio);
  float webGLThickness = pixelsThickness * u_correctionRatio / u_sizeRatio;

  // Here, we move the point to leave space for the arrow heads:
  // Source arrow head
  float sourceRadius = a_sourceRadius * a_sourceRadiusCoef;
  float sourceDirection = sign(sourceRadius);
  float webGLSourceRadius = sourceDirection * sourceRadius * 2.0 * u_correctionRatio / u_sizeRatio;
  float webGLSourceArrowHeadLength = webGLThickness * u_lengthToThicknessRatio * 2.0;
  vec2 sourceCompensationVector =
    vec2(-sourceDirection * unitNormal.y, sourceDirection * unitNormal.x)
    * (webGLSourceRadius + webGLSourceArrowHeadLength);
    
  // Target arrow head
  float targetRadius = a_targetRadius * a_targetRadiusCoef;
  float targetDirection = sign(targetRadius);
  float webGLTargetRadius = targetDirection * targetRadius * 2.0 * u_correctionRatio / u_sizeRatio;
  float webGLTargetArrowHeadLength = webGLThickness * u_lengthToThicknessRatio * 2.0;
  vec2 targetCompensationVector =
  vec2(-targetDirection * unitNormal.y, targetDirection * unitNormal.x)
    * (webGLTargetRadius + webGLTargetArrowHeadLength);

  // Here is the proper position of the vertex
  gl_Position = vec4((u_matrix * vec3(position + unitNormal * webGLThickness + sourceCompensationVector + targetCompensationVector, 1)).xy, 0, 1);

  v_thickness = webGLThickness / u_zoomRatio;

  v_normal = unitNormal;

  v_feather = u_feather * u_correctionRatio / u_zoomRatio / u_pixelRatio * 2.0;

  #ifdef PICKING_MODE
  // For picking mode, we use the ID as the color:
  v_color = a_id;
  #else
  // For normal mode, we use the color:
  v_color = a_color;
  #endif

  v_color.a *= bias;
}
`,qr=Xr,tr=WebGLRenderingContext,rr=tr.UNSIGNED_BYTE,O=tr.FLOAT,Kr=["u_matrix","u_zoomRatio","u_sizeRatio","u_correctionRatio","u_pixelRatio","u_feather","u_minEdgeThickness","u_lengthToThicknessRatio"],Qr={lengthToThicknessRatio:P.lengthToThicknessRatio};function or(r){var e=C(C({},Qr),r||{});return(function(n){function t(){return w(this,t),N(this,t,arguments)}return x(t,n),F(t,[{key:"getDefinition",value:function(){return{VERTICES:6,VERTEX_SHADER_SOURCE:qr,FRAGMENT_SHADER_SOURCE:Te,METHOD:WebGLRenderingContext.TRIANGLES,UNIFORMS:Kr,ATTRIBUTES:[{name:"a_positionStart",size:2,type:O},{name:"a_positionEnd",size:2,type:O},{name:"a_normal",size:2,type:O},{name:"a_color",size:4,type:rr,normalized:!0},{name:"a_id",size:4,type:rr,normalized:!0},{name:"a_sourceRadius",size:1,type:O},{name:"a_targetRadius",size:1,type:O}],CONSTANT_ATTRIBUTES:[{name:"a_positionCoef",size:1,type:O},{name:"a_normalCoef",size:1,type:O},{name:"a_sourceRadiusCoef",size:1,type:O},{name:"a_targetRadiusCoef",size:1,type:O}],CONSTANT_DATA:[[0,1,-1,0],[0,-1,1,0],[1,1,0,1],[1,1,0,1],[0,-1,1,0],[1,-1,0,-1]]}}},{key:"processVisibleItem",value:function(o,a,s,u,v){var h=v.size||1,m=s.x,_=s.y,f=u.x,l=u.y,c=k(v.color),d=f-m,E=l-_,b=s.size||1,R=u.size||1,T=d*d+E*E,A=0,p=0;T&&(T=1/Math.sqrt(T),A=-E*T*h,p=d*T*h);var g=this.array;g[a++]=m,g[a++]=_,g[a++]=f,g[a++]=l,g[a++]=A,g[a++]=p,g[a++]=c,g[a++]=o,g[a++]=b,g[a++]=R}},{key:"setUniforms",value:function(o,a){var s=a.gl,u=a.uniformLocations,v=u.u_matrix,h=u.u_zoomRatio,m=u.u_feather,_=u.u_pixelRatio,f=u.u_correctionRatio,l=u.u_sizeRatio,c=u.u_minEdgeThickness,d=u.u_lengthToThicknessRatio;s.uniformMatrix3fv(v,!1,o.matrix),s.uniform1f(h,o.zoomRatio),s.uniform1f(l,o.sizeRatio),s.uniform1f(f,o.correctionRatio),s.uniform1f(_,o.pixelRatio),s.uniform1f(m,o.antiAliasingFeather),s.uniform1f(c,o.minEdgeThickness),s.uniform1f(d,e.lengthToThicknessRatio)}}])})(I)}var jn=or();function Zr(r){return ee([or(r),V(r),V(C(C({},r),{},{extremity:"source"}))])}var Wn=Zr();var ir=WebGLRenderingContext,$n=ir.UNSIGNED_BYTE,Yn=ir.FLOAT;var ar=WebGLRenderingContext,Xn=ar.UNSIGNED_BYTE,qn=ar.FLOAT;var tt=Sr(ur());function Jr(r,e){if(typeof r!="object"||!r)return r;var n=r[Symbol.toPrimitive];if(n!==void 0){var t=n.call(r,e||"default");if(typeof t!="object")return t;throw new TypeError("@@toPrimitive must return a primitive value.")}return(e==="string"?String:Number)(r)}function vr(r){var e=Jr(r,"string");return typeof e=="symbol"?e:e+""}function _r(r,e,n){return(e=vr(e))in r?Object.defineProperty(r,e,{value:n,enumerable:!0,configurable:!0,writable:!0}):r[e]=n,r}function cr(r,e){var n=Object.keys(r);if(Object.getOwnPropertySymbols){var t=Object.getOwnPropertySymbols(r);e&&(t=t.filter(function(i){return Object.getOwnPropertyDescriptor(r,i).enumerable})),n.push.apply(n,t)}return n}function ne(r){for(var e=1;e<arguments.length;e++){var n=arguments[e]!=null?arguments[e]:{};e%2?cr(Object(n),!0).forEach(function(t){_r(r,t,n[t])}):Object.getOwnPropertyDescriptors?Object.defineProperties(r,Object.getOwnPropertyDescriptors(n)):cr(Object(n)).forEach(function(t){Object.defineProperty(r,t,Object.getOwnPropertyDescriptor(n,t))})}return r}function en(r,e){if(!(r instanceof e))throw new TypeError("Cannot call a class as a function")}function lr(r,e){for(var n=0;n<e.length;n++){var t=e[n];t.enumerable=t.enumerable||!1,t.configurable=!0,"value"in t&&(t.writable=!0),Object.defineProperty(r,vr(t.key),t)}}function rn(r,e,n){return e&&lr(r.prototype,e),n&&lr(r,n),Object.defineProperty(r,"prototype",{writable:!1}),r}function te(r){return te=Object.setPrototypeOf?Object.getPrototypeOf.bind():function(e){return e.__proto__||Object.getPrototypeOf(e)},te(r)}function mr(){try{var r=!Boolean.prototype.valueOf.call(Reflect.construct(Boolean,[],function(){}))}catch{}return(mr=function(){return!!r})()}function dr(r){if(r===void 0)throw new ReferenceError("this hasn't been initialised - super() hasn't been called");return r}function nn(r,e){if(e&&(typeof e=="object"||typeof e=="function"))return e;if(e!==void 0)throw new TypeError("Derived constructors may only return object or undefined");return dr(r)}function tn(r,e,n){return e=te(e),nn(r,mr()?Reflect.construct(e,n||[],te(r).constructor):e.apply(r,n))}function pe(r,e){return pe=Object.setPrototypeOf?Object.setPrototypeOf.bind():function(n,t){return n.__proto__=t,n},pe(r,e)}function on(r,e){if(typeof e!="function"&&e!==null)throw new TypeError("Super expression must either be null or a function");r.prototype=Object.create(e&&e.prototype,{constructor:{value:r,writable:!0,configurable:!0}}),Object.defineProperty(r,"prototype",{writable:!1}),e&&pe(r,e)}function be(r,e){(e==null||e>r.length)&&(e=r.length);for(var n=0,t=Array(e);n<e;n++)t[n]=r[n];return t}function an(r){if(Array.isArray(r))return be(r)}function sn(r){if(typeof Symbol<"u"&&r[Symbol.iterator]!=null||r["@@iterator"]!=null)return Array.from(r)}function un(r,e){if(r){if(typeof r=="string")return be(r,e);var n={}.toString.call(r).slice(8,-1);return n==="Object"&&r.constructor&&(n=r.constructor.name),n==="Map"||n==="Set"?Array.from(r):n==="Arguments"||/^(?:Ui|I)nt(?:8|16|32)(?:Clamped)?Array$/.test(n)?be(r,e):void 0}}function cn(){throw new TypeError(`Invalid attempt to spread non-iterable instance.
In order to be iterable, non-array objects must have a [Symbol.iterator]() method.`)}function Re(r){return an(r)||sn(r)||un(r)||cn()}function hr(r,e,n,t){var i=Math.pow(1-r,2)*e.x+2*(1-r)*r*n.x+Math.pow(r,2)*t.x,o=Math.pow(1-r,2)*e.y+2*(1-r)*r*n.y+Math.pow(r,2)*t.y;return{x:i,y:o}}function ln(r,e,n){for(var t=20,i=0,o=r,a=0;a<t;a++){var s=hr((a+1)/t,r,e,n);i+=Math.sqrt(Math.pow(o.x-s.x,2)+Math.pow(o.y-s.y,2)),o=s}return i}function fn(r){var e=r.curvatureAttribute,n=r.defaultCurvature,t=r.keepLabelUpright,i=t===void 0?!0:t;return function(o,a,s,u,v){var h=v.edgeLabelSize,m=a[e]||n,_=v.edgeLabelFont,f=v.edgeLabelWeight,l=v.edgeLabelColor.attribute?a[v.edgeLabelColor.attribute]||v.edgeLabelColor.color||"#000":v.edgeLabelColor.color,c=a.label;if(c){o.fillStyle=l,o.font="".concat(f," ").concat(h,"px ").concat(_);var d=!i||s.x<u.x,E=d?s.x:u.x,b=d?s.y:u.y,R=d?u.x:s.x,T=d?u.y:s.y,A=(E+R)/2,p=(b+T)/2,g=R-E,j=T-b,S=Math.sqrt(Math.pow(g,2)+Math.pow(j,2)),Ae=d?1:-1,G=A+j*m*Ae,U=p-g*m*Ae,M=a.size*.7+5,Y={x:U-b,y:-(G-E)},Se=Math.sqrt(Math.pow(Y.x,2)+Math.pow(Y.y,2)),X={x:T-U,y:-(R-G)},Ce=Math.sqrt(Math.pow(X.x,2)+Math.pow(X.y,2));E+=M*Y.x/Se,b+=M*Y.y/Se,R+=M*X.x/Ce,T+=M*X.y/Ce,G+=M*j/S,U-=M*g/S;var we={x:G,y:U},Fe={x:E,y:b},ke={x:R,y:T},q=ln(Fe,we,ke);if(!(q<s.size+u.size)){var W=o.measureText(c).width,Oe=q-s.size-u.size;if(W>Oe){var Ne="\u2026";for(c=c+Ne,W=o.measureText(c).width;W>Oe&&c.length>1;)c=c.slice(0,-2)+Ne,W=o.measureText(c).width;if(c.length<4)return}for(var oe={},ie=0,Er=c.length;ie<Er;ie++){var ae=c[ie];oe[ae]||(oe[ae]=o.measureText(ae).width*(1+m*.35))}for(var B=.5-W/q/2,se=0,Rr=c.length;se<Rr;se++){var xe=c[se],Pe=hr(B,Fe,we,ke),pr=2*(1-B)*(G-E)+2*B*(R-G),br=2*(1-B)*(U-b)+2*B*(T-U),yr=Math.atan2(br,pr);o.save(),o.translate(Pe.x,Pe.y),o.rotate(yr),o.fillText(xe,0,0),o.restore(),B+=oe[xe]/q}}}}}function vn(r){var e=r.arrowHead,n=e?.extremity==="target"||e?.extremity==="both",t=e?.extremity==="source"||e?.extremity==="both",i=`
precision highp float;

varying vec4 v_color;
varying float v_thickness;
varying float v_feather;
varying vec2 v_cpA;
varying vec2 v_cpB;
varying vec2 v_cpC;
`.concat(n?`
varying float v_targetSize;
varying vec2 v_targetPoint;`:"",`
`).concat(t?`
varying float v_sourceSize;
varying vec2 v_sourcePoint;`:"",`
`).concat(e?`
uniform float u_lengthToThicknessRatio;
uniform float u_widenessToThicknessRatio;`:"",`

float det(vec2 a, vec2 b) {
  return a.x * b.y - b.x * a.y;
}

vec2 getDistanceVector(vec2 b0, vec2 b1, vec2 b2) {
  float a = det(b0, b2), b = 2.0 * det(b1, b0), d = 2.0 * det(b2, b1);
  float f = b * d - a * a;
  vec2 d21 = b2 - b1, d10 = b1 - b0, d20 = b2 - b0;
  vec2 gf = 2.0 * (b * d21 + d * d10 + a * d20);
  gf = vec2(gf.y, -gf.x);
  vec2 pp = -f * gf / dot(gf, gf);
  vec2 d0p = b0 - pp;
  float ap = det(d0p, d20), bp = 2.0 * det(d10, d0p);
  float t = clamp((ap + bp) / (2.0 * a + b + d), 0.0, 1.0);
  return mix(mix(b0, b1, t), mix(b1, b2, t), t);
}

float distToQuadraticBezierCurve(vec2 p, vec2 b0, vec2 b1, vec2 b2) {
  return length(getDistanceVector(b0 - p, b1 - p, b2 - p));
}

const vec4 transparent = vec4(0.0, 0.0, 0.0, 0.0);

void main(void) {
  float dist = distToQuadraticBezierCurve(gl_FragCoord.xy, v_cpA, v_cpB, v_cpC);
  float thickness = v_thickness;
`).concat(n?`
  float distToTarget = length(gl_FragCoord.xy - v_targetPoint);
  float targetArrowLength = v_targetSize + thickness * u_lengthToThicknessRatio;
  if (distToTarget < targetArrowLength) {
    thickness = (distToTarget - v_targetSize) / (targetArrowLength - v_targetSize) * u_widenessToThicknessRatio * thickness;
  }`:"",`
`).concat(t?`
  float distToSource = length(gl_FragCoord.xy - v_sourcePoint);
  float sourceArrowLength = v_sourceSize + thickness * u_lengthToThicknessRatio;
  if (distToSource < sourceArrowLength) {
    thickness = (distToSource - v_sourceSize) / (sourceArrowLength - v_sourceSize) * u_widenessToThicknessRatio * thickness;
  }`:"",`

  float halfThickness = thickness / 2.0;
  if (dist < halfThickness) {
    #ifdef PICKING_MODE
    gl_FragColor = v_color;
    #else
    float t = smoothstep(
      halfThickness - v_feather,
      halfThickness,
      dist
    );

    gl_FragColor = mix(v_color, transparent, t);
    #endif
  } else {
    gl_FragColor = transparent;
  }
}
`);return i}function _n(r){var e=r.arrowHead,n=e?.extremity==="target"||e?.extremity==="both",t=e?.extremity==="source"||e?.extremity==="both",i=`
attribute vec4 a_id;
attribute vec4 a_color;
attribute float a_direction;
attribute float a_thickness;
attribute vec2 a_source;
attribute vec2 a_target;
attribute float a_current;
attribute float a_curvature;
`.concat(n?`attribute float a_targetSize;
`:"",`
`).concat(t?`attribute float a_sourceSize;
`:"",`

uniform mat3 u_matrix;
uniform float u_sizeRatio;
uniform float u_pixelRatio;
uniform vec2 u_dimensions;
uniform float u_minEdgeThickness;
uniform float u_feather;

varying vec4 v_color;
varying float v_thickness;
varying float v_feather;
varying vec2 v_cpA;
varying vec2 v_cpB;
varying vec2 v_cpC;
`).concat(n?`
varying float v_targetSize;
varying vec2 v_targetPoint;`:"",`
`).concat(t?`
varying float v_sourceSize;
varying vec2 v_sourcePoint;`:"",`
`).concat(e?`
uniform float u_widenessToThicknessRatio;`:"",`

const float bias = 255.0 / 254.0;
const float epsilon = 0.7;

vec2 clipspaceToViewport(vec2 pos, vec2 dimensions) {
  return vec2(
    (pos.x + 1.0) * dimensions.x / 2.0,
    (pos.y + 1.0) * dimensions.y / 2.0
  );
}

vec2 viewportToClipspace(vec2 pos, vec2 dimensions) {
  return vec2(
    pos.x / dimensions.x * 2.0 - 1.0,
    pos.y / dimensions.y * 2.0 - 1.0
  );
}

void main() {
  float minThickness = u_minEdgeThickness;

  // Selecting the correct position
  // Branchless "position = a_source if a_current == 1.0 else a_target"
  vec2 position = a_source * max(0.0, a_current) + a_target * max(0.0, 1.0 - a_current);
  position = (u_matrix * vec3(position, 1)).xy;

  vec2 source = (u_matrix * vec3(a_source, 1)).xy;
  vec2 target = (u_matrix * vec3(a_target, 1)).xy;

  vec2 viewportPosition = clipspaceToViewport(position, u_dimensions);
  vec2 viewportSource = clipspaceToViewport(source, u_dimensions);
  vec2 viewportTarget = clipspaceToViewport(target, u_dimensions);

  vec2 delta = viewportTarget.xy - viewportSource.xy;
  float len = length(delta);
  vec2 normal = vec2(-delta.y, delta.x) * a_direction;
  vec2 unitNormal = normal / len;
  float boundingBoxThickness = len * a_curvature;

  float curveThickness = max(minThickness, a_thickness / u_sizeRatio);
  v_thickness = curveThickness * u_pixelRatio;
  v_feather = u_feather;

  v_cpA = viewportSource;
  v_cpB = 0.5 * (viewportSource + viewportTarget) + unitNormal * a_direction * boundingBoxThickness;
  v_cpC = viewportTarget;

  vec2 viewportOffsetPosition = (
    viewportPosition +
    unitNormal * (boundingBoxThickness / 2.0 + sign(boundingBoxThickness) * (`).concat(e?"curveThickness * u_widenessToThicknessRatio":"curveThickness",` + epsilon)) *
    max(0.0, a_direction) // NOTE: cutting the bounding box in half to avoid overdraw
  );

  position = viewportToClipspace(viewportOffsetPosition, u_dimensions);
  gl_Position = vec4(position, 0, 1);
    
`).concat(n?`
  v_targetSize = a_targetSize * u_pixelRatio / u_sizeRatio;
  v_targetPoint = viewportTarget;
`:"",`
`).concat(t?`
  v_sourceSize = a_sourceSize * u_pixelRatio / u_sizeRatio;
  v_sourcePoint = viewportSource;
`:"",`

  #ifdef PICKING_MODE
  // For picking mode, we use the ID as the color:
  v_color = a_id;
  #else
  // For normal mode, we use the color:
  v_color = a_color;
  #endif

  v_color.a *= bias;
}
`);return i}var gr=.25,mn={arrowHead:null,curvatureAttribute:"curvature",defaultCurvature:gr};var Tr=WebGLRenderingContext,fr=Tr.UNSIGNED_BYTE,L=Tr.FLOAT;function ye(r){var e=ne(ne({},mn),r||{}),n=e,t=n.arrowHead,i=n.curvatureAttribute,o=n.drawLabel,a=t?.extremity==="target"||t?.extremity==="both",s=t?.extremity==="source"||t?.extremity==="both",u=["u_matrix","u_sizeRatio","u_dimensions","u_pixelRatio","u_feather","u_minEdgeThickness"].concat(Re(t?["u_lengthToThicknessRatio","u_widenessToThicknessRatio"]:[]));return(function(v){on(h,v);function h(){var m;en(this,h);for(var _=arguments.length,f=new Array(_),l=0;l<_;l++)f[l]=arguments[l];return m=tn(this,h,[].concat(f)),_r(dr(m),"drawLabel",o||fn(e)),m}return rn(h,[{key:"getDefinition",value:function(){return{VERTICES:6,VERTEX_SHADER_SOURCE:_n(e),FRAGMENT_SHADER_SOURCE:vn(e),METHOD:WebGLRenderingContext.TRIANGLES,UNIFORMS:u,ATTRIBUTES:[{name:"a_source",size:2,type:L},{name:"a_target",size:2,type:L}].concat(Re(a?[{name:"a_targetSize",size:1,type:L}]:[]),Re(s?[{name:"a_sourceSize",size:1,type:L}]:[]),[{name:"a_thickness",size:1,type:L},{name:"a_curvature",size:1,type:L},{name:"a_color",size:4,type:fr,normalized:!0},{name:"a_id",size:4,type:fr,normalized:!0}]),CONSTANT_ATTRIBUTES:[{name:"a_current",size:1,type:L},{name:"a_direction",size:1,type:L}],CONSTANT_DATA:[[0,1],[0,-1],[1,1],[0,-1],[1,1],[1,-1]]}}},{key:"processVisibleItem",value:function(_,f,l,c,d){var E,b=d.size||1,R=l.x,T=l.y,A=c.x,p=c.y,g=k(d.color),j=(E=d[i])!==null&&E!==void 0?E:gr,S=this.array;S[f++]=R,S[f++]=T,S[f++]=A,S[f++]=p,a&&(S[f++]=c.size),s&&(S[f++]=l.size),S[f++]=b,S[f++]=j,S[f++]=g,S[f++]=_}},{key:"setUniforms",value:function(_,f){var l=f.gl,c=f.uniformLocations,d=c.u_matrix,E=c.u_pixelRatio,b=c.u_feather,R=c.u_sizeRatio,T=c.u_dimensions,A=c.u_minEdgeThickness;if(l.uniformMatrix3fv(d,!1,_.matrix),l.uniform1f(E,_.pixelRatio),l.uniform1f(R,_.sizeRatio),l.uniform1f(b,_.antiAliasingFeather),l.uniform2f(T,_.width*_.pixelRatio,_.height*_.pixelRatio),l.uniform1f(A,_.minEdgeThickness),t){var p=c.u_lengthToThicknessRatio,g=c.u_widenessToThicknessRatio;l.uniform1f(p,t.lengthToThicknessRatio),l.uniform1f(g,t.widenessToThicknessRatio)}}}]),h})(I)}var dn=ye(),hn=ye({arrowHead:P}),ut=ye({arrowHead:ne(ne({},P),{},{extremity:"both"})});export{hn as EdgeCurvedArrowProgram,dn as default};
