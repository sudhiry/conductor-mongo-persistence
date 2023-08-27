# Conductor server



# Local development start

1. Build `server` project
2. Build `ui` project
3. Run `docker compose build`
4. Run `docker compose up -d`
   - There is dependency on "ElasticSearch" image which is commented because of Volume issue in Colima
   - If volume is not working in Colima then install separate elastic search
5. UI is still updated in the docker compose.