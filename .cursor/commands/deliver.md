# Deliver a behavior-changing slice end-to-end.

Optional shortcut — the parent agent should run this workflow automatically when the
user states a delivery goal in natural language (see subagent-routing-mandate Auto-intent).

Invoke `Task(subagent_type=delivery-orchestrator)` with this user goal:

$ARGUMENTS

Follow `.cursor/skills/delivery-pipeline/SKILL.md` stages 0–13.
Supervisor mode: stay in this chat; spawn specialists; do not ask the user to open new chats.
Honor `no-commit` / `no-push` if present in the conversation.
