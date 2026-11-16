//package com.MiniGame.TankArena;
//
//public class HTML {
//    <!DOCTYPE html>
//<html lang="en">
//<head>
//    <meta charset="UTF-8">
//    <meta name="viewport" content="width=device-width, initial-scale=1.0">
//    <title>Tank Arena</title>
//    <style>
//    body { margin: 0; padding: 0; background: #000; display: flex; justify-content: center; align-items: center; height: 100vh; font-family: Arial, sans-serif; }
//        #game-container { position: relative; }
//        #scoreboard { position: absolute; top: 10px; right: 10px; background: rgba(0,0,0,0.7); color: white; padding: 15px; border-radius: 10px; min-width: 220px; box-shadow: 0 0 10px rgba(0,255,0,0.5); }
//        #scoreboard strong { font-size: 1.2em; color: #0f0; }
//        #scores { list-style: none; padding: 0; margin: 10px 0 0 0; }
//    h1 { position: absolute; top: 10px; left: 10px; color: #0ff; margin: 0; font-size: 2em; text-shadow: 0 0 10px #0ff; }
//        #timer { position: absolute; top: 10px; left: 50%; transform: translateX(-50%); color: #ff0; font-size: 1.8em; background: rgba(0,0,0,0.6); padding: 10px 30px; border-radius: 10px; }
//        #winner-overlay { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); font-size: 5em; color: #ff0; text-shadow: 0 0 30px #f00; display: none; pointer-events: none; z-index: 100; }
//        #name-input { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); background: rgba(0,0,0,0.9); padding: 40px; border-radius: 20px; color: white; text-align: center; z-index: 200; box-shadow: 0 0 20px #0f0; }
//        #name-input input { padding: 15px; font-size: 1.5em; width: 250px; margin: 20px 0; border-radius: 10px; border: none; }
//        #name-input button { padding: 15px 30px; font-size: 1.5em; background: #0f0; color: black; border: none; border-radius: 10px; cursor: pointer; }
//    </style>
//</head>
//<body>
//<div id="game-container">
//    <h1>🛡️ Tank Arena 🛡️</h1>
//    <div id="timer">Time: 3:00</div>
//    <div id="scoreboard">
//        <strong>Scoreboard</strong>
//        <ul id="scores"></ul>
//    </div>
//    <div id="winner-overlay"></div>
//
//    <!-- Name input -->
//    <div id="name-input">
//    <h2>Welcome to Tank Arena!</h2>
//    <p>Enter your name</p>
//        <input type="text" id="player-name" placeholder="Player" maxlength="12">
//        <div style="display: flex; gap: 10px; justify-content: center;">
//            <button id="single-btn" style="background: #0af;">Single Player</button>
//            <button id="multi-btn" style="background: #0f0;">Multiplayer</button>
//        </div>
//    </div>
//
//<!-- Libraries -->
//<script src="https://cdn.jsdelivr.net/npm/phaser@3.90.0/dist/phaser.min.js"></script>
//<script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
//<script src="https://cdn.jsdelivr.net/npm/@stomp/stompjs@7.0.0/bundles/stomp.umd.min.js"></script>
//
//<script>
//    // const config = {
//    //     type: Phaser.AUTO,
//    //     width: 2016,
//    //     height: 1152,
//    //     parent: 'game-container',
//    //     backgroundColor: '#222222',
//    //     physics: { default: 'arcade' },
//    //     scene: { preload, create, update }
//    // };
//
//    const MAP_WIDTH  = 5376;
//    const MAP_HEIGHT = 3072;
//
//    const config = {
//        type: Phaser.AUTO,
//                width: window.innerWidth,
//                height: window.innerHeight,
//                parent: 'game-container',
//                backgroundColor: '#000000',
//                scale: {
//            mode: Phaser.Scale.RESIZE,
//                    autoCenter: Phaser.Scale.CENTER_BOTH
//        },
//        physics: { default: 'arcade' },
//        scene: { preload, create, update }
//    };
//
//    const game = new Phaser.Game(config);
//
//    let playerTank = null;
//    let otherTanks = {};
//    let bullets = {};
//    let obstacles = [];
//    let stompClient = null;
//    let myId = null;
//    let scene = null;
//    let playerName = 'Player';
//    let gameMode = 'MULTI'; // Default mode
//
//    function preload() {
//        scene = this;
//        this.load.image('map', 'assets/map.png');
//        this.load.image('tankBody', 'assets/tank_body.png');
//        this.load.image('tankTurret', 'assets/tank_turret.png');
//        this.load.image('tree', 'assets/tree.png');
//        this.load.image('stone', 'assets/stone.png');
//    }
//
//    function create() {
//        const map = this.add.image(0, 0, 'map').setOrigin(0, 0);
//        this.cameras.main.setBounds(0, 0, MAP_WIDTH, MAP_HEIGHT);
//
//        const cam = this.cameras.main;
//        const zoomX = cam.width  / MAP_WIDTH;
//        const zoomY = cam.height / MAP_HEIGHT;
//        cam.setZoom(Math.min(zoomX, zoomY));
//        cam.centerOn(MAP_WIDTH / 2, MAP_HEIGHT / 2);
//
//        // --- STEP 4 LOGIC STARTS HERE ---
//
//        // Single Player Button Click
//        document.getElementById('single-btn').onclick = () => {
//                gameMode = 'SINGLE';
//        handleStart();
//        };
//
//        // Multiplayer Button Click
//        document.getElementById('multi-btn').onclick = () => {
//                gameMode = 'MULTI';
//        handleStart();
//        };
//
//        function handleStart() {
//            playerName = document.getElementById('player-name').value.trim() || 'Player';
//            document.getElementById('name-input').style.display = 'none';
//            connectWebSocket();
//        }
//
//        this.keys = this.input.keyboard.createCursorKeys();
//        this.keys.w = this.input.keyboard.addKey('W');
//        this.keys.a = this.input.keyboard.addKey('A');
//        this.keys.s = this.input.keyboard.addKey('S');
//        this.keys.d = this.input.keyboard.addKey('D');
//        this.keys.space = this.input.keyboard.addKey('SPACE');
//    }
//
//    // ... (Keep your MAP constants and Phaser config as they are)
//
////     function connectWebSocket() {
////
////         // Replace your old socket line with this:
////         // If it was /ws or something else, change it to:
////         // const socket = new SockJS('/game-websocket');
////         // stompClient = Stomp.over(socket);
////
////
////
////         // 1. Get the current host (works for localhost or your IP)
////         const serverUrl = window.location.origin + '/game-websocket';
////         const socket = new SockJS(serverUrl);
////
//// // 2. Use the modern Client syntax that you know works
////         stompClient = new StompJs.Client({
////             webSocketFactory: () => socket,
////             debug: (str) => { console.log(str); }, // This will show connection logs in Console
////             reconnectDelay: 5000,
////             heartbeatIncoming: 4000,
////             heartbeatOutgoing: 4000
////         });
////
//// // 3. Define what happens when connected
////         stompClient.onConnect = (frame) => {
////             console.log('Connected: ' + frame);
////
////             // Subscribe to the game state
////             stompClient.subscribe('/topic/gameState', (message) => {
////                 const state = JSON.parse(message.body);
////                 updateGameState(state);
////             });
////
////             // Send the join message so the server knows you exist
////             stompClient.publish({
////                 destination: '/app/join',
////                 body: JSON.stringify({ name: 'Player_' + Math.floor(Math.random() * 1000) })
////             });
////         };
////
////         stompClient.onStompError = (frame) => {
////             console.error('Broker reported error: ' + frame.headers['message']);
////             console.error('Additional details: ' + frame.body);
////         };
////
//// // 4. Actually start the connection
////         stompClient.activate();
////
////
////
////
////
////
////         // const socket = new SockJS('http://localhost:8080/game-websocket');
////         // stompClient = new StompJs.Client({ webSocketFactory: () => socket });
////         //
////         // stompClient.onConnect = () => {
////         //     // This is the internal sessionId from the SockJS transport
////         //     const parts = socket._transport.url.split('/');
////         //     myId = parts[parts.length - 2];
////         //
////         //     // LOGIC: If Single Player, the room topic is our unique sessionId.
////         //     // If Multi, it's the shared 'LOBBY' topic.
////         //     const roomId = (gameMode === 'SINGLE') ? myId : 'LOBBY';
////         //     const topic = '/topic/state/' + roomId;
////         //
////         //     console.log(`Connected! Mode: ${gameMode} | Room: ${roomId}`);
////         //
////         //     // Subscribe only to the specific room topic
////         //     stompClient.subscribe(topic, msg => {
////         //         const state = JSON.parse(msg.body);
////         //         updateGameState(state);
////         //     });
////         //
////         //     // Tell the server we are joining
////         //     stompClient.publish({
////         //         destination: '/app/join',
////         //         body: JSON.stringify({
////         //             playerName: playerName,
////         //             mode: gameMode
////         //         })
////         //     });
////         // };
////         //
////         // stompClient.activate();
////     }
//
//
//
//    // for multiplayer
//    function connectWebSocket() {
//        // 1. Dynamic URL detection (Works for Localhost and your IP 192.168.0.103)
//        const serverUrl = window.location.origin + '/game-websocket';
//        const socket = new SockJS(serverUrl);
//
//        // 2. Modern StompJs Client setup
//        stompClient = new StompJs.Client({
//                webSocketFactory: () => socket,
//                debug: (str) => { console.log(str); },
//        reconnectDelay: 5000,
//                heartbeatIncoming: 4000,
//                heartbeatOutgoing: 4000
//        });
//
//        stompClient.onConnect = (frame) => {
//                console.log('Connected to STOMP');
//
//        // 3. GET SESSION ID
//        // We extract the unique ID from the SockJS URL to identify this specific player
//            const parts = socket._transport.url.split('/');
//        myId = parts[parts.length - 2];
//
//        // 4. DETERMINE ROOM ID
//        // Logic: Multiplayer uses 'LOBBY'. Singleplayer uses your unique Session ID.
//        // Make sure the variable 'gameMode' is defined globally (e.g., let gameMode = 'MULTI';)
//            const roomId = (typeof gameMode !== 'undefined' && gameMode === 'SINGLE') ? myId : 'LOBBY';
//
//        // This destination MUST match your GameLoopScheduler (messagingTemplate destination)
//            const topic = '/topic/gameState/' + roomId;
//
//        console.log(`Subscribing to: ${topic}`);
//
//        // 5. SUBSCRIBE
//        stompClient.subscribe(topic, (message) => {
//                const state = JSON.parse(message.body);
//        updateGameState(state); // Now tanks will appear!
//            });
//
//        // 6. JOIN REQUEST
//        // Ensure keys match your Java 'JoinInput' DTO (playerName and mode)
//        stompClient.publish({
//                destination: '/app/join',
//                body: JSON.stringify({
//                playerName: 'Player_' + Math.floor(Math.random() * 1000),
//                mode: (typeof gameMode !== 'undefined') ? gameMode : "MULTI"
//                })
//            });
//        };
//
//        stompClient.onStompError = (frame) => {
//                console.error('Broker error: ' + frame.headers['message']);
//        };
//
//        // 7. ACTIVATE
//        stompClient.activate();
//    }
//
//    // Ensure update() uses the established stompClient
//    function update() {
//        if (!stompClient?.connected) return;
//
//        if (!this.prevKeys) this.prevKeys = { UP: false, DOWN: false, LEFT: false, RIGHT: false };
//
//        const keys = {
//                UP: this.input.keyboard.addKey('W').isDown || this.input.keyboard.addKey('UP').isDown,
//                DOWN: this.input.keyboard.addKey('S').isDown || this.input.keyboard.addKey('DOWN').isDown,
//                LEFT: this.input.keyboard.addKey('A').isDown || this.input.keyboard.addKey('LEFT').isDown,
//                RIGHT: this.input.keyboard.addKey('D').isDown || this.input.keyboard.addKey('RIGHT').isDown
//        };
//
//        ['UP', 'DOWN', 'LEFT', 'RIGHT'].forEach(dir => {
//            const isDown = keys[dir];
//        if (isDown !== this.prevKeys[dir]) {
//                const action = isDown ? dir : 'STOP_' + dir;
//            stompClient.publish({
//                    destination: '/app/move',
//                    body: JSON.stringify({ direction: action })
//                });
//        }
//        });
//
//        this.prevKeys = keys;
//
//        if (Phaser.Input.Keyboard.JustDown(this.input.keyboard.addKey('SPACE'))) {
//            stompClient.publish({ destination: '/app/shoot' });
//        }
//    }
//
//
//    function createTank(scene, x, y, colorHex) {
//    const body = scene.add.image(0, 0, 'tankBody');
//        body.setOrigin(0.5);
//        body.setRotation(Phaser.Math.DegToRad(90));
//        body.setScale(0.2);
//
//        if (colorHex) {
//            body.setTint(parseInt(colorHex.replace('#','0x')));
//        }
//
//    const container = scene.add.container(x, y, [ body ]);
//        container.setDepth(2);
//
//        return { container, body };
//    }
//
//
//
//
//
//
//
//
//
//
//    function updateGameState(state) {
//        // Timer
//        const mins = Math.floor(state.remainingTime / 60).toString().padStart(2, '0');
//        const secs = (state.remainingTime % 60).toString().padStart(2, '0');
//        document.getElementById('timer').textContent = `Time: ${mins}:${secs}`;
//
//        // Winner
//        if (state.winner) {
//            document.getElementById('winner-overlay').textContent = `${state.winner} WINS!`;
//            document.getElementById('winner-overlay').style.display = 'block';
//        }
//
//        // Clear old
//        obstacles.forEach(o => o.destroy());
//        obstacles = [];
//
//// Draw obstacles
//        state.obstacles.forEach(o => {
//    const sprite = scene.add.image(o.x, o.y, o.type);
//
//        sprite.setDisplaySize(o.width, o.height);
//        sprite.setDepth(1); // behind tanks
//
//
//
//        obstacles.push(sprite);
//        });
//
//
//
//
//
//
//
//
//        // Tanks (container rotates = movement + turret)
//        // Tanks (container rotates = movement + turret)
//        state.tanks.forEach(tank => {
//            const isMe = tank.id === myId;
//            const color = parseInt(tank.color.substr(1), 16);
//            const angle = Phaser.Math.DegToRad(tank.angle); // BACKEND angle
//
//        if (isMe) {
//            if (!playerTank) {
//                playerTank = createTank(scene, tank.x, tank.y);
//                scene.cameras.main.startFollow(playerTank.container);
//
//            }
//
//            // 1️⃣ move tank
//            playerTank.container.setPosition(tank.x, tank.y);
//
//
//
//            // 2️⃣ rotate tank (THIS IS WHERE IT GOES)
//            playerTank.container.setRotation(angle);
//
//            // 3️⃣ alive / dead
//            playerTank.container.setAlpha(tank.alive ? 1 : 0.3);
//
//
//        } else {
//            if (!otherTanks[tank.id]) {
//                otherTanks[tank.id] = createTank(scene, tank.x, tank.y, tank.color);
//
//            }
//
//                const t = otherTanks[tank.id];
//
//            // 1️⃣ move tank
//            t.container.setPosition(tank.x, tank.y);
//
//            // 2️⃣ rotate tank (THIS IS WHERE IT GOES)
//            t.container.setRotation(angle);
//
//            // 3️⃣ alive / dead
//            t.container.setAlpha(tank.alive ? 1 : 0.3);
//        }
//        });
//
//
//
//
//
//
//
//        // Clean up disconnected
//        Object.keys(otherTanks).forEach(id => {
//        if (!state.tanks.find(t => t.id === id)) {
//            otherTanks[id].container.destroy();
//
//
//            // otherTanks[id].body.destroy();
//            // otherTanks[id].turret.destroy();
//            delete otherTanks[id];
//        }
//        });
//
//        // Bullets
//        const CAMERA_RANGE = 800;
//
//// Mark all bullets inactive
//// Reset active status for all known bullets
//        Object.values(bullets).forEach(b => b.active = false);
//
//        state.bullets.forEach(b => {
//        if (!bullets[b.id]) {
//            // Create bullet if it doesn't exist
//            bullets[b.id] = scene.add.circle(b.x, b.y, 6, 0xffff00);
//            bullets[b.id].setDepth(3);
//        } else {
//            // Update existing bullet position
//            bullets[b.id].setPosition(b.x, b.y);
//        }
//        bullets[b.id].active = true;
//        });
//
//        // Destroy bullets that the server is no longer tracking
//        Object.keys(bullets).forEach(id => {
//        if (!bullets[id].active) {
//            bullets[id].destroy();
//            delete bullets[id];
//        }
//        });
//
//
//
//        // Explosions on death (simple particle burst)
//        state.tanks.forEach(tank => {
//        if (!tank.alive) {
//            tank.exploded = true;
//                const emitter = scene.add.particles(0, 0, null, {
//                    speed: 100,
//                    lifespan: 600,
//                    quantity: 30,
//                    scale: { start: 1, end: 0 },
//            tint: 0xff8800,
//                    blendMode: 'ADD'
//                });
//            emitter.explode(30, tank.x, tank.y);
//        }
//        });
//
//        // Scoreboard
//        const list = document.getElementById('scores');
//        list.innerHTML = '';
//        Object.entries(state.scoreboard).forEach(([name, score]) => {
//            const li = document.createElement('li');
//            li.textContent = `${name}: ${score}`;
//            list.appendChild(li);
//        });
//
//        // Play Again button after winner
//        if (state.winner && !document.getElementById('play-again-btn')) {
//            const btn = document.createElement('button');
//            btn.id = 'play-again-btn';
//            btn.textContent = 'Play Again';
//            btn.style.position = 'absolute';
//            btn.style.bottom = '80px';
//            btn.style.left = '50%';
//            btn.style.transform = 'translateX(-50%)';
//            btn.style.padding = '20px 40px';
//            btn.style.fontSize = '2em';
//            btn.style.background = '#0f0';
//            btn.style.color = 'black';
//            btn.style.border = 'none';
//            btn.style.borderRadius = '15px';
//            btn.style.cursor = 'pointer';
//            btn.style.zIndex = '200';
//            btn.style.boxShadow = '0 0 20px #0f0';
//            btn.onclick = () => {
//                    stompClient.publish({ destination: '/app/reset' });
//            location.reload();
//};
//            // Restart game
//            document.body.appendChild(btn);
//        }
//    }
//</script>
//</body>
//</html>
//}
