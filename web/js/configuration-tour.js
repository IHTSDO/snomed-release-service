// Instance the tour
var configurationTour = new Tour({ debug: true, orphan: true });
configurationTour.addSteps([
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
	element: "#tour-stop-3",
	title: "In-page help text",
	content: "Content of second step",
	placement: "left",
	backdrop: true
  }
]);

function startConfigurationTour() {
	alert ("Configuration Tour is not yet available");
	//configurationTour.init();
	//configurationTour.restart();
	//console.log("Configuration Tour underway!");
}


