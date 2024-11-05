(function() {
    const TRANSLATIONS = {
        'Pokemon': '포켓몬',
        'Trainer': '트레이너즈',
        'Supporter': '서포터',
        
        // 'grass': '풀',
        // 'fire': '불',
        // 'water': '물',
        // 'electric': '번개',
        // 'psychic': '에스퍼',
        // 'fighting': '격투',
        // 'darkness': '악',
        // 'metal': '강철',
        // 'dragon': '드래곤',
        // 'colorless': '무색'
    };

    function translateValue(value) {
        return TRANSLATIONS[value] || value;
    }

    // 이미지가 있는 옵션인지 확인
    function hasIcon(category, value) {
        const imageCategories = {
            'type': ['grass', 'fire', 'water', 'electric', 'psychic', 
                    'fighting', 'darkness', 'metal', 'dragon', 'colorless'],
            'rarity': ['crown', 'diamond1', 'diamond2', 'diamond3', 'diamond4','star1', 'star2',
                      'star3'],
            'sets': true // 모든 팩은 이미지가 있다고 가정
        };
        
        if (category === 'type') return imageCategories.type.includes(value);
        if (category === 'rarity') return imageCategories.rarity.includes(value);
        if (category === 'sets') return true;
        return false;
    }

    function createFilterOption(category, value) {
        const option = document.createElement('div');
        option.className = 'filter-option';
        option.dataset.value = value;
        
        if (hasIcon(category, value)) {
            const img = document.createElement('img');
            img.src = `/dataset/icon/${value.toLowerCase()}.png`;
            img.alt = value;
            option.appendChild(img);
        } else {
            const span = document.createElement('span');
            span.textContent = window.LanguageManager.currentLanguage === 'KR' && TRANSLATIONS[value] 
                ? translateValue(value) 
                : value;
            option.appendChild(span);
        }
    
        return option;
    }

    function createFilterSection(title, category, options) {
        const section = document.createElement('div');
        section.className = 'filter-section';
        
        const heading = document.createElement('h3');
        heading.textContent = title;
        section.appendChild(heading);
        
        const optionsContainer = document.createElement('div');
        optionsContainer.className = 'filter-options';
        
        options.forEach(value => {
            const option = createFilterOption(category, value);
            option.addEventListener('click', () => {
                if (option.classList.contains('selected')) {
                    // 이미 선택된 상태면 선택 해제
                    option.classList.remove('selected');
                } else {
                    // 같은 섹션의 다른 옵션들의 선택 해제
                    optionsContainer.querySelectorAll('.filter-option').forEach(opt => {
                        opt.classList.remove('selected');
                    });
                    // 현재 옵션 선택
                    option.classList.add('selected');
                }
            });
            optionsContainer.appendChild(option);
        });
        
        section.appendChild(optionsContainer);
        return section;
    }

    function createFilterModal() {
        const modalHTML = `
            <div id="filter-modal" class="filter-modal">
                <div class="filter-modal-content">
                    <span class="filter-close">&times;</span>
                    <h2><span data-translate="filterTitle"></span></h2>
                    <div id="filter-sections">
                        <!-- Filter sections will be added here -->
                    </div>
                    <div class="filter-buttons">
                        <button type="button" id="reset-filter">
                            <span data-translate="filterReset"></span>
                        </button>
                        <button type="submit" id="apply-filter">
                            <span data-translate="filterApply"></span>
                        </button>
                    </div>
                </div>
            </div>
        `;
        document.body.insertAdjacentHTML('beforeend', modalHTML);
        if (window.LanguageManager) {
            window.LanguageManager.updateUI();
        }
    }

    function initializeFilters() {
        const filterSections = document.getElementById('filter-sections');
        if (filterSections) {
            filterSections.innerHTML = ''; // 기존 필터 삭제
        }
        fetch('/api/filter-options')
            .then(response => response.json())
            .then(data => {
                const filterSections = document.getElementById('filter-sections');
                
                // 카드 타입 필터
                filterSections.appendChild(
                    createFilterSection('카드 타입', 'cardType', data.cardTypes)
                );
                
                // 포켓몬 타입 필터
                filterSections.appendChild(
                    createFilterSection('타입', 'type', data.pokemonTypes)
                );
                
                // 팩 필터
                filterSections.appendChild(
                    createFilterSection('팩', 'sets', data.sets)
                );
                
                // 시리즈 필터
                filterSections.appendChild(
                    createFilterSection('시리즈', 'sections', data.sections)
                );
                
                // 레어도 필터
                filterSections.appendChild(
                    createFilterSection('레어도', 'rarity', data.rarities)
                );
            })
            .catch(error => console.error('Error loading filter options:', error));
    }

    function initFilterModal() {
        if (!document.getElementById('filter-modal')) {
            createFilterModal();
        }
        
        const filterModal = document.getElementById('filter-modal');
        const closeButton = filterModal.querySelector('.filter-close');
        const applyButton = document.getElementById('apply-filter');
        const resetButton = document.getElementById('reset-filter');
    
        closeButton.addEventListener('click', () => {
            filterModal.style.display = 'none';
        });
    
        window.addEventListener('click', (event) => {
            if (event.target === filterModal) {
                filterModal.style.display = 'none';
            }
        });
    
        resetButton.addEventListener('click', () => {
            document.querySelectorAll('.filter-option').forEach(option => {
                option.classList.remove('selected');
            });
        });
    
        applyButton.addEventListener('click', () => {
            const filterCriteria = {};
            const categories = {
                1: 'cardType',
                2: 'type',
                3: 'sets',
                4: 'sections',
                5: 'rarity'
            };
    
            // 각 섹션에서 선택된 옵션 수집
            document.querySelectorAll('.filter-section').forEach((section, index) => {
                const selectedOption = section.querySelector('.filter-option.selected');
                if (selectedOption) {
                    const key = categories[index + 1];
                    filterCriteria[key] = selectedOption.dataset.value;
                }
            });
    
            // 빈 값 제거
            Object.keys(filterCriteria).forEach(key => {
                if (filterCriteria[key] === '') {
                    delete filterCriteria[key];
                }
            });
    
            // console.log('Applied filters:', filterCriteria); // 디버깅용
    
            window.postMessage({ type: 'filterApplied', criteria: filterCriteria }, '*');
            filterModal.style.display = 'none';
        });
    }

    function addFilterButtonListener() {
        const filterButton = document.getElementById('filter-button');
        if (filterButton) {
            filterButton.addEventListener('click', () => {
                const filterModal = document.getElementById('filter-modal');
                if (filterModal) {
                    filterModal.style.display = 'block';
                }
            });
        }
    }

    function initialize() {
        initFilterModal();
        addFilterButtonListener();
        initializeFilters();
    }

    window.addEventListener('languageChanged', () => {
        initializeFilters();
    });

    if (document.readyState === 'loading') {
        window.addEventListener('languageManagerReady', initialize);
    } else {
        if (window.LanguageManager) {
            initialize();
        } else {
            window.addEventListener('languageManagerReady', initialize);
        }
    }

    window.initFilterModal = initFilterModal;
    window.addFilterButtonListener = addFilterButtonListener;
})();