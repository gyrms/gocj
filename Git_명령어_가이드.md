# Git 완전 초보 가이드 🐣

---

## Git이 뭐야?

> 쉽게 말하면 **"코드 저장 + 시간여행 기계"** 야
> 파일을 수정했다가 망해도 이전으로 돌아갈 수 있어!

```
비유하자면...
Git = 게임 세이브 포인트
커밋 = 세이브 하기
브랜치 = 다른 루트로 플레이해보기
```

---

## ⚙️ 1단계 - 처음 딱 한번만 설정

```bash
# 내 이름이랑 이메일 등록 (누가 작업했는지 표시됨)
git config --global user.name "홍길동"
git config --global user.email "hong@gmail.com"

# 잘 설정됐는지 확인
git config --list
# user.name=홍길동
# user.email=hong@gmail.com
```

---

## 📁 2단계 - 시작하기

### 새 프로젝트 시작할 때
```bash
# 내 컴퓨터에 새 폴더 만들고
mkdir my-project
cd my-project

# git 시작!
git init
# 결과: Initialized empty Git repository in /my-project/.git/
# 이제 이 폴더는 git이 관리해줘
```

### 남의 코드 가져올 때 (복사)
```bash
git clone https://github.com/gyrms/gocj.git

# 실행하면 gocj 폴더가 생기면서 파일이 다 들어와있어
# 마치 구글드라이브에서 폴더 통째로 다운받는 느낌
```

---

## 💾 3단계 - 저장하기 (가장 중요!)

> **핵심 개념** : Git은 2단계로 저장해
> 1. `add` → 저장할 파일 고르기 (장바구니에 담기)
> 2. `commit` → 실제로 저장하기 (결제하기)

```bash
# 현재 상태 확인 (뭐가 변경됐는지)
git status

# 아무것도 없을 때
# nothing to commit, working tree clean

# index.html 파일 수정했을 때
# Changes not staged for commit:
#   modified: index.html   ← 빨간색으로 표시
```

```bash
# 특정 파일만 장바구니에 담기
git add index.html

# 전체 파일 담기
git add .

# add 후 status 확인하면
# Changes to be committed:
#   modified: index.html   ← 초록색으로 바뀜 (담긴 상태)
```

```bash
# 실제 저장 (커밋)
git commit -m "로그인 버튼 색상 변경"
#              ↑ 여기에 어떤 작업인지 메시지 작성

# 결과:
# [main a1b2c3d] 로그인 버튼 색상 변경
# 1 file changed, 3 insertions(+), 1 deletion(-)
```

### 커밋 메시지 잘 쓰는 법
```bash
# ❌ 나쁜 예
git commit -m "수정"
git commit -m "ㅇㅇ"
git commit -m "asdf"

# ✅ 좋은 예
git commit -m "로그인 버튼 파란색으로 변경"
git commit -m "회원가입 이메일 유효성 검사 추가"
git commit -m "메인 페이지 레이아웃 버그 수정"
```

---

## 🌐 4단계 - GitHub에 올리기/받기

```bash
# 내 컴퓨터 → GitHub 에 올리기
git push origin main
# origin = GitHub 주소 별명
# main = 브랜치 이름

# GitHub → 내 컴퓨터 로 받기
git pull
# 팀원이 작업한 내용을 내 컴퓨터로 가져올 때
```

### 처음 GitHub 연결할 때
```bash
# GitHub에서 새 레포 만든 후
git remote add origin https://github.com/내이름/레포이름.git

# 연결됐는지 확인
git remote -v
# origin  https://github.com/내이름/레포이름.git (fetch)
# origin  https://github.com/내이름/레포이름.git (push)
```

---

## 🌿 5단계 - 브랜치 (독립 작업공간)

> **브랜치 비유**
> main = 완성된 게임 본판
> branch = 새 기능 테스트 서버
> 테스트 서버에서 맘껏 작업하다가 완성되면 본판에 합치는 것!

```bash
# 브랜치 목록 보기
git branch
# * main   ← * 표시가 현재 있는 브랜치

# 새 브랜치 만들기
git branch feature/login

# 그 브랜치로 이동
git checkout feature/login
# Switched to branch 'feature/login'

# 만들면서 바로 이동 (위 두 줄을 한번에)
git checkout -b feature/signup
```

```bash
# feature/login에서 작업 후 main에 합치기
git checkout main          # main으로 이동
git merge feature/login    # 합치기

# 합친 후 브랜치 삭제
git branch -d feature/login
```

---

## ⏪ 6단계 - 되돌리기

```bash
# 시나리오1: 파일 수정했는데 그냥 원래대로 되돌리고 싶어
git restore index.html
# index.html이 마지막 커밋 상태로 돌아감


# 시나리오2: git add 했는데 취소하고 싶어
git restore --staged index.html
# 스테이징(장바구니)에서 빼기. 파일 내용은 그대로


# 시나리오3: 커밋했는데 취소하고 싶어 (코드는 유지)
git reset --soft HEAD~1
# 커밋만 취소, 작업한 코드는 살아있음


# 시나리오4: 커밋도 취소하고 코드도 되돌리고 싶어 ⚠️
git reset --hard HEAD~1
# 커밋 + 코드 모두 이전으로 (복구 불가!)
```

---

## 🗂️ 7단계 - 임시 저장 (stash)

> **stash 비유**
> 작업하다가 급하게 다른 일 해야할 때
> 서랍에 잠깐 넣어두는 것!

```bash
# 시나리오:
# feature/login 작업 중인데 갑자기 main에서 버그 고쳐야 함!

git stash           # 현재 작업 서랍에 넣기
# Saved working directory: WIP on feature/login

git checkout main   # main으로 이동
# 버그 수정 작업...
git checkout feature/login   # 다시 돌아와서

git stash pop       # 서랍에서 꺼내기
# 아까 하던 작업이 다시 살아남!

git stash list      # 서랍 목록 보기
# stash@{0}: WIP on feature/login
# stash@{1}: WIP on main
```

---

## 📜 8단계 - 기록 보기

```bash
git log
# commit a1b2c3d4e5f6...
# Author: 홍길동 <hong@gmail.com>
# Date:   Mon Jan 15 2025
#
#     로그인 기능 추가

# 간략하게 한줄로
git log --oneline
# a1b2c3d 로그인 기능 추가
# e4f5g6h 회원가입 페이지 생성
# i7j8k9l 초기 커밋

# 브랜치 흐름까지 시각화
git log --oneline --graph
# * a1b2c3d (main) 브랜치 합치기
# |\
# | * b2c3d4e (feature/login) 로그인 기능 추가
# |/
# * i7j8k9l 초기 커밋
```

---

## 📌 실제 개발 하루 흐름

```bash
# 출근 후
git pull                              # 팀원 작업 내용 받기

# 새 기능 작업 시작
git checkout -b feature/마이페이지    # 새 브랜치 생성

# 코딩...코딩...코딩...

# 중간 저장
git add .
git commit -m "마이페이지 UI 작성"

# 또 코딩...

git add .
git commit -m "마이페이지 API 연동"

# 작업 완료 후 올리기
git push origin feature/마이페이지

# GitHub에서 PR(Pull Request) 생성
# → 팀원 코드 리뷰
# → main에 merge!
```

---

## ❓ 자주 하는 실수

```bash
# ❌ 실수1: main에서 바로 작업함
# → 항상 브랜치 만들고 작업하는 습관!

# ❌ 실수2: 커밋 안하고 push 하려함
# → add → commit → push 순서 지키기!

# ❌ 실수3: git reset --hard 남발
# → 코드 다 날아가니까 조심!

# ✅ 모르면 일단 이거
git status    # 현재 상태 확인이 기본!
```
