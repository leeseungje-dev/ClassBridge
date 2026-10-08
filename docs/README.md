<!-- README.md 시작 -->

# ClassBridge

개인 강사가 학생별 수업 일정, 출결, 수업일보, 학습 관찰 기록을 관리하고 다음 수업 준비에 활용하는 웹 서비스입니다.

---

## 1. 만들 서비스

### 핵심 흐름

```text
회원가입 / 로그인
→ 학생 등록
→ 수업 예약
→ 출결 처리
→ 출석 수업 일보 작성
→ 학습 관찰 기록
→ 다음 수업 준비 정보 확인
```

### 사용자

- 개인 또는 소규모 외국어 강사
- 학생은 로그인하지 않으며 강사가 관리하는 데이터입니다.
- 첫 버전에서는 강사만 계정을 만듭니다.

---

## 2. 첫 버전에 포함할 기능

1. 강사 회원가입, 로그인, 로그아웃
2. 강사별 데이터 접근 제한
3. 학생 등록, 조회, 수정, 삭제
4. 학생별 단일 수업 예약
5. 수업 상태 변경
   - 예정
   - 출석
   - 취소
   - 결석
6. 출석 수업의 수업일보 작성과 수정
7. 출석 수업의 학습 관찰 기록
8. 학생별 다음 수업 준비 요약
   - 다음 예정 수업
   - 최근 수업 기록
   - 다음 수업 확인사항
   - 출석·취소·결석 횟수
   - 반복 학습 관찰 항목 횟수

### 첫 버전에서 만들지 않는 기능

- 학생, 학부모, 관리자 계정
- 소셜 로그인과 비밀번호 찾기
- 결제
- 알림, 채팅, 이메일, 문자
- 반복 수업 예약
- 일정 충돌 검사
- Google Calendar 연동
- 파일 업로드
- AI 분석과 학생 실력 자동 평가
- 다중 강사 협업

---

## 3. 개발 환경 및 기술

```text
VS Code
├─ HTML
├─ CSS
└─ JavaScript

Eclipse
└─ Java + Spring Boot

Oracle SQL Developer
└─ Oracle DB
```

### 인증

- 회원가입과 로그인은 이메일과 비밀번호를 사용합니다.
- 이메일은 중복 등록할 수 없는 고유 로그인 값으로 사용합니다.
- 비밀번호는 평문으로 저장하지 않고 안전하게 해시하여 저장합니다.
- Spring Security를 이용해 로그인한 강사를 인증하고, 자신의 데이터만 접근할 수 있도록 인가를 적용합니다.
- Session 또는 JWT 중 어떤 인증 방식을 사용할지는 구현 단계에서 학습 후 결정하며, 현재 기획에서는 특정 방식을 강제하지 않습니다.

---

## 4. 데이터 구조

```text
강사(User)
└── 학생(Student)
    └── 수업(Lesson)
        ├── 수업일보(LessonReport)
        └── 학습 관찰 기록(LearningObservation)
```

### User

| 필드 | 설명 |
|---|---|
| id | 강사 식별값 |
| email | 로그인 이메일, 중복 불가 |
| password | 해시한 비밀번호 |
| role | `INSTRUCTOR` |
| createdAt | 가입 시각 |

### Student

| 필드 | 설명 |
|---|---|
| id | 학생 식별값 |
| instructorId | 학생을 등록한 강사 |
| name | 학생 이름 |
| learningStartDate | 학습 시작일 |
| currentLevel | 현재 레벨 |
| instructorMemo | 강사용 메모 |
| createdAt | 등록 시각 |

### Lesson

| 필드 | 설명 |
|---|---|
| id | 수업 식별값 |
| studentId | 수업 대상 학생 |
| scheduledAt | 수업 일시 |
| status | 수업 상태 |
| statusMemo | 취소·결석 관련 메모 |
| createdAt | 등록 시각 |

수업 상태:

```text
SCHEDULED = 예정
ATTENDED = 출석
CANCELED = 취소
ABSENT = 결석
```

### LessonReport

| 필드 | 설명 |
|---|---|
| id | 수업일보 식별값 |
| lessonId | 연결된 수업 |
| lessonContent | 진행 내용 |
| studentResponse | 학생 반응 |
| instructorMemo | 강사 메모 |
| nextLessonChecklist | 다음 수업 확인사항 |
| createdAt | 작성 시각 |
| updatedAt | 수정 시각 |

### LearningObservation

| 필드 | 설명 |
|---|---|
| id | 관찰 기록 식별값 |
| lessonId | 연결된 수업 |
| category | 관찰 분류 |
| item | 세부 관찰 항목 |
| memo | 관찰 메모 |
| createdAt | 기록 시각 |

관찰 분류:

```text
GRAMMAR = 문법
EXPRESSION = 표현
PRONUNCIATION = 발음
SPEAKING = 말하기
LISTENING = 듣기
```

세부 항목 예시:

```text
문법: 조사, 시제, 어미, 존댓말
표현: 어휘 선택, 문장 연결
발음: 받침, 연음, 억양
말하기: 어휘 회상, 문장 구성
듣기: 숫자 이해, 핵심어 파악
```

---

## 5. 반드시 지킬 규칙

1. 강사는 자신의 학생과 하위 데이터만 볼 수 있고 수정·삭제할 수 있습니다.
2. 학생을 등록하면 현재 로그인한 강사가 자동으로 소유자가 됩니다.
3. 새 수업은 항상 `SCHEDULED` 상태로 시작합니다.
4. 수업 상태는 예정, 출석, 취소, 결석만 허용합니다.
5. 수업일지는 `ATTENDED` 상태에서만 작성하고 수정할 수 있습니다.
6. 수업 하나에는 수업일지 하나만 만들 수 있습니다.
7. 취소·결석 수업에는 정식 수업일지를 작성할 수 없습니다.
8. 학습 관찰 기록은 출석 수업에서만 작성할 수 있습니다.
9. 관찰 기록을 저장할 때는 관찰 항목을 하나 이상 선택해야 합니다.
10. 학생을 삭제하면 연결된 수업, 수업일지, 관찰 기록도 함께 삭제합니다.
11. 서비스는 관찰 횟수만 보여주며 학생 능력을 자동 평가하지 않습니다.

---

## 6. API 목록

### 인증

| 기능 | HTTP | 주소 |
|---|---|---|
| 회원가입 | POST | `/api/auth/signup` |
| 로그인 | POST | `/api/auth/login` |
| 로그아웃 | POST | `/api/auth/logout` |
| 내 정보 조회 | GET | `/api/auth/me` |

### 학생

| 기능 | HTTP | 주소 |
|---|---|---|
| 학생 목록 | GET | `/api/students` |
| 학생 등록 | POST | `/api/students` |
| 학생 상세 | GET | `/api/students/{studentId}` |
| 학생 수정 | PUT | `/api/students/{studentId}` |
| 학생 삭제 | DELETE | `/api/students/{studentId}` |
| 학생 준비 요약 | GET | `/api/students/{studentId}/summary` |

### 수업

| 기능 | HTTP | 주소 |
|---|---|---|
| 학생 수업 목록 | GET | `/api/students/{studentId}/lessons` |
| 수업 예약 | POST | `/api/students/{studentId}/lessons` |
| 수업 상세 | GET | `/api/lessons/{lessonId}` |
| 수업 일시 수정 | PUT | `/api/lessons/{lessonId}/schedule` |
| 수업 상태 변경 | PATCH | `/api/lessons/{lessonId}/status` |
| 상태 메모 수정 | PATCH | `/api/lessons/{lessonId}/status-memo` |

### 수업일보와 학습 관찰

| 기능 | HTTP | 주소 |
|---|---|---|
| 수업일보 조회 | GET | `/api/lessons/{lessonId}/report` |
| 수업일보 작성 | POST | `/api/lessons/{lessonId}/report` |
| 수업일보 수정 | PUT | `/api/lessons/{lessonId}/report` |
| 관찰 기록 목록 | GET | `/api/lessons/{lessonId}/observations` |
| 관찰 기록 추가 | POST | `/api/lessons/{lessonId}/observations` |
| 관찰 기록 삭제 | DELETE | `/api/observations/{observationId}` |

---

## 7. 화면 목록

1. 랜딩/서비스 소개 화면
2. 로그인 화면
3. 로그인 실패 안내
4. 회원가입 화면
5. 중복 계정 안내
6. 학생 목록 화면
7. 학생 등록 화면
8. 학생 상세 화면
9. 학생 정보 수정 화면
10. 수업 등록 화면
11. 수업 이력 목록 화면
12. 수업 상세 화면
13. 수업 상태 변경 화면
14. 상태 메모 입력 화면
15. 수업 일시 수정 화면
16. 수업일보 작성 화면
17. 수업일보 상세 화면
18. 학습 관찰 기록 화면
19. 비출석 수업 안내

### 학생 상세 화면에 보여줄 내용

- 학생 기본 정보
- 다음 예정 수업
- 최근 수업 기록
- 다음 수업 확인사항
- 출석·취소·결석 횟수
- 반복 학습 관찰 항목별 횟수
- 학생 수정·삭제 버튼
- 수업 예약 버튼
- 수업 이력 보기 버튼

---

## 8. 만드는 순서

### 1단계: 프로젝트 준비

- Spring Boot 프로젝트를 만듭니다.
- Oracle DB를 연결하고 Oracle SQL Developer에서 접속을 확인합니다.
- VS Code에서 HTML, CSS, JavaScript 프론트엔드 파일 구조를 준비합니다.
- Swagger를 연결합니다.
- 공통 오류 응답 형식을 만듭니다.

### 2단계: 회원가입과 로그인

- User 엔티티를 만듭니다.
- 이메일과 비밀번호를 사용하는 회원가입 API를 만듭니다.
- 이메일 중복 가입을 막습니다.
- 비밀번호를 안전하게 해시하여 저장합니다.
- Spring Security를 이용해 로그인과 로그아웃, 인증 상태 확인을 구현합니다.
- 로그인하지 않은 사용자의 보호된 기능 접근을 막습니다.
- Session 또는 JWT 방식은 이 단계에서 학습한 뒤 하나를 선택합니다.

### 3단계: 학생 관리

- Student 엔티티를 만듭니다.
- 학생 등록·목록·상세·수정·삭제 API를 만듭니다.
- 다른 강사의 학생 데이터는 접근할 수 없게 만듭니다.
- 학생 목록과 등록 화면을 만듭니다.

### 4단계: 수업과 출결

- Lesson 엔티티를 만듭니다.
- 수업 예약 API를 만듭니다.
- 수업 상태 변경 기능을 만듭니다.
- 취소·결석 메모 기능을 만듭니다.
- 학생별 수업 이력을 시간순으로 보여줍니다.

### 5단계: 수업일보

- LessonReport 엔티티를 만듭니다.
- 출석 수업에서만 수업일지를 저장할 수 있게 만듭니다.
- 수업당 하나의 일지만 저장하게 만듭니다.
- 수업일지 작성·상세·수정 화면을 만듭니다.

### 6단계: 학습 관찰

- LearningObservation 엔티티를 만듭니다.
- 관찰 분류와 세부 항목을 선택하게 만듭니다.
- 출석 수업에서만 저장되게 만듭니다.
- 수업일지 상세 화면에서 관찰 기록을 확인하게 만듭니다.

### 7단계: 다음 수업 준비 요약

- 가장 가까운 예정 수업을 조회합니다.
- 가장 최근 수업을 조회합니다.
- 최근 출석 수업의 다음 수업 확인사항을 조회합니다.
- 출석·취소·결석 횟수를 집계합니다.
- 관찰 항목별 기록 횟수를 집계합니다.
- 학생 상세 화면에 요약 정보를 표시합니다.

---

## 9. AI 코딩 도구에 처음 보낼 요청

아래 문장을 복사해 Cursor, Claude Code, Codex 등의 AI 코딩 도구에 전달합니다.

```text
Eclipse에서 Java와 Spring Boot를 사용하고, Oracle DB를 데이터베이스로 사용하는 ClassBridge 백엔드를 만들어줘. 프론트엔드는 VS Code에서 HTML, CSS, JavaScript로 구현할 예정이야.

ClassBridge는 개인 강사가 학생별 수업 일정, 출결, 수업일보, 학습 관찰 기록을 관리하는 웹 서비스다.

회원가입과 로그인은 이메일과 비밀번호를 사용한다. 이메일은 중복될 수 없으며, 비밀번호는 안전하게 해시해서 저장한다. 인증과 인가는 Spring Security를 사용하되, Session/JWT 방식은 아직 확정하지 말고 구현 단계에서 선택할 수 있게 설명해줘.

엔티티는 User, Student, Lesson, LessonReport, LearningObservation이다.

핵심 규칙:
- 로그인한 강사는 자신의 학생과 하위 데이터만 접근할 수 있다.
- Student는 User에 속한다.
- Lesson은 Student에 속한다.
- LessonReport는 Lesson당 하나만 만들 수 있다.
- LessonReport는 ATTENDED 상태 수업에서만 만들 수 있다.
- LearningObservation은 ATTENDED 상태 수업에서만 만들 수 있다.
- Lesson 상태는 SCHEDULED, ATTENDED, CANCELED, ABSENT다.
- Student 삭제 시 연결된 Lesson, LessonReport, LearningObservation도 삭제한다.

먼저 프로젝트 구조, 엔티티, enum, JPA 연관관계, DTO, 공통 오류 처리, 데이터베이스 설정을 만들어줘.
각 파일의 역할과 실행 방법도 함께 설명해줘.
```

---

## 10. 완성 확인 목록

- [ ] 회원가입과 로그인
- [ ] 강사별 데이터 접근 제한
- [ ] 학생 등록·조회·수정·삭제
- [ ] 수업 예약
- [ ] 수업 상태 변경
- [ ] 취소·결석 메모
- [ ] 출석 수업 일보 작성
- [ ] 비출석 수업 일보 작성 차단
- [ ] 학습 관찰 기록
- [ ] 학생별 준비 요약
- [ ] Swagger 문서
- [ ] 핵심 흐름 테스트

<!-- README.md 끝 -->
