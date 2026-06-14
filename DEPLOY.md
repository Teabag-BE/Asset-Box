# AssetBox 배포 가이드 (EC2 Ubuntu + Docker Compose)

처음 배포면 **이 순서 그대로** 한 번 해보기 (자동화(GitHub Actions)는 이게 된 다음)

## 0. 사전 준비
- EC2 Ubuntu 인스턴스 (t3.small 이상 권장 — mysql+redis+jvm+nginx)
- **보안그룹 인바운드**: `22`(SSH), `80`(HTTP), `443`(HTTPS)
- (선택) 도메인 1개

## 1. 서버에 Docker 설치 (EC2에서)
```bash
sudo apt update && sudo apt install -y docker.io docker-compose-plugin
sudo usermod -aG docker ubuntu        # sudo 없이 docker 쓰기
# 재접속(로그아웃→로그인) 후 적용됨
docker --version && docker compose version
```

## 1.5 스왑 메모리 설정 (단일 인스턴스라 권장)
mysql+jvm+redis+nginx를 한 박스에 올리므로, RAM 부족 시 OOM kill을 막기 위해 스왑(EBS를 가상 RAM처럼)을 둔다.
```bash
sudo fallocate -l 4G /swapfile        # 4GB 스왑 파일 (RAM 2배 정도)
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab   # 재부팅에도 유지
sudo sysctl vm.swappiness=10          # 진짜 부족할 때만 스왑 사용
echo 'vm.swappiness=10' | sudo tee -a /etc/sysctl.conf
free -h                               # Swap 잡혔는지 확인
```
> ⚠️ 스왑은 디스크라 느림 → "터지지 않게 하는 안전망". JVM 힙(`-Xmx512m`)은 RAM에 머물게 두고, 스왑은 버스트 흡수용.

## 2. 코드 가져오기 (형제 폴더로 둘 다)
```bash
cd ~ && mkdir AssetBox && cd AssetBox
git clone <team repo>        team          # 백엔드
git clone <front repo>       teabag-front  # 프론트
# 결과: ~/AssetBox/team , ~/AssetBox/teabag-front  (compose가 ../teabag-front 참조)
```

## 3. 비밀설정 만들기
```bash
cd ~/AssetBox/team
cp .env.production.example .env.production
nano .env.production    # DB비번/JWT/S3/OAuth/baseUrl(도메인) 실값으로 채우기
```

## 4. ⚠️ 운영 스키마 설정 (필수!)
`src/main/resources/application.yml` 의
```yaml
jpa:
  hibernate:
    ddl-auto: create-drop   # ← 이대로면 재시작마다 DB 전체 삭제됨!
```
를 **`validate`** (또는 Flyway)로 바꿔야 데이터가 보존됩니다. **최초 1회는 `update`로 테이블 생성 후 `validate`로 고정** 추천.

## 5. 실행
```bash
cd ~/AssetBox/team
docker compose --env-file .env.production up -d --build
docker compose ps           # 4개 컨테이너 Up 확인
docker compose logs -f app  # 백엔드 기동 로그 (Started AssetBoxApplication)
```
→ 브라우저 `http://<EC2 공인IP>` 접속되면 성공 (프론트 + /api 프록시).

## 6. 도메인 연결
- 도메인 DNS에 **A 레코드: `@` → EC2 공인 IP** 추가.
- `.env.production` 의 `baseUrl=https://your-domain.com` 으로 맞추고 `docker compose --env-file .env.production up -d` 재적용.

## 7. HTTPS (도메인 연결 후)
가장 쉬운 방법 — **Caddy**를 80/443 앞단에 두고 자동 인증서:
```bash
# 호스트에 caddy 설치 후 /etc/caddy/Caddyfile:
your-domain.com {
    reverse_proxy localhost:80
}
# (이 경우 compose frontend 포트를 8081:80 등으로 바꾸고 Caddy가 443 담당)
```
또는 nginx + certbot(Let's Encrypt)도 가능. **HTTPS 필수** — 리프레시 토큰 쿠키가 `Secure`라 http면 로그인 유지가 안 됨.

## 8. OAuth (쓸 경우)
- Google/Naver 콘솔에 **redirect URI를 실 도메인 기준**으로 등록.
- Naver는 앱이 "개발 중"이면 테스터만 로그인됨 → 검수 필요.

---
## 자주 막히는 것
- `${redis.port}` 에러 → `.env.production` 누락/오타. host는 `redis`, `db`(컨테이너명).
- 로그인 403 → `baseUrl`이 실제 접속 도메인과 다름 (CORS).
- mysql 접속 실패 → `dbUrl`의 DB명 = `MYSQL_DATABASE`, `dbUserName/dbPassWord` = compose의 MYSQL_USER/PASSWORD 일치 확인.
- 재시작 후 데이터 사라짐 → 4번(ddl-auto) 안 바꿈.

## ghcr 이미지로 운영 (선택, 자동화 단계)
로컬/CI에서 빌드·푸시 → EC2에선 pull만:
```bash
# 빌드 머신에서
docker compose --env-file .env.production build
docker compose --env-file .env.production push
# EC2에서 (build 대신 pull)
docker login ghcr.io -u <id> -p <PAT>
docker compose --env-file .env.production pull
docker compose --env-file .env.production up -d
```
