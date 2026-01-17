#!/bin/bash
# Arrêter et supprimer seulement l'application
docker compose stop socobis-app
docker rm socobis-wildfly 2>/dev/null || true

# Nettoyer les images et caches
# docker image prune -f
docker builder prune -f

# Rebuilder
docker compose build --no-cache socobis-app

# Redémarrer
docker compose up socobis-app