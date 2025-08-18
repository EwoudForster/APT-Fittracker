# Seeders voor FitnessTracker

Je hebt 2 manieren om data in te laden:

## Optie 1 — Via de API (aanrader zolang de app draait)
- Bestand: `powershell/seed-api.ps1`
- Draait health-check, maakt users, voegt workouts toe, en increment progress via de Gateway.

Gebruik:
```powershell
cd powershell
./seed-api.ps1
```

## Optie 2 — Direct in de databases
### Postgres (users)
- Bestand: `postgres/seed-users.sql`
- Laad het script in de `usersdb` database:
```powershell
docker compose cp postgres/seed-users.sql postgres:/tmp/seed-users.sql
docker compose exec -T postgres psql -U admin -d usersdb -f /tmp/seed-users.sql
```

### Mongo (workouts)
- Bestand: `mongo/seed-workouts.js`
- Laad in de `fittrackr` database:
```powershell
docker compose cp mongo/seed-workouts.js mongo:/tmp/seed-workouts.js
docker compose exec -T mongo mongosh --quiet --eval "load('/tmp/seed-workouts.js')"
```

> Let op: Progress gebruikt Postgres in deze setup. Omdat het schema kan variëren, zaaien we progress bij voorkeur via de **API** (increment endpoint).

## Herhaald seeden
De scripts gebruiken vaste UUID’s voor users (Alice/Bob/Cara). Daardoor kan je veilig meerdere keren seeden zonder duplicaten.
