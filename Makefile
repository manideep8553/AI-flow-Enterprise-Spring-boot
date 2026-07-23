.PHONY: up down logs frontend dev

up:
	docker compose -f docker/docker-compose.yml up -d

down:
	docker compose -f docker/docker-compose.yml down

logs:
	docker compose -f docker/docker-compose.yml logs -f

frontend:
	cd frontend && npm run dev

dev:
	@echo "Starting backend..."
	docker compose -f docker/docker-compose.yml up -d
	@echo "Starting frontend..."
	cd frontend && npm run dev
