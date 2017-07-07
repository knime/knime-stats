dataExplorerNamespace = function() {
	
	var view = {};
	var _representation, _value;
	
	view.init = function(representation, value) {
	    _representation = representation;
	    _value = value;
	    
	    alert(JSON.stringify(representation));
	}
	
	view.validate = function() {
	    return true;
	}
	
	view.getComponentValue = function() {
		return _value;
	}
	
	return view;
	
}();