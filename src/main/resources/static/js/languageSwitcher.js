// 언어 설정 관리를 위한 전역 객체


const LanguageManager = {
    currentLanguage: 'KR',
    initialized: false,
    
    // 언어별 텍스트 매핑
    translations: {
        KR: {
            totalCards: '전체 카드:',
            pokemon: '포켓몬:',
            trainers: '트레이너즈:',
            supporter: '서포터:',
            clearDeck: '덱리스트 초기화',
            downloadDeck: '다운로드',
            copyClipboard: '텍스트(Copy to Clipboard)',
            imageView: '이미지',
            textView: '텍스트',
            filter: '필터',
            // 모달 관련 번역
            modalPack: '팩:',
            modalCardType: '카드 타입:',
            modalRarity: '레어도:',
            modalIllustrator: '일러스트레이터:',
            modalPokemon: '포켓몬:',
            modalHp: 'HP:',
            modalType: '타입:',
            modalWeakness: '약점:',
            modalRetreat: '후퇴:',
            modalAbility: '특성:',
            modalMoves: '기술:',
            modalMove: '기술',
            modalCost: '비용:',
            modalDamage: '데미지:',
            modalEffect: '효과:',
            modalSeries: '시리즈:',
            modalSpecialRule: '특수 규칙:',
            // 필터 관련 번역
            filterTitle: '필터',
            filterCardType: '카드타입',
            filterType: '타입(포켓몬)',
            filterSets: '팩',
            filterSections: '시리즈',
            filterRarity: '레어도',
            filterReset: '리셋',
            filterApply: '필터 적용'
        },
        EN: {
            totalCards: 'Total Cards:',
            pokemon: 'Pokemon:',
            trainers: 'Trainers:',
            supporter: 'Supporter:',
            clearDeck: 'Clear Deck',
            downloadDeck: 'Download',
            copyClipboard: 'Text (Copy to Clipboard)',
            imageView: 'Image',
            textView: 'Text',
            filter: 'Filter',

            // 모달 관련 번역
            modalPack: 'Pack:',
            modalCardType: 'Card Type:',
            modalRarity: 'Rarity:',
            modalIllustrator: 'Illustrator:',
            modalPokemon: 'Pokemon:',
            modalHp: 'HP:',
            modalType: 'Type:',
            modalWeakness: 'Weakness:',
            modalRetreat: 'Retreat:',
            modalAbility: 'Ability:',
            modalMoves: 'Moves:',
            modalMove: 'Move',
            modalCost: 'Cost:',
            modalDamage: 'Damage:',
            modalEffect: 'Effect:',
            modalSeries: 'Series:',
            modalSpecialRule: 'Special Rule:',
            // 필터 관련 번역
            filterTitle: 'Filter Cards',
            filterCardType: 'Card Type',
            filterType: 'Type',
            filterSets: 'Sets',
            filterSections: 'Sections',
            filterRarity: 'Rarity',
            filterReset: 'Reset',
            filterApply: 'Apply Filter'
        }
    },

    // async init() {
    //     // 현재 언어 설정 가져오기
    //     try {
    //         const response = await fetch('/api/current-language');
    //         const currentLanguage = await response.text();
    //         this.currentLanguage = currentLanguage;
    //         this.updateUI();
    //     } catch (error) {
    //         console.error('Error fetching current language:', error);
    //     }
    // },
    async init() {
        try {
            const response = await fetch('/api/current-language');
            if (response.ok) {
                const language = await response.text();
                // JSON 응답에서 따옴표 제거하고 설정
                this.currentLanguage = language.replace(/^"(.*)"$/, '$1');
                //this.updateUI();
                //return; // 성공적으로 설정됨
            }
        } catch (error) {
            console.error('Error fetching current language:', error);
            this.currentLanguage = 'EN';
        }

        this.initialized = true;
        this.updateUI();
        window.dispatchEvent(new Event('languageManagerReady'));
    },

    getText(key) {
        // 번역이 없는 경우 기본값 반환
        if (!this.translations[this.currentLanguage] || 
            !this.translations[this.currentLanguage][key]) {
            return this.translations['KR'][key] || key;
        }
        return this.translations[this.currentLanguage][key];
    },

    async setLanguage(lang) {
        try {
            const response = await fetch('/api/language', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ language: lang })
            });
            
            if (response.ok) {
                this.currentLanguage = lang;
                this.updateUI();
                // 카드 데이터 새로 로드
                await window.fetchCardImages();
                window.dispatchEvent(new Event('languageChanged'));
            } else {
                throw new Error('Language change failed');
            }
        } catch (error) {
            console.error('Error:', error);
            alert('언어 변경 중 오류가 발생했습니다.');
        }
    },

    getText(key) {
        return this.translations[this.currentLanguage][key] || key;
    },

    updateUI() {
        //console.log('Current language in updateUI:', this.currentLanguage);
        // UI 업데이트
        document.querySelectorAll('[data-translate]').forEach(element => {
            const key = element.getAttribute('data-translate');
            element.textContent = this.getText(key);
        });

        // 언어 버튼 활성화 상태 업데이트
        document.querySelectorAll('.lang-switch').forEach(button => {
            if (button.getAttribute('data-lang') === this.currentLanguage) {
                button.classList.add('active');
            } else {
                button.classList.remove('active');
            }
        });

        // 필터 관련 번역 처리
        window.enablePokemonTypeTranslation = (this.currentLanguage === 'KR');
    }
};

// 언어 변환 버튼 이벤트 리스너
document.addEventListener('DOMContentLoaded', () => {
    // 언어 매니저 초기화
    LanguageManager.init();
    // 언어 변환 버튼 이벤트 리스너
    const langButtons = document.querySelectorAll('.lang-switch');
    langButtons.forEach(button => {
        button.addEventListener('click', () => {
            LanguageManager.setLanguage(button.getAttribute('data-lang'));
        });
    });
});

window.LanguageManager = LanguageManager;