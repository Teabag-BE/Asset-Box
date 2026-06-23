# AssetBox 배포 가이드

이 구성은 백엔드, MySQL, Redis, 프론트엔드를 단일 서버에서 Docker Compose로 실행합니다.

## 1. 서버 준비

EC2 Ubuntu 서버 기준입니다.

```bash
sudo apt update
sudo apt install -y docker.io docker-compose-plugin
sudo usermod -aG docker ubuntu
```

권한 적용을 위해 로그아웃 후 다시 접속한 뒤 설치를 확인합니다.

```bash
docker --version
docker compose version
```

작은 인스턴스에서는 메모리 부족을 줄이기 위해 swap을 추가합니다.

```bash
sudo fallocate -l 4G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
sudo sysctl vm.swappiness=10
echo 'vm.swappiness=10' | sudo tee -a /etc/sysctl.conf
```

## 2. 운영 환경 변수 설정

서버의 `/home/ubuntu` 경로에 운영 환경 변수 파일을 준비합니다.

```bash
cp .env.production.example /home/ubuntu/.env.production
vi /home/ubuntu/.env.production
```

`CHANGE_ME` 값은 실제 운영 값으로 교체합니다. `.env.production`에는 DB 비밀번호, OAuth secret, S3 key 등이 들어가므로 저장소에 커밋하지 않습니다.

## 3. GitHub Actions Secret

배포 워크플로에서 GHCR push/pull을 수행하려면 repository secret에 `GHCR_TOKEN`을 등록해야 합니다.

필요 권한:

```text
read:packages
write:packages
```

## 4. 배포 흐름

`main` 브랜치에 push되면 `.github/workflows/ci-cd.yml`이 실행됩니다.

```text
main push
-> 태그 생성
-> GitHub Release 생성
-> 백엔드 Docker 이미지 빌드
-> GHCR push
-> self-hosted runner에서 EC2 배포
```

배포 빌드는 `./gradlew build -x test`로 실행하므로 `main` 배포 단계에서는 테스트를 실행하지 않습니다. 테스트는 `dev` 대상 PR 또는 `dev` push에서 `.github/workflows/ci-test.yml`이 담당합니다.

## 5. 수동 배포 확인

서버에서 수동으로 상태를 확인하거나 compose를 재기동할 때는 아래 명령을 사용합니다.

```bash
cd /home/ubuntu
sudo docker compose --env-file .env.production pull
sudo docker compose --env-file .env.production up -d --no-build
sudo docker compose ps
```

컨테이너가 정상 상태가 되면 브라우저에서 서버 주소를 확인합니다.

```text
http://<server-ip>
```

## 주의 사항

- 운영 환경 변수 파일인 `.env.production`은 절대 커밋하지 않습니다.
- 현재 `application.yml`은 `ddl-auto: create-drop`을 사용합니다. 실제 운영 데이터가 필요한 시점에는 Flyway 기반 마이그레이션이나 `validate` 전략으로 전환해야 합니다.
- OAuth redirect와 CORS 설정은 `baseUrl` 값에 영향을 받으므로 실제 도메인과 일치해야 합니다.
