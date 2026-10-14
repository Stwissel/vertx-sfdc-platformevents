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

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Deployable;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.ThreadingModel;
import io.vertx.core.VerticleBase;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.eventbus.ReplyFailure;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import net.wissel.salesforce.vertx.auth.AuthInfo;
import net.wissel.salesforce.vertx.auth.AuthInfoCodec;
import net.wissel.salesforce.vertx.config.AppConfig;
import net.wissel.salesforce.vertx.config.AuthConfig;
import net.wissel.salesforce.vertx.config.BaseConfig;
import net.wissel.salesforce.vertx.config.ConsumerConfig;
import net.wissel.salesforce.vertx.config.DedupConfig;
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
public class ApplicationStarter extends VerticleBase {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private final List<String> loadedVerticles = new ArrayList<String>();
	private AppConfig appConfig = null;
	private Router router = null;
	private final Date startDate = new Date();
	private ConfigRetriever retriever = null;
	private int listeningPort = -1;

	/**
	 * Creates the starter; Vert.x instantiates it on deployment
	 */
	public ApplicationStarter() {
		// Nothing to set up
	}

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

	/**
	 * @see io.vertx.core.VerticleBase#start()
	 */
	@Override
	public Future<?> start() throws Exception {
		this.logger.trace("Client for SFDC Platform Events starting");
		this.router = Router.router(this.vertx);

		// Load config, then verticles, then the web server
		return this.loadAppConfig()
				.compose(v -> this.loadVerticles())
				.compose(v -> this.startWebServer());
	}

	/**
	 * The port the admin web server actually listens on. Differs from the
	 * configured port when that was 0 (pick any free port)
	 *
	 * @return the actual port, -1 before the server is up
	 */
	int getListeningPort() {
		return this.listeningPort;
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
		this.addParametersFromEnvironment(configCandidate, System.getenv());
	}

	/**
	 * Testable variant of the environment override logic
	 *
	 * @param configCandidate
	 *            - the JSON object with the initial settings
	 * @param env
	 *            - the environment variables to apply
	 */
	void addParametersFromEnvironment(final JsonObject configCandidate, final Map<String, String> env) {
		// We need the PORT for our http listener once
		final String portCandidate = env.get(Constants.CONFIG_PORT);
		if (portCandidate != null) {
			try {
				// AppConfig maps the lower case "port" property
				configCandidate.put("port", Integer.parseInt(portCandidate));
			} catch (Exception e) {
				this.logger.error("Cannot parse PORT environment variable", e);
			}
		}

		// and parameters with prefix for each listener / consumer entry
		List<String> configNames = Arrays.asList("listenerConfigurations", "consumerConfigurations",
				"authConfigurations", "dedupConfigurations");
		List<String> envNames = Arrays.asList("Proxy", "ProxyPort", "sfdcUser", "sfdcPassword", "consumerToken", "consumerSecret");
		List<String> proxyNames = Arrays.asList("Proxy", "ProxyPort");
		for (String cName : configNames) {
			try {
				JsonArray cArray = configCandidate.getJsonArray(cName);
				if (cArray != null) {
					cArray.forEach(oneConf -> {
						JsonObject c = (JsonObject) oneConf;
						String prefix = c.getString(Constants.CONFIG_AUTHNAME);
						if (prefix != null) {
							List<String> toProcess = (cName.equals("authConfigurations") || cName.equals("dedupConfigurations")) ? envNames : proxyNames;
							toProcess.forEach(key -> {
								String candidate = env.get(prefix + "_" + key);
								if (candidate != null) {
									c.put(key, candidate);
								}
							});

						}
					});
				}
			} catch (Exception e) {
				this.logger.error("Environment override failed for {}", cName, e);
			}
		}
	}

	/**
	 * Builds the deployment options for one configured verticle
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
		options.setThreadingModel(bc.isDeployAsWorker() ? ThreadingModel.WORKER : ThreadingModel.EVENT_LOOP);
		options.setInstances(bc.getVerticleInstanceCount());
		final JsonObject param = JsonObject.mapFrom(bc);
		options.setConfig(param);
		return options;
	}

	/**
	 * Read configuration from the JSON file and the environment
	 *
	 * @return Future that completes when the configuration is loaded
	 */
	private Future<Void> loadAppConfig() {

		//Register codec for Authentication scheme
		final Vertx vertx = this.vertx;
		vertx.eventBus().registerDefaultCodec(AuthInfo.class, new AuthInfoCodec());

		final ConfigStoreOptions fileConfig = new ConfigStoreOptions().setType("file").setFormat("json")
				.setConfig(new JsonObject().put("path", this.getOptionFileName()));
		final ConfigRetrieverOptions options = new ConfigRetrieverOptions().addStore(fileConfig);
		this.retriever = ConfigRetriever.create(this.vertx, options);

		return this.retriever.getConfig().compose(payload -> {
			try {
				this.addParametersFromEnvironment(payload);
				AppConfig newConfig = payload.mapTo(AppConfig.class);
				if (!newConfig.equals(this.appConfig)) {
					this.appConfig = newConfig;
					// Hot reload for config changes
					vertx.eventBus().publish(Constants.BUS_CONFIGUPDATE, payload);
				}
				return Future.succeededFuture();
			} catch (final Throwable t) {
				return Future.failedFuture(t);
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
	 * @return a future that completes when all verticles loaded successfully
	 */
	private Future<Void> loadVerticles() {
		final List<Future<?>> allLoadedVerticles = new ArrayList<>();

		// Authorizers
		for (final AuthConfig ac : this.appConfig.authConfigurations) {
			allLoadedVerticles.add(this.loadVerticle(ac.getVerticleName(), this.getDeploymentOptions(ac)));
		}

		// Deduplication
		for (final DedupConfig dc : this.appConfig.dedupConfigurations) {
			allLoadedVerticles.add(this.loadVerticle(dc.getVerticleName(), this.getDeploymentOptions(dc)));
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
		return Future.all(allLoadedVerticles).compose(allLoaded -> {
			DeliveryOptions delOps = new DeliveryOptions().addHeader(Constants.MESSAGE_ISSTARTUP,
					Constants.TRUESTRING);
			this.vertx.eventBus().publish(Constants.BUS_START_STOP, Constants.MESSAGE_START, delOps);
			return Future.succeededFuture();
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
		return this.vertx.deployVerticle(verticleId, options)
				.onSuccess(vid -> {
					this.logger.info("{} started as {}", verticleId, vid);
					this.loadedVerticles.add(vid);
				})
				.onFailure(t -> this.logger.error("Deployment failed for {}", verticleId, t))
				.mapEmpty();
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
		final String verticleId = bc.getVerticleName();
		final DeploymentOptions options = this.getDeploymentOptions(bc);
		// Instance based deployment cannot deploy multiple instances
		options.setInstances(1);

		try {
			final Class<?> vClass = Class.forName(verticleId);
			final Deployable v = (Deployable) vClass.getDeclaredConstructor().newInstance();

			/*
			 * This is the magic: we hand over the router to the verticle to be
			 * used only sparingly
			 */
			if (v instanceof SFDCRouterExtension) {
				((SFDCRouterExtension) v).addRoutes(this.router);
			}
			return this.vertx.deployVerticle(v, options)
					.onSuccess(vid -> {
						this.logger.trace("{} started as {}", verticleId, vid);
						this.loadedVerticles.add(vid);
					})
					.onFailure(t -> this.logger.error("Deployment failed for {}", verticleId, t))
					.mapEmpty();

		} catch (ReflectiveOperationException | ClassCastException e) {
			this.logger.error("Could not instantiate {}", verticleId, e);
			return Future.failedFuture(e);
		}

	}

	/**
	 * Shuts down all verticles that were loaded. Keeps the sequence of first
	 * shutting down all listeners, so no new data enters then shuts down the
	 * consumers, so messages can drain out of the bus
	 *
	 * @return a future that completes when all verticles are unloaded
	 */
	private Future<Void> shutDownVerticles() {
		final EventBus eb = this.vertx.eventBus();

		final List<Future<?>> stopListening = new ArrayList<>();
		this.logger.info("Shutting down listeners, can take a while....");
		DeliveryOptions delOpt = new DeliveryOptions();
		// The shutdown might be mightly delayed
		// Due to the nature of cometD, so we wait up to 120 seconds
		delOpt.setSendTimeout(120000);
		// Make the listeners stop listening
		for (final ListenerConfig lc : this.appConfig.listenerConfigurations) {
			final String shutdowAddress = Constants.BUS_START_STOP + Constants.DELIMITER + lc.getInstanceName();
			try {
				stopListening.add(eb.request(shutdowAddress, Constants.MESSAGE_STOP, delOpt));
			} catch (Throwable t) {
				this.logger.error("Could not send stop message to {}", shutdowAddress, t);
			}
		}

		// Continue when all stopped listening
		return Future.all(stopListening)
				.compose(ar -> {
					// Now unload the verticles
					this.logger.info("Shutting down verticles..");
					final List<Future<?>> downedVerticles = new ArrayList<>();
					this.loadedVerticles.forEach(moriturus -> {
						this.logger.info("Shutting down {}", moriturus);
						downedVerticles.add(this.vertx.undeploy(moriturus));
					});
					return Future.all(downedVerticles);
				})
				.onSuccess(v -> this.logger.info("All Verticles unloaded"))
				.onFailure(t -> this.logger.error("Shutdown of verticles failed", t))
				.mapEmpty();
	}

	/**
	 * Loading of the API & WEB UI to provide a minimal Admin GUI to watch the
	 * system. WIP
	 *
	 * @return a future with the listening HTTP server
	 */
	private Future<HttpServer> startWebServer() {

		// Sanitize the parameters and capture cookies / headers
		// TODO:
		// this.router.route().handler(RequestSanitizer.create(this.getVertx()));

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
		//TODO: define place for static resources
		this.router.route().handler(StaticHandler.create());

		// Launch the server - a failed bind fails the verticle start
		return this.vertx.createHttpServer()
				.requestHandler(this.router)
				.listen(this.appConfig.port)
				.onSuccess(server -> {
					this.listeningPort = server.actualPort();
					this.logger.info("Web Server listening on port {}", this.listeningPort);
				})
				.onFailure(t -> this.logger.error("Web Server could not bind to port {}", this.appConfig.port, t));
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
		final Duration dur = Duration.between(this.startDate.toInstant(), Instant.now());
		goodby.put("Duration", dur.toString());
		response.putHeader(Constants.CONTENT_HEADER, Constants.CONTENT_TYPE_JSON).setStatusCode(202)
				.end(goodby.encodePrettily());
		try {
			this.shutDownVerticles().onComplete(fResult -> {
				if (fResult.failed()) {
					this.logger.error("Shutdown failed", fResult.cause());
					System.exit(-1);
				}
				this.logger.info("Good by!");
				this.vertx.close().onComplete(handler -> {
					if (handler.failed()) {
						this.logger.error("Vertx close failed", handler.cause());
					}
					System.exit(0);
				});
			});
		} catch (Exception e) {
			this.logger.error(e.getMessage(), e);
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
