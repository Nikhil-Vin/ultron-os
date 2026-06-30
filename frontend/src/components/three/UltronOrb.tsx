import { useEffect, useRef } from "react";

/**
 * UltronOrb — the GOLD CORE particle nucleus. Ports the reference Three.js gold particle sphere +
 * luxury rings, loaded from CDN at runtime so the Vite build needs zero extra dependencies. Reacts
 * to voice state: pulses faster/brighter when listening/speaking.
 *
 * Production note: swap the CDN loader for `@react-three/fiber` + `drei` (per the Phase 5 spec)
 * once those deps are installed — the visual contract (gold sphere, rings, pulse) stays the same.
 */
declare global {
  interface Window { THREE?: any; }
}

const THREE_CDN = "https://cdnjs.cloudflare.com/ajax/libs/three.js/r125/three.min.js";

function loadThree(): Promise<any> {
  if (window.THREE) return Promise.resolve(window.THREE);
  return new Promise((resolve, reject) => {
    const existing = document.querySelector(`script[src="${THREE_CDN}"]`);
    if (existing) {
      existing.addEventListener("load", () => resolve(window.THREE));
      return;
    }
    const s = document.createElement("script");
    s.src = THREE_CDN;
    s.onload = () => resolve(window.THREE);
    s.onerror = () => reject(new Error("failed to load three.js"));
    document.head.appendChild(s);
  });
}

export default function UltronOrb({ voiceState = "idle" }: { voiceState?: string }) {
  const ref = useRef<HTMLDivElement>(null);
  const stateRef = useRef(voiceState);
  stateRef.current = voiceState;

  useEffect(() => {
    let raf = 0;
    let renderer: any;
    let disposed = false;
    const container = ref.current;
    if (!container) return;

    loadThree().then((THREE) => {
      if (disposed || !THREE) return;
      const width = container.clientWidth || 600;
      const height = container.clientHeight || 600;
      const scene = new THREE.Scene();
      const camera = new THREE.PerspectiveCamera(75, width / height, 0.1, 1000);
      renderer = new THREE.WebGLRenderer({ alpha: true, antialias: true });
      renderer.setPixelRatio(window.devicePixelRatio);
      renderer.setSize(width, height);
      container.appendChild(renderer.domElement);

      const group = new THREE.Group();
      scene.add(group);
      const GOLD = 0xd4af37;
      const WHITE = 0xffffff;

      const count = 5000;
      const positions = new Float32Array(count * 3);
      const original = new Float32Array(count * 3);
      const geometry = new THREE.BufferGeometry();
      const radius = 2.5;
      for (let i = 0; i < count; i++) {
        const phi = Math.acos(-1 + (2 * i) / count);
        const theta = Math.sqrt(count * Math.PI) * phi;
        const x = radius * Math.cos(theta) * Math.sin(phi);
        const y = radius * Math.sin(theta) * Math.sin(phi);
        const z = radius * Math.cos(phi);
        positions[i * 3] = x; positions[i * 3 + 1] = y; positions[i * 3 + 2] = z;
        original[i * 3] = x; original[i * 3 + 1] = y; original[i * 3 + 2] = z;
      }
      geometry.setAttribute("position", new THREE.BufferAttribute(positions, 3));
      const material = new THREE.PointsMaterial({
        color: GOLD, size: 0.035, transparent: true, opacity: 0.9, blending: THREE.AdditiveBlending,
      });
      group.add(new THREE.Points(geometry, material));

      const rings: any[] = [];
      for (let i = 0; i < 4; i++) {
        const ringGeo = new THREE.TorusGeometry(3.0 + i * 0.2, 0.01, 16, 100);
        const ringMat = new THREE.MeshBasicMaterial({
          color: i % 2 === 0 ? GOLD : WHITE, transparent: true, opacity: 0.3, blending: THREE.AdditiveBlending,
        });
        const ring = new THREE.Mesh(ringGeo, ringMat);
        ring.rotation.x = Math.random() * Math.PI;
        ring.rotation.y = Math.random() * Math.PI;
        group.add(ring);
        rings.push(ring);
      }

      scene.add(new THREE.PointLight(GOLD, 8, 15));
      scene.add(new THREE.AmbientLight(0x222222));
      camera.position.z = 8;

      const posAttr = geometry.attributes.position;
      function animate(t: number) {
        raf = requestAnimationFrame(animate);
        const active = stateRef.current !== "idle";
        const speaking = stateRef.current === "speaking" || stateRef.current === "listening";
        const time = t * (speaking ? 0.0035 : 0.002);
        group.rotation.y += active ? 0.006 : 0.003;
        group.rotation.x += 0.001;
        const amp = speaking ? 0.32 : 0.2;
        for (let i = 0; i < count; i++) {
          const ix = i * 3;
          const ox = original[ix], oy = original[ix + 1], oz = original[ix + 2];
          const noise = Math.sin(ox * 1.2 + time) * Math.cos(oy * 1.2 + time) * Math.sin(oz * 1.2 + time);
          const pulse = Math.sin(time * 1.5) * 0.15;
          const offset = 1 + noise * amp + pulse;
          posAttr.array[ix] = ox * offset;
          posAttr.array[ix + 1] = oy * offset;
          posAttr.array[ix + 2] = oz * offset;
        }
        posAttr.needsUpdate = true;
        rings.forEach((ring, i) => {
          ring.rotation.z += 0.005 * (i + 1);
          const rp = 1 + Math.sin(time * 2 + i) * 0.05;
          ring.scale.set(rp, rp, rp);
        });
        renderer.render(scene, camera);
      }
      animate(0);

      const onResize = () => {
        const w = container.clientWidth || 600;
        const h = container.clientHeight || 600;
        renderer.setSize(w, h);
        camera.aspect = w / h;
        camera.updateProjectionMatrix();
      };
      window.addEventListener("resize", onResize);
      (container as any)._cleanup = () => window.removeEventListener("resize", onResize);
    });

    return () => {
      disposed = true;
      cancelAnimationFrame(raf);
      if (container) {
        (container as any)._cleanup?.();
        if (renderer?.domElement && container.contains(renderer.domElement)) {
          container.removeChild(renderer.domElement);
        }
        renderer?.dispose?.();
      }
    };
  }, []);

  return <div ref={ref} className="h-full w-full" style={{ filter: "drop-shadow(0 0 30px rgba(212,175,55,0.2))" }} />;
}
