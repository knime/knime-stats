dataExplorerNamespace = function() {
	
	var view = {};
	var _representation, _value;
	var knimeTable = null;
    var previewTable = null;
    var previewDataTable = null;
    var nominalTable = null;
    var nominalDataTable = null;
	var dataTable = null;
	var selection = {};
	var allCheckboxes = [];
	var initialized = false;
    var svgWidth = 100;
    var svgHeight = 30;
    var svgWsmall = 100;
    var svgHsmall = 30;
    var svgWbig = 500;
    var svgHbig = 200;
    var xScale, yScale;
    var xAxis, yAxis, barsScale;
    var margin = {top:0.35*svgHeight, left: 0.3*svgWidth, bottom: 0.3*svgHeight, right:0.2*svgHeight};
    var content;
    var hideUnselected = false;
    var histSizes = [];
    var histCol;
    var pageLength;
    var pageLengths;
    var order = [];
    var buttons = [];
    
	
	//register neutral ordering method for clear selection button
	$.fn.dataTable.Api.register('order.neutral()', function () {
	    return this.iterator('table', function (s) {
	        s.aaSorting.length = 0;
	        s.aiDisplay.sort( function (a,b) {
	            return a-b;
	        });
	        s.aiDisplayMaster.sort( function (a,b) {
	            return a-b;
	        } );
	    } );
	});
	
	view.init = function(representation, value) {
		if (!representation || !representation.statistics) {
			$('body').append("Error: No data available");
			return;
		}
		_representation = representation;
	    _value = value;
	    if (parent && parent.KnimePageLoader) {
			drawTable();
		} else {
			$(document).ready(function() {
                
                var tabs = $('<div />').attr('id', 'tabs').appendTo('body');
                var listOfTabNames = $('<ul />').attr("class", "nav nav-tabs").attr('role', 'tabList').appendTo(tabs);
                content = $('<div />').attr('class', 'tab-content').appendTo(tabs);

                $('<li class="active"><a href="#tabs-knimePreviewContainer" data-toggle="tab" aria-expanded="true">' + 'Data Preview' + '</a></li>').appendTo(listOfTabNames);
                
                $('<li class=""><a href="#tabs-knimeDataExplorerContainer" data-toggle="tab" aria-expanded="false">' + 'Statistics' + '</a></li>').appendTo(listOfTabNames);
                
                $('<li class=""><a href="#tabs-knimeNominalContainer" data-toggle="tab" aria-expanded="false">' + 'Nominal' + '</a></li>').appendTo(listOfTabNames);
                
				drawNumericTable();
                drawDataPreviewTable();
                drawNominalTable();
                
                $('a[data-toggle="tab"]').on( 'shown.bs.tab', function (e) {
                    $.fn.dataTable.tables( {visible: true, api: true} ).columns.adjust().responsive.recalc();
                } );
                
            });
		}
        
	}
    
    drawNominalTable = function() {

        try {
            nominalTable = new kt();
			nominalTable.setDataTable(_representation.nominal);
            
			var wrapper = $('<div id="tabs-knimeNominalContainer">').attr("class", "tab-pane");
			content.append(wrapper);
            
			if (_representation.title != null && _representation.title != '') {
				wrapper.append('<h1>' + _representation.title + '</h1>')
			}
			if (_representation.subtitle != null && _representation.subtitle != '') {
				wrapper.append('<h2>' + _representation.subtitle + '</h2>')
			}
			var table = $('<table id="knimeNominal" class="table table-striped table-bordered" width="100%">');
			wrapper.append(table);
			
			var colArray = [];
			var colDefs = [];
            
            if (_representation.displayRowIds || true) {
				var title = _representation.displayRowIds ? 'Column' : '';
				var orderable = _representation.displayRowIds;
				colArray.push({
					'title': title, 
					'orderable': orderable,
					'className': 'no-break'
				});
			}
            
            if (_representation.enableSelection) {
				var all = _value.selectAll;
				colArray.push({'title': /*'<input name="select_all" value="1" id="checkbox-select-all" type="checkbox"' + (all ? ' checked' : '')  + ' />'*/ 'Exclude Column'})
				colDefs.push({
					'targets': 1,
					'searchable':false,
					'orderable':false,
					'className': 'dt-body-center',
					'render': function (data, type, full, meta) {
						//var selected = selection[data] ? !all : all;
						setTimeout(function(){
							var el = $('#checkbox-select-all').get(0);
							/*if (all && selection[data] && el && ('indeterminate' in el)) {
								el.indeterminate = true;
							}*/
						}, 0);
						return '<input type="checkbox" name="id[]"'
							+ (selection[data] ? ' checked' : '')
							+' value="' + $('<div/>').text(full[0]).html() + '">';
					}
				});
			}
            
            for (var i = 0; i < nominalTable.getColumnNames().length; i++) {
				var colType = nominalTable.getColumnTypes()[i];
				var knimeColType = nominalTable.getKnimeColumnTypes()[i];
				var colDef = {
					'title': nominalTable.getColumnNames()[i],
					'orderable' : isColumnSortable(colType),
					'searchable': isColumnSearchable(colType)					
				}
				if (_representation.displayMissingValueAsQuestionMark) {
					colDef.defaultContent = '<span class="missing-value-cell">?</span>';
				}
				if (colType == 'number' && _representation.enableGlobalNumberFormat) {
					if (nominalTable.getKnimeColumnTypes()[i].indexOf('double') > -1) {
						colDef.render = function(data, type, full, meta) {
                            //console.log("data4", data)
                            //console.log("full4", full)
                            //console.log(data)
							if (!$.isNumeric(data)) {
								return data;
							}
							return Number(data).toFixed(_representation.globalNumberFormatDecimals);
						}
					}
				}
				colArray.push(colDef);
			}
            
            pageLength = _representation.initialPageSize;
			if (_value.pageSize) {
				pageLength = _value.pageSize;
			}
			pageLengths = _representation.allowedPageSizes;
			if (_representation.pageSizeShowAll) {
				var first = pageLengths.slice(0);
				first.push(-1);
				var second = pageLengths.slice(0);
				second.push("All");
				pageLengths = [first, second];
			}
			//var order = [];
			if (_value.currentOrder) {
				order = _value.currentOrder;
			}
			//var buttons = [];
			if (_representation.enableSorting && _representation.enableClearSortButton) {
				var unsortButton = {
						'text': "Clear Sorting",
						'action': function (e, dt, node, config) {
							dt.order.neutral();
							dt.draw();
						},
						'enabled': (order.length > 0)
				}
				buttons.push(unsortButton);
			}
			var firstChunk = getDataSlice(0, _representation.initialPageSize, nominalTable);
            
			var searchEnabled = _representation.enableSearching || (knimeService && knimeService.isInteractivityAvailable());
            
			nominalDataTable = $('#knimeNominal').DataTable( {
                'columns': colArray,
				'columnDefs': colDefs,
				'order': order,
                //'retrieve': true,
				'paging': _representation.enablePaging,
				'pageLength': pageLength,
				'lengthMenu': pageLengths,
				'lengthChange': _representation.enablePageSizeChange,
				'searching': searchEnabled,
				'ordering': _representation.enableSorting,
				'processing': true,
				'deferRender': !_representation.enableSelection,
				'data': firstChunk,
				'buttons': buttons,
                'responsive': true,
                'scrollX':false,
				'fnDrawCallback': function() {
					if (!_representation.displayColumnHeaders) {
						$("#knimeDataExplorer thead").remove();
				  	}
					if (searchEnabled && !_representation.enableSearching) {
						$('#knimeDataExplorer_filter').remove();
					}
				}
			});
            
            drawControls("knimeNominal", nominalTable, nominalDataTable);
            
            if (_representation.enableSelection) {
				// Handle click on "Select all" control
				var selectAllCheckbox = $('#checkbox-select-all').get(0);
				if (selectAllCheckbox) {
					if (selectAllCheckbox.checked && ('indeterminate' in selectAllCheckbox)) {
						selectAllCheckbox.indeterminate = _value.selectAllIndeterminate;
					}
					selectAllCheckbox.addEventListener('click', function() {
						selectAll(this.checked);
					});
				}

				// Handle click on checkbox to set state of "Select all" control
				$('#knimeNominal tbody').on('change', 'input[type="checkbox"]', function() {
					//var el = $('#checkbox-select-all').get(0);
					//var selected = el.checked ? !this.checked : this.checked;
					// we could call delete _value.selection[this.value], but the call is very slow 
					// and we can assume that a user doesn't click on a lot of checkboxes
					selection[this.value] = this.checked;
					
					if (this.checked) {
						/*if (knimeService && knimeService.isInteractivityAvailable() && _value.publishSelection) {
							knimeService.addRowsToSelection(_representation.table.id, [this.value], selectionChanged);
						}*/
					} else {
						// If "Select all" control is checked and has 'indeterminate' property
						if(selectAllCheckbox && selectAllCheckbox.checked && ('indeterminate' in selectAllCheckbox)){
							// Set visual state of "Select all" control as 'indeterminate'
							selectAllCheckbox.indeterminate = true;
							_value.selectAllIndeterminate = true;
						}
						if (hideUnselected) {
							nominalTable.draw('full-hold');
						}
						/*if (knimeService && knimeService.isInteractivityAvailable() && _value.publishSelection) {
							knimeService.removeRowsFromSelection(_representation.table.id, [this.value], selectionChanged);
						}*/
					}
				});
			}
			
			//load all data
			setTimeout(function() {
				var initialChunkSize = 10;
				addDataToTable(_representation.initialPageSize, initialChunkSize, nominalTable, nominalDataTable, "knimeNominal");
			}, 0);
        } catch (err) {
			if (err.stack) {
				alert(err.stack);
			} else {
				alert (err);
			}
		}
    }
	
	drawNumericTable = function() {
		if (_representation.enableSelection && _value.selection) {
			for (var i = 0; i < _value.selection.length; i++) {
				selection[_value.selection[i]] = true;
			}
		}
		try {
			knimeTable = new kt();
			knimeTable.setDataTable(_representation.statistics);
            
			var wrapper = $('<div id="tabs-knimeDataExplorerContainer">').attr("class", "tab-pane");
			content.append(wrapper);
            
			if (_representation.title != null && _representation.title != '') {
				wrapper.append('<h1>' + _representation.title + '</h1>')
			}
			if (_representation.subtitle != null && _representation.subtitle != '') {
				wrapper.append('<h2>' + _representation.subtitle + '</h2>')
			}
			var table = $('<table id="knimeDataExplorer" class="table table-striped table-bordered" width="100%">');
			wrapper.append(table);
			
			var colArray = [];
			var colDefs = [];
            
            if (_representation.displayRowIds || true) {
				var title = _representation.displayRowIds ? 'Column' : '';
				var orderable = _representation.displayRowIds;
				colArray.push({
					'title': title, 
					'orderable': orderable,
					'className': 'no-break'
				});
			}
			if (_representation.enableSelection) {
				var all = _value.selectAll;
				colArray.push({'title': /*'<input name="select_all" value="1" id="checkbox-select-all" type="checkbox"' + (all ? ' checked' : '')  + ' />'*/ 'Exclude Column'})
				colDefs.push({
					'targets': 1,
					'searchable':false,
					'orderable':false,
					'className': 'dt-body-center',
					'render': function (data, type, full, meta) {
						//var selected = selection[data] ? !all : all;
						setTimeout(function(){
							var el = $('#checkbox-select-all').get(0);
							/*if (all && selection[data] && el && ('indeterminate' in el)) {
								el.indeterminate = true;
							}*/
						}, 0);
						return '<input type="checkbox" name="id[]"'
							+ (selection[data] ? ' checked' : '')
							+' value="' + $('<div/>').text(full[0]).html() + '">';
					}
				});
			}
			

			//console.log(knimeTable);
			for (var i = 0; i < knimeTable.getColumnNames().length; i++) {
				var colType = knimeTable.getColumnTypes()[i];
				var knimeColType = knimeTable.getKnimeColumnTypes()[i];
				var colDef = {
					'title': knimeTable.getColumnNames()[i],
					'orderable' : isColumnSortable(colType),
					'searchable': isColumnSearchable(colType)					
				}
				if (_representation.displayMissingValueAsQuestionMark) {
					colDef.defaultContent = '<span class="missing-value-cell">?</span>';
				}
				if (colType == 'number' && _representation.enableGlobalNumberFormat) {
					if (knimeTable.getKnimeColumnTypes()[i].indexOf('double') > -1) {
						colDef.render = function(data, type, full, meta) {
                            //console.log("data4", data)
                            //console.log("full4", full)
                            //console.log(data)
							if (!$.isNumeric(data)) {
								return data;
							}
							return Number(data).toFixed(_representation.globalNumberFormatDecimals);
						}
					}
				}
				colArray.push(colDef);
			}
            
            xScale = d3.scale.linear(), 
            yScale = d3.scale.linear(),
            _representation.histograms.forEach(function(d) {histSizes.push(d.bins.length)});
            
            var colDef = {
                'title' :"Histogram",
                'orderable': false, 
                'searchable': false,
                'defaultContent':  '<span class="missing-value-cell">?</span>'
            }

            colDef.render = function(data, type, full, meta) {
                //console.log("data", data)
                svgHeight = svgHsmall + margin.top;
                svgWidth = svgWsmall;
                
                xScale.range([0, svgWidth])
                    .domain([0, data.bins[data.bins.length-1].def.second - data.bins[0].def.first]);
                yScale.range([svgHsmall, 0])
                    .domain([0, data.maxCount]);
                
                //var fill = colorScale(data.colIndex);
                var histDiv = document.createElement("div");

                var svg = d3.select(histDiv).attr("class", "hist")
                    .append("svg")
                    .attr("height", svgHeight)
                    .attr("width", svgWidth)
                    .attr("class", "svg_hist")
                    .attr("id", "svg"+data.colIndex);
                    //.attr("id", "svg"+meta.row);
                
                var bar_group = svg.append("g")
                    .attr("transform", "translate(" + [0 , margin.top] + ")")
                    .attr("class", "bars")
                    //.attr("id", "id"+data.colIndex);
                    .attr("id", "svg"+meta.row);
                
                var bars = bar_group.selectAll("rect")
                    .data(data.bins)
                        .enter()
                    .append("rect")
                    .attr("class", "rect"+data.colIndex)
                    //.attr("class", "rect"+meta.row)
                    .attr("x", function (d) {return xScale(d.def.first - data.bins[0].def.first);})
                    .attr("y", function(d) {return yScale(d.count);})
                    .attr("width", function(d) {return xScale(d.def.second - d.def.first)})
                    .attr("height", function(d){return svgHsmall - yScale(d.count);})
                    .attr("fill", "#547cac")
                    .attr("stroke", "black")
                    .attr("stroke-width", "1px")
                    .append("title")
                    .text(function(d, i) { return d.tooltip.slice(0,-13); });
                
                return $('<div/>').append(histDiv).html();
            }
            colArray.push(colDef);
            
            //number of histogram column to use in the next calculations
            histCol = colArray.length - 1;
            
			pageLength = _representation.initialPageSize;
			if (_value.pageSize) {
				pageLength = _value.pageSize;
			}
			pageLengths = _representation.allowedPageSizes;
			if (_representation.pageSizeShowAll) {
				var first = pageLengths.slice(0);
				first.push(-1);
				var second = pageLengths.slice(0);
				second.push("All");
				pageLengths = [first, second];
			}
			//var order = [];
			if (_value.currentOrder) {
				order = _value.currentOrder;
			}
			//var buttons = [];
			if (_representation.enableSorting && _representation.enableClearSortButton) {
				var unsortButton = {
						'text': "Clear Sorting",
						'action': function (e, dt, node, config) {
							dt.order.neutral();
							dt.draw();
						},
						'enabled': (order.length > 0)
				}
				buttons.push(unsortButton);
			}
			var firstChunk = getDataSlice(0, _representation.initialPageSize, knimeTable);
            
			var searchEnabled = _representation.enableSearching || (knimeService && knimeService.isInteractivityAvailable());
			dataTable = $('#knimeDataExplorer').DataTable( {
                'columns': colArray,
				'columnDefs': colDefs,
				'order': order,
                //'retrieve': true,
				'paging': _representation.enablePaging,
				'pageLength': pageLength,
				'lengthMenu': pageLengths,
				'lengthChange': _representation.enablePageSizeChange,
				'searching': searchEnabled,
				'ordering': _representation.enableSorting,
				'processing': true,
				'deferRender': !_representation.enableSelection,
				'data': firstChunk,
				'buttons': buttons,
                'responsive': true,
                'scrollX':false,
				'fnDrawCallback': function() {
					if (!_representation.displayColumnHeaders) {
						$("#knimeDataExplorer thead").remove();
				  	}
					if (searchEnabled && !_representation.enableSearching) {
						$('#knimeDataExplorer_filter').remove();
					}
				}
			});
            
            drawControls("knimeDataExplorer", knimeTable, dataTable);
			
			//Clear sorting button placement and enable/disable on order change
//			if (_representation.enableSorting && _representation.enableClearSortButton) {
//				dataTable.buttons().container().appendTo('#knimeDataExplorer_wrapper .col-sm-6:eq(0)');
//				$('#knimeDataExplorer_length').css({'display': 'inline-block', 'margin-right': '10px'});
//				dataTable.on('order.dt', function () {
//					var order = dataTable.order();
//					dataTable.button(0).enable(order.length > 0);
//				});
//			}
//			
//			$('#knimeDataExplorer_paginate').css('display', 'none');
//
//			$('#knimeDataExplorer_info').html(
//				'<strong>Loading data</strong> - Displaying '
//				+ 1 + ' to ' + Math.min(knimeTable.getNumRows(), _representation.initialPageSize)
//				+ ' of ' + knimeTable.getNumRows() + ' entries.');
//			
//			if (knimeService) {
//				if (_representation.enableSearching && !_representation.title) {
//					knimeService.floatingHeader(false);
//				}
//				if (_representation.displayFullscreenButton) {
//					knimeService.allowFullscreen();
//				}
//			}
			
			if (_representation.enableSelection) {
				// Handle click on "Select all" control
				var selectAllCheckbox = $('#checkbox-select-all').get(0);
				if (selectAllCheckbox) {
					if (selectAllCheckbox.checked && ('indeterminate' in selectAllCheckbox)) {
						selectAllCheckbox.indeterminate = _value.selectAllIndeterminate;
					}
					selectAllCheckbox.addEventListener('click', function() {
						selectAll(this.checked);
					});
				}

				// Handle click on checkbox to set state of "Select all" control
				$('#knimeDataExplorer tbody').on('change', 'input[type="checkbox"]', function() {
					//var el = $('#checkbox-select-all').get(0);
					//var selected = el.checked ? !this.checked : this.checked;
					// we could call delete _value.selection[this.value], but the call is very slow 
					// and we can assume that a user doesn't click on a lot of checkboxes
					selection[this.value] = this.checked;
					
					if (this.checked) {
						/*if (knimeService && knimeService.isInteractivityAvailable() && _value.publishSelection) {
							knimeService.addRowsToSelection(_representation.table.id, [this.value], selectionChanged);
						}*/
					} else {
						// If "Select all" control is checked and has 'indeterminate' property
						if(selectAllCheckbox && selectAllCheckbox.checked && ('indeterminate' in selectAllCheckbox)){
							// Set visual state of "Select all" control as 'indeterminate'
							selectAllCheckbox.indeterminate = true;
							_value.selectAllIndeterminate = true;
						}
						if (hideUnselected) {
							dataTable.draw('full-hold');
						}
						/*if (knimeService && knimeService.isInteractivityAvailable() && _value.publishSelection) {
							knimeService.removeRowsFromSelection(_representation.table.id, [this.value], selectionChanged);
						}*/
					}
				});
			}
			
			//load all data
			setTimeout(function() {
				var initialChunkSize = 10;
				addDataToTable(_representation.initialPageSize, initialChunkSize, knimeTable, dataTable, "knimeDataExplorer");
			}, 0);
            
            dataTable.on("responsive-display", function(e, datatable, row, showHide, update) {
                var textScale = d3.scale.linear()
                    .range([8, 11])
                    .domain([d3.max(histSizes), 16]);
                
                var data = row.data()[histCol];
                
                //when responsive is opened it creates an additional div of the same class right under its original one
                var bigHist = $(".hist")[data.colIndex % dataTable.page.len() + 1];
                //var bigHist = $(".hist")[row[0][0] % _representation.initialPageSize + 1];
                svgWidth = svgWbig;
                svgHeight = svgHbig;
                var svgBigHist = d3.select(bigHist).select("#svg"+data.colIndex)[0][0]
                //var svgBigHist = d3.select(bigHist).select("#svg"+row[0][0])[0][0]
                
                var min = data.bins[0].def.first;
                var max = data.bins[data.bins.length - 1].def.second;
                var barWidthValue = (max - min)/data.bins.length;
                
                xScale.range([0, svgWidth - margin.left - margin.right])
                    .domain([min, (max + barWidthValue * 0.5)]);
                yScale.range([svgHeight - 2*margin.top - margin.bottom, 0])
                    .domain([0, data.maxCount]);
                
                var svg = d3.select(svgBigHist).attr("width", svgWidth).attr("height", svgHeight);
                
                var bar_group = svg.selectAll(".bars")
                    .attr("transform", "translate("+[margin.left , margin.top]+")");
                var barWidth = xScale((barWidthValue + min))
                
                var bars = svg.selectAll(".bars")
                    .selectAll(".rect"+data.colIndex)
                    //.selectAll(".rect"+row[0][0])
                    .data(data.bins)
                    .attr("x", function (d) {return xScale(d.def.first);})
                    .attr("y", function(d) {return yScale(d.count);})
                    .attr("width", function(d) {return barWidth;})
                    .attr("height", function(d){return svgHeight - 2*margin.top - margin.bottom -  yScale(d.count);})
                
                var text_group = svg.append("g")
                    .attr("class", "caption")
                    .attr("transform", "translate(" + [margin.left , margin.top] + ")")
                    .attr("id", "id"+data.colIndex);
                    //.attr("id", "id"+row[0][0]);
                
                var texts = text_group.selectAll("text")
                    .data(data.bins)
                    .enter()
                    .append("text")
                    .attr("x", function (d) {return xScale(d.def.first) + barWidth/2;})
                    .attr("y", function(d) {return yScale(d.count) - 2;})
                    .text(function(d) {return d.count;})
                    .attr("font-size", Math.round(Math.min(svgHeight/15, 11))+"px")
                    .attr("text-anchor", "middle");
                
                var ticks = [];
                data.bins.forEach(function(d,i) {
                    ticks.push(d.def.first);
                })
                ticks.push(data.bins[data.bins.length - 1].def.second)
                
                xAxis = d3.svg.axis()
                    .scale(xScale)
                    .orient("bottom")
                    .tickValues(ticks)
                    .tickFormat(d3.format(".2f"))
                
                yAxis = d3.svg.axis()
                    .scale(yScale)
                    .orient("left")
                    .ticks(5);
                
                var axisX = svg.append("g")
                    .attr("class", "x axis")
                    .attr("id", "xAxis"+data.colIndex)
                    //.attr("id", "xAxis"+row[0][0])
                    .attr("transform", "translate(" + [margin.left, svgHeight - margin.bottom - margin.top] + ")")
                    .style("font-size", function (d) {
                        if (data.bins.length > 15) {
                            return textScale(data.bins.length)+"px"
                        } else {
                            return "12px";
                        }
                    })
                    .call(xAxis);
                
                var axisY = svg.append("g")
                    .attr("class", "y axis")
                    .attr("id", "yAxis"+data.colIndex)
                    //.attr("id", "yAxis"+row[0][0])
                    .attr("transform", "translate(" + [margin.left, margin.top] + ")")
                    .style("font-size", Math.round(Math.min(svgHeight/15, 12))+"px")
                    .call(yAxis);
            })
			
		} catch (err) {
			if (err.stack) {
				alert(err.stack);
			} else {
				alert (err);
			}
		}
	}
    
    drawControls = function(tableName, knTable, jsDataTable) {
        if (_representation.enableSorting && _representation.enableClearSortButton) {
            jsDataTable.buttons().container().appendTo('#'+ tableName +'_wrapper .col-sm-6:eq(0)');
            $('#' + tableName + '_length').css({'display': 'inline-block', 'margin-right': '10px'});
            jsDataTable.on('order.dt', function () {
                var order = dataTable.order();
                jsDataTable.button(0).enable(order.length > 0);
            });
        }

        $('#' + tableName +'_paginate').css('display', 'none');

        $('#' + tableName + '_info').html(
            '<strong>Loading data</strong> - Displaying '
            + 1 + ' to ' + Math.min(knTable.getNumRows(), _representation.initialPageSize)
            + ' of ' + knTable.getNumRows() + ' entries.');

        if (knimeService) {
            if (_representation.enableSearching && !_representation.title) {
                knimeService.floatingHeader(false);
            }
            if (_representation.displayFullscreenButton) {
                knimeService.allowFullscreen();
            }
        }
    }
    
    drawDataPreviewTable = function() {
		try {
			previewTable = new kt();
			previewTable.setDataTable(_representation.dataPreview);
			
			var wrapper = $('<div id="tabs-knimePreviewContainer">').attr("class", "tab-pane active");
			content.append(wrapper);
			var table = $('<table id="knimePreview" class="table table-striped table-bordered" width="100%">');
			wrapper.append(table);
			
			var colArray = [];
			var colDefs = [];
            
            if (_representation.displayRowIds || true) {
				var title = _representation.displayRowIds ? 'Row ID' : '';
				var orderable = _representation.displayRowIds;
				colArray.push({
					'title': title, 
					'orderable': orderable,
					'className': 'no-break'
				});
			}
            
            for (var i = 0; i < previewTable.getColumnNames().length; i++) {
				var colType = previewTable.getColumnTypes()[i];
				var knimeColType = previewTable.getKnimeColumnTypes()[i];
				var colDef = {
					'title': previewTable.getColumnNames()[i],
					'orderable' : isColumnSortable(colType),
					'searchable': isColumnSearchable(colType)					
				}
				if (_representation.displayMissingValueAsQuestionMark) {
					colDef.defaultContent = '<span class="missing-value-cell">?</span>';
				}
				if (colType == 'number' && _representation.enableGlobalNumberFormat) {
					if (previewTable.getKnimeColumnTypes()[i].indexOf('double') > -1) {
						colDef.render = function(data, type, full, meta) {
							if (!$.isNumeric(data)) {
								return data;
							}
							return Number(data).toFixed(_representation.globalNumberFormatDecimals);
						}
					}
				}
				colArray.push(colDef);
			}
            
            var dataPreview = []
            for (var i = 0; i < previewTable.getRows().length; i++) {
                dataPreview.push(previewTable.getRow(i).data);
            }
            
            var firstChunk = getDataSlice(0, _representation.initialPageSize, previewTable);
            
            var searchEnabled = _representation.enableSearching || (knimeService && knimeService.isInteractivityAvailable());
            previewDataTable = $('#knimePreview').DataTable( {
                'columns': colArray,
				'columnDefs': colDefs,
				'order': order,
				'paging': true,
                'pageLength': pageLength,
				'lengthMenu': pageLengths,
				'lengthChange': _representation.enablePageSizeChange,
				'searching': searchEnabled,
				'ordering': _representation.enableSorting,
				'processing': true,
				'data': firstChunk,
				'buttons': buttons,
                'responsive': true//,
			});
            
            drawControls("knimePreview", previewTable, previewDataTable);
            
            //load all data
			setTimeout(function() {
				var initialChunkSize = 10;
				addDataToTable(_representation.initialPageSize, initialChunkSize, previewTable, previewDataTable, "knimePreview");
			}, 0);
            
        } catch (err) {
			if (err.stack) {
				alert(err.stack);
			} else {
				alert (err);
			}
		}
    }
	
	addDataToTable = function(startIndex, chunkSize, knTable, jsDataTable, tableName) {
		var startTime = new Date().getTime();
		var tableSize = knTable.getNumRows()
		var endIndex  = Math.min(tableSize, startIndex + chunkSize);
		var chunk = getDataSlice(startIndex, endIndex, knTable);
        //console.log("datarow",chunk)
		jsDataTable.rows.add(chunk);
		var endTime = new Date().getTime();
		var chunkDuration = endTime - startTime;
		var newChunkSize = chunkSize;
		if (startIndex + chunkSize < tableSize) {
			$('#' + tableName + '_info').html(
				'<strong>Loading data ('
				+ endIndex + ' of ' + tableSize + ' records)</strong> - Displaying '
				+ 1 + ' to ' + Math.min(tableSize, _representation.initialPageSize) 
				+ ' of ' + tableSize + ' entries.');
			if (chunkDuration > 300) {
				newChunkSize = Math.max(1, Math.floor(chunkSize / 2));
			} else if (chunkDuration < 100) {
				newChunkSize = chunkSize * 2;
			}
			setTimeout((function(i, c, t, js, tn) {
				return function() {
					addDataToTable(i, c, t, js, tn);
				};
			})(startIndex + chunkSize, newChunkSize, knTable, jsDataTable, tableName), chunkDuration);
		} else {
			$('#' + tableName + '_paginate').css('display', 'block');
			applyViewValue(jsDataTable);
			jsDataTable.draw();
            if (knTable.getNumRows == "numeric") {
                finishInit(jsDataTable);
            }
		}
	}
	
	getDataSlice = function(start, end, knTable) {
		if (typeof end == 'undefined') {
			end = knTable.getNumRows();
		}
		var data = [];
		for (var i = start; i < Math.min(end, knTable.getNumRows()); i++) {
			var row = knTable.getRows()[i];
			var dataRow = [];
            
			if (_representation.enableSelection) {
                switch (knTable.getTableId()) {
                    case "numeric":
                        dataRow.push(row.rowKey);
                        break;
                    case "nominal":
                        dataRow.push(row.rowKey);
                        break;
                    default: 
                        break;
                }
			}
            
//			if (_representation.displayRowIndex) {
//				dataRow.push(i);
//			}
            
			if (_representation.displayRowIds) {
				var string = '';
				if (_representation.displayRowIds) {
					string += '<span class="rowKey">' + row.rowKey + '</span>';
				}
				dataRow.push(string);
			}
			var dataRow = dataRow.concat(row.data);
            switch (knTable.getTableId()) {
                case "numeric":
                    dataRow.push(_representation.histograms[i]);
                    break;
                case "preview":
                    break;
                default: 
                    break;
            }
			data.push(dataRow);
		}
		return data;
	}
	
	applyViewValue = function(jsDataTable) {
		if (_representation.enableSearching && _value.filterString) {
			jsDataTable.search(_value.filterString);
		}
		if (_representation.enablePaging && _value.currentPage) {
			setTimeout(function() {
				jsDataTable.page(_value.currentPage).draw('page');
			}, 0);
		}
	}
	
	finishInit = function(jsDataTable) {
		allCheckboxes = jsDataTable.column(1).nodes().to$().find('input[type="checkbox"]');
		initialized = true;
	}
	
	selectAll = function(all) {
		// cannot select all rows before all data is loaded
		if (!initialized) {
			setTimeout(function() {
				selectAll(all);
			}, 500);
		}
		
		// Check/uncheck all checkboxes in the table
		selection = {};
		_value.selectAllIndeterminate = false;
		allCheckboxes.each(function() {
			this.checked = all;
			if ('indeterminate' in this && this.indeterminate) {
				this.indeterminate = false;
			}
			if (all) {
				selection[this.value] = true;
			}
		});
		_value.selectAll = all ? true : false;
		if (hideUnselected) {
			dataTable.draw();
		}
		//publishCurrentSelection();
	}
	
	isColumnSortable = function (colType) {
		var allowedTypes = ['boolean', 'string', 'number', 'dateTime'];
		return allowedTypes.indexOf(colType) >= 0;
	}
	
	isColumnSearchable = function (colType) {
		var allowedTypes = ['boolean', 'string', 'number', 'dateTime', 'undefined'];
		return allowedTypes.indexOf(colType) >= 0;
	}
	
	view.validate = function() {
	    return true;
	}
	
	view.getComponentValue = function() {
		if (!_value) {
			return null;
		}
		_value.selection = [];
		for (var id in selection) {
			if (selection[id]) {
				_value.selection.push(id);
			}
		}
		if (_value.selection.length == 0) {
			_value.selection = null;
		}
		var pageNumber = dataTable.page();
		if (pageNumber > 0) {
			_value.currentPage = pageNumber;
		}
		var pageSize = dataTable.page.len();
		if (pageSize != _representation.initialPageSize) {
			_value.pageSize = pageSize;
		}
		var searchString = dataTable.search();
		if (searchString.length) {
			_value.filterString = searchString;
		}
		var order = dataTable.order();
		if (order.length > 0) {
			_value.currentOrder = order;
		}
		if (_representation.enableColumnSearching) {
			_value.columnFilterStrings = [];
			var filtered = false;
			dataTable.columns().every(function (index) {
		        var input = $('input', this.footer());
		        if (input.length) {
		        	var filterString = input.val();
		        	_value.columnFilterStrings.push(filterString);
		        	filtered |= filterString.length;
		        } else {
		        	_value.columnFilterStrings.push("");
		        }
		    });
			if (!filtered) {
				_value.columnFilterStrings = null;
			}
		}
		hideUnselected = document.getElementById('showSelectedOnlyCheckbox');
		if (hideUnselected) {
			_value.hideUnselected = hideUnselected.checked;
		}
		return _value;
	}
	
	return view;
	
}();