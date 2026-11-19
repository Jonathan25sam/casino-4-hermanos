const SUITS = ['hearts', 'diamonds', 'clubs', 'spades'];
const VALUES = ['2', '3', '4', '5', '6', '7', '8', '9', '10', 'J', 'Q', 'K', 'A'];
const SYMBOLS = {
    'hearts': '♥',
    'diamonds': '♦',
    'clubs': '♣',
    'spades': '♠'
};

let deck = [];
let playerCards = [];
let dealerCards = [];
let communityCards = [];
let playerBalance = 0;
let currentBet = 0;
let currentPot = 0;
let gameStage = 'pre-deal';
let gameHistory = [];

const balanceDisplay    = document.getElementById('balance');
const playerCardsEl     = document.getElementById('playerCards');
const dealerCardsEl     = document.getElementById('dealerCards');
const communityCardsEl  = document.getElementById('communityCards');
const betAmountEl       = document.getElementById('betAmount');
const decreaseBetBtn    = document.getElementById('decreaseBet');
const increaseBetBtn    = document.getElementById('increaseBet');
const dealBtn           = document.getElementById('dealBtn');
const foldBtn           = document.getElementById('foldBtn');
const callBtn           = document.getElementById('callBtn');
const raiseBtn          = document.getElementById('raiseBtn');
const gameStatusEl      = document.getElementById('gameStatus');
const handResultEl      = document.getElementById('handResult');
const historyListEl     = document.getElementById('historyList');

// Nueva función para establecer saldo inicial mejorada
function setInitialBalance(balance) {
    playerBalance = parseFloat(balance) || 0;
    console.log(`Saldo inicial recibido: $${playerBalance}`);
    
    updateBalanceDisplay();
    updateBetControls();
    validateBet();
    
    if (gameStatusEl) gameStatusEl.textContent = 'Ajusta tu apuesta y presiona "Repartir"';
}

// Nueva función robusta para sincronizar saldo
function updateBalanceInJava() {
    try {
        if (window.balanceManager && typeof window.balanceManager.saveBalance === 'function') {
            const success = window.balanceManager.saveBalance(playerBalance);
            if (success) {
                console.log(`✓ Saldo guardado exitosamente: $${playerBalance}`);
                // Verificar que se guardó correctamente
                const savedBalance = window.balanceManager.getBalance();
                if (Math.abs(savedBalance - playerBalance) > 0.01) {
                    console.error(`⚠ Error en persistencia: esperado $${playerBalance}, actual $${savedBalance}`);
                    // Reintentar guardado
                    window.balanceManager.forceBalance(playerBalance);
                }
            } else {
                console.error(`✗ Error al guardar saldo: operación falló`);
                // Reintento inmediato
                setTimeout(() => window.balanceManager.forceBalance(playerBalance), 100);
            }
        } else {
            console.error('⚠ BalanceManager no disponible');
        }
    } catch (error) {
        console.error(`✗ Error crítico en guardado: ${error.message}`);
    }
}

// Función mejorada para actualizar display
function updateBalanceDisplay() {
    if (balanceDisplay) {
        balanceDisplay.textContent = `$${playerBalance.toFixed(2)}`;
    }
}

// Nueva función para actualizar controles de apuesta
function updateBetControls() {
    if (betAmountEl) {
        betAmountEl.max = Math.max(playerBalance, 0);
        if (parseFloat(betAmountEl.value) > playerBalance) {
            betAmountEl.value = Math.min(10, playerBalance);
        }
    }
}

function createDeck() {
    deck = [];
    for (let suit of SUITS) {
        for (let value of VALUES) {
            deck.push({ suit, value });
        }
    }
}

function shuffleDeck() {
    for (let i = deck.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [deck[i], deck[j]] = [deck[j], deck[i]];
    }
}

function dealCard() {
    return deck.pop();
}

function displayPlayerCards() {
    if (playerCardsEl) {
        playerCardsEl.innerHTML = playerCards.map(card => 
            `<div class="card ${card.suit}">
                <div class="card-content">
                    <div class="card-corner top-left">${card.value}${SYMBOLS[card.suit]}</div>
                    <div class="card-symbol">${SYMBOLS[card.suit]}</div>
                    <div class="card-corner bottom-right">${card.value}${SYMBOLS[card.suit]}</div>
                </div>
            </div>`
        ).join('');
    }
}

function displayDealerCards(hidden = false) {
    if (dealerCardsEl) {
        if (dealerCards.length !== 2 || !dealerCards.every(card => card.suit && card.value)) {
            console.error(`Cartas del dealer inválidas: ${JSON.stringify(dealerCards)}`);
            return;
        }
        dealerCardsEl.innerHTML = dealerCards.map((card, index) => 
            hidden && index === 1 
                ? `<div class="card card-back"></div>`
                : `<div class="card ${card.suit}">
                    <div class="card-content">
                        <div class="card-corner top-left">${card.value}${SYMBOLS[card.suit]}</div>
                        <div class="card-symbol">${SYMBOLS[card.suit]}</div>
                        <div class="card-corner bottom-right">${card.value}${SYMBOLS[card.suit]}</div>
                    </div>
                </div>`
        ).join('');
    }
}

function displayCommunityCards() {
    if (communityCardsEl) {
        communityCardsEl.innerHTML = communityCards.map(card => 
            `<div class="card ${card.suit}">
                <div class="card-content">
                    <div class="card-corner top-left">${card.value}${SYMBOLS[card.suit]}</div>
                    <div class="card-symbol">${SYMBOLS[card.suit]}</div>
                    <div class="card-corner bottom-right">${card.value}${SYMBOLS[card.suit]}</div>
                </div>
            </div>`
        ).join('');
    }
}

function validateBet() {
    const betValue = parseFloat(betAmountEl?.value) || 0;
    
    if (dealBtn) {
        dealBtn.disabled = betValue <= 0 || betValue > playerBalance || gameStage !== 'pre-deal';
    }
    
    if (decreaseBetBtn) decreaseBetBtn.disabled = betValue <= 1;
    if (increaseBetBtn) increaseBetBtn.disabled = betValue >= playerBalance;
}

function decreaseBet() {
    const currentValue = parseFloat(betAmountEl?.value) || 0;
    const newValue = Math.max(1, currentValue - 1);
    if (betAmountEl) betAmountEl.value = newValue;
    validateBet();
}

function increaseBet() {
    const currentValue = parseFloat(betAmountEl?.value) || 0;
    const newValue = Math.min(playerBalance, currentValue + 1);
    if (betAmountEl) betAmountEl.value = newValue;
    validateBet();
}

function startNewHand() {
    const betAmount = parseFloat(betAmountEl?.value) || 0;
    
    if (betAmount <= 0 || betAmount > playerBalance) {
        alert('Apuesta inválida');
        return;
    }

    currentBet = betAmount;
    currentPot = currentBet * 2;
    playerBalance -= currentBet;

    createDeck();
    shuffleDeck();
    
    playerCards = [dealCard(), dealCard()];
    dealerCards = [dealCard(), dealCard()];
    communityCards = [];
    
    gameStage = 'pre-flop';
    
    displayPlayerCards();
    displayDealerCards(true);
    displayCommunityCards();
    updateBalanceDisplay();
    updateBetControls();
    
    if (gameStatusEl) gameStatusEl.textContent = `Apuesta: $${currentBet}, Bote: $${currentPot}`;
    if (handResultEl) handResultEl.textContent = '';
    
    if (dealBtn) dealBtn.disabled = true;
    if (foldBtn) foldBtn.disabled = false;
    if (callBtn) callBtn.disabled = false;
    if (raiseBtn) raiseBtn.disabled = false;
    
    validateBet();
}

function foldHand() {
    const loss = currentBet;
    gameHistory.push(`Fold: -$${loss}`);
    
    if (handResultEl) handResultEl.textContent = `Te retiraste. Pérdida: $${loss}`;
    updateHistoryDisplay();
    
    // Guardar saldo inmediatamente después de pérdida
    updateBalanceInJava();
    resetGame();
}

function callBet() {
    advanceGameStage();
}

function raiseBet() {
    const raiseAmount = currentBet;
    if (raiseAmount > playerBalance) {
        alert('Fondos insuficientes para subir');
        return;
    }
    
    playerBalance -= raiseAmount;
    currentPot += raiseAmount;
    currentBet += raiseAmount;
    
    updateBalanceDisplay();
    updateBetControls();
    if (gameStatusEl) gameStatusEl.textContent = `Subiste $${raiseAmount}. Bote: $${currentPot}`;
    
    advanceGameStage();
}

function advanceGameStage() {
    switch (gameStage) {
        case 'pre-flop':
            communityCards.push(dealCard(), dealCard(), dealCard());
            gameStage = 'flop';
            if (gameStatusEl) gameStatusEl.textContent = `Flop revelado. Bote: $${currentPot}`;
            break;
            
        case 'flop':
            communityCards.push(dealCard());
            gameStage = 'turn';
            if (gameStatusEl) gameStatusEl.textContent = `Turn revelado. Bote: $${currentPot}`;
            break;
            
        case 'turn':
            communityCards.push(dealCard());
            gameStage = 'river';
            if (gameStatusEl) gameStatusEl.textContent = `River revelado. Bote: $${currentPot}`;
            break;
            
        case 'river':
            endHand();
            return;
    }
    
    displayCommunityCards();
}

function endHand() {
    displayDealerCards(false);
    
    const allCards = [...playerCards, ...communityCards];
    const dealerAllCards = [...dealerCards, ...communityCards];
    
    const playerHand = getBestHand(allCards);
    const dealerHand = getBestHand(dealerAllCards);
    
    const handNames = [
        '', 'Carta Alta', 'Par', 'Doble Par', 'Trío', 'Escalera',
        'Color', 'Full House', 'Póker', 'Escalera de Color', 'Escalera Real'
    ];
    
    let resultText = '';
    let balanceChange = 0;
    
    if (playerHand.value > dealerHand.value || 
        (playerHand.value === dealerHand.value && playerHand.highCard > dealerHand.highCard)) {
        balanceChange = currentPot;
        playerBalance += balanceChange;
        resultText = `¡Ganaste con ${handNames[playerHand.value]}! +$${balanceChange}`;
        gameHistory.push(`Victoria: +$${balanceChange} (${handNames[playerHand.value]})`);
    } else if (playerHand.value < dealerHand.value || 
               (playerHand.value === dealerHand.value && playerHand.highCard < dealerHand.highCard)) {
        balanceChange = -currentBet;
        resultText = `Perdiste. Dealer tiene ${handNames[dealerHand.value]}. -$${Math.abs(balanceChange)}`;
        gameHistory.push(`Derrota: ${balanceChange} (${handNames[dealerHand.value]} vs ${handNames[playerHand.value]})`);
    } else {
        balanceChange = currentBet;
        playerBalance += balanceChange;
        resultText = `Empate con ${handNames[playerHand.value]}. Recuperas $${balanceChange}`;
        gameHistory.push(`Empate: +$${balanceChange} (${handNames[playerHand.value]})`);
    }

    updateBalanceDisplay();
    updateBetControls();
    if (handResultEl) handResultEl.textContent = resultText;
    updateHistoryDisplay();
    
    // Guardar saldo inmediatamente después del resultado
    updateBalanceInJava();
    resetGame();
}

function getBestHand(cards) {
    return evaluateHand(cards);
}

function evaluateHand(cards) {
    const valueMap = {
        '2': 2, '3': 3, '4': 4, '5': 5, '6': 6, '7': 7, '8': 8, '9': 9, '10': 10,
        'J': 11, 'Q': 12, 'K': 13, 'A': 14
    };

    const values = cards.map(card => valueMap[card.value]).sort((a, b) => b - a);
    const suits = cards.map(card => card.suit);

    const valueCounts = {};
    const suitCounts = {};
    values.forEach(val => valueCounts[val] = (valueCounts[val] || 0) + 1);
    suits.forEach(suit => suitCounts[suit] = (suitCounts[suit] || 0) + 1);

    const maxValueCount = Math.max(...Object.values(valueCounts));
    const maxSuitCount = Math.max(...Object.values(suitCounts));

    const uniqueValues = [...new Set(values)].sort((a, b) => a - b);
    const isStraight = uniqueValues.length >= 5 && (uniqueValues[4] - uniqueValues[0] === 4);
    const isFlush = maxSuitCount >= 5;

    let handValue = 1;
    let highCard = Math.max(...values);

    if (isFlush && isStraight && uniqueValues[4] === 14) {
        handValue = 10;
    } else if (isFlush && isStraight) {
        handValue = 9;
    } else if (maxValueCount === 4) {
        handValue = 8;
        highCard = parseInt(Object.keys(valueCounts).find(key => valueCounts[key] === 4));
    } else if (maxValueCount === 3 && Object.values(valueCounts).includes(2)) {
        handValue = 7;
        highCard = parseInt(Object.keys(valueCounts).find(key => valueCounts[key] === 3));
    } else if (isFlush) {
        handValue = 6;
    } else if (isStraight) {
        handValue = 5;
    } else if (maxValueCount === 3) {
        handValue = 4;
        highCard = parseInt(Object.keys(valueCounts).find(key => valueCounts[key] === 3));
    } else if (Object.values(valueCounts).filter(count => count === 2).length === 2) {
        handValue = 3;
        highCard = Math.max(...Object.keys(valueCounts).filter(key => valueCounts[key] === 2).map(Number));
    } else if (maxValueCount === 2) {
        handValue = 2;
        highCard = parseInt(Object.keys(valueCounts).find(key => valueCounts[key] === 2));
    }

    return { value: handValue, highCard };
}

function resetGame() {
    gameStage = 'pre-deal';
    currentBet = 0;
    currentPot = 0;
    
    if (dealBtn) dealBtn.disabled = false;
    if (foldBtn) foldBtn.disabled = true;
    if (callBtn) callBtn.disabled = true;
    if (raiseBtn) raiseBtn.disabled = true;
    
    if (gameStatusEl) gameStatusEl.textContent = 'Ajusta tu apuesta y presiona "Repartir"';
    
    validateBet();
}

function updateHistoryDisplay() {
    if (historyListEl) {
        historyListEl.innerHTML = gameHistory.slice(-10).map(entry => 
            `<li class="history-item ${entry.includes('Victoria') || entry.includes('+') ? 'win' : entry.includes('Derrota') || entry.includes('-') ? 'lose' : ''}">${entry}</li>`
        ).join('');
    }
}

function initGame() {
    console.log('Inicializando Poker Texas Hold\'em...');
    validateBet();
    gameStage = 'pre-deal';
    if (gameStatusEl) gameStatusEl.textContent = 'Cargando saldo...';
}

// Event listeners mejorados
document.addEventListener('DOMContentLoaded', () => {
    initGame();
    
    if (dealBtn) dealBtn.addEventListener('click', startNewHand);
    if (foldBtn) foldBtn.addEventListener('click', foldHand);
    if (callBtn) callBtn.addEventListener('click', callBet);
    if (raiseBtn) raiseBtn.addEventListener('click', raiseBet);
    if (decreaseBetBtn) decreaseBetBtn.addEventListener('click', decreaseBet);
    if (increaseBetBtn) increaseBetBtn.addEventListener('click', increaseBet);
    if (betAmountEl) betAmountEl.addEventListener('input', validateBet);
});

// Función para guardar saldo al cerrar ventana
window.addEventListener('beforeunload', () => {
    updateBalanceInJava();
});
