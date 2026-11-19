const suits = ['♦', '♣', '♥', '♠'];
const values = ['A', '2', '3', '4', '5', '6', '7', '8', '9', '10', 'J', 'Q', 'K'];

let deck = [];
let playerHand = [];
let dealerHand = [];
let balance = 0;
let currentBet = 0;
let isInitialized = false;

const currentBetSpan = document.getElementById('current-bet');
let balanceSpan = document.getElementById('balance');
const dealerCardsDiv = document.getElementById('dealer-cards');
const playerCardsDiv = document.getElementById('player-cards');
const dealerScoreSpan = document.getElementById('dealer-score');
const playerScoreSpan = document.getElementById('player-score');
const messageDiv = document.getElementById('message');

function setInitialBalance(initialBalance) {
    console.log(`Recibiendo saldo inicial: $${initialBalance}`);
    balance = initialBalance;
    isInitialized = true;
    updateBalanceDisplay();
    startGame();
}

function getBalance() {
    console.log(`Devolviendo saldo: $${balance}`);
    return balance;
}

function createDeck() {
    deck = [];
    for (let suit of suits) {
        for (let value of values) {
            deck.push({ suit, value });
        }
    }
    deck.sort(() => Math.random() - 0.5);
}

function getCardValue(card) {
    if (['J', 'Q', 'K'].includes(card.value)) return 10;
    if (card.value === 'A') return 11;
    return parseInt(card.value);
}

function calculateScore(hand) {
    let score = 0;
    let aces = 0;
    for (let card of hand) {
        score += getCardValue(card);
        if (card.value === 'A') aces++;
    }
    while (score > 21 && aces > 0) {
        score -= 10;
        aces--;
    }
    return score;
}

function renderHand(hand, container) {
    if (container) {
        container.innerHTML = '';
        for (let card of hand) {
            const cardDiv = document.createElement('div');
            cardDiv.className = 'card';
            cardDiv.textContent = `${card.value}${card.suit}`;
            container.appendChild(cardDiv);
        }
    }
}

function updateScores() {
    if (playerScoreSpan) playerScoreSpan.textContent = calculateScore(playerHand);
    if (dealerScoreSpan) dealerScoreSpan.textContent = calculateScore(dealerHand);
}

function updateBalanceDisplay() {
    balanceSpan = document.getElementById('balance');
    if (balanceSpan) {
        balanceSpan.textContent = balance.toFixed(2);
    } else {
        console.log("Error: balanceSpan no encontrado");
        if (messageDiv) messageDiv.textContent = "Error: No se pudo mostrar el saldo.";
    }
    if (currentBetSpan) {
        currentBetSpan.textContent = currentBet.toFixed(2);
    }
    if (window.javaBridge) {
        window.javaBridge.updateBalance(balance);
    }
}

function endGame() {
    const hitButton = document.getElementById('hit');
    const standButton = document.getElementById('stand');
    const restartButton = document.getElementById('restart');
    const betButtons = document.querySelectorAll('.bet-button');
    const clearBetButton = document.getElementById('clear-bet');
    if (hitButton) hitButton.disabled = true;
    if (standButton) standButton.disabled = true;
    betButtons.forEach(button => button.disabled = true);
    if (clearBetButton) clearBetButton.disabled = true;
    if (restartButton) {
        restartButton.disabled = false;
        setTimeout(() => {
            if (restartButton) restartButton.disabled = false;
        }, 100);
    } else {
        console.log("Error: Botón 'restart' no encontrado");
        if (messageDiv) messageDiv.textContent = "Error: Botón de reinicio no encontrado.";
    }
}

function checkWinner() {
    if (!isInitialized) {
        console.log("No se puede jugar: saldo no inicializado.");
        if (messageDiv) messageDiv.textContent = "Error: Saldo no inicializado.";
        return;
    }

    const playerScore = calculateScore(playerHand);
    const dealerScore = calculateScore(dealerHand);
    const bet = currentBet;

    if (bet <= 0) {
        if (messageDiv) messageDiv.textContent = 'Apuesta inválida. Debe ser mayor a $0.';
        endGame();
        return;
    }
    if (bet > balance) {
        if (messageDiv) messageDiv.textContent = 'Saldo insuficiente.';
        endGame();
        return;
    }

    let result = '';
    if (playerScore > 21) {
        result = `¡Te pasaste! Pierdes $${bet}.`;
        balance -= bet;
    } else if (dealerScore > 21 || playerScore > dealerScore) {
        result = `¡Ganaste! Ganas $${bet}.`;
        balance += bet;
    } else if (playerScore < dealerScore) {
        result = `Perdiste $${bet}.`;
        balance -= bet;
    } else {
        result = 'Empate. No ganas ni pierdes.';
    }

    if (messageDiv) messageDiv.textContent = result;
    currentBet = 0;
    updateBalanceDisplay();
    endGame();
}

function startGame() {
    if (!isInitialized) {
        console.log("Esperando inicialización del saldo");
        if (messageDiv) messageDiv.textContent = "Esperando saldo inicial...";
        return;
    }
    createDeck();
    playerHand = [deck.pop(), deck.pop()];
    dealerHand = [deck.pop(), deck.pop()];
    renderHand(playerHand, playerCardsDiv);
    renderHand(dealerHand, dealerCardsDiv);
    updateScores();
    if (messageDiv) messageDiv.textContent = currentBet > 0 ? '' : 'Por favor, coloca una apuesta mayor a $0.';
    const hitButton = document.getElementById('hit');
    const standButton = document.getElementById('stand');
    const restartButton = document.getElementById('restart');
    const betButtons = document.querySelectorAll('.bet-button');
    const clearBetButton = document.getElementById('clear-bet');
    if (hitButton) hitButton.disabled = currentBet === 0;
    if (standButton) standButton.disabled = currentBet === 0;
    if (restartButton) restartButton.disabled = true;
    betButtons.forEach(button => button.disabled = false);
    if (clearBetButton) clearBetButton.disabled = false;
    currentBet = 0;
    updateBalanceDisplay();
}

const hitButton = document.getElementById('hit');
if (hitButton) {
    hitButton.addEventListener('click', () => {
        if (!isInitialized) {
            console.log("No se puede jugar: saldo no inicializado.");
            if (messageDiv) messageDiv.textContent = "Error: Saldo no inicializado.";
            return;
        }
        if (currentBet === 0) {
            console.log("No se puede jugar: apuesta no colocada.");
            if (messageDiv) messageDiv.textContent = "Por favor, coloca una apuesta mayor a $0.";
            return;
        }
        const betButtons = document.querySelectorAll('.bet-button');
        const clearBetButton = document.getElementById('clear-bet');
        betButtons.forEach(button => button.disabled = true);
        if (clearBetButton) clearBetButton.disabled = true;
        playerHand.push(deck.pop());
        renderHand(playerHand, playerCardsDiv);
        updateScores();
        if (calculateScore(playerHand) > 21) {
            checkWinner();
        }
    });
}

const standButton = document.getElementById('stand');
if (standButton) {
    standButton.addEventListener('click', () => {
        if (!isInitialized) {
            console.log("No se puede jugar: saldo no inicializado.");
            if (messageDiv) messageDiv.textContent = "Error: Saldo no inicializado.";
            return;
        }
        if (currentBet === 0) {
            console.log("No se puede jugar: apuesta no colocada.");
            if (messageDiv) messageDiv.textContent = "Por favor, coloca una apuesta mayor a $0.";
            return;
        }
        const betButtons = document.querySelectorAll('.bet-button');
        const clearBetButton = document.getElementById('clear-bet');
        betButtons.forEach(button => button.disabled = true);
        if (clearBetButton) clearBetButton.disabled = true;
        while (calculateScore(dealerHand) < 17) {
            dealerHand.push(deck.pop());
            renderHand(dealerHand, dealerCardsDiv);
            updateScores();
        }
        checkWinner();
    });
}

const restartButton = document.getElementById('restart');
if (restartButton) {
    restartButton.addEventListener('click', () => {
        if (!isInitialized) {
            console.log("No se puede reiniciar: saldo no inicializado.");
            if (messageDiv) messageDiv.textContent = "Error: Saldo no inicializado.";
            return;
        }
        playerHand = [];
        dealerHand = [];
        renderHand(playerHand, playerCardsDiv);
        renderHand(dealerHand, dealerCardsDiv);
        updateScores();
        if (messageDiv) messageDiv.textContent = '';
        startGame();
    });
}

const betButtons = document.querySelectorAll('.bet-button');
betButtons.forEach(button => {
    button.addEventListener('click', () => {
        if (!isInitialized) {
            console.log("No se puede apostar: saldo no inicializado.");
            if (messageDiv) messageDiv.textContent = "Error: Saldo no inicializado.";
            return;
        }
        const amount = parseInt(button.getAttribute('data-amount'));
        const newBet = currentBet + amount;
        if (newBet > balance) {
            currentBet = balance;
            if (messageDiv) messageDiv.textContent = `Máximo apuesta: $${balance}.`;
        } else {
            currentBet = newBet;
            if (messageDiv) messageDiv.textContent = '';
        }
        updateBalanceDisplay();
        console.log(`Intento de apuesta: +$${amount}. Apuesta actual: $${currentBet}`);
        // Enable hit and stand buttons once a valid bet is placed
        const hitButton = document.getElementById('hit');
        const standButton = document.getElementById('stand');
        if (hitButton) hitButton.disabled = currentBet === 0;
        if (standButton) standButton.disabled = currentBet === 0;
    });
});

const clearBetButton = document.getElementById('clear-bet');
if (clearBetButton) {
    clearBetButton.addEventListener('click', () => {
        if (!isInitialized) {
            console.log("No se puede limpiar apuesta: saldo no inicializado.");
            if (messageDiv) messageDiv.textContent = "Error: Saldo no inicializado.";
            return;
        }
        currentBet = 0;
        updateBalanceDisplay();
        if (messageDiv) messageDiv.textContent = 'Por favor, coloca una apuesta mayor a $0.';
        // Disable hit and stand buttons when bet is cleared
        const hitButton = document.getElementById('hit');
        const standButton = document.getElementById('stand');
        if (hitButton) hitButton.disabled = true;
        if (standButton) standButton.disabled = true;
        console.log("Apuesta limpiada. Apuesta actual: $0");
    });
}

if (!balanceSpan || !currentBetSpan) {
    console.log("Error: Elementos del DOM no encontrados");
    if (messageDiv) messageDiv.textContent = "Error: Interfaz no cargada correctamente.";
} else {
    console.log("Script cargado, esperando saldo inicial desde JavaFX.");
}