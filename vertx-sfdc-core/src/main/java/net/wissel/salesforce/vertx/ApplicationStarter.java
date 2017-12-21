/** ========================================================================= *
 * Copyright (C)  2017, 2018 Salesforce Inc ( http://www.salesforce.com/      *
 *                            All rights reserved.                            *
 *                                                                            *
 *  @author     Stephan H. Wissel (stw) <swissel@salesforce.com>              *
 *                                       @notessensei                         *
 * @version     1.0                                                           *
 * ========================================================================== *
 *                                                                            *
 * Licensed under the  Apache License, Version 2.0  (the "License").  You may *
 * not use this file except in compliance with the License.  You may obtain a *
 * copy of the License at <http://www.apache.org/licenses/LICENSE-2.0>.       *
 *                                                                            *
 * Unless  required  by applicable  law or  agreed  to  in writing,  software *
 * distributed under the License is distributed on an  "AS IS" BASIS, WITHOUT *
 * WARRANTIES OR  CONDITIONS OF ANY KIND, either express or implied.  See the *
 * License for the  specific language  governing permissions  and limitations *
 * under the License.                                                         *
 *                                                                            *
 * ========================================================================== *
 */
package net.wissel.salesforce.vertx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormat;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Verticle;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.eventbus.ReplyFailure;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import net.wissel.salesforce.vertx.config.AppConfig;
import net.wissel.salesforce.vertx.config.AuthConfig;
import net.wissel.salesforce.vertx.config.BaseConfig;
import net.wissel.salesforce.vertx.config.ConsumerConfig;
import net.wissel.salesforce.vertx.config.ListenerConfig;

/**
 * Main Verticle that loads the entire application and its dependent Verticles
 * Loads a configuration file in JSON format and loads up to 4 categories of
 * Verticles - Authentication providers - Listeners (that listen to incoming
 * events, currently CometD) - Consumers (listen on the eventbus and do
 * something to the incoming data) - other verticles
 * 
 * @author stw
 *
 */
public class ApplicationStarter extends AbstractVerticle {

	/**
	 * Start Helper to test in IDE or Command line
	 *
	 * @param args
	 *            - ignored here
	 */
	public static void main(final String[] args) {
		// Start in debug mode
		Runner.runVerticle(ApplicationStarter.class.getName(), true);
	}

	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());
	private final List<String> loadedVerticles = new ArrayList<String>();
	private AppConfig appConfig = null;
	private Router router = null;
	private final Date startDate = new Date();

	/**
	 * @see io.vertx.core.AbstractVerticle#start(io.vertx.core.Future)
	 */
	@Override
	public void start(final Future<Void> startFuture) throws Exception {
		this.logger.trace("Client for SFDC Platform Events starting");
		this.router = Router.router(this.getVertx());

		// Load config and verticles and signal back that we are done
		final Future<Void> verticleLoad = Future.future();
		verticleLoad.setHandler(ar -> {
			if (ar.succeeded()) {
				this.startWebServer(startFuture);
			} else {
				startFuture.fail(ar.cause());
			}
		});

		this.loadAppConfig(verticleLoad);

	}

	/**
	 * After loading a configuration from the JSON file, this function checks
	 * for a given number of settings in the environment. Most notably
	 * user/password/port This allows to keep the configuration in Version
	 * Control without disclosing credentials. This is in line with the PaaS
	 * practises to provide credentials on the environment
	 * 
	 * @param configCandidate
	 *            - the JSON object with the initial settings
	 */
	private void addParametersFromEnvironment(final JsonObject configCandidate) {
		// We need the PORT for our http listener once
		String portCandidate = System.getenv(Constants.CONFIG_PORT);
		if (portCandidate != null) {
			try {
				configCandidate.put(Constants.CONFIG_PORT, Integer.parseInt(portCandidate));
			} catch (Exception e) {
				this.logger.fatal(e);
			}
		}

		// and parameters with prefix for each listener / consumer entry
		List<String> configNames = Arrays.asList("listenerConfigurations", "consumerConfigurations",
				"authConfigurations");
		List<String> envNames = Arrays.asList("Proxy", "ProxyPort", "sfdcUser", "sfdcPassword");
		List<String> proxyNames = Arrays.asList("Proxy", "ProxyPort");
		for (String cName : configNames) {
			try {
				JsonArray cArray = configCandidate.getJsonArray(cName);
				if (cArray != null) {
					cArray.forEach(oneConf -> {
						JsonObject c = (JsonObject) oneConf;
						String prefix = c.getString(Constants.CONFIG_AUTHNAME);
						if (prefix != null) {
							List<String> toProcess = (cName.equals("authConfigurations")) ? envNames : proxyNames;
							toProcess.forEach(key -> {
								String candidate = System.getenv(prefix + "_" + key);
								if (candidate != null) {
									c.put(key, candidate);
								}
							});

						}
					});
				}
			} catch (Exception e) {
				e.printStackTrace();
				this.logger.fatal(e);
			}
		}
	}

	/**
	 *
	 * @param bc
	 *            Base configuration as retrieved from JSON config file and
	 *            environment The verticle gets only its own configuration, not
	 *            the whole file
	 * 
	 * @return Options to load verticle
	 */
	private DeploymentOptions getDeploymentOptions(final BaseConfig bc) {
		// Update global proxy if any
		bc.setProxyFromAppConfig(this.appConfig);
		final DeploymentOptions options = new DeploymentOptions();
		options.setWorker(bc.isDeployAsWorker());
		options.setInstances(bc.getVerticleInstanceCount());
		final JsonObject param = JsonObject.mapFrom(bc);
		options.setConfig(param);
		return options;
	}

	/**
	 * Read configuration, then hand over to loadVerticles
	 *
	 * @param verticleLoad
	 *            - Future to report completions
	 */
	private void loadAppConfig(final Future<Void> verticleLoad) {
		final ConfigStoreOptions fileConfig = new ConfigStoreOptions().setType("file").setFormat("json")
				.setConfig(new JsonObject().put("path", this.getOptionFileName()));
		final ConfigRetrieverOptions options = new ConfigRetrieverOptions().addStore(fileConfig);
		final ConfigRetriever retriever = ConfigRetriever.create(this.getVertx(), options);

		retriever.getConfig(ar -> {
			if (ar.failed()) {
				verticleLoad.fail(ar.cause());
			} else {
				final JsonObject payload = ar.result();
				try {
					this.addParametersFromEnvironment(payload);
					this.appConfig = payload.mapTo(AppConfig.class);
					this.loadVerticles(verticleLoad);
				} catch (final Throwable t) {
					verticleLoad.fail(t);
					return;
				}
				// Hot reload for config changes
				// retriever.listen(change -> {
				// // TODO: implement hot reload
				// });
			}
		});

	}

	/**
	 * Get the file name for the Options File. Default defined in
	 * Constants.OPTION_FILE_NAME
	 * 
	 * @return
	 */
	private String getOptionFileName() {
		return this.config().getString(Constants.OPTION_FILE_NAME, Constants.OPTION_FILE_NAME);
	}

	/**
	 * Main rountine to load all verticles with parameters
	 * 
	 * @param verticleLoad
	 *            - a future that completes when all verticles loaded
	 *            successfully
	 */
	private void loadVerticles(final Future<Void> verticleLoad) {
		@SuppressWarnings("rawtypes")
		final List<Future> allLoadedVerticles = new ArrayList<Future>();

		// Authorizers
		for (final AuthConfig ac : this.appConfig.authConfigurations) {
			allLoadedVerticles.add(this.loadVerticle(ac.getVerticleName(), this.getDeploymentOptions(ac)));
		}

		// Consumers
		for (final ConsumerConfig cc : this.appConfig.consumerConfigurations) {
			// Consumers might have a router extension (mainly for webSockets)
			if (cc.isProvidesRouterExtension()) {
				allLoadedVerticles.add(this.loadVerticleByClass(cc));
			} else {
				allLoadedVerticles.add(this.loadVerticle(cc.getVerticleName(), this.getDeploymentOptions(cc)));
			}
		}

		// Listeners
		for (final ListenerConfig lc : this.appConfig.listenerConfigurations) {
			allLoadedVerticles.add(this.loadVerticle(lc.getVerticleName(), this.getDeploymentOptions(lc)));
		}

		// Other
		for (final Map.Entry<String, Map<String, Object>> me : this.appConfig.verticlesToLoad.entrySet()) {
			final String vid = me.getKey();
			final DeploymentOptions options = new DeploymentOptions();
			options.setConfig(new JsonObject(me.getValue()));
			allLoadedVerticles.add(this.loadVerticle(vid, options));
		}

		// Signal start
		CompositeFuture.all(allLoadedVerticles).setHandler(allLoaded -> {
			if (allLoaded.succeeded()) {
				DeliveryOptions delOps = new DeliveryOptions().addHeader(Constants.MESSAGE_ISSTARTUP,
						Constants.TRUESTRING);
				this.getVertx().eventBus().publish(Constants.BUS_START_STOP, Constants.MESSAGE_START, delOps);
				verticleLoad.complete();
			} else {
				verticleLoad.fail(allLoaded.cause());
			}

		});
	}

	/**
	 * Name based loading of a Verticle with configured options
	 *
	 * @param verticleId
	 *            the full qualified name - can be any supported Verticel
	 *            language
	 * @param options
	 *            The configuration object
	 * @return a Future that resolves after loading
	 */
	private Future<Void> loadVerticle(final String verticleId, final DeploymentOptions options) {
		final Future<Void> result = Future.future();
		this.getVertx().deployVerticle(verticleId, options, r -> {
			if (r.succeeded()) {
				final String vid = r.result();
				this.logger.info(verticleId + " started as " + vid);
				this.loadedVerticles.add(vid);
				result.complete();
			} else {
				this.logger.fatal(r.cause());
				result.fail(r.cause());
			}
		});
		return result;
	}

	/**
	 * Loads a Verticle by instantiating the Java class first needed when
	 * objects need to be handed over
	 * 
	 * @param bc
	 *            the Base configuration
	 * @return a future that resolves after loading
	 */
	private Future<Void> loadVerticleByClass(final BaseConfig bc) {
		final Future<Void> result = Future.future();
		final String verticleId = bc.getVerticleName();
		final DeploymentOptions options = this.getDeploymentOptions(bc);

		try {
			@SuppressWarnings("unchecked")
			final Class<Verticle> vClass = (Class<Verticle>) Class.forName(verticleId);
			final Verticle v = vClass.newInstance();

			/*
			 * This is the magic: we hand over the router to the verticle to be
			 * used only sparingly
			 */
			if (v instanceof SFDCRouterExtension) {
				((SFDCRouterExtension) v).addRoutes(this.router);
			}
			this.getVertx().deployVerticle(v, options, r -> {
				if (r.succeeded()) {
					final String vid = r.result();
					this.logger.trace(verticleId + "started as " + vid);
					this.loadedVerticles.add(vid);
					result.complete();
				} else {
					this.logger.fatal(r.cause());
					result.fail(r.cause());
				}
			});

		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
			this.logger.fatal(e.getMessage(), e);
			result.fail(e);
		}

		return result;

	}

	/**
	 * Shuts down all verticles that were loaded. Keeps the sequence of first
	 * shutting down all listeners, so no new data enters then shuts down the
	 * consumers, so messages can drain out of the bus
	 * 
	 * @param stopFuture
	 *            completes when all verticles are unloaded
	 */
	private void shutDownVerticles(final Future<Void> stopFuture) {
		final EventBus eb = this.getVertx().eventBus();

		@SuppressWarnings("rawtypes")
		final List<Future> stopListening = new ArrayList<Future>();
		this.logger.info("Shutting down listeners, can take a while....");
		DeliveryOptions delOpt = new DeliveryOptions();
		// The shutdown might be mightly delayed
		// Due to the nature of cometD, so we wait up to 120 seconds
		delOpt.setSendTimeout(120000);
		// Make the listeners stop listening
		for (final ListenerConfig lc : this.appConfig.listenerConfigurations) {
			final Future<Void> curFuture = Future.future();
			final String shutdowAddress = Constants.BUS_START_STOP + Constants.DELIMITER + lc.getVerticleName();
			try {
				eb.send(shutdowAddress, Constants.MESSAGE_STOP, delOpt, ar -> {
					if (ar.succeeded()) {
						curFuture.complete();
					} else {
						curFuture.fail(ar.cause());
					}
				});
				stopListening.add(curFuture);
			} catch (Throwable t) {
				this.logger.error(t);
			}
		}

		// Continue when all stopped listening
		CompositeFuture.all(stopListening).setHandler(ar -> {
			if (ar.succeeded()) {
				// Now unload the verticles
				this.logger.info("Shutting down verticles..");
				@SuppressWarnings("rawtypes")
				final List<Future> downedVerticles = new ArrayList<Future>();
				this.loadedVerticles.forEach(moriturus -> {
					this.logger.info("Shutting down " + moriturus);
					final Future<Void> curFuture = Future.future();
					this.vertx.undeploy(moriturus, curFuture.completer());
					downedVerticles.add(curFuture);
				});

				CompositeFuture.all(downedVerticles).setHandler(downResult -> {
					if (downResult.succeeded()) {
						this.logger.info("All Verticles unloaded");
						stopFuture.complete();
					} else {
						this.logger.fatal(downResult.cause());
						stopFuture.fail(downResult.cause());
					}
				});

			} else {
				stopFuture.fail(ar.cause());
			}
		});

	}

	/**
	 * Loading of the API & WEB UI to provide a minimal Admin GUI to watch the
	 * system. WIP
	 */
	private void startWebServer(final Future<Void> startFuture) {

		// Sanitize the parameters and capture cookies / headers
		// TODO:
		// this.router.route().handler(RequestSanitizer.create(this.vertx));

		final String apiRoute = this.config().getString(Constants.API_ROOT, Constants.API_ROOT);

		// API route
		this.router.route(apiRoute).handler(this::rootHandler);

		// API Routes
		this.setupRouteSecurity(this.router);

		// To be able to access the request body
		this.router.route(apiRoute + "/*").handler(BodyHandler.create());

		// TODO: Deal with failures
		// this.router.route(apiRoute +
		// "/*").failureHandler(this::failureHandler);

		// Allow shutdown with a proper authorized request
		this.router.post(apiRoute + "/shutdown").handler(this::shutdownHandler);

		// Static pages
		this.router.route().handler(StaticHandler.create());

		// Launch the server
		this.logger.info("Listening on port " + Integer.toString(this.appConfig.port));
		this.vertx.createHttpServer().requestHandler(this.router::accept).listen(this.appConfig.port);

		// Finally done
		startFuture.complete();
	}

	/**
	 * Handler for the /api endpoint, listing out routes and loaded verticles
	 * 
	 * @param ctx
	 */
	private void rootHandler(final RoutingContext ctx) {
		ctx.response().putHeader(Constants.CONTENT_HEADER, Constants.CONTENT_TYPE_JSON);
		final JsonObject result = new JsonObject().put("RunningSince", Utils.getDateString(this.startDate));
		final JsonObject routeObject = new JsonObject();
		for (final Route r : this.router.getRoutes()) {
			final String p = r.getPath();
			if (p != null) {
				routeObject.put(p, String.valueOf(r));
			}
		}
		result.put("Routes", routeObject);
		final JsonArray verticleArray = new JsonArray(this.loadedVerticles);
		result.put("Verticles", verticleArray);
		ctx.response().end(result.encodePrettily());
	}

	/**
	 * Ensure proper authentication happens when accessing the GUI
	 * 
	 * @param router
	 */
	private void setupRouteSecurity(final Router router) {
		// TODO: Needs fixing
	}

	/**
	 * Executes the actual shutdown once one of the shutdown handlers has
	 * accepted the shutdown request
	 * 
	 * @param response
	 *            Tells the caller some metrics
	 */
	private void shutdownExecution(final HttpServerResponse response) {
		final JsonObject goodby = new JsonObject();
		goodby.put("Goodby", "It was a pleasure doing business with you");
		goodby.put("StartDate", Utils.getDateString(this.startDate));
		goodby.put("EndDate", Utils.getDateString(new Date()));
		final Duration dur = new Duration(new DateTime(this.startDate), new DateTime());
		goodby.put("Duration", PeriodFormat.getDefault().print(new Period(dur)));
		response.putHeader(Constants.CONTENT_HEADER, Constants.CONTENT_TYPE_JSON).setStatusCode(202)
				.end(goodby.encodePrettily());
		try {
			Future<Void> shutdownFuture = Future.future();
			shutdownFuture.setHandler(fResult -> {
				if (fResult.failed()) {
					this.logger.fatal(fResult.cause());
					System.exit(-1);
				}
				this.logger.info("Good by!");
				this.getVertx().close(handler -> {
					if (handler.failed()) {
						this.logger.fatal(handler.cause());
					}
					System.exit(0);
				});
			});
			this.shutDownVerticles(shutdownFuture);
		} catch (Exception e) {
			this.logger.fatal(e.getMessage(), e);
		}
	}

	/**
	 *  Use to terminate the application using a HTTP Post
	 *  It requires an AdminKey header to work
	 */
	private void shutdownHandler(final RoutingContext ctx) {
		// check for AdminKey header
		String adminKey = this.config().getString("AdminKey");
		if (adminKey == null || adminKey.equals(ctx.request().getHeader("AdminKey"))) {
			// TODO: check the body for the right credentials
			this.shutdownExecution(ctx.response());
		} else {
			ctx.fail(new ReplyException(ReplyFailure.RECIPIENT_FAILURE, 401, "Sucker nice try!"));
		}
	}

}
