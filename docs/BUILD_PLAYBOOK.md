# Ultron-OS — Build Playbook

How this repo was built and how to keep building it safely.

## Working protocol
1. Before code: a 5-line plan + the exact files to touch.
2. Test-first; after each phase run the full suite and paste **real** output.
3. Never merge a phase until acceptance criteria pass.
4. Branch → PR → review → merge. No direct commits to main.

## Verify locally
```bash
# Backend (offline, H2). Local Maven on JDK 17 → add -Djava.version=17
cd backend && ./mvnw -o "-Djava.version=17" test     # expect BUILD SUCCESS

# Frontend
cd frontend && npm run build                          # tsc --noEmit + vite build

# AI layer
cd ai-layer && python -m unittest discover -s tests -t .

# Syntax-check all python (voice + ai-layer)
python -m py_compile $(git ls-files '*.py')
```

## Engineering principles
Clean, typed, observable · human-in-the-loop on every mutating action · fully offline-capable · zero secrets in code · fail-safe degradation · everything tested.

## Migration discipline
Flyway versions are append-only and consistent with JPA entities (H2 tests create from entities; Postgres runs the SQL). Current head: `V6`.

## Adding a capability
- New worker → implement `Worker`, it auto-registers via `WorkerRegistry`.
- New tool → implement `Tool`, auto-registers via `ToolRegistry`.
- New connector → `@Component`, read free, mutations through `ApprovalGate`, token in `ConnectorProperties` (blank default).
- New language → registry entry via `/api/languages` (no pipeline code change for whisper-covered languages).
- Money/contact actions → ship disabled extension points; never auto-arm.
