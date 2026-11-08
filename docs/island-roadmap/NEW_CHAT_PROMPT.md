# Kickoff prompts for a new chat

Copy one of the blocks below into a fresh Claude Code chat opened on the Droidbubble repo.

---

## A. Start the roadmap (first session)

```
We're executing the Droidbubble island roadmap in docs/island-roadmap/.

1. Read docs/island-roadmap/PLAN.md fully (especially §0: polish, don't redesign; keep
   the current look and squircle corners; Apple's Dynamic Island is a REFERENCE, not
   something to copy 1:1; §2 what Dynamic-Island quality means here; §3 decisions;
   §6 definition of done).
2. Read docs/island-roadmap/tasks/README.md for the task index and execution order.
3. Before any code: confirm the uncommitted island-polish work is committed/checkpointed,
   and ask me the open decisions in PLAN.md §3 that block the first tasks (D1 minSdk, D5, D6).
4. Then start with the first task in the execution order. Read its full entry in the
   phase file under docs/island-roadmap/tasks/, and the research section it cites.
   Verify file:line anchors against the current code before editing (they may have drifted).
5. One task = one focused change. Run the tests named in the task, check its acceptance
   criteria, and tell me what you verified and what still needs a device check.
   Then mark the task done in tasks/README.md (status column) and propose the next one.
```

---

## B. Pick up a specific task

```
We're executing the Droidbubble island roadmap. Do task <ID> (e.g. B2.1).

- Read docs/island-roadmap/PLAN.md §0, §2 and §6 (Apple is a reference, not a clone;
  Droidbubble identity; definition of done).
- Read the task's full entry in docs/island-roadmap/tasks/ and the research it cites in
  docs/island-roadmap/research/.
- Check its "Depends on" tasks are marked done in tasks/README.md; if not, tell me first.
- Verify file:line anchors against the current code, implement, run the listed tests,
  check acceptance criteria, then update the status in tasks/README.md.
```

---

## C. Run a parallel track (after Phase 2 lands)

```
We're executing the Droidbubble island roadmap. Work the <Motion | Icons | Layout | Functions |
A11y> track from docs/island-roadmap/PLAN.md §5, in the order given in
docs/island-roadmap/tasks/README.md. Only touch files owned by this track; if a task needs
a file another track is editing, stop and tell me. Follow PLAN.md §0 and §6 for every task.
```

---

## D. Orchestrator: read the plan and launch agents

```
You are the orchestrator for the Droidbubble island roadmap in docs/island-roadmap/.

1. Read docs/island-roadmap/PLAN.md fully (§0 polish, don't redesign: keep the current
   look and squircle corners; Apple is a REFERENCE, not a clone; §3 decisions; §5 order;
   §6 definition of done) and docs/island-roadmap/tasks/README.md (task index and order).
2. Ask me any open §3 decisions that block the next tasks before launching anything.
   Confirm the island-polish work is committed first.
3. Launch subagents to do the tasks, following these rules:
   - Phases 0–2 (S, B, F) run first. Run them one agent at a time, or in parallel only
     when the tasks touch different files.
   - After Phase 2 is merged, launch one agent per track (Motion, Icons, Layout,
     Functions, A11y; see PLAN.md §5). Each agent works in its own git worktree
     (isolation: worktree) on its own branch.
   - Never let two agents edit the same file at the same time, especially IslandView.kt.
     If two tasks collide, run them one after the other.
   - Each agent does ONE task at a time: read the task entry and its research section,
     verify file:line anchors, implement, run the listed tests, check the acceptance
     criteria, and report back what it verified and what still needs a device check.
4. After each agent reports, review its diff yourself, update the status in
   tasks/README.md, and tell me which branches are ready to merge. Don't merge or push
   without my OK.
5. Stop and ask me whenever a task needs a design decision, a device check, or would
   change the current look.
```

---

### Notes for whoever runs these
- **Polish, don't redesign.** Keep the current design: squircle corners, cards, palette. The improvements are layout precision, motion, gestures, dynamic icons and Dynamic-Island-like liveliness.
- **Apple is a reference, not a clone target.** Take the principle and implement it the Android way (PLAN.md §0 and §2).
- **Baselines first.** The Phase 0 tests (S1–S5) are the regression net for everything after them. Don't skip them.
- **`IslandView.kt` is the contention point.** Don't run two writing sessions on it at once.
