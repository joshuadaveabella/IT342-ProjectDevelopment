# Dila-Dila – User Accounts, Authentication, Profile and Password Management (Increments 1 and 2)

Spring Boot 3 + PostgreSQL backend, React (Vite) frontend. Server-side sessions, BCrypt hashes, RBAC in both tiers.

## Run it
Requirements: JDK 17+, Maven 3.9+, Node 18+, PostgreSQL 14+.

1. Database: `createdb dila_dila` (tables are created automatically; `database/schema.sql` is the equivalent SQL).
2. Backend (credentials come from environment variables, nothing is committed):
   ```
   cd backend
   export DB_URL=jdbc:postgresql://localhost:5432/dila_dila DB_USERNAME=postgres DB_PASSWORD=<your password>
   mvn test              # integration tests (H2 in-memory)
   mvn spring-boot:run   # http://localhost:8080
   ```
3. Frontend: `cd frontend && npm install && npm run dev` then open http://localhost:5173

## Features
| Requirement | Where |
|---|---|
| Registration, validation, duplicate prevention, BCrypt(12), role assignment | `AuthController`, `AuthService`, `RegisterRequest`, unique index on `users.email` |
| Login / logout, generic error for bad credentials, session fixation protection, CSRF, 8h HttpOnly session | `AuthController`, `SecurityConfig` |
| Role dashboards (LEAD / MEMBER), redirect after login, role-based nav | `Login.jsx`, `Layout.jsx`, `*DashboardController` |
| RBAC backend: URL rules + `@PreAuthorize`; frontend: `ProtectedRoute` | `SecurityConfig`, controllers, `ProtectedRoute.jsx` |

API: `POST /api/auth/register|login|logout`, `GET /api/auth/me`, `GET /api/member/dashboard` (MEMBER, LEAD), `GET /api/lead/dashboard`, `GET /api/lead/users` (LEAD only). Errors: 400 validation, 401 bad/no session, 403 wrong role, 409 duplicate email.

## Demo script
1. Register a Project Lead and a Team Member at `/register`; try a duplicate email (409 message) and a weak password.
2. Check the DB: `select email, role, password_hash from users;` (hashes start with `$2a$12$`).
3. Sign in wrong password -> "Invalid email or password."
4. Sign in as Member -> `/member`; nav shows no lead links; open `/lead` -> 403 page; `curl` of `/api/lead/dashboard` with the member session -> 403.
5. Sign in as Lead -> `/lead` with account stats; `/lead/team` lists accounts.
6. Sign out -> back to login; the old session cookie now gets 401; `/member` redirects to login.

## Increment 2: profile and password management
| Requirement | Where |
|---|---|
| View/edit own profile (name, phone, bio, photo link); email and role are read-only | `ProfileController`, `AccountService`, `ProfileRequest`, `Profile.jsx` |
| Change password (current password required, confirm, new != old, BCrypt) | `POST /api/account/change-password`, `ChangePasswordRequest`, `Profile.jsx` |
| Forgot / reset password (random 256-bit token, SHA-256 hash stored, expires in 30 min, single use, same answer for unknown emails, 60 s per-account cool-down) | `PasswordResetController`, `PasswordResetService`, `PasswordResetToken`, `ForgotPassword.jsx`, `ResetPassword.jsx` |

New endpoints: `GET/PUT /api/profile`, `POST /api/account/change-password`, `POST /api/auth/forgot-password`, `POST /api/auth/reset-password`.
New tables/columns: `users.phone, bio, avatar_url, updated_at` and `password_reset_tokens` (see `database/schema.sql`; Hibernate creates them automatically).

**Reset emails.** With no SMTP configured (default), the reset link is printed in the backend console ("DEV MODE password reset link ...") so the flow can be demonstrated. To send real email set these environment variables: `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT` (587), `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`, `SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true`, `SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true`, `MAIL_FROM`. Use an app password, never a personal one in a file.

### Demo script (Increment 2)
1. Sign in, open **My Profile**, change the name/phone/bio, save; try an empty name and a `http://` photo link to see validation errors.
2. **Change password**: wrong current password (error), mismatch (error), then a valid change. Sign out; the old password is rejected, the new one works.
3. **Forgot password** on the login page: enter the email, then copy the link from the backend console (or email). Set a new password, sign in with it.
4. Open the same link again: "invalid or has expired". Check `select email, password_hash from users;` and `select * from password_reset_tokens;` (only hashes).

## SRS note
The SRS specifies Google sign-in (FR-001, NFR-004) with per-project roles. This increment implements local email/password accounts with account-level roles (LEAD, MEMBER) as the assignment requires. Suggested SRS addendum: local credentials are an additional sign-in method; Google OAuth remains planned for a later increment.
