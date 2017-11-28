package net.wissel.salesforce.vertx.listener;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;

public class CometDChangeObject extends CometD {

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
				System.out.println("Sending to [" + destination+objectType + "]:" + payload.toString());
			} catch (final Throwable t) {
				this.logger.error(t.getMessage(), t);
			}
		});
	}
	
	

}
