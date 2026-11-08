# Prompts for new chats

Open a fresh Claude Code chat on the Droidbubble repo and paste one of the prompts below. Work **one phase per chat, in order**. When a phase is done, start a new chat with the next phase's prompt.

Before the first chat, commit the staged island-polish work on `main`.

---

## D. Orchestrator (whole roadmap)

```
You are the orchestrator for the Droidbubble island roadmap in docs/island-roadmap/.

1. Read docs/island-roadmap/PLAN.md fully (§0 polish, don't redesign: keep the current
   look and squircle corners; Apple is a REFERENCE, not a clone; §3 decisions; §5 order;
   §6 definition of done) and docs/island-roadmap/tasks/README.md (task index and order).
2. Ask me any open decisions that block the next tasks before launching anything.
   Confirm the staged island-polish work on main is committed first.
3. Launch subagents to do the tasks, following these rules:
   - Phases 0–2 (S, B, F) run first. Run them one agent at a time, or in parallel only
     when the tasks touch different files.
   - After Phase 2 is merged, launch one agent per track (Motion, Icons, Layout,
     Functions, A11y; see tasks/README.md). Each agent works in its own git worktree
     (isolation: worktree) on its own branch.
   - Never let two agents edit the same file at the same time, especially IslandView.kt.
     If two tasks collide, run them one after the other.
   - Each agent does ONE task at a time: read the task entry and its research section,
     verify file:line anchors, implement, run the listed tests, check the acceptance
     criteria, and report back what it verified and what still needs a device check.
4. After each agent reports, review its diff yourself, set the task's Status to done in
   tasks/README.md, and tell me which branches are ready to merge. Don't merge or push
   without my OK.
5. Stop and ask me whenever a task needs a design decision, a device check, or would
   change the current look.
```

---

## Per-phase prompts (use one at a time, in this order)

Every phase prompt starts with the same **shared rules** block, then a phase block. Paste both.

**Shared rules (paste first, every time):**
```
You are the orchestrator for one phase of the Droidbubble island roadmap (docs/island-roadmap/).
Rules for this chat:
- Read docs/island-roadmap/PLAN.md §0 (polish, don't redesign: keep the current look and
  squircle corners; Apple is a reference, not a clone) and §6 (definition of done), plus
  docs/island-roadmap/tasks/README.md.
- Read this phase's task file fully and follow its "Execution order" section.
- Check that every task this phase depends on is marked done in tasks/README.md. If not, stop and tell me.
- Launch subagents, one task per agent. Tasks that touch different files may run in
  parallel, each in its own git worktree. Never let two agents edit the same file at once
  (especially IslandView.kt, IslandOverlayService.kt, NotificationReceiverService.kt,
  MainScreen.kt).
- Each agent: read its task entry and the research it cites, verify file:line anchors,
  implement, run the listed tests, check acceptance criteria, report what it verified and
  what still needs a device check.
- After each task: review the diff, set Status to done in tasks/README.md, tell me which
  branches are ready to merge. Don't merge or push without my OK.
- Stop and ask me whenever a task needs a design decision, a device check, or would
  change how the island looks.
```

### Chat 1: Phase 0–1, safety net and bug fixes
```
Phase: 0–1. Task file: docs/island-roadmap/tasks/phase-0-1-safety-and-fixes.md
(S1.1–S5.3, then B1.1–B11.3).
First confirm the staged island-polish work on main is committed.
Ask me before starting: D5 (live activities on by default), D6 (swipe-right no longer
hides), and whether to keep adb debug control for B1.1.
Do all of Phase 0 (S) before any Phase 1 (B) task. It is the safety net; don't skip it.
```

### Chat 2: Phase 2–3, foundations and one living shape
```
Phase: 2–3. Task file: docs/island-roadmap/tasks/phase-2-3-foundations-and-shape.md
(F1.1–F6.4, then M1.1–M5.3).
Ask me before starting: Decision D1 (raise minSdk 29 → 33?) and D7 (window strategy).
F tasks first ship an exact copy of today's behaviour, then the intended change. Show me
screenshots or video for every intended visual or motion change (F2.3, F4.2, F4.3,
M1.3, M1.4, M5.3).
Follow the file's order: M1.1 and M2.1–M2.4 before M1.3/M1.4.
```

### Chat 3: Phase 5, dynamic icons (can run alongside chat 4 if no file overlap)
```
Phase: 5. Task file: docs/island-roadmap/tasks/phase-5-dynamic-icons.md (I0.1–I4.2).
Ask me before I3.3: should a small bolt icon be added to the charging pill?
Tasks that edit IslandView.kt or LiveActivities.kt must not run at the same time as any
other chat editing those files.
Show me a screen recording of each effect in the I4.1 catalogue for tuning.
```

### Chat 4: Phase 4 + 6, layout polish and signature motion
```
Phase: 4 + 6. Task file: docs/island-roadmap/tasks/phase-4-6-layout-and-motion-polish.md
(VD0.1, V1.1–V7.3, then D1.1–D6.1).
Stop at every owner checkpoint (CP1–CP6) and show me the screenshots or videos listed
there before continuing. At CP1 ask me about margins and pill height.
Optional tasks (V7.3, D1.3, D6.1): ask me before doing them.
D1.x (liquid shader) needs Decision D1; if minSdk stays 29, use the API 33 guard and fallback.
```

### Chat 5: Phase 7, live activities
```
Phase: 7. Task file: docs/island-roadmap/tasks/phase-7-live-activities.md (A1.1–A9.2).
New presentations reuse the existing card and compact designs and SquircleShape; no redesign.
Optional tasks (A2.5, A7.7): ask me before doing them.
Device-check with real apps where the task says so (Clock timer, Maps navigation,
WhatsApp call, delivery/ride apps).
```

### Chat 6: Phase 8, accessibility and companion fixes
```
Phase: 8. Task file: docs/island-roadmap/tasks/phase-8-accessibility-and-companion.md
(C1.1–C5.6).
No settings-app redesign: fixes stay inside the current layout. C5.1 (splitting
MainScreen.kt) is optional; ask me first. Do C2.4 together with V1.4 if V1.4 isn't done.
Re-check MainScreen.kt line ranges; they have drifted.
Include the manual TalkBack check from C3 before marking it done.
```
