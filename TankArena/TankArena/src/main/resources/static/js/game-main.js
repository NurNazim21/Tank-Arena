// game-main.js - Main game logic with voice, chat, and emoji integration

const MAP_WIDTH = 5376;
const MAP_HEIGHT = 2944;

let selectedMap = 'desert';
let playerName = 'Commander';
let gameMode = 'MULTI';
let voiceEnabled = false;
let stompClient = null;
let myId = null;
let scene = null;
let playerTank = null;
let otherTanks = {};
let bullets = {};
let obstacles = [];
let powerUps = {};
let menuMusic = null;
let isMusicPlaying = false;

// Particle emitters
let muzzleFlashEmitter = null;
let explosionEmitter = null;
let dustEmitter = null;

// Make variables global for other modules - FIX: Actually assign them!
window.stompClient = null;
window.myId = null;
window.playerTank = null;
window.otherTanks = otherTanks; // Share the same object reference

// Map theme configurations
const mapThemes = {
    desert: { bg: '#d4a574', tint: 0xffaa00 },
    snow: { bg: '#e8f4f8', tint: 0x88ccff },
    forest: { bg: '#2d4a2b', tint: 0x44aa44 },
    urban: { bg: '#3a3a3a', tint: 0x666666 }
};

// Menu interactions
document.querySelectorAll('.map-card').forEach(card => {
    card.addEventListener('click', () => {
        document.querySelectorAll('.map-card').forEach(c => c.classList.remove('selected'));
        card.classList.add('selected');
        selectedMap = card.dataset.map;
    });
});

document.getElementById('single-btn').onclick = () => {
    gameMode = 'SINGLE';
    startGame();
};

document.getElementById('multi-btn').onclick = () => {
    gameMode = 'MULTI';
    startGame();
};

document.getElementById('music-toggle').onclick = function() {
    if (menuMusic) {
        if (isMusicPlaying) {
            menuMusic.pause();
            this.textContent = '🔇 Music OFF';
            isMusicPlaying = false;
        } else {
            menuMusic.play();
            this.textContent = '🔊 Music ON';
            isMusicPlaying = true;
        }
    }
};

async function startGame() {
    playerName = document.getElementById('player-name').value.trim() || 'Commander';
    voiceEnabled = document.getElementById('enable-voice').checked;

    document.getElementById('home-screen').style.display = 'none';
    document.getElementById('game-container').classList.add('active');
    document.getElementById('loading').style.display = 'block';

    if (menuMusic && isMusicPlaying) {
        menuMusic.pause();
    }

    // Initialize voice chat if enabled
    if (voiceEnabled) {
        const success = await window.voiceChat.initialize();
        if (!success) {
            console.warn('Voice chat initialization failed, continuing without voice');
        }
    }

    setTimeout(() => {
        initGame();
    }, 500);
}

// Phaser config
const config = {
    type: Phaser.AUTO,
    width: window.innerWidth,
    height: window.innerHeight,
    parent: 'game-container',
    backgroundColor: mapThemes[selectedMap].bg,
    scale: {
        mode: Phaser.Scale.RESIZE,
        autoCenter: Phaser.Scale.CENTER_BOTH
    },
    physics: {
        default: 'arcade',
        arcade: { fps: 60 }
    },
    scene: { preload, create, update }
};

let game = null;

function initGame() {
    if (game) game.destroy(true);
    game = new Phaser.Game(config);
}

function preload() {
    scene = this;

    this.load.on('progress', (value) => {
        const fillBar = document.getElementById('loading-bar-fill');
        if (fillBar) {
            fillBar.style.width = (value * 100) + '%';
        }
    });

    this.load.on('loaderror', (file) => {
        console.error('Failed to load:', file.key, file.url);
    });

    this.load.on('complete', () => {
        document.getElementById('loading').style.display = 'none';
    });

    // Load assets
    this.load.audio('menuMusic', '/assets/menu_music.mp3');
    this.load.image('mapBackground', '/assets/map.png');
    this.load.image('snow_bg', '/assets/snow.png');
    this.load.image('forest_bg', '/assets/forest.png');
    this.load.image('urban_bg', '/assets/urban.png');
    this.load.image('tankBody', '/assets/tank_body.png');
    this.load.image('tree', '/assets/tree.png');
    this.load.image('stone', '/assets/stone.png');

    // Create particle textures
    const graphics = this.add.graphics();

    // Muzzle flash particle
    graphics.fillStyle(0xffaa00, 1);
    graphics.fillCircle(8, 8, 8);
    graphics.generateTexture('muzzleParticle', 16, 16);
    graphics.clear();

    // Explosion particle
    graphics.fillStyle(0xff6600, 1);
    graphics.fillCircle(8, 8, 8);
    graphics.generateTexture('explosionParticle', 16, 16);
    graphics.clear();

    // Dust particle
    graphics.fillStyle(0xd4a574, 0.5);
    graphics.fillCircle(4, 4, 4);
    graphics.generateTexture('dustParticle', 8, 8);
    graphics.clear();
}

function create() {
    scene = this;

    // Initialize menu music
    if (!menuMusic) {
        menuMusic = this.sound.add('menuMusic', {
            loop: true,
            volume: 1
        });
        menuMusic.play();
        isMusicPlaying = true;
        document.getElementById('music-toggle').textContent = '🔊 Music ON';
    }

    // Setup background
    const initialTexture = selectedMap === 'desert' ? 'mapBackground' : `${selectedMap}_bg`;
    this.mapSprite = this.add.image(MAP_WIDTH / 2, MAP_HEIGHT / 2, initialTexture);
    this.mapSprite.setDisplaySize(MAP_WIDTH, MAP_HEIGHT);
    this.mapSprite.setDepth(0);

    // Camera setup
    this.cameras.main.setBounds(0, 0, MAP_WIDTH, MAP_HEIGHT);
    const cam = this.cameras.main;
    cam.setBackgroundColor(mapThemes[selectedMap].bg);
    const zoomX = cam.width / MAP_WIDTH;
    const zoomY = cam.height / MAP_HEIGHT;
    cam.setZoom(Math.min(zoomX, zoomY) * 1.2);
    cam.centerOn(MAP_WIDTH / 2, MAP_HEIGHT / 2);

    // Setup particle effects
    setupParticleEffects(this);

    // Input keys
    this.keys = this.input.keyboard.createCursorKeys();
    this.keys.w = this.input.keyboard.addKey('W');
    this.keys.a = this.input.keyboard.addKey('A');
    this.keys.s = this.input.keyboard.addKey('S');
    this.keys.d = this.input.keyboard.addKey('D');
    this.keys.space = this.input.keyboard.addKey('SPACE');

    // Initialize emoji and chat managers
    window.emojiManager = new EmojiManager(scene);
    window.chatManager = new ChatManager();

    // Connect to server
    connectWebSocket();

    // Show controls hint
    setTimeout(() => {
        const hint = document.getElementById('controls-hint');
        if (hint) {
            setTimeout(() => hint.classList.add('hidden'), 5000);
        }
    }, 2000);
}

function setupParticleEffects(scene) {
    // Muzzle flash emitter
    muzzleFlashEmitter = scene.add.particles(0, 0, 'muzzleParticle', {
        speed: { min: 100, max: 200 },
        angle: { min: -20, max: 20 },
        scale: { start: 0.8, end: 0 },
        alpha: { start: 1, end: 0 },
        lifespan: 200,
        frequency: -1,
        tint: [0xffaa00, 0xff6600]
    });
    muzzleFlashEmitter.setDepth(50);

    // Explosion emitter
    explosionEmitter = scene.add.particles(0, 0, 'explosionParticle', {
        speed: { min: 50, max: 200 },
        angle: { min: 0, max: 360 },
        scale: { start: 1, end: 0 },
        alpha: { start: 1, end: 0 },
        lifespan: 600,
        frequency: -1,
        tint: [0xff0000, 0xff6600, 0xffaa00]
    });
    explosionEmitter.setDepth(50);

    // Dust emitter
    dustEmitter = scene.add.particles(0, 0, 'dustParticle', {
        speed: { min: 20, max: 50 },
        angle: { min: 0, max: 360 },
        scale: { start: 0.5, end: 1 },
        alpha: { start: 0.5, end: 0 },
        lifespan: 800,
        frequency: 100
    });
    dustEmitter.setDepth(1);
}

function connectWebSocket() {
    const protocol = window.location.protocol === 'https:' ? 'https' : 'http';
    const serverUrl = protocol + '://' + window.location.host + '/game-websocket';
    const socket = new SockJS(serverUrl);

    stompClient = new StompJs.Client({
        webSocketFactory: () => socket,
        debug: () => {},
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000
    });

    stompClient.onConnect = () => {
        const parts = socket._transport.url.split('/');
        myId = parts[parts.length - 2];
        window.myId = myId;
        window.stompClient = stompClient;

        const roomId = (gameMode === 'SINGLE') ? myId : 'LOBBY';

        // Subscribe to game state
        stompClient.subscribe('/topic/gameState/' + roomId, (message) => {
            const state = JSON.parse(message.body);
            updateGameState(state);
        });

        // Subscribe to emoji events
        stompClient.subscribe('/topic/emoji/' + roomId, (message) => {
            const data = JSON.parse(message.body);
            window.emojiManager.showEmojiOnPlayer(data.playerId, data.emoji);
        });

        // Subscribe to chat messages
        stompClient.subscribe('/topic/chat/' + roomId, (message) => {
            const data = JSON.parse(message.body);
            window.chatManager.receiveMessage(data.playerName, data.message);
        });

        // Subscribe to voice signals
        stompClient.subscribe('/user/queue/voiceSignal', (message) => {
            const data = JSON.parse(message.body);
            window.voiceChat.handleSignal(data.senderId, data.signal);
        });

        // Subscribe to player join events
        stompClient.subscribe('/topic/playerJoined/' + roomId, (message) => {
            const data = JSON.parse(message.body);
            if (data.playerId !== myId && window.voiceChat.enabled) {
                window.voiceChat.connectToPeer(data.playerId);
            }

            // Show system message
            window.chatManager.addSystemMessage(`${data.playerName} joined the game`);
        });

        // Join the game
        stompClient.publish({
            destination: '/app/join',
            body: JSON.stringify({
                playerName: playerName,
                mode: gameMode,
                theme: selectedMap
            })
        });

        window.chatManager.addSystemMessage('Connected to game server');
    };

    stompClient.activate();
}

function update() {
    if (!stompClient?.connected) return;

    if (!this.prevKeys) this.prevKeys = { UP: false, DOWN: false, LEFT: false, RIGHT: false };

    const keys = {
        UP: this.input.keyboard.addKey('W').isDown || this.input.keyboard.addKey('UP').isDown,
        DOWN: this.input.keyboard.addKey('S').isDown || this.input.keyboard.addKey('DOWN').isDown,
        LEFT: this.input.keyboard.addKey('A').isDown || this.input.keyboard.addKey('LEFT').isDown,
        RIGHT: this.input.keyboard.addKey('D').isDown || this.input.keyboard.addKey('RIGHT').isDown
    };

    ['UP', 'DOWN', 'LEFT', 'RIGHT'].forEach(dir => {
        const isDown = keys[dir];
        if (isDown !== this.prevKeys[dir]) {
            const action = isDown ? dir : 'STOP_' + dir;
            stompClient.publish({
                destination: '/app/move',
                body: JSON.stringify({ direction: action })
            });
        }
    });

    this.prevKeys = keys;

    // Add dust particles when moving
    if (playerTank && (keys.UP || keys.DOWN || keys.LEFT || keys.RIGHT)) {
        if (Math.random() < 0.3) {
            dustEmitter.setPosition(playerTank.container.x, playerTank.container.y);
            dustEmitter.explode(2);
        }
    }

    // Shoot on SPACE
    if (Phaser.Input.Keyboard.JustDown(this.input.keyboard.addKey('SPACE'))) {
        stompClient.publish({ destination: '/app/shoot' });

        // Client-side shoot effect
        if (playerTank) {
            const angle = playerTank.container.rotation;
            const offsetX = Math.cos(angle) * 50;
            const offsetY = Math.sin(angle) * 50;

            muzzleFlashEmitter.setPosition(
                playerTank.container.x + offsetX,
                playerTank.container.y + offsetY
            );
            muzzleFlashEmitter.setAngle(Phaser.Math.RadToDeg(angle));
            muzzleFlashEmitter.explode(10);

            // Camera shake
            scene.cameras.main.shake(100, 0.003);
        }
    }
}

function updateGameState(state) {
    // Update theme
    if (state.theme && mapThemes[state.theme]) {
        const themeConfig = mapThemes[state.theme];
        scene.cameras.main.setBackgroundColor(themeConfig.bg);

        if (scene.mapSprite) {
            const textureKey = state.theme === 'desert' ? 'mapBackground' : `${state.theme}_bg`;
            if (scene.textures.exists(textureKey)) {
                scene.mapSprite.setTexture(textureKey);
            }
        }
    }

    // Update timer
    const mins = Math.floor(state.remainingTime / 60).toString().padStart(2, '0');
    const secs = (state.remainingTime % 60).toString().padStart(2, '0');
    document.getElementById('timer').textContent = `Time: ${mins}:${secs}`;

    // Winner display
    if (state.winner) {
        document.getElementById('winner-overlay').textContent = `🏆 ${state.winner} WINS! 🏆`;
        document.getElementById('winner-overlay').style.display = 'block';

        if (!document.getElementById('play-again-btn')) {
            const btn = document.createElement('button');
            btn.id = 'play-again-btn';
            btn.textContent = '🔄 Play Again';
            btn.onclick = () => {
                stompClient.publish({ destination: '/app/reset' });
                location.reload();
            };
            document.body.appendChild(btn);
        }
    }

    // Update obstacles
    obstacles.forEach(o => o.destroy());
    obstacles = [];

    if (state.obstacles) {
        state.obstacles.forEach(o => {
            const sprite = scene.add.image(o.x, o.y, o.type);
            sprite.setDisplaySize(o.width, o.height);
            sprite.setDepth(1);
            obstacles.push(sprite);
        });
    }

    // Update power-ups
    Object.values(powerUps).forEach(p => p.active = false);

    if (state.powerUps) {
        state.powerUps.forEach(p => {
            if (!powerUps[p.id]) {
                const color = p.type === 'HEALTH' ? 0xff0000 : 0x00ffff;
                const circle = scene.add.circle(p.x, p.y, 20, color);
                circle.setDepth(1.5);

                scene.tweens.add({
                    targets: circle,
                    scale: { from: 1, to: 1.3 },
                    alpha: { from: 1, to: 0.7 },
                    duration: 800,
                    yoyo: true,
                    repeat: -1
                });

                const icon = scene.add.text(p.x, p.y, p.type === 'HEALTH' ? '❤️' : '⚡', {
                    fontSize: '20px',
                    align: 'center'
                });
                icon.setOrigin(0.5);
                icon.setDepth(1.6);

                powerUps[p.id] = { circle, icon };
            } else {
                powerUps[p.id].circle.setPosition(p.x, p.y);
                powerUps[p.id].icon.setPosition(p.x, p.y);
            }
            powerUps[p.id].active = true;
        });
    }

    Object.keys(powerUps).forEach(id => {
        if (!powerUps[id].active) {
            powerUps[id].circle.destroy();
            powerUps[id].icon.destroy();
            delete powerUps[id];
        }
    });

    // Update tanks
    state.tanks.forEach(tank => {
        const isMe = tank.id === myId;
        const angle = Phaser.Math.DegToRad(tank.angle);

        if (isMe) {
            if (!playerTank) {
                playerTank = createTank(scene, tank.x, tank.y);
                window.playerTank = playerTank; // FIX: Update global reference
                scene.cameras.main.startFollow(playerTank.container, true, 0.1, 0.1);
                playerTank.wasAlive = tank.alive;
            }

            // Check for death
            if (playerTank.wasAlive && !tank.alive) {
                createExplosion(tank.x, tank.y, true);
            }
            playerTank.wasAlive = tank.alive;

            playerTank.container.setPosition(tank.x, tank.y);
            playerTank.container.setRotation(angle);
            playerTank.container.setAlpha(tank.alive ? 1 : 0.3);

            const healthPercent = tank.health / 100;
            playerTank.hpBar.setScale(healthPercent, 1);

            // Health-based color
            if (healthPercent > 0.6) {
                playerTank.hpBar.setFillStyle(0x00ff00);
            } else if (healthPercent > 0.3) {
                playerTank.hpBar.setFillStyle(0xffaa00);
            } else {
                playerTank.hpBar.setFillStyle(0xff0000);
            }

            // Color tint based on health
            if (healthPercent < 0.3) {
                playerTank.body.setTint(0xff6666);
            } else if (healthPercent < 0.6) {
                playerTank.body.setTint(0xffaa66);
            } else {
                playerTank.body.clearTint();
            }
        } else {
            if (!otherTanks[tank.id]) {
                otherTanks[tank.id] = createTank(scene, tank.x, tank.y, tank.color);
                otherTanks[tank.id].wasAlive = tank.alive;
            }

            const t = otherTanks[tank.id];

            // Check for death
            if (t.wasAlive && !tank.alive) {
                createExplosion(tank.x, tank.y, false);
            }
            t.wasAlive = tank.alive;

            t.container.setPosition(tank.x, tank.y);
            t.container.setRotation(angle);
            t.container.setAlpha(tank.alive ? 1 : 0.3);

            const healthPercent = tank.health / 100;
            t.hpBar.setScale(healthPercent, 1);
            t.hpBar.setFillStyle(healthPercent > 0.5 ? 0x00ff00 : healthPercent > 0.25 ? 0xffaa00 : 0xff0000);
        }
    });

    // Remove disconnected tanks
    Object.keys(otherTanks).forEach(id => {
        if (!state.tanks.find(t => t.id === id)) {
            otherTanks[id].container.destroy();
            delete otherTanks[id];
        }
    });

    // Update bullets
    Object.values(bullets).forEach(b => b.active = false);

    state.bullets.forEach(b => {
        if (!bullets[b.id]) {
            bullets[b.id] = scene.add.circle(b.x, b.y, 6, 0xffff00);
            bullets[b.id].setDepth(3);
        } else {
            bullets[b.id].setPosition(b.x, b.y);
        }
        bullets[b.id].active = true;
    });

    Object.keys(bullets).forEach(id => {
        if (!bullets[id].active) {
            // Create small explosion when bullet disappears
            const pos = bullets[id];
            if (pos) {
                explosionEmitter.setPosition(pos.x, pos.y);
                explosionEmitter.explode(8);
            }
            bullets[id].destroy();
            delete bullets[id];
        }
    });

    // Update scoreboard
    const list = document.getElementById('scores');
    list.innerHTML = '';
    Object.entries(state.scoreboard).forEach(([name, score]) => {
        const li = document.createElement('li');
        li.textContent = `${name}: ${score}`;
        list.appendChild(li);
    });
}

function createTank(scene, x, y, colorHex) {
    const body = scene.add.image(0, 0, 'tankBody');
    body.setOrigin(0.5);
    body.setRotation(Phaser.Math.DegToRad(90));
    body.setScale(0.4);

    if (colorHex) {
        body.setTint(parseInt(colorHex.replace('#', '0x')));
    }

    const hpBg = scene.add.rectangle(0, -60, 80, 10, 0x000000);
    hpBg.setOrigin(0.5);

    const hpBar = scene.add.rectangle(0, -60, 80, 10, 0x00ff00);
    hpBar.setOrigin(0.5);

    const hpBorder = scene.add.graphics();
    hpBorder.lineStyle(2, 0xffffff, 0.5);
    hpBorder.strokeRect(-40, -65, 80, 10);

    const container = scene.add.container(x, y, [body, hpBg, hpBar, hpBorder]);
    container.setDepth(2);

    return { container, body, hpBar };
}

function createExplosion(x, y, withShake) {
    explosionEmitter.setPosition(x, y);
    explosionEmitter.explode(25);

    if (withShake) {
        scene.cameras.main.shake(200, 0.008);
        scene.cameras.main.flash(100, 255, 100, 0, true);
    }
}
