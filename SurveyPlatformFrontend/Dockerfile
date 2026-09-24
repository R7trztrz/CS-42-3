FROM node:22-alpine AS build

WORKDIR /app

RUN corepack enable

ARG VITE_API_BASE_URL=http://localhost:8080
ARG VITE_TURNSTILE_SITE_KEY=your-turnstile-site-key
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_TURNSTILE_SITE_KEY=$VITE_TURNSTILE_SITE_KEY

COPY package.json pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile

COPY . .
RUN pnpm build

EXPOSE 5173

CMD ["pnpm", "preview", "--host", "0.0.0.0", "--port", "5173"]
