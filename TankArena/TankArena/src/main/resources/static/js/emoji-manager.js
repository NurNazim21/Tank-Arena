// emoji-manager.js - Emoji Reactions Manager

class EmojiManager {
    constructor(scene) {
        this.scene = scene;
        this.emojiWheelOpen = false;
        this.setupEmojiWheel();
    }

    setupEmojiWheel() {
        const wheel = document.getElementById('emoji-wheel');

        // Toggle emoji wheel with E key
        document.addEventListener('keydown', (e) => {
            // FIX: Don't open if chat is focused
            const chatInput = document.getElementById('chat-input');
            if (document.activeElement === chatInput) {
                console.log('Emoji wheel blocked - chat is focused');
                return;
            }

            if (e.key === 'e' || e.key === 'E') {
                e.preventDefault(); // Prevent any default behavior
                this.emojiWheelOpen = !this.emojiWheelOpen;

                if (this.emojiWheelOpen) {
                    wheel.classList.add('show');
                    console.log('✓ Emoji wheel opened');
                } else {
                    wheel.classList.remove('show');
                    console.log('✓ Emoji wheel closed');
                }
            }

            // Close with ESC
            if (e.key === 'Escape' && this.emojiWheelOpen) {
                e.preventDefault();
                this.emojiWheelOpen = false;
                wheel.classList.remove('show');
                console.log('✓ Emoji wheel closed (ESC)');
            }
        });

        // Handle emoji selection
        document.querySelectorAll('.emoji-option').forEach(option => {
            option.addEventListener('click', () => {
                const emoji = option.dataset.emoji;
                console.log('👆 Clicked emoji:', emoji);
                this.sendEmoji(emoji);

                // Close wheel
                this.emojiWheelOpen = false;
                wheel.classList.remove('show');

                // Visual feedback
                option.style.transform = 'scale(1.5)';
                setTimeout(() => {
                    option.style.transform = '';
                }, 200);
            });
        });
    }

    sendEmoji(emoji) {
        if (!window.stompClient?.connected) {
            console.warn('❌ Cannot send emoji - not connected to server');
            return;
        }

        console.log('😀 Sending emoji:', emoji);

        window.stompClient.publish({
            destination: '/app/emoji',
            body: JSON.stringify({ emoji: emoji })
        });

        // Show emoji locally immediately
        this.showEmojiOnPlayer(window.myId, emoji);
    }

    showEmojiOnPlayer(playerId, emoji) {
        console.log(`Attempting to show emoji ${emoji} on player ${playerId}`);

        // FIX: Access tanks from window object
        const tank = playerId === window.myId ? window.playerTank : window.otherTanks[playerId];

        if (!tank) {
            console.warn(`❌ Tank not found for player ${playerId}`);
            console.log('Available tanks:', {
                myId: window.myId,
                playerTank: window.playerTank,
                otherTanks: Object.keys(window.otherTanks)
            });
            return;
        }

        if (!this.scene) {
            console.warn('❌ Scene not available');
            return;
        }

        if (!tank.container) {
            console.warn('❌ Tank container not found');
            return;
        }

        console.log(`✓ Creating emoji at position: x=${tank.container.x}, y=${tank.container.y}`);

        // Create emoji text in Phaser
        const emojiText = this.scene.add.text(
            tank.container.x,
            tank.container.y - 80,
            emoji,
            {
                fontSize: '48px',
                fontFamily: 'Arial'
            }
        );
        emojiText.setOrigin(0.5);
        emojiText.setDepth(1000);

        console.log('✓ Emoji text created:', emojiText);

        // Animate emoji floating up
        this.scene.tweens.add({
            targets: emojiText,
            y: tank.container.y - 160,
            alpha: { from: 1, to: 0 },
            scale: { from: 1, to: 1.5 },
            duration: 2000,
            ease: 'Power2',
            onComplete: () => {
                emojiText.destroy();
                console.log('✓ Emoji animation completed and destroyed');
            }
        });

        // Add bounce effect at start
        this.scene.tweens.add({
            targets: emojiText,
            scale: { from: 0.5, to: 1 },
            duration: 300,
            ease: 'Back.easeOut'
        });

        console.log(`✓ Showing emoji ${emoji} on player ${playerId}`);
    }

    // Show system notification emoji (e.g., player joined, killed, etc.)
    showSystemEmoji(x, y, emoji) {
        if (!this.scene) {
            console.warn('❌ Scene not available for system emoji');
            return;
        }

        const emojiText = this.scene.add.text(x, y, emoji, {
            fontSize: '64px',
            fontFamily: 'Arial'
        });
        emojiText.setOrigin(0.5);
        emojiText.setDepth(1000);

        this.scene.tweens.add({
            targets: emojiText,
            y: y - 100,
            alpha: { from: 1, to: 0 },
            scale: { from: 0, to: 2 },
            duration: 1500,
            ease: 'Power2',
            onComplete: () => {
                emojiText.destroy();
            }
        });

        console.log(`✓ System emoji ${emoji} shown at (${x}, ${y})`);
    }
}

// Global emoji manager (initialized in game create())
window.emojiManager = null;