package sorcer.provider.sqrt.impl;

import sorcer.core.context.PositionalContext;
import sorcer.core.context.ServiceContext;
import sorcer.core.provider.Provider;
import sorcer.provider.sqrt.Sqrt;
import sorcer.service.Context;
import sorcer.service.ContextException;

import java.rmi.RemoteException;
import java.util.List;
import java.util.logging.Logger;
import java.lang.Math;

@SuppressWarnings("rawtypes")
public class SqrtImpl implements Sqrt {
	public static final String RESULT_PATH = "result/value";
	private Provider provider;
	private static Logger logger = Logger.getLogger(SqrtImpl.class.getName());
	
	public void init(Provider provider) {
		this.provider = provider;
		try {
			logger = provider.getLogger();
		} catch (RemoteException e) {
			// ignore it, local call
		}
	}
	
	public Context add(Context context) throws RemoteException, ContextException {
		// get inputs and outputs from the service context
		PositionalContext cxt = (PositionalContext) context;
		List<Double> inputs = cxt.getInValues();
		logger.info("inputs: " + inputs);
		List<String> outpaths = cxt.getOutPaths();
		logger.info("outpaths: " + outpaths);

		// calculate the result
		Double result = 0.0;
		//for (Double value : inputs)
		//	result += value; 
		result = Math.sqrt(inputs.get(0)+inputs.get(1));

		logger.info("result: " + result);
		
		// update the service context
		if (provider != null)
			cxt.putValue("calculated/provider", provider.getProviderName());
		else
			cxt.putValue("calculated/provider", getClass().getName());
		if (((ServiceContext)context).getReturnPath() != null) {
			((ServiceContext)context).setReturnValue(result);
		} else if (outpaths.size() == 1) {
			// put the result in the existing output path
			cxt.putValue(outpaths.get(0), result);
		} else {
			cxt.putValue(RESULT_PATH, result);
		}

		// get a custom provider property
		if (provider != null) {
			try {
				int st = new Integer(provider.getProperty("provider.sleep.time"));
				if (st > 0) {
					Thread.sleep(st);
					logger.info("slept for: " + st);
					cxt.putValue("provider/slept/ms", st);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		return cxt;
	}

}
