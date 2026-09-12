# Pull Request Summary

## Description

<!-- Briefly describe what this PR does and why. -->

Issue / Task:

* <!-- Task 1 -->
* <!-- Task 2 -->
* <!-- Task 3 -->

---

## Module(s) Affected

* [ ] Authentication & User Management
* [ ] Transactions
* [ ] Categories
* [ ] Financial Accounts
* [ ] Receipts / Receipt Items
* [ ] Budgets
* [ ] Notifications
* [ ] AI / RAG
* [ ] Chat
* [ ] Database
* [ ] Frontend UI
* [ ] DevOps / Configuration
* [ ] Bug Fix
* [ ] Other: <!-- Specify -->

---

## API Changes

Endpoints Added:

* <!-- Endpoint(s) or None -->

Endpoints Modified:

* <!-- Endpoint(s) or None -->

Endpoints Removed:

* <!-- Endpoint(s) or None -->

---

## Database Changes

Migration required?

* [ ] Yes
* [ ] No

Details:

* <!-- Database tables/columns/constraints/indexes changed -->
* <!-- Flyway migrations added -->
* <!-- JPA/Hibernate changes -->
* <!-- Other database changes -->

---

## Security Review

### Authentication

* [ ] Authentication requirements verified
* [ ] Protected routes verified
* [ ] No authentication bypass introduced

### Authorization

* [ ] Authorization requirements verified
* [ ] Ownership checks verified
* [ ] No unauthorized access introduced

### Data Isolation

* [ ] Users can access only their own data
* [ ] Cross-user access is prevented
* [ ] Ownership checks are applied where required

### Sensitive Data

* [ ] No passwords or credentials exposed
* [ ] No secrets committed
* [ ] Sensitive data is handled appropriately

Notes:

<!-- Add security-related notes or write "Not applicable". -->

---

## AI Module Review (If Applicable)

* [ ] AI functionality tested
* [ ] RAG functionality tested
* [ ] Embeddings functionality tested
* [ ] Receipt/OCR processing tested
* [ ] No unintended AI behavior introduced

Changes:

<!-- Describe AI-related changes or write "Not applicable". -->

---

## Frontend Changes

Pages Added:

* <!-- Page(s) or None -->

Components Added:

* <!-- Component(s) or None -->

Forms Added/Updated:

* <!-- Form(s) or None -->

Routes Added/Modified:

* <!-- Route(s) or None -->

---

## Screenshots (Required for UI Changes)

<!-- Add screenshots here if the PR contains UI changes. -->

<!-- Not applicable if there are no UI changes. -->

---

## Testing Performed

### Backend

* [ ] Application starts successfully
* [ ] Unit tests passed
* [ ] Integration tests passed
* [ ] API endpoints tested
* [ ] Validation tested
* [ ] Error cases tested
* [ ] Database changes tested
* [ ] Flyway migrations tested
* [ ] Hibernate/JPA validation tested

Notes:

<!-- Describe the tests performed and important results. -->

### Frontend

* [ ] UI tested
* [ ] Form validation tested
* [ ] Responsive behavior checked
* [ ] Error/loading states tested

Notes:

<!-- Describe frontend testing or write "Not applicable". -->

### Integration

* [ ] Frontend connected to backend
* [ ] API responses verified
* [ ] Database integration verified

Notes:

<!-- Add integration testing details or write "Not applicable". -->

---

## Analytics Verification (If Applicable)

* [ ] Dashboard calculations verified
* [ ] Transaction aggregations verified
* [ ] Category aggregations verified
* [ ] Date-based calculations verified
* [ ] Filtering verified

Notes:

<!-- Add analytics verification details or write "Not applicable". -->

---

## Deployment Notes

Anything reviewers should know before merging?

* <!-- Migration instructions -->
* <!-- Configuration requirements -->
* <!-- Environment variables -->
* <!-- Deployment considerations -->

---

## Final Checklist

* [ ] Code builds successfully
* [ ] Tests pass
* [ ] Lint/formatting passes
* [ ] No console errors
* [ ] No hardcoded secrets
* [ ] Database migrations tested
* [ ] API documentation updated if needed
* [ ] Security review completed
* [ ] Tested before opening PR
* [ ] PR follows project architecture
* [ ] PR is reasonably scoped and reviewable
* [ ] No unnecessary changes included
* [ ] Ready for review
