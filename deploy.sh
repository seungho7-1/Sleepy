#!/bin/bash
# sleepy-backend deployment script

echo "Pulling latest changes..."
git pull origin main

echo "Building and starting Docker containers..."
docker-compose -f docker-compose.prod.yml up -d --build

echo "Cleaning up dangling images..."
docker image prune -f

echo "Deployment complete!"
docker ps
