# Auth API Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Wire Sign In / Sign Up forms to the Spring Boot backend via an apiFetch wrapper and AuthContext, with inline success/error messages on every response.

**Architecture:** `api/client.js` owns a single `apiFetch` wrapper that attaches the JWT Bearer token to every request; `api/auth.js` exposes `login` and `register` using that wrapper; `AuthContext` holds token state and calls the API; forms call context methods and display mapped messages inline.

**Tech Stack:** React 18, Vite 5 (proxy to localhost:8080), Tailwind CSS 3. No extra libraries.

---

## File Map

| Action | Path | Responsibility |
|---|---|---|
| Modify | `vite.config.js` | Proxy `/api/*` → `http://localhost:8080` |
| Create | `src/api/client.js` | `apiFetch` — attaches Bearer token to every fetch |
| Create | `src/api/auth.js` | `login(email, pw)`, `register(fullName, email, pw)` |
| Create | `src/context/AuthContext.jsx` | Token state, `login` / `register` / `logout` actions |
| Modify | `src/main.jsx` | Wrap `<App />` in `<AuthProvider />` |
| Modify | `src/components/auth/SignInForm.jsx` | Email label, API call, inline messages, loading state |
| Modify | `src/components/auth/SignUpForm.jsx` | Remove phone field, API call, inline messages, loading state |

---

### Task 1: Vite proxy

**Files:**
- Modify: `vite.config.js`

- [ ] **Step 1: Add proxy to vite.config.js**

```js
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
```

- [ ] **Step 2: Verify the file saves without syntax errors**

```bash
node -e "import('./vite.config.js').then(() => console.log('ok'))"
```
Expected: `ok` printed, no error.

- [ ] **Step 3: Commit**

```bash
git add vite.config.js
git commit -m "feat: proxy /api to localhost:8080 in dev"
```

---

### Task 2: api/client.js — apiFetch wrapper

**Files:**
- Create: `src/api/client.js`

- [ ] **Step 1: Create the file**

```js
export function apiFetch(path, options = {}) {
  const token = localStorage.getItem('token');
  return fetch(path, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  });
}
```

- [ ] **Step 2: Verify the file exists**

```bash
ls src/api/client.js
```
Expected: file listed, no error.

- [ ] **Step 3: Commit**

```bash
git add src/api/client.js
git commit -m "feat: add apiFetch wrapper with Bearer token support"
```

---

### Task 3: api/auth.js — login and register

**Files:**
- Create: `src/api/auth.js`

- [ ] **Step 1: Create the file**

```js
import { apiFetch } from './client';

export async function login(email, password) {
  const res = await apiFetch('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  });
  const data = await res.json();
  if (!res.ok) throw { status: res.status, message: data.message ?? 'Unknown error' };
  return data; // { token }
}

export async function register(fullName, email, password) {
  const res = await apiFetch('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify({ fullName, email, password }),
  });
  const data = await res.json();
  if (!res.ok) throw { status: res.status, message: data.message ?? 'Unknown error' };
  return data; // { message }
}
```

- [ ] **Step 2: Verify the file exists**

```bash
ls src/api/auth.js
```
Expected: file listed, no error.

- [ ] **Step 3: Commit**

```bash
git add src/api/auth.js
git commit -m "feat: add auth API — login and register"
```

---

### Task 4: AuthContext

**Files:**
- Create: `src/context/AuthContext.jsx`

- [ ] **Step 1: Create the file**

```jsx
import { createContext, useContext, useState } from 'react';
import { login as apiLogin, register as apiRegister } from '../api/auth';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem('token'));

  async function login(email, password) {
    const data = await apiLogin(email, password); // throws on error
    localStorage.setItem('token', data.token);
    setToken(data.token);
  }

  async function register(fullName, email, password) {
    await apiRegister(fullName, email, password); // throws on error, no token stored
  }

  function logout() {
    localStorage.removeItem('token');
    setToken(null);
  }

  return (
    <AuthContext.Provider value={{ token, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
```

- [ ] **Step 2: Verify the file exists**

```bash
ls src/context/AuthContext.jsx
```
Expected: file listed, no error.

- [ ] **Step 3: Commit**

```bash
git add src/context/AuthContext.jsx
git commit -m "feat: add AuthContext with login, register, logout"
```

---

### Task 5: Wrap App in AuthProvider

**Files:**
- Modify: `src/main.jsx`

- [ ] **Step 1: Update main.jsx**

```jsx
import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App.jsx";
import { AuthProvider } from "./context/AuthContext.jsx";
import "./index.css";

ReactDOM.createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <AuthProvider>
      <App />
    </AuthProvider>
  </React.StrictMode>
);
```

- [ ] **Step 2: Commit**

```bash
git add src/main.jsx
git commit -m "feat: wrap App in AuthProvider"
```

---

### Task 6: Update SignInForm

**Files:**
- Modify: `src/components/auth/SignInForm.jsx`

- [ ] **Step 1: Replace the file content**

```jsx
import { useState } from 'react';
import AvatarPlaceholder from '../ui/AvatarPlaceholder';
import InputField from '../ui/InputField';
import PillButton from '../ui/PillButton';
import { useAuth } from '../../context/AuthContext';

function mapSignInError(status) {
  if (status === 401) return 'Invalid email or password.';
  if (status === 403) return 'Account not verified. Please check your email.';
  return 'Something went wrong. Please try again.';
}

export default function SignInForm() {
  const { login } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [status, setStatus] = useState('idle'); // idle | loading | success | error
  const [message, setMessage] = useState('');

  async function handleSubmit(e) {
    e.preventDefault();
    if (!email || !password) return;
    setStatus('loading');
    setMessage('');
    try {
      await login(email, password);
      setStatus('success');
      setMessage('Login successful!');
    } catch (err) {
      setStatus('error');
      setMessage(mapSignInError(err.status));
    }
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-6">
      <AvatarPlaceholder />
      <InputField
        label="Email"
        type="email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
      />
      <InputField
        label="Password"
        type="password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
      />
      <PillButton onClick={handleSubmit} disabled={status === 'loading'}>
        {status === 'loading' ? 'Loading...' : 'Continue'}
      </PillButton>
      {message && (
        <p className={`text-center text-sm font-medium ${status === 'success' ? 'text-green-600' : 'text-red-500'}`}>
          {message}
        </p>
      )}
      <hr className="border-[#3A3A3A]" />
      <div className="text-center">
        <a href="#" className="text-sm text-gray-400 underline">
          Forgot Password?
        </a>
      </div>
    </form>
  );
}
```

- [ ] **Step 2: Update PillButton to accept disabled prop**

Open `src/components/ui/PillButton.jsx` and replace with:

```jsx
export default function PillButton({ children, onClick, disabled = false, className = '' }) {
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      className={`w-full rounded-full bg-[#F5D26B] py-4 text-xl font-bold text-black transition hover:brightness-95 disabled:opacity-50 disabled:cursor-not-allowed ${className}`}
    >
      {children}
    </button>
  );
}
```

- [ ] **Step 3: Commit**

```bash
git add src/components/auth/SignInForm.jsx src/components/ui/PillButton.jsx
git commit -m "feat: wire SignInForm to backend with inline messages"
```

---

### Task 7: Update SignUpForm

**Files:**
- Modify: `src/components/auth/SignUpForm.jsx`

- [ ] **Step 1: Replace the file content**

```jsx
import { useState } from 'react';
import InputField from '../ui/InputField';
import PillButton from '../ui/PillButton';
import { useAuth } from '../../context/AuthContext';

function mapSignUpError(status, backendMessage) {
  if (status === 409) return 'This email is already registered.';
  if (status === 400) return backendMessage ?? 'Invalid input. Please check your details.';
  return 'Something went wrong. Please try again.';
}

export default function SignUpForm() {
  const { register } = useAuth();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [status, setStatus] = useState('idle'); // idle | loading | success | error
  const [message, setMessage] = useState('');

  async function handleSubmit(e) {
    e.preventDefault();
    if (!name || !email || !password || !confirm) return;
    if (password !== confirm) {
      setStatus('error');
      setMessage('Passwords do not match.');
      return;
    }
    setStatus('loading');
    setMessage('');
    try {
      await register(name, email, password);
      setStatus('success');
      setMessage('Registration successful. Please check your email to verify your account.');
    } catch (err) {
      setStatus('error');
      setMessage(mapSignUpError(err.status, err.message));
    }
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-6">
      <InputField
        label="Name"
        value={name}
        onChange={(e) => setName(e.target.value)}
      />
      <InputField
        label="Email"
        type="email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
      />
      <InputField
        label="Password"
        type="password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
      />
      <InputField
        label="Confirm password"
        type="password"
        value={confirm}
        onChange={(e) => setConfirm(e.target.value)}
      />
      <PillButton onClick={handleSubmit} disabled={status === 'loading'}>
        {status === 'loading' ? 'Loading...' : 'Continue'}
      </PillButton>
      {message && (
        <p className={`text-center text-sm font-medium ${status === 'success' ? 'text-green-600' : 'text-red-500'}`}>
          {message}
        </p>
      )}
    </form>
  );
}
```

- [ ] **Step 2: Commit**

```bash
git add src/components/auth/SignUpForm.jsx
git commit -m "feat: wire SignUpForm to backend with inline messages"
```

---

### Task 8: End-to-end smoke test

- [ ] **Step 1: Ensure backend is running**

The Spring Boot app must be running on `http://localhost:8080`. If not, start it from the backend directory.

- [ ] **Step 2: Start the frontend dev server**

```bash
npm run dev
```
Expected: Vite starts at `http://localhost:5173`.

- [ ] **Step 3: Test Sign Up — success path**

Open `http://localhost:5173`. Click Sign Up tab. Fill in:
- Name: `Test User`
- Email: a fresh email not in the DB
- Password: `password123`
- Confirm password: `password123`

Click Continue. Expected: green message `"Registration successful. Please check your email to verify your account."`

- [ ] **Step 4: Test Sign Up — duplicate email**

Fill the same email again. Click Continue. Expected: red message `"This email is already registered."`

- [ ] **Step 5: Test Sign Up — passwords mismatch**

Fill valid fields but set Confirm password to something different. Click Continue. Expected: red message `"Passwords do not match."`

- [ ] **Step 6: Test Sign In — unverified account**

Click Sign In tab. Use the email you just registered (not yet verified). Expected: red message `"Account not verified. Please check your email."`

- [ ] **Step 7: Test Sign In — wrong password**

Use a registered+verified email with wrong password. Expected: red message `"Invalid email or password."`

- [ ] **Step 8: Test Sign In — success**

Use a verified account's correct credentials. Expected: green message `"Login successful!"` and token visible in `localStorage` under key `token` (check DevTools → Application → Local Storage).
