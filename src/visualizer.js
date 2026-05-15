import * as THREE from 'https://cdn.skypack.dev/three@0.136.0';
import { OrbitControls } from 'https://cdn.skypack.dev/three@0.136.0/examples/jsm/controls/OrbitControls.js';

class SimulationView {
    constructor(containerId) {
        this.scene = new THREE.Scene();
        this.camera = new THREE.PerspectiveCamera(75, window.innerWidth / window.innerHeight, 0.1, 10000);
        this.renderer = new THREE.WebGLRenderer({ antialias: true });
        this.bodies = [];
        this.scale = 1e-11; // Scales meters to manageable scene units

        // Setup Renderer
        this.renderer.setSize(window.innerWidth, window.innerHeight);
        document.getElementById(containerId).appendChild(this.renderer.domElement);

        // Controls
        this.controls = new OrbitControls(this.camera, this.renderer.domElement);
        this.camera.position.set(20, 20, 20);
        
        this.scene.add(new THREE.AmbientLight(0x404040, 2));
        this.animate();
    }

    // Maps Spectral Class to Colors
    getSpectralColor(char) {
        const colors = { 'O': 0x9bb0ff, 'B': 0xaabfff, 'A': 0xcad7ff, 'F': 0xf8f7ff, 
                         'G': 0xfff4ea, 'K': 0xffd2a1, 'M': 0xffcc6f };
        return colors[char] || 0xffffff;
    }

    addBody(name, spectralClass = 'G') {
        const color = this.getSpectralColor(spectralClass);
        const geometry = new THREE.SphereGeometry(0.5, 32, 32);
        const material = new THREE.MeshStandardMaterial({ color: color, emissive: color, emissiveIntensity: 0.5 });
        const mesh = new THREE.Mesh(geometry, material);
        
        // Trail logic
        const trailGeom = new THREE.BufferGeometry();
        const trailMat = new THREE.LineBasicMaterial({ color: color, transparent: true, opacity: 0.5 });
        const trail = new THREE.Line(trailGeom, trailMat);

        const bodyData = { mesh, trail, history: [] };
        this.bodies.push(bodyData);
        this.scene.add(mesh);
        this.scene.add(trail);
        return bodyData;
    }

    parseLine(line) {
        // Splits line by "|" and then by spaces to extract coordinates
        const segments = line.split('|').map(s => s.trim().split(/\s+/).map(Number));
        segments.pop(); // Remove time value at the end
        return segments;
    }

    updatePositions(coordArrays) {
        coordArrays.forEach((coords, i) => {
            if (!this.bodies[i]) this.addBody(`Body ${i}`);
            
            const [x, y, z] = coords.map(c => c * this.scale);
            this.bodies[i].mesh.position.set(x, y, z);
            
            // Update Trail (Max 100 points)
            this.bodies[i].history.push(new THREE.Vector3(x, y, z));
            if (this.bodies[i].history.length > 100) this.bodies[i].history.shift();
            this.bodies[i].trail.geometry.setFromPoints(this.bodies[i].history);
        });
    }

    animate() {
        requestAnimationFrame(() => this.animate());
        this.controls.update();
        this.renderer.render(this.scene, this.camera);
    }
}

export default SimulationView;