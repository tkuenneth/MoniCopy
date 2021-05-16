$java_home = "C:\Program Files\Java\jdk-14.0.2"
$base_dir = "C:\Users\tkuen\Entwicklung\GitHub\MoniCopy"

$version = "???"
$source = Get-Content -Path $base_dir\src\com\thomaskuenneth\monicopy\Main.java
foreach($line in $source) {
    if ($line -match "VERSION = `"(.+)`"") {
        $version = $matches[1]
        break
    }
}

Set-Location $base_dir

Write-Output "java_home: $java_home"
Write-Output "base_dir: $base_dir"
Write-Output "version: $version"

$command = "$java_home\bin\jpackage.exe"
$arguments = "--win-menu --win-menu-group `"Thomas Kuenneth`" --vendor `"Thomas Kuenneth`" --name MoniCopy --icon $base_dir\artwork\MoniCopy.ico --type msi --app-version $version --module-path $base_dir\dist\MoniCopy.jar;`"C:\Program Files\Java\javafx-jmods-13.0.2`" -m main/com.thomaskuenneth.monicopy.Main"

Write-Output $arguments

Start-Process -RedirectStandardOutput stdout.txt -RedirectStandardError stderr.txt -FilePath $command -ArgumentList $arguments -Wait