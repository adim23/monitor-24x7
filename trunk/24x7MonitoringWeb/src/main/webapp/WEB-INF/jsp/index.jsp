<div class="main_content">
	<script type="text/javascript" src="https://www.google.com/jsapi"></script>
	<script type="text/javascript">
		chart = null;
	
		function createList(ul, array) {
			$.each(array, function(index, item) {
			    var li = $(document.createElement('li'));
				li.attr("id", item.fullName);
				var $img = $(document.createElement('img'));
				var $isClass = true;
				$.each(item.subItems, function(i, value) {
					if(value.subItems.length > 0) {
						$isClass = false;
						return;
					}
				});
				 if(item.subItems.length == 0) {
					$img.attr('src', 'images/method.png');
				}
				else if(item.subItems[0].itemName.indexOf("()") >= 0) {
					$img.attr('src', 'images/class.png');
				}
				
				else {
					$img.attr('src', 'images/folder.png');
				}
				$img.attr('style', 'margin-right: 5px;');
				li.append($img);
				li.append('<a href="#"><span style="display:none">' + item.fullName + '</span>' + item.itemName + '</a>');
				if(item.subItems.length > 0 ) {
				    var ul2 = $('<ul>').appendTo(li);
					createList(ul2, item.subItems);
	 			}
				ul.append(li);
			});
			
		}
	
	 	function drawChart($tracersArray) {

	        var data = google.visualization.arrayToDataTable($tracersArray);
	
	        var options = {
	          title: 'Application Performance'
	        };
			if(chart == null) {
				chart = new google.visualization.LineChart(document.getElementById('statsChart'));
			}
			chart.draw(data, options);
	    }
	    
	    function findTracer($methodName, $tracers) {
	    	var $return = null;
	    	$.each($tracers, function (index, item) {
		    	if(item.methodName == $methodName) {
		    		$return = item;
		    		return false;
		    	}
			});
			return $return;
	    }
	    
	    function setDateTimePickerEvent() {
	    	$('#fromRange, #toRange').datetimepicker({
				controlType: 'select',
				timeFormat: 'hh:mm tt'
			});
	    	
	    	$("#customRangeSubmit").click(function() {
	    		
	    		var $fromRange = $("#fromRange").val();
       		    var $toRange = $("#toRange").val();
       		    
    		 	if(!$fromRange || !$toRange) {
    		 		alert("'From' and 'To' ranges missing");
    		 		return false;
    		 	}
    		 	
    		 	var height = $(".center_content").css("height");
				$("#ajax_box").css("height", height);
				$("#ajax_box").show();

				var $selectedTreeNode = $('#classTree').jstree('get_selected').attr('id');

   			 	var searchFilter = new Object();
			    searchFilter.fromRange = $fromRange;
			    searchFilter.toRange = $toRange;
			    retrievePerformanceNumbers(searchFilter, $selectedTreeNode);
	    		  
	    	});
	    }
		
	    function setDefaultTextEvent() {
	    	$(".defaultText").focus(function(arg) {
   		        if ($(this).val() == $(this)[0].title){
   		            $(this).removeClass("defaultTextActive");
   		            $(this).val("");
   		        }
   		    });
   		    
   		    $(".defaultText").blur(function(){
   		        if ($(this).val() == ""){
   		            $(this).addClass("defaultTextActive");
   		            $(this).val($(this)[0].title);
   		        }
   		    });
   		    
   		    $(".defaultText").blur(); 
	    	
	    }
	    
	    function setTimeRangeEventEvent() {
			$('#timeRangeInMins').bind('change', function(ev) {
				var $selectedTimeRange = $(this).val();
				if(!$selectedTimeRange) {
					$("#customRangeSelector").css("visibility", "visible");
					return false;
				}
				$("#customRangeSelector").css("visibility", "hidden");
				var height = $(".center_content").css("height");
				$("#ajax_box").css("height", height);
				$("#ajax_box").show();

				
				var $selectedResolution = $selectedTimeRange;
			    var $selectedTreeNode = $('#classTree').jstree('get_selected').attr('id');

			 	var searchFilter = new Object();
			    searchFilter.timeRangeInMins = $selectedTimeRange;
			    retrievePerformanceNumbers(searchFilter, $selectedTreeNode);
			});
		}
		
	    function setSearchEvent() {
			$("#searchButton").click(function() {
	    		
				var $keyword = $("#searchKeyword").val();
				if(!$keyword) {
					alert("Please search for a keyword");
					return false;
				}
				$("#classTree").jstree("search", $keyword);
				  
	    	});
	    }
	    
	    function handlePerformanceStatsRetrieval($tracedItem, $searchedItems, $updateChartOnly) {
	    	
	    	var height = $(".center_content").css("height");
			$("#ajax_box").css("height", height);
			$("#ajax_box").show();
			
			var $selectedTimeRange = $("#timeRangeInMins").val();
			var $fromRange = $("#fromRange").val();
			var $toRange = $("#toRange").val();
			
			var searchFilter = new Object();
		    searchFilter.timeRangeInMins = $selectedTimeRange;
		    searchFilter.fromRange = $fromRange;
		    searchFilter.toRange = $toRange;
		    searchFilter.searchedItems = $searchedItems;
		    retrievePerformanceNumbers(searchFilter, $tracedItem, $updateChartOnly);
	    	
	    }
	    
		function retrievePerformanceNumbers(searchFilter, methodName, $updateChartOnly) {
			$("#chart").attr('src', "about:blank");
			
			var $jsonString = JSON.stringify( searchFilter );
			if(methodName == null) {
				$("#ajax_box").hide();
				alert("please select a node from the tree!");
				return false;
			}
			if(!searchFilter.timeRangeInMins 
					&& (!searchFilter.fromRange || !searchFilter.toRange)) {
				$("#ajax_box").hide();
				alert("please select a valid range");
				return false;
			}
		    $.ajax(
		            {
		              url:"json/methodTracingInfo/" + methodName, 
		              type: "POST",  
		              contentType: "application/json; charset=utf-8",
		              data:  $jsonString,
		              complete: callback, 
		            } ); 
			function callback(jsonResponse) {
				updateDataSet(searchFilter, methodName, jsonResponse, $updateChartOnly);
			}

			
		}
		
		function updateDataSet(searchFilter, methodName, jsonResponse, $updateChartOnly) {
		    $("#ajax_box").hide();
		    
	  		var $jsonArray = jQuery.parseJSON(jsonResponse.responseText);
	  		
	  		if($jsonArray.tracersGrouped.length == 0 ) {
	  			$('#statsGrid').html("");
	  			$(".scrollbar").css("display", "none");
	  			return;
	  		}
	  		
	  		if(!$updateChartOnly) {
	  			var $groupedArray = [];
		  		
				$.each($jsonArray.tracersGrouped, function (index, value) {
					var $itemArray = [];
				    $itemArray.push(value.methodName);
				    $itemArray.push(parseFloat(value.average).toFixed(2));
				    $itemArray.push(value.max);
				    $itemArray.push(value.min);
				    $itemArray.push(value.count);
			    	$groupedArray.push($itemArray);
				});   
				$('#statsGrid')
				.TidyTable({
					enableCheckbox : true,
					enableMenu     : false
				},
				{
					columnTitles : ['Method Name','Response<br/>Time (ms)','Max','Min','Count'],
					columnValues : $groupedArray
			       
				});
				
				updateChartOnCheckBoxChange();
					
				$(".scrollbar").css("display", "block");
				$('#scrollbar1').tinyscrollbar();
	  		}
	  		
	  		var $chartForm = $('#chartForm');
	  		var $methodSignature = $("<input>").attr("type", "hidden").attr("name", "methodSignature").val(methodName);
	  		var $timeRangeInMins = $("<input>").attr("type", "hidden").attr("name", "timeRangeInMins").val(searchFilter.timeRangeInMins);
	  		var $fromRange = $("<input>").attr("type", "hidden").attr("name", "fromRange").val(searchFilter.fromRange);
	  		var $toRange = $("<input>").attr("type", "hidden").attr("name", "toRange").val(searchFilter.toRange);
	  		
	  		$chartForm.append($methodSignature);
	  		$chartForm.append($timeRangeInMins);
	  		$chartForm.append($fromRange);
	  		$chartForm.append($toRange);
	  		$chartForm.append($toRange);

		   
	    	if(searchFilter.searchedItems != null 
	    			&& searchFilter.searchedItems != undefined
	    			&& searchFilter.searchedItems.length > 0 ) {
	    		
	    		searchFilter.searchedItems.forEach(function (item) { 
	    	  		var $searchedItems = $("<input>").attr("type", "hidden").attr("name", "searchedItems").val(item);
	    	  		$chartForm.append($searchedItems);
			    });
	    	} else {
	    		var $searchedItems = $("<input>").attr("type", "hidden").attr("name", "searchedItems").val("");
    	  		$chartForm.append($searchedItems);
	    	}
	    	
	    	
	    	$chartForm.submit();
	    	$chartForm.html('');
		}
		
		function updateChartOnCheckBoxChange() {
			$('.tidy_table :checkbox').change(function() {
				var $checkedItems;
				if(this.value == "all") {
					$checkedItems = $('.tidy_table :checkbox').map(function () {
						  return this.value;
						}).get();
					$checkedItems.splice(0, 1);
				}
				else {
					$checkedItems = $('.tidy_table :checkbox:checked').map(function () {
						 return this.value;
						}).get();
				}
					 
				var $tracedItem = $checkedItems[0];
				$checkedItems.splice(0, 1); // remove first element.
				handlePerformanceStatsRetrieval($tracedItem, $checkedItems, true);
			});
			
		}
        $(document).ready(function () {
        		setTimeRangeEventEvent();
        		setDateTimePickerEvent();
        		setDefaultTextEvent();
        		setSearchEvent();
        		var height = $(".center_content").css("height");
				$("#ajax_box").css("height", height);
				$("#ajax_box").show();
				
				$.ajax
				(
					{
					  url:"json/getTracedMethods", 
					  type: "GET",  
					  data: "",
					  complete: function(jsonResponse) {
					  		$("#ajax_box").hide();
							var objectArray = jQuery.parseJSON(jsonResponse.responseText);
							var ul = $(document.createElement('ul'));
							createList(ul, objectArray);
							$("#classTree").html(ul);
							$("#classTree")
								.jstree({"search" : {"case_insensitive" : true}, "plugins" : ["themes","html_data","ui", "search"] })
								// 1) if using the UI plugin bind to select_node
								.bind("select_node.jstree", function (event, data) { 
									var $tracedItem = data.rslt.obj.attr("id");
									handlePerformanceStatsRetrieval($tracedItem);
	               
								})
								.bind("search.jstree", function (e, data) {
									$('#classTree').jstree('close_all');
									if(data.rslt.nodes.length == 0) {
										alert("No results found! please redefine your search..");
										return false;
									}
									var $searchedItems = [];
									$.each(data.rslt.nodes, function(index, item) {
										var $nodeName = item.parentElement.id;
										var $exists = false;
										$.each($searchedItems, function(i, value) {
											if ($nodeName.indexOf(value) >= 0) {
												$exists = true;
												return true; // exit for loop.
											}
										});
										if(!$exists) {
											$searchedItems.push($nodeName);
										}
									});
									var $tracedItem = $searchedItems[0];
									$searchedItems.splice(0, 1); // remove first element.
									handlePerformanceStatsRetrieval($tracedItem, $searchedItems);
								})
								// 2) if not using the UI plugin - the Anchor tags work as expected
								//    so if the anchor has a HREF attirbute - the page will be changed
								//    you can actually prevent the default, etc (normal jquery usage)
								.delegate("a", "click", function (event, data) { event.preventDefault(); 
							})
						
				           
						}
					} 
				);
	
		});
		
	</script>
	 <div id="ajax_box" class="ajax_box" style="display:none">
			<div class="ajax_box" style="background: url(images/ajax-loader.gif) no-repeat center center; "></div>
	 </div>
	 
	 <table class="main_table">
	 	<tr>
	 		<td width="25%">
	 			<div style="border: none;" id="classTree">
	               
	            </div>
	 		</td>
	 		<td width="75%">
	 			<div class="filter" style="height:50px; margin:10px'">
				 	<div class="floatL">
				 		time range: 
				 			<select id="timeRangeInMins" name="timeRangeInMins" style="background:#F0F0F0;border:1px solid #e8e8e8;margin:5px">
				 			  <option value="30">Last 30 minutes</option>
							  <option value="120">Last 2 hours</option>
							  <option value="360">Last 6 hours</option>
							  <option value="720">Last 12 hours</option>
							  <option value="1440">Last 24 hours</option>
							  <option value="10080">Last 7 days</option>
							  <option value="">Custom Range</option>
						    </select>
						    
						    <span id="customRangeSelector" style="visibility:hidden">
						    	<br/>
							    From: <input size=15 id="fromRange" /> 
							    To: <input size="15" id="toRange" />
							    <button id="customRangeSubmit">submit</button>
							</span>
					</div>
					<div class="floatL" style="margin-left:-100px">
					Resolution: 
				 			<select id="resolutionInSecs" name="resolutionInSecs" style="background:#F0F0F0;border:1px solid #e8e8e8" disabled="disabled">
							  <option value="30">30 secs</option>
							  <option value="120">2 minutes</option>
							  <option value="360">6 minutes</option>
							  <option value="720">12 minutes</option>
							  <option value="1440">24 minutes</option>
							  <option value="10080">3 hours</option>
							  <option value="">Custom</option>
						    </select>
					</div>
					<div class="floatL" style="margin-left:20px">
						<input size=50 id="searchKeyword" class="defaultText" title="search for class, method, SQL...."/>
						<button id="searchButton">search</button>
					</div>
				 </div>
	 			<div id="scrollbar1" >
					<div class="scrollbar" style="display:none"><div class="track"><div class="thumb"><div class="end"></div></div></div></div>
					<div class="viewport">
						 <div class="overview" id="statsGrid"  align="center">
						 	
						 </div>
					</div>
				</div>	
				<div style="height:20px">&nbsp</div>
	        	<div id="statsChart" style="width:99%; height:600px" >
					<iframe id="chart" style="border:none;" width="100%" height="550px" marginheight="0" marginwidth="0" frameborder="0">
					  <p>Your browser does not support iframes.</p>
					</iframe>
					<form id="chartForm" target="chart" action="getchart.do" method="post">

					</form>
	        	</div>
	 		</td>	
	 		
	 	</tr>

	 </table>
	
 </div>