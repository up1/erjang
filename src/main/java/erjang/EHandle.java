/**
 * This file is part of Erjang - A JVM-based Erlang VM
 *
 * Copyright (c) 2009 by Trifork
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/


package erjang;

import kilim.Pausable;

/**
 * An EHandle is either an EPort or an EPID.  EHandles can be sent messages
 */
public abstract class EHandle extends EObject {

	ETask<?> task() {
		throw new Error("only local handles can provide task reference");
	}

	/**
	 * @param msg
	 * @throws Pausable 
	 */
	public void send(EObject msg) throws Pausable {
		task().mbox_send(msg);
	}

	/**
	 * @param self
	 * @param result
	 * @throws Pausable 
	 */
	public void exit_signal(EHandle from, EObject reason) throws Pausable {
		task().send_exit(from, reason);
	}

	/**
	 * A one-way link message.  (other is already linked to this handle).
	 * 
	 * @param other
	 */
	public abstract void link_oneway(EHandle other);
	
}
