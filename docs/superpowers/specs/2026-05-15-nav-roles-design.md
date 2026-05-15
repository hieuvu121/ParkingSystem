# Navigation & Role-Based UI Design

**Date:** 2026-05-15
**Status:** Approved
**Scope:** Top navbar shell, role-aware AuthContext, Account Settings page, Admin User Management page. No React Router — page state only.

---

## Architecture

### Page State

`App.jsx` owns a `page` state that drives which page renders:

```
'dashboard' | 'account' | 'admin-users'
```

`TopNav` receives `page` and `setPage` as props. Navigating is a `setPage(...)` call — no URL changes.

### AuthContext Changes

`AuthContext` gains a `user` field: `{ id, fullName, email, role }`.

- On `login()`: after storing the token, immediately call `GET /api/users/me` and store the result as `user`
- On app init (token already in localStorage): call `GET /api/users/me` to rehydrate `user` — if it fails (expired token), call `logout()`
- `user` is `null` while loading or when logged out
- `role === 'ADMIN'` is the single source of truth for what nav items and pages appear

`useAuth()` continues to export `{ token, user, login, register, logout }`.

---

## File Map

| Action | Path | Responsibility |
|---|---|---|
| Create | `src/api/user.js` | `getMe()`, `updateMe(fullName)` |
| Create | `src/api/subscription.js` | `getMySubscription()` |
| Modify | `src/context/AuthContext.jsx` | Add `user` state, fetch on login + init |
| Create | `src/components/layout/TopNav.jsx` | Shared top navigation bar |
| Modify | `src/App.jsx` | Add `page` state, render TopNav + active page |
| Create | `src/pages/AccountPage.jsx` | Profile edit + subscription + sign out |
| Create | `src/pages/AdminUsersPage.jsx` | Admin user table (list, change role, delete) |

---

## Components

### `TopNav`

Props: `page`, `setPage`, `user`, `wsConnected`

**Normal user nav items:**
- Dashboard → `setPage('dashboard')`
- Bookings → `setPage('bookings')` *(placeholder — navigates to dashboard for now)*
- Account → `setPage('account')`

**Admin nav items:**
- Dashboard → `setPage('dashboard')`
- Users → `setPage('admin-users')`
- Analytics → `setPage('dashboard')` *(placeholder — links to dashboard for now)*
- Account → `setPage('account')`

**Visual tokens:**
- Container: `bg-[#1C1C1E]`, `border-b border-[#2C2C2E]`
- Active item: `text-[#F5D26B]` + `border-b-2 border-[#F5D26B]`
- Inactive item: `text-[#A1A1AA] hover:text-white`
- Logo: `text-[#F5D26B] font-bold` 🅿 + "Parking System"
- Live dot: pulsing green `#4ADE80` when `wsConnected`, gray `#6B7280` otherwise
- Mobile (< 768px): text labels hidden, show icon-only nav items using emoji/SVG

### `App.jsx`

```jsx
// App.jsx holds wsConnected so TopNav can show the live dot
// across all pages (not just while DashboardPage is mounted)
const { token } = useAuth();
const [page, setPage] = useState('dashboard');
const [wsConnected, setWsConnected] = useState(false);

if (!token) return <AuthPage />;
return (
  <>
    <TopNav page={page} setPage={setPage} user={user} wsConnected={wsConnected} />
    {page === 'dashboard'   && <DashboardPage onWsStatusChange={setWsConnected} />}
    {page === 'account'     && <AccountPage />}
    {page === 'admin-users' && <AdminUsersPage />}
  </>
);
```

`wsConnected` state lives in `App.jsx`. `DashboardPage` receives an `onWsStatusChange(bool)` callback prop and calls it when the STOMP client connects or disconnects. `App.jsx` passes `wsConnected` to `TopNav` for the live dot.

### `AccountPage`

Sections (vertical stack, white card on `#111111` background):

**Profile:**
- Read-only: `fullName`, `email`
- Edit mode: clicking "Edit" shows text input pre-filled with `fullName` + Save / Cancel
- Save calls `PUT /api/users/me` → updates `user` in AuthContext via a new `updateUser(profile)` setter

**Preferred centre:** list item, "Coming soon" label — no API

**Notifications:** list item, "Coming soon" label — no API

**Subscription:**
- Calls `GET /api/subscriptions/my` on mount
- Active: shows `packageName`, `startDate` → `endDate`
- None (404): shows "No active subscription"

**Sign Out:**
- Golden pill button at the bottom
- Calls `logout()` → clears token + user → App renders `<AuthPage />`

### `AdminUsersPage`

- Fetches `GET /api/admin/users?page=0&size=20` on mount
- Displays paginated table: columns = #, Full Name, Email, Role, Actions
- **Change Role button:** toggles `USERS` ↔ `ADMIN` via `PATCH /api/admin/users/{id}/role`; disabled when row is the logged-in user's own account
- **Delete button:** shows inline "Are you sure? [Confirm] [Cancel]" before calling `DELETE /api/admin/users/{id}`; removes row from list on success
- Loading skeleton while fetching; inline error message on failure

---

## API Modules

### `src/api/user.js`
```js
getMe()                    // GET /api/users/me → UserProfileResponse
updateMe(fullName)         // PUT /api/users/me → UserProfileResponse
listUsers(page, size)      // GET /api/admin/users?page&size → PageResponse<UserProfileResponse>
changeRole(id, role)       // PATCH /api/admin/users/{id}/role → UserProfileResponse
deleteUser(id)             // DELETE /api/admin/users/{id} → void
```

### `src/api/subscription.js`
```js
getMySubscription()        // GET /api/subscriptions/my → SubscriptionResponse | throws 404
```

---

## Data Flow

```
App mounts
  → token in localStorage?
      yes → GET /api/users/me
              ok  → set user, render TopNav + page
              fail → logout(), render AuthPage
      no  → render AuthPage

AuthPage: login()
  → POST /api/auth/login → token
  → GET /api/users/me    → user
  → set token + user in context
  → App re-renders → TopNav + DashboardPage

AccountPage mounts
  → GET /api/subscriptions/my → show subscription or "none"
  → Edit name → PUT /api/users/me → updateUser() in context

AdminUsersPage mounts
  → GET /api/admin/users → render table
  → Change role → PATCH → update row in local state
  → Delete → confirm → DELETE → remove row from local state
```

---

## Out of Scope

- React Router / URL-based navigation
- My Bookings page (separate spec)
- Admin Analytics page (separate spec)
- Notifications settings (no API)
- Preferred centre settings (no API)
- WebSocket auth (WS uses same token)
