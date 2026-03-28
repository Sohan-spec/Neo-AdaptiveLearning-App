import React, { useRef, useEffect, useState } from 'react';
import * as THREE from 'three';

// Vertex Shader: Simplex Noise Displacement
const vertexShader = `
  varying vec2 vUv;
  varying vec3 vNormal;
  varying vec3 vPosition;
  uniform float uTime;

  vec3 mod289(vec3 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
  vec4 mod289(vec4 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
  vec4 permute(vec4 x) { return mod289(((x*34.0)+1.0)*x); }
  vec4 taylorInvSqrt(vec4 r) { return 1.79284291400159 - 0.85373472095314 * r; }

  float snoise(vec3 v) {
    const vec2  C = vec2(1.0/6.0, 1.0/3.0) ;
    const vec4  D = vec4(0.0, 0.5, 1.0, 2.0);
    vec3 i  = floor(v + dot(v, C.yyy) );
    vec3 x0 = v - i + dot(i, C.xxx) ;
    vec3 g = step(x0.yzx, x0.xyz);
    vec3 l = 1.0 - g;
    vec3 i1 = min( g.xyz, l.zxy );
    vec3 i2 = max( g.xyz, l.zxy );
    vec3 x1 = x0 - i1 + C.xxx;
    vec3 x2 = x0 - i2 + C.yyy;
    vec3 x3 = x0 - D.yyy;
    i = mod289(i);
    vec4 p = permute( permute( permute(
              i.z + vec4(0.0, i1.z, i2.z, 1.0 ))
            + i.y + vec4(0.0, i1.y, i2.y, 1.0 ))
            + i.x + vec4(0.0, i1.x, i2.x, 1.0 ));
    float n_ = 0.142857142857;
    vec3  ns = n_ * D.wyz - D.xzx;
    vec4 j = p - 49.0 * floor(p * ns.z * ns.z);
    vec4 x_ = floor(j * ns.z);
    vec4 y_ = floor(j - 7.0 * x_ );
    vec4 x = x_ *ns.x + ns.yyyy;
    vec4 y = y_ *ns.x + ns.yyyy;
    vec4 h = 1.0 - abs(x) - abs(y);
    vec4 b0 = vec4( x.xy, y.xy );
    vec4 b1 = vec4( x.zw, y.zw );
    vec4 s0 = floor(b0)*2.0 + 1.0;
    vec4 s1 = floor(b1)*2.0 + 1.0;
    vec4 sh = -step(h, vec4(0.0));
    vec4 a0 = b0.xzyw + s0.xzyw*sh.xxyy ;
    vec4 a1 = b1.xzyw + s1.xzyw*sh.zzww ;
    vec3 p0 = vec3(a0.xy,h.x);
    vec3 p1 = vec3(a0.zw,h.y);
    vec3 p2 = vec3(a1.xy,h.z);
    vec3 p3 = vec3(a1.zw,h.w);
    vec4 norm = taylorInvSqrt(vec4(dot(p0,p0), dot(p1,p1), dot(p2, p2), dot(p3,p3)));
    p0 *= norm.x;
    p1 *= norm.y;
    p2 *= norm.z;
    p3 *= norm.w;
    vec4 m = max(0.6 - vec4(dot(x0,x0), dot(x1,x1), dot(x2,x2), dot(x3,x3)), 0.0);
    m = m * m;
    return 42.0 * dot( m*m, vec4( dot(p0,x0), dot(p1,x1),
                                  dot(p2,x2), dot(p3,x3) ) );
  }

  void main() {
    vUv = uv;
    vNormal = normal;
    float noise = snoise(vec3(position * 2.5 + uTime * 0.4));
    vec3 newPos = position + normal * noise * 0.2;
    vPosition = newPos;
    gl_Position = projectionMatrix * modelViewMatrix * vec4(newPos, 1.0);
  }
`;

// Fragment Shader: Glass & Glow
const fragmentShader = `
  varying vec2 vUv;
  varying vec3 vNormal;
  varying vec3 vPosition;
  uniform float uTime;
  uniform vec3 uColor1;
  uniform vec3 uColor2;
  uniform vec3 uColor3;
  uniform float uIntensity;

  void main() {
    float noise = sin(vPosition.x * 1.5 + uTime * 0.6) * 0.5 + 0.5;
    vec3 color = mix(uColor1, uColor2, noise);
    color = mix(color, uColor3, sin(vPosition.y * 2.0 + uTime * 0.9) * 0.5 + 0.5);

    vec3 viewDirection = normalize(cameraPosition - vPosition);
    float fresnel = pow(1.0 - dot(viewDirection, vNormal), 4.0);
    float glow = sin(vPosition.z * 10.0 + uTime * 2.0) * 0.1 + 0.9;
    
    vec3 finalColor = color * glow * uIntensity;
    finalColor += fresnel * 0.8; 
    
    gl_FragColor = vec4(finalColor, 0.95);
  }
`;

const App = () => {
  const containerRef = useRef(null);
  const timeoutRef = useRef(null);
  const intensityRef = useRef(1.0);
  const [isHovered, setIsHovered] = useState(false);

  useEffect(() => {
    if (!containerRef.current) return;

    // --- SETUP SCENE ---
    const scene = new THREE.Scene();
    const camera = new THREE.PerspectiveCamera(35, window.innerWidth / window.innerHeight, 0.1, 1000);
    camera.position.z = 5;

    const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    renderer.setSize(window.innerWidth, window.innerHeight);
    containerRef.current.appendChild(renderer.domElement);

    // --- GEOMETRY & MATERIAL ---
    const geometry = new THREE.SphereGeometry(1, 128, 128);
    const material = new THREE.ShaderMaterial({
      vertexShader,
      fragmentShader,
      transparent: true,
      side: THREE.DoubleSide,
      uniforms: {
        uTime: { value: 0 },
        uColor1: { value: new THREE.Color("#6366f1") },
        uColor2: { value: new THREE.Color("#ec4899") },
        uColor3: { value: new THREE.Color("#0ea5e9") },
        uIntensity: { value: 1.0 },
      }
    });

    const orb = new THREE.Mesh(geometry, material);
    scene.add(orb);

    // --- HANDLE MOUSE / RESIZE ---
    const handleResize = () => {
      camera.aspect = window.innerWidth / window.innerHeight;
      camera.updateProjectionMatrix();
      renderer.setSize(window.innerWidth, window.innerHeight);
      
      // Responsive scaling
      const scale = window.innerWidth < 768 ? 0.8 : 1.2;
      orb.scale.set(scale, scale, scale);
    };

    const handlePointerMove = () => {
      if (timeoutRef.current) clearTimeout(timeoutRef.current);
      intensityRef.current = 1.5;
      timeoutRef.current = setTimeout(() => {
        intensityRef.current = 1.0;
      }, 250);
    };

    window.addEventListener('resize', handleResize);
    window.addEventListener('pointermove', handlePointerMove);
    handleResize();

    // --- ANIMATION LOOP ---
    const clock = new THREE.Clock();
    let requestID;

    const animate = () => {
      const elapsedTime = clock.getElapsedTime();
      
      material.uniforms.uTime.value = elapsedTime;
      // Smoothly interpolate intensity
      material.uniforms.uIntensity.value = THREE.MathUtils.lerp(
        material.uniforms.uIntensity.value, 
        intensityRef.current, 
        0.1
      );

      orb.rotation.y = elapsedTime * 0.1;
      orb.rotation.z = elapsedTime * 0.05;

      renderer.render(scene, camera);
      requestID = requestAnimationFrame(animate);
    };

    animate();

    // --- CLEANUP ---
    return () => {
      window.removeEventListener('resize', handleResize);
      window.removeEventListener('pointermove', handlePointerMove);
      if (timeoutRef.current) clearTimeout(timeoutRef.current);
      cancelAnimationFrame(requestID);
      renderer.dispose();
      if (containerRef.current) containerRef.current.removeChild(renderer.domElement);
    };
  }, []);

  return (
    <div className="relative w-full h-screen bg-[#020617] flex flex-col items-center justify-center overflow-hidden">
      {/* Background Decor */}
      <div className="absolute inset-0 pointer-events-none">
        <div className="absolute top-[-10%] left-[-10%] w-[50%] h-[50%] rounded-full bg-indigo-500/10 blur-[140px]" />
        <div className="absolute bottom-[-10%] right-[-10%] w-[50%] h-[50%] rounded-full bg-pink-500/10 blur-[140px]" />
      </div>

      {/* Hero Header */}
      <div className="absolute top-16 left-0 w-full text-center z-20 pointer-events-none px-4">
        <h1 className="text-white text-4xl md:text-5xl font-extralight tracking-tight mb-3 opacity-90 drop-shadow-2xl">
          Lumina <span className="font-bold">Orb v2</span>
        </h1>
        <p className="text-indigo-400/50 text-[10px] uppercase tracking-[0.6em] font-bold">
          Vanilla WebGL Implementation
        </p>
      </div>

      {/* Main Container */}
      <div 
        ref={containerRef}
        className="w-full h-full cursor-none transition-transform duration-1000 ease-out"
        style={{ transform: isHovered ? 'scale(1.05)' : 'scale(1)' }}
        onMouseEnter={() => setIsHovered(true)}
        onMouseLeave={() => setIsHovered(false)}
      />

      {/* Status Bar */}
      <div className="absolute bottom-12 flex flex-col items-center gap-4 z-20 pointer-events-none">
        <div className="flex gap-2">
          {[0, 1, 2].map(i => (
            <div 
              key={i} 
              className="w-1.5 h-1.5 rounded-full bg-indigo-500/40 animate-pulse" 
              style={{ animationDelay: `${i * 0.2}s` }} 
            />
          ))}
        </div>
        <p className="text-white/20 text-[9px] font-mono tracking-[0.4em]">SYNCING NEURAL ENGINE</p>
      </div>

      {/* Controls */}
      <div className="absolute bottom-10 right-10 flex gap-4 z-20">
        <div className="px-5 py-3 rounded-2xl bg-white/5 backdrop-blur-3xl border border-white/10 flex items-center gap-4 shadow-2xl">
            <div className="w-1.5 h-1.5 rounded-full bg-cyan-400 animate-ping" />
            <span className="text-white/40 text-[10px] font-bold tracking-widest">STABLE</span>
        </div>
      </div>
    </div>
  );
};

export default App;