# ClassBridge 데이터 설계

갱신일: 2026-11-17

## 1. 전체 관계

- 한 강사는 여러 학생을 관리한다.
- 각 학생은 한 강사에게 속한다.
- 한 학생은 여러 수업을 가진다.
- 각 수업은 한 학생에게 속한다.
- 수업 한 건에는 수업일보를 0건 또는 1건 작성한다.
- 수업일보 한 건에는 학습 관찰 기록을 0건 이상 작성한다.

연결 구조:

APP_USERS
→ STUDENTS
→ LESSONS
→ LESSON_REPORTS
→ LEARNING_OBSERVATIONS

홈 달력과 수업 준비 화면은 기존 데이터를 조회한다.
화면별로 별도의 테이블에 데이터를 복사 저장하지 않는다.

## 2. 강사 계정

테이블 이름: APP_USERS  
Java 클래스 이름: User

강사 계정은 이메일과 비밀번호로 로그인한다.
학생은 로그인 계정을 만들지 않는다.

### 테이블 상세 설계

| 열 이름 | Oracle 자료형 | 제약조건 | 의미 |
|---|---|---|---|
| ID | NUMBER(19) | PRIMARY KEY | 강사 식별 번호 |
| EMAIL | VARCHAR2(255 CHAR) | NOT NULL, UNIQUE | 로그인 이메일 |
| PASSWORD | VARCHAR2(255 CHAR) | NOT NULL | 해시된 비밀번호 |
| ROLE | VARCHAR2(20 CHAR) | NOT NULL, 기본값 INSTRUCTOR, CHECK | 사용자 역할 |
| CREATED_AT | DATE | NOT NULL, 기본값 CURRENT_DATE | 가입 시각 |

### 저장 규칙

- ID는 APP_USERS_SEQ 시퀀스로 생성한다.
- 이메일 중복 등록을 허용하지 않는다.
- 비밀번호는 BCrypt로 해시하여 저장한다.
- ROLE은 현재 INSTRUCTOR만 허용한다.
- 이메일이 바뀌더라도 학생과의 연결을 유지하도록 별도의 ID를 사용한다.
- 테이블은 SQL로 직접 생성한다.

## 3. 학생 정보

테이블 이름: STUDENTS  
Java 클래스 이름: Student

로그인한 강사가 학생을 등록하고 관리한다.

### 테이블 상세 설계

| 열 이름 | Oracle 자료형 | 제약조건 | 의미 |
|---|---|---|---|
| ID | NUMBER(19) | PRIMARY KEY | 학생 식별 번호 |
| INSTRUCTOR_ID | NUMBER(19) | NOT NULL, FOREIGN KEY | 학생을 등록한 강사 ID |
| NAME | VARCHAR2(100 CHAR) | NOT NULL | 학생 이름 |
| LEARNING_START_DATE | DATE | 선택 입력 | 학습 시작일 |
| CURRENT_LEVEL | VARCHAR2(50 CHAR) | 선택 입력 | 현재 학습 수준 |
| INSTRUCTOR_MEMO | VARCHAR2(1000 CHAR) | 선택 입력 | 강사용 메모 |
| CREATED_AT | DATE | NOT NULL, 기본값 CURRENT_DATE | 등록 시각 |

### 관계와 접근 규칙

- STUDENTS.INSTRUCTOR_ID는 APP_USERS.ID를 참조한다.
- 학생 등록 시 로그인한 강사의 ID를 저장한다.
- 강사는 자신의 학생만 조회·수정·삭제할 수 있다.
- 연결된 수업이 있는 학생은 현재 외래 키 제약조건으로 삭제가 거절된다.
- 연결된 수업이 있는 학생의 삭제 정책과 사용자용 안내는 후속 작업으로 남긴다.

## 4. 수업 일정 및 출결

테이블 이름: LESSONS  
Java 클래스 이름: Lesson

### 테이블 상세 설계

| 열 이름 | Oracle 자료형 | 제약조건 | 의미 |
|---|---|---|---|
| ID | NUMBER(19) | PRIMARY KEY | 수업 식별 번호 |
| STUDENT_ID | NUMBER(19) | NOT NULL, FOREIGN KEY | 수업을 받는 학생 ID |
| LESSON_AT | DATE | NOT NULL | 수업 날짜와 시간 |
| STATUS | VARCHAR2(20 CHAR) | NOT NULL, 기본값 SCHEDULED, CHECK | 수업 상태 |
| STATUS_MEMO | VARCHAR2(1000 CHAR) | 선택 입력 | 지각·결석·취소 등의 상태 메모 |
| CREATED_AT | DATE | NOT NULL, 기본값 CURRENT_DATE | 일정 등록 시각 |

### 관계와 기본 규칙

- LESSONS.STUDENT_ID는 STUDENTS.ID를 참조한다.
- ID는 LESSONS_SEQ 시퀀스로 생성한다.
- 수업 등록 시 기본 상태는 SCHEDULED이다.
- LESSON_AT은 수업의 일정 일시다.
- CREATED_AT은 해당 일정을 시스템에 등록한 시각이다.
- Oracle DATE는 날짜와 시·분·초를 저장한다.
- 로그인한 강사는 자신의 학생에게 속한 수업만 조회·변경할 수 있다.
- 상태 메모는 LESSONS에, 수업 내용은 LESSON_REPORTS에 저장한다.

### 수업 상태

| DB 저장값 | 의미 | 화면 표시 |
|---|---|---|
| SCHEDULED | 예정 | 予定 |
| ATTENDED | 출석 | 出席 |
| LATE | 지각 | 遅刻 |
| CANCELLED | 취소 | キャンセル |
| ABSENT | 결석 | 欠席 |

- CK_LESSONS_STATUS 제약조건으로 위 다섯 가지 상태값만 허용한다.
- 취소·결석도 수업 이력으로 남기므로 상태 변경으로 처리한다.
- 일보는 출석·지각 상태인 수업에 작성할 수 있다.
- 일본 시간 기준 수업 시작 전에는 출석·지각·결석으로 변경할 수 없다.
- 일보가 있는 수업은 출석·지각 사이에서만 상태를 변경할 수 있다.
- 일보가 있는 수업을 예정·결석·취소로 변경하는 것은 거절한다.
- 상태에 따른 업무 규칙은 Service에서 검사한다.

### 상태 메모

- 상태 메모는 선택 입력이며 최대 1000자다.
- 앞뒤 공백을 제거하고 빈 문자열은 null로 정리한다.
- 출결 통합 저장에서는 지각·결석·취소 상태에만 메모를 저장한다.
- 예정·출석으로 저장하면 상태 메모는 null로 저장한다.
- 상태 메모를 비우고 저장하면 기존 메모도 제거한다.

## 5. 수업일보 — 구현 완료

테이블 이름: LESSON_REPORTS  
Java 클래스 이름: LessonReport

### 테이블 상세 설계

| 열 이름 | Oracle 자료형 | 제약조건 | 의미 |
|---|---|---|---|
| ID | NUMBER(19) | PRIMARY KEY | 수업일보 식별 번호 |
| LESSON_ID | NUMBER(19) | NOT NULL, UNIQUE, FOREIGN KEY | 연결된 수업 ID |
| CONTENT | VARCHAR2(2000 CHAR) | NOT NULL | 수업에서 다룬 내용 |
| NEXT_CHECK | VARCHAR2(1000 CHAR) | 선택 입력 | 다음 수업에서 확인할 내용 |
| CREATED_AT | DATE | NOT NULL, 기본값 CURRENT_DATE | 최초 작성 시각 |
| UPDATED_AT | DATE | NOT NULL, 기본값 CURRENT_DATE | 마지막 수정 시각 |

### 관계와 저장 규칙

- LESSON_REPORTS.LESSON_ID는 LESSONS.ID를 참조한다.
- 수업 한 건에 일보는 최대 한 건이다.
- LESSON_ID의 UNIQUE 제약조건으로 중복 일보를 막는다.
- Service에서도 기존 일보의 존재 여부를 확인한다.
- ID는 LESSON_REPORTS_SEQ 시퀀스로 생성한다.
- CONTENT는 필수이며 최대 2000자다.
- NEXT_CHECK는 선택 입력이며 최대 1000자다.
- 학생 반응은 별도 필드에 중복 저장하지 않고 관찰 항목에 기록한다.
- CREATED_AT은 수정 시 변경하지 않는다.
- UPDATED_AT은 최초 저장 시 기본값으로 기록한다.
- 수정할 때 UPDATE SQL에서 UPDATED_AT을 CURRENT_DATE로 갱신한다.

## 6. 학습 관찰 기록 — 구현 완료

테이블 이름: LEARNING_OBSERVATIONS  
Java 클래스 이름: LearningObservation

### 테이블 상세 설계

| 열 이름 | Oracle 자료형 | 제약조건 | 의미 |
|---|---|---|---|
| ID | NUMBER(19) | PRIMARY KEY | 관찰 기록 식별 번호 |
| LESSON_REPORT_ID | NUMBER(19) | NOT NULL, FOREIGN KEY | 연결된 수업일보 ID |
| CATEGORY | VARCHAR2(30 CHAR) | NOT NULL, CHECK | 관찰 분류 |
| ITEM | VARCHAR2(100 CHAR) | NOT NULL | 관찰 항목 |
| MEMO | VARCHAR2(1000 CHAR) | NOT NULL | 구체적인 관찰 내용 |
| CREATED_AT | DATE | NOT NULL, 기본값 CURRENT_DATE | 최초 작성 시각 |
| UPDATED_AT | DATE | NOT NULL, 기본값 CURRENT_DATE | 마지막 수정 시각 |

### 관계와 저장 규칙

- LEARNING_OBSERVATIONS.LESSON_REPORT_ID는 LESSON_REPORTS.ID를 참조한다.
- 수업일보 한 건에 관찰 기록을 0건 이상 연결한다.
- LESSON_REPORT_ID에는 UNIQUE를 설정하지 않는다.
- ID는 LEARNING_OBSERVATIONS_SEQ 시퀀스로 생성한다.
- CATEGORY는 정해진 값 중 선택한다.
- ITEM과 MEMO는 강사가 직접 입력한다.
- ITEM은 필수이며 최대 100자다.
- MEMO는 필수이며 최대 1000자다.
- CREATED_AT은 유지하고 수정 시 UPDATED_AT을 갱신한다.
- 누적 관찰 이력은 기존 기록을 조회하며 별도로 복사 저장하지 않는다.

### 관찰 분류

| DB 저장값 | 의미 | 화면 표시 |
|---|---|---|
| GRAMMAR | 문법 | 文法 |
| EXPRESSION | 표현 | 表現 |
| PRONUNCIATION | 발음 | 発音 |
| SPEAKING | 회화 | 会話 |
| LISTENING | 듣기 | 聞き取り |
| OTHER | 기타 | その他 |

- CHECK 제약조건으로 위 여섯 가지 값만 허용한다.
- 동일한 분류의 관찰 항목을 한 일보에 여러 개 작성할 수 있다.
- 분류를 수정하면 해당 기록의 CATEGORY를 변경한다.
- 이후 누적 이력을 다시 조회하면 변경된 분류와 건수에 반영된다.

## 7. 출결·일보·관찰 기록의 처리 규칙

### 입력값 검사

- 필수 문자열은 Service에서 null과 공백 여부를 검사한다.
- 앞뒤 공백을 정리하고 각 문자열의 최대 길이를 검사한다.
- 관찰 항목이 없어도 일보를 저장할 수 있다.
- 관찰 항목을 추가했다면 분류·항목·내용은 모두 필수다.
- 수정 요청에 포함된 기존 관찰 ID가 해당 일보에 속하는지 확인한다.
- 같은 기존 관찰 ID가 중복 제출되지 않도록 검사한다.

### 출결 통합 저장

- LessonAttendanceService에서 출결과 일보 저장을 조정한다.
- 출석·지각 저장 시 수업 내용을 필수로 검사하고 일보도 함께 저장한다.
- 기존 일보가 없으면 등록하고, 있으면 수정한다.
- 출결과 일보·관찰 기록의 변경을 하나의 트랜잭션으로 처리한다.
- 일부 처리에 실패하면 전체 변경을 롤백한다.
- 예정·결석·취소 저장 시 입력 중인 일보 내용은 저장하지 않는다.
- 기존 일보를 자동 삭제하지 않는다.
- 일보가 있으면 예정·결석·취소로의 상태 변경 자체를 거절한다.

### 일보와 관찰 기록 수정

- 일보와 관찰 항목을 함께 수정한다.
- 새 관찰 항목은 INSERT한다.
- 기존 관찰 항목의 변경 내용은 UPDATE한다.
- 기존 관찰 항목 중 제출 목록에서 제거된 항목은 DELETE한다.
- 관찰 항목을 모두 제거해도 일보 자체는 유지한다.
- 전체 변경은 하나의 트랜잭션으로 처리한다.

### DB 제약조건과 Service의 역할

- 외래 키는 데이터 간 연결을 보장한다.
- UNIQUE는 수업별 일보 중복을 방지한다.
- CHECK는 저장 가능한 상태와 분류값을 제한한다.
- 강사별 접근 권한, 수업 시작 시각 제한, 일보 작성 가능 상태는 Service와 조회 SQL에서 검사한다.
- 현재 DB 구조는 출석·지각 수업에 일보가 없는 상태도 허용한다.
- 다만 현재 출결 입력 화면의 통합 저장은 출석·지각과 일보를 함께 저장한다.

## 8. 접근 권한

- 로그인한 강사의 학생에게 속한 데이터만 조회·변경할 수 있다.
- 요청에서 강사 ID를 직접 받지 않고 로그인 사용자 정보에서 확인한다.
- 수업은 LESSONS → STUDENTS 연결로 소유 강사를 확인한다.
- 일보는 LESSON_REPORTS → LESSONS → STUDENTS 연결로 확인한다.
- 관찰 기록은 LEARNING_OBSERVATIONS → LESSON_REPORTS → LESSONS → STUDENTS 연결로 확인한다.
- 달력과 홈 일정 조회에도 강사 ID 조건을 적용한다.
- 다른 일보에 속한 관찰 ID로 수정·삭제할 수 없도록 검사한다.

## 9. 홈 일정 조회

### 기본 표시 날짜

- 일본 시간 기준 오늘 00:00부터 조회한다.
- SCHEDULED 수업이 있는 가장 가까운 날짜를 선택한다.
- 해당 날짜가 없으면 표시할 기본 수업일은 null이다.
- 선택한 날짜의 수업 목록은 모든 상태를 포함한다.
- 오늘 예정 수업이 하나도 남지 않으면 다음 예정 수업일을 선택한다.

### 날짜별 수업 목록

- 선택한 날짜 00:00 이상, 다음 날 00:00 미만으로 조회한다.
- 로그인한 강사의 수업만 조회한다.
- 학생 이름을 함께 조회한다.
- 일보는 LEFT JOIN하여 일보가 없는 수업도 포함한다.
- 수업 일시 오름차순, 수업 ID 오름차순으로 정렬한다.
- 달력에서 선택한 날짜에도 같은 조회를 사용한다.
- 선택한 날짜에 수업이 없으면 빈 목록을 반환한다.

### 미작성 일보 목록

- STATUS가 ATTENDED 또는 LATE인 수업을 대상으로 한다.
- 연결된 일보가 없는 수업만 조회한다.
- 로그인한 강사의 수업만 조회한다.
- 수업 일시 오름차순, 수업 ID 오름차순으로 정렬한다.
- 일보 작성 후에는 목록에서 제외된다.

## 10. 달력 조회

- 조회할 월은 Java의 YearMonth로 다룬다.
- 해당 월 1일 00:00 이상, 다음 달 1일 00:00 미만으로 조회한다.
- 로그인한 강사의 수업만 조회한다.
- SCHEDULED·ATTENDED·LATE 상태만 점 표시 대상에 포함한다.
- CANCELLED·ABSENT 상태만 있는 날짜는 점 표시 대상에서 제외한다.
- TRUNC로 수업 일시를 날짜 단위로 처리한다.
- DISTINCT로 같은 날짜의 중복을 제거한다.
- 날짜 오름차순으로 반환한다.
- 달력 날짜와 빈칸은 Controller에서 생성한다.
- 달력 표시용 테이블은 별도로 만들지 않는다.
- 점 표시 조건과 날짜별 수업 목록 조회 조건은 구분한다.
- 따라서 점이 없는 날도 선택하면 결석·취소 수업을 조회할 수 있다.

## 11. 수업 준비 조회

### 직전 출석·지각 수업

- 선택한 수업의 학생과 동일한 학생의 수업을 조회한다.
- 선택한 수업 일시보다 앞선 수업만 대상으로 한다.
- STATUS가 ATTENDED 또는 LATE인 수업만 대상으로 한다.
- 수업 일시 내림차순, 수업 ID 내림차순으로 정렬하여 한 건을 조회한다.
- 해당 수업의 일보를 조회한다.
- 직전 수업의 일보가 없다고 더 오래된 일보로 대체하지 않는다.
- 일보가 없으면 미작성 상태로 표시한다.
- 일보가 있으면 CONTENT와 NEXT_CHECK를 수업 준비 정보로 표시한다.

### 누적 관찰 기록

- 선택한 수업과 같은 학생의 이전 관찰 기록을 조회한다.
- 연결된 수업 일시가 선택한 수업 일시보다 앞선 기록만 포함한다.
- 직전 수업의 일보 유무와 관계없이 조회한다.
- 수업 일시 내림차순, 수업 ID 내림차순, 관찰 ID 오름차순으로 정렬한다.
- Service에서 CATEGORY별로 묶는다.
- LinkedHashMap으로 문법·표현·발음·회화·듣기·기타 순서를 유지한다.
- 기록이 없는 분류는 결과에 넣지 않는다.
- 분류별 건수는 조회한 목록의 크기로 계산한다.
- 최근 3건과 나머지 기록의 펼치기는 화면에서 처리한다.
- 현재는 누적 기록 전체를 조회하며 DB에서 3건으로 제한하지 않는다.
- 기록을 해결됨으로 표시하거나 숨기는 데이터 항목은 아직 없다.

## 12. 조회용 DTO

### HomeLesson

홈 일정과 미작성 일보 목록에 사용할 조회 결과를 담는다.

- id
- studentId
- studentName
- lessonAt
- status
- lessonReportId

수업·학생·일보를 연결한 조회 결과이며 별도 테이블은 없다.

### ObservationHistory

수업 준비 화면의 누적 관찰 기록 조회 결과를 담는다.

- id
- lessonId
- lessonAt
- category
- item
- memo

관찰 기록과 연결된 수업 일시·수업 ID를 함께 전달한다.
별도 테이블은 없다.

## 13. 날짜와 시간

- 업무상 오늘 날짜와 현재 시각은 Asia/Tokyo 기준으로 계산한다.
- 수업 일시는 Oracle DATE에 저장한다.
- Oracle DATE 자체에는 시간대 정보가 없다.
- Java에서는 수업 일시를 LocalDateTime으로 다룬다.
- 달력 날짜는 LocalDate, 조회할 월은 YearMonth로 다룬다.
- 일별·월별 조회는 시작 시각 이상, 다음 기간 시작 시각 미만으로 처리한다.
- CREATED_AT과 UPDATED_AT의 CURRENT_DATE는 Oracle 세션 시간대를 따른다.

## 14. 삭제 정책

- 현재 외래 키에는 ON DELETE CASCADE를 설정하지 않는다.
- 부모 데이터 삭제 시 하위 데이터가 자동으로 삭제되지 않는다.
- 일보 수정에서 제거한 관찰 항목은 명시적으로 삭제한다.
- 관찰 기록을 모두 제거해도 일보는 유지한다.
- 수업일보 자체를 삭제하는 기능은 아직 제공하지 않는다.
- 연결된 수업이 있는 학생의 삭제 정책과 사용자 안내는 후속 작업이다.
- 일보 삭제와 출결 정정은 함께 검토한다.
- 해결된 관찰 기록의 삭제 또는 해결됨 처리 방식은 아직 확정하지 않았다.

## 15. DB 변경 관리

- 테이블과 제약조건은 SQL로 직접 관리한다.
- DB 변경 SQL은 docs/sql/에 저장한다.
- 로컬 DB와 운영 DB는 별도로 관리한다.
- 애플리케이션 배포에 필요한 DB 변경은 운영 DB에도 반영한다.
- 지각 상태 추가는 006_add_late_lesson_status.sql로 관리한다.
- 로컬과 운영 DB의 CK_LESSONS_STATUS에 LATE 반영을 완료했다.

## 16. 후속 검토 사항

- 학생별 수업 목록의 연도·월별 조회
- 학생 상세 준비 중 영역의 조회 규칙
- 출결만 먼저 저장하고 일보를 나중에 작성하는 흐름
- 홈 일정에 표시할 수업 상태 범위
- 해결된 관찰 기록의 정리 방식
- 누적 관찰 기록이 많아질 경우 조회 범위와 페이지 처리
- 일보 삭제와 출결 정정 정책
- 연결된 수업이 있는 학생의 삭제 정책

출결과 일보의 저장 방식은 현재 통합 저장을 유지한다.
전체 사용 흐름을 확인한 뒤 변경 필요성을 판단한다.