# ClassBridge 데이터 설계

## 강사 계정

강사 계정은 이메일과 비밀번호로 로그인한다.
학생은 계정을 만들지 않는다.

| 항목 | 의미 | 규칙 |
|---|---|---|
| id | 강사 식별 번호 | 기본 키, 필수, 중복 불가 |
| email | 로그인 이메일 | 필수, 중복 불가 |
| password | 해시된 비밀번호 | 필수 |
| role | 사용자 역할 | INSTRUCTOR |
| created_at | 가입 시각 | 필수 |

## 관계

- 한 강사는 여러 학생을 관리한다.
- 각 학생은 한 강사에게 속한다.
- 학생은 강사의 id를 통해 소유 강사와 연결된다.

## 설계 이유

- 이메일이 바뀌더라도 학생과의 연결을 유지하도록 별도의 id를 사용한다.
- 비밀번호는 평문 대신 해시된 값을 저장한다.

## 강사 테이블 상세 설계

테이블 이름: APP_USERS
Java 클래스 이름: User

| 열 이름 | Oracle 자료형 | 제약조건 | 의미 |
|---|---|---|---|
| id | NUMBER(19) | PRIMARY KEY | 강사 식별 번호 |
| email | VARCHAR2(255 CHAR) | NOT NULL, UNIQUE | 로그인 이메일 |
| password | VARCHAR2(255 CHAR) | NOT NULL | 해시된 비밀번호 |
| role | VARCHAR2(20 CHAR) | NOT NULL, 기본값 INSTRUCTOR, CHECK로 INSTRUCTOR만 허용 | 사용자 역할 |
| created_at | DATE | NOT NULL, 기본값 CURRENT_DATE | 가입 시각 |

- id는 시퀀스로 생성한다.
- role은 첫 버전에서 INSTRUCTOR만 허용한다.
- 테이블 자동 생성 대신 SQL로 직접 생성한다.

## 학생 정보

학생은 로그인 계정을 만들지 않는다.
로그인한 강사가 학생을 등록하고 관리한다.

- 한 강사는 여러 학생을 관리한다.
- 각 학생은 한 강사에게 속한다.
- STUDENTS.INSTRUCTOR_ID는 APP_USERS.ID를 참조하는 외래 키다.
- 학생 등록 시 현재 로그인한 강사의 ID를 저장한다.
- 강사는 자신의 학생만 조회·수정·삭제할 수 있다.

| 열 이름 | Oracle 자료형 | 제약조건 | 의미 |
|---|---|---|---|
| ID | NUMBER(19) | PRIMARY KEY | 학생 식별 번호 |
| INSTRUCTOR_ID | NUMBER(19) | NOT NULL, FOREIGN KEY | 학생을 등록한 강사 ID |
| NAME | VARCHAR2(100 CHAR) | NOT NULL | 학생 이름 |
| LEARNING_START_DATE | DATE | 선택 입력 | 학습 시작일 |
| CURRENT_LEVEL | VARCHAR2(50 CHAR) | 선택 입력 | 현재 학습 수준 |
| INSTRUCTOR_MEMO | VARCHAR2(1000 CHAR) | 선택 입력 | 강사용 메모 |
| CREATED_AT | DATE | NOT NULL, 기본값 CURRENT_DATE | 등록 시각 |

## 수업 일정 및 출결

테이블 이름: LESSONS
Java 클래스 이름: Lesson

- 한 학생은 여러 수업을 가진다.
- 수업 한 건은 한 학생에게 속한다.
- LESSONS.STUDENT_ID는 STUDENTS.ID를 참조하는 외래 키다.
- 수업 등록 시 기본 상태는 SCHEDULED이다.
- 상태 메모와 수업일보는 별도로 관리한다.
- 로그인한 강사는 자신의 학생에게 속한 수업만 조회·변경할 수 있다.

### 수업 테이블 상세 설계

| 열 이름 | Oracle 자료형 | 제약조건 | 의미 |
|---|---|---|---|
| ID | NUMBER(19) | PRIMARY KEY | 수업 식별 번호 |
| STUDENT_ID | NUMBER(19) | NOT NULL, FOREIGN KEY | 수업을 받는 학생 ID |
| LESSON_AT | DATE | NOT NULL | 수업 날짜와 시간 |
| STATUS | VARCHAR2(20 CHAR) | NOT NULL, 기본값 SCHEDULED, CHECK | 수업 상태 |
| STATUS_MEMO | VARCHAR2(1000 CHAR) | 선택 입력 | 취소·결석 등의 상태 메모 |
| CREATED_AT | DATE | NOT NULL, 기본값 CURRENT_DATE | 일정 등록 시각 |

- ID는 LESSONS_SEQ 시퀀스로 생성한다.
- LESSON_AT은 실제 수업이 예정된 날짜와 시간이다.
- CREATED_AT은 해당 일정을 시스템에 등록한 시각이다.
- Oracle DATE는 날짜와 시·분·초를 저장한다.

### 수업 상태

| DB 저장값 | 의미 | 화면 표시 |
|---|---|---|
| SCHEDULED | 예정 | 予定 |
| ATTENDED | 출석 | 出席 |
| CANCELLED | 취소 | キャンセル |
| ABSENT | 결석 | 欠席 |

- CHECK 제약조건으로 위 네 가지 상태값만 허용한다.
- 취소·결석도 수업 이력으로 남기므로 상태 변경으로 처리한다.
- 정식 수업일보는 출석 상태인 수업에만 작성할 수 있다.
- 취소·결석 사유는 STATUS_MEMO에 기록한다.

## 수업일보 — 구현 예정

테이블 이름: LESSON_REPORTS
Java 클래스 이름: LessonReport

- 수업 한 건에 수업일보를 0건 또는 1건 작성한다.
- 수업일보 한 건은 반드시 수업 한 건에 속한다.
- LESSON_ID는 LESSONS.ID를 참조하는 외래 키다.
- LESSON_ID의 UNIQUE 제약조건으로 중복 일보를 막는다.
- 수업 내용과 다음 확인 사항을 기록한다.
- 학생 반응은 별도 필드에 중복 입력하지 않고 학습 관찰 항목에 기록한다.

### 수업일보 테이블 상세 설계

| 열 이름 | Oracle 자료형 | 제약조건 | 의미 |
|---|---|---|---|
| ID | NUMBER(19) | PRIMARY KEY | 수업일보 식별 번호 |
| LESSON_ID | NUMBER(19) | NOT NULL, UNIQUE, FOREIGN KEY | 연결된 수업 ID |
| CONTENT | VARCHAR2(2000 CHAR) | NOT NULL | 수업에서 다룬 내용 |
| NEXT_CHECK | VARCHAR2(1000 CHAR) | 선택 입력 | 다음 수업에서 확인할 내용 |
| CREATED_AT | DATE | NOT NULL, 기본값 CURRENT_DATE | 최초 작성 시각 |
| UPDATED_AT | DATE | NOT NULL, 기본값 CURRENT_DATE | 마지막 수정 시각 |

- ID는 LESSON_REPORTS_SEQ 시퀀스로 생성한다.
- CREATED_AT은 수정 시 변경하지 않는다.
- UPDATED_AT은 최초 저장 시 기본값으로 기록한다.
- 수정할 때는 UPDATE SQL에서 UPDATED_AT을 CURRENT_DATE로 갱신한다.

## 학습 관찰 기록 — 구현 예정

테이블 이름: LEARNING_OBSERVATIONS
Java 클래스 이름: LearningObservation

- 수업일보 한 건에 관찰 기록을 0건 이상 작성한다.
- 관찰 기록 한 건은 반드시 수업일보 한 건에 속한다.
- LESSON_REPORT_ID는 LESSON_REPORTS.ID를 참조하는 외래 키다.
- 일보 작성 화면에서 관찰 항목을 함께 입력한다.
- 학생 상세페이지에서는 같은 기록을 조회하여 누적 이력으로 보여준다.
- 학생 상세페이지용 데이터를 별도로 복사 저장하지 않는다.

### 학습 관찰 기록 테이블 상세 설계

| 열 이름 | Oracle 자료형 | 제약조건 | 의미 |
|---|---|---|---|
| ID | NUMBER(19) | PRIMARY KEY | 관찰 기록 식별 번호 |
| LESSON_REPORT_ID | NUMBER(19) | NOT NULL, FOREIGN KEY | 연결된 수업일보 ID |
| CATEGORY | VARCHAR2(30 CHAR) | NOT NULL, CHECK | 관찰 분류 |
| ITEM | VARCHAR2(100 CHAR) | NOT NULL | 관찰 항목 |
| MEMO | VARCHAR2(1000 CHAR) | NOT NULL | 구체적인 관찰 내용 |
| CREATED_AT | DATE | NOT NULL, 기본값 CURRENT_DATE | 최초 작성 시각 |
| UPDATED_AT | DATE | NOT NULL, 기본값 CURRENT_DATE | 마지막 수정 시각 |

- ID는 LEARNING_OBSERVATIONS_SEQ 시퀀스로 생성한다.
- LESSON_REPORT_ID에는 UNIQUE를 설정하지 않는다.
- 같은 일보에 여러 관찰 기록을 연결할 수 있다.
- CATEGORY는 정해진 값 중 선택한다.
- ITEM과 MEMO는 강사가 직접 입력한다.
- CREATED_AT은 유지하고 수정 시 UPDATED_AT을 갱신한다.

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

## 일보와 관찰 기록의 처리 규칙 — 구현 예정

### 입력과 저장

- 일보와 관찰 항목은 한 화면에서 입력하고 함께 저장한다.
- 관찰 항목이 없어도 일보를 저장할 수 있다.
- 관찰 항목을 추가했다면 분류·항목·내용은 모두 필수다.
- 필수 문자열은 Service에서 null과 공백 여부를 검사한다.
- 각 문자열의 최대 길이도 Service에서 검사한다.
- 전체 저장은 하나의 트랜잭션으로 처리한다.
- 일부 저장이 실패하면 전체 저장을 롤백한다.
- 수정 시 관찰 항목의 추가·변경·삭제를 지원한다.

### 접근 권한과 수업 상태

- 로그인한 강사의 학생에게 속한 기록만 조회·작성·수정할 수 있다.
- 관찰 기록 → 수업일보 → 수업 → 학생의 연결로 소유 강사를 확인한다.
- ATTENDED 상태인 수업에만 일보를 작성·수정한다.
- 일보가 있는 수업은 ATTENDED 이외의 상태로 변경하지 못하게 한다.
- 출석 상태 확인과 상태 변경 제한은 외래 키만으로 처리되지 않으므로
  Service에서 구현한다.

### 학생 상세페이지 조회

- 다음 수업은 현재 이후의 SCHEDULED 수업 중 가장 가까운 일시다.
- 최근 일보는 연결된 수업 일시를 기준으로 찾는다.
- 최근 일보의 NEXT_CHECK를 다음 확인 사항으로 표시한다.
- 관찰 이력은 수업 일시의 최신순으로 표시한다.
- CATEGORY로 분류별 조회를 제공한다.
- 기록이 없으면 안내 문구를 표시한다.

### 삭제 정책

- 현재 외래 키에는 ON DELETE CASCADE를 설정하지 않는다.
- 부모 데이터 삭제 시 하위 데이터가 자동으로 삭제되지 않는다.
- 관련 기록이 있는 학생의 삭제 정책과 사용자 안내는 별도로 정한 뒤 구현한다.