$ ->
    shortDateFormatter = d3.time.format('%d.%m.%Y %X')
    window.formatter =
      power: (d) -> d3.format(',.3f')(d) + 'KWh'
      data: (d) -> d3.format(',.2s')(d) + 'Byte'
    window.charts = [
      { id: 'powerUsageByHost', title: 'Power usage by host', format: formatter.power },
      { id: 'powerUsageByUser', title: 'Power usage by user', format: formatter.power },
      { id: 'powerUsageByType', title: 'Power usage by type', format: formatter.power },
      { id: 'storageUsageByUser', title: 'Average storage usage by user', format: formatter.data, pieAvg: true },
      { id: 'trafficUsageByUser', title: 'Traffic usage by user', format: formatter.data }
    ]


    extractDataFromTimeFrame = (report, field) ->
        dataByKey = {}
        x = []
        for frame in report.timeFrames
            x.push ((frame.endTime + frame.startTime + 1) / 2) * 1000
            for key, value of frame[field]
                dataByKey[key] ||= []
                dataByKey[key].push value
        [
          ['x'].concat x
        ].concat(
          [key].concat values for key, values of dataByKey
        )

    pieAndLineChart = (data, htmlId) ->

    pieAndLineChart = (data, htmlId) ->
          pieChart = nv.models.pieChart()
              .x((d) -> d.key).y((d) -> d3.sum(d.values.map((v) -> v.y)))
              .valueFormat((d) -> d3.format('.3f')(d) + 'KWh')
          d3.select("#" + htmlId + "-pie-chart").datum(data).call(pieChart)
          nv.utils.windowResize(pieChart.update)

          lineChart = nv.models.multiBarChart().stacked(true)
               .tooltipContent (key, x, y, e, graph) ->
                   "<h3>#{key}</h3><p>#{y} at #{d3.time.format('%d.%m.%Y %H:%M')(e.point.startTime)}-#{d3.time.format('%H:%M')(e.point.endTime)}</p>"
          lineChart.yAxis.tickFormat (d) -> d3.format(',.3f')(d) + 'KWh'
          lineChart.xAxis.tickFormat (d,e,f) ->
              d3.time.format('%H:%M')(new Date(d*1000))
          d3.select("#" + htmlId + "-line-chart").datum(data).transition().duration(500).call(lineChart)
          nv.utils.windowResize(lineChart.update)

    report = $.ajax
        dataType: "json",
        url: "/api/v1/user-report.json#{location.search}",
        beforeSend: -> console.debug "[#{new Date()}] Loading report."
        error: (jqXHR, textStatus, errorThrown) -> console.log "Ups", jqXHR, textStatus, errorThrown, errorThrown
        success: (report) ->
            console.debug "[#{new Date()}] Got report!"
            window.report = report # debug

            # ### nav/header ###
            reportStartTime = shortDateFormatter(new Date(report.startTime * 1000))
            reportEndTime = shortDateFormatter(new Date(report.endTime * 1000))
            $('#date-container').text("#{reportStartTime} - #{reportEndTime} (Resolution #{report.intervalSize / 60}min)")
            $("#nav-mode-#{report.mode}").addClass('active')

            # ### charts ###
            for chart in charts
              data = extractDataFromTimeFrame(report, chart.id)

              $("#charts").append $("""
                <h2>#{chart.title}</h2>
                <div class="row">
                  <div class="col-xs-4"><div id="#{chart.id}-pie-chart"/></div>
                  <div class="col-xs-8"><div id="#{chart.id}-line-chart"/></div>
                </div>
              """)

              if chart.pieAvg
                pieData = for col in data
                  [col[0], col.slice(1).reduce((sum, current) -> sum + current) / (col.length - 1)]
              else
                pieData = data

              pieChart = c3.generate
                bindto: "#" + chart.id + "-pie-chart"
                data:
                  type: 'pie'
                  x: 'x'
                  columns: pieData
                tooltip:
                  format:
                    value: chart.format

              lineChart = c3.generate
                bindto: "#" + chart.id + "-line-chart"
                data:
                  type: 'area'
                  x: 'x'
                  columns: data
                  groups: [d[0] for d in data]
                axis:
                  x:
                    type: 'timeseries'
                  y:
                    tick: format: chart.format
                tooltip:
                  format:
                    title: d3.time.format('%d.%m.%Y %H:00-%H:59')

            # ### hide loading banner ###
            $(".loading-banner").hide()

