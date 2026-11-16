# GitStats

## Quick Start

1. Create an access token in GitLab with API read permissions (`read_api` / `api`).
2. Add the token to the `.env` file in the project root directory:
   ```env
   PRIVATE_TOKEN=XXX
   ```
3. Start the services using Docker Compose:
   ```bash
   docker compose -f path-to-project\docker-compose.yml -p gitstats up -d --build
   ```
4. On the **Projects** page, click **Add default projects**.
5. Wait until projects are synced.