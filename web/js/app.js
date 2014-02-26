
App = Ember.Application.create({
	ApplicationController: Ember.Controller.extend({
		routeChanged: function() {
			// Scroll top top of new page
			window.scrollTo(0, 0);

			//Initialise popovers for all elements that include the relevant attribute
			//Needs to be repeated each time the DOM changes
			Ember.run.scheduleOnce('afterRender', this, afterRender);
		}.observes('currentPath')
	})
});

App.Router.map(function() {
	this.resource('pre-login');
	this.resource('release-centre', { path: '/:releaseCentre_id' }, function() {
		this.resource('rule-sets', function() {
//			this.resource('rule-set', { path: '/:ruleSet_id' });
			this.resource('rule-set', { path: '/international-edition-only' });
		});
		this.resource('extension', { path: '/:extension_id' }, function() {
			this.resource('product', { path: '/:product_id' }, function() {
				this.resource('build', { path: '/:build_id' }, function() {
					this.resource('package', { path: '/:package_id' }, function() {
						this.resource('build-input');
						this.resource('pre-conditions');
						this.resource('post-conditions');
						this.resource('build-trigger');
					});
					this.resource('pre-execution');
					this.resource('build-history');
					this.resource('execution', function() {
						this.route('results');
						this.route('debug');
						this.route('output');
						this.route('publish');
					});
				});
			});
		});
	});
	this.resource('admin', function() {
		this.resource('create-release-centre');
	});
});


App.AbstractRoute = Ember.Route.extend({
	init: function() {
		App.set('lastKnownRoute', this);
	}
});

App.AuthorisedRoute = App.AbstractRoute.extend({
	beforeModel: function() {
		// Close any open modals
		if ($('.modal').size() > 0) this.send('closeModal');

		//Redirect user to login page if no authorisation token is stored.
		if (sessionStorage.authorisationToken === undefined){
//			this.transitionTo('pre-login');
		}
	}
});

App.ApplicationRoute = Ember.Route.extend({
	actions: {
		removeEntity: function(model) {
			console.log('removeEntity', model);
			this.send('openModal', 'remove-entity', model);
		},
		openModal: function(modalName, params) {
			console.log('openModal');
			var controller = this.controllerFor(modalName);
			var model = controller.getModel(params);
			controller.set('model', model);
			return this.render(modalName, {
				into: 'application',
				outlet: 'modal',
				controller: controller
			});
		},
		modalInserted: function() {
			var $modal = $('.modal');
			var route = this;
			$modal.on('hidden.bs.modal', function(e) {
				route.disconnectOutlet({
					outlet: 'modal',
					parentView: 'application'
				});
			})
			$modal.modal('show');
		},
		closeModal: function() {
			$('.modal').modal('hide');
		}
	}
});

//Pre-Login
App.PreLoginRoute = App.AbstractRoute.extend();

// Index
App.IndexRoute = App.AuthorisedRoute.extend({
	model: function() {
		return {
			releaseCentres: this.store.filter('centre', {}, function(centre) {
				return !centre.get('removed');
			})
		}
	},
	actions: {
		addProduct: function(extension) {
			this.send('openModal', 'create-product', extension);
		}
	}
})

// ReleaseCentre
App.ReleaseCentreRoute = App.AuthorisedRoute.extend({
	model: function(params) {
		return this.store.find('centre', params.releaseCentre_id);
	}
})
App.ReleaseCentreIndexRoute = App.AuthorisedRoute.extend({
	model: function() {
		return this.modelFor('release-centre');
	}
})
App.CreateReleaseCentreView = Ember.View.extend({
	templateName: 'admin/create-release-centre',
	didInsertElement: function() {
		this.controller.send('modalInserted');
	}
})
App.CreateReleaseCentreController = Ember.ObjectController.extend({
	getModel: function() {
		return this.store.createRecord('centre');
	},
	actions: {
		submit: function() {
			var model = this.get('model');
			model.save();
			this.send('closeModal');
		}
	}
})

// Rule Set
App.RuleSetsRoute = App.AuthorisedRoute.extend({
	model: function() {
		return this.modelFor('release-centre');
	}
})
App.RuleSetsIndexRoute = App.AuthorisedRoute.extend({
	model: function() {
		return this.modelFor('rule-sets');
	}
})
App.RuleSetRoute = App.AuthorisedRoute.extend({
	model: function() {
		return this.modelFor('rule-sets');
	}
})

// Extension
App.ExtensionRoute = App.AuthorisedRoute.extend({
	model: function(params) {
		var centre = this.modelFor('release-centre');
		return centre.get('extensions').then(function(extensions) {
			var extension = extensions.findBy('id', params.extension_id);
			extension.set('parent', centre);
			return  extension;
		});
	}
})
App.ExtensionIndexRoute = App.AuthorisedRoute.extend({
	model: function() {
		return this.modelFor('extension');
	}
})

// Product
App.ProductRoute = App.AuthorisedRoute.extend({
	model: function(params) {
		var extension = this.modelFor('extension');
		return extension.get('products').then(function(products) {
			var product = products.findBy('id', params.product_id);
			product.set('parent', extension);
			return product;
		});
	}
})
App.ProductIndexRoute = App.AuthorisedRoute.extend({
	model: function() {
		return this.modelFor('product');
	}
})
App.ProductIndexController = Ember.ObjectController.extend({
	selectedBuild: function() {
		var controller = this;
		this.get('model.builds').then(function(builds) {
			var lastBuild = null;
			builds.forEach(function(build) {
				lastBuild = build;
			})
			controller.set('selectedBuild', lastBuild);
		});
	}.property('selectedBuild')
})
App.CreateProductView = Ember.View.extend({
	templateName: 'product-maintenance',
	didInsertElement: function() {
		this.controller.send('modalInserted');
	}
})
App.CreateProductController = Ember.ObjectController.extend({
	getModel: function(extension) {
		var product = this.store.createRecord('product', {
			parent: extension
		});
		return  product;
	},
	actions: {
		submit: function() {
			var product = this.get('model');
			var extension = product.get('parent');
			extension.get('products').pushObject(product);
			product.save();
			this.send('closeModal');
		}
	}
})

// Build
App.BuildIndexController = Ember.ObjectController.extend({
	actions: {
		initiateBuild: function (selectedBuild) {
			this.transitionToRoute('pre-execution', selectedBuild);
		}
	}
})

App.BuildRoute = App.AuthorisedRoute.extend({
	model: function(params) {
		var product = this.modelFor('product');
		return product.get('builds').then(function(builds) {
			var build = builds.findBy('id', params.build_id);
			build.set('parent', product);
			return build;
		});
	}
})

App.BuildIndexRoute = App.AuthorisedRoute.extend({
	model: function() {
		return this.modelFor('build');
	}
})

App.PreExecutionRoute = App.AuthorisedRoute.extend({
	model: function(params) {
		return this.modelFor('build');
	}
})
App.PreExecutionController = Ember.ObjectController.extend({
	actions: {
		runBuild: function (build) {
			var url = 'api/v1/builds/' + build.id + '/run'
			$.ajax({
				url: url,
				type: 'POST'
			})
		}
	}
});

// Package
App.PackageRoute = App.AuthorisedRoute.extend({
	model: function(params) {
		var build = this.modelFor('build');
		return build.get('packages').then(function(packages) {
			var thePackage = packages.findBy('id', params.package_id);
			thePackage.set('parent', build);
			return thePackage;
		});
	}
})

App.BuildInputRoute = App.AuthorisedRoute.extend({
	model: function(params) {
		return this.modelFor('package');
	},
	setupController: function(controller, model) {
		controller.set('model', model);
		controller.set('inputfiles', model.get('inputfiles'));
	}
})
App.BuildInputController = Ember.ObjectController.extend({
	actions: {
		reload: function() {
			var inputfiles = this.get('inputfiles');
			inputfiles.reloadLinks();
		}
	}
})
App.PostConditionsRoute = App.AuthorisedRoute.extend({
	model: function(params) {
		return this.modelFor('package');
	}
})


App.ExecutionRoute = App.AuthorisedRoute.extend({
	model: function() {
		var build = this.modelFor('build');
		var pack = { name: '17 Feb, 2014 01:05:00 (UTC)', parent: build };
		return pack;
	}
})

App.ExecutionIndexRoute = App.AuthorisedRoute.extend({
	beforeModel: function() {
		this.transitionTo('execution.results');
	}
})

App.RemoveEntityView = Ember.View.extend({
	templateName: 'remove-entity',
	didInsertElement: function() {
		this.controller.send('modalInserted');
	}
})
App.RemoveEntityController = Ember.ObjectController.extend({
	getModel: function(entity) {
		return entity;
	},
	actions: {
		submit: function() {
			var model = this.get('model');
			model.set('removed', true);
			model.save();
			this.send('closeModal');
		}
	}
})

function signinCallback(authResult) {
	if (authResult['status']['signed_in']) {
		//Store the token in session storage.  Note we can't store against Ember
		//App or it will be lost on page reload.
		sessionStorage.authorisationToken = authResult['access_token'];
		
		//And return the user to whatever page there were on when they got booted here
		var currentRoute = App.get('lastKnownRoute');
		currentRoute.transitionTo('index');
	} else {
		debug('Sign-in state: ' + authResult['error']);
		sessionStorage.authorisationToken = "Athentication Failed in Client";
	}
}

function afterRender() {
	$("[data-toggle='popover']").popover();
	$("[data-toggle='tooltip']").tooltip();
	$("[data-toggle='dropdown']").dropdown();
	initBuildInputFileUploadForm();
}

function initBuildInputFileUploadForm() {
	var $button = $('.panel-build-input form .btn');
	$('.panel-build-input form').submit(function () {
		var $form = $(this);
		if ($form.valid()) {
			var actionPath = $('.actionpath', $form).text()
			var shortName = $('input[name="shortName"]', $form).val();
			var action = actionPath + shortName;
			console.log('Upload url = "' + action + '"');
			$form.attr('action', action);
			$button.val('Uploading...');
			$button.prop('disabled', true);
			return true;
		} else {
			return false;
		}
	});
	$('.panel-build-input #buildInputFileUploadIframe').load(function() {
		$button.val('Upload');
		$button.prop('disabled', false);
		$('.panel-build-input form')[0].reset();
		$('.panel-build-input .reloadmodel').click();
	});
	effectPulse($('.build-history .traffic-light-in-progress'));
}

function effectPulse($selection) {
	window.setInterval(function() {

	}, 1000)
	if ($selection && $selection.size() > 0) {
		$selection.animate({
			opacity: 0
		}, 500, function() {
			$selection.animate({
				opacity: 1
			}, 500, function() {
				effectPulse($selection);
			})
		})
	}
}
