// chat-manager.js - Text Chat with Emoji Support and Sound Notifications

class ChatManager {
    constructor() {
        this.chatOpen = false;
        this.chatVisible = false;
        this.hideTimeout = null;
        this.unreadMessages = 0;
        this.setupChatInput();
        this.setupEmojiPicker();
        this.createNotificationSound();
    }

    createNotificationSound() {
        // Create a simple notification sound using Web Audio API
        this.audioContext = null;
        try {
            this.audioContext = new (window.AudioContext || window.webkitAudioContext)();
        } catch (e) {
            console.warn('Web Audio API not supported');
        }
    }

    showChatBox() {
        const chatBox = document.getElementById('chat-box');
        chatBox.classList.add('visible');
        this.chatVisible = true;
        this.unreadMessages = 0;
        this.hideBadge();

        // Clear hide timeout if exists
        if (this.hideTimeout) {
            clearTimeout(this.hideTimeout);
            this.hideTimeout = null;
        }
    }

    hideChatBox() {
        const chatBox = document.getElementById('chat-box');

        // Don't hide if input is focused
        if (this.chatOpen) return;

        chatBox.classList.remove('visible');
        this.chatVisible = false;
    }

    autoHideChatBox() {
        // Auto-hide chat after 5 seconds if not focused
        if (this.hideTimeout) {
            clearTimeout(this.hideTimeout);
        }

        this.hideTimeout = setTimeout(() => {
            if (!this.chatOpen) {
                this.hideChatBox();
            }
        }, 5000);
    }

    showBadge() {
        const badge = document.getElementById('chat-notification');
        badge.textContent = `${this.unreadMessages} New Message${this.unreadMessages > 1 ? 's' : ''}! Press T`;
        badge.style.display = 'block';
    }

    hideBadge() {
        const badge = document.getElementById('chat-notification');
        badge.style.display = 'none';
    }

    setupChatInput() {
        const input = document.getElementById('chat-input');

        // Press T to open chat - FIX: Only when not already focused
        document.addEventListener('keydown', (e) => {
            // FIX: Don't interfere with game controls if chat input is not focused
            if (document.activeElement !== input) {
                if (e.key === 't' || e.key === 'T') {
                    e.preventDefault();
                    input.focus();
                    this.chatOpen = true;
                    this.showChatBox();
                    input.placeholder = 'Type your message...';
                    return;
                }
            }

            // ESC to close chat
            if (e.key === 'Escape' && this.chatOpen) {
                e.preventDefault();
                input.blur();
                this.chatOpen = false;
                input.placeholder = 'Press T to chat...';
                input.value = '';

                // Close emoji picker if open
                document.getElementById('emoji-picker').classList.remove('show');

                // Auto-hide after ESC
                this.autoHideChatBox();
            }
        });

        // Send message on Enter
        input.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault(); // Prevent any default behavior
                const message = input.value.trim();
                if (message) {
                    this.sendMessage(message);
                    input.value = '';
                }
                input.blur();
                this.chatOpen = false;
                input.placeholder = 'Press T to chat...';

                // Close emoji picker
                document.getElementById('emoji-picker').classList.remove('show');

                // Auto-hide after sending message
                this.autoHideChatBox();
            }
        });

        // FIX: Prevent WASD and arrow keys from propagating to game when typing
        input.addEventListener('keydown', (e) => {
            // Stop propagation for game control keys when typing in chat
            if (['w', 'a', 's', 'd', 'ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight', ' '].includes(e.key.toLowerCase())) {
                e.stopPropagation();
            }
        });

        input.addEventListener('blur', () => {
            setTimeout(() => {
                this.chatOpen = false;
                input.placeholder = 'Press T to chat...';
            }, 100);
        });

        input.addEventListener('focus', () => {
            this.chatOpen = true;
            this.showChatBox();
        });
    }

    setupEmojiPicker() {
        const picker = document.getElementById('emoji-picker');
        const input = document.getElementById('chat-input');
        const toggleBtn = document.getElementById('emoji-toggle');

        // Toggle emoji picker
        toggleBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            picker.classList.toggle('show');
        });

        // Insert emoji into chat input
        document.querySelectorAll('#emoji-picker .emoji-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                const emoji = btn.dataset.emoji;
                input.value += emoji;
                input.focus();

                // Don't close picker, allow multiple selections
                // picker.classList.remove('show');
            });
        });

        // Close picker when clicking outside
        document.addEventListener('click', (e) => {
            if (!picker.contains(e.target) && e.target !== toggleBtn) {
                picker.classList.remove('show');
            }
        });
    }

    sendMessage(message) {
        if (!window.stompClient?.connected) {
            this.addSystemMessage('Not connected to server');
            return;
        }

        console.log('💬 Sending message:', message);

        window.stompClient.publish({
            destination: '/app/chat',
            body: JSON.stringify({
                message: message
            })
        });
    }

    receiveMessage(playerName, message, isSystem = false) {
        const messagesDiv = document.getElementById('chat-messages');

        const messageEl = document.createElement('div');
        messageEl.className = 'chat-message';

        if (isSystem) {
            messageEl.style.borderLeftColor = '#ffaa00';
            messageEl.innerHTML = `
                <span class="player-name" style="color: #ffaa00;">SYSTEM:</span>
                <span class="message-text">${this.escapeHtml(message)}</span>
            `;
        } else {
            messageEl.innerHTML = `
                <span class="player-name">${this.escapeHtml(playerName)}:</span>
                <span class="message-text">${this.escapeHtml(message)}</span>
            `;
        }

        messagesDiv.appendChild(messageEl);

        // Auto-scroll to bottom
        messagesDiv.scrollTop = messagesDiv.scrollHeight;

        // Remove old messages (keep last 50)
        const messages = messagesDiv.querySelectorAll('.chat-message');
        if (messages.length > 50) {
            messages[0].remove();
        }

        // Show chat box temporarily when message received
        this.showChatBox();
        this.autoHideChatBox();

        // Play notification sound
        this.playNotificationSound();

        // Show badge if chat is hidden or will be hidden
        if (!this.chatVisible) {
            this.unreadMessages++;
            this.showBadge();
        }

        console.log('Message displayed:', playerName, message);
    }

    addSystemMessage(message) {
        this.receiveMessage('', message, true);
    }

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    clearChat() {
        const messagesDiv = document.getElementById('chat-messages');
        messagesDiv.innerHTML = '';
    }

    // Notification sound
    playNotificationSound() {
        if (!this.audioContext) return;

        try {
            const oscillator = this.audioContext.createOscillator();
            const gainNode = this.audioContext.createGain();

            oscillator.connect(gainNode);
            gainNode.connect(this.audioContext.destination);

            // Two-tone notification (like a "ding")
            oscillator.frequency.value = 800;
            oscillator.type = 'sine';

            gainNode.gain.setValueAtTime(0.15, this.audioContext.currentTime);
            gainNode.gain.exponentialRampToValueAtTime(0.01, this.audioContext.currentTime + 0.15);

            oscillator.start(this.audioContext.currentTime);
            oscillator.stop(this.audioContext.currentTime + 0.15);

            // Second tone
            setTimeout(() => {
                const osc2 = this.audioContext.createOscillator();
                const gain2 = this.audioContext.createGain();

                osc2.connect(gain2);
                gain2.connect(this.audioContext.destination);

                osc2.frequency.value = 1000;
                osc2.type = 'sine';

                gain2.gain.setValueAtTime(0.12, this.audioContext.currentTime);
                gain2.gain.exponentialRampToValueAtTime(0.01, this.audioContext.currentTime + 0.15);

                osc2.start(this.audioContext.currentTime);
                osc2.stop(this.audioContext.currentTime + 0.15);
            }, 100);
        } catch (error) {
            console.warn('Could not play notification sound:', error);
        }
    }
}

// Global chat manager instance
window.chatManager = null;