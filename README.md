HistogramLogAnalyzer
----------------------------------------------

HistogramLogAnalyzer is a UI tool that plots log files in histogram log format produced by jHiccup, cassandra-stress or other tools that support this format.

How to build
----------------------------------------------

Use Maven to build it from source code

    % mvn clean package

How to run
----------------------------------------------

Run it from the command line (java -jar HistogramLogAnalyzer.jar) or just by double-clicking on jar file.

Select log file to plot (via “File->Open” menu, command line option “-f file” or just drag and drop log file in the main window).

Timeline and percentile charts
----------------------------------------------

The tool produces two charts arranged vertically one above the other:

 - Timeline chart
 - Percentile chart

The timeline chart plots the maximum latency duration observed in each time interval. There is a spike for every recorded sample. The height of each spike indicates the maximum pause time experienced during that interval.

![timeline example plot]

The percentile chart plots the observed latency durations at varying percentiles.

![percentile example plot]

The tool allows to view SLA (service level agreement) data in the percentile chart. Use “Master SLA” button in toolbar to open new tab and configure SLA values (arbitrary number of percentile/latency pairs). Use “Show SLA” button in toolbar to display SLA in the percentile chart.

Customize charts
----------------------------------------------

The tool provides several options to customize the charts:

 - Chart properties

Right clicking on chart displays a popup menu. Clicking on “Properties…” menu item opens Editor pane that allows to customize different chart settings (titles, fonts, colors, etc).

 - Zooming functionality

To zoom in - left click inside a chart and drag over an area in the chart to zoom in<br />
To zoom out - left click inside a chart and drag outside the chart<br />
When zooming in on a range in the timeline chart, the percentile chart changes to represent only the percentiles in that time range.<br />

Snapshot
----------------------------------------------

The tool also provides option to make a snapshot image of charts via “Snapshot” button in toolbar.

Plotting tags
----------------------------------------------

The tool supports log files with multiple tags. It plots multiple tags (with different colors) on the same chart. Clicking on a tag item in chart legend toggles visibility of this tag in the chart.

[timeline example plot]:https://raw.github.com/HdrHistogram/HistogramLogAnalyzer/master/examples/screenshots/exampleTimelinePlot.png "Example timeline plot"
[percentile example plot]:https://raw.github.com/HdrHistogram/HistogramLogAnalyzer/master/examples/screenshots/examplePercentilePlot.png "Example timeline plot"
