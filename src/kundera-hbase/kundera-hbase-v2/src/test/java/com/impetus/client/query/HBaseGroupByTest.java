/*******************************************************************************
 *  * Copyright 2015 Impetus Infotech.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 ******************************************************************************/
package com.impetus.client.query;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Persistence;

import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.impetus.kundera.client.query.GroupByBaseTest;

/**
 * The Class HBaseGroupByTest.
 * 
 * @author: karthikp.manchala
 */
public class HBaseGroupByTest extends GroupByBaseTest
{
    /** The node. */
    private static Node node = null;

    /** The property map. */
    private static Map<String, String> propertyMap = new HashMap<String, String>();

    /**
     * Sets the up before class.
     * 
     * @throws Exception
     *             the exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception
    {
        if (!checkIfServerRunning())
        {
            ImmutableSettings.Builder builder = ImmutableSettings.settingsBuilder();
            builder.put("path.data", "target/data");
            node = new NodeBuilder().settings(builder).node();
        }
        propertyMap.put("kundera.indexer.class", "com.impetus.client.es.index.ESIndexer");
        emf = Persistence.createEntityManagerFactory("queryTest", propertyMap);
        em = emf.createEntityManager();
        init();
    }

    /**
     * Test aggregation.
     */
    @Test
    public void test()
    {
        testAggregation();
    }

    /**
     * Tear down after class.
     * 
     * @throws Exception
     *             the exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception
    {
        em.createQuery("Delete from Person p").executeUpdate();
        waitThread();

        em.close();
        emf.close();

        if (node != null)
            node.close();
    }

}