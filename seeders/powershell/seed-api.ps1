param(
  [string]$Gateway = "http://localhost:8080",
  [int]$DaysBack = 7,                # aantal dagen terug om workouts te maken (0 = alleen vandaag)
  [int]$TargetIncrements = 6,        # progress → exact naar dit aantal increments per user
  [int]$PauseMs = 0,                 # throttle tussen requests
  [switch]$OnlyUsers,
  [switch]$OnlyWorkouts,
  [switch]$OnlyProgress,
  [switch]$DryRun                    # geen calls, alleen tonen wat zou gebeuren
)

$ErrorActionPreference = "Stop"

function Sleep-IfNeeded() { if ($PauseMs -gt 0) { Start-Sleep -Milliseconds $PauseMs } }

function Invoke-WithRetry {
  param([scriptblock]$Action, [int]$Retries = 3, [int]$DelayMs = 400)
  $last = $null
  for ($i=1; $i -le $Retries; $i++) {
    try { return & $Action } catch { $last = $_; Start-Sleep -Milliseconds $DelayMs }
  }
  if ($last) { throw $last } else { throw "Unknown error after $Retries retries" }
}

function Invoke-Json {
  param(
    [string]$Method, [string]$Url, $Body = $null,
    [string]$ContentType = "application/json"
  )
  if ($DryRun) { Write-Host "DRYRUN $Method $Url`n$($Body | ConvertTo-Json -Depth 8)" -ForegroundColor Yellow; return $null }
  Invoke-WithRetry { 
    if ($Body -ne $null) {
      $json = $Body | ConvertTo-Json -Depth 10
      Invoke-RestMethod -Method $Method -Uri $Url -ContentType $ContentType -Body $json
    } else {
      Invoke-RestMethod -Method $Method -Uri $Url
    }
  }
}

function Check-Health {
  Write-Host "Checking health..." -ForegroundColor Cyan
  $res = Invoke-WithRetry { Invoke-RestMethod -Uri "$Gateway/actuator/health" -Method GET }
  $res
}

# ---------- Data ----------
$Users = @(
  @{ id="11111111-1111-1111-1111-111111111111"; email="alice@example.com"; displayName="Alice";   best=@{bench=85; squat=120; deadlift=150} },
  @{ id="22222222-2222-2222-2222-222222222222"; email="bob@example.com";   displayName="Bob";     best=@{bench=95; squat=140; deadlift=180} },
  @{ id="33333333-3333-3333-3333-333333333333"; email="cara@example.com";  displayName="Cara";    best=@{bench=60; squat=90;  deadlift=120} },
  @{ id="44444444-4444-4444-4444-444444444444"; email="dave@example.com";  displayName="Dave";    best=@{bench=110; squat=160; deadlift=200} },
  @{ id="55555555-5555-5555-5555-555555555555"; email="eve@example.com";   displayName="Eve";     best=@{bench=50;  squat=70;  deadlift=95} },
  @{ id="66666666-6666-6666-6666-666666666666"; email="frank@example.com"; displayName="Frank";   best=@{bench=80;  squat=110; deadlift=145} }
)

# Workout templates per dag (simpele basis)
$Templates = @{
  "Upper" = @(
    @{ name="Bench Press";      sets=3; reps=10; weight=40.0 },
    @{ name="Incline DB Press"; sets=3; reps=8;  weight=22.5 },
    @{ name="Row";              sets=3; reps=10; weight=35.0 }
  )
  "Lower" = @(
    @{ name="Squat";            sets=3; reps=8;  weight=60.0 },
    @{ name="Romanian Deadlift";sets=3; reps=8;  weight=70.0 }
  )
  "Pull" = @(
    @{ name="Deadlift";         sets=2; reps=5;  weight=90.0 },
    @{ name="Lat Pulldown";     sets=3; reps=10; weight=45.0 }
  )
  "Push" = @(
    @{ name="Overhead Press";   sets=3; reps=8;  weight=30.0 },
    @{ name="Dips";             sets=3; reps=10; weight=0.0 }
  )
}

# Roteer templates per dag
function Get-TemplateForDay([int]$offset) {
  $keys = @("Upper","Lower","Pull","Push")
  $idx = ($offset % $keys.Count)
  $Templates[$keys[$idx]]
}

# ---------- Seeder helpers ----------
function Upsert-Users {
  $endpoint = "$Gateway/api/users"
  foreach ($u in $Users) {
    try {
      $resp = Invoke-Json -Method "POST" -Url $endpoint -Body @{
        id=$u.id; email=$u.email; displayName=$u.displayName
      }
      Write-Host "User upserted:" ($resp | ConvertTo-Json -Depth 4)
    } catch { Write-Warning "User seed failed for $($u.email): $($_.Exception.Message)" }
    Sleep-IfNeeded
  }
}

function Create-Workouts {
  $endpoint = "$Gateway/api/workouts"
  $today = Get-Date
  foreach ($u in $Users) {
    for ($d = 0; $d -le $DaysBack; $d++) {
      $date = $today.AddDays(-$d).ToString("o")
      $exs = @( Get-TemplateForDay $d )
      try {
        $resp = Invoke-Json -Method "POST" -Url $endpoint -Body @{
          userId = $u.id
          date = $date
          exercises = $exs
        }
        Write-Host "Workout created for $($u.displayName) $($date.Substring(0,10))" `
          -ForegroundColor DarkGreen
      } catch { Write-Warning "Workout seed failed ($($u.displayName) d-$d): $($_.Exception.Message)" }
      Sleep-IfNeeded
    }
  }
}

function Ensure-Progress-Record([string]$userId) {
  $url = "$Gateway/api/progress?userId=$userId"
  try {
    return Invoke-Json -Method "GET" -Url $url
  } catch {
    Write-Warning "Progress GET failed for ${userId}: $($_.Exception.Message)"
    return $null
  }
}


function Ensure-Progress-Increments([string]$userId, [int]$target) {
  $p = Ensure-Progress-Record $userId
  if (-not $p) { return }

  $curr = [int]$p.workoutsCompleted
  $diff = [math]::Max(0, $target - $curr)
  if ($diff -le 0) {
    Write-Host "Progress already >= target ($curr >= $target) for $userId" -ForegroundColor Yellow
    return
  }

  $inc = "$Gateway/api/progress/$userId/increment"
  1..$diff | ForEach-Object {
    try {
      Invoke-Json -Method "PUT" -Url $inc -Body @{} | Out-Null
    } catch { Write-Warning "Progress increment failed ($userId): $($_.Exception.Message)" }
    Sleep-IfNeeded
  }
  $p2 = Ensure-Progress-Record $userId
  Write-Host "Progress ensured to $($p2.workoutsCompleted) for $userId" -ForegroundColor DarkCyan
}

function Upsert-BestLifts([string]$userId, $bestObj, [int]$workoutsCompleted) {
  $endpoint = "$Gateway/api/progress"
  # JSON-string (geen -Compress nodig)
  $bestJson = ($bestObj | ConvertTo-Json -Depth 10)
  try {
    $resp = Invoke-Json -Method "PUT" -Url $endpoint -Body @{
      userId = $userId
      workoutsCompleted = $workoutsCompleted
      bestLifts = $bestJson
    }
    Write-Host "Best lifts upserted for ${userId} -> $bestJson" -ForegroundColor DarkYellow
  } catch {
    Write-Warning "Best lifts upsert failed (${userId}): $($_.Exception.Message)"
  }
}


function Summary {
  Write-Host ""
  Write-Host "=== SUMMARY (progress per user) ===" -ForegroundColor Cyan
  foreach ($u in $Users) {
    $p = $null
    try { $p = Invoke-RestMethod -Uri "$Gateway/api/progress?userId=$($u.id)" -Method GET } catch {}
    if ($p) {
      "{0,-6} {1} → workoutsCompleted={2} updatedAt={3}" -f `
        $u.displayName, $u.id, $p.workoutsCompleted, $p.updatedAt
    } else {
      "{0,-6} {1} → (no progress)" -f $u.displayName, $u.id
    }
  }
}

# ---------- RUN ----------
$hc = Check-Health
if (-not $hc -or $hc.status -ne "UP") { throw "Gateway health not UP: $($hc | ConvertTo-Json -Depth 6)" }

if (-not $OnlyWorkouts -and -not $OnlyProgress) {
  Upsert-Users
}

if (-not $OnlyUsers -and -not $OnlyProgress) {
  Create-Workouts
}

if (-not $OnlyUsers -and -not $OnlyWorkouts) {
  foreach ($u in $Users) {
    # 1) Zorg dat record bestaat & increment exact naar target
    Ensure-Progress-Increments -userId $u.id -target $TargetIncrements
    # 2) Best lifts upsert met het (target) workoutsCompleted
    Upsert-BestLifts -userId $u.id -bestObj $u.best -workoutsCompleted $TargetIncrements
  }
}

Summary
Write-Host "`nDONE." -ForegroundColor Green
