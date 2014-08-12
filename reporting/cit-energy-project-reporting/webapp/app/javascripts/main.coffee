$ ->
    report = $.ajax
        dataType: "json",
        url: '/api/v1/user-report.json',
        error: (jqXHR, textStatus, errorThrown) ->
            console.log "Ups", jqXHR, textStatus, errorThrown, errorThrown
        success: (report) ->
            console.log "[#{new Date()}] Got report:", report
            header = "#{new Date(report.from * 1000)} - #{new Date(report.to * 1000)} (Auflösung #{report.resolution / 60}min)"
            $("#date-container").text(header)

            # ### storage ###
            storageDataByUser = {}
            for frame in report.timeFrames
                for user, value of frame.storage
                    storageDataByUser[user] ||= []
                    storageDataByUser[user].push { x: new Date(frame.startTime*1000), y: value }
            storageData = []
            for user, data of storageDataByUser
                storageData.push { key: user, values: data }

            storageChart = nv.addGraph () ->
                chart = nv.models.multiBarChart()
                chart.xAxis.tickFormat(d3.time.format('%d.%m.%Y %HUhr'))
                chart.yAxis.tickFormat(d3.format(',.1f'))
                d3.select('#storage-chart svg')
                    .datum(storageData)
                    .transition().duration(500)
                    .call(chart)
                nv.utils.windowResize(chart.update)
                chart
            $(".loading-banner").hide()
    console.log "[#{new Date()}] Loading report"

