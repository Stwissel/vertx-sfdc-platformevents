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

import io.vertx.core.Future;

/**
 * Interface that needs to be implemented by a Verticle to participate in the
 * SDFC lifecycle SFDCVerticles load, but don't do anything until they receive a
 * startListening() command The application starter sends a start listening
 * signal initially, that a SDFC Verticle can choose to ignore, so it doesn't
 * work on the initial startup
 * 
 * @author swissel
 *
 */
public interface SFDCVerticle {

	/** 
	 * 
	 * Verticle actually starting to listen to EventBus or incoming messages
	 * @return self - fluent API
	 */
	public SFDCVerticle startListening();

	/**
	 *  End of listening, typically before unload or interactive mode
	 * @param stopListenFuture Indicator when stop listening concluded
	 * @return self - fluent API
	 */
	public SFDCVerticle stopListening(final Future<Void> stopListenFuture);

}
