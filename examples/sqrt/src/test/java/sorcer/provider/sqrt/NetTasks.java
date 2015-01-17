package sorcer.provider.sqrt;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.service.*;
import sorcer.service.Strategy.Access;
import sorcer.service.Strategy.Wait;

import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/service")
public class NetTasks {
	private final static Logger logger = Logger.getLogger(NetTasks.class.getName());
	
	@Test
	public void exertTask() throws Exception  {

		Task t5 = srv("t5", sig("add", Sqrt.class),
				cxt("add", inEnt("arg/x1", 2.0), inEnt("arg/x2", 2.0), result("result/y")));

		Exertion out = exert(t5);
		Context cxt = context(out);
		logger.info("out context: " + cxt);
		logger.info("context @ arg/x1: " + value(cxt, "arg/x1"));
		logger.info("context @ arg/x2: " + value(cxt, "arg/x2"));
		logger.info("context @ result/y: " + value(cxt, "result/y"));

		// get a single context argument
		assertEquals(2.0, value(cxt, "result/y"));

		// get the subcontext output from the context
		assertTrue(context(ent("arg/x1", 2.0), ent("result/y", 2.0)).equals(
				value(cxt, result("result/context", from("arg/x1", "result/y")))));
	}

	@Test
	public void valueTask() throws SignatureException, ExertionException, ContextException  {

		Task t5 = srv("t5", sig("add", Sqrt.class),
				cxt("add", inEnt("arg/x1", 3.0), inEnt("arg/x2", 6.0), result("result/y")));

		// get the result value
		assertEquals(3.0, value(t5));

		// get the subcontext output from the exertion
		assertTrue(context(ent("arg/x1", 3.0), ent("result/z", 3.0)).equals(
				value(t5, result("result/z", from("arg/x1", "result/z")))));

	}

	@Test
	public void spaceTask() throws Exception {

		Task t5 = task(
				"t5",
				sig("add", Sqrt.class),
				context("add", inEnt("arg/x1", 2.0),
						inEnt("arg/x2", 2.0), outEnt("result/y")),
				strategy(Access.PULL, Wait.YES));

		t5 = exert(t5);
		logger.info("t5 context: " + context(t5));
		logger.info("t5 value: " + get(t5, "result/y"));
		assertEquals(get(t5, "result/y"), 2.0);
	}


}
	
	
