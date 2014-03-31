// Instance the tour
var executionTour = new Tour({  debug: true, 
								orphan: true,
								backdrop: true,
								animation: false });
executionTour.addSteps([
  {
	element: "#tour-stop-1",
	title: "Snomed Release Service",
	content: "The Homepage/Dashboard Screen shows the Extensions and Products 'owned by the default Release Center of the currently logged in user.",
	placement: "right",
	backdrop: false
  },
  {
	element: "#tour-stop-2",
	title: "In-page Help Text",
	content: "Most pages feature context specific help text to explain how the application functions.  The user will be able to hide this text, once they feel confident with the application.",
	placement: "left",

  },
  {
	element: ".tour-stop-3",
	title: "List of Products",
	content: "This panel shows the Products 'owned' by the current Extension.",
	placement: "right",

  },
  {
	element: "#tour-stop-4",
	title: "Starred Builds",
	content: "The user can select any number of builds they have a particular interest in, and these appear here as shortcut links either to the configuration (by clicking on the build name) or to the results of the last execution (by clicking on the traffic light icon). ",
	placement: "left"
  },
  {
    //Going to show the whole page without a backdrop again, just before we move on.
	element: "#tour-stop-1",
	title: "Ready to move on?",
	content: "We'll just show the entire screen here for a moment, before moving on to the ProductBbuild screen as if the user has clicked on a Starred Build.",
	placement: "right",
	backdrop: false,
	onNext: function () {	
							window.location.hash = "/international/snomed_ct_international_edition/snomed_ct_release/1_20140731_international_release_build";
							recommence(executionTour, 1000);
  						}	
  },
  {
	element: "#tour-stop-5",
	title: "Product Build Screen",
	content: "This screen shows the Packages containing the SNOMED content which IHTSDO (or hosted tenant) releases.",
	placement: "right",
	backdrop: false
  },
  {
	element: "#tour-stop-6",
	title: "Packages",
	content: "Here we see the Packages 'owned' by the current build.",
	placement: "right"
  },
  {
	element: "#tour-stop-7",
	title: "Execution History",
	content: "This panel displays the last few executions of the current build, and the traffic light icons give an indication of the status of each execution.",
	placement: "left"
  },
  {
	element: "#tour-stop-8",
	title: "Start an Execution",
	content: "As well as offering support for a nightly build, this panel allows an execution to be triggered manually - which we'd use when performing a 6-monthly release.",
	placement: "right"
  },
  {
	element: "#tour-stop-5",
	title: "Product Build Screen - Review",
	content: "We'll show the whole screen here again, before moving on to the Execution Summary as if we'd clicked on the 'Build Now...' button.",
	placement: "right",
	backdrop: false,
	onNext: function () {	
						window.location.hash="/international/snomed_ct_international_edition/snomed_ct_release/1_20140731_international_release_build/2014-03-31T09:30:23/configuration";
						recommence(executionTour, 1000);
					}	
  },
  {
	element: "#tour-stop-9",
	title:  "Build Execution Screen",
	content: "This screen lists the configuration that is about to be applied to the current execution.",
	placement: "right",
	backdrop: false,
  },
  {
	element: "#tour-stop-10",
	title:  "Build Execution Screen",
	content: "This button will add the current execution into a processing queue, at which point the status will update to 'Queued'.",
	placement: "right",
	backdrop: false,
  },
  {
	element: "#tour-stop-9",
	title:  "Build Execution Screen",
	content: "Ready to move on?  Clicking the 'Run Build' button now...",
	placement: "right",
	backdrop: false,
	onNext: function () {	
						window.location.hash="/international/snomed_ct_international_edition/snomed_ct_release/1_20140731_international_release_build/2014-03-31T13:08:12/output";
					}	
  },
  {
	element: "#tour-stop-9",
	title: "Execution Output",
	content: "Here we see the logs output by the build process - currently in a raw format, but this will change to a more user friendly summary listing in a subsequent iteration.",
	placement: "right",
	backdrop: false
  } ,
  {
	title: "Execution Tour End",
	content: "Thank you for watching. Returning to home page now...",
	backdrop: false,
	onHidden: function() {	
						window.location.hash="/";
					}	
  } 
]);

function startExecutionTour() {
	//Ensure user is at start of application and that this page has had a chance to 
	//load fully before starting the tour.
	window.location.hash = "";
	window.setTimeout( function() {
		executionTour.init();
		//We might have held onto a tour from last time - end if so.
		executionTour.end();
		executionTour.restart();  //Always restart at beginning.
		console.log("Execution Tour underway!");
		} , 500 );
}

function recommence(tour, delay){
	//Workaround for tour moving on to next step before the DOM is ready.
	//If it does that, we get the popup in the middle of the screen.
	//For now, we will end the tour, and the recommence 1s later at nextStep
	var currentStep = tour.getCurrentStep();
	tour.end();
	window.setTimeout ( function() {
		tour.restart();
		tour.goTo(currentStep + 1);
		} , delay );
}


