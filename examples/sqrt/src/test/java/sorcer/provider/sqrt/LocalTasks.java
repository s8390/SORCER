package sorcer.provider.sqrt;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.provider.sqrt.impl.SqrtImpl;
import sorcer.service.*;

import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.value;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/service")
public class LocalTasks {
	private final static Logger logger = Logger.getLogger(LocalTasks.class.getName());
	
	@Test
	public void exertTask() throws Exception  {

		Service t5 = service("t5", sig("add", SqrtImpl.class),
				cxt("add", inEnt("arg/x1", 2.0), inEnt("arg/x2", 2.0)));

		Service out = exert(t5);
		Context cxt = context(out);
		logger.info("out context: " + cxt);
		logger.info("context @ arg/x1: " + get(cxt, "arg/x1"));
		logger.info("context @ arg/x2: " + value(cxt, "arg/x2"));
		logger.info("context @ result/value: " + value(cxt, "result/value"));

		// get a single context argument
		assertEquals(2.0, value(cxt, "result/value"));

		// get the subcontext output from the context
		assertTrue(context(ent("arg/x1", 2.0), ent("result/value", 2.0)).equals(
				value(cxt, result("result/value", from("arg/x1", "result/value")))));

	}


	@Test
	public void evaluateTask() throws SignatureException, ExertionException, ContextException  {

		Task t5 = task("t5", sig("add", SqrtImpl.class),
				cxt("add", inEnt("arg/x1", 3.0), inEnt("arg/x2", 6.0), result("result/y")));

		// get the result value
		assertEquals(3.0, value(t5));

		// get the subcontext output from the exertion
		assertTrue(context(ent("arg/x1", 3.0), ent("result/z", 3.0)).equals(
				value(t5, result("result/z", from("arg/x1", "result/z")))));

	}

}
	
	
