function createChild() {
    $.ajax({
	    url: 'createchild', 
	    type : 'POST',
	    success: function(data) {
		console.log("Child Creation: Success");
	    },
	    error: function() {
                console.log("Child Creation: Error");
	    },
	    complete: function() {
	    }
	});
};
