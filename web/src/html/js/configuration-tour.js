// Instance the tour
var configurationTour = new Tour({  debug: true, 
								orphan: true,
								backdrop: false,
								animation: false,
								placement: "right"
								 });
configurationTour.addSteps([
  {

	title: "Configuration Tour",
	content: "Welcome to the Configuration Tour!  We will start with an overview of this application's 'domain model' - how each of the elements within the application relate to each other - before proceeding to the configuration for a release.",
	backdrop: true
  },
  {
	element: "#tour-stop-1",
	title: "Snomed Release Service Homepage",
	content: "The Homepage/Dashboard Screen represents the top level of our domain hierarchy - A Release Center.",
	onNext: function () {	
							window.location.hash = "/international/";
							recommence(configurationTour, 300, true);
  						}	
  },
  {
	element: "#tour-stop-c2",
	title: "Extension",
	content: "Each Release Center 'owns' any number of Extensions.",
	onNext: function () {	
							window.location.hash = "/international/snomed_ct_international_edition";
							recommence(configurationTour, 300, true);
						},
	onPrev: function () {	
							window.location.hash = "/";
							recommence(configurationTour, 300, false);
						}	
  },
  {
	element: "#tour-stop-c3",
	title: "List of Products",
	content: "...Extensions own any number of Products...",
	onNext: function () {	
							window.location.hash = "/international/snomed_ct_international_edition/snomed_ct_release";
							recommence(configurationTour, 300, true);
						},
	onPrev: function () {	
							window.location.hash = "/international";
							recommence(configurationTour, 300, false);
						}	
  },
  {
	element: "#tour-stop-c4",
	title: "List of Builds",
	content: "...Products own a number of Builds...",
	onPrev: function () {	
							window.location.hash = "/international/snomed_ct_international_edition/";
							recommence(configurationTour, 300, false);
						}	
  },
   {
	element: "#tour-stop-c5",
	title: "List of Builds",
	content: "(just to mention in passing that we can use this screen to specify that build is of interest by click its star, making it a starred build appearing on the Home Page for quicker access.)",
	onNext: function () {	
							window.location.hash = "/international/snomed_ct_international_edition/snomed_ct_release/1_20140731_international_release_build";
							recommence(configurationTour, 300, true);
						}
  },
{
	element: "#tour-stop-c6",
	title: "List of Packages",
	content: "...Builds have a number of Packages (the actual content that gets published)...",
	onNext: function () {	
							window.location.hash = "/international/snomed_ct_international_edition/snomed_ct_release/1_20140731_international_release_build/package/snomed_release_package";
							recommence(configurationTour, 300, true);
						},
	onPrev: function () {	
							window.location.hash = "/international/snomed_ct_international_edition/snomed_ct_release";
							recommence(configurationTour, 300, false);
						}		
  },
  {
	element: "#tour-stop-c7",
	title: "Package Configuration",
	content: "...and each Package has a number of items of configuration associated with it.",
	onNext: function () {	
							window.location.hash = "/international/snomed_ct_international_edition/snomed_ct_release/1_20140731_international_release_build/package/snomed_release_package/build-input";
							recommence(configurationTour, 300, true);
						},
	onPrev: function () {	
							window.location.hash = "/international/snomed_ct_international_edition/snomed_ct_release/1_20140731_international_release_build";
							recommence(configurationTour, 300, false);
						}	
  },
  {
	element: "#tour-stop-c8",
	title: "Package Configuration - Input Files",
	placement: "left",
	content: "...such as the input files...",
	onNext: function () {	
							window.location.hash = "/international/snomed_ct_international_edition/snomed_ct_release/1_20140731_international_release_build/package/snomed_release_package/pre-conditions";
							recommence(configurationTour, 300, true);
						},
	onPrev: function () {	
							window.location.hash = "/international/snomed_ct_international_edition/snomed_ct_release/1_20140731_international_release_build/package/snomed_release_package";
							recommence(configurationTour, 300, false);
						}	
  },
{
	element: "#tour-stop-c9",
	title: "Package Configuration - Pre-Conditions",
	placement: "left",
	content: "...the Pre-condition checks (which are not actually configurable items, but are shown here as a list as they run automatically as part of a build execution and any one of them might fail)...",
	onNext: function () {	
							window.location.hash = "/international/snomed_ct_international_edition/snomed_ct_release/1_20140731_international_release_build/package/snomed_release_package/post-conditions";
							recommence(configurationTour, 300, true);
						},
	onPrev: function () {	
							window.location.hash = "/international/snomed_ct_international_edition/snomed_ct_release/1_20140731_international_release_build/package/snomed_release_package/build-input";
							recommence(configurationTour, 300, false);
						}		
  },
  {
	element: "#tour-stop-c10",
	title: "Package Configuration - Post Conditions",
	placement: "left",
	content: "...and the Post-condition checks, also known as the QA process previously performed by the 'Release Assertion Toolkit'.",
	onPrev: function () {	
							window.location.hash = "/international/snomed_ct_international_edition/snomed_ct_release/1_20140731_international_release_build/package/snomed_release_package/pre-conditions";
							recommence(configurationTour, 300, false);
						}	
  },
  {
	element: "#tour-stop-c11",
	title: "Post Conditions - Selecting Rule Sets",
	content: "Here the user can click any number of pre-defined sets of Quality Assurance rules to run against the results of the build execution."
  },
  {
	element: ".tour-stop-c12",
	title: "Maintain Rule Sets",
	content: "And this link allows the user to configure which rules should be grouped together into sets."
  },
  {
	title: "Configuration Tour End",
	content: "Thank you for watching!",
	backdrop: true
  } 
]);

function startConfigurationTour() {
	//Ensure user is at start of application and that this page has had a chance to 
	//load fully before starting the tour.
	window.location.hash = "";
	window.setTimeout( function() {
		configurationTour.init();
		//We might have held onto a tour from last time - end if so.
		configurationTour.end();
		configurationTour.restart();  //Always restart at beginning.
		console.log("configuration Tour underway!");
		} , 500 );
}




