const TimeUnit =
{
     minute: {unit: 'minute', scale: 1},
     hour: {unit: 'hour', scale: 60},
}
const mainColor = "#00FF00"
const activeButtonColor = "#305232"
const defaultButtonColor = "#000000"

var isRunning = false
var activeTaskId
var segments = {}
var phases = []
var grid_size = 20
var canvas_width
var canvas_height
var load_scale = 1
var time_scale = TimeUnit.minute.unit

var canvas
var ctx
var x_axis_distance_grid_lines
var y_axis_distance_grid_lines
var num_lines_x
var num_lines_y

var max_ops
var min_ops
var avg_ops
var total_ops
var execution_time

var max_ops_summary
var min_ops_summary
var total_ops_summary
var segment_count_summary

var is_plan_polluted = false
var eventSource

document.addEventListener('DOMContentLoaded', () => {
    canvas = document.getElementById("chart");
    ctx = canvas.getContext("2d")
    canvas_width = canvas.width
    canvas_height = canvas.height
    x_axis_distance_grid_lines = canvas_height / grid_size
    y_axis_distance_grid_lines = canvas_width / grid_size
    num_lines_x = Math.floor(canvas_height/grid_size)
    num_lines_y = Math.floor(canvas_width/grid_size)
    document.getElementById("load-btn-x1").style.background = activeButtonColor
    document.getElementById("time-btn-m").style.background = activeButtonColor
    drawGrid()
    drawAxes()
    drawChart()
    getStatus()
});

function drawAxes() {
    var axes = document.getElementById("axes");
    var axesCtx = axes.getContext("2d")
    axesCtx.clearRect(0, 0, axes.width, axes.height);

    for(i=0; i <= num_lines_x; i++) {
        if(i % 5 == 0) {
            axesCtx.beginPath();
            axesCtx.fillStyle = mainColor;
            axesCtx.font = '12px Arial';
            axesCtx.textAlign = 'right';
            axesCtx.fillText((num_lines_x - i)*load_scale, (axes.width - canvas_width)/2 - 5, grid_size*i + (axes.width - canvas_width)/2 + 5);
        }
    }

    for(i=1; i <= num_lines_y; i++) {
        if(i % 10 == 0) {
            var value
            var unit = getTimeUnitShort()
            switch(time_scale) {
                case TimeUnit.minute.unit: value = i; break
                case TimeUnit.hour.unit: value = i; break
            }
            axesCtx.beginPath();
            axesCtx.fillStyle = mainColor;
            axesCtx.font = '12px Arial';
            axesCtx.textAlign = 'start';
            axesCtx.fillText(Math.floor(value) + unit, grid_size*i + (axes.width - canvas_width)/2 - 5, (axes.height - canvas_height)/2 + canvas_height + 15)
        }
    }
}

function drawGrid() {
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    ctx.globalAlpha = 1.0

    var x_axis_starting_point = { number: 1, suffix: '\u03a0' };
    var y_axis_starting_point = { number: 1, suffix: '' };

    for(var i=0; i<=num_lines_x; i++) {
        ctx.beginPath()
        ctx.strokeStyle = mainColor
        if(i == x_axis_distance_grid_lines) {
            ctx.lineWidth = 2
            ctx.globalAlpha = 1.0
        } else {
            ctx.lineWidth = 0.5
            if(i % 5 == 0) {
                ctx.globalAlpha = 1.0
            } else {
                ctx.globalAlpha = 0.5
            }
        }

        ctx.moveTo(0, grid_size*i);
        ctx.lineTo(canvas_width, grid_size*i);
        ctx.stroke();
    }

    for(i=0; i<=num_lines_y; i++) {
        ctx.beginPath()
        ctx.strokeStyle = mainColor
        if(i == 0) {
            ctx.lineWidth = 2
            ctx.globalAlpha = 1.0
        } else {
            ctx.lineWidth = 0.5
            if(i % 10 == 0) {
               ctx.globalAlpha = 1.0
            } else {
               ctx.globalAlpha = 0.5
            }
        }

        ctx.moveTo(grid_size*i, 0);
        ctx.lineTo(grid_size*i, canvas_height);
        ctx.stroke();
    }
}

function drawChart() {
    var letsdraw = false;
    var canvasOffset = $('#chart').offset();

      $('#chart').mousemove(function(e) {
        if (letsdraw && !isRunning) {
          var x = e.pageX - canvasOffset.left
          var y = e.pageY - canvasOffset.top
          ctx.lineTo(x, y);
          ctx.stroke();
          let phase = {}
          phase.x = x
          phase.y = canvas_height - y
          phases.push(phase)
        }
      });

      $('#chart').mousedown(function(e) {
          if(!isRunning) {
            phases = []
            clearProgressBar()
            clearPlanParams()
            setControlButtonVisible()
            drawGrid()
            letsdraw = true;
            ctx.strokeStyle = mainColor;
            ctx.lineWidth = 2;
            ctx.beginPath();
            var x = e.pageX - canvasOffset.left
            var y = e.pageY - canvasOffset.top
            ctx.moveTo(x, y);
            let phase = {}
            phase.x = e.pageX
            phase.y = canvas_height - y
            phases.push(phase)
        }
      });

      $(window).mouseup(function(e) {
        if(letsdraw && !isRunning) {
            letsdraw = false;
            var x = e.pageX - canvasOffset.left
            var y = e.pageY - canvasOffset.top
            let phase = {}
            phase.x = x
            phase.y = canvas_height - y
            phases.push(phase)
            createPlan()
            createPlanParams()
            setControlButtonVisible()
        }
      });
}

function drawPlan() {
    drawGrid()
    for(i=0; i<=num_lines_y; i++) {
        var value = segments[i]*grid_size/load_scale
        ctx.beginPath();
        ctx.lineWidth = grid_size;
        ctx.globalAlpha = 0.3
        ctx.strokeStyle = mainColor;
        drawPlanColumn(grid_size*i - Math.floor(grid_size/2), canvas_height, grid_size*i - Math.floor(grid_size/2), canvas_height-value)
    }
    is_plan_polluted = false
}

function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

async function drawPlanColumn(x1, y1, x2, y2) {
    ctx.moveTo(x1, y1);
    ctx.lineTo(x2, y2);
    ctx.stroke();
}

function createPlan() {
    segments = {}
    // first we take higher value of each grid point
    var reducedToHigherValue = phases.reduce((group, phase) => {
        var prev = group[phase.x];
        if(prev != null){
          if(phase.y > prev.y) {
            group[phase.x] = phase
          }
        } else {
            group[phase.x] = phase
        }
        return group
    }, {})
    // next we take higher/avg value of each grid segmens
    var deviationRange = Math.floor(canvas_height * 0.03) // 3% of Y-axis is deviation range
    var points = []
    for(i = 0; i <= canvas_width; i++) {
        var phase = reducedToHigherValue[i]
        if(phase != null) {
            points.push(phase.y)
        }
        if(i > 0 && i % grid_size == 0) {
            var avgValue = 0
            var pointCounter = 0
            if(points.length > 0){
                points.sort(function (a, b) {  return b - a;  })
                var last = points[0]
                for(j = 0; j < points.length; j++) {
                    if(Math.abs(last - points[j]) <= deviationRange) {
                        avgValue = avgValue + points[j]
                        last = points[j]
                        pointCounter++
                    }
                }
            }
            // scale to grid size and load_scale
            var value = Math.floor(avgValue/pointCounter*load_scale/grid_size)
            segments[i/grid_size] = value || 0
            points = []
        }
    }

    // next we fill values of empty grid segments
    var firstFound = false
    var emptySegmentsCounter = 0
    var lastKnown
    for(i = 0; i < num_lines_y; i++) {
        var segment = segments[i]
        if(segment > 0) {
            firstFound = true
            if(emptySegmentsCounter > 0) {
                var diff = Math.floor((segment - lastKnown)/emptySegmentsCounter)
                for(j = i-1; j >= i - emptySegmentsCounter; j--) {
                    segments[j] = segment - diff
                }
            }
            lastKnown = segment
            emptySegmentsCounter = 0
        } else if(firstFound) {
            emptySegmentsCounter++
        }
    }
    // nex we create plan params
    for(i = 1; i <= num_lines_y; i++) {
        var segment = segments[i]
        if(i <= 1) {
            max_ops = segment
            min_ops = segment
            total_ops = segment
        } else {
            if(segment > max_ops)
                max_ops = segment
            if(segment < min_ops)
                min_ops = segment
            total_ops += segment
        }
    }
    avg_ops = Number((total_ops/num_lines_y).toFixed(1))
    execution_time = getScaledTime(num_lines_y)
    drawPlan()
    createPlanParams()
}

function setLoadScale(e, id) {
    if(!isRunning) {
        var loadButtons = document.getElementsByClassName("load-btn")
        for(i = 0; i < loadButtons.length; i++) {
            loadButtons[i].style.background = defaultButtonColor
        }
        document.getElementById(id).style.background = activeButtonColor
        load_scale = e
        drawAxes()
        createPlan()
    }
}

function setTimeScale(e, id) {
    if(!isRunning) {
        var timeButtons = document.getElementsByClassName("time-btn")
        for(i = 0; i < timeButtons.length; i++) {
            timeButtons[i].style.background = defaultButtonColor
        }
        document.getElementById(id).style.background = activeButtonColor
        time_scale = e
        drawAxes()
        execution_time = getScaledTime(num_lines_y)
        createPlanParams()
    }
}

function getTimeScale() {
    switch(time_scale) {
        case TimeUnit.minute.unit: return TimeUnit.minute.scale
        case TimeUnit.hour.unit: return TimeUnit.hour.scale
    }
}

function getTimeUnitShort() {
    switch(time_scale) {
        case TimeUnit.minute.unit: return 's'
        case TimeUnit.hour.unit: return 'm'
    }
}

function getScaledTime(segmentCount) {
    switch(time_scale) {
        case TimeUnit.minute.unit: return segmentCount
        case TimeUnit.hour.unit: return segmentCount
    }
}

function start() {
    if(is_plan_polluted)
        drawPlan()
    clearProgressBar()
    clearSummaryParams()
    eventSource = new EventSource("/flexible-load-generator/progress");
    eventSource.addEventListener("progress", function(event) {
        var result = JSON.parse(event.data)
        if(result.finished) {
            eventSource.close()
            getStatus()
            createSummaryParams()
        } else {
            drawProgress(result.segmentNumber, result.actualOPS)
            computeSummary(result)
        }
    })
    fetch('/flexible-load-generator/start', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({segments: segments, timeScale: getTimeScale()})
    })
    .then(response => response.json())
    .then(response => {
        if(response.responseStatus == "SUCCESS"){
            isRunning = response.result.isRunning
            activeTaskId = response.result.taskId
            drawProgress(0,0)
        } else {
            eventSource.close()
            console.log(response.errorMessage)
        }
        setControlButtonVisible()
    })
    .catch(error => {
        eventSource.close()
        console.log(error)
    })
}

function stop() {
    fetch('/flexible-load-generator/stop', {
        method: 'PUT'
    })
    .then(response => response.json())
    .then(response => {
        if(response.responseStatus == "SUCCESS"){
            isRunning = response.result.isRunning
            activeTaskId = response.result.taskId
        } else {
            console.log(response.errorMessage)
        }
        eventSource.close()
        finish(response.result)
        setControlButtonVisible()
    })
    .catch(error => {
        console.log(error)
    })
}

function getStatus() {
    fetch('/flexible-load-generator/status', {
      method: 'GET'
    })
    .then(response => response.json())
    .then(response => {
        if(response.responseStatus == "SUCCESS"){
            isRunning = response.result.isRunning
            activeTaskId = response.result.taskId
        } else {
            console.log(response.errorMessage)
        }
        setControlButtonVisible()
    })
    .catch(error => {
        console.log(error)
    });
}

function setControlButtonVisible() {
    if(phases.length != 0 || isRunning) {
        if(isRunning) {
            document.getElementById("start-btn").style.display = 'none'
            document.getElementById("stop-btn").style.display = ''
            document.getElementById("stop-btn").style.background = activeButtonColor
        } else {
            document.getElementById("start-btn").style.display = ''
            document.getElementById("stop-btn").style.display = 'none'
        }
    } else {
        document.getElementById("start-btn").style.display = 'none'
        document.getElementById("stop-btn").style.display = 'none'
    }
}

function createPlanParams(){
    if(phases.length != 0) {
        document.getElementById("execution_time_plan").innerHTML = `${execution_time}${getTimeUnitShort()}`
        document.getElementById("total_plan").innerHTML = `${total_ops*getTimeScale()}`
        document.getElementById("max_ops_plan").innerHTML = `${max_ops}`
        document.getElementById("min_ops_plan").innerHTML = `${min_ops}`
        document.getElementById("avg_ops_plan").innerHTML = `${avg_ops}`
    }
}

function clearPlanParams(){
    total_ops = 0
    max_ops = 0
    min_ops = 0
    execution_time = 0
    avg_ops = 0
    document.getElementById("execution_time_plan").innerHTML = ``
    document.getElementById("total_plan").innerHTML = ``
    document.getElementById("max_ops_plan").innerHTML = ``
    document.getElementById("min_ops_plan").innerHTML = ``
    document.getElementById("avg_ops_plan").innerHTML = ``
}

function computeSummary(response) {
    if(response.segmentNumber <= 1) {
        segment_count_summary = 1
        total_ops_summary = response.processedOperations
        max_ops_summary = response.actualOPS
        min_ops_summary = response.actualOPS
    } else {
        segment_count_summary++
        total_ops_summary += response.processedOperations
        if(response.actualOPS > max_ops_summary)
            max_ops_summary = response.actualOPS
        if(response.actualOPS < min_ops_summary)
            min_ops_summary = response.actualOPS
    }
}

function createSummaryParams(){
    var total_ops_summary_local
    if(segment_count_summary > 0)
        total_ops_summary_local = Number((total_ops_summary/getTimeScale()/segment_count_summary).toFixed(1))
    else
        total_ops_summary_local = 0
    document.getElementById("execution_time_summary").innerHTML = `${getScaledTime(segment_count_summary)}${getTimeUnitShort()}`
    document.getElementById("total_summary").innerHTML = `${total_ops_summary}`
    document.getElementById("max_ops_summary").innerHTML = `${max_ops_summary}`
    document.getElementById("min_ops_summary").innerHTML = `${min_ops_summary}`
    document.getElementById("avg_ops_summary").innerHTML = `${total_ops_summary_local}`
}

function clearSummaryParams(){
    total_ops_summary = 0
    max_ops_summary = 0
    min_ops_summary = 0
    segment_count_summary = 0
    document.getElementById("execution_time_summary").innerHTML = ``
    document.getElementById("total_summary").innerHTML = ``
    document.getElementById("max_ops_summary").innerHTML = ``
    document.getElementById("min_ops_summary").innerHTML = ``
    document.getElementById("avg_ops_summary").innerHTML = ``
}

function finish(response) {
    createSummaryParams(response)
    setControlButtonVisible()
}

function drawProgress(segmentNumber, actualOPS) {
    if(isRunning) {
        fillProgressColumn(segmentNumber, actualOPS)
        drawProgressBar(segmentNumber)
    } else {
        eventSource.close()
        getStatus()
    }
}

function clearProgressBar() {
    document.getElementById("progress-percentage").innerText = ""
    document.getElementById("progress-bar").style.width = "0px";
}

function fillProgressColumn(columnNumber, columnValue) {
    ctx.beginPath();
    ctx.lineWidth = grid_size;
    ctx.globalAlpha = 0.5
    ctx.strokeStyle = mainColor;
    drawPlanColumn(grid_size*columnNumber - Math.floor(grid_size/2), canvas_height, grid_size*columnNumber - Math.floor(grid_size/2), canvas_height-columnValue*grid_size/load_scale)
    is_plan_polluted = true
}

function drawProgressBar(percentage) {
    document.getElementById("progress-percentage").innerText = Math.ceil(percentage*1.66) + "%"
    document.getElementById("progress-bar").style.width = percentage * grid_size + "px";
}