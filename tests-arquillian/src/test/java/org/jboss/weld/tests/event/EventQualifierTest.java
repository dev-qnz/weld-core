/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.weld.tests.event;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.BeanArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.weld.test.util.Utils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class EventQualifierTest {
    @Deployment
    public static Archive<?> deploy() {
        return ShrinkWrap.create(BeanArchive.class, Utils.getDeploymentNameAsHash(EventQualifierTest.class))
                .addPackage(EventQualifierTest.class.getPackage());
    }

    /*
     * description = "WELD-226"
     */
    @Test
    public void testDefaultQualifierNotRequired(Bar bar) {
        bar.fireWithNoQualifiers();
        Assert.assertTrue(bar.isUnqualifiedObserved());
        Assert.assertFalse(bar.isUpdatedObserved());
        bar.reset();
        bar.fireWithNoQualifiersViaManager();
        Assert.assertTrue(bar.isUnqualifiedObserved());
        Assert.assertFalse(bar.isUpdatedObserved());
        bar.reset();
        bar.fireWithUpdatedQualifierViaAnnotation();
        Assert.assertTrue(bar.isUnqualifiedObserved());
        Assert.assertTrue(bar.isUpdatedObserved());
        bar.reset();
        bar.fireWithUpdatedQualifierViaManager();
        Assert.assertTrue(bar.isUpdatedObserved());
        Assert.assertTrue(bar.isUnqualifiedObserved());
        bar.reset();
        bar.fireWithUpdatedQualifierViaSelect();
        Assert.assertTrue(bar.isUnqualifiedObserved());
        Assert.assertTrue(bar.isUpdatedObserved());
    }

}
