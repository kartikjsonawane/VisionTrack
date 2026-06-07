$ErrorActionPreference = "Stop"
$GITHUB_USERNAME = "kartikjsonawane"
$REPO_NAME = "VisionTrack"

try {
    Set-Location "$PSScriptRoot"
    Write-Host "Working in: $(Get-Location)"

    if (Test-Path ".git\config.lock") {
        Remove-Item ".git\config.lock" -Force
        Write-Host "Removed stale lock"
    }

    git config user.name "Kush"
    git config user.email "kartikjaywantsonawane@gmail.com"
    Write-Host "Git identity set"

    $gitignore = "# Gradle`n.gradle/`nbuild/`n**/build/`n*.apk`n*.aab`nlocal.properties`n`n# IDE`n.idea/`n*.iml`n.DS_Store`n`n# Secrets`ngoogle-services.json`napp/src/main/assets/yolov8n.tflite`n`n# OS`nThumbs.db"
    [System.IO.File]::WriteAllText("$PSScriptRoot\.gitignore", $gitignore)
    Write-Host "Created .gitignore"

    git add -A
    Write-Host "Staged all files"

    git commit -m "feat: initial commit - VisionTrack YOLOv8 Android app"
    Write-Host "Committed"

    git branch -M main

    $remote = "https://github.com/$GITHUB_USERNAME/$REPO_NAME.git"
    git remote remove origin 2>$null
    git remote add origin $remote
    Write-Host "Remote: $remote"

    Write-Host "Pushing... (enter GitHub username + Personal Access Token when prompted)"
    git push -u origin main

    Write-Host ""
    Write-Host "SUCCESS: https://github.com/$GITHUB_USERNAME/$REPO_NAME"

} catch {
    Write-Host "ERROR: $_" -ForegroundColor Red
}

Read-Host "Press Enter to close"
