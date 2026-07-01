import { useEffect, useRef } from "react";

declare global {
  interface Window { THREE?: any; }
}

const THREE_CDN = "https://cdnjs.cloudflare.com/ajax/libs/three.js/r125/three.min.js";

function loadThree(): Promise<any> {
  if (window.THREE) return Promise.resolve(window.THREE);
  return new Promise((resolve, reject) => {
    const ex = document.querySelector(`script[src="${THREE_CDN}"]`);
    if (ex) { ex.addEventListener("load", () => resolve(window.THREE)); return; }
    const s = document.createElement("script");
    s.src = THREE_CDN;
    s.onload = () => resolve(window.THREE);
    s.onerror = () => reject(new Error("three load failed"));
    document.head.appendChild(s);
  });
}

const MODE_COLOR: Record<string, number> = {
  default: 0x00e5ff, // cyan
  trading: 0xffb300, // gold/amber
  research: 0x9b5cff, // purple
};

/**
 * The living neural network sphere. Each particle represents a stored memory — the count comes from
 * real /api/system data, not a random number. Faint connection lines between near neighbours, and
 * orbital rings at different speeds. Reacts to the mouse and to `mode` (colour) + `thinking`
 * (faster swirl, brighter). Raw Three.js via CDN — no npm 3D deps.
 */
export default function NeuralSphere({
  mode = "default",
  thinking = false,
  memoryCount = 100,
}: {
  mode?: string;
  thinking?: boolean;
  memoryCount?: number;
}) {
  const ref = useRef<HTMLDivElement>(null);
  const modeRef = useRef(mode);
  const thinkRef = useRef(thinking);
  const memCountRef = useRef(memoryCount);
  modeRef.current = mode;
  thinkRef.current = thinking;
  memCountRef.current = memoryCount;

  useEffect(() => {
    let raf = 0;
    let renderer: any;
    let disposed = false;
    const el = ref.current;
    if (!el) return;

    loadThree().then((THREE) => {
      if (disposed || !THREE) return;
      const W = () => el.clientWidth || window.innerWidth;
      const H = () => el.clientHeight || window.innerHeight;

      const scene = new THREE.Scene();
      const camera = new THREE.PerspectiveCamera(70, W() / H(), 0.1, 100);
      camera.position.z = 9;
      renderer = new THREE.WebGLRenderer({ alpha: true, antialias: true });
      renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
      renderer.setSize(W(), H());
      el.appendChild(renderer.domElement);

      const group = new THREE.Group();
      scene.add(group);

      // --- particles on a sphere: each particle = one memory ---
      const N = Math.max(memoryCount, 50); // minimum 50 for visual baseline
      const radius = 3.2;
      const positions = new Float32Array(N * 3);
      const original = new Float32Array(N * 3);
      for (let i = 0; i < N; i++) {
        const phi = Math.acos(-1 + (2 * i) / N);
        const theta = Math.sqrt(N * Math.PI) * phi;
        const x = radius * Math.cos(theta) * Math.sin(phi);
        const y = radius * Math.sin(theta) * Math.sin(phi);
        const z = radius * Math.cos(phi);
        positions[i * 3] = x; positions[i * 3 + 1] = y; positions[i * 3 + 2] = z;
        original[i * 3] = x; original[i * 3 + 1] = y; original[i * 3 + 2] = z;
      }
      const pGeo = new THREE.BufferGeometry();
      pGeo.setAttribute("position", new THREE.BufferAttribute(positions, 3));
      const pMat = new THREE.PointsMaterial({
        color: MODE_COLOR[mode] ?? MODE_COLOR.default,
        size: 0.06, transparent: true, opacity: 0.95, blending: THREE.AdditiveBlending,
      });
      const points = new THREE.Points(pGeo, pMat);
      group.add(points);

      // --- connections: each particle to nearest 2 neighbours (computed once) ---
      const pairs: number[] = [];
      for (let i = 0; i < N; i++) {
        const dists: { j: number; d: number }[] = [];
        for (let j = 0; j < N; j++) {
          if (i === j) continue;
          const dx = original[i * 3] - original[j * 3];
          const dy = original[i * 3 + 1] - original[j * 3 + 1];
          const dz = original[i * 3 + 2] - original[j * 3 + 2];
          dists.push({ j, d: dx * dx + dy * dy + dz * dz });
        }
        dists.sort((a, b) => a.d - b.d);
        for (let k = 0; k < 2; k++) if (i < dists[k].j) pairs.push(i, dists[k].j);
      }
      const linePos = new Float32Array(pairs.length * 3);
      const lGeo = new THREE.BufferGeometry();
      lGeo.setAttribute("position", new THREE.BufferAttribute(linePos, 3));
      const lMat = new THREE.LineBasicMaterial({
        color: MODE_COLOR[mode] ?? MODE_COLOR.default,
        transparent: true, opacity: 0.12, blending: THREE.AdditiveBlending,
      });
      const lines = new THREE.LineSegments(lGeo, lMat);
      group.add(lines);

      // --- orbital rings (partial arcs, varied speeds/tilts) ---
      const rings: any[] = [];
      for (let i = 0; i < 4; i++) {
        const g = new THREE.TorusGeometry(4 + i * 0.5, 0.012, 8, 120, Math.PI * (1 + Math.random()));
        const m = new THREE.MeshBasicMaterial({
          color: MODE_COLOR[mode] ?? MODE_COLOR.default,
          transparent: true, opacity: 0.35, blending: THREE.AdditiveBlending,
        });
        const ring = new THREE.Mesh(g, m);
        ring.rotation.x = Math.random() * Math.PI;
        ring.rotation.y = Math.random() * Math.PI;
        group.add(ring);
        rings.push(ring);
      }

      let mx = 0, my = 0;
      const onMove = (e: MouseEvent) => {
        mx = (e.clientX / window.innerWidth - 0.5);
        my = (e.clientY / window.innerHeight - 0.5);
      };
      window.addEventListener("mousemove", onMove);

      const tmpColor = new THREE.Color();
      function animate(t: number) {
        raf = requestAnimationFrame(animate);
        const thinking = thinkRef.current;
        const target = MODE_COLOR[modeRef.current] ?? MODE_COLOR.default;
        // smooth colour shift
        tmpColor.setHex(target);
        pMat.color.lerp(tmpColor, 0.05);
        lMat.color.lerp(tmpColor, 0.05);
        rings.forEach((r) => r.material.color.lerp(tmpColor, 0.05));
        pMat.opacity = thinking ? 1.0 : 0.9;
        lMat.opacity = thinking ? 0.22 : 0.12;

        const time = t * (thinking ? 0.0016 : 0.0008);
        group.rotation.y += (thinking ? 0.004 : 0.0018) + mx * 0.02;
        group.rotation.x += 0.0006 + my * 0.015;

        const arr = pGeo.attributes.position.array as Float32Array;
        const amp = thinking ? 0.18 : 0.09;
        for (let i = 0; i < N; i++) {
          const ix = i * 3;
          const ox = original[ix], oy = original[ix + 1], oz = original[ix + 2];
          const noise = Math.sin(ox * 1.5 + time * 3) * Math.cos(oy * 1.5 + time * 3) * Math.sin(oz * 1.5 + time * 3);
          const off = 1 + noise * amp + Math.sin(time * 4) * 0.04;
          arr[ix] = ox * off; arr[ix + 1] = oy * off; arr[ix + 2] = oz * off;
        }
        pGeo.attributes.position.needsUpdate = true;

        const lp = lGeo.attributes.position.array as Float32Array;
        for (let p = 0; p < pairs.length; p += 2) {
          const a = pairs[p] * 3, b = pairs[p + 1] * 3, o = (p / 2) * 6;
          lp[o] = arr[a]; lp[o + 1] = arr[a + 1]; lp[o + 2] = arr[a + 2];
          lp[o + 3] = arr[b]; lp[o + 4] = arr[b + 1]; lp[o + 5] = arr[b + 2];
        }
        lGeo.attributes.position.needsUpdate = true;

        rings.forEach((r, i) => { r.rotation.z += 0.004 * (i + 1) * (thinking ? 2 : 1); });
        renderer.render(scene, camera);
      }
      animate(0);

      const onResize = () => { renderer.setSize(W(), H()); camera.aspect = W() / H(); camera.updateProjectionMatrix(); };
      window.addEventListener("resize", onResize);
      (el as any)._cleanup = () => { window.removeEventListener("mousemove", onMove); window.removeEventListener("resize", onResize); };
    });

    return () => {
      disposed = true;
      cancelAnimationFrame(raf);
      if (el) {
        (el as any)._cleanup?.();
        if (renderer?.domElement && el.contains(renderer.domElement)) el.removeChild(renderer.domElement);
        renderer?.dispose?.();
      }
    };
  }, []);

  return <div ref={ref} className="absolute inset-0 z-0 pointer-events-none" />;
}
