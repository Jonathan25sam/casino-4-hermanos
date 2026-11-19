let balance = 10000;
let currentBet = 0;
let isGameActive = false;
let currentQuestionIndex = 0;
let correctAnswers = 0;
let totalWinnings = 0;
let consecutiveWins = 0;
let questionsAnswered = 0;
let timeLeft = 5;
let currentRound = 1;
const baseMultiplier = 1.10;
let questionBank = [];
let currentQuestions = [];
let usedQuestions = new Set();
let timer;
let isProcessingAnswer = false;

// Establecer saldo inicial desde JavaFX
function setBalance(newBalance) {
    balance = newBalance;
    updateDisplay();
    console.log(`Saldo inicial establecido desde JavaFX: $${newBalance}`);
}

// Obtener saldo actual para JavaFX
function getBalance() {
    return balance;
}

// Cargar banco de preguntas
function loadQuestions() {
    questionBank = [
        { question: '¿Cuál es la capital de Francia?', options: ['Madrid', 'París', 'Londres', 'Roma'], correctAnswer: 1 },
        { question: '¿Qué planeta es conocido como el planeta rojo?', options: ['Venus', 'Marte', 'Júpiter', 'Saturno'], correctAnswer: 1 },
        { question: '¿Quién pintó la Mona Lisa?', options: ['Picasso', 'Da Vinci', 'Van Gogh', 'Monet'], correctAnswer: 1 },
        { question: '¿Cuál es el río más largo del mundo?', options: ['Amazonas', 'Nilo', 'Yangtsé', 'Misisipi'], correctAnswer: 1 },
        { question: '¿En qué año llegó el hombre a la luna?', options: ['1965', '1969', '1972', '1980'], correctAnswer: 1 },
        { question: '¿Cuál es el elemento químico con símbolo O?', options: ['Oro', 'Oxígeno', 'Osmio', 'Oganesón'], correctAnswer: 1 },
        { question: '¿Qué país tiene más habitantes?', options: ['India', 'China', 'Estados Unidos', 'Indonesia'], correctAnswer: 0 },
        { question: '¿Quién escribió "Don Quijote de la Mancha"?', options: ['García Márquez', 'Cervantes', 'Borges', 'Lope de Vega'], correctAnswer: 1 },
        { question: '¿Cuál es el océano más grande?', options: ['Atlántico', 'Índico', 'Pacífico', 'Ártico'], correctAnswer: 2 },
        { question: '¿En qué continente está Egipto?', options: ['Asia', 'África', 'Europa', 'Oceanía'], correctAnswer: 1 },
        { question: '¿Cuál es la capital de Japón?', options: ['Seúl', 'Tokio', 'Pekín', 'Bangkok'], correctAnswer: 1 },
        { question: '¿Qué instrumento toca un violinista?', options: ['Flauta', 'Violín', 'Trompeta', 'Batería'], correctAnswer: 1 },
        { question: '¿Cuál es el metal más abundante en la Tierra?', options: ['Hierro', 'Aluminio', 'Cobre', 'Plata'], correctAnswer: 1 },
        { question: '¿Qué animal es conocido como el rey de la selva?', options: ['Tigre', 'Elefante', 'León', 'Gorila'], correctAnswer: 2 },
        { question: '¿En qué año comenzó la Segunda Guerra Mundial?', options: ['1939', '1914', '1945', '1929'], correctAnswer: 0 }
    ];
    if (questionBank.length < 5) {
        showMessage('No hay suficientes preguntas en el banco. Usando predeterminada.');
        questionBank.push({ question: '¿Cuál es la capital de Francia?', options: ['Madrid', 'París', 'Londres', 'Roma'], correctAnswer: 1 });
    }
}

// Actualizar interfaz
function updateDisplay() {
    document.getElementById('balance').textContent = `Saldo: $${Math.floor(balance)}`;
    document.getElementById('bet').textContent = `Apuesta Actual: $${Math.floor(currentBet)}`;
    document.getElementById('multiplier').textContent = `Multiplicador: x${getCurrentMultiplier().toFixed(2)}`;
    document.getElementById('winnings').textContent = `Ganancias Totales: $${Math.floor(totalWinnings)}`;
    document.getElementById('wins').textContent = `Victorias Seguidas: ${consecutiveWins}`;
    document.getElementById('answered').textContent = `Preguntas Respondidas: ${questionsAnswered}`;
}

// Añadir cantidad a la apuesta
function addToBet(amount) {
    if (balance >= amount && !isGameActive) {
        currentBet += amount;
        balance -= amount;
        updateDisplay();
        if (typeof javaApp !== 'undefined') {
            javaApp.updateBalance(balance);
            console.log(`Saldo actualizado a JavaFX desde addToBet: $${balance}`);
        }
    } else if (isGameActive) {
        showMessage('No puedes cambiar la apuesta durante el juego');
    } else {
        showMessage('Saldo insuficiente');
    }
}

// Limpiar apuesta
function clearBet() {
    if (!isGameActive) {
        balance += currentBet;
        currentBet = 0;
        updateDisplay();
        if (typeof javaApp !== 'undefined') {
            javaApp.updateBalance(balance);
            console.log(`Saldo actualizado a JavaFX desde clearBet: $${balance}`);
        }
    }
}

// Iniciar nuevo juego
function startNewGame() {
    if (currentBet <= 0) {
        showMessage('Debes hacer una apuesta primero');
        return;
    }
    isGameActive = true;
    currentQuestionIndex = 0;
    correctAnswers = 0;
    questionsAnswered = 0;
    currentRound = 1;
    usedQuestions.clear(); // Reiniciar preguntas usadas al iniciar un nuevo juego
    selectRandomQuestions();
    document.getElementById('waiting-state').classList.add('hidden');
    document.getElementById('game-area').classList.remove('hidden');
    updateGameArea();
    updateDisplay();
    startTimer();
}

// Seleccionar preguntas aleatorias no usadas
function selectRandomQuestions() {
    currentQuestions = [];
    const availableQuestions = questionBank.filter((_, index) => !usedQuestions.has(index));
    
    if (availableQuestions.length < 5) {
        showMessage('No hay suficientes preguntas nuevas. Reiniciando el banco de preguntas.');
        usedQuestions.clear();
    }

    const usedIndices = [];
    for (let i = 0; i < 5; i++) {
        let randomIndex;
        do {
            randomIndex = Math.floor(Math.random() * questionBank.length);
        } while (usedQuestions.has(randomIndex) || usedIndices.includes(randomIndex));
        usedIndices.push(randomIndex);
        usedQuestions.add(randomIndex);
        currentQuestions.push(questionBank[randomIndex]);
    }
}

// Actualizar área de juego
function updateGameArea() {
    if (currentQuestionIndex < currentQuestions.length) {
        const q = currentQuestions[currentQuestionIndex];
        document.getElementById('question-number').textContent = `Pregunta ${currentQuestionIndex + 1} de 5 (Ronda ${currentRound})`;
        document.getElementById('question-text').textContent = q.question;
        const optionsDiv = document.getElementById('options');
        optionsDiv.innerHTML = '';
        q.options.forEach((option, i) => {
            const btn = document.createElement('button');
            btn.className = 'option-btn w-full';
            btn.textContent = option;
            btn.setAttribute('aria-label', `Opción ${i + 1}: ${option}`);
            btn.onclick = () => selectAnswer(i);
            optionsDiv.appendChild(btn);
        });
        isProcessingAnswer = false;
        optionsDiv.classList.add('animate-fade-in');
    } else {
        endGameWithLoss();
    }
}

// Iniciar temporizador
function startTimer() {
    if (timer) clearInterval(timer);
    timeLeft = 5;
    document.getElementById('timer').textContent = timeLeft;
    timer = setInterval(() => {
        timeLeft--;
        document.getElementById('timer').textContent = timeLeft;
        if (timeLeft <= 0) {
            clearInterval(timer);
            endGameWithLoss();
        }
    }, 1000);
}

// Seleccionar respuesta
function selectAnswer(selectedIndex) {
    if (!isGameActive || isProcessingAnswer) return;
    isProcessingAnswer = true;
    clearInterval(timer);
    const q = currentQuestions[currentQuestionIndex];
    const buttons = document.querySelectorAll('#options button');
    buttons.forEach(btn => btn.disabled = true);
    if (selectedIndex === q.correctAnswer) {
        buttons[selectedIndex].classList.add('correct');
        correctAnswers++;
        questionsAnswered++;
        consecutiveWins++;
        setTimeout(() => {
            currentQuestionIndex++;
            if (currentQuestionIndex >= currentQuestions.length) {
                showDoubleOrNothing();
            } else {
                updateGameArea();
                startTimer();
            }
        }, 1500);
    } else {
        buttons[selectedIndex].classList.add('incorrect');
        buttons[q.correctAnswer].classList.add('correct');
        setTimeout(() => endGameWithLoss(), 2000);
    }
    updateDisplay();
}

// Mostrar modal de Doble o Nada
function showDoubleOrNothing() {
    isProcessingAnswer = true;
    const currentMultiplier = getCurrentMultiplier();
    const netWinnings = Math.floor(currentBet * (currentMultiplier - 1));
    const prize = Math.floor(currentBet * currentMultiplier);
    const modalContent = `
        <div class="text-center">
            <h2 class="text-2xl font-bold text-neon-green mb-4">¡Felicitaciones!</h2>
            <p class="text-gray-300 mb-2">Has completado la Ronda ${currentRound}</p>
            <p class="text-green-400 text-xl mb-2">Ganancia: $${netWinnings}</p>
            <p class="text-gray-500 mb-4">Próximo multiplicador: x${(currentMultiplier + 0.25).toFixed(2)}</p>
            <div class="flex justify-center gap-4 mb-4">
                <button class="modal-btn" onclick="collectWinnings()" aria-label="Retirar premio">Retirar $${prize}</button>
                <button class="modal-btn continue" onclick="continueGame()" aria-label="Continuar con Doble o Nada">Doble o Nada</button>
            </div>
            <p class="text-red-400 italic">⚠️ Si continúas y fallas, pierdes todo</p>
        </div>
    `;
    showModal(modalContent);
}

// Recolectar ganancias
function collectWinnings() {
    const currentMultiplier = getCurrentMultiplier();
    const prize = Math.floor(currentBet * currentMultiplier);
    const netWinnings = prize - currentBet;
    balance += prize;
    totalWinnings += netWinnings;
    showEndGame('¡Ganancias Retiradas!', `+$${netWinnings}`, balance, currentRound, true);
    if (typeof javaApp !== 'undefined') {
        javaApp.updateBalance(balance);
        console.log(`Saldo actualizado a JavaFX desde collectWinnings: $${balance}`);
    }
    setTimeout(() => endGame(), 2000);
}

// Continuar juego
function continueGame() {
    currentRound++;
    currentQuestionIndex = 0;
    selectRandomQuestions();
    updateGameArea();
    startTimer();
    hideModal();
}

// Terminar juego con pérdida
function endGameWithLoss() {
    if (timer) clearInterval(timer);
    showEndGame('Fin del Juego', `-$${currentBet}`, balance, correctAnswers, false);
    if (typeof javaApp !== 'undefined') {
        javaApp.updateBalance(balance);
        console.log(`Saldo actualizado a JavaFX desde endGameWithLoss: $${balance}`);
    }
    setTimeout(() => endGame(), 2000);
}

// Mostrar modal de fin de juego
function showEndGame(title, amount, finalBalance, stat, isWin) {
    isProcessingAnswer = true;
    const modalContent = `
        <div class="text-center">
            <h2 class="text-2xl font-bold text-neon-green mb-4">${title}</h2>
            <p class="${isWin ? 'text-green-400' : 'text-red-400'} mb-2">${isWin ? 'Ganaste:' : 'Perdiste:'}</p>
            <p class="${isWin ? 'text-green-400' : 'text-red-400'} text-xl mb-2">${amount}</p>
            <p class="text-gray-300 mb-2">Saldo final: $${Math.floor(finalBalance)}</p>
            <p class="text-gray-300 mb-4">${isWin ? 'Rondas completadas: ' : 'Preguntas correctas: '}${stat}</p>
            <button class="modal-btn" onclick="endGame()" aria-label="Jugar de nuevo">Jugar de Nuevo</button>
        </div>
    `;
    showModal(modalContent);
}

// Finalizar juego
function endGame() {
    if (timer) clearInterval(timer);
    isGameActive = false;
    currentBet = 0;
    consecutiveWins = 0;
    correctAnswers = 0;
    questionsAnswered = 0;
    currentQuestionIndex = 0;
    currentRound = 1;
    isProcessingAnswer = false;
    try {
        document.getElementById('game-area').classList.add('hidden');
        document.getElementById('waiting-state').classList.remove('hidden');
    } catch (e) {
        console.error("Error al actualizar la interfaz: ", e);
    }
    updateDisplay();
    hideModal();
    if (typeof javaApp !== 'undefined') {
        javaApp.updateBalance(balance);
        console.log(`Saldo actualizado a JavaFX desde endGame: $${balance}`);
    }
}

// Calcular multiplicador
function getCurrentMultiplier() {
    return baseMultiplier + ((currentRound - 1) * 0.25);
}

// Mostrar modal
function showModal(content) {
    try {
        document.getElementById('modal-content').innerHTML = content;
        document.getElementById('modal').classList.remove('hidden');
    } catch (e) {
        console.error("Error al mostrar el modal: ", e);
    }
}

// Ocultar modal
function hideModal() {
    try {
        document.getElementById('modal').classList.add('hidden');
        isProcessingAnswer = false;
    } catch (e) {
        console.error("Error al ocultar el modal: ", e);
    }
}

// Mostrar mensaje
function showMessage(message) {
    isProcessingAnswer = true;
    const modalContent = `
        <div class="text-center">
            <p class="text-lg text-gray-300 mb-4">${message}</p>
            <button class="modal-btn" onclick="hideModal()" aria-label="Aceptar">Aceptar</button>
        </div>
    `;
    showModal(modalContent);
}

// Inicializar juego
window.onload = () => {
    loadQuestions();
    updateDisplay();
};