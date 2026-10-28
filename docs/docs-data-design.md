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