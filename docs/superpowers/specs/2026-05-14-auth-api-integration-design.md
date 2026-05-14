# Auth API Integration Design

**Date:** 2026-05-14
**Status:** Approved
**Scope:** Wire Sign In / Sign Up forms to Spring Boot backend; add apiFetch wrapper, AuthContext, Vite proxy

---

## Architecture

```
src/
  api/
    client.js          ← apiFetch wrapper — attaches Bearer token to every request
    auth.js            ← login(email, pw), register(name, email, pw)
  context/
    AuthContext.jsx    ← token state + login / register / logout actions
  components/
    auth/
      SignInForm.jsx   ← updated: email label, API call, inline messages
      SignUpForm.jsx   ← updated: phone field removed, API call, inline messages
vite.config.js         ← proxy /api → http://localhost:8080 (dev CORS workaround)
main.jsx               ← wrap <App /> in <AuthProvider />
```

---

## API Layer

### `src/api/client.js`

```js
const BASE = '';

export function apiFetch(path, options = {}) {
  const token = localStorage.getItem('token');
  return fetch(BASE + path, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  });
}
```

### `src/api/auth.js`

```js
// login → { token } on 200, throws { status, message } on error
export async function login(email, password) { ... }

// register → { message } on 201, throws { status, message } on error
export async function register(fullName, email, password) { ... }
```

Error shape thrown: `{ status: number, message: string }`

---

## AuthContext

**File:** `src/context/AuthContext.jsx`

**State:** `token` (string | null), initialised from `localStorage.getItem('token')`

**Methods:**
- `login(email, password)` — calls `api/auth.login`, stores token in state + localStorage, throws on error
- `register(fullName, email, password)` — calls `api/auth.register`, does NOT store token (email verification required), throws on error
- `logout()` — clears token from state + localStorage

**Consumer pattern:** forms call context methods, not `api/auth.js` directly

---

## Form Updates

### SignInForm changes
- Label: "Username" → "Email", `type="email"`
- Local state: `email`, `password`, `status` (`idle|loading|success|error`), `message`
- On submit: validate non-empty → set loading → call `AuthContext.login()` → set success or map error

### SignUpForm changes
- Remove `phone` / `setPhone` state and `InputField` for phone number
- Fields: Name, Email, Password, Confirm password
- Local state: `name`, `email`, `password`, `confirm`, `status`, `message`
- On submit: validate non-empty + passwords match → set loading → call `AuthContext.register()` → set success or map error

---

## Message Map

| HTTP status | Context | Message shown |
|---|---|---|
| 200 | Sign In | `"Login successful!"` (green) |
| 401 | Sign In | `"Invalid email or password."` (red) |
| 403 | Sign In | `"Account not verified. Please check your email."` (red) |
| 201 | Sign Up | `"Registration successful. Please check your email to verify your account."` (green) |
| 409 | Sign Up | `"This email is already registered."` (red) |
| 400 | Sign Up | backend validation message (red) |
| any other | both | `"Something went wrong. Please try again."` (red) |

Message banner appears below the Continue button. Continue button shows `"Loading..."` and is disabled while request is in flight.

---

## Vite Proxy

`vite.config.js` proxies all `/api/*` requests to `http://localhost:8080` in dev — no CORS config needed on the backend.

---

## Out of Scope

- Route guards / PrivateRoute / AdminRoute
- Token expiry / refresh
- Backend CORS config for production
- Redirect after login
- Phone number field on backend
