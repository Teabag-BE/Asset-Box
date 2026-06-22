# AssetBox Deploy Guide

This setup runs backend, MySQL, Redis, and frontend on one server with Docker Compose.

## 1. Server Prerequisites

Use Ubuntu on EC2 or another Linux server.

```bash
sudo apt update
sudo apt install -y docker.io docker-compose-plugin
sudo usermod -aG docker ubuntu
```

Log out and back in, then verify:

```bash
docker --version
docker compose version
```

For a small single instance, add swap to reduce OOM risk:

```bash
sudo fallocate -l 4G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
sudo sysctl vm.swappiness=10
echo 'vm.swappiness=10' | sudo tee -a /etc/sysctl.conf
```

## 2. Clone Repositories

```bash
mkdir -p ~/AssetBox
cd ~/AssetBox
git clone <backend-repo-url> team
git clone <frontend-repo-url> teabag-front
```

`docker-compose.yml` expects the frontend repository at `../teabag-front`.

## 3. Configure Secrets

```bash
cd ~/AssetBox/team
cp .env.production.example .env.production
vi .env.production
```

Replace every `CHANGE_ME` value. Do not commit `.env.production`.

## 4. Run

```bash
docker compose --env-file .env.production up -d --build
docker compose ps
docker compose logs -f app
```

Open `http://<server-ip>` after the containers are healthy.

## 5. GHCR Image Flow

The GitHub Actions workflow publishes the backend image on pushes to `dev` and `main`.

On the server, after logging in to GHCR:

```bash
docker login ghcr.io -u <github-id>
docker compose --env-file .env.production pull app
docker compose --env-file .env.production up -d
```

## Notes

- Current `application.yml` uses `ddl-auto: create-drop`; do not use that unchanged for real production data.
- For production data, switch to Flyway-managed schema or at least `validate` after the first migration path is ready.
- OAuth redirect and CORS depend on `baseUrl`; keep it aligned with the real domain.
