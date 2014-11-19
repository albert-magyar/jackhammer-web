$(function(){
	$(".point_select").click(function(e){
		var $e = $(this)
		var pid = $e.attr("id");
		e.preventDefault();
		console.log("Clicked point " + pid);
		$("#params_table .row").not(".header-row").remove();
		$("#params_table").append(
					  $("<div>").addClass("row").append(
									    $("<span>").addClass("cell primary").attr("data-label","Parameter").text("test"),
									    $("<span>").addClass("cell").attr("data-label","Parameter").text("test")
									    )
					  );

	    });
  return false;    
});