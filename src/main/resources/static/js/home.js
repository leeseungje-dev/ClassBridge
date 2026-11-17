// 로그인한 사용자의 홈에서만 실행
const dashboard = document.querySelector(".home-dashboard");

if (dashboard) {
    const message = document.getElementById("home-load-message");

    // 새 요청이 들어오면 이전 요청을 취소하기 위해 보관
    let activeRequest = null;

    // 현재 수업 목록의 날짜에 맞춰 달력 선택 표시
    function updateSelectedDate() {
        const schedule = document.getElementById("lesson-schedule");
        const selectedDate = schedule.dataset.selectedDate;

        dashboard.querySelectorAll(".calendar-day").forEach((button) => {
            button.setAttribute(
                "aria-pressed",
                String(button.dataset.date === selectedDate)
            );
        });
    }

    // 서버에서 HTML 조각을 받아 해당 영역만 교체
    async function loadFragment(url, targetId) {
        if (activeRequest) {
            activeRequest.abort();
        }

        const request = new AbortController();
        activeRequest = request;

        message.hidden = false;
        message.textContent = "読み込み中です。";

        try {
            const response = await fetch(url, {
                signal: request.signal,
                headers: {
                    "Accept": "text/html"
                }
            });

            // 로그인 만료 등으로 다른 화면으로 이동한 경우
            if (response.redirected) {
                throw new Error("ログイン状態を確認して、再度お試しください。");
            }

            if (!response.ok) {
                throw new Error("読み込めませんでした。もう一度お試しください。");
            }

            const html = await response.text();

            // 취소된 이전 요청의 결과는 화면에 반영하지 않음
            if (activeRequest !== request) {
                return;
            }

            const parsed = new DOMParser().parseFromString(html, "text/html");
            const replacement = parsed.getElementById(targetId);

            if (!replacement) {
                throw new Error("表示する内容を取得できませんでした。");
            }

            document.getElementById(targetId).replaceWith(replacement);
            updateSelectedDate();

            message.textContent = "";
            message.hidden = true;
        } catch (error) {
            // 연속 클릭으로 취소한 요청은 오류 안내를 표시하지 않음
            if (error.name === "AbortError" || activeRequest !== request) {
                return;
            }

            message.textContent =
                error instanceof TypeError
                    ? "通信できませんでした。接続を確認して再度お試しください。"
                    : error.message;
            message.hidden = false;
        } finally {
            if (activeRequest === request) {
                activeRequest = null;
            }
        }
    }

    // 달력 HTML이 교체되어도 클릭을 받을 수 있도록 바깥에 연결
    dashboard.addEventListener("click", (event) => {
        const button = event.target.closest(
            ".calendar-day, .calendar-month-button"
        );

        if (!button || !dashboard.contains(button)) {
            return;
        }

        if (button.matches(".calendar-day")) {
            // 선택한 날짜의 수업 목록 요청
            const url = new URL(dashboard.dataset.lessonsUrl, location.href);
            url.searchParams.set("date", button.dataset.date);

            loadFragment(url, "lesson-schedule");
        } else {
            // 선택한 월의 달력 요청
            const url = new URL(dashboard.dataset.calendarUrl, location.href);
            url.searchParams.set("month", button.dataset.month);

            loadFragment(url, "lesson-calendar");
        }
    });

    // 처음 홈을 열었을 때도 표시 중인 수업 날짜를 선택 표시
    updateSelectedDate();
}