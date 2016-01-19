HeapStatsFXAnalyzer
===================

***CAUTION***: HeapStatsFXAnalyzer has been ported as a part program of [heapstats](https://github.com/HeapStats/heapstats/). If you want reporting issues and/or pulling requests, please contribute to [heapstats](https://github.com/HeapStats/heapstats/). 

***注意***: HeapStatsFXAnalyzer は [heapstats](https://github.com/HeapStats/heapstats/) のプログラムの一部として移植されました。もしバグ報告やプルリクエストがありましたら、[heapstats](https://github.com/HeapStats/heapstats/) にお願い致します。

-----

日本語は英語の後に記載しています。Japanese following English.

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

HeapStatsFXAnalyzer is composed two modules.

* heapstats-mbean: MBean module
* heapstats-core: Core library of HeapStats
  * contrib: Additional Libraries
    * RefTreePlugin: Reference Tree Plugin
* heapstats-analyzer: Main program of HeapStatsFXAnalyzer
* heapstats-cli: Commandline interface of HeapStats

## JDP ##

* An automatic discovery function of JDP (Java Discovery Protocol) is enabled
by JVM which is run with the following options.

```
-Dcom.sun.management.jmxremote.port=<JMX port>
-Dcom.sun.management.jmxremote.authenticate=<true|false>
-Dcom.sun.management.jmxremote.ssl=<true|false>
-Dcom.sun.management.jmxremote.autodiscovery=true
```

* JDP sends a packet at uniform intervals: 5 seconds by default. If analyzer
can NOT recive a JDP packet from an instance, the color of the instance's name
will be change to orange.
* OracleJDK can provide a real-time detection of JVM crash, but OpenJDK does
NOT support it. In addition, OracleJDK needs to be run with the following options.

```
-XX:+TransmitErrorReport
-XX:ErrorReportServer=<address>:<port (4711 by default)>
```
## License ##

 GNU General Public License, version 2


HeapStatsFXAnalyzer (Japanese)
==============================

HeapStatsFXAnalyzer は [HeapStats](http://icedtea.classpath.org/wiki/HeapStats/jp)
のアナライザを Java8 + JavaFX8 で作り直すプロジェクトです。
まだベータ版であり、HeapStats Analyzer の全機能を実装しているものではありません。

## Pre-Requirements ##

* OracleJDK 8 (or OpenJDK8)
* JavaFX 8 (or OpenJFX8)
* オプション (以下の環境で開発されています)
  * NetBeans 8.0+
  * Scene Builder 2.0+

OracleJDK8 か OpenJFX がない場合、HeapStatsFXAnalyzer はビルドできません。

## Modules ##

HeapStatsFXAnalyzerは2つのモジュールで構成されています。

* heapstats-mbean: MBean module
* heapstats-core: パーサー等のコア機能ライブラリ
  * contrib: 追加ライブラリ
    * RefTreePlugin: 参照ツリー表示プラグイン
* heapstats-analyzer: HeapStatsFXAnalyzer本体
* heapstats-cli: HeapStatsコマンドラインインターフェース

## JDP ##

* JDP (Java Discovery Protocol) のオートディスカバリ機能は、以下の起動引数が付与されて起動している JVM に対して行われます。

```
-Dcom.sun.management.jmxremote.port=<JMX port>
-Dcom.sun.management.jmxremote.authenticate=<true|false>
-Dcom.sun.management.jmxremote.ssl=<true|false>
-Dcom.sun.management.jmxremote.autodiscovery=true
```

* JDP パケットは一定周期（デフォルト5秒）で送信されます。この周期を超えても後続のパケットが受信できなかった場合、当該インスタンス名がオレンジ色に変わります。
* クラッシュのリアルタイム検知はOracleJDKのみで動作します。また、Java が 以下のオプションが付与されて起動している必要があります。

```
-XX:+TransmitErrorReport
-XX:ErrorReportServer=<address>:<port (デフォルト4711)>
```
## License ##

 GNU General Public License, version 2

