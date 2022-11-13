# Quick Start

## Prerequisites

To follow this quick start guide, you will need:

- Access to an Ubuntu Server 22.04 with sudo privileges.

## Step 1: Install Docker

First, update your existing packages:

```
$ sudo apt update && sudo apt upgrade
```

Next, install a few prerequisite packages:

```
$ sudo apt install apt-transport-https ca-certificates curl software-properties-common
```

Then add the GPG key for the official Docker repository:

```
$ curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg
$ echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
```

Update package again:

```
$ sudo apt update
```

Install Docker:

```
$ sudo apt install docker-ce
```

Check if Docker is running:

```
$ sudo systemctl status docker
Output
● docker.service - Docker Application Container Engine
     Loaded: loaded (/lib/systemd/system/docker.service; enabled; vendor preset: enabled)
     Active: active (running) ...
     ...
```

Install Docker Compose:

```
$ sudo apt-get install docker-compose-plugin
```

## Step 2: Download Configuration Files

First, download `quickstart.tar.gz` and extract:

```
$ wget https://github.com/michaelliao/itranswarp/raw/master/quickstart.tar.gz
$ tar zxvf quickstart.tar.gz
```

## Step 3: Run Docker Compose

Run Docker Compose in `quickstart` directory:

```
$ cd quickstart
$ sudo docker compose up -d
[+] Running 4/4
 ⠿ Network quickstart_default  Created   0.1s
 ⠿ Container mysql             Healthy  11.6s
 ⠿ Container redis             Started   1.1s
 ⠿ Container itranswarp        Started  12.0s
```

## Visit Web Site

You can access `http://ip` and login as `admin@itranswarp.com` with password `password`.

Change admin password in management console.

https is not configured in `docker-compose.yml`. Consider using a gateway like AWS Elastic Balancer for https access.

## Backup Data

MySQL data is stored in `~/quickstart/docker/mysql-data`. Backup the directory or use `mysqldump` for data backup.
