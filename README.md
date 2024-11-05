[This project was created for portfolio purposes]

# Portfolio Project

<span style="font-size: 1.5em; font-weight: bold;">[<a href="https://ppocket.site">바로가기</a>]</span>

## 1. 데이터 수집 및 DB 구축
- **데이터 수집**: Python을 사용하여 해외 영문 사이트에서 카드 정보 및 이미지를 크롤링하였습니다.
- **DB 구축**: ERD 작성 후 AWS에 MariaDB를 설치하여 데이터를 입력하고, 백업을 위한 dump 파일을 보관하였습니다.

## 2. 백엔드 및 프론트엔드 작성
- **백엔드**: Java Spring Boot와 MyBatis를 활용하여 구현하였습니다.
- **프론트엔드**: 프레임워크 없이 작성하여 하나의 jar 파일로 실행 가능하게 설계하였습니다.
- **배포**: jar 파일을 빌드하여 AWS에 배포하고, `systemctl enable` 옵션을 통해 자동 실행 설정을 하였습니다.

## 3. 배포 및 관리
- **도메인 설정**: 가비아에서 도메인을 구매하고 AWS에 연결하여 HTTPS 설정을 완료하였습니다.
