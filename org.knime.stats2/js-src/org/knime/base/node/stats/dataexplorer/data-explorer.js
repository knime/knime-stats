dataExplorerNamespace = function() {
	
	var view = {};
	var _representation, _value;
	var knimeTable = null;
	var dataTable = null;
	var selection = {};
	var allCheckboxes = [];
	var initialized = false;
	
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
				drawTable();
			});
		}
	}
	
	drawTable = function() {
		var body = $('body');
		if (_representation.enableSelection && _value.selection) {
			for (var i = 0; i < _value.selection.length; i++) {
				selection[_value.selection[i]] = true;
			}
		}
		try {
			knimeTable = new kt();
			knimeTable.setDataTable(_representation.statistics);
            console.log("histogra data: ",_representation.histograms)
			
			var wrapper = $('<div id="knimeDataExplorerContainer">');
			body.append(wrapper);
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
			
            var svgWidth = 100,
                svgHeight = 30;
            
            var xScale = d3.scale.linear()
                .range([0, svgWidth]), 
                
                yScale = d3.scale.linear()
                .range([0,svgHeight]),
                
                colorScale = d3.scale.category10()
                .domain([0, knimeTable.getNumRows]);
            
            var colDef = {
                'title' :"Histogram",
                'orderable': false, 
                'searchable': false,
                'defaultContent':  '<span class="missing-value-cell">?</span>'
            }
            console.log("")
            colDef.render = function(data, type, full, meta) {
                console.log("data", data)
                //var circle=$('<div class="circle">Hi</div>');
                //var div = $('<div class=bar></div>')
                //return $('<div/>').append('<svg width="50" height="50"><circle cx="25" cy="25" r="25" fill="purple" /></svg>').html();
                xScale.domain([0, data.realMax-data.realMin]);
                yScale.domain([0, data.maxCount]);
                var fill = colorScale(data.colIndex);
                var corr = data.realMin;
                var histDiv = document.createElement("div");
                var svg = d3.select(histDiv)
                    .append("svg")
                    .attr("height", svgHeight)
                    .attr("width", svgWidth)
                    .selectAll("rect")
                    .data(data.bins)
                        .enter()
                    .append("rect")
                    .attr("x", function (d) {return xScale(d.def.first - corr);})
                    .attr("y", function(d) {return svgHeight - yScale(d.count);})
                    .attr("width", function(d) {return xScale(d.def.second - d.def.first);})
                    .attr("height", function(d){return yScale(d.count);})
                    .attr("fill", fill)
                    .attr("stroke", "#999999")
                    .attr("stroke-width", "1px")
                    .append("title")
                    .text(function(d, i) { return d.tooltip.slice(0,-13); });
                //return $('<div/>').append(div).html();
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
        console.log("datarow",chunk)
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