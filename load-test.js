const url = 'https://sleepyslime.p-e.kr/api/products/list?page=0&size=20';
const CONCURRENT_USERS = 1000; // 동시에 접속할 유저 수

console.log(`🚀 [부하 테스트 시작] ${CONCURRENT_USERS}명의 유저가 동시에 접속합니다...`);

const startTime = Date.now();

const requests = Array.from({ length: CONCURRENT_USERS }).map(() => {
    return fetch(url)
        .then(res => {
            if (res.ok) {
                return Date.now() - startTime;
            } else {
                throw new Error(`상태 코드 ${res.status}`);
            }
        });
});

Promise.all(requests)
    .then((times) => {
        const totalTime = Date.now() - startTime;
        console.log(`✅ [테스트 완료]`);
        console.log(`⏱️ 모든 요청 처리에 걸린 총 시간: ${totalTime / 1000}초`);
    })
    .catch((err) => {
        console.error('❌ 테스트 중 에러 발생:', err);
    });
