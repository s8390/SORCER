/*
 * Copyright 2009 the original author or authors.
 * Copyright 2009 SorcerSoft.org.
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

package sorcer.service.modeling;

import java.io.Serializable;
import java.rmi.RemoteException;

import sorcer.core.context.ApplicationDescription;
import sorcer.service.ArgException;
import sorcer.service.ArgSet;
import sorcer.service.Evaluation;
import sorcer.service.EvaluationException;
import sorcer.service.Identifiable;
import sorcer.service.Perturbation;
import sorcer.service.Setter;
import sorcer.service.SetterException;

/**
 * A variable has id, name and value. Its value can be evaluated by a related
 * Evaluator.
 *
 * @author Mike Sobolewski
 */
public interface Variability<T> extends Identifiable, Evaluation<T>, Setter, Perturbation<T>, Serializable {

	/**
	 * Types allow for grouping in the functional model so all variables of a
	 * particular type can be retrieved as a functional collection, e.g.,
	 * DESIGN, RESPONSE, DERIVATIVE or GRADIENT. Selected types can be used to
	 * define the nature of the variable (to the clients) by grouping them in in
	 * a list of types called kinds - addKind(Type). FUNDMENTAL - if scalar has
	 * meaning to the client
	 */
	public enum Type {
		INPUT, CONSTANT, INVARIANT, OUTPUT, RESPONSE, DESIGN, PARAMETER, LINKED, CONSTRAINT, OBJECTIVE, 
		DERIVATIVE, GRADIENT, RANDOM, BOUNDED, FUNDMENTAL, RAW, DELEGATION, COMPOSITION, 
		FILTER, PERSISTER, EVALUATOR, EVALUATION, PRODUCT, WATCHABLE, NONE
	}

	public enum MathType {
		CONTINUOUS, // ordered, element math defined as usual, continuous design
					// variable
		DISCRETE, // unordered, no element math defined, discrete design
					// variable
		DISCRETE_WITH_ORDER, // ordered, no element math defined, discrete
								// design variable
		DISCRETE_WITH_MATH, // ordered, element math defined, discrete design
							// variable
		DISCRETE_NO_ORDER, // non-ordered, element math undefined, discrete
							// design variable
		PROBLEM_PARAMETER, // ordered, element math defined as usual, continuous
							// problem parameter
		REAL, // ordered, element math defined as usual, continuous, element
				// class is Double, design variable
		LINEAR, // Linear function of the dependent variables
		
		QUADRATIC // Quadratic function of dependent variables
	}

	public Type getType();

	public ApplicationDescription getDescription();

	public void setValue(Object varValue) throws SetterException, RemoteException;

	public Class<?> getValueType();

	public ArgSet getArgs();

	public void addArgs(ArgSet set) throws EvaluationException;
	
	public T getArg(String varName) throws ArgException;
	
	public boolean isValueCurrent();

	public void valueChanged(Object obj) throws EvaluationException,
			RemoteException;

	void valueChanged() throws EvaluationException;
}
