param(
    [ValidateRange(1024, 65535)]
    [int]$Port = 8080
)

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$runtime = Join-Path $root 'build/runtime'
$tomcatVersion = '9.0.124'
$h2Version = '2.4.240'
$tomcat = Join-Path $runtime "apache-tomcat-$tomcatVersion"
$base = Join-Path $runtime 'instance'
$app = Join-Path $runtime 'app'
$h2 = Join-Path $runtime "h2-$h2Version.jar"

function Invoke-Checked {
    param([string]$Program, [string[]]$Arguments)
    & $Program @Arguments
    if ($LASTEXITCODE -ne 0) { throw "$Program exited with code $LASTEXITCODE" }
}

function Get-Dependency {
    param([string]$Url, [string]$File)
    if (Test-Path -LiteralPath $File) { return }
    Invoke-Checked 'curl.exe' @('-fsSL', '--retry', '2', $Url, '-o', "$File.download")
    Move-Item -LiteralPath "$File.download" -Destination $File
}

$java = (Get-Command java.exe -ErrorAction Stop).Source
$javac = (Get-Command javac.exe -ErrorAction Stop).Source
$listener = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, $Port)
try { $listener.Start() }
catch { throw "Local port $Port is already in use. Start with -Port 8081 or stop the existing instance." }
finally { $listener.Stop() }

New-Item -ItemType Directory -Force $runtime, $base | Out-Null
$maven = 'https://repo.maven.apache.org/maven2'
$archive = Join-Path $runtime "tomcat-$tomcatVersion.zip"
Get-Dependency "$maven/org/apache/tomcat/tomcat/$tomcatVersion/tomcat-$tomcatVersion.zip" $archive
Get-Dependency "$maven/com/h2database/h2/$h2Version/h2-$h2Version.jar" $h2
if (!(Test-Path -LiteralPath (Join-Path $tomcat 'lib/servlet-api.jar'))) {
    Expand-Archive -LiteralPath $archive -DestinationPath $runtime -Force
}

Copy-Item -Path (Join-Path $tomcat 'conf') -Destination $base -Recurse -Force
$paths = @('lib', 'logs', 'temp', 'webapps', 'work', 'conf/Catalina/localhost')
foreach ($path in $paths) {
    New-Item -ItemType Directory -Force (Join-Path $base $path) | Out-Null
}
Copy-Item -LiteralPath $h2 -Destination (Join-Path $base 'lib') -Force

New-Item -ItemType Directory -Force $app | Out-Null
Copy-Item -Path (Join-Path $root 'WebContent/*') -Destination $app -Recurse -Force
$classes = Join-Path $app 'WEB-INF/classes'
New-Item -ItemType Directory -Force $classes | Out-Null
$sources = Get-ChildItem -LiteralPath (Join-Path $root 'src/studentCurriculum') -Filter '*.java' |
    ForEach-Object FullName
$servlet = Join-Path $tomcat 'lib/servlet-api.jar'
Invoke-Checked $javac (@('-encoding', 'UTF-8', '-classpath', $servlet, '-d', $classes) + $sources)

function Invoke-DemoSql {
    param([string]$JdbcUrl, [string]$File)
    Invoke-Checked $java @('-cp', $h2, 'org.h2.tools.RunScript', '-url', $JdbcUrl,
        '-user', 'sa', '-script', (Join-Path $root "demo/$File"), '-options', 'CHARSET', "'UTF-8'")
}

$db = (Join-Path $runtime 'curriculum').Replace('\', '/')
$jdbc = "jdbc:h2:file:${db};MODE=MSSQLServer"
if (!(Test-Path -LiteralPath "$db.mv.db")) {
    $seed = (Join-Path $runtime ("initializing-" + [guid]::NewGuid())).Replace('\', '/')
    try {
        Invoke-DemoSql "jdbc:h2:file:${seed};MODE=MSSQLServer" 'schema.sql'
        Invoke-DemoSql "jdbc:h2:file:${seed};MODE=MSSQLServer" 'scheduling.sql'
        Move-Item -LiteralPath "$seed.mv.db" -Destination "$db.mv.db"
    } finally {
        foreach ($suffix in @('.mv.db', '.trace.db')) {
            if (Test-Path -LiteralPath "$seed$suffix") { Remove-Item -LiteralPath "$seed$suffix" }
        }
    }
} else {
    Invoke-DemoSql $jdbc 'scheduling.sql'
}

$setup = Join-Path $base 'conf/Catalina/localhost/ROOT.xml'
$appXml = [Security.SecurityElement]::Escape($app)
$jdbcXml = [Security.SecurityElement]::Escape($jdbc)
@"
<Context docBase="$appXml">
  <Resource name="jdbc/SCS" type="javax.sql.DataSource" driverClassName="org.h2.Driver"
            url="$jdbcXml" username="sa" password="" maxTotal="20" maxWaitMillis="5000"/>
</Context>
"@ | Set-Content -LiteralPath $setup -Encoding UTF8

@"
<Server port="-1">
  <Service name="Catalina">
    <Connector address="127.0.0.1" port="$Port" protocol="HTTP/1.1" URIEncoding="UTF-8" />
    <Engine name="Catalina" defaultHost="localhost">
      <Host name="localhost" appBase="webapps" autoDeploy="false" deployOnStartup="true" />
    </Engine>
  </Service>
</Server>
"@ | Set-Content -LiteralPath (Join-Path $base 'conf/server.xml') -Encoding UTF8

$env:SCS_DEMO = 'true'
$bootstrap = Join-Path $tomcat 'bin/bootstrap.jar'
$logging = Join-Path $tomcat 'bin/tomcat-juli.jar'
Write-Host "Student Curriculum System: http://127.0.0.1:$Port/ (Ctrl+C to stop)"
Invoke-Checked $java @("-Dcatalina.home=$tomcat", "-Dcatalina.base=$base",
    "-Djava.io.tmpdir=$(Join-Path $base 'temp')", '-cp', "$bootstrap;$logging",
    'org.apache.catalina.startup.Bootstrap', 'start')
