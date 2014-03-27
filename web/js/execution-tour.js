// Instance the tour
var executionTour = new Tour({ debug: true, orphan: true });
executionTour.addSteps([
  {
	element: "#tour-stop-1",
	title: "Snomed Release Service",
	content: "Content of first step",
	placement: "bottom",
	backdrop: true
  },
  {
	element: "#tour-stop-2",
	title: "In-page help text",
	content: "Content of second step",
	placement: "left",
	backdrop: true
  },
  {
	element: ".tour-stop-3",
	title: "List of Products",
	content: "Content of third step",
	placement: "right",
	backdrop: true
  },
  {
	element: "#tour-stop-4",
	title: "Starred Builds",
	content: "Content of fourth step",
	placement: "left",
	backdrop: true
  }
]);

function startExecutionTour() {
	executionTour.init();
	executionTour.restart();
	console.log("Execution Tour underway!");
}


