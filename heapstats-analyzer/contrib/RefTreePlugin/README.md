Reference Tree Plugin for HeapStatsFXAnalyzer
===================
Reference Tree Plugin は [HeapStats 1.1](http://icedtea.classpath.org/wiki/HeapStats/jp)
で取得したオブジェクト参照関係を可視化するための [HeapStatsFXAnalyzer](https://github.com/YaSuenag/HeapStatsFXAnalyzer)
向けプラグインです。このプラグインを HeapStatsFXAnalyzer に組み込むことで、HeapStats
1.1 付属のアナライザ相当の機能を使用することができます。

Reference Tree Plugin is plugin for [HeapStatsFXAnalyzer](https://github.com/YaSuenag/HeapStatsFXAnalyzer)
to show reference graph which is got by [HeapStats 1.1](http://icedtea.classpath.org/wiki/HeapStats) .
This plugin provides a feature of HeapStats Analyzer 1.1 .

Reference Tree Plugin と HeapStatsFXAnalyzer は HeapStats のアナライザを
Java8 + JavaFX8 で作り直すプロジェクトです。
まだアルファ版であり、HeapStats Analyzer の全機能を実装しているものではありません。

Reference Tree Plugin HeapStatsFXAnalyzer is the project of rebuilding HeapStats
Analyzer with Java8 and JavaFX8.
HeapStatsFXAnalyzer is alpha version. Thus this software does NOT implement all
functions of HeapStats Analyzer.

## Pre-Requirements ##
 * OracleJDK 8 (or OpenJDK8)
 * JavaFX 8 (or OpenJFX8)
 * [JGraphX](http://www.jgraph.com/jgraphdownload.html)
 * Optional (developed by follow softwares)
  * NetBeans 8.0
  * Scene Builder 2.0

## How to Use ##

 1. jgraphx.jar と RefTreePlugin.jar を `</path/to/HeapStatsFXAnalyzer`>/lib にコピーします。  
    Copy jgraphx.jar and RefTreePlugin.jar to `</path/to/HeapStatsFXAnalyzer`>/lib .

 2. jp.co.ntt.oss.heapstats.plugin.reftree を heapstats.properties の plugins に追記します。（デリミタはセミコロンです）  
    Add jp.co.ntt.oss.heapstats.plugin.reftree to "plugins" entry in heapstats.properties. (Delimiter of each plugin is semi-colon.)

## License ##

 GNU General Public License, version 2  

