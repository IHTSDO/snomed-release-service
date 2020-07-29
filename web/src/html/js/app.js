
App = Ember.Application.create();

App.ApplicationController = Ember.ObjectController.extend({
	isAuthenticated: function() {
		var currentUser = this.get('currentUser');
		var b = true == currentUser.authenticated;
		return b;
	}.property('currentUser'),
	routeChanged: function() {
		// Scroll top top of new page
		window.scrollTo(0, 0);

		//Initialise popovers for all elements that include the relevant attribute
		//Needs to be repeated each time the DOM changes
		//Ember.run.scheduleOnce('afterRender', this, afterRender);
		Ember.run.later(window, afterRender, 1000);
	}.observes('currentPath')
});

App.Router.map(function() {
	this.resource('release-center', { path: '/:releaseCenter_id' }, function() {
		this.resource('rule-sets', function() {
//			this.resource('rule-set', { path: '/:ruleSet_id' });
			this.resource('rule-set', { path: '/international-edition-only' });
		});
		this.resource('extension', { path: '/:extension_id' }, function() {
			this.resource('product', { path: '/:product_id' }, function() {
				this.resource('build-input');
				this.resource('pre-conditions');
				this.resource('post-conditions');
				this.resource('build-trigger');
				this.resource('pre-execution');
				this.resource('execution-history');
				this.resource('build-mock', function() {
					this.route('results');
					this.route('debug');
					this.route('output');
					this.route('publish');
				});
				this.resource('build', { path: '/:build_id' }, function() {
					this.route('configuration');
					this.route('build-scripts');
					this.route('output');
					this.route('results');
					this.route('publish');
				});
			});
		});
	});
	this.resource('admin', function() {
		this.resource('create-release-center');
	});
	this.resource('confirm-dialog');
	this.resource('reload');
});


App.AbstractRoute = Ember.Route.extend({
	init: function() {
		App.set('lastKnownRoute', this);
	}
});

App.AbstractController = Ember.ObjectController.reopen({
	needs: "application"
})

App.AuthorisedRoute = App.AbstractRoute.extend({
	beforeModel: function() {
		// Close any open modals
		if ($('.modal').size() > 0) this.send('closeModal');
	}
});

App.ApplicationRoute = Ember.Route.extend({
	beforeModel: function() {
		App.store = this.store;
	},
	model: function() {
		return Ember.Object.create({
			currentUser: App.loadCurrentUser()
		})
	},
	actions: {
		signin: function() {
			var $form = $('.navbar form.login');
			var username = $form.find('[name="username"]').val();
			var password = $form.find('[name="password"]').val();
			try {
				var user = App.login(username, password);
				this.currentModel.set('currentUser', user);
			} catch (error) {
				console.log('error', error);
				$form.find('.login\\-error').show();
			}
		},
		signout: function() {
			var user = App.logout();
			this.currentModel.set('currentUser', user);
			this.transitionTo('reload');
		},
		removeEntity: function(model) {
			console.log('removeEntity', model);
			this.send('openModal', 'remove-entity', model);
		},
		openModal: function(modalName, params) {
			console.log('openModal');
			var controller = this.controllerFor(modalName);
			var model = null;
			if (controller.getModel) {
				model = controller.getModel(params);
			} else {
				model = params;
			}
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

// Index
App.IndexRoute = App.AuthorisedRoute.extend({
	model: function() {
		return {
			releaseCenters: this.store.filter('center', {}, function(center) {
				return !center.get('removed');
			})
		}
	},
	actions: {
		addProduct: function(releaseCenter) {
			this.send('openModal', 'create-product', releaseCenter);
		}
	}
})

App.ReloadRoute = App.AbstractRoute.extend({
	beforeModel: function() {
		this.transitionTo('index');
	}
})

// Admin
App.AdminRoute = App.AuthorisedRoute.extend({
	model: function() {
		return {
			releaseCenters: this.store.filter('center', {}, function(center) {
				return !center.get('removed');
							})
		}
	}
})

// ReleaseCenter
App.ReleaseCenterRoute = App.AuthorisedRoute.extend({
	model: function(params) {
		return this.store.find('center', params.releaseCenter_id);
	}
})
App.ReleaseCenterIndexRoute = App.AuthorisedRoute.extend({
	model: function() {
		return this.modelFor('release-center');
	},
	actions: {
		addProduct: function(releaseCenter) {
			this.send('openModal', 'create-product', releaseCenter);
		}			
	}
})
App.CreateReleaseCenterView = Ember.View.extend({
	templateName: 'admin/create-release-center',
	didInsertElement: function() {
		this.controller.send('modalInserted');
	}
})
App.CreateReleaseCenterController = Ember.ObjectController.extend({
	getModel: function() {
		return this.store.createRecord('center');
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
		return this.modelFor('release-center');
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
App.ProductRoute = App.AuthorisedRoute.extend({
	model: function(params) {
		var center = this.modelFor('release-center');
		return center.get('products').then(function(products) {
			var product = products.findBy('id', params.product_id);
			product.set('parent', center);
			return  product;
		});
	}
})
App.ProductIndexRoute = App.AuthorisedRoute.extend({
	model: function() {
		return this.modelFor('product');
	}
})
App.CreateProductView = Ember.View.extend({
	templateName: 'product-maintenance',
	didInsertElement: function() {
		this.controller.send('modalInserted');
	}
})
App.CreateProductController = Ember.ObjectController.extend({
	getModel: function(releaseCenter) {
		var product = this.store.createRecord('product', {
			parent: releaseCenter
		});
		return  product;
	},
	actions: {
		submit: function() {
			var product = this.get('model');
			var releaseCenter = product.get('parent');
			releaseCenter.get('products').pushObject(product);
			product.save();
			this.send('closeModal');
		}
	}
})

// Build
App.BuildIndexController = Ember.ObjectController.extend({
	actions: {
		createExecution: function (selectedBuild) {
			var execution = this.store.createRecord('execution', {
				parent: selectedBuild
			})
			var controller = this;
			execution.save().then(function(execution) {
				execution.set('parent', selectedBuild);
				controller.transitionToRoute('execution', execution);
			})
		},
		addPackage: function(build) {
			this.send('openModal', 'create-package', build);
		}			
	}
})

App.BuildRoute = App.AuthorisedRoute.extend({
	model: function(params) {
		var product = this.modelFor('product');
		return product.get('builds').then(function(builds) {
			var build = builds.findBy('id', params.build_id);
			build.set('product', product);
			return build;
		});
	}
})
App.CreateBuildView = Ember.View.extend({
	templateName: 'build-maintenance',
	didInsertElement: function() {
		this.controller.send('modalInserted');
	}
})
App.CreateBuildController = Ember.ObjectController.extend({
	getModel: function(product) {
		var build = this.store.createRecord('build', {
			parent: product
		});
		return  build;
	},
	actions: {
		submit: function() {
			var build = this.get('model');
			var product = build.get('parent');
			product.get('builds').pushObject(build);
			build.save();
			this.send('closeModal');
		}
	}
})

App.BuildIndexRoute = App.AuthorisedRoute.extend({
	model: function() {
		return this.modelFor('build');
	}
})
App.ExecutionHistoryRoute = App.AuthorisedRoute.extend({
	model: function(params) {
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
App.CreatePackageView = Ember.View.extend({
	templateName: 'package-maintenance',
	didInsertElement: function() {
		this.controller.send('modalInserted');
	}
})
App.CreatePackageController = Ember.ObjectController.extend({
	getModel: function(build) {
		var package = this.store.createRecord('package', {
			parent: build
		});
		return  package;
	},
	actions: {
		submit: function() {
			var package = this.get('model');
			var build = package.get('parent');
			build.get('packages').pushObject(package);
			package.save();
			this.send('closeModal');
		}
	}
})

App.BuildInputRoute = App.AuthorisedRoute.extend({
	model: function(params) {
		return this.modelFor('package');
	},
	setupController: function(controller, model) {
		//controller.set('model', model);
		controller.set('content', model);
		console.log ("Got me a " +  model.get('inputfiles') + " and a " + model.get('manifest'));
		//controller.set('inputfiles', model.get('inputfiles'));
		//controller.set('manifest', model.get('manifest'));
	}
})
App.BuildInputController = Ember.ObjectController.extend({
	actions: {
		reload: function() {
			var inputfiles = this.get('inputfiles');
			inputfiles.reloadLinks();
			var manifest = this.get('manifest');
			manifest.reloadLinks(this.get('model'), 'manifest');
		},
		deleteInputFile: function(inputFile) {
			console.log('deleteInputFile');
			console.log(inputFile);
			var store = this.store;
			this.send('openModal', 'confirm-dialog',
				{
					question: 'Delete input file?',
					guardedFunction: function() {
						console.log('deleting', inputFile);
						var parent = inputFile.get('parent');
						inputFile.deleteRecord();
						inputFile.set('parent', parent);
						inputFile.save().then(function() {
							parent.get('inputfiles').removeObject(inputFile);
						})
					}
				});
		}
	}
})
App.PostConditionsRoute = App.AuthorisedRoute.extend({
	model: function(params) {
		return this.modelFor('package');
	}
})


App.ExecutionMockRoute = App.AuthorisedRoute.extend({
	model: function() {
		var build = this.modelFor('build');
		var pack = { name: '17 Feb, 2014 01:05:00 (UTC)', parent: build };
		return pack;
	}
})
App.ExecutionMockIndexRoute = App.AuthorisedRoute.extend({
	beforeModel: function() {
		this.transitionTo('execution-mock.results');
	}
})

App.ExecutionRoute = App.AuthorisedRoute.extend({
	model: function(params) {
		var build = this.modelFor('build');
		return build.get('executions').then(function(executions) {
			var execution = executions.findBy('id', params.execution_id);
			execution.set('parent', build);
			return execution;
		});

	},
	setupController: function(controller, model) {
		controller.set('model', model);
		controller.onload();
	},
	deactivate: function(){
		this.controller.unload();
	}
})
App.ExecutionController = Ember.ObjectController.extend({
	onload: function() {
		var model = this.get('model');
		App.ExecutionController.updateStatusHalt = false;
		this.updateStatus(model);
	},
	unload: function() {
		App.ExecutionController.updateStatusHalt = true;
	},
	updateStatus: function(model) {
		var controller = this;
		$.ajax({
			url: model._data.url,
			success: function(data) {
				var newStatus = data.status;
				model.set('status', newStatus);
				if (newStatus == App.BuildStatus.BUILT) {
					// No need to keep updating, status can't change now.
					App.ExecutionController.updateStatusHalt = true;
				}
				if (!App.ExecutionController.updateStatusHalt) {
					setTimeout(function() {
						controller.updateStatus(model);
					}, 1000)
				}
			}
		});
	}
})
App.ExecutionIndexRoute = App.AuthorisedRoute.extend({
	beforeModel: function() {
		this.transitionTo('execution.configuration');
	}
})
App.ExecutionConfigurationRoute = App.AuthorisedRoute.extend({
	model: function(params) {
		return this.modelFor('execution');
	},
	actions: {
		triggerBuild: function(execution) {
			var store = this.store;
			var adapter = store.adapterFor(App.Execution);
			var build = execution.get('parent');
			var url = adapter.buildNestedURL(execution);
			url += '/trigger';
			$.ajax({
				url: url,
				type: 'POST',
				success: function(executionData) {
					executionData.parent = build;
					store.push(App.Execution, executionData, true);
				}
			})
		}
	}
})
App.ExecutionBuildScriptsRoute = App.AuthorisedRoute.extend({
	model: function(params) {
		return this.modelFor('execution');
	}
})
App.ExecutionOutputRoute = App.AuthorisedRoute.extend({
	model: function(params) {
		var model = this.modelFor('execution');
		var log = model.get('output-log');
		if (!log) {
			model.set('output-log', 'Loading...');
			$.ajax({
				url: model._data.url + '/output/target/maven.log',
				success: function(data) {
					model.set('output-log', data);
				}
			})
		}
		return model;
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

App.ConfirmDialogView = Ember.View.extend({
	templateName: 'confirm-dialog',
	didInsertElement: function() {
		this.controller.send('modalInserted');
	}
})
App.ConfirmDialogController = Ember.ObjectController.extend({
	actions: {
		proceed: function() {
			var model = this.get('model');
			var guardedFunction = model.guardedFunction;
			guardedFunction();
		}
	}
})


function afterRender() {
	console.log("in function afterRender()");
	$("[data-toggle='popover']").popover();
	$("[data-toggle='tooltip']").tooltip();
	$("[data-toggle='dropdown']").dropdown();
	effectPulse($('.traffic-light-in-progress'));	
	initUploadForm();
}

function initUploadForm() {
	$('.uploadForm').each(function() {
		var $form = $(this);
		$form.submit(function () {
			if ($form.valid()) {
				var action = $('.actionpath', $form).text();
				$form.attr('action', action);
				var $button = $form.find('input[type="submit"]');
				$button.val('Uploading...');
				$button.prop('disabled', true);

				//Submitting a form does not allow Basic Auth headers to be set, so we're going to pass
				//the auth token in a hidden field (XMLHttpRequest method also tried but problems with file data)
				if (App.authenticationToken) {
					$form.find('input[name="auth_token"]').remove();// remove any existing auth_tokens
					$form.append('<input type="hidden" name="auth_token" value="' + btoa(App.authenticationToken) + '" />');
				}
				return true;
			} else {
				return false;
			}
		});
	})

//	$('.panel-build-input #buildInputFileUploadIframe').load(function() {
//		var formIndex = isManifest?0:1;
//		$button.val('Upload');
//		$button.prop('disabled', false);
//		$('.panel-build-input form')[formIndex].reset();
//		$('.panel-build-input .reloadmodel').click();
//	});
}


function effectPulse($selection) {
	
	if ($selection && $selection.size() > 0) {
		$selection.stop();
		$selection.clearQueue();
		$selection.animate({
			opacity: 0
		}, 900, function() {
			$selection.animate({
				opacity: 1
			}, 900, function() {
				effectPulse($('.traffic-light-in-progress'));
			})
		})
	}
}
