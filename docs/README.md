# Documentation map

Applies to: repository layout at 1.9.1 | Last verified: 2026-08-13 | Status: Current

How `docs/` is organised, what belongs in each tree, and the conventions every page follows.
This page describes the repository, so it carries no driver version in its status line.

**Looking for the actual documentation?** Start at **[user/index.md](user/index.md)**.

---

## The one thing to know first

`docs/` contains **two trees with opposite publication rules**:

| Tree | Git | Audience | Rule |
|---|---|---|---|
| **[`user/`](user/)** | **Tracked and committed** | End users, browsed on GitHub | The only documentation that ships. Every claim must be verified against current code. |
| **`maintainer/`** | **Gitignored — never committed** | The maintainer and AI agents | Working notes. Never published, never mirrored into `user/`. |

`maintainer/` is excluded by [`.gitignore`](../.gitignore) (`docs/maintainer/`), as are the root agent
guides `AGENTS.md`, `CLAUDE.md`, and `CODEX.md`. **This page and `docs/user/` are therefore the only
documentation a fresh clone contains** — which is why the conventions below are written down here and
not only in the agent guide.

> **Agents: `maintainer/` and `AGENTS.md` will not exist in a fresh clone.** If they are missing,
> nothing is broken and nothing needs restoring — you are simply on a machine that never had them. Do
> not reconstruct them, and do not assume another session's `BUGS.md` findings are available to you.

The practical consequence: **anything speculative, internal, or unconfirmed belongs in `maintainer/`.**
Source-file locations, suspected causes, unreproduced reports, and half-finished investigations stay
local. What reaches `user/` is behaviour a user can observe, stated at a confidence level the evidence
supports.

---

## `docs/user/` — public documentation

34 pages and 37 images. Tracked, committed, and browsed directly on GitHub, so relative links between
pages must work without a site generator.

| Section | Path | Contents |
|---|---|---|
| Landing page | [`index.md`](user/index.md) | Table of contents, bridge gallery with purchase links |
| Getting started | [`getting-started/`](user/getting-started/) | Installation, using Matter devices (2 pages) |
| Configuration | [`configuration/`](user/configuration/) | Preferences, commands and states (2 pages) |
| Drivers | [`drivers/`](user/drivers/) | One page per driver, plus the assignment table (15 pages) |
| Compatibility | [`compatibility/`](user/compatibility/) | Overview, device types, the support matrix (3 pages) |
| Matter bridges | [`bridges/`](user/bridges/) | Aqara, Hue, SwitchBot, Tuya/Zemismart, others (5 pages) |
| Help | [`help/`](user/help/) | Troubleshooting, known issues, support links (3 pages) |
| Project | [`project/`](user/project/) | Revision history, terminology, contributing (3 pages) |
| Images | `user/assets/images/` | Screenshots and product photos (37 files) |

### Conventions every page follows

**1. A status line on line 3**, immediately under the H1:

```
Applies to: <version> | Last verified: YYYY-MM-DD | Status: Current | Experimental | Historical
```

`Historical` is the honest default for content carried over from the wiki that has not been
re-verified. Two driver pages are `Experimental` (Camera AV Stream, Signal) and one is `Historical`
(SwitchBot Button, deprecated).

**2. An evidence label on every support claim** — one of **Confirmed**, **Reported**, **Implemented
unverified**, **Unsupported**, **Unknown**, or **Historical**. Never a bare `?`, `check`, or `TODO`
in a compatibility table. *Confirmed* means tested on a named hub/bridge/device combination; if the
bridge is not named, the report cannot be recorded, because the same device behaves differently
behind different bridges.

**3. Naming.** Page files are lowercase kebab-case. Driver pages drop the
`Matter Generic Component` / `Matter Custom Component` prefix and are named by function
(`window-shade.md`), while the page's H1 is the exact driver name as Hubitat shows it. Images are
`<flattened-page-path>-NN.png` — `getting-started/installation.md` owns
`getting-started-installation-01.png` onward.

**4. Two pages have a code source of truth** and must change in the same commit as that code:

| Page | Source of truth |
|---|---|
| [`drivers/index.md`](user/drivers/index.md) | `mapMatterCategory()` in `Matter_Advanced_Bridge.groovy` |
| [`configuration/preferences.md`](user/configuration/preferences.md) | the `preferences` block, **plus** the `#include`d libraries |

Component metadata is not only in the component file — `ping`/`rtt` come from `matterHealthStatusLib`
and the whole `utilities` command from `matterUtilitiesLib`. Grep `Libraries/` too, or you will
document a driver as having fewer commands than it has.

---

## `docs/maintainer/` — local working documents

**Not in git.** Present only on the maintainer's machine. Described here so an agent that *does* have
it knows what each file is for.

| Path | Contents |
|---|---|
| `README.md` | The maintainer-tree guide (more detail than the summary here) |
| `TODO.md` | Open user requests harvested from the community thread |
| `bugs/BUGS.md` | Reviewed defect list — **authoritative** for open/closed status |
| `bugs/BUGS_CODEX.md` | Earlier Codex analysis (2026-07-04), read-only, superseded by `BUGS.md` |
| `plans/` | Implementation and migration plans (8 files) |
| `status/` | Progress records and migration evidence (4 files) |
| `archive/` | Backups and superseded material — currently the wiki baseline bundle |

`BUGS.md` and `TODO.md` are worked **one item at a time**, and an item is marked `[x]` only after the
user confirms a hub test. `docs/user/help/known-issues.md` is **not** a mirror of `BUGS.md`: a public
known issue gets affected version, symptom, workaround, and resolution status — and nothing else.

---

## Where does this go?

| I want to write about… | It goes in |
|---|---|
| Behaviour a user can see and act on | `docs/user/` (the applicable page) |
| A defect, its cause, and its source location | `maintainer/bugs/BUGS.md` |
| A user-facing issue that is real but unfixed | `user/help/known-issues.md` **and** `BUGS.md` |
| A technical record of a change | [`CHANGELOG.md`](../CHANGELOG.md) (repo root) |
| A plain-language record of a change | [`user/project/revisions-history.md`](user/project/revisions-history.md) |
| Instructions for AI agents | `AGENTS.md` (repo root, local only) |
| A deferred doc improvement or unresolved claim | `maintainer/status/documentation-open-items.md` |
| A feature request from the forum | `maintainer/TODO.md` |

`CHANGELOG.md` and `revisions-history.md` are **both** needed for a behaviour change, and neither is
generated from the other. The first is the technical record for developers; the second is the
user-facing narrative.

---

## Editing checklist

1. **Verify against current code, never against the changelog or a comment.** The driver header
   records what was true when written. Read the metadata and the method.
2. **Audit against `main`, not `development`.** Where they differ materially, document released
   behaviour and label the upcoming change explicitly.
3. **Update the status line** — at minimum `Last verified:` — on any page you touch.
4. **A user-visible behaviour change** requires auditing the applicable `docs/user/` page in the same
   change, plus a `CHANGELOG.md` entry.
5. **Documentation-only edits need no version bump or driver timestamp change.** Code changes do need
   the timestamp; only the maintainer decides version bumps.
6. **Never publish** absolute local paths, private hub addresses (RFC1918), or personal data — strip
   them from screenshots too.
7. **The wiki is frozen** at baseline commit `c4000b7` (2026-07-27) and superseded by `docs/user/`.
   Do not edit wiki pages, and never copy content *from* the wiki — it is 2024 material describing
   limitations that no longer exist. It is kept so old forum links keep working.
8. **External publication, wiki edits, and link changes require explicit authorization.**

Where `AGENTS.md` is present, its §7 "Documentation rules" is the authority and this page is the map.
Where it is not — any fresh clone — this page is what there is.

---

## Related files outside `docs/`

| File | In git? | Role |
|---|---|---|
| [`README.md`](../README.md) | tracked | Repository landing page |
| [`CHANGELOG.md`](../CHANGELOG.md) | tracked | Technical changelog, Keep a Changelog format |
| `packageManifest.json` | tracked | Hubitat Package Manager manifest — release metadata |
| `AGENTS.md` | **local only** | Agent guide; source of truth for how to work in this repo |
| `CLAUDE.md`, `CODEX.md` | **local only** | Pointers to `AGENTS.md` — no content of their own |
