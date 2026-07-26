# Demo learner walkthrough (KEEP-8)

**Audience:** Engineers learning the acceptance catalog.  
**Scope:** Eight bank-letter Live templates only ([demo-catalog-keep-bank-letters](./demo-catalog-keep-bank-letters.md)).  
**No new demo products.**

## 1. Bring up the stack

```bash
pwsh ./scripts/docker-deploy-queue.ps1
```

Wait until backend `:8080/healthz` and frontend `:4173` respond.

## 2. Import and publish KEEP-8

Linux/macOS (FOS-W14-2 wrappers):

```bash
./deploy/import-all-demos.sh
./deploy/publish-all-demos.sh
```

Windows / direct PowerShell:

```powershell
.\deploy\import-all-demos.ps1
.\deploy\publish-all-demos.ps1
```

## 3. Open one flagship letter

1. Sign in as global admin (`10000001` / seed password).
2. Open **Templates** and find `CORP-FOL-OFFER` (Meridian Wholesale FOL).
3. Open the package hub → current published version.
4. Inspect **Design / bindings**: anchors map to structured content modules.
5. Inspect a content module: structure uses the editor `nodes` shape (see FOL modules migrated under FOS-W14-4).
6. Open **API access / contract**: copy credential headers + generate example (English-first).

## 4. Prove runtime generate

```bash
pnpm -C frontend test:e2e:docker:demos
```

FOS-W14-1: missing / non-PUBLISHED KEEP-8 templates **fail** the suite (not skip).

## 5. Optional generate-all

```bash
./deploy/generate-all-demos.sh
```

## Related

- Ops SoT: [demo-catalog-keep-bank-letters.md](./demo-catalog-keep-bank-letters.md)
- Deploy index: [deploy/README.md](../../deploy/README.md)
- Behavior: [fos-demo-literacy-path.md](../behavior/fos-demo-literacy-path.md)
