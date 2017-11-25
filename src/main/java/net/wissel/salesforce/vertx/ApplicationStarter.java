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
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Verticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import net.wissel.salesforce.vertx.config.AppConfig;
import net.wissel.salesforce.vertx.config.AuthConfig;
import net.wissel.salesforce.vertx.config.BaseConfig;
import net.wissel.salesforce.vertx.config.ConsumerConfig;
import net.wissel.salesforce.vertx.config.ListenerConfig;
import net.wissel.salesforce.vertx.consumer.SFDCConsumer;

/**
 * Main Verticle that loads the entire application and its dependent Verticles
 *
 * @author stw
 *
 */
public class ApplicationStarter extends AbstractVerticle {

	/**
	 * Start Helper
	 *
	 * @param args
	 *            - ignored here
	 */
	public static void main(final String[] args) {
		// Start in debug mode
		Runner.runVerticle(ApplicationStarter.class.getName(), true);
	}

	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());
	private final Collection<String> loadedVerticles = new ArrayList<String>();
	private AppConfig appConfig = null;
	private Router router = null;
	int port = 8443;
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
	 * @see io.vertx.core.AbstractVerticle#stop(io.vertx.core.Future)
	 */
	@Override
	public void stop(final Future<Void> stopFuture) throws Exception {
		this.shutDownVerticles(stopFuture);
	}

	private void addParametersFromEnvironment(final AppConfig appConfig2) {
		// TODO Auto-generated method stub

	}

	/**
	 *
	 * @param bc
	 *            Base configuration
	 * @return Options to load verticle
	 */
	private DeploymentOptions getDeploymentOptions(final BaseConfig bc) {
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
		final ConfigStoreOptions env = new ConfigStoreOptions().setType("file").setFormat("json")
				.setConfig(new JsonObject().put("path", Constants.OPTION_FILE_NAME));
		final ConfigRetrieverOptions options = new ConfigRetrieverOptions().addStore(env);
		final ConfigRetriever retriever = ConfigRetriever.create(this.getVertx(), options);

		retriever.getConfig(ar -> {
			if (ar.failed()) {
				verticleLoad.fail(ar.cause());
			} else {
				final JsonObject payload = ar.result();
				try {
					this.appConfig = payload.mapTo(AppConfig.class);
					this.addParametersFromEnvironment(this.appConfig);
					this.loadedVerticles(verticleLoad);
				} catch (final Throwable t) {
					verticleLoad.fail(t);
					return;
				}
				// Hot reload for config changes
				retriever.listen(change -> {
					// TODO: implement hot reload
				});
			}
		});

	}

	private void loadedVerticles(final Future<Void> verticleLoad) {
		@SuppressWarnings("rawtypes")
		final List<Future> allLoadedVerticles = new ArrayList<Future>();

		// Authorizers
		for (final AuthConfig ac : this.appConfig.authConfigurations.values()) {
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
				this.getVertx().eventBus().publish(Constants.BUS_START_STOP, Constants.MESSAGE_START);
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
				this.logger.trace(verticleId + "started as " + vid);
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
	 * Loads a Verticle by instantiating the Java class first
	 * needed when objects need to be handed over
	 * @param bc the Base configuration
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
			 * used only sparringly
			 */
			if (v instanceof SFDCConsumer) {
				((SFDCConsumer) v).setRouter(this.router);
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

	private void shutDownVerticles(final Future<Void> stopFuture) {
		final EventBus eb = this.getVertx().eventBus();
		@SuppressWarnings("rawtypes")
		final List<Future> stopListening = new ArrayList<Future>();
		this.logger.info("Shutting down listeners, can take a while....");
		// Make the listeners stop listening
		for (final ListenerConfig lc : this.appConfig.listenerConfigurations) {
			final Future<Void> curFuture = Future.future();
			final String message = Constants.MESSAGE_STOP + Constants.DELIMITER + lc.getVerticleName();
			eb.send(Constants.BUS_START_STOP, message, ar -> {
				if (ar.succeeded()) {
					curFuture.complete();
				} else {
					curFuture.fail(ar.cause());
				}
			});
			stopListening.add(curFuture);
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

	private void startWebServer(final Future<Void> startFuture) {
		// TODO Auto-generated method stub

		this.logger.trace("System started " + Utils.getDateString(this.startDate));
		startFuture.complete();
	}

}
