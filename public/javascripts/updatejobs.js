(function updateJobs() {
    $.ajax({
	    url: 'jobs', 
	    type : 'GET',
	    dataType : 'json',
	    success: function(data) {
		console.log("AJAX Success");
		$("#job_table .row").not(".header-row").remove();
		dataByPointID = _.sortBy(data, function(point){ return point.pointID; });
		_.map(dataByPointID, function(point){
			var $div = $("<div>").addClass("row").attr("id",point.pointID);
			$div.append($("<span>").addClass("cell").attr("data-label","Point ID").text(point.pointID));
			$div.append($("<span>").addClass("cell").attr("data-label","Current Tool").text(point.currentTool));
			$div.append($("<span>").addClass("cell").attr("data-label","Elapsed Time").text(point.elapsedTime));
			$div.append($("<span>").addClass("cell").attr("data-label","Node ID").text(point.nodeID));
			$div.append($("<span>").addClass("cell").attr("data-label","Child PID").text(point.childPID));
			$("#job_table").append($div);
		    });
	    },
	    error: function() {
                console.log("AJAX Error");
	    },
	    complete: function() {
		setTimeout(updateJobs, 500);
	    }
	});
})();
