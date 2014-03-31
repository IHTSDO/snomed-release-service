// Instance the tour
var executionTour = new Tour({  debug: true, 
								orphan: true,
								backdrop: true,
								animation: false });
executionTour.addSteps([
  {
	element: "#tour-stop-1",
	title: "Snomed Release Service",
	content: "Content of first step",
	placement: "right",
	backdrop: false
  },
  {
	element: "#tour-stop-2",
	title: "In-page help text",
	content: "Content of second step",
	placement: "left",

  },
  {
	element: ".tour-stop-3",
	title: "List of Products",
	content: "Content of third step",
	placement: "right",

  },
  {
	element: "#tour-stop-4",
	title: "Starred Builds",
	content: "Content of fourth step",
	placement: "left"
  },
  {
    //Going to show the whole page without a backdrop again, just before we move on.
	element: "#tour-stop-1",
	title: "Ready to move on",
	content: "Page overview before moving on to product build screen as if we'd clicked on Starred Build",
	placement: "right",
	backdrop: false,
	onNext: function () {	
							window.location.hash = "/international/snomed_ct_international_edition/snomed_ct_release/1_20140731_international_release_build";
							recommence();
  						}	
  },
  {
	element: "#tour-stop-5",
	title: "Product Build Screen",
	content: "Content of fifth step",
	placement: "right",
	backdrop: false
  },
  {
	element: "#tour-stop-6",
	title: "Packages",
	content: "Content of 6th step",
	placement: "right"
  },
  {
	element: "#tour-stop-7",
	title: "Execution History",
	content: "Content of 7th step",
	placement: "left"
  },
  {
	element: "#tour-stop-8",
	title: "Start an Execution",
	content: "Content of 8th step",
	placement: "right"
  },
  {
	element: "#tour-stop-5",
	title: "Product Build Screen",
	content: "Screen summary before moving on as if we'd clicked 'Create Execution and Review'",
	placement: "right",
	backdrop: false,
	onNext: function () {	
						window.location.hash="/international/snomed_ct_international_edition/snomed_ct_release/1_20140731_international_release_build/2014-03-31T09:30:23/configuration";
						recommence();
					}	
  },
   {
	element: "#tour-stop-9",
	title:  "Build Execution Screen",
	content: "Screen summary as if we'd clicked 'Create Execution and Review'",
	placement: "right",
	backdrop: false
  },
     {
	element: "#tour-stop-10",
	title:  "Build Execution Screen",
	content: "This button will add the current execution into a processing queue, at which point the status will update to 'Queued'",
	placement: "right",
	backdrop: false
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
		} , 200 );
}

function recommence(){
	//Workaround for tour moving on to next step before the DOM is ready.
	//If it does that, we get the popup in the middle of the screen.
	//For now, we will end the tour, and the recommence 200ms later at nextStep
	var currentStep = executionTour.getCurrentStep();
	executionTour.end();
	window.setTimeout ( function() {
		executionTour.start(true);
		executionTour.goTo(currentStep + 1);
		} , 200 );
}


