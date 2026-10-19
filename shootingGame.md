[JDK]
https://adoptium.net/ja

[shootingGame db]
copy command from AWS GUI about RDS console

[行カウント方法]
press "ctrl + shift + p" and type "VsCodeCounter: Count lines in workspace"

[PlantUML使用方法]
~~ install "https://www.graphviz.org/download/" to use ~~
install extention "https://marketplace.visualstudio.com/items?itemName=jebbs.plantuml" to vsr
press "alt + d" to view map
press "ctrl + shift + p", type "PlantUML: Export Current Diagram" and sellect "svg" to export

[DrawIO閲覧方法]
install extention "https://marketplace.visualstudio.com/items?itemName=hediet.vscode-drawio" to vsc

[起動方法]
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd clean spring-boot:run
