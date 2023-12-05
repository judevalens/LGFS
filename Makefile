build-image:
	docker image rm -f lgfs:latest
	docker build -t lgfs:latest ./
build-dist:
	./gradlew installDist
cluster-up:
	docker compose --profile all  up

cluster-chunk-up:
	docker compose --profile chunk up
build: build-dist build-image
build-cluster: build-dist build-image cluster-up