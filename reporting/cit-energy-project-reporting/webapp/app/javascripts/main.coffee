$ ->
    shortDateFormatter = d3.time.format('%d.%m.%Y %X')

    extractDataFromTimeFrame = (report, field) ->
        dataByKey = {}
        for frame in report.timeFrames
            for key, value of frame[field]
                dataByKey[key] ||= []
                dataByKey[key].push x: (frame.endTime + frame.startTime + 1) / 2, y: value, startTime: new Date(frame.startTime * 1000), endTime: new Date(frame.endTime * 1000)
        for key, data of dataByKey
            { key: key, values: data }

    report = $.ajax
        dataType: "json",
        url: "http://#{location.hostname}:50201/api/v1/user-report.json#{location.search}",
        beforeSend: -> console.debug "[#{new Date()}] Loading report."
        error: (jqXHR, textStatus, errorThrown) -> console.log "Ups", jqXHR, textStatus, errorThrown, errorThrown
        success: (report) ->
            console.debug "[#{new Date()}] Got report!"
            window.report = report # debug

            # ### nav/header ###
            reportStartTime = shortDateFormatter(new Date(report.startTime * 1000))
            reportEndTime = shortDateFormatter(new Date(report.endTime * 1000))
            $('#date-container').text("#{reportStartTime} - #{reportEndTime} (Auflösung #{report.intervalSize / 60}min)")
            $("#nav-mode-#{report.mode}").addClass('active')

            # ### power ###
            powerUsage = extractDataFromTimeFrame(report, "powerUsageSum")

            powerUsagePieChart = nv.models.pieChart()
                .x((d) -> d.key).y((d) -> d3.sum(d.values.map((v) -> v.y)))
                .valueFormat((d) -> d3.format('.3f')(d) + 'KWh')
            d3.select("#power-pie-chart").datum(powerUsage).call(powerUsagePieChart)
            nv.utils.windowResize(powerUsagePieChart.update)

            powerUsageLineChart = nv.models.multiBarChart().stacked(true)
                 .tooltipContent (key, x, y, e, graph) ->
                     "<h3>#{key}</h3><p>#{y} at #{d3.time.format('%d.%m.%Y %H:%M')(e.point.startTime)}-#{d3.time.format('%H:%M')(e.point.endTime)}</p>"
            powerUsageLineChart.yAxis.tickFormat (d) -> d3.format(',.3f')(d) + 'KWh'
            powerUsageLineChart.xAxis.tickFormat (d,e,f) ->
                d3.time.format('%H:%M')(new Date(d*1000))
            d3.select("#power-line-chart").datum(powerUsage).transition().duration(500).call(powerUsageLineChart)
            nv.utils.windowResize(powerUsageLineChart.update)

            # ### storage ###

            # ### hide loading banner ###
            $(".loading-banner").hide()

