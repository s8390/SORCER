/*
 * Copyright 2013 the original author or authors.
 * Copyright 2013 SorcerSoft.org.
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
package sorcer.core.context.model.par;

import sorcer.co.tuple.Entry;
import sorcer.co.tuple.EntryList;
import sorcer.core.SorcerConstants;
import sorcer.core.context.ApplicationDescription;
import sorcer.core.context.ServiceContext;
import sorcer.service.*;
import sorcer.service.modeling.Variability;
import sorcer.util.url.sos.SdbUtil;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.security.Principal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * In service-based modeling, a parameter (for short a par) is a special kind of
 * variable, used in a service context {@link ParModel} to refer to one of the
 * pieces of data provided as input to the invokers (subroutines of the
 * context). These pieces of data are called arguments.
 * 
 * @author Mike Sobolewski
 */
@SuppressWarnings({"unchecked", "rawtypes" })
public class Par<T> extends Entry<T> implements Variability<T>, Arg, Mappable<T>, Evaluation<T>, 
	Invocation<T>, Setter, Scopable, Comparable<T>, Reactive<T>, Serializable {

	private static final long serialVersionUID = 7495489980319169695L;
	 
	private static Logger logger = Logger.getLogger(Par.class.getName());

	protected final String name;
	
	private Principal principal;

	protected T value;

	protected Context<T> scope;

	// data store URL for this par
	private URL dbURL;

	// A context returning value at the path
	// that is this par name
	// Sorcer Mappable: Context, Exertion, or Var args
	protected Mappable mappable;

	protected String selectedFidelity;

	// par fidelities for this par
	protected Map<String, Object> fidelities;

	public Par(String parname) {
		super(parname);
		name = parname;
		value = null;
	}
	
	public Par(Identifiable identifiable) {
		super(identifiable.getName());
		name = identifiable.getName();
		value = (T)identifiable;
	}
	
	public Par(String path, T argument) throws EvaluationException {
		super(path);
		name = path;
		
		if (argument instanceof EntryList) {
			if (fidelities == null)
				fidelities = new HashMap<String, Object>();
			for (Entry e : (EntryList)argument) {
				fidelities.put(e.getName(), e);
			}
			
			Entry first = ((EntryList)argument).get(0);
			selectedFidelity = first.getName();
			value = (T)first;
		} else {
			value = argument;
		}
	}
	
	public Par(String path, Object argument, Context scope)
			throws RemoteException, ContextException {
		this(path, (T)argument);
		if (((ServiceContext)scope).containsKey(Condition._closure_))
			((ServiceContext) scope).remove(Condition._closure_);
		this.scope = scope;
		if (argument instanceof Scopable)
			((Scopable)argument).setScope(this.scope);
	}
	
	public Par(String name, String path, Service map) {
		this(name);
		value =  (T)path;
		mappable = (Mappable)map;
	}
	
	/* (non-Javadoc)
	 * @see sorcer.service.Evaluation#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	public void setValue(Object value) throws SetterException, RemoteException {
		if (isPersistent && mappable == null) {
			try {
				if (SdbUtil.isSosURL(this.value)) {
					SdbUtil.update((URL)this.value, value);
				} else  {
					this.value = (T)SdbUtil.store(value);
				}
			} catch (Exception e) {
				throw new SetterException(e);
			}
			return;
		}
		if (mappable != null && this.value instanceof String ) {
			try {
				Object val = mappable.asis((String)this.value);
				if (val instanceof Par) {
					((Par)val).setValue(value);
				} else if (isPersistent) {
					if (SdbUtil.isSosURL(val)) {
						SdbUtil.update((URL)val, value);
					} else {
						URL url = SdbUtil.store(value);
						Par p = new Par((String)this.value, url);
						p.setPersistent(true);
						if (mappable instanceof ServiceContext)
							((ServiceContext)mappable).put((String)this.value, p);
						else
							mappable.putValue((String)this.value, p);
					}
				} else {
					mappable.putValue((String)this.value, value);
				}
			} catch (Exception e) {
				throw new SetterException(e);
			}
		}
		else
			this.value = (T)value;
	}

	@Override
	public T value() {
		return value;
	}

	/* (non-Javadoc)
         * @see sorcer.service.Evaluation#getAsis()
         */
	@Override
	public T asis() throws EvaluationException, RemoteException {
		return value;
	}
	
	/* (non-Javadoc)
	 * @see sorcer.service.Evaluation#getValue(sorcer.co.tuple.Parameter[])
	 */
	@Override
	public T getValue(Arg... entries) throws EvaluationException,
	RemoteException {
		T val = null;
		try {
			substitute(entries);
			if (selectedFidelity != null) {
				Object obj = fidelities.get(selectedFidelity);
				if (!isFidelityValid(obj)) {
					obj = scope.asis(selectedFidelity);
				}
				value = (T)obj;
			}
			if (mappable != null && value instanceof String) {
				Object obj = mappable.asis((String) value);
				if (obj instanceof Par && ((Par)obj).isPersistent())
					return (T)((Par)obj).getValue();
				else
					val = (T) mappable.getValue((String) value);
			} else if (value == null && scope != null) {
				val = (T) ((ServiceContext<T>) scope).get(name);
			} else {
				val = value;
			}
			if (val instanceof Evaluation) {
				if (val instanceof Par && ((Par)val).asis() == null && value == null) {
					logger.warning("undefined par: " + val);
					return null;
				}
				// direct scope
				if (val instanceof Scopable && ((Scopable)val).getScope() != null) {
					((Context)((Scopable)val).getScope()).append(scope);
				}

				// indirect scope for enty values
				if (val instanceof Entry) {
					Object ev = ((Entry)val).asis();
					if (ev instanceof Scopable && ((Scopable)ev).getScope() != null) {
						((Context)((Scopable)ev).getScope()).append(scope);
					}
				}

				if (val instanceof Exertion) {
					// TODO context binding for all exertions, works for tasks only
					Context cxt = ((Exertion)val).getDataContext();
					List<String> paths = ((ServiceContext)cxt).getPaths();
					for (String an : ((Map<String, Object>)scope).keySet()) {
						for (String p : paths) {
							if (p.endsWith(an)) {
								cxt.putValue(p, scope.getValue(an));
								break;
							}
						}
					}
				}
				val = ((Evaluation<T>) val).getValue(entries);
			}

			if (isPersistent) {
				if (SdbUtil.isSosURL(val)) {
					val = (T) ((URL) val).getContent();
				} else {
					URL url = SdbUtil.store(val);
					Par p = null;
					if (mappable != null && this.value instanceof String
							&& mappable.asis((String) this.value) != null) {
						p = new Par((String) this.value, url);
						p.setPersistent(true);
						mappable.putValue((String) this.value, p);
					} else if (this.value instanceof Identifiable) {
						p = new Par((String) ((Identifiable) this.value).getName(), url);
						p.setPersistent(true);
					} else {
						this.value = (T)url;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new EvaluationException(e);
		}
		return val;
	}
	
	/* (non-Javadoc)
	 * @see sorcer.service.Evaluation#substitute(sorcer.co.tuple.Parameter[])
	 */
	@Override
	public Evaluation<T> substitute(Arg... parameters) throws SetterException,
			RemoteException {
		if (parameters == null)
			return this;
		for (Arg p : parameters) {
			try {
				if (p instanceof Par) {
					if (name.equals(((Par<T>) p).name)) {
						value = ((Par<T>) p).value;
						if (((Par<T>) p).getScope() != null)
							scope.append(((Par<T>) p).getScope());

					}
				} else if (p instanceof SelectionFidelity && fidelities != null) {
					selectedFidelity = p.getName();
				} else if (p instanceof Context) {
					if (scope == null)
						scope = (Context) p;
					else
						scope.append((Context) p);
				}
			} catch (ContextException e) {
				e.printStackTrace();
				throw new SetterException(e);
			}
		}
		return this;
	}

	private boolean isFidelityValid(Object fidelity) throws EvaluationException {
		if (fidelity == null || fidelity == Context.none)
			return false;
		if (fidelity instanceof Entry) {
			Object obj = null;
			try {
				obj = ((Entry)fidelity).asis();
			} catch (RemoteException e) {
				throw new EvaluationException(e);
			}
			if (obj == null || obj == Context.none) return false;
		}
		 return true;
	}

	public Context getScope() {
		return scope;
	}

	public void setScope(Context scope) {
		if (((ServiceContext)scope).containsKey(Condition._closure_))
			((ServiceContext) scope).remove(Condition._closure_);
		this.scope = scope;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(T o) {
		if (o == null)
			throw new NullPointerException();
		if (o instanceof Par<?>)
			return name.compareTo(((Par<?>) o).getName());
		else
			return -1;
	}
	
	@Override
	public String toString() {
		return "par [name: " + name + ", value: " + value + ", path: "+_1+"]";
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Perturbation#getPerturbedValue(java.lang.String)
	 */
	@Override
	public T getPerturbedValue(String varName) throws EvaluationException,
			RemoteException {
		return null;
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Perturbation#getPerturbation()
	 */
	@Override
	public double getPerturbation() {
		return 0;
	}

	/* (non-Javadoc)
	 * @see sorcer.vfe.Variability#getType()
	 */
	@Override
	public Type getType() {
		return Type.PARAMETER;
	}

	/* (non-Javadoc)
	 * @see sorcer.vfe.Variability#getDescription()
	 */
	@Override
	public ApplicationDescription getDescription() {
		return null;
	}

	/* (non-Javadoc)
	 * @see sorcer.vfe.Variability#getValueType()
	 */
	@Override
	public Class getValueType() {
		return null;
	}

	/* (non-Javadoc)
	 * @see sorcer.vfe.Variability#getArgs()
	 */
	@Override
	public ArgSet getArgs() {
		return null;
	}

	/* (non-Javadoc)
	 * @see sorcer.vfe.Variability#getArg(java.lang.String)
	 */
	@Override
	public T getArg(String varName) throws ArgException {
		try {
			return (T) scope.getValue(varName);
		} catch (ContextException e) {
			throw new ArgException(e);
		}
	}

	/**
	 * <p>
	 * Returns a Contextable (Context or Exertion) of this Par that by a its
	 * name provides values of this Par.
	 * </p>
	 * 
	 * @return the contextable
	 */
	public Mappable getContextable() {
		return mappable;
	}

	/* (non-Javadoc)
	 * @see sorcer.vfe.Variability#isValueCurrent()
	 */
	@Override
	public boolean isValueCurrent() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see sorcer.vfe.Variability#valueChanged(java.lang.Object)
	 */
	@Override
	public void valueChanged(Object obj) throws EvaluationException,
			RemoteException {		
	}

	/* (non-Javadoc)
	 * @see sorcer.vfe.Variability#valueChanged()
	 */
	@Override
	public void valueChanged() throws EvaluationException {		
	}


	public Principal getPrincipal() {
		return principal;
	}

	public URL getDbURL() throws MalformedURLException {
		URL url = null;
		if (dbURL != null)
			url = dbURL;
		else if (((ServiceContext)scope).getDbUrl() != null)
			url = new URL(((ServiceContext)scope).getDbUrl());
		
		return url;
	}

	public URL getURL() throws ContextException {
		if (isPersistent) {
			if (mappable != null)
				return (URL)mappable.asis((String)value);
			else
				return (URL)value;
		}
		return null;
	}
	
	public void setDbURL(URL dbURL) {
		this.dbURL = dbURL;
	}
	
	/* (non-Javadoc)
	 * @see sorcer.vfe.Persister#isPersistable()
	 */
	@Override
	public boolean isPersistent() {
		return isPersistent;
	}

	public void setPersistent(boolean state) {
		isPersistent = state;
	}
	
	public Mappable getMappable() {
		return mappable;
	}

	public void setMappable(Mappable mappable) {
		this.mappable = mappable;
	}
	
	public boolean isMappable() {
		return (mappable != null);
	}

	public Map<String, Object> getFidelities() {
		return fidelities;
	}

	public String getSelectedFidelity() {
		return selectedFidelity;
	}

	public void setSelectedFidelity(String selectedFidelity) {
		this.selectedFidelity = selectedFidelity;
	}
	
	/* (non-Javadoc)
	 * @see sorcer.service.Invocation#invoke(sorcer.service.Context, sorcer.service.Arg[])
	 */
	@Override
	public T invoke(Context context, Arg... entries) throws RemoteException,
			InvocationException {
		try {
			if (context != null)
				scope.append(context);
			return getValue(entries);
		} catch (Exception e) {
			throw new InvocationException(e);
		}
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Mappable#getValue(java.lang.String, sorcer.service.Arg[])
	 */
	@Override
	public T getValue(String path, Arg... args) throws ContextException {
		String[] attributes = path.split(SorcerConstants.CPS);
		if (attributes[0].equals(name)) {
			if (attributes.length == 1)
				try {
					return (T)getValue(args);
				} catch (RemoteException e) {
					throw new ContextException(e);
				}
			else if (mappable != null)
				return (T)mappable.getValue(path.substring(name.length()), args);
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Mappable#asis(java.lang.String)
	 */
	@Override
	public T asis(String path) throws ContextException {
		String[] attributes = path.split(SorcerConstants.CPS);
		if (attributes[0].equals(name)) {
			if (attributes.length == 1)
				return value;
			else if (mappable != null)
				return (T)mappable.asis(path.substring(name.length()));
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Mappable#putValue(java.lang.String, java.lang.Object)
	 */
	@Override
	public T putValue(String path, Object value) throws ContextException {
		String[] attributes = path.split(SorcerConstants.CPS);
		if (attributes[0].equals(name)) {
			if (attributes.length == 1)
				this.value = (T)value;
			else if (mappable != null)
				mappable.putValue(path.substring(name.length()), value);
		}
		return (T)value;	
	}

	/* (non-Javadoc)
	 * @see sorcer.core.context.model.Variability#addArgs(ArgSet set)
	 */
	@Override
	public void addArgs(ArgSet set) throws EvaluationException {
		Iterator<Arg> i = set.iterator();
		while (i.hasNext()) {
			Par par = (Par)i.next();
			try {
				putValue(par.getName(), par.asis());
			} catch (Exception e) {
				throw new EvaluationException(e);
			} 
		}
		
	}
	
	@Override
	public int hashCode() {
		int hash = name.length() + 1;
		return hash = hash * 31 + name.hashCode();
	}
	
	@Override
	public boolean equals(Object object) {
		if (object instanceof Par
				&& ((Par) object).name.equals(name))
			return true;
		else
			return false;
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Scopable#setScope(java.lang.Object)
	 */
	public void setScope(Object scope) throws RemoteException {
		this.scope = (Context)scope;
		
	}

	public void putFidelity(EntryList fidelity) throws EvaluationException,
			RemoteException {
		if (fidelities == null)
			fidelities = new HashMap<String, Object>();
		for (Entry e : fidelity)
			fidelities.put(e.getName(), e.asis());
	}

	public void addFidelity(EntryList fidelity) throws EvaluationException,
			RemoteException {
		putFidelity(fidelity);
	}

	public void selectFidelity(String name) throws ParException {
		if (fidelities.containsKey(name))
			value = (T) fidelities.get(name);
		else
			throw new ParException("no such service fidelity: " + name + " at: " + this);
	}

	public void setFidelities(Map<String, Object> fidelities) {
		this.fidelities = fidelities;
	}
	

	@Override
	public boolean isReactive() {
		return true;
	}
}
