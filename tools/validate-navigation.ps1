param([int]$MutexTimeoutMinutes = 15, [switch]$FullPlayback, [switch]$RepresentativeGui, [switch]$ValidationCoverage, [string]$ValidationLessons = '', [string]$ValidationLocales = '', [string]$ValidationDescriptions = '', [string]$ValidationCaptions = '')

$ErrorActionPreference = 'Stop'
if ($ValidationLessons -and !$FullPlayback) { throw 'ValidationLessons requires FullPlayback.' }
if ($RepresentativeGui -and $FullPlayback) { throw 'RepresentativeGui cannot be combined with FullPlayback.' }
if ($ValidationLocales -and $RepresentativeGui) { throw 'ValidationLocales uses the locale font loop, not RepresentativeGui.' }
if ($ValidationCoverage -and ($FullPlayback -or $RepresentativeGui -or $ValidationLessons -or $ValidationLocales)) {
    throw 'ValidationCoverage runs its own item and locale checks; do not combine it with other validation modes.'
}
if ($PSBoundParameters.ContainsKey('ValidationDescriptions') -and !$ValidationCoverage) {
    throw 'ValidationDescriptions requires ValidationCoverage.'
}
if ($PSBoundParameters.ContainsKey('ValidationCaptions') -and !$ValidationCoverage) {
    throw 'ValidationCaptions requires ValidationCoverage.'
}
if ($ValidationCoverage) {
    $hasDescriptionFilter = $PSBoundParameters.ContainsKey('ValidationDescriptions')
    $hasCaptionFilter = $PSBoundParameters.ContainsKey('ValidationCaptions')
    if ($hasDescriptionFilter -ne $hasCaptionFilter) {
        throw 'ValidationDescriptions and ValidationCaptions must be supplied together.'
    }
    if ($hasDescriptionFilter) {
        $expectedDescriptionIds = @('coverage_ore_blocks', 'coverage_lasers', 'coverage_fissile_materials')
        $requestedDescriptionIds = @($ValidationDescriptions -split ',' | ForEach-Object { $_.Trim() })
        $missingDescriptionIds = @($expectedDescriptionIds | Where-Object { $_ -cnotin $requestedDescriptionIds })
        $unexpectedDescriptionIds = @($requestedDescriptionIds | Where-Object { $_ -cnotin $expectedDescriptionIds })
        if ($requestedDescriptionIds.Count -ne $expectedDescriptionIds.Count -or
            @($requestedDescriptionIds | Sort-Object -Unique).Count -ne $expectedDescriptionIds.Count -or
            $missingDescriptionIds.Count -or $unexpectedDescriptionIds.Count) {
            throw 'ValidationCoverage requires exactly -ValidationDescriptions coverage_ore_blocks,coverage_lasers,coverage_fissile_materials.'
        }
        $expectedCaptionIds = @('power_2', 'diagnose_processing_3')
        $requestedCaptionIds = @($ValidationCaptions -split ',' | ForEach-Object { $_.Trim() })
        $missingCaptionIds = @($expectedCaptionIds | Where-Object { $_ -cnotin $requestedCaptionIds })
        $unexpectedCaptionIds = @($requestedCaptionIds | Where-Object { $_ -cnotin $expectedCaptionIds })
        if ($requestedCaptionIds.Count -ne $expectedCaptionIds.Count -or
            @($requestedCaptionIds | Sort-Object -Unique).Count -ne $expectedCaptionIds.Count -or
            $missingCaptionIds.Count -or $unexpectedCaptionIds.Count) {
            throw 'ValidationCoverage requires exactly -ValidationCaptions power_2,diagnose_processing_3.'
        }
    }
}
$projectRoot = Split-Path -Parent $PSScriptRoot
$mutex = [System.Threading.Mutex]::new($false, 'Local\PonderSeriesValidation')
$held = $false
try {
    Write-Output 'Waiting for the Ponder-series Minecraft validation mutex...'
    try { $held = $mutex.WaitOne([TimeSpan]::FromMinutes($MutexTimeoutMinutes)) }
    catch [System.Threading.AbandonedMutexException] { $held = $true }
    if (!$held) { throw 'Another Ponder-series validation still owns the mutex; no client was started.' }

    $matching = @(Get-CimInstance Win32_Process -Filter "Name = 'java.exe' OR Name = 'javaw.exe'" |
        Where-Object {
            $_.CommandLine -and $_.CommandLine.Contains($projectRoot) -and
            $_.CommandLine -match 'validationClientRun|net\.neoforged\.devlaunch\.Main'
        })
    if ($matching.Count) {
        throw ('An existing client for this project is running (PID ' + ($matching.ProcessId -join ', ') + '). No duplicate was started.')
    }
    $env:JAVA_HOME = Split-Path -Parent (Split-Path -Parent (Get-Command java -ErrorAction Stop).Source)
    Push-Location -LiteralPath $projectRoot
    try {
        $validationArguments = @('-PvalidateGuide')
        if ($ValidationCoverage) { $validationArguments += '-PvalidateCoverage' }
        elseif (!$FullPlayback) { $validationArguments += '-PvalidateNavigation' }
        if ($ValidationDescriptions) { $validationArguments += "-PvalidationDescriptions=$ValidationDescriptions" }
        if ($ValidationCaptions) { $validationArguments += "-PvalidationCaptions=$ValidationCaptions" }
        if ($RepresentativeGui) { $validationArguments += '-PvalidateRepresentativeGui' }
        if ($ValidationLessons) { $validationArguments += "-PvalidationLessons=$ValidationLessons" }
        if ($ValidationLocales) { $validationArguments += "-PvalidationLocales=$ValidationLocales" }
        $validationArguments += @('runValidationClient', '--console=plain')
        & .\gradlew.bat @validationArguments
        if ($LASTEXITCODE -ne 0) { throw "Navigation validation failed with exit code $LASTEXITCODE" }
    } finally { Pop-Location }
} finally {
    if ($held) { $mutex.ReleaseMutex() }
    $mutex.Dispose()
}
