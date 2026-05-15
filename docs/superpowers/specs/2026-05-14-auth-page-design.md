# Auth Page — Login & Sign-Up UI

**Date:** 2026-05-14  
**Status:** Approved  
**Scope:** Frontend only — no API calls, replaces current `App.jsx` dashboard temporarily

---

## Visual Tokens

| Token | Value |
|---|---|
| Page background | `#F8F7F0` (cream) |
| Card background | `#F8F7F0` |
| Active tab background | `#F5D26B` (golden yellow) |
| Active tab text | black |
| Inactive tab background | `#4A4A4A` (dark gray) |
| Inactive tab text | white |
| Input border | `#3A3A3A`, 2px solid, fully pill-rounded |
| Input background | transparent |
| Label | black, `font-bold`, `text-lg` |
| Continue button | `#F5D26B`, black text, pill shape, full-width |
| Avatar placeholder | `#D9D9D9` circle, ~200px diameter |
| Forgot Password link | gray, underlined, small |

---

## Architecture

`App.jsx` renders only `<AuthPage />`.

```
AuthPage
├── AuthTabs
├── SignInForm   (rendered when activeTab === 'signin')
│   ├── AvatarPlaceholder
│   ├── InputField (username)
│   ├── InputField (password)
│   ├── PillButton (Continue)
│   └── Forgot Password link
└── SignUpForm   (rendered when activeTab === 'signup')
    ├── InputField (name)
    ├── InputField (phone)
    ├── InputField (email)
    ├── InputField (password)
    ├── InputField (confirm password)
    └── PillButton (Continue)
```

---

## File Structure

```
src/
  components/
    ui/
      InputField.jsx
      PillButton.jsx
      AvatarPlaceholder.jsx
    auth/
      AuthPage.jsx
      AuthTabs.jsx
      SignInForm.jsx
      SignUpForm.jsx
  App.jsx
  index.css
  main.jsx
```

---

## Component Contracts

### `AuthPage.jsx`
- State: `activeTab: 'signin' | 'signup'`, initialised to `'signin'`
- Renders card shell (white/cream card, centered, max-width ~420px), `<AuthTabs>`, and the active form

### `AuthTabs.jsx`
- Props: `activeTab`, `onTabChange`
- Renders a dark-gray pill container with two child pill buttons
- Active button: `#F5D26B` bg, black text; inactive: transparent bg, white text

### `SignInForm.jsx`
- Local state: `username`, `password`
- Renders: `AvatarPlaceholder`, two `InputField`s, `PillButton`, horizontal `<hr>`, "Forgot Password?" `<a>`
- Continue: `preventDefault`, validates non-empty fields, logs `{ username }` to console

### `SignUpForm.jsx`
- Local state: `name`, `phone`, `email`, `password`, `confirm`
- Renders: five `InputField`s, `PillButton`
- Continue: `preventDefault`, validates non-empty + passwords match, logs `{ name, phone, email }` to console

### `ui/InputField.jsx`
- Props: `label`, `value`, `onChange`, `type` (default `"text"`), `placeholder`
- Renders: bold `<label>` above a full-width pill `<input>`

### `ui/PillButton.jsx`
- Props: `children`, `onClick`, `className` (optional override)
- Renders: full-width `#F5D26B` pill `<button>`

### `ui/AvatarPlaceholder.jsx`
- No props
- Renders: centered `#D9D9D9` circle, ~200px, `rounded-full`

---

## Data Flow

- Tab state lives in `AuthPage`, passed to `AuthTabs` as controlled props
- Form state lives locally in each form component — switching tabs does not reset state
- No global state, no context, no API calls
- Validation is client-side only; failures are silent (no error UI in this pass)

---

## Out of Scope

- Backend API integration (login/register endpoints)
- React Router routing
- Error/validation UI
- Loading states or spinners
- Persistent session / JWT storage
