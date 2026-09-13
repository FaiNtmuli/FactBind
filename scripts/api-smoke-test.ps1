# Minimal end-to-end check of the running backend.
#
#   cd backend && ./mvnw spring-boot:run          # in one terminal
#   pwsh scripts/api-smoke-test.ps1               # in another terminal
#
# It calls every main API once and prints the HTTP status plus a short body preview.

param(
    [string]$BaseUrl = 'http://localhost:8080'
)

$script:failures = 0

function Invoke-Api {
    param(
        [string]$Method,
        [string]$Path,
        [object]$Body = $null,
        [int]$Expect
    )

    $uri = "$BaseUrl$Path"
    $status = -1
    $content = ''

    if ($PSVersionTable.PSVersion.Major -ge 7) {
        # PowerShell 7 can return non 2xx responses without throwing.
        $params = @{ Uri = $uri; Method = $Method; UseBasicParsing = $true }
        $params.SkipHttpErrorCheck = $true
        if ($null -ne $Body) {
            $params.ContentType = 'application/json'
            $params.Body = ($Body | ConvertTo-Json -Depth 6 -Compress)
        }
        $response = Invoke-WebRequest @params
        $status = [int]$response.StatusCode
        $content = $response.Content
    }
    else {
        # Windows PowerShell 5.1 throws on non 2xx responses.
        try {
            $params = @{ Uri = $uri; Method = $Method; UseBasicParsing = $true }
            if ($null -ne $Body) {
                $params.ContentType = 'application/json'
                $params.Body = ($Body | ConvertTo-Json -Depth 6 -Compress)
            }
            $response = Invoke-WebRequest @params
            $status = [int]$response.StatusCode
            $content = $response.Content
        }
        catch {
            $response = $_.Exception.Response
            if ($null -ne $response) {
                $status = [int]$response.StatusCode
                $reader = New-Object System.IO.StreamReader($response.GetResponseStream())
                $content = $reader.ReadToEnd()
            }
            else {
                $content = $_.Exception.Message
            }
        }
    }

    $ok = $status -eq $Expect
    if (-not $ok) { $script:failures++ }
    $preview = if ($content.Length -gt 110) { $content.Substring(0, 110) + '...' } else { $content }

    $mark = if ($ok) { 'PASS' } else { 'FAIL' }
    Write-Host ("[{0}] {1,-6} {2,-45} -> {3} (expected {4}) {5}" -f $mark, $Method, $Path, $status, $Expect, $preview)

    return $content
}

Write-Host "Running smoke test against $BaseUrl"

Invoke-Api GET  '/api/dashboard/summary'            $null 200 | Out-Null
Invoke-Api GET  '/api/dashboard/recent-orders?limit=3' $null 200 | Out-Null

Invoke-Api GET  '/api/users'                        $null 200 | Out-Null
Invoke-Api GET  '/api/users?keyword=alice&status=ACTIVE&page=0&size=5' $null 200 | Out-Null
Invoke-Api GET  '/api/users?status=NOT_A_STATUS'    $null 400 | Out-Null
Invoke-Api GET  '/api/users/1'                      $null 200 | Out-Null
Invoke-Api GET  '/api/users/999999'                 $null 404 | Out-Null
Invoke-Api GET  '/api/unknown-endpoint'             $null 404 | Out-Null

$stamp = Get-Date -Format 'HHmmssfff'
$newUser = Invoke-Api POST '/api/users' @{
    name   = "Smoke User $stamp"
    email  = "smoke.$stamp@example.com"
    age    = 30
    status = 'ACTIVE'
} 201
$userId = ($newUser | ConvertFrom-Json).id

Invoke-Api POST '/api/users' @{
    name = 'Bad Name'
    email = "smoke.$stamp@example.com"
    age = 30
} 409 | Out-Null

Invoke-Api POST '/api/users' @{ name = 'A'; email = 'nope'; age = 0 } 400 | Out-Null
Invoke-Api PUT  "/api/users/$userId" @{ name = "Smoke User $stamp v2"; email = "smoke.$stamp@example.com"; age = 31 } 200 | Out-Null
Invoke-Api PATCH "/api/users/$userId/status" @{ status = 'DISABLED' } 200 | Out-Null
Invoke-Api PATCH "/api/users/$userId/status" @{ status = 'ACTIVE' } 200 | Out-Null

Invoke-Api GET  '/api/products'                     $null 200 | Out-Null
Invoke-Api GET  '/api/products?status=ON_SALE&page=0&size=5' $null 200 | Out-Null

$product = Invoke-Api POST '/api/products' @{
    name   = "Smoke Product $stamp"
    sku    = "SKU-$stamp"
    price  = 42.50
    stock  = 7
    status = 'ON_SALE'
} 201
$productId = ($product | ConvertFrom-Json).id

Invoke-Api POST '/api/products' @{
    name = 'Duplicate'; sku = "SKU-$stamp"; price = 1; stock = 1
} 409 | Out-Null

Invoke-Api PATCH "/api/products/$productId/stock" @{ stock = 3 } 200 | Out-Null
Invoke-Api PUT   "/api/products/$productId" @{ name = "Smoke Product $stamp v2"; sku = "SKU-$stamp"; price = 45.00; stock = 3 } 200 | Out-Null

$order = Invoke-Api POST '/api/orders' @{
    userId = $userId
    remark = 'smoke test order'
    items  = @(@{ productId = $productId; quantity = 2 })
} 201
$orderId = ($order | ConvertFrom-Json).id

Invoke-Api GET   "/api/orders/$orderId"              $null 200 | Out-Null
Invoke-Api GET   "/api/orders?userId=$userId&status=CREATED" $null 200 | Out-Null
Invoke-Api PATCH "/api/orders/$orderId/status?notify=true" @{ status = 'PAID' } 200 | Out-Null
Invoke-Api PATCH "/api/orders/$orderId/status" @{ status = 'CREATED' } 409 | Out-Null
Invoke-Api PATCH "/api/orders/$orderId/status" @{ status = 'COMPLETED' } 200 | Out-Null
Invoke-Api POST  '/api/orders' @{ userId = $userId; items = @() } 400 | Out-Null
Invoke-Api POST  '/api/orders' @{ userId = $userId; items = @(@{ productId = $productId; quantity = 99 }) } 409 | Out-Null

Invoke-Api PATCH "/api/products/$productId/status" @{ status = 'OFF_SALE' } 200 | Out-Null
Invoke-Api DELETE "/api/orders/$orderId"            $null 204 | Out-Null
Invoke-Api DELETE "/api/products/$productId"        $null 204 | Out-Null
Invoke-Api DELETE "/api/users/$userId"              $null 204 | Out-Null

if ($script:failures -gt 0) {
    Write-Host "`n$($script:failures) check(s) FAILED" -ForegroundColor Red
    exit 1
}

Write-Host "`nAll checks passed" -ForegroundColor Green
