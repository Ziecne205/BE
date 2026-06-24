@REM ----------------------------------------------------------------------------
@REM Maven Wrapper startup batch script
@REM ----------------------------------------------------------------------------
@IF "%__MVNW_ARG0_NAME__%"=="" SET __MVNW_ARG0_NAME__=%~nx0
@SET __MVNW_CMD__=
@SET __MVNW_ERROR__=
@SET __MVNW_PSMODULEP_SAVE=%PSModulePath%
@SET PSModulePath=
@FOR /F "usebackq tokens=1* delims==" %%A IN (`powershell -noprofile "& {$scriptDir='%~dp0'; $wrapperJar=$scriptDir+'.mvn\wrapper\maven-wrapper.jar'; $wrapperUrl='https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar'; $wrapperProperties=$scriptDir+'.mvn\wrapper\maven-wrapper.properties'; if (Test-Path $wrapperProperties) { $props = Get-Content $wrapperProperties; foreach ($line in $props) { if ($line -match '^distributionUrl=(.*)') { $distributionUrl = $matches[1]; break } } }; if (-not (Test-Path $wrapperJar)) { Write-Output 'MVNW_VERBOSE=Downloading Maven wrapper jar...'; (New-Object Net.WebClient).DownloadFile($wrapperUrl, $wrapperJar) }; Write-Output ('MVNW_JAVAEXE=' + (Get-Command java).Source)}"`) DO @SET "%%A=%%B"
@SET PSModulePath=%__MVNW_PSMODULEP_SAVE%
@SET WRAPPER_JAR="%~dp0.mvn\wrapper\maven-wrapper.jar"
@SET WRAPPER_PROPERTIES="%~dp0.mvn\wrapper\maven-wrapper.properties"

@IF NOT EXIST %WRAPPER_JAR% (
    powershell -Command "(New-Object Net.WebClient).DownloadFile('https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar', '%~dp0.mvn\wrapper\maven-wrapper.jar')"
)

"%MVNW_JAVAEXE%" %JVM_CONFIG_MAVEN_PROPS% %MAVEN_OPTS% -jar %WRAPPER_JAR% -s %MAVEN_USER_SETTINGS% %MAVEN_CONFIG% %*
IF ERRORLEVEL 1 GOTO error
GOTO end

:error
SET ERROR_CODE=1

:end
@ENDLOCAL & SET ERROR_CODE=%ERROR_CODE%
EXIT /B %ERROR_CODE%
