# Installing OpenSearch Locally with Docker (Linux Mint)

Steps to run OpenSearch locally on Linux Mint using Docker and Docker Compose. Linux Mint 22 is based on Ubuntu 24.04 ("noble"), so the Docker install steps below target that release.

## Step 1: Install Docker Engine

```bash
# Remove any old versions
sudo apt remove docker docker-engine docker.io containerd runc

# Install prerequisites
sudo apt update
sudo apt install -y ca-certificates curl gnupg lsb-release

# Add Docker's official GPG key
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | \
  sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

# Add the Docker repository (Linux Mint 22 is based on Ubuntu 24.04 "noble")
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/ubuntu noble stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

# Allow your user to run docker without sudo
sudo usermod -aG docker $USER
newgrp docker

# Verify
docker --version
```

## Step 2: Set the Required Kernel Parameter

OpenSearch requires `vm.max_map_count` to be at least `262144`.

```bash
# Apply immediately (resets on reboot)
sudo sysctl -w vm.max_map_count=262144

# Make it permanent across reboots
echo 'vm.max_map_count=262144' | sudo tee -a /etc/sysctl.conf
```

## Step 3: Create a Docker Compose File

```bash
mkdir ~/opensearch-local && cd ~/opensearch-local
```

Copy [docs/docker-compose.yml](docker-compose.yml) from this repo into `~/opensearch-local/docker-compose.yml`. It defines two services:

- **opensearch** — single-node OpenSearch, REST API on port `9200`, Performance Analyzer on `9600`
- **opensearch-dashboards** — web UI on port `5601`

Security plugins are disabled on both for simplicity in local dev.

## Step 4: Start OpenSearch

```bash
cd ~/opensearch-local
docker compose up -d

# Watch the logs until you see "Node" and "started"
docker compose logs -f opensearch
```

## Verify It's Running

Wait ~30 seconds after starting, then:

```bash
curl http://localhost:9200
```

OpenSearch Dashboards will be available at [http://localhost:5601](http://localhost:5601).
