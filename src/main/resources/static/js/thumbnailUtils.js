/**
 * 포켓몬 카드 썸네일 관리를 위한 유틸리티 함수들
 */
const ThumbnailUtils = {
    /**
     * 카드의 image_name으로부터 썸네일 파일명을 추출
     * @param {string} imageName - 카드의 image_name (예: "056_Blastoise ex")
     * @returns {string|null} 썸네일 파일명 (예: "blastoise")
     */
    getThumbnailName: function(imageName) {
        if (!imageName) return null;

        // ex 카드 처리
        let name = imageName.includes(' ex') ? 
            imageName.substring(0, imageName.indexOf(' ex')) : 
            imageName;
            
        // _로 분리하고 뒷부분 사용
        const parts = name.split('_');
        if (parts.length > 1) {
            return parts[1].toLowerCase();
        }
        return null;
    },

    /**
     * 썸네일 이미지의 URL 생성
     * @param {string} thumbnailName - 썸네일 파일명
     * @returns {string} 썸네일 이미지의 전체 URL
     */
    createThumbnailImageUrl: function(thumbnailName) {
        return `/dataset/thumbnail_image/${thumbnailName}.png`;
    },

    /**
     * 썸네일 HTML 요소 생성
     * @param {string} thumbnailName - 썸네일 파일명
     * @param {string} originalImageName - 원본 카드의 image_name
     * @param {boolean} selectable - 선택 가능 여부
     * @returns {HTMLElement} 썸네일 div 요소
     */
    createThumbnailElement: function(thumbnailName, originalImageName, selectable = false) {
        const div = document.createElement('div');
        div.className = 'thumbnail-item';
        if (selectable) {
            div.dataset.imageName = originalImageName;
        }
        
        const img = document.createElement('img');
        img.src = this.createThumbnailImageUrl(thumbnailName);
        img.alt = thumbnailName;
        img.loading = 'lazy'; // 이미지 레이지 로딩 적용
        
        div.appendChild(img);
        return div;
    },

    /**
     * Pokemon 타입 카드 필터링
     * @param {Array} cards - 카드 객체 배열
     * @returns {Array} Pokemon 타입 카드만 필터링된 배열
     */
    filterPokemonCards: function(cards) {
        return cards.filter(card => card && card.card_type === 'Pokemon');
    },

    /**
     * 썸네일 선택 토글 (최대 2개)
     * @param {HTMLElement} element - 토글할 썸네일 요소
     * @returns {boolean} 선택 상태
     */
    toggleThumbnailSelection: function(element) {
        const selectedThumbnails = document.querySelectorAll('.thumbnail-item.selected');
        
        if (element.classList.contains('selected')) {
            element.classList.remove('selected');
            return false;
        } else if (selectedThumbnails.length < 2) {
            element.classList.add('selected');
            return true;
        }
        return false;
    },

    /**
     * 선택된 썸네일 이름들 가져오기
     * @param {HTMLElement} container - 썸네일 컨테이너 요소
     * @returns {Array} 선택된 썸네일 이름들의 배열
     */
    getSelectedThumbnailNames: function(container) {
        const selected = container.querySelectorAll('.thumbnail-item.selected');
        return Array.from(selected)
            .map(item => this.getThumbnailName(item.dataset.imageName))
            .filter(name => name != null);
    }
};

// 전역으로 노출
window.ThumbnailUtils = ThumbnailUtils;