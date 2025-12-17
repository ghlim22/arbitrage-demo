// 설정
const API_URL = 'http://localhost:8080/api/cycle/list/possible';
const UPDATE_INTERVAL_MS = 5000; // 5초
const CONTAINER_ID = 'table-container';

/**
 * 데이터를 가져와서 HTML 테이블로 변환하여 출력합니다.
 */
async function fetchAndRenderData() {
    const container = document.getElementById(CONTAINER_ID);
    if (!container) {
        console.error(`HTML 요소 ID '${CONTAINER_ID}'를 찾을 수 없습니다.`);
        return;
    }

    try {
        // 1. 데이터 가져오기 (GET 요청)
        const response = await fetch(API_URL);

        if (!response.ok) {
            // HTTP 오류 처리
            throw new Error(`HTTP 오류! 상태 코드: ${response.status}`);
        }

        const data = await response.json();

        // 2. 테이블 생성 및 HTML 업데이트
        const tableHtml = createTable(data);
        container.innerHTML = tableHtml;

        // 3. 마지막 갱신 시간 업데이트
        updateLastUpdated();

    } catch (error) {
        console.error('데이터를 가져오거나 렌더링하는 중 오류 발생:', error);
        container.innerHTML = `<p class="error">데이터 로드 실패: ${error.message}</p>`;
    }
}

/**
 * JSON 데이터를 기반으로 테이블 HTML 문자열을 생성합니다.
 * @param {Array<Object>} data - API에서 받은 데이터 배열
 * @returns {string} 생성된 테이블 HTML 문자열
 */
function createTable(data) {
    if (!data || data.length === 0) {
        return '<p>현재 유효한 차익 거래 사이클이 없습니다.</p>';
    }

    // 테이블 시작
    let html = '<table>';

    // 테이블 헤더
    html += '<thead><tr>';
    html += '<th>순환 경로 (Path)</th>';
    html += '<th>예상 수익률 (Expected Return)</th>';
    html += '<th>발생 시각 (Timestamp)</th>';
    html += '</tr></thead>';

    // 테이블 본문
    html += '<tbody>';

    data.forEach(item => {
        // 1. 예상 수익률 포맷팅 및 색상 지정
        const expectedPercent = item.expected.toFixed(4) + '%';
        const profitClass = item.possible ? 'success' : 'error'; // 수익률이 양수일 경우 녹색

        // 2. 타임스탬프 포맷팅
        const ts = new Date(item.ts);
        const formattedTime = ts.toLocaleString(); // 로컬 시간 형식으로 변환

        const params = new URLSearchParams({
            path: item.path
        });

        html += `<tr>`;
        html += `<td><a href="http://localhost:8080/cycleInfo.html?${params.toString()}">${item.path}</a></td>`;
        html += `<td class="${profitClass}">${expectedPercent}</td>`;
        html += `<td>${formattedTime}</td>`;
        html += `</tr>`;
    });

    html += '</tbody>';
    html += '</table>';

    return html;
}

/**
 * 마지막 갱신 시간을 표시합니다.
 */
function updateLastUpdated() {
    const timeElement = document.getElementById('last-updated');
    if (timeElement) {
        timeElement.textContent = new Date().toLocaleTimeString();
    }
}


// --- 초기 실행 및 주기적 갱신 설정 ---

// 1. 페이지 로드 시 즉시 데이터 로드
fetchAndRenderData();

// 2. 5초마다 데이터 갱신 설정
setInterval(fetchAndRenderData, UPDATE_INTERVAL_MS);