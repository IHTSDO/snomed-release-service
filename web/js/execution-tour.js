// Instance the tour
var executionTour = new Tour({  debug: true, 
								orphan: true,
								backdrop: true,
								animation: false
							 });
executionTour.addSteps([
  {
	title: "Execution Tour",
	content: "Let's start building a SNOMED release!"
  },
  {
	element: "#tour-stop-1",
	title: "Snomed Release Service",
	content: "The Dashboard shows the Release Center's Extensions and Products.",
	placement: "right",
	backdrop: false
  },
    {
	element: ".tour-stop-1b",
	title: "Snomed Release Service",
	content: "The International Release Center includes the Spanish Edition.",
	placement: "right"
  },
  {
	element: "#tour-stop-2",
	title: "In-page Help Text",
	content: "Context specific help, to explain how the application functions.",
	placement: "left",
  },
  {
	element: ".tour-stop-3",
	title: "List of Products",
	content: "Extensions consist of Products.",
	placement: "right",

  },
  {
	element: "#tour-stop-4",
	title: "Starred Builds",
	content: "Quickly navigate to the builds you're currently working on.",
	placement: "left"
  },
  {
    //Going to show the whole page without a backdrop again, just before we move on.
	element: "#tour-stop-1",
	title: "Ready to move on?",
	content: "Next stop will be Product Builds.",
	placement: "right",
	backdrop: false,
	onNext: function () {	
							window.location.hash = "/international/snomed_ct_international_edition/snomed_ct_release/1_20140731_international_release_build";
							recommence(executionTour, 1000, true);
  						}	
  },
  {
	element: "#tour-stop-5",
	title: "Product Build Page",
	content: "Builds assemble files into distributable packages.",
	placement: "right",
	backdrop: false,
	onPrev: function () {	
							window.location.hash = "/";
							recommence(executionTour, 300, false);
						}	
  },
  {
	element: "#tour-stop-6",
	title: "Packages",
	content: "Packages contain SNOMED CT files.",
	placement: "right"
  },
  {
	element: "#tour-stop-7",
	title: "Execution History",
	content: "Here are the last few executions of the current build. The traffic lights show the status of each execution.",
	placement: "left"
  },
  {
	element: "#tour-stop-8",
	title: "Start an Execution",
	content: "Behold! The green button speaks for itself :-)",
	placement: "right"
  },
  {
	element: "#tour-stop-5",
	title: "Product Build - Review",
	content: "Next stop, the configuration.",
	placement: "right",
	backdrop: false,
	onNext: function () {	
						window.location.hash="/international/snomed_ct_international_edition/snomed_ct_release/1_20140731_international_release_build/2014-03-31T09:30:23/configuration";
						recommence(executionTour, 1000, true);
					}	
  },
  {
	element: "#tour-stop-9",
	title:  "Build Execution",
	content: "View the configuration before executing.",
	placement: "right",
	backdrop: false,
	onPrev: function() {	
							window.location.hash = "/international/snomed_ct_international_edition/snomed_ct_release/1_20140731_international_release_build";
							recommence(executionTour, 300, false);
						}
  },
  {
	element: "#tour-stop-10",
	title:  "Build Execution",
	content: "Add the build to the queue.",
	placement: "right",
	backdrop: false,
  },
  {
	element: "#tour-stop-9",
	title:  "Build Execution",
	content: "Next stop, the build output.",
	placement: "right",
	backdrop: false,
	onNext: function () {	
						window.location.hash="/international/snomed_ct_international_edition/snomed_ct_release/1_20140731_international_release_build/2014-03-31T13:08:12/output";
					}	
  },
  {
	element: "#tour-stop-9",
	title: "Build Output",
	content: "The release files are ready for review.",
	placement: "right",
	backdrop: false
  } ,
  {
	title: "Execution Tour End",
	content: "Thank you for watching!"
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

function recommence(tour, delay, isFwd){
	//Workaround for tour moving on to next step before the DOM is ready.
	//If it does that, we get the popup in the middle of the screen.
	//For now, we will end the tour, and the recommence 1s later at nextStep
	var currentStep = tour.getCurrentStep();
	var direction = isFwd? 1 : -1;
	
	//disable return to homepage
	tour.onEnd = function() {}
	tour.end();
	
	//re-enable 
	tour.onEnd = function() { window.location.hash="/"; }
	
	window.setTimeout ( function() {
		tour.restart();
		tour.goTo(currentStep + direction);
		} , delay );
}


