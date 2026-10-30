# SimpleMail

[![CI](https://github.com/tonykalavanal/simplemail/actions/workflows/ci.yml/badge.svg)](https://github.com/tonykalavanal/simplemail/actions/workflows/ci.yml)

A small Spring Boot web service that sends an email, built end to end with DevOps tooling:
Maven, JUnit/Mockito tests, Docker, Kubernetes, Terraform, GitHub Actions and Trivy scanning.

## Quick start

| Goal | Command |
| --- | --- |
| Build and test | `mvn verify` |
| Run with Docker | `docker compose up --build` → app http://localhost:8080, inbox http://localhost:8025 |
| Run on Kubernetes (kind, via Terraform) | `./scripts/up.sh` |
| Tear down the cluster | `./scripts/down.sh` |

Locally, every email is caught by [Mailpit](https://mailpit.axllent.org/), so nothing reaches a real inbox.

## Pipeline

Every push to `main` runs: build and test → Docker image → Trivy scan (fails on fixable HIGH/CRITICAL) → deploy to a throwaway kind cluster → smoke test (sends a real email to Mailpit) → publish to `ghcr.io/tonykalavanal/simplemail`.

## More

Design, how each piece works, runbook and lessons learned: [docs/PROJECT.md](docs/PROJECT.md)

Personal learning project by Tony Jose, built with AI assistance (Claude Code).
