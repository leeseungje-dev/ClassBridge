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