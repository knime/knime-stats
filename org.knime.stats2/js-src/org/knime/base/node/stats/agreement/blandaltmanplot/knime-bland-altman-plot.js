knime_bland_altman_plot = function() {

	view = {};
	var _representation = null;
	var _value = null;
	var _keyedDataset = null;
	var chartManager = null;
	var containerID = "scatterContainer";
	var minWidth = 400;
	var minHeight = 300;
	var defaultFont = "sans-serif";
	var defaultFontSize = 12;

	view.init = function(representation, value) {
		if ((!representation.keyedDataset)
				|| representation.keyedDataset.rows.length < 1) {
			d3.select("body").text("Error: No data available");
			return;
		}
		_representation = representation;
		_value = value;
		try {
			_keyedDataset = new jsfc.KeyedValues2DDataset();
			for (var rowIndex = 0; rowIndex < _representation.keyedDataset.rows.length; rowIndex++) {
				var rowKey = _representation.keyedDataset.rows[rowIndex].rowKey;
				var row = _representation.keyedDataset.rows[rowIndex];
				var properties = row.properties;
				for (var col = 0; col < _representation.keyedDataset.columnKeys.length; col++) {
					var columnKey = _representation.keyedDataset.columnKeys[col];
					_keyedDataset.add(rowKey, columnKey, row.values[col]);
				}
				for ( var propertyKey in properties) {
					_keyedDataset.setRowProperty(rowKey, propertyKey,
							properties[propertyKey]);
				}
			}
			for (var col = 0; col < _representation.keyedDataset.columnKeys.length; col++) {
				var symbolProp = _representation.keyedDataset.symbols[col];
				if (symbolProp) {
					var columnKey = _representation.keyedDataset.columnKeys[col];
					var symbols = [];
					for ( var symbolKey in symbolProp) {
						symbols.push({
							"symbol" : symbolProp[symbolKey],
							"value" : symbolKey
						});
					}
					_keyedDataset.setColumnProperty(columnKey, "symbols",
							symbols);
				}
			}
			d3.select("html").style("width", "100%").style("height", "100%");
			d3.select("body").style("width", "100%").style("height", "100%")
					.style("margin", "0").style("padding", "0");
			var layoutContainer = "layoutContainer";
			d3.select("body").attr("id", "body").append("div").attr("id",
					layoutContainer).style("width", "100%").style("height",
					"100%").style("min-width", minWidth + "px").style(
					"min-height", (minHeight + getControlHeight()) + "px");
			drawChart(layoutContainer);
			if (_representation.enableViewConfiguration
					|| _representation.showZoomResetButton) {
				drawControls(layoutContainer);
			}
		} catch (err) {
			if (err.stack) {
				alert(err.stack);
			} else {
				alert(err);
			}
		}
		if (parent != undefined && parent.KnimePageLoader != undefined) {
			parent.KnimePageLoader.autoResize(window.frameElement.id);
		}
		d3.select("#chart_svg").append("line").attr("id", "upperLimit").attr(
				"x1", 0).attr("y1", 0).attr("x2", 0).attr("y2", 0).attr(
				"style", "stroke:rgb(200,0,0);stroke-width:2");
		d3.select("#chart_svg").append("line").attr("id", "bias").attr("x1", 0)
				.attr("y1", 0).attr("x2", 0).attr("y2", 0).attr("style",
						"stroke:rgb(0,0,200);stroke-width:2");
		d3.select("#chart_svg").append("line").attr("id", "lowerLimit").attr(
				"x1", 0).attr("y1", 0).attr("x2", 0).attr("y2", 0).attr(
				"style", "stroke:rgb(200,0,0);stroke-width:2");
		d3.select("#chart_svg").append("line").attr("id", "zero").attr("x1", 0)
				.attr("y1", 0).attr("x2", 0).attr("y2", 0).attr("style",
						"stroke:rgb(100,100,100);stroke-width:2").attr(
						"stroke-dasharray", "5, 5");
		updateLines(chartManager.getChart().getPlot());
	};

	view.getSVG = function() {
		if (!chartManager || !chartManager.getElement()) {
			return null;
		}
		var svg = chartManager.getElement();
		d3.select(svg).selectAll("circle").each(function() {
			this.removeAttributeNS("http://www.jfree.org", "ref");
		});
		return (new XMLSerializer()).serializeToString(svg);
	};

	buildXYDataset = function() {
		var xyDataset = jsfc.DatasetUtils.extractXYDatasetFromColumns2D(
				_keyedDataset, _value.xColumn, _value.yColumn);
		return xyDataset;
	};

	var lastPlotBounds = {
		x : 0,
		y : 0,
		width : 0,
		height : 0,
		upperYBound : 0,
		lowerYBound : 0
	};

	updateLines = function(plot) {
		var x = plot.dataArea()._x;
		var y = plot.dataArea()._y;
		var width = plot.dataArea()._width;
		var height = plot.dataArea()._height;
		var upperYBound = plot.getYAxis()._upperBound;
		var lowerYBound = plot.getYAxis()._lowerBound;
		if (x != lastPlotBounds.x || y != lastPlotBounds.y
				|| width != lastPlotBounds.width
				|| height != lastPlotBounds.height
				|| upperYBound != lastPlotBounds.upperYBound
				|| lowerYBound != lastPlotBounds.lowerYBound) {
			lastPlotBounds = {
				x : x,
				y : y,
				width : width,
				height : height,
				upperYBound : upperYBound,
				lowerYBound : lowerYBound
			};
			var x1 = x;
			var x2 = x1 + width;
			// Upper limit
			var upperLimit = _representation.upperLimit;
			var upperLimitY = (((upperLimit - upperYBound) / (lowerYBound - upperYBound)) * height)
					+ y;
			if (upperLimitY > y && upperLimitY < y + height) {
				d3.select("#upperLimit").attr("x1", x1).attr("y1", upperLimitY)
						.attr("x2", x2).attr("y2", upperLimitY);
			} else {
				d3.select("#upperLimit").attr("x1", 0).attr("y1", 0).attr("x2",
						0).attr("y2", 0);
			}
			// Bias
			var bias = _representation.bias;
			var biasY = (((bias - upperYBound) / (lowerYBound - upperYBound)) * height)
					+ y;
			if (biasY > y && biasY < y + height) {
				d3.select("#bias").attr("x1", x1).attr("y1", biasY).attr("x2",
						x2).attr("y2", biasY);
			} else {
				d3.select("#bias").attr("x1", 0).attr("y1", 0).attr("x2", 0)
						.attr("y2", 0);
			}
			// Lower limit
			var lowerLimit = _representation.lowerLimit;
			var lowerLimitY = (((lowerLimit - upperYBound) / (lowerYBound - upperYBound)) * height)
					+ y;
			if (lowerLimitY > y && lowerLimitY < y + height) {
				d3.select("#lowerLimit").attr("x1", x1).attr("y1", lowerLimitY)
						.attr("x2", x2).attr("y2", lowerLimitY);
			} else {
				d3.select("#lowerLimit").attr("x1", 0).attr("y1", 0).attr("x2",
						0).attr("y2", 0);
			}
			// Zero
			var zeroY = (((0 - upperYBound) / (lowerYBound - upperYBound)) * height)
					+ y;
			if (zeroY > y && zeroY < y + height) {
				d3.select("#zero").attr("x1", x1).attr("y1", zeroY).attr("x2",
						x2).attr("y2", zeroY);
			} else {
				d3.select("#zero").attr("x1", 0).attr("y1", 0).attr("x2", 0)
						.attr("y2", 0);
			}
		}
	}

	drawChart = function(layoutContainer) {
		if (!_value.xColumn) {
			alert("No column set for x axis!");
			return;
		}
		if (!_value.yColumn) {
			alert("No column set for y axis!");
			return;
		}
		var xAxisLabel = _value.xAxisLabel ? _value.xAxisLabel : _value.xColumn;
		var yAxisLabel = _value.yAxisLabel ? _value.yAxisLabel : _value.yColumn;
		var dataset = buildXYDataset();
		var chartWidth = _representation.imageWidth + "px;"
		var chartHeight = _representation.imageHeight + "px";
		if (_representation.resizeToWindow) {
			chartWidth = "100%";
			chartHeight = "calc(100% - " + getControlHeight() + "px)";
		}
		d3.select("#" + layoutContainer).append("div").attr("id", containerID)
				.style("width", chartWidth).style("height", chartHeight).style(
						"min-width", minWidth + "px").style("min-height",
						minHeight + "px").style("box-sizing", "border-box")
				.style("overflow", "hidden").style("margin", "0");
		var plot = new jsfc.XYPlot(dataset);
		plot.setStaggerRendering(true);
		plot.getXAxis().setLabel(xAxisLabel);
		plot.getXAxis().setLabelFont(
				new jsfc.Font(defaultFont, defaultFontSize, true));
		plot.getYAxis().setLabel(yAxisLabel);
		plot.getYAxis().setLabelFont(
				new jsfc.Font(defaultFont, defaultFontSize, true));
		plot.renderer = new jsfc.ScatterRenderer(plot);
		var chart = new jsfc.Chart(plot);
		chart.setTitleAnchor(new jsfc.Anchor2D(jsfc.RefPt2D.TOP_LEFT));
		var chartTitle = _value.chartTitle ? _value.chartTitle : "";
		var chartSubtitle = _value.chartSubtitle ? _value.chartSubtitle : "";
		chart.setTitle(chartTitle, chartSubtitle, chart.getTitleAnchor());
		chart.setLegendBuilder(null);
		d3.select("#" + containerID).append("svg").attr("id", "chart_svg")
				.style("width", "100%").style("height", "100%");
		var svg = document.getElementById("chart_svg");
		var zoomEnabled = _representation.enableZooming;
		var dragZoomEnabled = _representation.enableDragZooming;
		var panEnabled = _representation.enablePanning;
		chartManager = new jsfc.ChartManager(svg, chart, dragZoomEnabled,
				zoomEnabled, false);
		if (panEnabled) {
			var panModifier = new jsfc.Modifier(false, false, false, false);
			if (dragZoomEnabled) {
				panModifier = new jsfc.Modifier(false, true, false, false);
			}
			var panHandler = new jsfc.PanHandler(chartManager, panModifier);
			chartManager.addLiveHandler(panHandler);
		}
		setChartDimensions();
		chartManager.refreshDisplay();
		var win = document.defaultView || document.parentWindow;
		win.onresize = resize;
		var xMargin = (_value.xAxisMax - _value.xAxisMin) * 0.1;
		var yMargin = (_value.yAxisMax - _value.yAxisMin) * 0.1;
		plot.getXAxis().setBounds(_value.xAxisMin - xMargin,
				_value.xAxisMax + xMargin, true);
		plot.getYAxis().setBounds(_value.yAxisMin - yMargin,
				_value.yAxisMax + yMargin, true);
		plot.addListener(updateLines);
	};

	resize = function(event) {
		setChartDimensions();
		chartManager.refreshDisplay();
		updateLines(chartManager.getChart().getPlot());
	};

	setChartDimensions = function() {
		var container = document.getElementById(containerID);
		var w = _representation.imageWidth;
		var h = _representation.imageHeight;
		if (_representation.resizeToWindow) {
			w = Math.max(minWidth, container.clientWidth);
			h = Math.max(minHeight, container.clientHeight);
		}
        chartManager.getChart().setSize(w, h);
	};

	updateChart = function() {
		var plot = chartManager.getChart().getPlot();
		plot.setDataset(buildXYDataset());
		plot.getXAxis().setAutoRange(true);
		plot.getYAxis().setAutoRange(true);
	};

	updateTitle = function() {
		_value.chartTitle = document.getElementById("chartTitleText").value;
		chartManager.getChart().setTitle(_value.chartTitle,
				_value.chartSubtitle, chartManager.getChart().getTitleAnchor());
	};

	updateSubtitle = function() {
		_value.chartSubtitle = document.getElementById("chartSubtitleText").value;
		chartManager.getChart().setTitle(_value.chartTitle,
				_value.chartSubtitle, chartManager.getChart().getTitleAnchor());
	};

	updateXAxisLabel = function() {
		_value.xAxisLabel = document.getElementById("xAxisText").value;
		var newAxisLabel = _value.xAxisLabel;
		if (!_value.xAxisLabel) {
			newAxisLabel = _value.xColumn;
		}
		chartManager.getChart().getPlot().getXAxis().setLabel(newAxisLabel);
	};

	updateYAxisLabel = function() {
		_value.yAxisLabel = document.getElementById("yAxisText").value;
		var newAxisLabel = _value.yAxisLabel;
		if (!_value.xAxisLabel) {
			newAxisLabel = _value.yColumn;
		}
		chartManager.getChart().getPlot().getYAxis().setLabel(newAxisLabel);
	};

	drawControls = function(layoutContainer) {
		var controlContainer = d3.select("#" + layoutContainer).insert("table",
				"#" + containerID + " ~ *").attr("id", "scatterControls")
				.style("padding", "10px").style("margin", "0 auto").style(
						"box-sizing", "border-box").style("font-family",
						defaultFont).style("font-size", defaultFontSize + "px")
				.style("border-spacing", 0)
				.style("border-collapse", "collapse");
		if (_representation.showZoomResetButton) {
			var resetButtonContainer = controlContainer.append("tr").append(
					"td").attr("colspan", "4").style("text-align", "center");
			resetButtonContainer.append("button").text("Reset Zoom").on(
					"click", function() {
						var plot = chartManager.getChart().getPlot();
						plot.getXAxis().setAutoRange(true);
						plot.getYAxis().setAutoRange(true);
					});
		}
		if (!_representation.enableViewConfiguration)
			return;
		if (_representation.enableTitleChange
				|| _representation.enableSubtitleChange) {
			var titleEditContainer = controlContainer.append("tr");
			if (_representation.enableTitleChange) {
				titleEditContainer.append("td").append("label").attr("for",
						"chartTitleText").text("Chart Title:").style(
						"margin-right", "5px");
				var chartTitleText = titleEditContainer.append("td").append(
						"input").attr("type", "text").attr("id",
						"chartTitleText").attr("name", "chartTitleText").style(
						"font-family", defaultFont).style("font-size",
						defaultFontSize + "px").on("blur", function() {
					updateTitle();
				}).on("keypress", function() {
					if (d3.event.keyCode == 13) {
						updateTitle();
					}
				});
				if (_representation.enableYAxisLabelEdit) {
					chartTitleText.style("margin-right", "10px");
				}
				document.getElementById("chartTitleText").value = _value.chartTitle;
			}
			if (_representation.enableSubtitleChange) {
				titleEditContainer.append("td").append("label").attr("for",
						"chartSubtitleText").text("Chart Subtitle:").style(
						"margin-right", "5px");
				titleEditContainer.append("td").append("input").attr("type",
						"text").attr("id", "chartSubtitleText").attr("name",
						"chartSubtitleText").style("font-family", defaultFont)
						.style("font-size", defaultFontSize + "px").on("blur",
								function() {
									updateSubtitle();
								}).on("keypress", function() {
							if (d3.event.keyCode == 13) {
								updateSubtitle();
							}
						});
				document.getElementById("chartSubtitleText").value = _value.chartSubtitle;
			}
		}
		if (_representation.enableXColumnChange
				|| _representation.enableYColumnChange) {
			var columnChangeContainer = controlContainer.append("tr");
			if (_representation.enableXColumnChange) {
				columnChangeContainer.append("td").append("label").attr("for",
						"xColumnSelect").text("X Column:").style(
						"margin-right", "5px");
				var xSelect = columnChangeContainer.append("td").append(
						"select").attr("id", "xColumnSelect").attr("name",
						"xColumnSelect").style("font-family", defaultFont)
						.style("font-size", defaultFontSize + "px");
				var columnKeys = _keyedDataset.columnKeys();
				for (var colID = 0; colID < columnKeys.length; colID++) {
					xSelect.append("option").attr("value", columnKeys[colID])
							.text(columnKeys[colID]);
				}
				document.getElementById("xColumnSelect").value = _value.xColumn;
				xSelect.on("change",
						function() {
							_value.xColumn = document
									.getElementById("xColumnSelect").value;
							if (!_value.xAxisLabel) {
								chartManager.getChart().getPlot().getXAxis()
										.setLabel(_value.xColumn, false);
							}
							updateChart();
						});
				if (_representation.enableYColumnChange) {
					xSelect.style("margin-right", "10px");
				}
			}
			if (_representation.enableYColumnChange) {
				columnChangeContainer.append("td").append("label").attr("for",
						"yColumnSelect").text("Y Column:").style(
						"margin-right", "5px");
				var ySelect = columnChangeContainer.append("td").append(
						"select").attr("id", "yColumnSelect").attr("name",
						"yColumnSelect").style("font-family", defaultFont)
						.style("font-size", defaultFontSize + "px");
				var columnKeys = _keyedDataset.columnKeys();
				for (var colID = 0; colID < columnKeys.length; colID++) {
					ySelect.append("option").attr("value", columnKeys[colID])
							.text(columnKeys[colID]);
				}
				document.getElementById("yColumnSelect").value = _value.yColumn;
				ySelect.on("change",
						function() {
							_value.yColumn = document
									.getElementById("yColumnSelect").value;
							if (!_value.yAxisLabel) {
								chartManager.getChart().getPlot().getYAxis()
										.setLabel(_value.yColumn, false);
							}
							updateChart();
						});
			}
		}
		if (_representation.enableXAxisLabelEdit
				|| _representation.enableYAxisLabelEdit) {
			var axisLabelContainer = controlContainer.append("tr");
			if (_representation.enableXAxisLabelEdit) {
				axisLabelContainer.append("td").append("label").attr("for",
						"xAxisText").text("X Axis Label:").style(
						"margin-right", "5px");
				var xAxisText = axisLabelContainer.append("td").append("input")
						.attr("type", "text").attr("id", "xAxisText").attr(
								"name", "xAxisText").style("font-family",
								defaultFont).style("font-size",
								defaultFontSize + "px").on("blur", function() {
							updateXAxisLabel();
						}).on("keypress", function() {
							if (d3.event.keyCode == 13) {
								updateXAxisLabel();
							}
						});
				if (_representation.enableYAxisLabelEdit) {
					xAxisText.style("margin-right", "10px");
				}
				document.getElementById("xAxisText").value = _value.xAxisLabel;
			}
			if (_representation.enableYAxisLabelEdit) {
				axisLabelContainer.append("td").append("label").attr("for",
						"yAxisText").text("Y Axis Label:").style(
						"margin-right", "5px");
				axisLabelContainer.append("td").append("input").attr("type",
						"text").attr("id", "yAxisText").attr("name",
						"yAxisText").style("font-family", defaultFont).style(
						"font-size", defaultFontSize + "px").on("blur",
						function() {
							updateYAxisLabel();
						}).on("keypress", function() {
					if (d3.event.keyCode == 13) {
						updateYAxisLabel();
					}
				});
				document.getElementById("yAxisText").value = _value.yAxisLabel;
			}
		}
		if (_representation.enableDotSizeChange) {
			var dotSizeContainer = controlContainer.append("tr");
			dotSizeContainer.append("td").append("label").attr("for",
					"dotSizeInput").text("Dot Size:").style("margin-right",
					"5px");
			dotSizeContainer.append("td").append("input")
					.attr("type", "number").attr("id", "dotSizeInput").attr(
							"name", "dotSizeInput").attr("value",
							_value.dotSize).style("font-family", defaultFont)
					.style("font-size", defaultFontSize + "px");
		}
	};

	getControlHeight = function() {
		var rows = 0;
		var sizeFactor = 25;
		var padding = 10;
		if (_representation.showZoomResetButton)
			rows++;
		if (_representation.enableViewConfiguration) {
			if (_representation.enableTitleChange
					|| _representation.enableSubtitleChange)
				rows++;
			if (_representation.enableXColumnChange
					|| _representation.enableYColumnChange)
				rows++;
			if (_representation.enableXAxisLabelEdit
					|| _representation.enableYAxisLabelEdit)
				rows++;
			if (_representation.enableDotSizeChange)
				rows++;
		}
		var height = rows * sizeFactor;
		if (height > 0)
			height += padding;
		return height;
	};

	view.validate = function() {
		return true;
	};

	view.getComponentValue = function() {
		return _value;
	};

	return view;
}();