HeapStatsFXAnalyzer
===================
HeapStatsFXAnalyzer は [HeapStats](http://icedtea.classpath.org/wiki/HeapStats/jp)
のアナライザを Java8 + JavaFX8 で作り直すプロジェクトです。
まだアルファ版であり、HeapStats Analyzer の全機能を実装しているものではありません。

HeapStatsFXAnalyzer aims to rebuild [HeapStats](http://icedtea.classpath.org/wiki/HeapStats)
Analyzer with Java8 and JavaFX8. Now, this project is released as an alpha version, so have
NOT yet implemented all existing functions of HeapStats Analyzer.


## Pre-Requirements ##

* OracleJDK 8 (or OpenJDK8)
* JavaFX 8 (or OpenJFX8)
* Optional (developed by follow softwares)
  * NetBeans 8.0+
  * Scene Builder 2.0+

If you do not have OracleJDK8 or OpenJFX, you cannot build heapstats-analyzer.

## Modules ##
HeapStatsFXAnalyzerは2つのモジュールで構成されています。
HeapStatsFXAnalyzer is composed two modules.

* heapstats-mbean: MBean module
* heapstats-core: パーサー等のコア機能ライブラリ Core library of HeapStats
  * contrib: 追加ライブラリ Additional Libraries
    * RefTreePlugin: 参照ツリー表示プラグイン Reference Tree Plugin
* heapstats-analyzer: HeapStatsFXAnalyzer本体 Main program of HeapStatsFXAnalyzer
* heapstats-cli: HeapStatsコマンドラインインターフェース Commandline interface of HeapStats

## JDP ##
* JDP のオートディスカバリ機能は、以下の起動引数が付与されて起動している JVM に対して行われます。
```
-Dcom.sun.management.jmxremote.port=<JMXポート番号>
-Dcom.sun.management.jmxremote.authenticate=<true|false>
-Dcom.sun.management.jmxremote.ssl=<true|false>
-Dcom.sun.management.jmxremote.autodiscovery=true
```
* JDP パケットは一定周期（デフォルト5秒）で送信されます。この周期を超えても後続のパケットが受信できなかった場合、当該インスタンス名がオレンジ色に変わります。

* クラッシュのリアルタイム検知はOracleJDKのみで動作します。また、Java が 以下のオプションが付与されて起動している必要があります。  
ErrorReportServerのポート番号のデフォルトは4711です。
```
-XX:+TransmitErrorReport -XX:ErrorReportServer=<アドレス>:<ポート>
```
## License ##

 GNU General Public License, version 2  

