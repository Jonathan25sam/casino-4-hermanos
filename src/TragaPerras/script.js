const balanceEl = document.getElementById('balance');
const betAmountEl = document.getElementById('betAmount');
const betTable = document.getElementById('betTable');
const spinBtn = document.getElementById('spinBtn');
const clearBtn = document.querySelector('.clear-btn');
const chipButtons = document.querySelectorAll('.chip');
const slots = [
  document.getElementById('slot1'),
  document.getElementById('slot2'),
  document.getElementById('slot3')
];
const messageEl = document.getElementById('message');
const winDisplay = document.getElementById('win-display');

const symbols = [
  { symbol: 'cereza', img: 'img/cherry.png', payoutThree: 1, payoutTwo: 0.2 },
  { symbol: 'limón', img: 'img/lemon.png', payoutThree: 2, payoutTwo: 0.3 },
  { symbol: 'naranja', img: 'img/orange.png', payoutThree: 5, payoutTwo: 0.3 },
  { symbol: 'trébol', img: 'img/clover.png', payoutThree: 10, payoutTwo: 0.5 },
  { symbol: 'diamante', img: 'img/diamond.png', payoutThree: 20, payoutTwo: 1 },
  { symbol: 'siete', img: 'img/seven.png', payoutThree: 50, payoutTwo: 2 }
];

let currentBet = 0;
let balance = 0;
let spinning = false;
let stopped = [false, false, false];
let results = [];
let intervalID = null;
let isInitialized = false;

const MIN_BET = 1;
const BET_STEP = 1;
const chipValues = [1000, 500, 100, 50, 10, 1];
const chipClasses = ['chip-1000', 'chip-500', 'chip-100', 'chip-50', 'chip-10', 'chip-1'];

function setInitialBalance(initialBalance) {
  if (typeof initialBalance !== 'number' || initialBalance < 0 || isNaN(initialBalance)) {
    console.error('Saldo inicial inválido:', initialBalance);
    showMessage('Error: Saldo inicial inválido.');
    return;
  }
  balance = initialBalance;
  isInitialized = true;
  updateBalanceDisplay();
  initializeGame();
}

function getBalance() {
  return balance;
}

function updateBalanceDisplay() {
  if (!balanceEl) {
    console.error('Elemento de saldo no encontrado');
    showMessage('Error: Interfaz no cargada.');
    return;
  }
  balanceEl.textContent = balance.toFixed(2);

  if (window.javaBridge) {
    try {
      window.javaBridge.updateBalance(balance);
    } catch (e) {
      console.error('Error al notificar saldo:', e.message);
    }
  }
}

function initializeGame() {
  if (!isInitialized) {
    showMessage('Esperando saldo inicial...');
    return;
  }
  if (balance < MIN_BET) {
    showMessage('No tienes suficiente saldo. Añade fondos.');
    disableGameControls();
    return;
  }
  spinBtn.disabled = true;
  chipButtons.forEach(button => (button.disabled = false));
  clearBtn.disabled = true;
  showMessage('');
  winDisplay.textContent = '';
}

function disableGameControls() {
  spinBtn.disabled = true;
  chipButtons.forEach(button => (button.disabled = true));
  clearBtn.disabled = true;
}

function showMessage(text) {
  if (messageEl) {
    messageEl.textContent = text;
    messageEl.classList.toggle('show', !!text);
  }
}

function updateBalance(amount) {
  if (typeof amount !== 'number' || isNaN(amount)) {
    console.error('Cantidad inválida:', amount);
    return;
  }
  
  balance = Math.max(0, balance + amount);
  updateBalanceDisplay();
  notifyJavaFX(balance);
}

function updateBet(amount) {
  if (!isInitialized) {
    showMessage('Error: Saldo no inicializado.');
    return;
  }
  if (typeof amount !== 'number' || isNaN(amount) || amount < 0) {
    showMessage('Error: Apuesta inválida.');
    return;
  }
  
  currentBet = Math.max(0, Math.min(amount, balance));
  if (betAmountEl) betAmountEl.textContent = currentBet.toFixed(2);
  
  spinBtn.disabled = currentBet < MIN_BET || currentBet > balance;
  clearBtn.disabled = currentBet === 0;
  chipButtons.forEach(button => (button.disabled = balance < 1));
  
  if (balance < 1) {
    showMessage('No tienes suficiente saldo. Añade fondos.');
    disableGameControls();
  } else if (currentBet < MIN_BET && currentBet > 0) {
    showMessage(`La apuesta mínima para girar es $${MIN_BET}.`);
  } else if (currentBet > balance) {
    showMessage('Apuesta excede el saldo.');
  } else {
    showMessage('');
  }
  renderBetChips();
}

function renderBetChips() {
  if (!betTable) return;
  betTable.innerHTML = '';
  let remainingBet = currentBet;
  
  chipValues.forEach((value, index) => {
    while (remainingBet >= value) {
      const chip = document.createElement('div');
      chip.className = `bet-chip ${chipClasses[index]}`;
      chip.textContent = value;
      betTable.appendChild(chip);
      remainingBet -= value;
    }
  });
}

function getWeightedRandomSymbol() {
  // Usar pesos diferentes para apuesta de 1 peso
  const weights = currentBet === 1 
    ? [3, 3, 2, 2, 0.1, 0.1] // Reducir diamante y siete para apuesta de 1 peso
    : [2, 2, 1, 1, 1, 1]; // Pesos normales para otras apuestas
  const totalWeight = weights.reduce((sum, w) => sum + w, 0);
  
  const rand = Math.random() * totalWeight;
  let sum = 0;
  for (let i = 0; i < weights.length; i++) {
    sum += weights[i];
    if (rand < sum) return symbols[i];
  }
  return symbols[symbols.length - 1];
}

function getNonMatchingSymbol(symbol) {
  let otherSymbol;
  do {
    otherSymbol = getWeightedRandomSymbol();
  } while (otherSymbol === symbol);
  return otherSymbol;
}

function spin() {
  if (spinning || currentBet < MIN_BET || !isInitialized || currentBet > balance) {
    showMessage('No puede girar. Verifique su apuesta o saldo.');
    return;
  }

  try {
    spinning = true;
    spinBtn.disabled = true;
    clearBtn.disabled = true;
    chipButtons.forEach(button => {
      button.disabled = true;
    });
    showMessage('');
    winDisplay.textContent = '';
    slots.forEach(slot => {
      slot.classList.remove('win');
      slot.classList.add('spinning');
    });

    updateBalance(-currentBet);

    // Determinar probabilidad de ganancia según la apuesta
    let winProbability;
    if (currentBet < 10) {
      winProbability = 0.55; // 55% (< 10 pesos)
    } else if (currentBet >= 10 && currentBet <= 15) {
      winProbability = 0.45; // 45% (10-100 pesos)
    } 
    else if (currentBet === 16) {
      winProbability = 1; // 100% (16)
    }
    else if (currentBet >= 17 && currentBet < 100) {
      winProbability = 0.45; // 45% (10-100 pesos)
    }
        else if (currentBet >= 100 && currentBet < 500) {
      winProbability = 0.3; // 30% (100-500 pesos)
    } else {
      winProbability = 0.1; // 10% (≥ 500 pesos)
    }

    const rand = Math.random();
    if (rand < winProbability) {
      // Ganancia: 50% tres iguales, 50% dos iguales
      if (Math.random() < 0.5) {
        const symbol = getWeightedRandomSymbol();
        results = [symbol, symbol, symbol];
      } else {
        const symbol = getWeightedRandomSymbol();
        const otherSymbol = getNonMatchingSymbol(symbol);
        const pairPosition = Math.floor(Math.random() * 3);
        if (pairPosition === 0) {
          results = [symbol, symbol, otherSymbol];
        } else if (pairPosition === 1) {
          results = [otherSymbol, symbol, symbol];
        } else {
          results = [symbol, otherSymbol, symbol];
        }
      }
    } else {
      let symbol1 = getWeightedRandomSymbol();
      let symbol2, symbol3;
      do {
        symbol2 = getWeightedRandomSymbol();
      } while (symbol2 === symbol1);
      do {
        symbol3 = getWeightedRandomSymbol();
      } while (symbol3 === symbol1 || symbol3 === symbol2);
      results = [symbol1, symbol2, symbol3];
    }

    console.log(`Iniciando giro, apuesta=${currentBet}, winProbability=${winProbability}, results:`, results.map(r => r.symbol));
    stopped = [false, false, false];
    intervalID = setInterval(() => {
      slots.forEach((slot, i) => {
        if (!stopped[i]) {
          updateSlotImage(slot, getWeightedRandomSymbol());
        }
      });
    }, 80);

    setTimeout(() => stopSlot(0), 800);
    setTimeout(() => stopSlot(1), 1200);
    setTimeout(() => stopSlot(2), 1600);

    setTimeout(() => {
      if (spinning) {
        console.warn('Terminando forzosamente el giro');
        if (intervalID !== null) {
          clearInterval(intervalID);
          intervalID = null;
        }
        stopped = [true, true, true];
        slots.forEach((slot, i) => {
          if (results[i]) {
            updateSlotImage(slot, results[i]);
          }
          slot.classList.remove('spinning');
        });
        calculatePrize();
        spinning = false;
        setTimeout(() => {
          updateBet(0);
          if (balance >= MIN_BET) {
            chipButtons.forEach(button => {
              button.disabled = false;
            });
            clearBtn.disabled = true;
            spinBtn.disabled = true;
          } else {
            disableGameControls();
          }
        }, 3000);
      }
    }, 2200);
  } catch (e) {
    console.error('Error en spin:', e);
    showMessage('Error en el juego. Por favor, recarga.');
    spinning = false;
    if (intervalID !== null) {
      clearInterval(intervalID);
      intervalID = null;
    }
    slots.forEach(slot => slot.classList.remove('spinning'));
  }
}

function stopSlot(index) {
  try {
    console.log(`Deteniendo slot ${index}, stopped antes:`, stopped);
    if (!results[index]) {
      throw new Error(`Resultado inválido para slot ${index}`);
    }
    stopped[index] = true;
    updateSlotImage(slots[index], results[index]);
    slots[index].classList.remove('spinning');
    console.log(`Slot ${index} detenido, stopped después:`, stopped);

    if (stopped.every(s => s)) {
      console.log('Todos los slots detenidos, limpiando intervalo');
      if (intervalID !== null) {
        clearInterval(intervalID);
        intervalID = null;
      }
      calculatePrize();
      spinning = false;
      setTimeout(() => {
        updateBet(0);
        if (balance >= MIN_BET) {
          chipButtons.forEach(button => {
            button.disabled = false;
          });
          clearBtn.disabled = true;
          spinBtn.disabled = true;
        } else {
          disableGameControls();
        }
      }, 3000);
    }
  } catch (e) {
    console.error(`Error en stopSlot ${index}:`, e);
    showMessage('Error en el juego. Por favor, recarga.');
    spinning = false;
    if (intervalID !== null) {
      clearInterval(intervalID);
      intervalID = null;
    }
    slots.forEach(slot => slot.classList.remove('spinning'));
  }
}

function updateSlotImage(slotElement, symbolObj) {
  if (!slotElement || !symbolObj || !symbolObj.img || !symbolObj.symbol) {
    console.error('Parámetros inválidos en updateSlotImage:', { slotElement, symbolObj });
    return;
  }
  slotElement.innerHTML = '';
  const img = document.createElement('img');
  img.src = symbolObj.img;
  img.alt = symbolObj.symbol;
  img.onerror = () => {
    console.warn(`Error al cargar imagen ${symbolObj.img}, usando texto de respaldo`);
    slotElement.textContent = symbolObj.symbol.charAt(0);
  };
  slotElement.appendChild(img);
}

function calculatePrize() {
  try {
    console.log('Calculando premio, results:', results.map(r => r.symbol));
    if (!results[0] || !results[1] || !results[2] || 
        !results[0].symbol || !results[1].symbol || !results[2].symbol) {
      throw new Error('Resultados de slots inválidos');
    }

    const [a, b, c] = results;
    let winMultiplier = 0;
    let winningSymbol = null;
    let winMessage = '';

    if (a.symbol === b.symbol && b.symbol === c.symbol) {
      winMultiplier = a.payoutThree;
      winningSymbol = a;
      winMessage = `¡${a.symbol.charAt(0).toUpperCase() + a.symbol.slice(1)} x3!`;
      slots.forEach(slot => slot.classList.add('win'));
    } else if (a.symbol === b.symbol) {
      winMultiplier = a.payoutTwo || 0;
      winningSymbol = a;
      winMessage = `¡${a.symbol.charAt(0).toUpperCase() + a.symbol.slice(1)} x2!`;
      slots[0].classList.add('win');
      slots[1].classList.add('win');
    } else if (b.symbol === c.symbol) {
      winMultiplier = b.payoutTwo || 0;
      winningSymbol = b;
      winMessage = `¡${b.symbol.charAt(0).toUpperCase() + b.symbol.slice(1)} x2!`;
      slots[1].classList.add('win');
      slots[2].classList.add('win');
    } else if (a.symbol === c.symbol) {
      winMultiplier = a.payoutTwo || 0;
      winningSymbol = a;
      winMessage = `¡${a.symbol.charAt(0).toUpperCase() + a.symbol.slice(1)} x2!`;
      slots[0].classList.add('win');
      slots[2].classList.add('win');
    }

    const netWinnings = (currentBet * winMultiplier).toFixed(2);
    const totalToAdd = (parseFloat(netWinnings) + currentBet).toFixed(2);

    if (winMultiplier > 0) {
      winDisplay.textContent = `¡Ganaste ${netWinnings} pesos!`;
      winDisplay.className = 'win';
      showMessage(`${winMessage} ¡Sigue así, estás en racha!`);
      updateBalance(parseFloat(totalToAdd));
    } else {
      winDisplay.textContent = `Perdiste ${currentBet.toFixed(2)} pesos`;
      winDisplay.className = 'loss';
      showMessage(`¡Casi lo logras! Inténtalo otra vez.`);
    }
    console.log(`Premio calculado: winMultiplier=${winMultiplier}, netWinnings=${netWinnings}, totalToAdd=${totalToAdd}`);
    setTimeout(() => { 
      winDisplay.textContent = ''; 
      winDisplay.className = ''; 
      showMessage('');
    }, 3000);

    if (balance < MIN_BET) {
      showMessage('Juego terminado. Añade fondos.');
      disableGameControls();
    }
  } catch (e) {
    console.error('Error en calculatePrize:', e);
    showMessage('Error en el juego. Por favor, recarga.');
  }
}

function notifyJavaFX(newBalance) {
  if (!window.javaBridge) return;

  let attempts = 0;
  const maxAttempts = 3;
  
  function attemptNotification() {
    try {
      window.javaBridge.updateBalance(newBalance);
      const javaBalance = window.javaBridge.getBalance();
      if (Math.abs(javaBalance - newBalance) > 0.01) {
        throw new Error('Discrepancia en balance');
      }
    } catch (e) {
      attempts++;
      if (attempts < maxAttempts) {
        setTimeout(attemptNotification, 300 * attempts);
      }
    }
  }
  attemptNotification();
}

chipButtons.forEach(button => {
  button.addEventListener('click', () => {
    const value = parseInt(button.getAttribute('data-value'), 10);
    updateBet(currentBet + value);
  });
});

clearBtn?.addEventListener('click', () => updateBet(0));
spinBtn?.addEventListener('click', spin);

slots.forEach((slot, index) => updateSlotImage(slot, symbols[index % symbols.length]));

document.addEventListener('DOMContentLoaded', () => {
  const elements = { balanceEl, betAmountEl, betTable, spinBtn, clearBtn, slots, winDisplay };
  if (Object.values(elements).some(el => !el || (Array.isArray(el) && el.some(s => !s)))) {
    showMessage('Error: Interfaz no cargada.');
  }
});