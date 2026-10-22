[JDK]
https://adoptium.net/ja

[data]
H2 のファイル(実行フォルダの data/shooting_game_db.mv.db)。バックアップはこのファイルをコピーする

[行カウント方法]
press "ctrl + shift + p" and type "VsCodeCounter: Count lines in workspace"

[PlantUML使用方法]
~~ install "https://www.graphviz.org/download/" to use ~~
install extention "https://marketplace.visualstudio.com/items?itemName=jebbs.plantuml" to vsr
press "alt + d" to view map
press "ctrl + shift + p", type "PlantUML: Export Current Diagram" and sellect "svg" to export

[起動方法]
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd clean spring-boot:run

[単体テスト(DB抜き)]
.\mvnw.cmd test "-Djava.awt.headless=true" "-Dtest=GameUtilsTest,BeamBlockTest,DexServiceTest,UserDataTest,DifficultyServiceTest,ChatBoxContextTest,ShipFactoryTest,DamageFormulaTest,StageLockIconTest,MissionStarsTest,StarRewardServiceTest"

[全体テスト]
.\mvnw.cmd test "-Djava.awt.headless=true"
