# pessoa-faladora

## Set up Ollama

Install Ollama.

Run:

```shell
ollama pull qwen3:4b
ollama pull qwen3-embedding:8b
```

## Viewing DB

http://localhost:6333/dashboard#/collections using the key in docker-compose.yaml

## Viewing traces

http://localhost:16686

## Run docker compose

To start pgsql.

## Running the application in dev mode

```shell script
./gradlew quarkusDev
```
