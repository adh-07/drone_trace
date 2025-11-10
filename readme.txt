Start-Sleep -Seconds 3; Set-Location -Path C:\Users\moham\Downloads\java; $env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-25.0.0.36-hotspot"; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"; .\apache-maven-3.9.5\bin\mvn.cmd javafx:run


"c:\Users\moham\Downloads\java" ; $env:JAVA_HOME = (Get-Command java).Source -replace 'bin\\java.exe', '' ; .\apache-maven-3.9.5\bin\mvn.cmd javafx:run