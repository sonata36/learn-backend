param(
    [int]$Port = 18080,
    [string]$JarPath = ""
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
if (!$JarPath) {
    $JarPath = Join-Path $projectRoot "target/learn-backend-0.0.1-SNAPSHOT.jar"
}
$JarPath = (Resolve-Path -LiteralPath $JarPath).Path
$java = (Get-Command java -ErrorAction Stop).Source
$baseUrl = "http://127.0.0.1:$Port"
$runId = [Guid]::NewGuid().ToString("N")
$todoId = $null
$script:appProcess = $null
$script:startCount = 0

# Refuse to send acceptance requests to an unrelated running application.
$listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, $Port)
try { $listener.Start() } finally { $listener.Stop() }

function Start-TestApp {
    $script:startCount++
    $stdout = Join-Path $projectRoot "target/stage1-$runId-$script:startCount.log"
    $stderr = Join-Path $projectRoot "target/stage1-$runId-$script:startCount-error.log"
    $script:appProcess = Start-Process -FilePath $java -ArgumentList @(
        "-jar", "`"$JarPath`"", "--server.port=$Port", "--server.address=127.0.0.1"
    ) -WorkingDirectory $projectRoot -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput $stdout -RedirectStandardError $stderr
    for ($attempt = 0; $attempt -lt 120; $attempt++) {
        if ($script:appProcess.HasExited) {
            throw "Application exited. Inspect $stdout and $stderr"
        }
        try {
            $health = Invoke-RestMethod "$baseUrl/actuator/health" -TimeoutSec 2
            if ($health.status -eq "UP") { return }
        } catch { }
        Start-Sleep -Milliseconds 500
    }
    throw "Application did not become ready. Inspect $stdout and $stderr"
}

function Stop-TestApp {
    if ($script:appProcess -and !$script:appProcess.HasExited) {
        Stop-Process -Id $script:appProcess.Id -ErrorAction Stop
        $script:appProcess.WaitForExit()
    }
    $script:appProcess = $null
}

function Assert-That([bool]$Condition, [string]$Message) {
    if (!$Condition) { throw $Message }
}

function Assert-HttpStatus([string]$Method, [string]$Path, [string]$Body, [int]$Expected) {
    $request = @{ Uri = "$baseUrl$Path"; Method = $Method; UseBasicParsing = $true; TimeoutSec = 10 }
    if ($Body) { $request.Body = $Body; $request.ContentType = "application/json" }
    $actual = $null
    try { $actual = [int](Invoke-WebRequest @request).StatusCode }
    catch {
        if (!$_.Exception.Response) { throw }
        $actual = [int]$_.Exception.Response.StatusCode
    }
    Assert-That ($actual -eq $Expected) "$Method $Path returned $actual; expected $Expected"
}

try {
    Start-TestApp
    $title = "stage1-$runId"
    $body = @{ title = $title; content = "restart acceptance" } | ConvertTo-Json -Compress
    $created = Invoke-RestMethod "$baseUrl/api/todos" -Method Post -ContentType "application/json" -Body $body
    $todoId = $created.data.id
    Assert-That ($created.code -eq 0 -and $todoId -gt 0) "Create failed"
    Assert-That (!$created.data.done -and $created.data.createdAt -and $created.data.updatedAt) "Missing defaults or timestamps"
    Write-Output "PASS: create Todo with defaults and timestamps"

    Stop-TestApp
    Start-TestApp
    $loaded = Invoke-RestMethod "$baseUrl/api/todos/$todoId"
    Assert-That ($loaded.data.title -eq $title -and $loaded.data.content -eq "restart acceptance") "Todo did not survive application restart"
    $list = Invoke-RestMethod "$baseUrl/api/todos"
    Assert-That (@($list.data.id) -contains $todoId) "List does not contain persisted Todo"
    Write-Output "PASS: a new application process reads the committed Todo from MySQL"

    $body = @{ title = "$title-updated"; content = "updated"; done = $true } | ConvertTo-Json -Compress
    $updated = Invoke-RestMethod "$baseUrl/api/todos/$todoId" -Method Put -ContentType "application/json" -Body $body
    Assert-That ($updated.data.done -and $updated.data.content -eq "updated" -and $updated.data.title -eq "$title-updated") "Update failed"
    Assert-HttpStatus "PUT" "/api/todos/$todoId" '{"title":"   "}' 400
    Assert-HttpStatus "POST" "/api/todos" '{"title":""}' 400
    Assert-HttpStatus "GET" "/api/todos/abc" "" 400
    Write-Output "PASS: update fields and reject invalid input over HTTP"

    $null = Invoke-RestMethod "$baseUrl/api/todos/$todoId" -Method Delete
    Assert-HttpStatus "GET" "/api/todos/$todoId" "" 404
    Assert-HttpStatus "PUT" "/api/todos/$todoId" '{"done":false}' 404
    Assert-HttpStatus "DELETE" "/api/todos/$todoId" "" 404
    $todoId = $null
    Write-Output "PASS: delete and return 404 for missing Todo"
    Write-Output "Stage 1 HTTP and restart acceptance passed."
} finally {
    try {
        if ($null -ne $todoId) {
            if (!$script:appProcess -or $script:appProcess.HasExited) { Start-TestApp }
            $null = Invoke-RestMethod "$baseUrl/api/todos/$todoId" -Method Delete
        }
    } finally { Stop-TestApp }
}
