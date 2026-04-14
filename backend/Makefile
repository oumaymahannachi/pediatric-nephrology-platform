.PHONY: build up down clean fresh logs ps mvn-build docker-build

mvn-build:
	@echo ">>> Building all Maven modules..."
	mvn clean package -DskipTests -q
	@echo ">>> Maven build complete."

docker-build:
	@echo ">>> Building Docker images..."
	docker-compose build --no-cache
	@echo ">>> Docker images built."

build: mvn-build docker-build

up:
	@echo ">>> Starting PediaLink stack..."
	docker-compose up --build --force-recreate --remove-orphans -d
	@echo ""
	@echo "  Frontend       : http://localhost:4200"
	@echo "  Keycloak admin : http://localhost:8080  (admin / admin)"
	@echo "  Eureka         : http://localhost:8761"
	@echo "  API Gateway    : http://localhost:8090"
	@echo "  Auth Service   : http://localhost:8081"
	@echo "  Treatment Svc  : http://localhost:8082"
	@echo ""
	@echo "  Run 'make logs' to follow all container logs."

down:
	@echo ">>> Stopping PediaLink stack..."
	docker-compose down --remove-orphans
	@echo ">>> Done."

clean:
	@echo ">>> Full cleanup: removing all containers..."
	docker-compose down --remove-orphans
	@echo ">>> All containers removed."

fresh: clean
	@echo ">>> Starting fresh stack..."
	docker-compose up --build --force-recreate --remove-orphans -d

logs:
	docker-compose logs -f

ps:
	docker-compose ps
