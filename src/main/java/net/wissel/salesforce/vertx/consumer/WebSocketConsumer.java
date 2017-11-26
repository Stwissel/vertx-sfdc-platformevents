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
package net.wissel.salesforce.vertx.consumer;

import io.vertx.core.Future;
import io.vertx.ext.web.Router;
import net.wissel.salesforce.vertx.AbstractSFDCVerticle;

/**
 * @author swissel
 *
 */
public class WebSocketConsumer extends AbstractSFDCVerticle implements SFDCConsumer {
	
	private Router router = null;

	@Override
	public void setRouter(Router router) {
		this.router = router;
	}
	

	@Override
	protected void startListening() {
		System.out.println("Start listening:" + this.getClass().getName());
		this.listening = true;		
	}

	@Override
	protected void stopListening(Future<Void> stopListenFuture) {
		System.out.println("Stop listening:" + this.getClass().getName());
		this.listening = false;
		stopListenFuture.complete();
		
	}


}
