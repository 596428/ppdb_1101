// Global variables
window.allCards = [];
window.currentSize = 1;
window.currentFilterCriteria = {};

// 이미지 경로 생성을 위한 헬퍼 함수 추가
function getImagePath(packName, imageName) {
    const langSuffix = window.LanguageManager.currentLanguage.toLowerCase();
    return `/dataset/${packName}_${langSuffix}/${imageName}.jpg`;
}

// Fetch card images
window.fetchCardImages = async function() {
    try {
        const response = await fetch('/api/cards');
        window.allCards = await response.json();
        window.renderCards(window.allCards);
    } catch (error) {
        console.error('Error fetching card data:', error);
    }
};

// Show card details
window.showCardDetails = function(event) {
    const imageName = event.target.dataset.imageName;
    fetch(`/api/card/${imageName}`)
        .then(response => response.json())
        .then(data => {
            window.showModal(data);
        })
        .catch(error => console.error('Error:', error));
};

// Search and filter cards
window.searchAndFilterCards = function(searchTerm, filterCriteria) {
    let filteredCards = window.allCards;
    
    if (searchTerm.trim()) {
        filteredCards = filteredCards.filter(card => {
            if (window.LanguageManager.currentLanguage === 'EN') {
                const cardName = card.image_name.split('_')[1].toLowerCase();
                return cardName.includes(searchTerm.toLowerCase());
            } else {
                const cardName = card.card_name;
                return cardName.includes(searchTerm.toLowerCase());
            }
        });
    }
    
    if (filterCriteria && Object.keys(filterCriteria).length > 0) {
        filteredCards = filteredCards.filter(card => {
            return Object.entries(filterCriteria).every(([key, value]) => {
                switch(key) {
                    case 'cardType':
                        return card.card_type === value;
                    case 'type':
                        return card.pokemon_type === value;
                    case 'sets':
                        return card.pack_name === value;
                    case 'sections':
                        return card.section_names && card.section_names.includes(value);
                    case 'rarity':
                        return card.rarity === value;
                    default:
                        return true;
                }
            });
        });
    }
    
    window.renderCards(filteredCards);
};

// Render cards (to be overridden in builder.js)
window.renderCards = function(cards) {
    const cardGallery = document.getElementById('card-gallery');
    if (!cardGallery) return;

    cardGallery.innerHTML = '';
    cards.forEach(card => {
        const cardItem = document.createElement('div');
        cardItem.className = 'card-item';
        const imgName = card.image_name;
        const [cardIndex, cardName] = imgName.split('_');
        const imagePath = getImagePath(card.pack_name, imgName);
        cardItem.innerHTML = `
            <img src="${imagePath}" alt="${card.card_name}" data-image-name="${imgName}">
            <p>${card.card_name}</p>
            <p>${card.pack_name} ${cardIndex}</p>
        `;
        cardItem.querySelector('img').addEventListener('click', window.showCardDetails);
        cardGallery.appendChild(cardItem);
    });
    window.updateSize();
};

// Update size (to be overridden in builder.js)
window.updateSize = function() {
    const cardGallery = document.getElementById('card-gallery');
    if (!cardGallery) return;

    const sizes = ['small', 'medium', 'large'];
    cardGallery.className = `card-gallery size-${sizes[window.currentSize]}`;
};

// Helper function to create energy icon elements
function createEnergyIcons(energyData) {
    return Object.entries(energyData).map(([type, count]) => {
        const iconContainer = document.createElement('span');
        for (let i = 0; i < count; i++) {
            const img = document.createElement('img');
            img.src = `/dataset/icon/${type}.png`;
            img.alt = type;
            img.className = 'energy-icon';
            iconContainer.appendChild(img);
        }
        return iconContainer.outerHTML;
    }).join('');
}

// Helper function to replace energy JSON in text with icons
function replaceEnergyJsonWithIcons(text) {
    return text.replace(/\{([^}]+)\}/g, (match, p1) => {
        try {
            const formattedJson = '{' + p1.replace(/(\w+):/g, '"$1":').replace(/\s/g, '') + '}';
            const energyData = JSON.parse(formattedJson);
            return createEnergyIcons(energyData);
        } catch (error) {
            console.error('Error parsing energy data:', error);
            return match;
        }
    });
}

// Show modal
window.showModal = function(cardData) {
    const modal = document.createElement('div');
    modal.className = 'modal';
    const imgName = cardData.image_name;
    const imagePath = getImagePath(cardData.pack_name, imgName);

    let content = `
        <div class="modal-content">
            <span class="close">&times;</span>
            <div class="modal-layout">
                <div class="card-image">
                    <img src="${imagePath}" alt="${cardData.card_name}">
                </div>
                <div class="card-details">
                    <h2>${cardData.card_name}</h2>
                    <p><span data-translate="modalPack">${LanguageManager.getText('modalPack')}</span> ${cardData.pack_name}</p>
                    <p><span data-translate="modalCardType">${LanguageManager.getText('modalCardType')}</span> ${cardData.card_type}</p>
                    <p><span data-translate="modalRarity">${LanguageManager.getText('modalRarity')}</span> ${cardData.rarity}</p>
                    <p><span data-translate="modalIllustrator">${LanguageManager.getText('modalIllustrator')}</span> ${cardData.illustrator}</p>
    `;

    if (cardData.card_type === 'Pokemon') {
        content += `
            <p><span data-translate="modalPokemon">${LanguageManager.getText('modalPokemon')}</span> ${cardData.stage || 'N/A'}</p>
            <p><span data-translate="modalHp">${LanguageManager.getText('modalHp')}</span> ${cardData.hp || 'N/A'}</p>
            <p><span data-translate="modalType">${LanguageManager.getText('modalType')}</span> <img src="/dataset/icon/${cardData.pokemon_type}.png" alt="${cardData.pokemon_type}" class="energy-icon"></p>
            <p><span data-translate="modalWeakness">${LanguageManager.getText('modalWeakness')}</span> <img src="/dataset/icon/${cardData.weakness_type}.png" alt="${cardData.weakness_type}" class="energy-icon"> (${cardData.weakness_value || 'N/A'})</p>
            <p><span data-translate="modalRetreat">${LanguageManager.getText('modalRetreat')}</span> ${Array(cardData.retreat_cost).fill('<img src="/dataset/icon/colorless.png" alt="colorless" class="energy-icon">').join('')}</p>
        `;
        if (cardData.ability_name && cardData.ability_effect) {
            content += `
                <div class="ability">
                    <h4><span data-translate="modalAbility">${LanguageManager.getText('modalAbility')}</span> ${cardData.ability_name}</h4>
                    <p>${replaceEnergyJsonWithIcons(cardData.ability_effect)}</p>
                </div>
            `;
        }
        if (cardData.movements && cardData.movements.length > 0) {
            content += `<h3><span data-translate="modalMoves">${LanguageManager.getText('modalMoves')}</span></h3>`;
            cardData.movements.forEach((movement, index) => {
                content += `
                    <div class="movement">
                        <h4><span data-translate="modalMove">${LanguageManager.getText('modalMove')}</span> ${index + 1}: ${movement.move_name}</h4>
                        <p><span data-translate="modalCost">${LanguageManager.getText('modalCost')}</span> ${createEnergyIcons(JSON.parse(movement.cost))}</p>
                `;
                if (movement.damage) {
                    content += `<p><span data-translate="modalDamage">${LanguageManager.getText('modalDamage')}</span> ${movement.damage}</p>`;
                }
                if (movement.effect) {
                    content += `<p><span data-translate="modalEffect">${LanguageManager.getText('modalEffect')}</span> ${replaceEnergyJsonWithIcons(movement.effect)}</p>`;
                }
                content += `</div>`;
            });
        } else {
            content += `<p>No moves available</p>`;
        }
    } else if (cardData.card_type === 'Trainer' || cardData.card_type === 'Supporter') {
        content += `
            <p><span data-translate="modalEffect">${LanguageManager.getText('modalEffect')}</span> ${replaceEnergyJsonWithIcons(cardData.trainer_effect || 'N/A')}</p>
        `;
    }

    if (cardData.section_names && cardData.section_names.length > 0) {
        content += `
            <p><span data-translate="modalSeries">${LanguageManager.getText('modalSeries')}</span> ${cardData.section_names.join(', ')}</p>
        `;
    }

    if (cardData.special_rule_name) {
        content += `
            <p><span data-translate="modalSpecialRule">${LanguageManager.getText('modalSpecialRule')}</span> ${cardData.special_rule_name}</p>
        `;
    }

    content += `
                </div>
            </div>
        </div>
    `;
    modal.innerHTML = content;
    document.body.appendChild(modal);

    modal.querySelector('.close').addEventListener('click', () => {
        document.body.removeChild(modal);
    });

    window.addEventListener('click', (event) => {
        if (event.target === modal) {
            document.body.removeChild(modal);
        }
    });
};

// Event listeners
document.addEventListener('DOMContentLoaded', function() {
    const searchInput = document.getElementById('search-input');
    const sizeDecreaseBtn = document.getElementById('size-decrease');
    const sizeIncreaseBtn = document.getElementById('size-increase');

    let searchTimeout;

    if (searchInput) {
        searchInput.addEventListener('input', function() {
            clearTimeout(searchTimeout);
            searchTimeout = setTimeout(() => {
                window.searchAndFilterCards(this.value, window.currentFilterCriteria);
            }, 1000);
        });
    }

    if (sizeDecreaseBtn) {
        sizeDecreaseBtn.addEventListener('click', () => {
            if (window.currentSize > 0) {
                window.currentSize--;
                window.updateSize();
            }
        });
    }

    if (sizeIncreaseBtn) {
        sizeIncreaseBtn.addEventListener('click', () => {
            if (window.currentSize < 2) {
                window.currentSize++;
                window.updateSize();
            }
        });
    }

    window.addEventListener('message', function(event) {
        if (event.data.type === 'filterApplied') {
            window.currentFilterCriteria = event.data.criteria;
            window.searchAndFilterCards(searchInput ? searchInput.value : '', event.data.criteria);
        }
    });

    // 언어 변경 이벤트 리스너 추가
    window.addEventListener('languageChanged', () => {
        // 카드 갤러리 다시 렌더링
        window.renderCards(window.allCards);
    });

    window.fetchCardImages();
});