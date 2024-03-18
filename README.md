# TT Market - Tomorrow to Twentydays Market
<br />
<br />

![TT Market 대문 이미지](https://github.com/ryukyungwoo/D-YES-Backend/blob/main/images/banner.jpg)
<br />
<br />


## 프로젝트 기획 의도

![프로젝트 설명 이미지 1](https://github.com/ryukyungwoo/D-YES-Backend/blob/main/images/1.png)
![프로젝트 설명 이미지 2](https://github.com/ryukyungwoo/D-YES-Backend/blob/main/images/%EA%B8%B0%ED%9A%8D%EC%9D%98%EB%8F%84%202.png)

TT Market은 다양한 데이터 분석을 통해 농산물 가격을 예측하고, 사용자가 효과적인 구매 결정을 할 수 있도록 돕는 통합 쇼핑몰 서비스를 생각하고 제작하였습니다. <br />
다음날부터 2주 후까지의 가격을 제공한다고해서 tomorrow to twenty days 즉 ttmarket 입니다
일조량, 강수량, 온도, 기존 시세 등을 분석하여 농산물 가격 변동을 예측하는 알고리즘을 파이썬과 머신러닝으로 구현하였습니다.<br />
<br />

이 서비스는 실시간 가격 변동 및 예측 정보를 제공하여 사용자들이 더 합리적인 구매 결정을 내릴 수 있도록 합니다. <br />
또한, 사용자 리뷰 공유와 레시피 서비스를 제공하여 농산물을 효과적으로 활용할 수 있도록 하였습니다.<br />
<br />

TT Market을 통해 사용자들은 농산물 구매에서 보다 정보에 기반한 결정을 내릴 수 있게 되었습니다.<br />

<br />
<br />
<br />


## Tech Stack

#### Frontend
![React](https://img.shields.io/badge/-React-61DAFB?logo=react&logoColor=white)
![MUI](https://img.shields.io/badge/-MUI-007FFF?logo=mui&logoColor=white)
![Zustand](https://img.shields.io/badge/-Zustand-black?logo=zustand&logoColor=white)

#### Backend
![Java](https://img.shields.io/badge/-Java-007396?logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/-Spring%20Boot-6DB33F?logo=spring-boot&logoColor=white)
![Python](https://img.shields.io/badge/-Python-3776AB?logo=python&logoColor=white)
![FastAPI](https://img.shields.io/badge/-FastAPI-009688?logo=fastapi&logoColor=white)

#### Database
![MySQL](https://img.shields.io/badge/-MySQL-4479A1?logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/-Redis-DC382D?logo=redis&logoColor=white)
![JPA](https://img.shields.io/badge/-JPA-59666C?logo=jpa&logoColor=white)

#### Deploy
![EC2](https://img.shields.io/badge/-EC2-FF9900?logo=amazon-aws&logoColor=white)
![Git Actions](https://img.shields.io/badge/-Git%20Actions-2088FF?logo=github-actions&logoColor=white)
![Docker](https://img.shields.io/badge/-Docker-2496ED?logo=docker&logoColor=white)








<br />
<br />
<br />



## 프로젝트 기능 설명

### 시작 화면
---
<img src="https://github.com/ryukyungwoo/D-YES-Backend/blob/main/images/ttmarket.jpg" width="75%" /><br />  
서비스에 들어오면 상단에는 카테고리, 중단에는 크로셀과 신상품 알림, 하단에는 가격 예측 테이블이 위치하고 있습니다.<br /><br />
- 신상품 알림: <img src="https://github.com/ryukyungwoo/D-YES-Backend/blob/main/images/%EC%8B%A0%EC%83%81%ED%92%88%20%EC%B6%94%EC%B2%9C.png" width="75%" /><br /> 최근에 등록된 상품 4개가 추천됩니다.<br /><br />
- 가격 예측 테이블: <img src="https://github.com/ryukyungwoo/D-YES-Backend/blob/main/images/%EC%B0%A8%ED%8A%B81.png" width="75%" /> <img src="https://github.com/ryukyungwoo/D-YES-Backend/blob/main/images/%EC%B0%A8%ED%8A%B82.png" width="75%" /><br /> 표와 그래프 모드를 전환할 수 있는 토글과 각종 야채의 종류에 따라 그래프색이 달라집니다.<br /><br />

### 회원가입 및 로그인
---
<img src="https://github.com/ryukyungwoo/D-YES-Backend/blob/main/images/%ED%9A%8C%EC%9B%90%EA%B0%80%EC%9E%85%20%EB%B0%8F%20%EB%A1%9C%EA%B7%B8%EC%9D%B8.png" width="75%" /><br />  
회원 가입 및 로그인은 모두 oauth로 진행하도록 하였습니다. 최초 가입시 oauth 서버에서 최소한의 정보만 가져오게 하고, 전화번호나 주소 등 나머지 정보는 사용자의 의지에 따라 입력하도록 하였습니다.<br /><br />

### 상품 리스트
---
<img src="https://github.com/ryukyungwoo/D-YES-Backend/blob/main/images/%EC%83%81%ED%92%88%20%EC%9D%B8%EB%8D%B1%EC%8A%A4.png" width="75%" /><br />  
상품리스트는 상품의 재배 방법과 어떤 농가에서 생산된 제품인지, 제품 이미지와 리뷰 및 할인률을 볼 수 있습니다. 생산지나 재배 방법, 야채의 종류에 따라 필터링이 가능합니다.<br /><br />

### 공동구매
---
<img src="https://github.com/ryukyungwoo/D-YES-Backend/blob/main/images/%EA%B3%B5%EB%8F%99%EA%B5%AC%EB%A7%A4.png" width="75%" /><br />  
공동 구매 상품은 달성량에 따라 할인률이 커지는 상품입니다. 현재 참여인원과 남은 일자를 볼 수 있습니다.<br /><br />

### 상품 상세보기
---
<img src="https://github.com/ryukyungwoo/D-YES-Backend/blob/main/images/%EC%83%81%EC%84%B8%EB%B3%B4%EA%B8%B0.png" width="75%" /><br />  
상품 상세보기 페이지에서는 대표 이미지와 상세 이미지들, 기본적인 옵션, 개수 등을 지정하여 장바구니에 넣거나 바로 구입할 수 있습니다.<br /><br />
<img src="https://github.com/ryukyungwoo/D-YES-Backend/blob/main/images/%EC%83%81%ED%92%88%20%EC%83%81%EC%84%B8%20%EC%A0%95%EB%B3%B4.png" width="75%" /><br />  
하단에는 상품 상세 설명을 등록자가 직접 기입할 수 있습니다.<br /><br />

### 장바구니
---
<img src="https://github.com/ryukyungwoo/D-YES-Backend/blob/main/images/%EC%9E%A5%EB%B0%94%EA%B5%AC%EB%8B%88.png" width="75%" /><br />  
사용자가 상품을 담으면 리스트 형태로 서버에 임시 저장되며, 체크 박스와 -, + 버튼으로 상품의 상태를 업데이트 및 삭제할 수 있습니다. 가격 밑에는 전체상품 주문과 선택 상품 주문 버튼을 나누어 선택적 주문을 할 수 있게 하였습니다.<br /><br />

### 주문하기
---
<img src="https://github.com/ryukyungwoo/D-YES-Backend/blob/main/images/%EC%A3%BC%EB%AC%B8%ED%95%98%EA%B8%B0.png" width="75%" /><br />  
주문하기 페이지에서는 장바구니의 상품을 주문할 수 있으며, 주소나 전화번호 등 사용자 정보를 기입하거나 업데이트 할 수 있습니다.<br /><br />
<img src="https://github.com/ryukyungwoo/D-YES-Backend/blob/main/images/%EC%B9%B4%EC%B9%B4%EC%98%A4%EA%B2%B0%EC%A0%9C.png" width="75%" /><br />  
결제는 카카오 결제 API를 사용하였습니다.<br /><br />

### 주문 목록
---
<img src="https://github.com/ryukyungwoo/D-YES-Backend/blob/main/images/%EC%A3%BC%EB%AC%B8%EB%AA%A9%EB%A1%9D.png" width="75%" /><br />  
주문한 상품에 따라 배송중이라면 주문 취소 요청 버튼이, 주문이 완료되었다면 환불 버튼이 나옵니다. 리뷰하기 버튼도 주문 완료 후 활성화됩니다.<br /><br />

### 레시피
---
<img src="https://github.com/ryukyungwoo/D-YES-Backend/blob/main/images/%EB%A0%88%EC%8B%9C%ED%94%BC.png" width="75%" /><br />  
레시피 페이지에서는 요리의 제목과 이미지, 조리 내용 등을 기입할 수 있습니다. 댓글 기능을 통해 사용자 간 의견을 나눌 수 있습니다.<br /><br />

### 문의하기
---
<img src="https://github.com/ryukyungwoo/D-YES-Backend/blob/main/images/%EB%AC%B8%EC%9D%98.png" width="75%" /><br />  
사용자는 관리자에게 문의를 남길 수 있으며, 답변 완료 시 이메일 알림을 받습니다.<br /><br />
<img src="https://github.com/ryukyungwoo/D-YES-Backend/blob/main/images/%EB%AC%B8%EC%9D%98%20%EB%82%B4%EC%97%AD.png" width="75%" /><br />  
자신이 문의했던 내용과 답변을 확인할 수 있습니다.<br /><br />

### 관리자 페이지
---
<img src="https://github.com/ryukyungwoo/D-YES-Backend/blob/main/images/%EA%B4%80%EB%A6%AC%EC%9E%90%20%ED%8E%98%EC%9D%B4%EC%A7%80.png" width="75%" /><br />  
관리자는 메인 페이지에서 상품의 등록 현황, 주문 목록 등을 확인하고, 상품, 판매자, 주문 관리 등을 할 수 있습니다.
<br />  <br />  
## 기능 요약

<img src="https://github.com/ryukyungwoo/D-YES-Backend/blob/main/images/6.jpg" width="75%" /><br /><br />

<img src="https://github.com/ryukyungwoo/D-YES-Backend/blob/main/images/5.jpg" width="75%" /><br /><br />
<br />  <br />  <br />  
## 팀원 구성

| 정다운(팀장) | 유경우 | 김형진 | 윤주아 |
|:---:|:---:|:---:|:---:|
| ![정다운](프로필사진URL) | ![유경우](프로필사진URL) | ![김형진](프로필사진URL) | ![윤주아](프로필사진URL) |
| [GitHub](깃헙링크) | [GitHub](깃헙링크) | [GitHub](깃헙링크) | [GitHub](깃헙링크) |

## 진행 일정

![진행 일정](https://github.com/ryukyungwoo/D-YES-Backend/blob/main/images/%EC%A7%84%ED%96%89%EC%9D%BC%EC%A0%95.png)  
**2023년 8월 1일 → 2023년 10월 6일**

## 역할 분담

유경우
1 | Spring

확인

구글, 네이버 회원가입 및 로그인
accessToken 재발행
회원 탈퇴
결제

카카오 페이 결제
환불 및 부분 환불
결제 데이터 업신전송
조율

상품 조율
장바구니에서 조율
조율 히스
공동구매

공동 구매 등록 및 삭제
공동 구매 매입 및 회기
공동 구매 페이지
문의

문의 등록
문의 대화
문의 알람
예외처리

결제 실패 시 예외처리
2 | Machine Learning

데이터 선별

Granger causality test
Pearson collation test
Cross collation test
모델 성장 및 학습

윤주아
1 | React

확인

마이페이지 조회
마이페이지 수정
마이페이지 배송지 조회
마이페이지 배송지 수정
마이페이지 배송지 삭제
관리자 등록
회원 등록 조회
상품

관리자 상품 등록
관리자 상품 수정
관리자 판매 상품 목록 조회
관리자 판매 상품 상세
관리자 판매 상품 옵션 조회
이벤트 상품 등록
이벤트 상품 수정
이벤트 상품 목록 조회
이벤트 상품 상세
관리자 주문 목록 조회
관리자 주문 상세 조회
관리자 상품 환불 목록 조회
관리자 상품 환불
공구

공구 등록
공구 수정
공구 목록 조회
공구 상세
리뷰

리뷰 등록
레시피

레시피 등록
기타

관리자 페이지 사이드 바
메인 페이지 하단1

김형진

1 | React

확인

OAuth 로그인 페이지
회원 탈퇴
상품

상품 목록
상품 상세보기
조율

조율 창조 화면
사용자 배송지 정보 수정
조율 목록 조회
장바구니

장바구니 조회
장바구니에 상품 담기
장바구니의 상품 수정
장바구니의 상품 삭제 조정
결제

장바구니에서 상품 결제
상품 상세보기 페이지에서 상품 결제
결제 실패 시 처리
결제 환불 신청
리뷰

리뷰 등록
레시피

레시피 등록
리뷰

리뷰 등록
레시피

레시피 등록
기타

관리자 페이지 사이드 바 설정
메인 페이지 하단1 설정
이미지 처리

AWS S3
이미지 업송
이미지 리사이징
이미지 버전 관리
UI

Header
Footer
Carousel

정다운
1 | Spring

확인

카카오 로그인, 로그아웃
프로필 등록, 수정, 이미지
내일의 조율 화면
이메일 조율 화면
주소록 등록, 조회, 수정, 삭제, 선택
장바구니 화면 조회
신규 화면 조회
상품

관리자 상품 등록
관리자 상품 수정
관리자 판매 상품 목록 조회
관리자 판매 상품 상세
관리자 판매 상품 옵션 조회
사용자 판매 상품 상세 조회
사용자 판매 상품 목록 조회
사용자 판매 상품 바구니 조회
사용자 판매 상품 신규 목록 조회
공구

공구 등록
공구 수정
공구 목록 조회
공구 상세
공구 상세 이미지
문의

사용자 나의 문의 목록 조회
농산물 예측 가격

fastapi를 통신하여 예측된 가격 전자
예측된 가격 목록 조회
레시피

레시피 등록
레시피 수정
레시피 상세
레시피 목록 조회
2 | React

문의

관리자 문의 목록 조회
관리자 문의 상세 이력
관리자 문의 답변 등록
사용자 문의 등록
사용자 나의 문의 목록 조회
사용자 나의 문의 내용 이력
이벤트 상품

사용자 이벤트 상품 목록 조회
사용자 이벤트 상품 상세 이력
관리자 메인 페이지

메인 페이지 대시보드
신규 상품 목록 테이블
신규 상품 목록 차트
신규 주문 목록 테이블
신규 주문 목록 차트
월 주문 통계 조회
3 | Machine Learning

모델 추출
머신러닝 소스코드 생성
모델 추출
4 | Infra

개발 환경 설정
Spring 초기 설정
FastAPI 초기 설정
CI, CD 파이프라인 구축
EC2 인스턴스 배포
AWS S3 저장소 설정
