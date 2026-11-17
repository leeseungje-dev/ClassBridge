const statusSelect = document.getElementById("status");
const memoFields = document.getElementById("status-memo-fields");
const reportFields = document.getElementById("report-fields");
const statusMemo = document.getElementById("statusMemo");
const observationList = document.getElementById("observation-list");
const observationTemplate = document.getElementById("observation-template");
const addButton = document.getElementById("add-observation");

// 선택 상태에 맞춰 표시 영역과 필수 입력 여부 변경
function updateFields() {
    const status = statusSelect.value;
    const needsReport = status === "ATTENDED" || status === "LATE";
    const needsMemo =
        status === "LATE"
        || status === "ABSENT"
        || status === "CANCELLED";

    // 숨기기만 하므로 입력한 값은 유지됨
    reportFields.hidden = !needsReport;
    memoFields.hidden = !needsMemo;

    // 숨겨진 일보 입력란 때문에 저장이 막히지 않도록 처리
    reportFields.querySelectorAll("[data-report-required]").forEach(
        function (field) {
            field.required = needsReport;
        }
    );

    // 상태에 맞는 메모 예시 표시
    if (status === "LATE") {
        statusMemo.placeholder = "例）電車の遅延で10分遅刻";
    } else if (status === "ABSENT") {
        statusMemo.placeholder = "例）連絡なしで欠席";
    } else {
        statusMemo.placeholder = "例）体調不良のためキャンセル";
    }
}

// 상태를 바꿀 때 입력 영역 갱신
statusSelect.addEventListener("change", updateFields);

// 새 관찰 항목 추가
addButton.addEventListener("click", function () {
    const newFields = observationTemplate.content.cloneNode(true);
    observationList.appendChild(newFields);

    // 새 입력란에도 현재 상태의 필수 입력 규칙 적용
    updateFields();
});

// 기존 항목과 새로 추가한 항목 모두 같은 방식으로 제거
observationList.addEventListener("click", function (event) {
    const removeButton = event.target.closest("[data-remove-observation]");

    if (removeButton) {
        removeButton.closest("fieldset").remove();
    }
});

// 처음 화면을 열거나 입력 오류로 돌아왔을 때도 적용
updateFields();