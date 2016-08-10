(kmplot_namespace = function() {
    var input = {};
    var layoutContainer;
    var MIN_HEIGHT = 300, MIN_WIDTH = 400;
    var maxTime = 0;
    var _representation, _value;
    var _data;
    var DEFAULT_GROUP = "Study Objects";
    
    input.init = function(representation, value) {
    	
    	if (!representation.table) {
    		return;//TODO: Error message
    	}
    	
    	// Store value and representation for later
        _value = value;
        _representation = representation;
        _data = createData();
        
        var body = d3.select("body");
        
        d3.selectAll("html, body").style({
        	margin : "0px",
        	padding : "0px",
        	width : "100%",
        	height : "100%"
        });
        
        // Create container for our content
        layoutContainer = body.append("div")
        					.attr("id", "layoutContainer")
                			.style("min-width", MIN_WIDTH + "px")
                			.style("min-height", MIN_HEIGHT + "px");
        
        // Size layout container based on sizing settings
        if (_representation.fullscreen && _representation.runningInView) {
            layoutContainer
            	.style("width", "100%")
            	.style("height", "100%");
        } else {
            layoutContainer
            	.style("width", _representation.width + "px")
            	.style("height", _representation.height + "px");
        }
        
        // Add container for user controls at the bottom if they are enabled and we are running in a view
        var controlHeight;
        if (_representation.enableViewControls && _representation.runningInView) {
             var controlsContainer = body.append("div")
             							.attr("id", "controlContainer");
             
             controlsContainer.style({
            	 "position" : "relative",
	             "bottom" : "0px",
	             "width" : "100%",
	             "padding" : "5px",
	             "padding-left" : "60px",
	             "border-top" : "1px solid black",
	             "background-color" : "white",
	             "box-sizing" : "border-box"
             });
             
             // Fill container
             createControls(controlsContainer);
             controlHeight = controlsContainer.node().getBoundingClientRect().height;
        } else {
            controlHeight = 0;
        }
        
        // Adjust plot height for control height
        if (_representation.fullscreen && _representation.runningInView && controlHeight) {
            layoutContainer.style("height", "calc(100% - " + controlHeight + "px)");
        }

        // Add SVG element
        var svg1 = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        layoutContainer[0][0].appendChild(svg1);
        
        var d3svg = d3.select("svg")
        			.style("font-family", "sans-serif");
        // Add rectangle for background color
        d3svg.append("rect")
        		.attr("id", "bgr")
        		.attr("fill", _representation.backgroundColor || "#fff");
        
        // Append a group for the plot and add a rectangle for the data area background
        d3svg.append("g")
				.attr("id", "plotG")
				.append("rect")
					.attr("id", "da")
					.attr("fill", _representation.daColor || "#fff");
        
        // Title
        d3svg.append("text")
            .attr("id", "title")
            .attr("font-size", 24)
            .attr("x", 20)
            .attr("y", 30)
            .text(_value.title);
        
        // Subtitle
        d3svg.append("text")
            .attr("id", "subtitle")
            .attr("font-size", 12)
            .attr("x", 20)
            .attr("y", 46)
            .text(_value.subtitle);
        
        drawChart();
        
        if (parent != undefined && parent.KnimePageLoader != undefined) {
            parent.KnimePageLoader.autoResize(window.frameElement.id);
        }
    }
    
    function createControls(controlsContainer) {
        if (_representation.enableViewControls) {

            var titleDiv;
        
            if (_representation.enableTitleEdit || _representation.enableSubtitleEdit) {
                titleDiv = controlsContainer.append("div").style({"margin-top" : "5px"});
            }
            
            if (_representation.enableTitleEdit) {
                titleDiv.append("label").attr("for", "titleIn").text("Title:").style({"display" : "inline-block", "width" : "100px"});
                titleDiv.append("input")
                .attr({id : "titleIn", type : "text", value : _value.title}).style("width", 150)
                .on("keyup", function() {
                    var hadTitles = (_value.title.length > 0) || (_value.subtitle.length > 0);
                    _value.title = this.value;
                    var hasTitles = (_value.title.length > 0) || (_value.subtitle.length > 0);
                    d3.select("#title").text(this.value);
                    if (hasTitles != hadTitles) {
                        drawChart(true);
                    }
                });
            }
        
            if (_representation.enableSubtitleEdit) {
                titleDiv.append("label").attr("for", "subtitleIn")
                	.text("Subtitle:")
                	.style({
                			"margin-left" : "10px",
                			"display" : "inline-block",
                			"width" : "100px"
                		});
                titleDiv.append("input")
                .attr({id : "subtitleIn", type : "text", value : _value.subtitle}).style("width", 150)
                .on("keyup", function() {
                    var hadTitles = (_value.title.length > 0) || (_value.subtitle.length > 0);
                    _value.subtitle = this.value;
                    var hasTitles = (_value.title.length > 0) || (_value.subtitle.length > 0);
                    d3.select("#subtitle").text(this.value);
                    if (hasTitles != hadTitles) {
                        drawChart(true);
                    }
                });
            }
        }
    }

    function createData() {
        var groups = {};
        var ktable = new kt();
        ktable.setDataTable(_representation.table);
		var table = _representation.table;
		//alert(JSON.stringify(_representation));
		var timeIdx = getDataColumnID(_representation.timeCol, table);
		var eventHappenedIdx = getDataColumnID("#True(" + _representation.eventCol + ")", table);
		var censorHappenedIdx = getDataColumnID("#False(" + _representation.eventCol + ")", table);
		var groupIdx = getDataColumnID(_representation.groupCol, table);
		
		var data = {};
		var counts = {};
		
		for (var r = 0; r < table.rows.length; r++) {
			var row = table.rows[r].data;
			var group = row[groupIdx] || DEFAULT_GROUP;
			if(!data.hasOwnProperty(group)) {
				data[group] = {
						plot : [[0, 1]],
						values : [],
						censors : []
				};
				counts[group] = 1;
			} else {
				var numEvent = row[eventHappenedIdx];
				var censored = row[censorHappenedIdx];
				counts[group] += numEvent + censored;
			}
			maxTime = Math.max(maxTime, table.rows[r].data[timeIdx]);
		}
		
		var prevTime = null;
		for (var r = 0; r < table.rows.length; r++) {
			var row = table.rows[r].data;
			var group = row[groupIdx] || DEFAULT_GROUP;
			var time = row[timeIdx];
			var numEvent = row[eventHappenedIdx];
			var censored = row[censorHappenedIdx];
			var newCount = counts[group] - numEvent;
			var value = newCount / counts[group];
			
			var prod = value;
			for (var i = 0; i < data[group].values.length; i++) {
				prod *= data[group].values[i];
			}
			
			data[group].values.push(value);
				
			data[group].plot.push([time, data[group].plot[data[group].plot.length - 1][1]]);
			
			data[group].plot.push([time, prod]);
			
			counts[group] = newCount - censored;
			prevTime = time;
			
			if (censored > 0) {
				data[group].censors.push([time, prod]);
			}
		}
        return data;
    }
    
    function getDataColumnID(columnName, table) {
    	if (columnName == null) {
    		return null;
    	}
		var colID = null;
		for (var i = 0; i < table.spec.numColumns; i++) {
			if (table.spec.colNames[i] === columnName) {
				colID = i;
				break;
			};
		};
		return colID;
	};
    
    // Draws the chart. If resizing is true, there are no animations.
    function drawChart(resizing) {
    	
        // Calculate the correct chart width
        var cw = Math.max(MIN_WIDTH, _representation.width);
        var ch = Math.max(MIN_HEIGHT, _representation.height);
        
        var chartWidth, chartHeight;
        // If we are fullscreen, we set the chart width to 100%
        if (_representation.fullscreen && _representation.runningInView) {
            chartWidth = "100%";
            chartHeight = "100%";
        } else {
        	chartWidth = cw + "px;"
            chartHeight = ch + "px";
        }
        
        var maxLength = 0;
        
		var color = d3.scale.category10();

		var d3svg = d3.select("svg")
		.attr({width : cw, height : ch})
		.style({width : chartWidth, height : chartHeight});
		
        d3.select(".legend").remove();
        var maxLength = 0;
        var mTop = (_value.subtitle || _value.title) ? 60 : 15;
        
        if (_representation.showLegend) {
	        var legendG = d3svg.append("g").attr("class", "legend");
	        
	        for (var i = 0; i < Object.keys(_data).length; i++) {
	        	var cat = Object.keys(_data)[i];
	        	var txt = legendG.append("text").attr("x", 20).attr("y", i * 23).text(cat);
	        	maxLength = Math.max(maxLength, txt.node().getComputedTextLength());
	        	
	        	legendG.append("circle").attr("cx", 5).attr("cy", i * 23 - 4).attr("r", 5)
	        	.attr("fill", color(cat));
	        }
	        maxLength += 35;
	        legendG.attr("transform", "translate(" + (parseInt(d3svg.style('width')) - maxLength) + "," + mTop + ")");
	        maxLength += 10;
        }
        
        // The margins for the plot area
        var margin = {
    		top : mTop,
			left : 40,
			bottom : 40,
			right : 10 + maxLength
    	};
        
        // Position the plot group based on the margins
        var plotG = d3svg.select("#plotG")
                .attr("transform", "translate(" + margin.left + "," + margin.top + ")");
        
        // Calculate size of the plot area (without axes)
        var w = Math.max(50, parseInt(d3svg.style('width')) - margin.left - margin.right);
        var h = Math.max(50, parseInt(d3svg.style('height')) - margin.top - margin.bottom);
        
        // Resize background rectangles
        plotG.select("#da").attr({
        	width : w,
        	height : h + 5}
        );
        d3svg.select("#bgr").attr({
        	width : w + margin.left + margin.right,
        	height : h + margin.top + margin.bottom
        });
        
        var xScale = d3.scale.linear().domain([0, maxTime]).range([0, w]);
        var yScale = d3.scale.linear().domain([0, 1]).range([h, 0]);
        
        var line = d3.svg.line().x(function(d) {
        	return xScale(d[0]);
        }).y(function(d) {
        	return yScale(d[1]);
        });
        
        var xAxis = d3.svg.axis()
        .scale(xScale)
        .orient("bottom");

	    var yAxis = d3.svg.axis()
	        .scale(yScale)
	        .orient("left");
        
	    // Clear content so it does not appear twice
	    plotG.selectAll("*").remove();
	    
	    plotG.append("g")
	      .attr("class", "x axis")
	      .attr("transform", "translate(0," + h + ")")
	      .call(xAxis)
	      	.append("text")
		      .attr("y", -16)
		      .attr("x", w)
		      .attr("dy", ".71em")
		      .style("text-anchor", "end")
		      .text("Time");
	    
	    plotG.append("g")
	      .attr("class", "y axis")
	      .call(yAxis)
		    .append("text")
		      .attr("transform", "rotate(-90)")
		      .attr("y", 6)
		      .attr("x", -10)
		      .attr("dy", ".71em")
		      .style("text-anchor", "end")
		      .text("Probability");
        
	    var c = 0;
	    for (var g in _data) {
	    	plotG.append("path")
		      .datum(_data[g].plot)
		      .attr("class", "line")
		      .attr("d", line)
		      .attr("stroke-width", 2)
		      .attr("stroke", color(g)).attr("fill", "none");
	    	
	    	var crossG = plotG.selectAll("g.cross" + c).data(_data[g].censors).enter()
	    	.append("g").attr("class", "cross" + c).attr("transform", function(d) {
	    		return "translate(" + xScale(d[0]) + "," + yScale(d[1]) + ")";
	    	});
	    	
	    	var crossColor = shadeColor2(color(g), -0.3);
	    	
	    	crossG.append("line").attr({
	    		x1 : -6, x2 : 6,
	    		y1 : 0, y2 : 0,
	    		stroke : crossColor,
	    		"stroke-width" : 2
	    	});
	    	
	    	crossG.append("line").attr({
	    		x1 : 0, x2 : 0,
	    		y1 : -6, y2 : 6,
	    		stroke : crossColor,
	    		"stroke-width" : 2
	    	});
	    	c++;
	    }
	    
	    d3.selectAll(".axis path").style({
	    	fill : "none",
	    	stroke : "#000",
	    	"shape-rendering" : "crispEdges"
	    });
	    
	    d3.selectAll(".axis").style({
	    	"font-weight" : "bold"
	    });
	    
        // Set resize handler
        if (_representation.fullscreen) {
            var win = document.defaultView || document.parentWindow;
            win.onresize = resize;
        }  
    }
    
    function shadeColor2(color, percent) {   
        var f=parseInt(color.slice(1),16),t=percent<0?0:255,p=percent<0?percent*-1:percent,R=f>>16,G=f>>8&0x00FF,B=f&0x0000FF;
        return "#"+(0x1000000+(Math.round((t-R)*p)+R)*0x10000+(Math.round((t-G)*p)+G)*0x100+(Math.round((t-B)*p)+B)).toString(16).slice(1);
    }
    
    input.getSVG = function() {
        var svg = d3.select("svg")[0][0];
        return (new XMLSerializer()).serializeToString(svg);
    };
    
    function resize(event) {
        drawChart(true);
    };
    

    input.validate = function() {
        return true;
    }

    input.getComponentValue = function() {
        return _value;
    }

    return input;

}());