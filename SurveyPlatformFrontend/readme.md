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

The API configuration is located in:

```text
src/services/api.ts
```

Create a local `.env` file based on `.env.example`.

Example:

```env
VITE_API_BASE_URL=http://localhost:8080
```

Environment-specific values should not be committed directly to the repository.

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