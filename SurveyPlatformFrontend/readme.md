# SurveyPlatform Frontend

Frontend application for the **CS-42 Next-Generation Social Media Survey and Eye-Tracking Platform**.

The frontend provides the foundation for the participant-facing social media interfaces and the researcher-facing configurable widget editor. It is designed to support reusable social media components, multiple platform styles, and integration with backend services.

## Tech Stack

- React 18
- TypeScript
- Vite
- Tailwind CSS
- Craft.js
- React Router
- Axios
- pnpm

## Requirements

Before running the project, make sure the following tools are installed:

- Node.js
- pnpm

Check the installed versions with:

```bash
node --version
pnpm --version
```

## Installation

From the `SurveyPlatformFrontend` directory, install the project dependencies:

```bash
pnpm install
```

## Development

Start the local development server:

```bash
pnpm dev
```

Vite will display the local development URL in the terminal, usually:

```text
http://localhost:5173/
```

## Production Build

Create a production build with:

```bash
pnpm build
```

The generated production files will be placed in the `dist` directory.

## Project Structure

```text
SurveyPlatformFrontend/
├── public/
├── src/
│   ├── assets/
│   ├── auth/
│   │   ├── api/                   # Authentication API calls
│   │   ├── pages/                 # Login, registration, and password-change pages
│   │   └── types/                 # Authentication request/response types
│   ├── components/
│   │   ├── editor/
│   │   └── widgets/
│   ├── layouts/
│   ├── pages/
│   ├── routes/
│   ├── services/
│   ├── App.tsx
│   ├── index.css
│   └── main.tsx
├── .env.example
├── index.html
├── package.json
├── pnpm-lock.yaml
├── tsconfig.app.json
├── tsconfig.json
├── tsconfig.node.json
└── vite.config.ts
```

## Current Frontend Features

The current frontend foundation includes:

- React Router based page navigation
- Basic routes for the seven supported social media platforms:
  - Facebook
  - Instagram
  - TikTok
  - X
  - Threads
  - Bluesky
  - Truth Social
- Reusable frontend widget components
- Text, Avatar, Image, and Action Bar widgets
- Craft.js integration for configurable widgets
- Widget library toolbox
- Drag-and-drop widget creation into the editor canvas
- Tailwind CSS based styling
- Axios based API service configuration
- Researcher registration and login pages
- Login and registration page navigation
- JWT-based authenticated frontend session handling
- Protected researcher routes with redirect to `/login` when unauthenticated
- Researcher logout by clearing the locally stored authentication token
- Researcher password-change page and validation
- Cloudflare Turnstile integration for researcher registration
- Automatic Bearer-token attachment for authenticated backend requests

## Researcher Authentication

The frontend provides researcher authentication and protected navigation integrated with the Spring Boot backend.

Current authentication routes include:

- `/register` — researcher account registration
- `/login` — researcher login
- `/account/change-password` — authenticated password change

After a successful login, the returned JWT access token is stored in browser `localStorage`.

Authenticated API requests automatically include the token using the HTTP `Authorization` header:

```text
Authorization: Bearer <access-token>
```

Protected researcher routes redirect unauthenticated users to `/login`.

Logging out removes the stored authentication token and redirects the researcher back to the login page.

The registration page also integrates Cloudflare Turnstile for human verification before account creation.

## Reusable Widgets

Reusable widgets are located in:

```text
src/components/widgets/
```

The current reusable widgets include:

```text
TextWidget
AvatarWidget
ImageWidget
ActionBarWidget
```

These components are designed to be reused across different social media templates and integrated with the Craft.js editor.

## Widget Editor

The widget editor uses Craft.js to support configurable frontend components.

The current implementation supports:

- Displaying available widgets in a widget library
- Dragging widgets from the library
- Adding new widget instances to the editor canvas
- Rendering reusable React components inside the editor

Further functionality such as platform-specific styling, property editing, and study configuration integration will be added during later development.

## API Configuration

The frontend uses Axios for communication with backend services.

The shared API configuration is located in:

```text
src/services/api.ts
```

Create a local `.env` file based on `.env.example`.

Example:

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_TURNSTILE_SITE_KEY=your-turnstile-site-key
```

`VITE_TURNSTILE_SITE_KEY` is a public client-side key used by the registration page. It is different from the backend `TURNSTILE_SECRET`, which must remain private.

Environment-specific configuration should be stored in `.env` files as appropriate. Never place backend secrets, such as `TURNSTILE_SECRET`, in frontend `VITE_*` variables because Vite exposes them to the browser.

## Development Workflow

Development should be performed on separate feature branches.

Example:

```bash
git checkout main
git pull origin main
git checkout -b feat/example-feature
```

After completing and testing the implementation:

```bash
git add .
git commit -m "feat(frontend): describe the change"
git push -u origin feat/example-feature
```

Create a pull request and merge the feature branch into `main` after review and validation.

## Validation

Before submitting frontend changes, run:

```bash
pnpm build
```

The build should complete successfully without TypeScript or Vite errors.

## Eye-tracking Calibration (FR-49)

The headless calibration module and its integration boundaries are documented in
[docs/fr-49-calibration.md](docs/fr-49-calibration.md).

For changes to this module, also run:

```bash
pnpm lint
pnpm typecheck:eyetracking
pnpm test
pnpm exec playwright install chromium
pnpm test:browser
```

The browser tests use synthetic media tracks and do not request a real camera.
The participant page, durable session event queue and browsing gaze collector are
integration responsibilities described in the module documentation.
