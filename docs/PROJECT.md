# SimpleMail – DevOps Practice Project

SimpleMail is a small Java web service that sends an email, built end to end with the DevOps tooling CGI asks for: Maven, tests, Docker, Kubernetes, CI/CD, security scanning and Terraform. This is the study guide: read it before any interview and you can explain every part.

## What it is and why

A user opens a web page, types sender, recipient, subject and body, and clicks **Send**. A Spring Boot backend validates the input and sends the email over SMTP. Locally, a fake mail server (**Mailpit**) catches every email, so nothing reaches a real inbox and no passwords are needed.

The app is deliberately simple. The point is everything around it: how it is built, tested, scanned, packaged, deployed and provisioned — the same lifecycle you ran at Global Relay.

| CGI job description asks for | Where this project covers it |
| --- | --- |
| CI/CD pipelines | GitHub Actions workflow (and a Jenkinsfile for comparison): build → test → scan → image → deploy |
| Infrastructure as Code | Terraform creates the local Kubernetes cluster and namespace |
| Docker and Kubernetes | Multi-stage Dockerfile; Deployment, Service, ConfigMap, Secret, probes |
| Security scanning in pipelines | Trivy scans the image and dependencies; build fails on high/critical findings |
| Monitoring and logging | Spring Boot Actuator health and metrics endpoints |
| Release management | Image tags from the Git commit, rollout and rollback with kubectl |
| Scripting | Bash scripts to spin up and tear down the environment |
| Java, Maven, tests | Spring Boot 3, Maven, JUnit 5 and Mockito |

## Architecture

```
 Developer ──git push──▶ GitHub repo ──▶ CI pipeline ───────────────▶ Image registry (GHCR)
 (you + Claude Code)                     - Maven build + JUnit tests      tag = commit SHA
                                          - Trivy scan (fail on high)            │
                                          - Docker image build                   │ image pulled on deploy
                                                                                 ▼
                      ┌──────────── kind cluster · namespace simplemail ───────────────┐
 Browser ───────────▶ │  Service ───▶ SimpleMail app (Deployment, 2 pods) ──SMTP──▶ Mailpit │
 (email form)         │               readiness + liveness probes             (fake SMTP,│
                      │                       ▲                                inbox 8025)│
 Terraform ─────────▶ │               ConfigMap + Secret (SMTP host, port, login)         │
 (builds the cluster) └────────────────────────────────────────────────────────────────────┘
```

Top row is the delivery path: a push triggers the pipeline, which tests, scans and builds an image tagged with the commit. Bottom is the runtime: the browser reaches the app through a Kubernetes Service, the app reads its settings from a ConfigMap and Secret and sends mail to Mailpit, and Terraform creates the cluster itself.

## Tech stack

Every tool is free and runs on your Mac. No cloud bill.

| Tool | What it does here | Interview talking point |
| --- | --- | --- |
| Java 21 + Spring Boot 3 | Web app: serves the page and the REST API | One service, front end as static files, backend as a controller |
| Spring Mail (JavaMailSender) | Sends the email over SMTP | SMTP host, port and credentials come from config, never code |
| Maven | Build, dependencies, tests, packaging into a jar | Same tool you used at Global Relay; `mvn verify` runs the tests |
| JUnit 5 + Mockito | Unit tests for the service and controller | Mock the mail sender so tests never send real email |
| Spring Boot Actuator | `/actuator/health` and metrics | Kubernetes probes call the health endpoint |
| Mailpit | Fake SMTP server with a web inbox | Safe testing: every email is caught locally |
| Docker (multi-stage) | Builds and packages the app into a small image | Build stage with Maven, run stage with only the JRE |
| Docker Compose | Runs app + Mailpit together on your laptop | Quick local loop before Kubernetes |
| kind (Kubernetes in Docker) | A real Kubernetes cluster on your laptop | Same `kubectl` and manifests as a cloud cluster |
| Kubernetes manifests | Deployment, Service, ConfigMap, Secret, probes | Config in ConfigMap, credentials in Secret |
| Terraform | Creates the kind cluster and the app namespace | `init` → `plan` → `apply`, state, providers |
| GitHub Actions | CI/CD: build, test, scan, build image, deploy to a test cluster | Runs on every push and pull request |
| Jenkinsfile | The same pipeline in Jenkins declarative syntax | Lets you compare the two tools you'll be asked about |
| Trivy | Scans the image and dependencies for vulnerabilities | Fails the pipeline on high or critical findings |

## Repository layout

```
simplemail/
├── README.md                  how to run it
├── docs/PROJECT.md            this study guide
├── pom.xml                    Maven build
├── src/main/java/.../         Spring Boot app (controller, service, request model)
├── src/main/resources/
│   ├── static/index.html      the email form (front end)
│   └── application.yml        settings, read from environment variables
├── src/test/java/.../         JUnit 5 + Mockito tests
├── Dockerfile                 multi-stage image build
├── docker-compose.yml         app + Mailpit for local testing
├── k8s/                       Kubernetes manifests
├── terraform/                 kind cluster + namespace
├── .github/workflows/ci.yml   GitHub Actions pipeline
├── Jenkinsfile                same pipeline for Jenkins
└── scripts/                   up.sh / down.sh helpers
```

## How each piece works

### The app

- The browser loads `index.html` from Spring Boot's `static` folder. JavaScript sends the form as JSON to `POST /api/emails`.
- The **controller** receives it, **validates** it (valid email addresses, subject and body not empty) and returns `400 Bad Request` with the reason if not.
- The **service** builds the message and calls `JavaMailSender`, which talks **SMTP** to the mail server. Success returns `202 Accepted`; a mail-server failure returns `502` and is logged.
- **SMTP basics:** port 25/587 (587 with STARTTLS is standard for sending); the app authenticates with a username and password. In production you'd use a provider such as Amazon SES or SendGrid.
- **The "from" address:** real mail providers only let you send from verified addresses, which is why the form's sender is mainly for demonstration. Mention this if asked about real email delivery (SPF, DKIM).
- **Configuration:** `application.yml` reads `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD` from environment variables. The same image runs everywhere; only the config changes.

### Maven and tests

- `pom.xml` uses the Spring Boot **parent POM**, which fixes dependency versions. Dependencies: web, mail, validation, actuator, test.
- Lifecycle: `validate → compile → test → package → verify → install`. `mvn verify` compiles, runs tests and builds the jar.
- **Unit tests** mock `JavaMailSender` with Mockito, so they check "we called send with the right message" without sending anything. **Controller tests** check that bad input returns 400.

### Docker

- **Multi-stage build:** stage 1 uses a Maven image to build the jar; stage 2 copies only the jar into a small Java runtime image. The final image has no Maven, no source code, fewer vulnerabilities.
- Runs as a **non-root user**, exposes port 8080.
- **Image tag = Git commit SHA**, so every build is traceable and rollback means "deploy the previous tag".

### Kubernetes

- **Deployment:** runs 2 replicas of the app; a rolling update replaces pods one at a time with no downtime.
- **Service:** a stable name and port in front of the pods (pods come and go, the Service stays).
- **ConfigMap:** non-secret settings (SMTP host, port). **Secret:** SMTP username and password. Both are injected as environment variables.
- **Probes:** *readiness* (`/actuator/health/readiness`) — don't send traffic until ready; *liveness* (`/actuator/health/liveness`) — restart the pod if it hangs.
- **Resources:** CPU and memory requests and limits, so one pod can't starve the node. Too little memory → `OOMKilled`.
- **Namespace:** `simplemail`, keeps the app's objects together and separate.
- **Rollback:** `kubectl rollout undo deployment/simplemail`.

### CI/CD (GitHub Actions and Jenkins)

- Trigger: every push and pull request to `main`.
- Stages: **checkout → set up Java → `mvn verify` (build + tests) → Trivy scan of dependencies → build Docker image → Trivy scan of image → push image to GitHub Container Registry (main only) → deploy to a throwaway kind cluster and smoke-test `/actuator/health`**.
- A failing test or a high/critical vulnerability **stops the pipeline**: a quality gate, like SonarQube at Global Relay.
- Secrets (registry token) come from GitHub's secret store, never the repo.
- The **Jenkinsfile** expresses the same stages in declarative syntax (`pipeline { stages { stage('Build') { steps { ... } } } post { ... } }`).

### Terraform

- **Providers:** `kind` (creates the cluster) and `kubernetes` (creates the namespace).
- `terraform init` downloads providers → `plan` shows what will change → `apply` makes it → `destroy` removes it.
- **State** (`terraform.tfstate`) records what Terraform created. Here it's local; in a team you'd store it remotely (for example an S3 bucket with locking) so two people can't apply at once.
- **Why Terraform for a local cluster?** Same workflow as provisioning EKS or AKS in the cloud, with no cost. Swapping the `kind` provider for `aws` would create a real cluster.

### Security

- Trivy scans for known vulnerabilities (CVEs) in Java libraries and the base image.
- No secrets in Git; Kubernetes Secret and GitHub secrets instead.
- Non-root container, minimal runtime image, input validation on the API.

## Build plan and status

One phase at a time; tick each off when it works on your Mac. Phases 1–5 are the must-haves for Friday.

- [ ] **1. App:** Spring Boot service, email form, REST endpoint, validation, Mailpit locally
- [ ] **2. Tests:** JUnit 5 + Mockito for the service and controller; `mvn verify` green
- [ ] **3. Docker:** multi-stage Dockerfile, non-root user; Docker Compose with Mailpit
- [ ] **4. Kubernetes:** kind cluster; Deployment, Service, ConfigMap, Secret, probes; Mailpit in the cluster
- [ ] **5. CI/CD:** GitHub Actions build → test → scan → image → push → deploy to kind → smoke test
- [ ] **6. Terraform:** create the kind cluster and namespace with Terraform
- [ ] **7. Jenkinsfile:** same pipeline in Jenkins declarative syntax
- [ ] **8. Stretch:** Helm chart, Prometheus metrics, a real SMTP provider (Amazon SES or Gmail app password)

## Runbook

Install once on your Mac: Docker Desktop, Java 21, Maven, kubectl, kind, Terraform (`brew install openjdk@21 maven kubectl kind terraform`).

| Goal | Command |
| --- | --- |
| Build and run tests | `mvn verify` |
| Run app only (needs Mailpit running) | `mvn spring-boot:run` → open http://localhost:8080 |
| Run app + Mailpit with Docker | `docker compose up --build` → app http://localhost:8080, inbox http://localhost:8025 |
| Create the cluster with Terraform | `cd terraform && terraform init && terraform apply` |
| Load the image into kind | `docker build -t simplemail:dev . && kind load docker-image simplemail:dev --name simplemail` |
| Deploy to Kubernetes | `kubectl apply -f k8s/` |
| Check it | `kubectl get pods -n simplemail` · `kubectl logs -n simplemail deploy/simplemail` |
| Open the app from the cluster | `kubectl port-forward -n simplemail svc/simplemail 8080:80` |
| Open the Mailpit inbox | `kubectl port-forward -n simplemail svc/mailpit 8025:8025` |
| Roll back | `kubectl rollout undo -n simplemail deployment/simplemail` |
| Tear it all down | `cd terraform && terraform destroy` |

Exact commands are confirmed as each phase is built; this table is updated to match the repo.

## Interview questions about this project

1. **Walk me through it.** A Spring Boot service with a web form that sends email over SMTP. Built with Maven, tested with JUnit and Mockito, packaged with a multi-stage Dockerfile, deployed to Kubernetes, provisioned with Terraform, and shipped through a GitHub Actions pipeline with Trivy scanning.
2. **Why Mailpit?** Safe, repeatable testing: it catches every email, so tests and demos never hit a real inbox or need real credentials. Production would point the same config at a real provider.
3. **How do you keep secrets out of the code?** Environment variables from a Kubernetes Secret; registry token in GitHub Secrets. Nothing sensitive in Git.
4. **Why multi-stage Docker?** Smaller, safer image: build tools stay in the build stage, only the jar and JRE ship.
5. **What happens if the app hangs?** The liveness probe fails and Kubernetes restarts the pod. Until it's healthy, the readiness probe keeps it out of the Service.
6. **How does a deployment roll out and roll back?** Rolling update replaces pods one at a time. `kubectl rollout undo` goes back to the previous version; each image is tagged with its commit.
7. **What stops a bad build reaching the cluster?** Failing tests or high/critical Trivy findings fail the pipeline before the image is pushed or deployed.
8. **What does Terraform state do, and where would you keep it in a team?** It records what Terraform manages. In a team, in remote storage with locking (for example S3 with DynamoDB locking, or Terraform Cloud).
9. **How would you take this to AWS?** Push images to ECR, swap the kind provider for EKS in Terraform, store SMTP credentials in AWS Secrets Manager, send through Amazon SES, and expose the app through a load balancer or ingress.
10. **GitHub Actions vs Jenkins?** Same stages. Actions is hosted and defined in YAML in the repo; Jenkins is self-hosted, uses a Groovy Jenkinsfile, and needs its own server and agents.
11. **What would you add next?** Helm for templated deployments, Prometheus and Grafana for metrics, an ingress, and GitOps (Argo CD) for deployments.

## How to talk about it honestly

It's a personal hands-on project, built with AI assistance (Claude Code), to refresh and extend your DevOps skills. Say exactly that. Interviewers respect a candidate who builds things to learn.

- **In the interview:** "To refresh my hands-on skills, I built a small Spring Boot email service and took it through the full lifecycle: Maven, tests, Docker, Kubernetes, Terraform, and a GitHub Actions pipeline with Trivy scanning. I built it with Claude Code, and I can walk you through every part."
- **On a resume (Projects):** "SimpleMail (personal project, 2026): Spring Boot email service with JUnit/Mockito tests, multi-stage Docker image, Kubernetes deployment with probes and secrets, Terraform-provisioned cluster, and a GitHub Actions pipeline with Trivy scanning."
- **What you can now say about Terraform and GitHub Actions:** "hands-on in a personal project", still not production.
- Only claim the phases you've actually finished and can explain.
