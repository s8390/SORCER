/*
 * Copyright 2014 the original author or authors.
 * Copyright 2014 SorcerSoft.org.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sorcer.core.context;

import sorcer.service.*;

import java.rmi.RemoteException;

/**
 * Copier copies values of entries from one Context to another one.
 */
public class Copier implements Evaluation<Context> {

	private Context fromContext;
	private Arg[] fromEntries;
	private Context toContext;
	private Arg[] toEntries;

	public Copier(Context fromContext, Arg[] fromEntries, Context toContext, Arg[] toEntries) throws EvaluationException {
		this.fromContext = fromContext;
		this.fromEntries = fromEntries;
		this.toContext = toContext;
		this.toEntries = toEntries;
		if (fromEntries.length != toEntries.length)
			throw new EvaluationException("Sizes of from and to arguments do not match");
	}

	@Override
	public Context asis() throws EvaluationException, RemoteException {
		return toContext;
	}

	@Override
	public Context getValue(Arg... entries) throws EvaluationException, RemoteException {
		try {
			for (int i = 0; i < fromEntries.length; i++) {
				toContext.putValue(toEntries[i].getName(), fromContext.getValue(fromEntries[i].getName()));
			}
			toContext.substitute(entries);
		} catch (Exception e) {
			throw new EvaluationException(e);
		}
		return toContext;
	}

	@Override
	public Evaluation<Context> substitute(Arg... entries) throws SetterException, RemoteException {
			toContext.substitute(entries);
			return this;
	}
}