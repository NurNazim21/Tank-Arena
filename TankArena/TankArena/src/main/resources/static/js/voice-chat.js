// voice-chat.js - WebRTC Voice Chat Manager

class VoiceChat {
    constructor() {
        this.peerConnections = new Map();
        this.localStream = null;
        this.enabled = false;
        this.muted = false;
        this.servers = {
            iceServers: [
                { urls: 'stun:stun.l.google.com:19302' },
                { urls: 'stun:stun1.l.google.com:19302' },
                { urls: 'stun:stun2.l.google.com:19302' }
            ]
        };
    }

    async initialize() {
        try {
            console.log('🎤 Requesting microphone access...');

            this.localStream = await navigator.mediaDevices.getUserMedia({
                audio: {
                    echoCancellation: true,
                    noiseSuppression: true,
                    autoGainControl: true,
                    sampleRate: 48000
                },
                video: false
            });

            this.enabled = true;
            console.log('✓ Voice chat initialized');

            // Show voice controls
            document.getElementById('voice-controls').classList.add('active');

            return true;
        } catch (error) {
            console.error('❌ Microphone access denied:', error);
            alert('Voice chat requires microphone access. Please allow microphone permissions and reload.');
            return false;
        }
    }

    async connectToPeer(peerId) {
        if (!this.localStream) {
            console.warn('No local stream available');
            return;
        }

        console.log('📞 Connecting to peer:', peerId);

        const pc = new RTCPeerConnection(this.servers);
        this.peerConnections.set(peerId, pc);

        // Add local audio tracks
        this.localStream.getTracks().forEach(track => {
            pc.addTrack(track, this.localStream);
        });

        // Handle incoming remote tracks
        pc.ontrack = (event) => {
            console.log('🔊 Received remote audio from:', peerId);
            this.playRemoteAudio(peerId, event.streams[0]);
        };

        // Handle ICE candidates
        pc.onicecandidate = (event) => {
            if (event.candidate) {
                this.sendSignal(peerId, {
                    type: 'ice-candidate',
                    candidate: event.candidate.toJSON()
                });
            }
        };

        // Connection state monitoring
        pc.onconnectionstatechange = () => {
            console.log(`Connection with ${peerId}: ${pc.connectionState}`);

            if (pc.connectionState === 'disconnected' ||
                pc.connectionState === 'failed') {
                this.closePeerConnection(peerId);
            }
        };

        // Create and send offer
        try {
            const offer = await pc.createOffer();
            await pc.setLocalDescription(offer);

            this.sendSignal(peerId, {
                type: 'offer',
                offer: offer.toJSON()
            });
        } catch (error) {
            console.error('Error creating offer:', error);
        }
    }

    async handleSignal(peerId, signal) {
        try {
            let pc = this.peerConnections.get(peerId);

            if (signal.type === 'offer') {
                // Create new peer connection if doesn't exist
                if (!pc) {
                    pc = new RTCPeerConnection(this.servers);
                    this.peerConnections.set(peerId, pc);

                    // Add local tracks
                    this.localStream.getTracks().forEach(track => {
                        pc.addTrack(track, this.localStream);
                    });

                    // Handle remote tracks
                    pc.ontrack = (event) => {
                        console.log('🔊 Received remote audio from:', peerId);
                        this.playRemoteAudio(peerId, event.streams[0]);
                    };

                    // Handle ICE candidates
                    pc.onicecandidate = (event) => {
                        if (event.candidate) {
                            this.sendSignal(peerId, {
                                type: 'ice-candidate',
                                candidate: event.candidate.toJSON()
                            });
                        }
                    };

                    pc.onconnectionstatechange = () => {
                        console.log(`Connection with ${peerId}: ${pc.connectionState}`);
                        if (pc.connectionState === 'disconnected' ||
                            pc.connectionState === 'failed') {
                            this.closePeerConnection(peerId);
                        }
                    };
                }

                await pc.setRemoteDescription(new RTCSessionDescription(signal.offer));
                const answer = await pc.createAnswer();
                await pc.setLocalDescription(answer);

                this.sendSignal(peerId, {
                    type: 'answer',
                    answer: answer.toJSON()
                });

            } else if (signal.type === 'answer') {
                if (pc) {
                    await pc.setRemoteDescription(new RTCSessionDescription(signal.answer));
                }
            } else if (signal.type === 'ice-candidate') {
                if (pc) {
                    await pc.addIceCandidate(new RTCIceCandidate(signal.candidate));
                }
            }
        } catch (error) {
            console.error('Error handling signal:', error);
        }
    }

    playRemoteAudio(peerId, stream) {
        // Remove existing audio element if present
        const existingAudio = document.getElementById(`audio-${peerId}`);
        if (existingAudio) {
            existingAudio.remove();
        }

        // Create new audio element
        const audio = document.createElement('audio');
        audio.id = `audio-${peerId}`;
        audio.srcObject = stream;
        audio.autoplay = true;
        audio.volume = 1.0;

        // Hide the audio element
        audio.style.display = 'none';
        document.body.appendChild(audio);

        console.log('✓ Playing audio from peer:', peerId);
    }

    sendSignal(peerId, signal) {
        if (window.stompClient && window.stompClient.connected) {
            window.stompClient.publish({
                destination: '/app/voiceSignal',
                body: JSON.stringify({
                    targetId: peerId,
                    signal: signal
                })
            });
        }
    }

    toggleMute() {
        if (!this.localStream) return false;

        this.muted = !this.muted;
        this.localStream.getAudioTracks().forEach(track => {
            track.enabled = !this.muted;
        });

        // Update button UI
        const muteBtn = document.getElementById('mute-btn');
        if (muteBtn) {
            if (this.muted) {
                muteBtn.textContent = '🔇 Muted';
                muteBtn.classList.add('muted');
            } else {
                muteBtn.textContent = '🎤 Unmuted';
                muteBtn.classList.remove('muted');
            }
        }

        console.log(this.muted ? '🔇 Muted' : '🎤 Unmuted');
        return this.muted;
    }

    closePeerConnection(peerId) {
        const pc = this.peerConnections.get(peerId);
        if (pc) {
            pc.close();
            this.peerConnections.delete(peerId);
        }

        // Remove audio element
        const audio = document.getElementById(`audio-${peerId}`);
        if (audio) {
            audio.remove();
        }

        console.log('🔌 Closed connection with:', peerId);
    }

    disconnect() {
        console.log('📞 Disconnecting voice chat...');

        // Close all peer connections
        this.peerConnections.forEach((pc, peerId) => {
            this.closePeerConnection(peerId);
        });
        this.peerConnections.clear();

        // Stop local stream
        if (this.localStream) {
            this.localStream.getTracks().forEach(track => {
                track.stop();
            });
            this.localStream = null;
        }

        // Remove all audio elements
        document.querySelectorAll('audio[id^="audio-"]').forEach(el => el.remove());

        // Hide voice controls
        document.getElementById('voice-controls').classList.remove('active');

        this.enabled = false;
        this.muted = false;

        console.log('✓ Voice chat disconnected');
    }

    // Connect to all existing players in the room
    connectToExistingPlayers(playerIds) {
        playerIds.forEach(peerId => {
            if (peerId !== window.myId) {
                this.connectToPeer(peerId);
            }
        });
    }
}

// Create global instance
window.voiceChat = new VoiceChat();

// Setup voice control buttons
document.addEventListener('DOMContentLoaded', () => {
    const muteBtn = document.getElementById('mute-btn');
    const leaveBtn = document.getElementById('leave-voice-btn');

    if (muteBtn) {
        muteBtn.addEventListener('click', () => {
            window.voiceChat.toggleMute();
        });
    }

    if (leaveBtn) {
        leaveBtn.addEventListener('click', () => {
            window.voiceChat.disconnect();
        });
    }
});










