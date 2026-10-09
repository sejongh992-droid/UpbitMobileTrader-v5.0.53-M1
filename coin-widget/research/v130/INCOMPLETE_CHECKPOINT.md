# Incomplete v1.3.0 checkpoint — 2026-10-09

The execution environment failed: exec-server initialize handshake timed out and local execution tools were removed. This is NOT a completed release or a completed three-strategy profitability validation.

## Preserved application work
- Fixed candidate/protocol commit: 3b587f350ab4f48bd51bfa8436cf04818d2cf8c4.
- Working application commit: 8ac942859c788720f9fba732603b9135acd7e11b.
- Candidate StrategyV130.java SHA256: a3da08d1ddc463db6cfce1767b78498c73e92c0d3b4e24f1297c46ef460684e7.
- Independent day/pre09/long rules, actual-quote cost-adjusted support/resistance, separate chart-pass/profitability approval, 2-minute pre09 and 5-minute other expiry, schema 7, version 1.3.0/code 8.
- ValidationPolicy.java and Evidence.java in that working commit are deliberately PENDING placeholders. Do not distribute the CI APK as a finished user update.
- CI run 37879393249 passed build/lint and API34/API36. Each device passed 44 deterministic tests plus one live public-data refresh. The workflow source tests passed 100 assertions.
- Live API36 snapshot had 29 day observations, 15 pre09 observations, 15 long observations; all lane fetch statuses were ok. No chart passes in that particular snapshot.
- CI APK uses the CI certificate. The user update must be signed with the existing private certificate locally; never upload the private key to CI.
- Expected release certificate SHA256: 18f5449f4af6f7abd4bd319a57519c891de51509ef53ce21966318b2bb45b383.

## Local work to recover
Workspace: /workspace/scratch/5e0d76f6b4be
- widget-work: two more long-fixture assertions (local source suite 102 passed) and HelpUi chart-pass wording changes not yet committed.
- validation-v130: data.py, prepare_cases.py, ReplayV130.java, analyze.py, audit_replay.py, create_findings.py, finalize_sources.py, retry_five.py, package_research.py, run_replay.sh and cached outputs.
- validation-work/public: immutable public API responses.
- Initial hourly/daily acquisition: 2304 tasks; remaining unavailable endpoints were BONK/STORJ/TT (HTTP404).
- Found and repaired a 20-day daily collection boundary gap with 145 gap requests plus retries. Final long outcome missing count was 0; overlapping response conflicts 0.
- Short study: 220 decision times / 6646 five-minute request tasks. Last confirmed cached response count was 4351 at 03:48:18 UTC. Collection is NOT confirmed running after environment failure.
- The short evaluation has NOT been computed to completion. Do not infer its performance or mark it passed.
- Last resumed collector session was 50403, log validation-v130/fetch-five-resume.log.
- Never change fixed candidate thresholds or evaluation dates in response to results.

## Completed long retrospective calculation (local JSON is authoritative)
Evaluation dates 2025-08-01 through 2026-01-31 (184 dates).
Primary horizon 90 days, next-day open entry, fee 0.05% and slippage 0.05% each side.
Both variants called the application Java rules on the same historical liquidity top-30 universe approximation.
- Candidate strict top5: 0 cases / 0 signal days. Profitability NOT evaluable; gate failed.
- Baseline strict top5: 24 cases / 18 signal days. Day-equal means: 30d +1.0795947325068014%, 60d -12.735801537914412%, 90d -29.912042049180286%.
- Candidate observation top5: 865 cases / 173 signal days. Day-equal means: 30d -18.237884707822484%, 60d -28.526682224670367%, 90d -37.46084602789987%.
- Baseline observation top5: 865 cases / 173 signal days. Day-equal means: 30d -18.6890893286071%, 60d -29.07308333638241%, 90d -38.67483485500354%.
- Candidate 90d stress mean -37.5233539591603%; matched BTC -19.03736442242604%; excess over matched liquidity universe +1.0113269109664955 percentage points; 95% 90-day-block bootstrap interval [-43.674020689210295, -31.08475934715865].
- Total long comparison output 18210 variant rows and 54630 Decimal-rechecked horizon returns; no missing outcomes.
- These are date-weighted hypothetical returns, not a real account return. Observation ranks are not buy recommendations. Long overlapping horizons, survivorship/universe approximations and earlier exposure to some 2026 outcomes prohibit a pristine holdout/prospective-validation claim.

## Remaining work after environment recovery
1. Inspect existing collector before starting another; avoid duplicate requests. Finish all 6646 tasks with the existing public read-only rate limiter, retry only missing/invalid responses.
2. Run final short preparation, Java replay, analysis, source manifest and no-future-data/entry/exit/cost audits. Preserve missing observations and data limitations.
3. Generate actual ValidationPolicy/Evidence/report from all three results. Failed gates retain observations and explicitly withhold validated-buy status. No post-hoc threshold tuning.
4. Finish easy Korean explanation, run final source checks, commit complete source to coin-widget-v130, run final API34/36 CI, download verified artifacts.
5. Locally sign the exact tested APK with the existing key; verify certificate, payload identity, ZIP integrity and alignment.
6. Save the APK, final Korean report and combined public research/runtime evidence. Repository-backed application source should remain in GitHub rather than be duplicated into Library.
7. Tell the user what was verified and what was not. Never call the current partial build the final updated APK.
