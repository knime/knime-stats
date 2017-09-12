dataExplorerNamespace = function() {
	
	var view = {};
	var _representation, _value;
	var knimeTable = null;
    var previewTable = null;
    var previewDataTable = null;
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
    var margin = {top:0.3*svgHeight, left: 0.3*svgWidth, bottom: 0.3*svgHeight};
    var content;
	
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

                $('<li class="active"><a href="#tabs-knimeDataExplorerContainer" data-toggle="tab" aria-expanded="true">' + 'Statistics' + '</a></li>').appendTo(listOfTabNames);
				drawTable();
                
                $('<li class=""><a href="#tabs-knimePreviewContainer" data-toggle="tab" aria-expanded="false">' + 'Data Preview' + '</a></li>').appendTo(listOfTabNames);
                drawDataPreviewTable();
                
//                $("#tabs").tabs( {
//                    "activate": function(event, ui) {
//                        $( $.fn.dataTable.tables( true ) ).DataTable().columns.adjust();
//                    }
//                } );
//                
//                $('table.display').dataTable( {
//                    "scrollY": "200px",
//                    "scrollCollapse": true,
//                    "paging": false,
//                    "jQueryUI": true
//                } );
                $('a[data-toggle="tab"]').on( 'shown.bs.tab', function (e) {
                    $.fn.dataTable.tables( {visible: true, api: true} ).columns.adjust();
                } );
                
//                $('table.table').DataTable( {
//                    //ajax:           '../ajax/data/arrays.txt',
//                    scrollY:        "200px",
//                    scrollCollapse: true,
//                    paging:         false, 
//                    jQueryUI:       true, 
//                    "aoColumnDefs": [
//                        { "sWidth": "10%", "aTargets": [ -1 ] }
//                    ]
//                } );
			});
		}
        
//        $('div.hist:hidden').attr("width", svgWbig)
//        dataTable.on( 'responsive-display', function ( e, datatable, row, showHide, update ) {
//            console.log( 'Details for row '+row.index()+' '+(showHide ? 'shown' : 'hidden') );
//        });
        
	}
	
	drawTable = function() {
		//var body = $('body');
		if (_representation.enableSelection && _value.selection) {
			for (var i = 0; i < _value.selection.length; i++) {
				selection[_value.selection[i]] = true;
			}
		}
		try {
			knimeTable = new kt();
			knimeTable.setDataTable(_representation.statistics);
            //console.log("histogra data: ",_representation.histograms)
            //var tabContent = $('<div />').attr("class", "tab-pane active").attr('id', 'tabContent-stats').appendTo(content);
            
			var wrapper = $('<div id="tabs-knimeDataExplorerContainer">').attr("class", "tab-pane active");
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
			if (_representation.enableSelection) {
				var all = _value.selectAll;
				colArray.push({'title': /*'<input name="select_all" value="1" id="checkbox-select-all" type="checkbox"' + (all ? ' checked' : '')  + ' />'*/ 'Exclude'})
				colDefs.push({
					'targets': 0,
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
							+' value="' + $('<div/>').text(data).html() + '">';
					}
				});
			}
			
			if (_representation.displayRowIds || true) {
				var title = _representation.displayRowIds ? 'Column' : '';
				var orderable = _representation.displayRowIds;
				colArray.push({
					'title': title, 
					'orderable': orderable,
					'className': 'no-break'
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
            barsScale = d3.scale.linear();
                
//                var colorScale = d3.scale.category10()
//                .domain([0, knimeTable.getNumRows]);
            
            var colDef = {
                'title' :"Histogram",
                'orderable': false, 
                'searchable': false,
                'defaultContent':  '<span class="missing-value-cell">?</span>'
            }

            colDef.render = function(data, type, full, meta) {
                //console.log("data", data)
                svgHeight = svgHbig;
                svgWidth = svgWbig;
                var min = data.bins[0].def.first;
                var max = data.bins[data.bins.length-1].def.second
                xScale.range([0, svgWidth-margin.left])
                    .domain([min, max]);
                yScale.range([svgHeight - margin.top - 2*margin.bottom, 0])
                    .domain([0, data.maxCount]);
                //var fill = colorScale(data.colIndex);
                var histDiv = document.createElement("div");

                var svg = d3.select(histDiv).attr("class", "hist")
                    .append("svg")
                    .attr("height", svgHeight)
                    .attr("width", svgWidth)
                    .attr("class", "svg_hist")
                    .attr("id", "svg"+data.colIndex);
                
                var bar_group = svg.append("g")
                    .attr("transform", "translate(" + [margin.left , margin.top] + ")")
                    .attr("class", "bars")
                    .attr("id", "id"+data.colIndex);
                
                var barWidth = xScale(data.bins[0].def.second - data.bins[0].def.first + min)
                
                var bars = bar_group.selectAll("rect")
                    .data(data.bins)
                        .enter()
                    .append("rect")
                    .attr("class", "rect"+data.colIndex)
                    .attr("x", function (d) {return xScale(d.def.first);})
                    .attr("y", function(d) {return yScale(d.count);})
                    .attr("width", barWidth)
                    .attr("height", function(d){return svgHeight - margin.top - 2*margin.bottom - yScale(d.count);})
                    .attr("fill", "purple")
                    .attr("stroke", "#999999")
                    .attr("stroke-width", "1px")
                    .append("title")
                    .text(function(d, i) { return d.tooltip.slice(0,-13); });
                
                var text_group = svg.append("g")
                    .attr("class", "caption")
                    .attr("transform", "translate(" + [margin.left , margin.top] + ")")
                    .attr("id", "id"+data.colIndex);
                
                var texts = text_group.selectAll("text")
                    .data(data.bins)
                    .enter()
                    .append("text")
                    .attr("x", function (d) {return xScale(d.def.first) + barWidth/2;})
                    .attr("y", function(d) {return yScale(d.count) - 1;})
                    .text(function(d) {return d.count;})
                    .attr("font-size", Math.round(Math.min(svgHeight/15, 12))+"px")
                    .attr("text-anchor", "middle");
                
                var ticks = [];
                data.bins.forEach(function(d,i) {
                    ticks.push(d.def.first.toFixed(2));
                })
                
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
                    .attr("transform", "translate(" + [margin.left, svgHeight - margin.bottom - margin.top] + ")")
                    .style("font-size", Math.round(Math.min(svgHeight/15, 12))+"px")
                    .call(xAxis);
                
                var axisY = svg.append("g")
                    .attr("class", "y axis")
                    .attr("id", "yAxis"+data.colIndex)
                    .attr("transform", "translate(" + [margin.left, margin.top] + ")")
                    .style("font-size", Math.round(Math.min(svgHeight/15, 12))+"px")
                    .call(yAxis);
                
                return $('<div/>').append(histDiv).html();
            }
            colArray.push(colDef);    
            
			var pageLength = _representation.initialPageSize;
			if (_value.pageSize) {
				pageLength = _value.pageSize;
			}
			var pageLengths = _representation.allowedPageSizes;
			if (_representation.pageSizeShowAll) {
				var first = pageLengths.slice(0);
				first.push(-1);
				var second = pageLengths.slice(0);
				second.push("All");
				pageLengths = [first, second];
			}
			var order = [];
			if (_value.currentOrder) {
				order = _value.currentOrder;
			}
			var buttons = [];
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
			var firstChunk = getDataSlice(0, _representation.initialPageSize);
            //console.log("firstChunk",firstChunk)
            
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
				'fnDrawCallback': function() {
					if (!_representation.displayColumnHeaders) {
						$("#knimeDataExplorer thead").remove();
				  	}
					if (searchEnabled && !_representation.enableSearching) {
						$('#knimeDataExplorer_filter').remove();
					}
				}
			});
            

			
			//Clear sorting button placement and enable/disable on order change
			if (_representation.enableSorting && _representation.enableClearSortButton) {
				dataTable.buttons().container().appendTo('#knimeDataExplorer_wrapper .col-sm-6:eq(0)');
				$('#knimeDataExplorer_length').css({'display': 'inline-block', 'margin-right': '10px'});
				dataTable.on('order.dt', function () {
					var order = dataTable.order();
					dataTable.button(0).enable(order.length > 0);
				});
			}
			
			$('#knimeDataExplorer_paginate').css('display', 'none');

			$('#knimeDataExplorer_info').html(
				'<strong>Loading data</strong> - Displaying '
				+ 1 + ' to ' + Math.min(knimeTable.getNumRows(), _representation.initialPageSize)
				+ ' of ' + knimeTable.getNumRows() + ' entries.');
			
			if (knimeService) {
				if (_representation.enableSearching && !_representation.title) {
					knimeService.floatingHeader(false);
				}
				if (_representation.displayFullscreenButton) {
					knimeService.allowFullscreen();
				}
			}
			
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
				var initialChunkSize = 100;
				addDataToTable(_representation.initialPageSize, initialChunkSize);
			}, 0);
            
            setTimeout(function() {
                dataTable.rows()[0].forEach(function(d,i){
                    var smallHist = document.getElementById("svg"+i);
                    svgWidth = svgWsmall;
                    svgHeight = svgHsmall;
                    var data = dataTable.column(dataTable.columns()[0].length - 1).data()[i];
                    xScale.range([0, svgWidth])
                        .domain([0, data.bins[data.bins.length-1].def.second - data.bins[0].def.first]);
                    yScale.range([svgHeight, 0])
                        .domain([0, data.maxCount]);
                    d3.select(smallHist).attr("width", svgWidth).attr("height", svgHeight);
                    d3.select(smallHist).selectAll(".axis").remove()
                    var bar_group = d3.select(smallHist).selectAll(".bars").attr("transform", "translate(0,0)");
                    var bars = d3.select(smallHist).selectAll(".bars").selectAll(".rect"+i)
                        .data(data.bins)
                        .attr("x", function (d) {return xScale(d.def.first - data.bins[0].def.first);})
                        .attr("y", function(d) {return yScale(d.count);})
                        .attr("width", function(d) {return xScale(d.def.second - d.def.first);})
                        .attr("height", function(d){return svgHeight- yScale(d.count);})
                    })
            }, 0)
			
		} catch (err) {
			if (err.stack) {
				alert(err.stack);
			} else {
				alert (err);
			}
		}
	}
    
    drawDataPreviewTable = function() {
        //var body = $('body');
		if (_representation.enableSelection && _value.selection) {
			for (var i = 0; i < _value.selection.length; i++) {
				selection[_value.selection[i]] = true;
			}
		}
		try {
			previewTable = new kt();
			previewTable.setDataTable(_representation.dataPreview);
            
            //var tabContent = $('<div />').attr("class", "tab-pane").attr('id', 'tabContent-preview').appendTo(content);
            //console.log("histogra data: ",_representation.histograms)
			
			var wrapper = $('<div id="tabs-knimePreviewContainer">').attr("class", "tab-pane");
			content.append(wrapper);
			var table = $('<table id="knimePreview" class="table table-striped table-bordered" width="100%">');
			wrapper.append(table);
			
			var colArray = [];
			var colDefs = [];
            
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
            
            var pageLength = _representation.initialPageSize;
			if (_value.pageSize) {
				pageLength = _value.pageSize;
			}
			var pageLengths = _representation.allowedPageSizes;
			if (_representation.pageSizeShowAll) {
				var first = pageLengths.slice(0);
				first.push(-1);
				var second = pageLengths.slice(0);
				second.push("All");
				pageLengths = [first, second];
			}
            
            var dataPreview = []
            for (var i = 0; i < previewTable.getRows().length; i++) {
                dataPreview.push(previewTable.getRow(i).data);
            }
            
            previewDataTable = $('#knimePreview').DataTable( {
                'columns': colArray,
				'columnDefs': colDefs,
				//'order': order,
                //'retrieve': true,
				'paging': false,
				'pageLength': dataPreview.length,
				//'lengthMenu': pageLengths,
				//'lengthChange': _representation.enablePageSizeChange,
				'searching': false,
				//'ordering': _representation.enableSorting,
				'processing': true,
				//'deferRender': !_representation.enableSelection,
				'data': dataPreview,
				//'buttons': buttons,
                'responsive': true//,
//				'fnDrawCallback': function() {
//					if (!_representation.displayColumnHeaders) {
//						$("#knimeDataExplorer thead").remove();
//				  	}
//					if (searchEnabled && !_representation.enableSearching) {
//						$('#knimeDataExplorer_filter').remove();
//					}
//				}
			});
            
            //load all data
//			setTimeout(function() {
//				var initialChunkSize = 100;
//				addDataToTable(_representation.initialPageSize, initialChunkSize);
//			}, 0);
            
        } catch (err) {
			if (err.stack) {
				alert(err.stack);
			} else {
				alert (err);
			}
		}
    }
	
	addDataToTable = function(startIndex, chunkSize) {
		var startTime = new Date().getTime();
		var tableSize = knimeTable.getNumRows()
		var endIndex  = Math.min(tableSize, startIndex + chunkSize);
		var chunk = getDataSlice(startIndex, endIndex);
        //console.log("datarow",chunk)
		dataTable.rows.add(chunk);
		var endTime = new Date().getTime();
		var chunkDuration = endTime - startTime;
		var newChunkSize = chunkSize;
		if (startIndex + chunkSize < tableSize) {
			$('#knimeDataExplorer_info').html(
				'<strong>Loading data ('
				+ endIndex + ' of ' + tableSize + ' records)</strong> - Displaying '
				+ 1 + ' to ' + Math.min(tableSize, _representation.initialPageSize) 
				+ ' of ' + tableSize + ' entries.');
			if (chunkDuration > 300) {
				newChunkSize = Math.max(1, Math.floor(chunkSize / 2));
			} else if (chunkDuration < 100) {
				newChunkSize = chunkSize * 2;
			}
			setTimeout((function(i, c) {
				return function() {
					addDataToTable(i, c);
				};
			})(startIndex + chunkSize, newChunkSize), chunkDuration);
		} else {
			$('#knimeDataExplorer_paginate').css('display', 'block');
			applyViewValue();
			dataTable.draw();
			finishInit();
		}
	}
	
	getDataSlice = function(start, end) {
		if (typeof end == 'undefined') {
			end = knimeTable.getNumRows();
		}
		var data = [];
		for (var i = start; i < Math.min(end, knimeTable.getNumRows()); i++) {
			var row = knimeTable.getRows()[i];
			var dataRow = [];
			if (_representation.enableSelection) {
				dataRow.push(row.rowKey);
			}
			if (_representation.displayRowIndex) {
				dataRow.push(i);
			}
			if (_representation.displayRowIds) {
				var string = '';
				if (_representation.displayRowIds) {
					string += '<span class="rowKey">' + row.rowKey + '</span>';
				}
				dataRow.push(string);
			}
			var dataRow = dataRow.concat(row.data);
            dataRow.push(_representation.histograms[i]);
			data.push(dataRow);
		}
		return data;
	}
	
	applyViewValue = function() {
		if (_representation.enableSearching && _value.filterString) {
			dataTable.search(_value.filterString);
		}
		if (_representation.enablePaging && _value.currentPage) {
			setTimeout(function() {
				dataTable.page(_value.currentPage).draw('page');
			}, 0);
		}
	}
	
	finishInit = function() {
		allCheckboxes = dataTable.column(0).nodes().to$().find('input[type="checkbox"]');
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
		var hideUnselected = document.getElementById('showSelectedOnlyCheckbox');
		if (hideUnselected) {
			_value.hideUnselected = hideUnselected.checked;
		}
		return _value;
	}
	
	return view;
	
}();