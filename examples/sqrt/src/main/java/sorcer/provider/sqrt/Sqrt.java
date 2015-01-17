package sorcer.provider.sqrt;

import sorcer.service.Context;
import sorcer.service.ContextException;

import java.rmi.RemoteException;

@SuppressWarnings("rawtypes")
public interface Sqrt {

	public Context add(Context context) throws RemoteException, ContextException;
}
