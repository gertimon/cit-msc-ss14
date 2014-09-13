(function(){$(function(){var a,b,c,d;return d=d3.time.format("%d.%m.%Y %X"),window.formatter={power:function(a){return d3.format(",.3f")(a)+"KWh"},data:function(a){return d3.format(",.2s")(a)+"Byte"}},window.charts=[{id:"powerUsageByHost",title:"Power usage by host",format:formatter.power},{id:"powerUsageByUser",title:"Power usage by user",format:formatter.power},{id:"powerUsageByType",title:"Power usage by type",format:formatter.power},{id:"storageUsageByUser",title:"Average storage usage by user",format:formatter.data,pieAvg:!0},{id:"trafficUsageByUser",title:"Traffic usage by user",format:formatter.data}],a=function(a,b){var c,d,e,f,g,h,i,j,k,l;for(c={},h=[],k=a.timeFrames,i=0,j=k.length;j>i;i++){d=k[i],h.push((d.endTime+d.startTime+1)/2*1e3),l=d[b];for(e in l)f=l[e],c[e]||(c[e]=[]),c[e].push(f)}return[["x"].concat(h)].concat(function(){var a;a=[];for(e in c)g=c[e],a.push([e].concat(g));return a}())},b=function(){},c=$.ajax({dataType:"json",url:"/api/v1/user-report.json"+location.search,beforeSend:function(){return console.debug("["+new Date+"] Loading report.")},error:function(a,b,c){return console.log("Ups",a,b,c,c)},success:function(b){var c,e,f,g,h,i,j,k,l,m,n;for(console.debug("["+new Date+"] Got report!"),window.report=b,l=d(new Date(1e3*b.startTime)),k=d(new Date(1e3*b.endTime)),$("#date-container").text(""+l+" - "+k+" (Resolution "+b.intervalSize/60+"min)"),$("#nav-mode-"+b.mode).addClass("active"),m=0,n=charts.length;n>m;m++)c=charts[m],g=a(b,c.id),$("#charts").append($("<h2>"+c.title+'</h2>\n<div class="row">\n  <div class="col-xs-4"><div id="'+c.id+'-pie-chart"/></div>\n  <div class="col-xs-8"><div id="'+c.id+'-line-chart"/></div>\n</div>')),j=c.pieAvg?function(){var a,b,c;for(c=[],a=0,b=g.length;b>a;a++)e=g[a],c.push([e[0],e.slice(1).reduce(function(a,b){return a+b})/(e.length-1)]);return c}():g,i=c3.generate({bindto:"#"+c.id+"-pie-chart",data:{type:"pie",x:"x",columns:j},tooltip:{format:{value:c.format}}}),h=c3.generate({bindto:"#"+c.id+"-line-chart",data:{type:"area",x:"x",columns:g,groups:[function(){var a,b,c;for(c=[],a=0,b=g.length;b>a;a++)f=g[a],c.push(f[0]);return c}()]},axis:{x:{type:"timeseries"},y:{tick:{format:c.format}}},tooltip:{format:{title:d3.time.format("%d.%m.%Y %H:00-%H:59")}}});return $(".loading-banner").hide()}})})}).call(this);