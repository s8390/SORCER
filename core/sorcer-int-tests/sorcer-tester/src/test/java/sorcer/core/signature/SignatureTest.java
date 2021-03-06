package sorcer.core.signature;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.tester.provider.Adder;
import sorcer.arithmetic.tester.provider.impl.AdderImpl;
import sorcer.service.Context;
import sorcer.service.Service;
import sorcer.service.Signature;

import java.lang.reflect.Proxy;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Logger;

import static org.junit.Assert.*;
import static sorcer.co.operator.inEnt;
import static sorcer.eo.operator.*;


/**
 * @author Mike Sobolewski
 */
@RunWith(SorcerTestRunner.class)
@ProjectContext("core/sorcer-int-tests/sorcer-tester")
public class SignatureTest {
	private final static Logger logger = Logger
			.getLogger(SignatureTest.class.getName());

	@Test
	public void newInstance() throws Exception {

		// Object orientation
		Signature s = sig("new", Date.class);
		// create a new instance
		Object obj = instance(s);
		logger.info("provider of s: " + obj);
		assertTrue(obj instanceof Date);

	}


	@Test
	public void referencingInstances() throws Exception {

		Object obj = new Date();
		Signature s = sig("getTime", obj);

		// get service provider - a given object
		Object prv = provider(s);
		logger.info("provider of s: " + prv);
		assertTrue(prv instanceof Date);

		logger.info("getTime: " + value(service("gt", s)));
		assertTrue(value(service("gt", s)) instanceof Long);

	}


	@Test
	public void referencingClassWithConstructor() throws Exception {

		Signature s = sig("getTime", Date.class);

		// get service provider for signature
		Object prv = provider(s);
		logger.info("provider of s: " + prv);
		logger.info("selector of s: " + selector(s));
		logger.info("service type of s: " + type(s));
		assertTrue(prv instanceof Date);
		service("time", s);

		logger.info("time: " + value(service("time", s)));
		assertTrue(value(service("time", s)) instanceof Long);

	}


	@Test
	public void referencingUtilityClass() throws Exception {

		Signature ms = sig(Math.class, "random");
		Object prv = provider(ms);
		logger.info("provider of s: " + prv);
		assertTrue(prv == Math.class);

		logger.info("random: " + value(service("random", ms)));
		assertTrue(value(service("random", ms)) instanceof Double);

		ms = sig(Math.class, "max");
		Context cxt = context(
				parameterTypes(new Class[] { double.class, double.class }),
				args(new Object[] { 200.11, 3000.0 }));

		// request the service
		logger.info("max: " + value(service("max", ms, cxt)));
		assertTrue(value(service("max", ms, cxt)) instanceof Double);
		assertTrue(value(service("max", ms, cxt)).equals(3000.0));

	}


	@Test
	public void referencingFactoryClass() throws Exception {

		Signature ps = sig("get", Calendar.class, "getInstance");

		Context cxt = context(
				parameterTypes(new Class[] { int.class }),
				args(new Object[] { Calendar.MONTH }));

		// get service provider for signature
		Object prv = provider(ps);
		logger.info("prv: " + prv);
		assertTrue(prv instanceof Calendar);

		// request the service
		logger.info("time: " + value(service("month", ps, cxt)));
		assertTrue(value(service("month", ps, cxt)) instanceof Integer);
		assertTrue(value(service("month", ps, cxt)).equals(((Calendar)prv).get(Calendar.MONTH)));

	}


	@Test
	public void localService() throws Exception  {

		Signature lps = sig("add", AdderImpl.class);
		Object prv = provider(lps);
		assertTrue(prv instanceof AdderImpl);
		assertFalse(prv instanceof Proxy);

		// request the local service
		Service as = service("as", lps,
				context("add",
						inEnt("arg/x1", 20.0),
						inEnt("arg/x2", 80.0),
						result("result/y")));

		assertEquals(100.0, value(as));

	}


	@Test
	public void referencingRemoteProvider() throws Exception  {

		Signature rps = sig("add", Adder.class);
		Object prv = provider(rps);
		logger.info("provider of rps: " + prv);
		assertTrue(prv instanceof Adder);
		assertTrue(prv instanceof Proxy);

		// request the local service
		Service as = service("as", rps,
				context("add",
						inEnt("arg/x1", 20.0),
						inEnt("arg/x2", 80.0),
						result("result/y")));

		assertEquals(100.0, value(as));

	}


	@Test
	public void referencingNamedRemoteProvider() throws Exception  {

		Signature ps = sig("add", Adder.class, prvName("Adder"));
		Object prv = provider(ps);
		logger.info("provider of ps: " + prv);
		assertTrue(prv instanceof Adder);
		assertTrue(prv instanceof Proxy);

		// request the local service
		Service as = service("as", ps,
				context("add",
						inEnt("arg/x1", 20.0),
						inEnt("arg/x2", 80.0),
						result("result/y")));

		assertEquals(100.0, value(as));
	}

}