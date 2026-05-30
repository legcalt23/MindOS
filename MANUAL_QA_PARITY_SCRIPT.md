# MANUAL_QA_PARITY_SCRIPT.md

**Date:** 2026-05-27

---

## Instructions

Test each screen on both Android and iOS. For each screen, verify all listed items. Record PASS/FAIL and capture screenshots.

---

## 1. Auth Screen

| Test | Expected | Android | iOS |
|---|---|---|---|
| Screen renders | Email, password fields, login button | | |
| Empty submit | Error message shown | | |
| Invalid email | Error message shown | | |
| Wrong password | "Credenciales no validas" message | | |
| Successful login | Navigate to Main | | |
| Create account toggle | Shows display name field | | |
| Create account success | Navigate to Main | | |
| Network error | "No se pudo conectar" message | | |
| Dark mode | Dark background, green accent | | |
| Keyboard behavior | Fields visible above keyboard | | |
| Back button | No navigation (root screen) | | |
| Loading state | Spinner during auth | | |

---

## 2. Home / Hub Screen

| Test | Expected | Android | iOS |
|---|---|---|---|
| Screen renders | Hub with navigation options | | |
| Open profile | Navigate to profile | | |
| Open feed | Navigate to GlobalFeed | | |
| Open DM list | Navigate to DMList | | |
| Open settings | Navigate to Settings | | |
| Open wallet | Navigate to Moneda | | |
| Sign out | Navigate to Auth | | |
| Dark mode | Consistent dark theme | | |

---

## 3. Profile Screen

| Test | Expected | Android | iOS |
|---|---|---|---|
| Own profile loads | Name, handle, bio, stats visible | | |
| Edit mode | Fields become editable | | |
| Save changes | Success, fields update | | |
| Cancel edit | Reverts to original | | |
| Other user profile | Follow/block buttons visible | | |
| Follow action | State changes to Following | | |
| Unfollow action | State changes to Follow | | |
| Block action | State changes to Blocked | | |
| Empty publications | Empty state shown | | |
| Loading state | Spinner shown | | |
| Error state | Error message shown | | |
| Back navigation | Returns to previous screen | | |
| Dark mode | Consistent | | |

---

## 4. Global Feed Screen

| Test | Expected | Android | iOS |
|---|---|---|---|
| Feed loads | Posts with author, content, attachments | | |
| Empty feed | Empty state shown | | |
| Open post composer | Navigate to composer | | |
| Open profile from post | Navigate to profile | | |
| Open media from post | Navigate to media viewer | | |
| Scroll performance | Smooth, no jank | | |
| Loading state | Spinner shown | | |
| Error state | Error with retry | | |
| Back navigation | Returns to Main | | |
| Dark mode | Consistent | | |

---

## 5. DM List Screen

| Test | Expected | Android | iOS |
|---|---|---|---|
| Conversations load | List of chats with last message | | |
| Empty state | No conversations message | | |
| Open chat | Navigate to DMChat | | |
| Open cajonera | Navigate to Cajonera | | |
| Loading state | Spinner shown | | |
| Back navigation | Returns to Main | | |
| Dark mode | Consistent | | |

---

## 6. DM Chat Screen

| Test | Expected | Android | iOS |
|---|---|---|---|
| Messages load | Chat history visible | | |
| Send text message | Message appears in chat | | |
| Send media message | Attachment shown | | |
| Empty chat | No messages prompt | | |
| Open user info | Navigate to DMChatUserInfo | | |
| Open media | Navigate to MediaViewer | | |
| Loading state | Spinner shown | | |
| Back navigation | Returns to DMList | | |
| Keyboard behavior | Input visible above keyboard | | |
| Dark mode | Consistent | | |

---

## 7. Search Screen

| Test | Expected | Android | iOS |
|---|---|---|---|
| Search renders | Input field with radar visual | | |
| Type username | Results appear after debounce | | |
| No results | "Sin contactos" message | | |
| Select result | Navigate to profile | | |
| Invalid input | Helper text shown | | |
| Loading state | Scanning animation | | |
| Dark mode | Consistent | | |

---

## 8. Settings Screen

| Test | Expected | Android | iOS |
|---|---|---|---|
| Screen renders | Privacy toggle visible | | |
| Toggle privacy | Overlay activates/deactivates | | |
| Back navigation | Returns to Main | | |
| Dark mode | Consistent | | |

---

## 9. Media Viewer

| Test | Expected | Android | iOS |
|---|---|---|---|
| Image displays | Full-screen image | | |
| Dismiss | Returns to previous screen | | |
| Dark mode | Dark background | | |

---

## 10. Wallet / Moneda Screen

| Test | Expected | Android | iOS |
|---|---|---|---|
| Screen renders | Balance and transaction list | | |
| Loading state | Spinner | | |
| Error state | Error message | | |
| Back navigation | Returns to Main | | |
| Dark mode | Consistent | | |

---

## 11. Cross-cutting tests

| Test | Android | iOS |
|---|---|---|
| Airplane mode -> error shown | | |
| Airplane mode -> retry works | | |
| Force quit -> restart -> auth persisted | | |
| Deep background -> foreground | | |
| Font scale large (accessibility) | | |
| Screen reader navigation | | |
| Privacy overlay covers content | | |
