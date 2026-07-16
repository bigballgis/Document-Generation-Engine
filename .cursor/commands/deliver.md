# Deliver a behavior-changing slice end-to-end.

Optional shortcut — the parent agent should run this workflow automatically when the
user states a delivery goal in natural language (see subagent-routing-mandate Auto-intent).

Invoke `Task(subagent_type=delivery-orchestrator)` with this user goal:

$ARGUMENTS

Follow `.cursor/skills/delivery-pipeline/SKILL.md` stages **−1** then 0–13.

**Stage −1 (mandatory on deliver):** Batch Recommendation —
`.cursor/skills/delivery-batch-recommend/SKILL.md`. Emit `batch_recommendation`
(`merge` | `solo` | `split`) from **repo facts** before provisioning a worktree.
Intentional related merge into **one** leaf amortizes fixed cost; this is **not**
multi-writer parallel (`force-parallel` remains opt-in only).

**Stage 0 (mandatory):** `worktree-router` → `../DGE-<slice-id>` → `move_agent_to_root`
before any file writes. Never implement on MAIN. Use the **single**
`proposed_slice_id` from Batch Recommendation.

Supervisor mode: stay in this chat; spawn specialists; do not ask the user to open new chats.
Honor `no-commit` / `no-push` / `main-only` if present in the conversation.
