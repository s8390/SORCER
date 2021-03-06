/*
 * Copyright to the original author or authors.
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
package sorcer.core.deploy;

import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.core.SorcerConstants;
import sorcer.core.provider.Provider;
import sorcer.service.*;
import sorcer.util.ProviderLookup;
import sorcer.util.Sorcer;

import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static sorcer.eo.operator.*;

/**
 * @author Dennis Reedy
 */
@RunWith(SorcerTestRunner.class)
@ProjectContext("core/sorcer-int-tests/deploy-tests")
public class DeployExertionTest2 extends DeploySetup implements SorcerConstants {
    private final static Logger logger = Logger.getLogger(DeployExertionTest.class.getName());

    @Test
    public void deployAndExec() throws Exception {
    	long t0 = System.currentTimeMillis();
        Provider provider = null;
        while(provider==null)
            provider = (Provider)ProviderLookup.getService(Provider.class);
		Assert.assertNotNull(provider);
        System.out.println("Waited "+(System.currentTimeMillis()-t0)+" millis for [Sorcer OS] provisioning");
        
        Job f1 = JobUtil.createJob();
        Assert.assertTrue(f1.isProvisionable());
        verifyExertion(f1);
        /* Run it again to make sure that the existing deployment is used */
        verifyExertion(f1);
    }

    private void verifyExertion(Job job) throws ExertionException, ContextException {
    	long t0 = System.currentTimeMillis();
        Exertion out = exert(job);
        System.out.println("Waited "+(System.currentTimeMillis()-t0)+" millis for exerting: " + out.getName());
        assertNotNull(out);
        assertEquals(get(out, "f1/f3/result/y3"), 400.0);
    }

}
