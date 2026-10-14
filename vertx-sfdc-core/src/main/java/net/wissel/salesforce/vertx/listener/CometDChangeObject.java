package net.wissel.salesforce.vertx.listener;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;

/**
 * CometD listener for Change Data Capture style events, which fan out by
 * object type.
 *
 * Where {@link CometD} publishes to the configured addresses as they are,
 * this one appends the payload's {@code ObjectType__c} to each address, so
 * subscribers can listen for one object type only
 *
 * @author stw
 */
public class CometDChangeObject extends CometD {

	/**
	 * Creates the listener; Vert.x instantiates it on deployment
	 */
	public CometDChangeObject() {
		// Nothing to set up
	}

	/**
	 * @see net.wissel.salesforce.vertx.listener.CometD#processOneResult(io.vertx.core.json.JsonObject)
	 */
	@Override
	protected void processOneResult(JsonObject dataChange) {
		final JsonObject data = dataChange.getJsonObject("data");
		final JsonObject payload = data.getJsonObject("payload");
		final String objectType = payload.getString("ObjectType__c");
		// We send it off to the eventbus
		final EventBus eb = this.getVertx().eventBus();
		this.getListenerConfig().getEventBusAddresses().forEach(destination -> {
			try {
				eb.publish(destination+objectType, payload);
				this.logger.debug("Sending to [{}]: {}", destination + objectType, payload);
			} catch (final Throwable t) {
				this.logger.error(t.getMessage(), t);
			}
		});
	}
	
	

}
