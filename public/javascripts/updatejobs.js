(function updateJobs() {
    $.ajax({
	    url: 'jobs', 
	    type : 'GET',
	    dataType : 'json',
	    success: function(data) {
		console.log("AJAX Success");
	    },
	    error: function() {
                console.log("AJAX Error");
	    },
	    complete: function() {
		setTimeout(updateJobs, 5000);
	    }
	});
})();
