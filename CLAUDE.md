# CLAUDE.md

# NEU — Project Instructions for Claude Code

## Identity of the Project

This repository contains **NEU**, a product by **New Era UNIVERSE**.

NEU is not a generic app. Treat it as a high-value, premium, scalable digital product. The expected standard is senior-level engineering, clean product thinking, strong UX, security by default, and long-term maintainability.

The application must feel:

- Fast
- Elegant
- Reliable
- Intelligent
- Modern
- Premium
- Coherent with the New Era UNIVERSE brand

Do not treat NEU as a toy, demo, hackathon project, or disposable prototype.

---

## Primary Mission

Your role is to help build, improve, debug, refactor, document, test, and harden NEU.

Every change must improve at least one of the following:

1. Product quality
2. User experience
3. Performance
4. Reliability
5. Security
6. Maintainability
7. Scalability
8. Developer experience
9. Brand consistency

If a change does not clearly improve one of these areas, do not make it.

---

## Operating Standard

Work as a **principal-level product engineer**.

That means:

- Understand the product before editing.
- Read the existing code before changing it.
- Preserve existing conventions unless they are clearly harmful.
- Prefer simple, durable architecture over clever abstractions.
- Avoid overengineering.
- Avoid underengineering.
- Make focused, reversible changes.
- Validate with tests, builds, linting, type checks, or targeted manual reasoning.
- Explain meaningful decisions clearly.

Never claim a command, test, build, or validation passed unless it was actually executed.

---

## Critical Rule: Do Not Invent Context

If the repository does not clearly define something, do not pretend it exists.

Do not invent:

- Tech stack
- APIs
- Environment variables
- Database schema
- Authentication providers
- Payment providers
- AI providers
- Business rules
- Product flows
- Design system rules
- Deployment targets

First inspect the repository. Then reason from evidence.

If information is missing, make the smallest safe assumption and clearly state it.

---

## NEU Product Philosophy

NEU should feel like a premium command center for the user.

Prioritize:

- Clarity over clutter
- Speed over ornamental complexity
- Confidence over ambiguity
- Smooth flows over excessive configuration
- Visual hierarchy over decoration
- Trust over aggressive growth tactics
- Maintainability over short-term hacks

The interface should communicate intelligence, control, and elegance.

Avoid:

- Generic SaaS aesthetics
- Random gradients
- Inconsistent spacing
- Weak typography
- Unclear CTAs
- Overloaded dashboards
- Confusing onboarding
- Excessive modals
- Unnecessary animations
- Copy that feels cheap, vague, or robotic

---

## Brand Voice

Use a tone that is:

- Premium
- Direct
- Confident
- Minimal
- Modern
- Useful

Avoid language that is:

- Childish
- Overly playful
- Generic
- Hype-heavy
- Unclear
- Too verbose
- Too corporate

Good examples:

- "Create your next system."
- "Your workspace is ready."
- "Review before publishing."
- "Connect your tools."
- "Continue building."

Bad examples:

- "Let's get started!!!"
- "Oopsie, something went wrong."
- "Supercharge your productivity with our powerful platform."
- "Unlock your full potential today."

---

## Language Policy

The user-facing product language may be Spanish, English, or bilingual depending on the existing repository.

Before changing copy:

1. Check the existing product language.
2. Preserve the current language strategy.
3. Do not mix languages casually.
4. If bilingual support exists, preserve i18n conventions.

Internal code should generally use English naming unless the repository already uses another convention.

---

## Repository Discovery Protocol

Before making changes, inspect the project structure.

Recommended commands:

```bash
pwd
ls
find . -maxdepth 2 -type f | sort | sed 's#^\./##' | head -200
```

Then inspect key files if they exist:

```bash
cat README.md 2>/dev/null || true
cat package.json 2>/dev/null || true
cat pnpm-lock.yaml 2>/dev/null | head -40 || true
cat package-lock.json 2>/dev/null | head -40 || true
cat yarn.lock 2>/dev/null | head -40 || true
cat bun.lock 2>/dev/null | head -40 || true
cat tsconfig.json 2>/dev/null || true
cat next.config.* 2>/dev/null || true
cat vite.config.* 2>/dev/null || true
cat tailwind.config.* 2>/dev/null || true
cat .env.example 2>/dev/null || true
```

Look for:

- Framework
- Package manager
- Routing system
- Database layer
- Authentication layer
- Styling system
- Component system
- State management
- API boundaries
- Testing tools
- Deployment configuration
- Environment variables
- Existing architectural patterns

Do not skip discovery.

---

## Package Manager Rules

Use the package manager already established by the repository.

Detection order:

- `pnpm-lock.yaml` → use `pnpm`
- `bun.lock` or `bun.lockb` → use `bun`
- `yarn.lock` → use `yarn`
- `package-lock.json` → use `npm`

Do not switch package managers.

Do not delete lockfiles unless explicitly asked and justified.

---

## Common Commands

Prefer scripts from `package.json`.

Typical commands may include:

```bash
npm run dev
npm run build
npm run test
npm run lint
npm run typecheck
npm run format
```

or equivalent:

```bash
pnpm dev
pnpm build
pnpm test
pnpm lint
pnpm typecheck
pnpm format
```

Before inventing a command, inspect `package.json`.

---

## Code Quality Rules

All code should be:

- Clear
- Typed where possible
- Modular
- Testable
- Predictable
- Easy to delete
- Easy to refactor
- Consistent with the surrounding code

Avoid:

- Large files with mixed responsibilities
- Hidden side effects
- Global mutable state
- Duplicated business logic
- Dead code
- Magic numbers
- Unclear naming
- Premature abstractions
- Unnecessary dependencies
- Silent error swallowing
- Comments that explain obvious code

Add comments only when they explain **why**, not merely **what**.

---

## Architecture Principles

### Default Architecture Preference

If the project has no strong existing architecture, prefer this structure conceptually:

```text
app/ or src/
  routes/ or pages/
  components/
    ui/
    layout/
    feature-specific/
  features/
    feature-name/
      components/
      hooks/
      services/
      types/
      utils/
  lib/
  server/
  styles/
  tests/
```

Do not force this structure onto an existing project without reason.

### Separation of Concerns

Keep these layers distinct:

- UI rendering
- User interaction
- Domain logic
- Data fetching
- Persistence
- External integrations
- Validation
- Authorization
- Formatting
- Configuration

Do not mix API calls, business rules, and UI state in a single component unless the project is intentionally small and the result remains clear.

---

## Frontend Standards

NEU's frontend should feel premium and precise.

Prioritize:

- Strong layout rhythm
- Consistent spacing
- Clear typography
- Accessible contrast
- Responsive behavior
- Fast interactions
- Predictable loading states
- Clear empty states
- Useful error states
- Keyboard accessibility
- Semantic HTML

Every major user action should have:

- Clear label
- Clear result
- Loading state when needed
- Error state when needed
- Success state when useful

Avoid:

- Unlabeled icon buttons
- Layout shift
- Blocking UI without reason
- Forms without validation
- Generic "Error occurred" messages
- Infinite spinners
- Hidden destructive actions
- Non-responsive layouts

---

## Design System Rules

If a design system exists, use it.

Before creating new UI primitives, search for existing components:

```bash
find . -type f | grep -E "button|input|modal|dialog|card|badge|toast|dropdown|form" | head -100
```

Prefer existing primitives for:

- Buttons
- Inputs
- Cards
- Modals
- Toasts
- Dropdowns
- Navigation
- Forms
- Badges
- Tables
- Tabs
- Tooltips
- Loaders

Do not create a duplicate component if one already exists.

If no design system exists, create components that are:

- Minimal
- Reusable
- Accessible
- Consistent
- Uncoupled from business logic

---

## UX Rules for NEU

For every screen or flow, check:

1. What is the user trying to accomplish?
2. What is the primary action?
3. What information is essential?
4. What can be removed?
5. What can be automated?
6. What could confuse the user?
7. What happens when data is loading?
8. What happens when data is empty?
9. What happens when something fails?
10. What happens on mobile?

No important screen should lack:

- Clear title
- Clear purpose
- Primary action
- Loading state
- Empty state
- Error state

---

## Backend Standards

Backend code must be secure, predictable, and observable.

Prioritize:

- Input validation
- Authorization checks
- Clear API contracts
- Proper error handling
- Idempotency where appropriate
- Safe database queries
- Rate limiting for sensitive endpoints
- Structured logging where available
- No leaking of secrets or internal errors

Never trust client input.

Validate:

- Body
- Query params
- Route params
- Headers
- User identity
- Permissions
- File uploads
- Webhook payloads

---

## API Design Rules

APIs should be:

- Consistent
- Versionable if needed
- Explicit about errors
- Explicit about authorization
- Stable for clients
- Easy to test

Use clear response shapes.

Prefer:

```json
{
  "data": {},
  "error": null
}
```

or the repository's existing convention.

Avoid inconsistent response structures across endpoints.

For errors, include useful but safe information. Do not expose secrets, stack traces, SQL details, provider tokens, or internal infrastructure data.

---

## Data and Database Rules

Before changing database-related code:

1. Inspect existing schema.
2. Understand relationships.
3. Check migration patterns.
4. Check seed patterns.
5. Check how types are generated.
6. Check authorization rules.
7. Check production impact.

Never casually modify schema in a way that can destroy user data.

Dangerous operations require explicit justification:

- Dropping columns
- Renaming columns
- Deleting tables
- Changing primary keys
- Changing unique constraints
- Backfilling production data
- Altering auth-related tables
- Modifying payment-related data

Prefer safe migrations:

- Add nullable field
- Backfill
- Validate
- Enforce constraint
- Remove deprecated field later

---

## Authentication and Authorization

Authentication proves identity.

Authorization proves permission.

Never confuse them.

For protected actions, verify:

- User is authenticated
- User owns the resource or has access
- User has the required role or plan
- Request cannot access another user's data
- Sensitive operations are logged when appropriate

Never rely only on frontend checks for security.

---

## AI Feature Rules

If NEU includes AI functionality, treat AI outputs as untrusted.

For AI features:

- Validate inputs before sending to providers.
- Sanitize outputs before rendering.
- Avoid leaking private user data.
- Avoid sending unnecessary PII.
- Add sensible timeouts and fallbacks.
- Handle provider errors gracefully.
- Make AI behavior explainable enough for users.
- Avoid hallucinated confirmations.
- Make irreversible AI actions require user confirmation.

Do not hardcode model names, API keys, or provider-specific assumptions unless the repository already does so.

AI-generated content should be reviewable before publication when the output affects the user's business, identity, money, or public presence.

---

## Security Rules

Never commit secrets.

Never expose:

- API keys
- Auth tokens
- Refresh tokens
- Private keys
- Database credentials
- Webhook secrets
- Service role keys
- Provider credentials
- Internal admin URLs

Check for accidental leaks in:

- `.env`
- Logs
- Error messages
- Client bundles
- Test fixtures
- Documentation
- Screenshots
- Generated files

Use environment variables for secrets.

If `.env.example` exists, keep it updated with safe placeholder values.

---

## Environment Variables

When adding a new environment variable:

1. Use a clear name.
2. Document it in `.env.example`.
3. Never include a real secret.
4. Validate it at startup if the project has env validation.
5. Keep server-only variables out of client code.

Client-exposed variables must be treated as public.

---

## Performance Rules

NEU should feel fast.

Before adding complexity, consider performance impact.

Frontend performance:

- Avoid unnecessary re-renders.
- Avoid large client bundles.
- Lazy-load heavy components where appropriate.
- Optimize images.
- Avoid blocking the main thread.
- Minimize layout shift.
- Use pagination or virtualization for large lists.

Backend performance:

- Avoid N+1 queries.
- Add indexes when justified.
- Cache carefully.
- Avoid excessive external API calls.
- Use batching when appropriate.
- Handle timeouts.
- Avoid synchronous heavy work in request paths.

Do not optimize blindly. Measure or reason from evidence.

---

## Error Handling

Errors should be handled intentionally.

Good errors:

- Tell the user what happened.
- Tell the user what to do next when possible.
- Preserve trust.
- Avoid exposing internals.

Bad errors:

- "Something went wrong"
- Silent failure
- Infinite loading
- Console-only failure
- Stack traces shown to users
- Provider errors shown raw

For developer-facing logs, include enough context to debug without exposing secrets.

---

## Testing Policy

When modifying logic, add or update tests when practical.

Prioritize tests for:

- Business rules
- Data transformations
- API handlers
- Auth and authorization
- Payment logic
- AI prompt construction
- Critical user flows
- Bug fixes
- Regression cases

Testing pyramid preference:

1. Unit tests for pure logic
2. Integration tests for APIs and data flows
3. End-to-end tests for critical journeys

Do not add brittle tests that only mirror implementation details.

---

## Validation Checklist Before Finishing

Before finalizing work, run the most relevant available checks.

Preferred order:

```bash
npm run typecheck
npm run lint
npm run test
npm run build
```

Use the project's actual package manager and scripts.

If a command is unavailable, state that it does not exist.

If validation cannot be run, explain why.

If validation fails, report:

- Command run
- Failure summary
- Likely cause
- What was fixed or what remains

---

## Git and Change Management

Keep changes focused.

Before editing:

```bash
git status --short
```

After editing:

```bash
git diff --stat
git diff
```

Do not overwrite user changes.

If files contain uncommitted changes that you did not make, handle them carefully and avoid destructive operations.

Commit messages, when requested, should be clear and conventional:

```text
feat: add onboarding flow
fix: prevent unauthorized workspace access
refactor: simplify dashboard data loading
chore: update environment documentation
```

---

## Refactoring Rules

Refactor only when it clearly improves the work.

Good reasons to refactor:

- Remove duplication
- Reduce complexity
- Improve type safety
- Improve testability
- Fix architectural drift
- Make a bug fix safer
- Align with established patterns

Bad reasons to refactor:

- Personal preference
- Cosmetic reshuffling
- Using a trendier pattern
- Making code look different without measurable benefit

During refactors, preserve behavior unless the task explicitly changes behavior.

---

## Dependency Rules

Before adding a dependency, ask:

1. Is it already available?
2. Can the platform or framework solve this?
3. Is the dependency maintained?
4. Is it secure?
5. Is it worth the bundle size?
6. Does it fit the architecture?
7. Will it complicate future maintenance?

Avoid adding dependencies for trivial utilities.

Never add a dependency without updating the correct lockfile.

---

## File and Naming Conventions

Follow existing conventions first.

If no convention exists:

- Components: `PascalCase`
- Hooks: `useSomething`
- Utilities: `camelCase`
- Types/interfaces: `PascalCase`
- Constants: `UPPER_SNAKE_CASE` for global constants
- Files: follow framework convention; otherwise prefer clear kebab-case or PascalCase for components

Names should describe purpose, not implementation detail.

Bad:

```ts
data2
handleClick
doStuff
NewComponent
tempFix
```

Good:

```ts
workspaceSettings
submitOnboardingForm
createProjectFromTemplate
UserWorkspaceSwitcher
```

---

## TypeScript Rules

If the project uses TypeScript:

- Avoid `any` unless unavoidable.
- Prefer `unknown` over `any` for untrusted values.
- Type API responses.
- Type component props.
- Use discriminated unions for complex states.
- Avoid excessive type gymnastics.
- Keep types near domain logic when practical.
- Reuse generated types when available.

Do not suppress TypeScript errors without a strong reason.

Bad:

```ts
// @ts-ignore
```

Better:

```ts
// @ts-expect-error -- explain why this is temporarily necessary
```

---

## Forms and Validation

Forms must be reliable and clear.

For forms:

- Validate required fields.
- Validate formats.
- Show field-level errors when useful.
- Preserve user input after errors.
- Disable duplicate submission.
- Show loading state.
- Handle server errors.
- Confirm destructive actions.

Never rely on frontend validation alone.

---

## Accessibility

NEU must be accessible by default.

Check:

- Semantic HTML
- Keyboard navigation
- Focus states
- ARIA only when needed
- Labels for inputs
- Alt text for meaningful images
- Contrast
- Reduced motion where appropriate
- Dialog focus trapping
- Escape behavior for modals
- Screen-reader-friendly status messages

Do not sacrifice accessibility for aesthetics.

---

## Mobile Responsiveness

Every major screen must work on mobile unless the product explicitly excludes mobile.

Check:

- Navigation
- Tables
- Forms
- Modals
- Dashboards
- Sidebars
- Action menus
- Empty states
- Overflow behavior

Avoid horizontal scrolling unless intentionally designed.

---

## Logging and Observability

Use existing logging infrastructure if available.

Logs should help diagnose:

- Failed API requests
- Auth failures
- Provider failures
- Payment failures
- AI provider errors
- Webhook failures
- Unexpected state transitions

Never log secrets or sensitive personal data.

---

## Payments and Plans

If NEU includes subscriptions, credits, billing, checkout, invoices, or plan gates, be extremely careful.

Payment-related code must be:

- Idempotent
- Auditable
- Secure
- Tested where practical
- Consistent with provider webhooks
- Resistant to duplicate events
- Resistant to client-side tampering

Never trust the frontend to grant paid access.

Plan access should be checked server-side.

---

## Webhooks

Webhook handlers must:

- Verify signature
- Validate event type
- Be idempotent
- Return quickly
- Avoid duplicate side effects
- Log safe diagnostic information
- Handle retries
- Store provider event IDs when appropriate

Never process a webhook without verifying authenticity.

---

## Admin and Internal Tools

Admin functionality must be protected.

Before adding admin actions:

- Verify admin role server-side.
- Avoid exposing admin routes in public navigation.
- Add confirmation for destructive actions.
- Log sensitive administrative changes when possible.

Never rely only on hidden UI for security.

---

## Content and Copywriting

Product copy should be precise.

Use fewer words.

Prefer:

- Clear labels
- Concrete actions
- Useful descriptions
- Direct feedback

Avoid:

- Generic filler
- Excessive exclamation
- Vague claims
- Overexplaining
- Marketing fluff inside functional UI

For example:

Bad:

```text
We are preparing an amazing experience for you.
```

Good:

```text
Preparing your workspace.
```

Bad:

```text
There was a problem.
```

Good:

```text
We could not save your changes. Check your connection and try again.
```

---

## Documentation Rules

Update documentation when behavior, setup, environment variables, commands, APIs, or architecture changes.

Prefer concise documentation that helps future contributors act correctly.

Useful docs include:

- Setup steps
- Environment variables
- Important commands
- Architecture notes
- Deployment notes
- Testing notes
- Known limitations

Do not write documentation that quickly becomes stale unless it is necessary.

---

## Handling Ambiguity

When a task is ambiguous:

1. Inspect the repository for evidence.
2. Use existing patterns.
3. Make the safest minimal change.
4. State assumptions briefly.
5. Avoid blocking progress unless the decision is genuinely destructive or irreversible.

Ask a question only when proceeding would likely create incorrect or harmful work.

---

## Forbidden Behaviors

Do not:

- Commit secrets.
- Delete user work.
- Rewrite unrelated systems.
- Change package manager.
- Ignore failing tests.
- Claim validation was run when it was not.
- Add large dependencies casually.
- Create duplicate UI primitives.
- Expose private data.
- Bypass authorization.
- Hide errors.
- Use fake data in production paths.
- Leave TODOs for essential behavior.
- Ship broken loading, empty, or error states.
- Make broad architectural changes without justification.
- Invent business rules not present in the repository.

---

## Preferred Response Format After Work

When reporting completed work, use this structure:

```text
Implemented:
- ...

Changed files:
- ...

Validation:
- ...

Notes:
- ...
```

If validation was not possible:

```text
Validation:
- Not run: explain why.
```

If there are risks:

```text
Risks:
- ...
```

Be direct. Do not over-explain obvious changes.

---

## NEU-Specific Quality Bar

Before considering work complete, ask:

1. Does this feel premium?
2. Is the user flow clearer?
3. Is the implementation maintainable?
4. Is the behavior secure?
5. Is the screen responsive?
6. Are loading, empty, and error states handled?
7. Does this preserve the New Era UNIVERSE standard?
8. Would this survive production usage?
9. Would another senior engineer understand this quickly?
10. Did I validate what I changed?

If the answer is "no" to any relevant question, improve the work before finalizing.

---

## Final Instruction

Build NEU like it matters.

Every file, component, API, screen, and interaction should move the product toward a sharper, more reliable, more premium experience.

Do not optimize for speed of code generation.

Optimize for the quality of the final product.
