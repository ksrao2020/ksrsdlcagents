---
description: Deliver a Jira user story end-to-end with BDD/TDD on the hexagonal/DDD blueprint
argument-hint: <JIRA-ID>
---
Deliver Jira story $ARGUMENTS. Follow CLAUDE.md and docs/architecture/ARCHITECTURE.md strictly. The `customer`
context is the template for every building block.

1. **Fetch**: Use the Jira MCP to read $ARGUMENTS (summary, description, acceptance criteria, linked issues,
   attachments). Run `git switch -c feature/$ARGUMENTS-<short-slug>` from an up-to-date main.
2. **Analyse (BDD)**: Delegate to `bdd-analyst`. If it returns open questions, post them as a Jira comment on
   $ARGUMENTS, show them to me, and STOP.
3. **Approve scenarios**: Show me the `.feature` file and wait for my explicit approval.
4. **Design**: Delegate to `solution-architect`. Show me the design summary (context, aggregate, API changes,
   mapping chain, migration) and wait for my approval. Commit the `.feature`, the design doc and `api.yaml`.
5. **Red**: Delegate to `test-writer`. Verify that the new tests fail for the right reason and the old ones still pass.
6. **Green/Refactor**: Delegate to `implementer`, one scenario at a time, until `mvn verify` is green
   (unit tests, ArchitectureTest, JaCoCo ≥ 80% line and branch).
7. **Quality gates**: Run `mvn verify sonar:sonar -Dsonar.qualitygate.wait=true` (needs SONAR_HOST_URL and
   SONAR_TOKEN) and, if dependencies changed, `mvn org.owasp:dependency-check-maven:check`. Fix the issues and repeat.
   If Docker is available, also run `mvn verify -Pit` (Testcontainers Oracle + Cucumber).
8. **Review**: Delegate to `architecture-reviewer` and `security-reviewer` in parallel. Fix every finding and
   re-run step 6 until both pass.
9. **Deliver**: Push the branch and open a PR. The PR body contains: the Jira link, the scenarios, the ticked
   per-story checklist from ARCHITECTURE.md §8, the coverage %, and the Sonar link. Comment on $ARGUMENTS with
   the PR link and move it to "In Review".

Never skip a step, weaken a gate, or merge the PR yourself.
