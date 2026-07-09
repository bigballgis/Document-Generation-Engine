# Queued Docker deploy / status.

Optional shortcut — parent auto-runs this when the user asks to deploy / check queue
(see subagent-routing-mandate Auto-intent).

$ARGUMENTS

Delegate to `build-deploy-agent`:
- Default: `.\scripts\docker-deploy-queue.ps1`
- Status: `.\scripts\docker-deploy-queue.ps1 -Status`
- Restart only: `-SkipBuild`
Never start a second compose project or invent port offsets on this host.
