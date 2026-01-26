# Testing Standards and Verification Rules

## Critical Rule: Test Verification Before Task Completion

**ABSOLUTE REQUIREMENT:** A task involving test creation or code changes with existing tests is NOT complete until ALL tests compile and pass with 100% success rate.

## Core Principles

### 1. Definition of "Complete"
- **Complete** means 100% tests passing, not "mostly working"
- Documentation of failures does NOT substitute for resolution
- Explanations of why tests fail do NOT count as completion
- A task with failing tests is an INCOMPLETE task

### 2. Test Creation Workflow
When creating new tests, you MUST follow this sequence:

1. **Write Test Code**
   - Ensure test code compiles without errors
   - Use programmatic test data generation when possible
   - Validate test data against specifications before use

2. **Verify Compilation**
   - Run `./gradlew :module-name:compileDebugUnitTestKotlin` (or equivalent)
   - Fix ALL compilation errors before proceeding
   - NEVER assume tests compile - verify explicitly

3. **Execute Tests**
   - Run `./gradlew :module-name:testDebugUnitTest` (or equivalent)
   - Wait for complete test execution results
   - Review ALL test outcomes

4. **Achieve 100% Pass Rate**
   - If ANY test fails, investigate and fix root cause
   - Re-run tests after each fix
   - Repeat until ALL tests pass

5. **Only Then Mark Complete**
   - Use `attempt_completion` only after confirming 100% pass rate
   - Include test execution results in completion report

### 3. Test Data Quality Standards

**Test data MUST conform to specifications:**
- Specifications are absolute, not guidelines
- "Almost valid" test data is invalid test data
- Manual test data creation is error-prone - prefer programmatic generation
- Validate test data format before using in tests
- When specifications exist (API contracts, data formats, protocols), test data must comply exactly

### 4. Verification Commands

Before marking any task complete, you MUST execute verification commands:

```bash
# Compile tests
./gradlew :module-name:compileDebugUnitTestKotlin

# Run tests
./gradlew :module-name:testDebugUnitTest

# For all modules
./gradlew test
```

**NEVER skip this step.** Always wait for and review the command output.

### 5. Handling Test Failures

When tests fail:

1. **Report the failure clearly**
   - State number of failing tests
   - Identify which tests failed
   - Explain root cause if known

2. **Investigate root cause**
   - Check test data validity
   - Verify specification compliance
   - Review implementation logic
   - Examine test assertions

3. **Fix and verify**
   - Implement fix for root cause
   - Re-run tests to confirm fix
   - Document what was corrected

4. **Never assume** - always verify the fix worked by running tests again

## Prohibited Behaviors

### ❌ NEVER Do These Things:

1. **Mark task complete with failing tests**
   - Even if you explain why they fail
   - Even if "only a few" tests fail
   - Even if failures seem minor

2. **Skip test execution**
   - Never assume tests will pass
   - Never trust that tests "should work"
   - Always run tests explicitly

3. **Use invalid test data**
   - Never use "close enough" data
   - Never manually create complex test data without validation
   - Never skip specification compliance

4. **Ignore compilation errors**
   - Tests must compile before they can pass
   - Compilation errors indicate incomplete work

## Workflow Integration

### When Starting a Task:
```markdown
- [ ] Understand requirements
- [ ] Implement solution
- [ ] Create/update tests
- [ ] Verify tests compile
- [ ] Run all tests
- [ ] Achieve 100% pass rate
- [ ] Mark task complete
```

### Before Using `attempt_completion`:
Ask yourself in `<thinking>` tags:
1. Did I create or modify any tests?
2. Did I run the test compilation command?
3. Did I run the test execution command?
4. Did I receive confirmation that ALL tests passed?
5. If any answer is "no" - DO NOT use `attempt_completion`

## Success Metrics

**Task completion requires:**
- ✅ All tests compile without errors
- ✅ All tests execute successfully
- ✅ 100% test pass rate achieved
- ✅ Test results verified in terminal output
- ✅ No assumptions made - explicit verification performed

## Common Failure Patterns to Avoid

**Pattern 1: Premature Completion**
- Created tests → Marked complete → Multiple tests failing
- **Correction:** Must verify 100% pass rate before completion

**Pattern 2: Invalid Test Data**
- Manual test data → Format/validation errors → Tests failing
- **Correction:** Programmatic test data generation + validation

**Pattern 3: Successful Approach**
- Planned approach → Implemented → Verified tests → 100% pass rate
- **Result:** Completed efficiently with minimal iterations

## Remember

> "It is pointless to create a failing test."  
> — Developer feedback from Phase 2

Tests exist to verify correctness. Failing tests indicate incomplete work, not documentation opportunities. Quality-first thinking means achieving 100% pass rate before claiming completion.