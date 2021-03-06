/*
 *
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
package sorcer.core.exertion;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.jini.core.transaction.Transaction;
import sorcer.core.context.ServiceContext;
import sorcer.core.context.ThrowableTrace;
import sorcer.service.Condition;
import sorcer.service.Conditional;
import sorcer.service.ConditionalExertion;
import sorcer.service.Exertion;
import sorcer.service.ExertionException;
import sorcer.service.ServiceExertion;
import sorcer.service.SignatureException;
import sorcer.service.Task;

/**
 * The alternative Exertion that executes sequentially a collection of optional
 * exertions. It executes the first optExertion in the collection such that its
 * condition is true.
 * 
 * @author Mike Sobolewski
 */
@SuppressWarnings("unchecked")
public class AltExertion extends Task implements ConditionalExertion {

	private static final long serialVersionUID = 4012356285896459828L;
	
	protected List<OptExertion> optExertions;

	public AltExertion(OptExertion... optExertions) {
		super();
		this.optExertions = Arrays.asList(optExertions);
	}
	
	public AltExertion(String name, OptExertion... optExertions) {
		super(name);
		this.optExertions = Arrays.asList(optExertions);
	}

	public AltExertion(String name, List<OptExertion> optExertions) {
		super(name);
		this.optExertions = optExertions;
	}

	@Override
	public Task doTask(Transaction txn) throws ExertionException,
			SignatureException, RemoteException {
		OptExertion opt = null;
		try {
			for (int i = 0; i < optExertions.size(); i++) {
				opt = optExertions.get(i);
				if (opt.condition.isTrue()) {
					opt.isActive = true;
					opt.getTarget().getDataContext().append(opt.condition.getConditionalContext());					
					opt.setTarget(opt.getTarget().exert(txn));
					dataContext = (ServiceContext)opt.getTarget().getContext();
					controlContext.append(opt.getTarget().getControlContext());
					dataContext.putValue(Condition.CONDITION_VALUE, true);
					dataContext.putValue(Condition.CONDITION_TARGET, opt.getName());
					return this;
				}
			}
			dataContext.putValue(Condition.CONDITION_VALUE, false);
			dataContext.putValue(Condition.CONDITION_TARGET, opt.getName());
			dataContext.setExertion(null);
		} catch (Exception e) {
			throw new ExertionException(e);
		}
		return this;
	}
	
	public OptExertion getActiveOptExertion() {
		OptExertion active = null;
		for (OptExertion oe : optExertions) {
			if (oe.isActive())
				return oe;
		}
		return active;
	}
		
	public List<OptExertion> getOptExertions() {
		return optExertions;
	}

	public void setOptExertions(List<OptExertion> optExertions) {
		this.optExertions = optExertions;
	}

	public OptExertion getOptExertion(int index) {
		return optExertions.get(index);
	}
	
	public boolean isConditional() {
		return true;
	}
	
	public void reset(int state) {
		for(ServiceExertion e : optExertions)
			e.reset(state);
		
		this.setStatus(state);
	}
	
	/* (non-Javadoc)
	 * @see sorcer.service.Conditional#getConditions()
	 */
	@Override
	public List<Conditional> getConditions() {
		List<Conditional> cs = new ArrayList<Conditional>(optExertions.size());
		for (OptExertion oe : optExertions)
			cs.add(oe.getCondition());
		return cs;
	}

	@Override
	public List<ThrowableTrace> getExceptions(List<ThrowableTrace> exceptions) {
		for (Exertion ext : optExertions) {
			exceptions.addAll(((ServiceExertion)ext).getExceptions(exceptions));
		}
		exceptions.addAll(this.getExceptions());
		return exceptions;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see sorcer.service.Exertion#getExertions()
	 */
	@Override
	public List<Exertion> getExertions() {
		ArrayList<Exertion> list = new ArrayList<Exertion>(1);
		list.addAll(optExertions);
		return list;
	}
	
	public List<Exertion> getExertions(List<Exertion> exs) {
		for (Exertion e : optExertions)
			((ServiceExertion) e).getExertions(exs);
		exs.add(this);
		return exs;
	}
	
	/* (non-Javadoc)
	 * @see sorcer.service.ConditionalExertion#getTargets()
	 */
	@Override
	public List<Exertion> getTargets() {
		List<Exertion> tl = new ArrayList<Exertion>(optExertions.size());
		for (OptExertion oe : optExertions)
			tl.add(oe.getTarget());
		return tl;
	}
	
	/* (non-Javadoc)
	 * @see sorcer.service.Exertion#isCompound()
	 */
	@Override
	public boolean isCompound() {
		return false;
	}

}
