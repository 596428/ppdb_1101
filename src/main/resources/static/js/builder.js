document.addEventListener('DOMContentLoaded', function() {
    const clearDeckButton = document.getElementById('clear-deck');
    const deckList = document.getElementById('deckList');
    const cardGallery = document.getElementById('card-gallery');
    const tabButtons = document.querySelectorAll('.tab-button');
    const deckViews = document.querySelectorAll('.deck-view');
    const copyClipboardBtn = document.getElementById('copy-clipboard');
    let deck = {};
    

    // 이미지 경로 생성을 위한 헬퍼 함수
    function getImagePath(packName, imageName) {
        const langSuffix = window.LanguageManager.currentLanguage.toLowerCase();
        return `/dataset/${packName}_${langSuffix}/${imageName}.jpg`;
    }

    clearDeckButton.addEventListener('click', function() {
        if (confirm('덱리스트를 초기화 하시겠습니까?')) {
            deck = {};
            updateDeckList();
            updateCardCounts();
        }
    });

    // 카드 이미지 사전 로딩 함수 추가
    function preloadCardImages() {
        const cardImages = document.querySelectorAll('#deck-stacked-view img');
        return Promise.all(Array.from(cardImages).map(img => {
            if (img.complete) {
                return Promise.resolve();
            }
            return new Promise(resolve => {
                img.onload = resolve;
                img.onerror = resolve;
            });
        }));
    }

    document.getElementById('export-deck').addEventListener('click', function() {
        fetch('/api/log-download', {
            method: 'POST'
        }).catch(error => console.error('Error logging download:', error));

        const stackedView = document.getElementById('deck-stacked-view');
        const originalWidth = stackedView.style.width;
        const originalMaxWidth = stackedView.style.maxWidth;
    
        stackedView.style.width = 'fit-content';
        stackedView.style.maxWidth = 'none';
    
        html2canvas(stackedView,{ scale: 2 }).then(canvas => {
            stackedView.style.width = originalWidth;
            stackedView.style.maxWidth = originalMaxWidth;
    
            const deckName = document.getElementById('deck-name').value || `deck_${Date.now()}`;
            const link = document.createElement('a');
            link.download = `${deckName}.png`;
            link.href = canvas.toDataURL();
            link.click();
        });
    });
    

    tabButtons.forEach(button => {
        button.addEventListener('click', () => {
            copyClipboardBtn.disabled = (button.dataset.view !== 'text');
            const view = button.dataset.view;
            tabButtons.forEach(btn => btn.classList.remove('active'));
            deckViews.forEach(v => {
                v.classList.remove('active');
                v.style.display = 'none';
            });
            button.classList.add('active');
            const activeView = document.getElementById(`deck-${view}-view`);
            activeView.classList.add('active');
            activeView.style.display = 'block';
            updateDeckList();
        });
    });

    copyClipboardBtn.addEventListener('click', function() {
        const textView = document.getElementById('deck-text-view');
        const deckList = Array.from(textView.children).map(child => child.textContent).join('\n');
        navigator.clipboard.writeText(deckList).then(() => {
            alert('Deck list copied to clipboard!');
        });
    });

    cardGallery.addEventListener('dragstart', function(e) {
        const cardItem = e.target.closest('.card-item');  // card-item 요소 찾기
        if (cardItem) {
            e.dataTransfer.setData('text/plain', cardItem.dataset.imageName);
            cardItem.classList.add('dragging');
            deckList.classList.add('drag-highlight');
        }
    });

    cardGallery.addEventListener('dragend', function(e) {
        if (e.target.classList.contains('card-item')) {
            e.target.classList.remove('dragging');
            deckList.classList.remove('drag-highlight');
        }
    });

    deckList.addEventListener('dragenter', function(e) {
        e.preventDefault();
        this.classList.add('drag-over');
    });

    deckList.addEventListener('dragleave', function(e) {
        e.preventDefault();
        if (!e.currentTarget.contains(e.relatedTarget)) {
            this.classList.remove('drag-over');
        }
    });

    deckList.addEventListener('dragover', function(e) {
        e.preventDefault();
    });

    deckList.addEventListener('drop', function(e) {
        e.preventDefault();
        this.classList.remove('drag-over');
        this.classList.remove('drag-highlight');
        
        const imageName = e.dataTransfer.getData('text/plain');
        addCardToDeck(imageName);
    });

    function addCardToDeck(imageName) {
        if (!deck[imageName]) {
            deck[imageName] = 0;
        }
        if (deck[imageName] < 2) {
            deck[imageName]++;
            updateDeckList();
            updateCardCounts();
        }
    }

    function removeCardFromDeck(imageName) {
        if (deck[imageName] && deck[imageName] > 0) {
            deck[imageName]--;
            if (deck[imageName] === 0) {
                delete deck[imageName];
            }
            updateDeckList();
            updateCardCounts();
        }
    }

    function updateDeckInfo() {
        const totalCards = Object.values(deck).reduce((sum, count) => sum + count, 0);
        const pokemonCount = Object.entries(deck).reduce((sum, [imageName, count]) => {
            const card = window.allCards.find(c => c.image_name === imageName);
            return card && card.card_type === 'Pokemon' ? sum + count : sum;
        }, 0);
        const trainersCount = Object.entries(deck).reduce((sum, [imageName, count]) => {
            const card = window.allCards.find(c => c.image_name === imageName);
            return card && card.card_type === 'Trainer' ? sum + count : sum;
        }, 0);
        const supportersCount = Object.entries(deck).reduce((sum, [imageName, count]) => {
            const card = window.allCards.find(c => c.image_name === imageName);
            return card && card.card_type === 'Supporter' ? sum + count : sum;
        }, 0);
    
        document.getElementById('total-cards').textContent = totalCards;
        document.getElementById('pokemon-count').textContent = pokemonCount;
        document.getElementById('trainers-count').textContent = trainersCount;
        document.getElementById('supporters-count').textContent = supportersCount;
    
        const totalCardsElement = document.getElementById('total-cards');
        if (totalCards > 20) {
            totalCardsElement.style.color = 'red';
        } else {
            totalCardsElement.style.color = '';
        }
    }

    function updateDeckList() {
        const stackedView = document.getElementById('deck-stacked-view');
        const textView = document.getElementById('deck-text-view');

        stackedView.innerHTML = '';
        textView.innerHTML = '';

        const sortedCards = Object.entries(deck).sort((a, b) => {
            const cardA = window.allCards.find(c => c.image_name === a[0]);
            const cardB = window.allCards.find(c => c.image_name === b[0]);
            
            if (!cardA || !cardB) return 0;

            const typeOrder = { 'Pokemon': 0, 'Trainer': 1, 'Supporter': 2 };
            if (typeOrder[cardA.card_type] !== typeOrder[cardB.card_type]) {
                return typeOrder[cardA.card_type] - typeOrder[cardB.card_type];
            }

            if (cardA.card_type === 'Pokemon' && cardB.card_type === 'Pokemon') {
                const pokemonTypeOrder = ['grass', 'fire', 'water', 'electric', 'psychic', 'fighting', 'darkness', 'metal', 'dragon', 'colorless'];
                const typeIndexA = pokemonTypeOrder.indexOf(cardA.pokemon_type);
                const typeIndexB = pokemonTypeOrder.indexOf(cardB.pokemon_type);
                if (typeIndexA !== typeIndexB) {
                    return typeIndexA - typeIndexB;
                }
            }

            const indexA = parseInt(a[0].split('_')[0]);
            const indexB = parseInt(b[0].split('_')[0]);
            return indexA - indexB;
        });

        let currentRow;
        sortedCards.forEach(([imageName, count], index) => {
            const card = window.allCards.find(c => c.image_name === imageName);
            if (card) {
                if (index % 5 === 0) {
                    currentRow = document.createElement('div');
                    currentRow.className = 'stacked-row';
                    stackedView.appendChild(currentRow);
                }
                const cardDiv = document.createElement('div');
                cardDiv.className = 'stacked-card';

                cardDiv.innerHTML = `
                    <img src="${getImagePath(card.pack_name, imageName)}" alt="${imageName}">
                    <span class="card-count">${count}</span>
                    <div class="click-area left">
                        <div class="overlay">
                            <span class="icon">-</span>
                        </div>
                    </div>
                    <div class="click-area right">
                        <div class="overlay">
                            <span class="icon">+</span>
                        </div>
                    </div>
                `;
                cardDiv.querySelector('.click-area.left').addEventListener('click', (e) => {
                    e.stopPropagation();
                    removeCardFromDeck(imageName);
                });
                cardDiv.querySelector('.click-area.right').addEventListener('click', (e) => {
                    e.stopPropagation();
                    addCardToDeck(imageName);
                });
                currentRow.appendChild(cardDiv);
            }
        });

        sortedCards.forEach(([imageName, count]) => {
            const card = window.allCards.find(c => c.image_name === imageName);
            if (card) {
                const cardItem = document.createElement('div');
                cardItem.className = 'deck-card-item';
                cardItem.textContent = `${card.card_name} (${card.pack_name} ${imageName.split('_')[0]}) x${count}`;
                textView.appendChild(cardItem);
            }
        });

        updateDeckInfo();
    }

    function updateCardCounts() {
        const cardItems = cardGallery.querySelectorAll('.card-item');
        cardItems.forEach(cardItem => {
            const imageName = cardItem.dataset.imageName;
            const countElement = cardItem.querySelector('.card-count');
            countElement.textContent = deck[imageName] || 0;
        });
    }

    // window.renderCards = function(cards) {
    //     cardGallery.innerHTML = '';
    //     cards.forEach(card => {
    //         const cardItem = document.createElement('div');
    //         cardItem.className = 'card-item';
    //         cardItem.draggable = true; // 드래그 가능하도록 설정
    //         const imgName = card.image_name;
    //         const [cardIndex, cardName] = imgName.split('_');
    //         const imagePath = getImagePath(card.pack_name, imgName);
    //         cardItem.dataset.imageName = imgName;
    //         cardItem.innerHTML = `
    //             <img src="${imagePath}" alt="${card.card_name}" data-image-name="${imgName}">
    //             <p>${card.card_name}</p>
    //             <p>${card.pack_name} ${cardIndex}</p>
    //             <div class="card-controls">
    //                 <button class="decrease-card">-</button>
    //                 <span class="card-count">0</span>
    //                 <button class="increase-card">+</button>
    //             </div>
    //         `;
            
    //         // 드래그 중인 카드의 미리보기 이미지 설정
    //         const img = cardItem.querySelector('img');
    //         img.addEventListener('dragstart', function(e) {
    //             // 드래그 이미지 설정 (옵션)
    //             const dragImage = img.cloneNode(true);
    //             dragImage.style.width = '100px';
    //             dragImage.style.height = 'auto';
    //             dragImage.style.opacity = '0.7';
    //             document.body.appendChild(dragImage);
    //             e.dataTransfer.setDragImage(dragImage, 50, 70);
    //             setTimeout(() => document.body.removeChild(dragImage), 0);
    //         });

    //         cardItem.querySelector('img').addEventListener('click', window.showCardDetails);
    //         cardItem.querySelector('.decrease-card').addEventListener('click', () => removeCardFromDeck(imgName));
    //         cardItem.querySelector('.increase-card').addEventListener('click', () => addCardToDeck(imgName));
    //         cardGallery.appendChild(cardItem);
    //     });
    //     updateCardCounts();
    // };
    window.renderCards = function(cards) {
        cardGallery.innerHTML = '';
        cards.forEach(card => {
            const cardItem = document.createElement('div');
            cardItem.className = 'card-item';
            cardItem.draggable = true; // 드래그 가능하도록 설정
            const imgName = card.image_name;
            const [cardIndex, cardName] = imgName.split('_');
            const imagePath = getImagePath(card.pack_name, imgName);
            cardItem.dataset.imageName = imgName;
            cardItem.innerHTML = `
                <img src="${imagePath}" alt="${card.card_name}" data-image-name="${imgName}">
                <p class="card-name">${card.card_name}</p>
                <p>${card.pack_name} ${cardIndex}</p>
                <div class="card-controls">
                    <button class="decrease-card">-</button>
                    <span class="card-count">0</span>
                    <button class="increase-card">+</button>
                </div>
            `;
            
            // 드래그 중인 카드의 미리보기 이미지 설정
            const img = cardItem.querySelector('img');
            img.addEventListener('dragstart', function(e) {
                // 드래그 이미지 설정 (옵션)
                const dragImage = img.cloneNode(true);
                dragImage.style.width = '100px';
                dragImage.style.height = 'auto';
                dragImage.style.opacity = '0.7';
                document.body.appendChild(dragImage);
                e.dataTransfer.setDragImage(dragImage, 50, 70);
                setTimeout(() => document.body.removeChild(dragImage), 0);
            });
    
            // 이미지 클릭 시 덱에 추가
            cardItem.querySelector('img').addEventListener('click', () => addCardToDeck(imgName));
            // 이미지 우클릭 덱에서 제거
            cardItem.querySelector('img').addEventListener('contextmenu', (e) => {
                e.preventDefault(); // 기본 컨텍스트 메뉴 방지
                removeCardFromDeck(imgName);
            });
            
            // 카드 이름 클릭 시 상세정보 표시
            cardItem.querySelector('.card-name').addEventListener('click', () => {
                window.showCardDetails({ target: img });
            });
    
            cardItem.querySelector('.decrease-card').addEventListener('click', () => removeCardFromDeck(imgName));
            cardItem.querySelector('.increase-card').addEventListener('click', () => addCardToDeck(imgName));
            cardGallery.appendChild(cardItem);
        });
        updateCardCounts();
    };

    

    // 언어 변경 이벤트 리스너 추가
    window.addEventListener('languageChanged', () => {
        updateDeckList();  // 덱 리스트 다시 렌더링
        window.renderCards(window.allCards);  // 카드 갤러리 다시 렌더링
    });

    window.fetchCardImages();
});