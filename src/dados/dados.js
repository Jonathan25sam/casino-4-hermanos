// Variables del juego
let balance = 0; // Inicialmente 0, se establecerá desde JavaFX
let currentBet = 0;
let prediction = null; // 'par' o 'impar'
let diceValue = 1;
let isRolling = false;
let history = [];

// Elementos DOM
const diceElement = document.getElementById('dice');
const balanceDisplay = document.getElementById('balanceDisplay');
const resultDisplay = document.getElementById('resultDisplay');
const betInput = document.getElementById('betAmount');
const btnPar = document.getElementById('btnPar');
const btnImpar = document.getElementById('btnImpar');
const btnRoll = document.getElementById('btnRoll');
const historyList = document.getElementById('historyList');

// Verificar que todos los elementos del DOM existan
if (!diceElement || !balanceDisplay || !resultDisplay || !betInput || !btnPar || !btnImpar || !btnRoll || !historyList) {
    console.error("Error: Uno o más elementos del DOM no fueron encontrados. Verifica el HTML.");
    if (resultDisplay) resultDisplay.textContent = "Error: Interfaz no cargada correctamente.";
    throw new Error("Fallo en la inicialización del DOM");
}

// Función para recibir el saldo inicial desde JavaFX
function setInitialBalance(initialBalance) {
    if (typeof initialBalance !== 'number' || initialBalance < 0) {
        console.error("Saldo inicial inválido recibido desde JavaFX: ", initialBalance);
        resultDisplay.textContent = "Error: Saldo inicial inválido.";
        return;
    }
    console.log("Recibiendo saldo inicial desde JavaFX: $" + initialBalance);
    balance = initialBalance;
    updateBalanceDisplay();
    validateBet();
}

// Función para devolver el saldo actual a JavaFX
function getBalance() {
    console.log("Devolviendo saldo a JavaFX: $" + balance);
    return balance;
}

// Inicialización del dado
updateDiceDisplay(1);

// Event Listeners
btnPar.addEventListener('click', () => {
    selectPrediction('par');
});

btnImpar.addEventListener('click', () => {
    selectPrediction('impar');
});

btnRoll.addEventListener('click', () => {
    rollDice();
});

betInput.addEventListener('input', validateBet);

// Funciones del juego
function selectPrediction(pred) {
    if (isRolling) return;
    
    prediction = pred;
    
    // Actualizar UI
    btnPar.classList.remove('selected');
    btnImpar.classList.remove('selected');
    
    if (pred === 'par') {
        btnPar.classList.add('selected');
    } else {
        btnImpar.classList.add('selected');
    }
    
    // Verificar apuesta
    validateBet();
}

function validateBet() {
    currentBet = parseInt(betInput.value);
    
    if (isNaN(currentBet) || currentBet <= 0 || currentBet > balance) {
        btnRoll.disabled = true;
        resultDisplay.textContent = `Cantidad inválida. Apuesta entre $1 y $${balance}`;
        return false;
    }
    
    if (!prediction) {
        btnRoll.disabled = true;
        resultDisplay.textContent = 'Selecciona si será par o impar';
        return false;
    }
    
    btnRoll.disabled = false;
    resultDisplay.textContent = `Apuesta $${currentBet} a ${prediction.toUpperCase()}`;
    return true;
}

function rollDice() {
    if (!validateBet() || isRolling) return;
    
    isRolling = true;
    btnRoll.disabled = true;
    btnPar.disabled = true;
    btnImpar.disabled = true;
    betInput.disabled = true;
    
    resultDisplay.textContent = 'Lanzando dado...';
    resultDisplay.className = 'result';
    
    // Animación de girar el dado
    let count = 0;
    const maxRolls = 10;
    
    const rollInterval = setInterval(() => {
        count++;
        const randomValue = Math.floor(Math.random() * 6) + 1;
        updateDiceDisplay(randomValue);
        
        // Dar efecto de giro
        diceElement.style.transform = `rotate(${count * 36}deg)`;
        
        if (count >= maxRolls) {
            clearInterval(rollInterval);
            
            // Resultado final del dado
            diceValue = Math.floor(Math.random() * 6) + 1;
            const isEven = diceValue % 2 === 0;
            const userWins = (prediction === 'par' && isEven) || (prediction === 'impar' && !isEven);
            
            // Actualizar UI después de una breve pausa
            setTimeout(() => {
                // Detener rotación
                diceElement.style.transform = 'rotate(0deg)';
                updateDiceDisplay(diceValue);
                
                // Actualizar saldo y mostrar resultado
                if (userWins) {
                    balance += currentBet;
                    resultDisplay.className = 'result win';
                    resultDisplay.textContent = `¡GANASTE! El dado muestra un ${diceValue} (${isEven ? 'PAR' : 'IMPAR'})`;
                } else {
                    balance -= currentBet;
                    resultDisplay.className = 'result lose';
                    resultDisplay.textContent = `¡PERDISTE! El dado muestra un ${diceValue} (${isEven ? 'PAR' : 'IMPAR'})`;
                }
                
                updateBalanceDisplay();
                
                // Guardar en historial
                addToHistory(userWins, diceValue, isEven ? 'PAR' : 'IMPAR', currentBet);
                
                // Reactivar controles
                isRolling = false;
                btnPar.disabled = false;
                btnImpar.disabled = false;
                betInput.disabled = false;
                btnRoll.disabled = true;
                
                // Quitar selecciones
                btnPar.classList.remove('selected');
                btnImpar.classList.remove('selected');
                prediction = null;
                
            }, 500);
        }
    }, 100);
}

function updateDiceDisplay(value) {
    // Limpiar el dado
    diceElement.className = 'dice';
    diceElement.innerHTML = ''; // Limpiar puntos anteriores

    // Definir las posiciones de los puntos según el valor del dado
    const dotPositions = {
        1: [{ top: '60px', left: '60px' }],
        2: [{ top: '30px', left: '30px' }, { top: '90px', right: '30px' }],
        3: [{ top: '30px', left: '30px' }, { top: '60px', left: '60px' }, { top: '90px', right: '30px' }],
        4: [{ top: '30px', left: '30px' }, { top: '30px', right: '30px' }, { top: '90px', left: '30px' }, { top: '90px', right: '30px' }],
        5: [{ top: '30px', left: '30px' }, { top: '30px', right: '30px' }, { top: '60px', left: '60px' }, { top: '90px', left: '30px' }, { top: '90px', right: '30px' }],
        6: [{ top: '30px', left: '30px' }, { top: '30px', right: '30px' }, { top: '60px', left: '30px' }, { top: '60px', right: '30px' }, { top: '90px', left: '30px' }, { top: '90px', right: '30px' }]
    };

    // Crear los puntos dinámicamente
    const positions = dotPositions[value] || [];
    positions.forEach(pos => {
        const dot = document.createElement('div');
        dot.className = 'dot';
        dot.style.top = pos.top;
        dot.style.left = pos.left;
        dot.style.right = pos.right || 'auto';
        diceElement.appendChild(dot);
    });
}

function updateBalanceDisplay() {
    if (balanceDisplay) {
        balanceDisplay.textContent = `Saldo: $${balance.toFixed(2)}`;
        console.log("Saldo mostrado en la interfaz: $" + balance.toFixed(2));
    } else {
        console.error("Error: balanceDisplay no encontrado en el DOM");
    }
    // Notificar a JavaFX del cambio en el saldo
    if (window.javaBridge && typeof window.javaBridge.updateBalance === 'function') {
        window.javaBridge.updateBalance(balance);
        console.log("Notificando saldo a JavaFX: $" + balance);
    } else {
        console.warn("JavaBridge no disponible o método updateBalance no definido.");
    }
}

function addToHistory(win, value, result, amount) {
    const now = new Date();
    const timeString = now.toLocaleTimeString();
    
    const historyItem = {
        time: timeString,
        win: win,
        dice: value,
        result: result,
        prediction: prediction.toUpperCase(),
        amount: amount
    };
    
    history.unshift(historyItem); // Añadir al principio
    
    // Limitar historial a 10 entradas
    if (history.length > 10) {
        history.pop();
    }
    
    updateHistoryDisplay();
}

function updateHistoryDisplay() {
    if (history.length === 0) {
        historyList.innerHTML = '<div class="history-item">No hay jugadas registradas</div>';
        return;
    }
    
    historyList.innerHTML = '';
    
    history.forEach(item => {
        const itemEl = document.createElement('div');
        itemEl.className = `history-item ${item.win ? 'win' : 'lose'}`;
        
        itemEl.textContent = `${item.time}: ${item.win ? 'GANASTE' : 'PERDISTE'} $${item.amount} - 
            Apostaste: ${item.prediction}, Resultado: ${item.result} (${item.dice})`;
        
        historyList.appendChild(itemEl);
    });
}

// Validar la apuesta cuando cambia el valor
betInput.addEventListener('input', validateBet);

// Inicialización final (espera saldo de JavaFX)
console.log("Script cargado, esperando saldo inicial desde JavaFX.");