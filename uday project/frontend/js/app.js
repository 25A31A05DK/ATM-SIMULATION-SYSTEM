/**
 * ATM SIMULATION SYSTEM - CLIENT CONTROLLER
 * Connects frontend HTML/CSS to Java Backend REST API
 */

const API_BASE = window.location.origin.includes('localhost:8080')
    ? '/api'
    : 'http://localhost:8080/api';

// Realistic Keypad Audio Click using Web Audio API
const audioCtx = (typeof window.AudioContext !== 'undefined' || typeof window.webkitAudioContext !== 'undefined')
    ? new (window.AudioContext || window.webkitAudioContext)()
    : null;

function playKeyBeep(freq = 700) {
    if (!audioCtx) return;
    try {
        if (audioCtx.state === 'suspended') {
            audioCtx.resume();
        }
        const osc = audioCtx.createOscillator();
        const gain = audioCtx.createGain();
        osc.type = 'sine';
        osc.frequency.value = freq;
        gain.gain.setValueAtTime(0.04, audioCtx.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.0001, audioCtx.currentTime + 0.08);
        osc.connect(gain);
        gain.connect(audioCtx.destination);
        osc.start();
        osc.stop(audioCtx.currentTime + 0.08);
    } catch (e) {
        // Audio might be blocked before first interaction
    }
}

// Live Clock in ATM header
function startAtmClock() {
    const clockEl = document.getElementById('live-clock');
    if (!clockEl) return;
    function tick() {
        const now = new Date();
        clockEl.textContent = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
    }
    tick();
    setInterval(tick, 1000);
}

// Session Helpers
const AtmSession = {
    getCard: () => sessionStorage.getItem('atm_card'),
    getPin: () => sessionStorage.getItem('atm_pin'),
    getUserName: () => sessionStorage.getItem('atm_user_name') || 'Valued Customer',
    getBalance: () => sessionStorage.getItem('atm_balance') || '0.00',
    getAccNumber: () => sessionStorage.getItem('atm_acc_number') || 'N/A',
    getAccType: () => sessionStorage.getItem('atm_acc_type') || 'SAVINGS',

    setSession: (card, pin, data) => {
        sessionStorage.setItem('atm_card', card);
        sessionStorage.setItem('atm_pin', pin);
        if (data && data.account) {
            sessionStorage.setItem('atm_user_name', data.account.userName);
            sessionStorage.setItem('atm_balance', data.account.balance.toFixed(2));
            sessionStorage.setItem('atm_acc_number', data.account.accountNumber);
            sessionStorage.setItem('atm_acc_type', data.account.accountType);
        }
    },

    updateBalance: (newBal) => {
        sessionStorage.setItem('atm_balance', parseFloat(newBal).toFixed(2));
    },

    clearSession: () => {
        sessionStorage.clear();
    },

    requireAuth: () => {
        if (!sessionStorage.getItem('atm_card') || !sessionStorage.getItem('atm_pin')) {
            window.location.href = 'index.html';
        }
    }
};

// Alert display helper
function showAlert(message, isError = false) {
    const alertBox = document.getElementById('alert-message');
    if (!alertBox) return;
    alertBox.textContent = message;
    alertBox.className = 'alert-box ' + (isError ? 'alert-error' : 'alert-success');
    alertBox.style.display = 'block';
}

// Keypad Event Handlers
document.addEventListener('DOMContentLoaded', () => {
    startAtmClock();

    // Attach sound and action to hardware keypad buttons
    const keyButtons = document.querySelectorAll('.key-btn');
    keyButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            const val = btn.getAttribute('data-key');
            handleKeypadPress(val);
        });
    });
});

function handleKeypadPress(val) {
    playKeyBeep(val === 'ENTER' ? 900 : (val === 'CANCEL' ? 400 : 700));

    // Look for active input or default focused input
    let targetInput = document.activeElement;
    if (!targetInput || targetInput.tagName !== 'INPUT') {
        targetInput = document.querySelector('.atm-input, input[type="text"], input[type="password"]');
    }

    if (!targetInput) return;

    if (val === 'CLEAR') {
        targetInput.value = '';
        targetInput.dispatchEvent(new Event('input'));
    } else if (val === 'CANCEL') {
        targetInput.value = targetInput.value.slice(0, -1);
        targetInput.dispatchEvent(new Event('input'));
    } else if (val === 'ENTER') {
        // Trigger submit button
        const submitBtn = document.querySelector('.submit-action-btn');
        if (submitBtn) submitBtn.click();
    } else {
        targetInput.value += val;
        targetInput.dispatchEvent(new Event('input'));
    }
}

// Helper: Eject Card / Logout
function ejectCard() {
    AtmSession.clearSession();
    window.location.href = 'index.html';
}

// Quick Sample Card filler
function fillSampleCard(cardNum, pin) {
    const cardInput = document.getElementById('card-input');
    const pinInput = document.getElementById('pin-input');
    if (cardInput) cardInput.value = cardNum;
    if (pinInput) pinInput.value = pin;
}
