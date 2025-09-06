// Loading Screen
document.addEventListener('DOMContentLoaded', function() {
    const loadingScreen = document.getElementById('loadingScreen');
    const loadingProgress = document.getElementById('loadingProgress');
    
    let progress = 0;
    const interval = setInterval(() => {
        progress += Math.random() * 30;
        if (progress > 100) progress = 100;
        loadingProgress.style.width = progress + '%';
        
        if (progress === 100) {
            clearInterval(interval);
            setTimeout(() => {
                loadingScreen.classList.add('hidden');
                initMatrix();
                init3D();
            }, 500);
        }
    }, 100);
});

// Matrix Background
function initMatrix() {
    const canvas = document.getElementById('matrixCanvas');
    const ctx = canvas.getContext('2d');
    
    canvas.width = window.innerWidth;
    canvas.height = window.innerHeight;
    
    const matrix = "ABCDEFGHIJKLMNOPQRSTUVWXYZ123456789@#$%^&*()*&^%+-/~{[|`]}";
    const matrixArray = matrix.split("");
    
    const fontSize = 10;
    const columns = canvas.width / fontSize;
    const drops = Array(Math.floor(columns)).fill(1);
    
    function draw() {
        ctx.fillStyle = 'rgba(10, 10, 11, 0.04)';
        ctx.fillRect(0, 0, canvas.width, canvas.height);
        
        ctx.fillStyle = '#00ff88';
        ctx.font = fontSize + 'px JetBrains Mono';
        
        for (let i = 0; i < drops.length; i++) {
            const text = matrixArray[Math.floor(Math.random() * matrixArray.length)];
            ctx.fillText(text, i * fontSize, drops[i] * fontSize);
            
            if (drops[i] * fontSize > canvas.height && Math.random() > 0.975) {
                drops[i] = 0;
            }
            drops[i]++;
        }
    }
    
    setInterval(draw, 35);
    
    window.addEventListener('resize', () => {
        canvas.width = window.innerWidth;
        canvas.height = window.innerHeight;
    });
}

// 3D Terminal
function init3D() {
    const container = document.getElementById('terminal3d');
    if (!container) return; // Exit if container doesn't exist
    
    const scene = new THREE.Scene();
    const camera = new THREE.PerspectiveCamera(75, container.offsetWidth / container.offsetHeight, 0.1, 1000);
    const renderer = new THREE.WebGLRenderer({ alpha: true });
    
    renderer.setSize(container.offsetWidth, container.offsetHeight);
    renderer.setClearColor(0x000000, 0);
    container.appendChild(renderer.domElement);
    
    // Create wireframe cube
    const geometry = new THREE.BoxGeometry(1, 1, 1);
    const material = new THREE.MeshBasicMaterial({ 
        color: 0x00ff88, 
        wireframe: true 
    });
    const cube = new THREE.Mesh(geometry, material);
    scene.add(cube);
    
    camera.position.z = 2;
    
    function animate() {
        requestAnimationFrame(animate);
        cube.rotation.x += 0.01;
        cube.rotation.y += 0.01;
        renderer.render(scene, camera);
    }
    animate();
}

// Navigation
function showSection(sectionId) {
    // Hide all sections
    const sections = document.querySelectorAll('.content-section');
    sections.forEach(section => {
        section.classList.remove('active');
    });
    
    // Remove active class from nav items
    const navItems = document.querySelectorAll('.nav-item');
    navItems.forEach(item => {
        item.classList.remove('active');
    });
    
    // Show selected section
    document.getElementById(sectionId).classList.add('active');
    
    // Add active class to clicked nav item
    event.target.classList.add('active');
}

// Terminal Controls
function toggleFullscreen() {
    if (!document.fullscreenElement) {
        document.documentElement.requestFullscreen();
    } else {
        document.exitFullscreen();
    }
}

function minimizeTerminal() {
    const terminal = document.querySelector('.terminal');
    terminal.style.transform = terminal.style.transform === 'scale(0.1)' ? 'scale(1)' : 'scale(0.1)';
}

function closeTerminal() {
    if (confirm('Are you sure you want to close the terminal?')) {
        document.body.style.opacity = '0';
        setTimeout(() => {
            alert('Thanks for visiting! Redirecting to LinkedIn...');
            window.open('https://linkedin.com/in/iudishkumar', '_blank');
        }, 1000);
    }
}

// Custom Cursor
document.addEventListener('mousemove', (e) => {
    document.documentElement.style.setProperty('--mouse-x', e.clientX + 'px');
    document.documentElement.style.setProperty('--mouse-y', e.clientY + 'px');
});

// Keyboard shortcuts
document.addEventListener('keydown', (e) => {
    if (e.ctrlKey) {
        switch(e.key) {
            case '1': 
                e.preventDefault();
                document.querySelector('.nav-item[onclick*="about"]').click();
                break;
            case '2': 
                e.preventDefault();
                document.querySelector('.nav-item[onclick*="experience"]').click();
                break;
            case '3': 
                e.preventDefault();
                document.querySelector('.nav-item[onclick*="skills"]').click();
                break;
            case '4': 
                e.preventDefault();
                document.querySelector('.nav-item[onclick*="projects"]').click();
                break;
            case '5': 
                e.preventDefault();
                document.querySelector('.nav-item[onclick*="contact"]').click();
                break;
        }
    }
});

// Easter egg - Konami Code
let konamiCode = [];
const konamiSequence = ['ArrowUp', 'ArrowUp', 'ArrowDown', 'ArrowDown', 'ArrowLeft', 'ArrowRight', 'ArrowLeft', 'ArrowRight', 'KeyB', 'KeyA'];

document.addEventListener('keydown', (e) => {
    konamiCode.push(e.code);
    if (konamiCode.length > konamiSequence.length) {
        konamiCode.shift();
    }
    
    if (konamiCode.join('') === konamiSequence.join('')) {
        document.body.style.animation = 'glitch 0.5s infinite';
        setTimeout(() => {
            document.body.style.animation = '';
            alert('🎮 Developer mode activated! You found the easter egg!');
        }, 2000);
    }
});

// Make functions globally available for onclick handlers
window.showSection = showSection;
window.toggleFullscreen = toggleFullscreen;
window.minimizeTerminal = minimizeTerminal;
window.closeTerminal = closeTerminal;