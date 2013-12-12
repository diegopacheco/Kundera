/*******************************************************************************
 * * Copyright 2012 Impetus Infotech.
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
package com.impetus.client.cassandra.pelops;

import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.PersistenceException;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EmbeddableType;

import net.dataforte.cassandra.pool.HostFailoverPolicy;
import net.dataforte.cassandra.pool.PoolConfiguration;

import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.BytesType;
import org.apache.cassandra.db.marshal.ListType;
import org.apache.cassandra.db.marshal.MapType;
import org.apache.cassandra.db.marshal.SetType;
import org.scale7.cassandra.pelops.SimpleConnectionAuthenticator;
import org.scale7.cassandra.pelops.pool.CommonsBackedPool;
import org.scale7.cassandra.pelops.pool.CommonsBackedPool.Policy;
import org.scale7.cassandra.pelops.pool.IThriftPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.impetus.client.cassandra.schemamanager.CassandraValidationClassMapper;
import com.impetus.client.cassandra.service.CassandraHost;
import com.impetus.kundera.metadata.model.EntityMetadata;
import com.impetus.kundera.metadata.model.MetamodelImpl;
import com.impetus.kundera.metadata.model.attributes.AbstractAttribute;
import com.impetus.kundera.property.PropertyAccessException;
import com.impetus.kundera.property.PropertyAccessorHelper;
import com.impetus.kundera.property.accessor.BigDecimalAccessor;
import com.impetus.kundera.property.accessor.IntegerAccessor;

/**
 * The Class PelopsUtils.
 */
public class PelopsUtils
{

    /** The logger. */
    private static Logger logger = LoggerFactory.getLogger(PelopsUtils.class);

    /**
     * Generate pool name.
     * 
     * @param persistenceUnit
     *            the persistence unit
     * @param puProperties
     * @return the string
     */
    public static String generatePoolName(String node, int port, String keyspace)
    {
        return node + ":" + port + ":" + keyspace;
    }

    /**
     * Gets the pool config policy.
     * 
     * @param persistenceUnitMetadata
     *            the persistence unit metadata
     * @param puProperties
     * @return the pool config policy
     */
    public static Policy getPoolConfigPolicy(CassandraHost cassandraHost)
    {
        Policy policy = new Policy();
        if (cassandraHost.getMaxActive() > 0)
        {
            policy.setMaxActivePerNode(cassandraHost.getMaxActive());
        }
        if (cassandraHost.getMaxIdle() > 0)
        {
            policy.setMaxIdlePerNode(cassandraHost.getMaxIdle());
        }
        if (cassandraHost.getMinIdle() > 0)
        {
            policy.setMinIdlePerNode(cassandraHost.getMinIdle());
        }
        if (cassandraHost.getMaxTotal() > 0)
        {
            policy.setMaxTotal(cassandraHost.getMaxTotal());
        }
        return policy;
    }

    /**
     * Gets the pool config policy.
     * 
     * @param persistenceUnitMetadata
     *            the persistence unit metadata
     * @param puProperties
     * @return the pool config policy
     */
    public static PoolConfiguration setPoolConfigPolicy(CassandraHost cassandraHost, PoolConfiguration prop)
    {
        int maxActivePerNode = cassandraHost.getMaxActive();
        int maxIdlePerNode = cassandraHost.getMaxIdle();
        int minIdlePerNode = cassandraHost.getMinIdle();
        int maxTotal = cassandraHost.getMaxTotal();
        boolean testOnBorrow = cassandraHost.isTestOnBorrow();
        boolean testWhileIdle = cassandraHost.isTestWhileIdle();
        boolean testOnConnect = cassandraHost.isTestOnConnect();
        boolean testOnReturn = cassandraHost.isTestOnReturn();
        int socketTimeOut = cassandraHost.getSocketTimeOut();
        int maxWaitInMilli = cassandraHost.getMaxWait();
        HostFailoverPolicy paramHostFailoverPolicy = cassandraHost.getHostFailoverPolicy();
        if (maxActivePerNode > 0)
        {
            prop.setInitialSize(maxActivePerNode);
            prop.setMaxActive(maxActivePerNode);
        }
        if (maxIdlePerNode > 0)
        {
            prop.setMaxIdle(maxIdlePerNode);
        }
        if (minIdlePerNode > 0)
        {
            prop.setMinIdle(minIdlePerNode);
        }
        if (maxTotal > 0)
        {
            prop.setMaxActive(maxTotal);
        }
        if (cassandraHost.getUser() != null)
        {
            prop.setUsername(cassandraHost.getUser());
            prop.setPassword(cassandraHost.getPassword());
        }

        prop.setSocketTimeout(socketTimeOut);
        prop.setTestOnBorrow(testOnBorrow);
        prop.setTestOnConnect(testOnConnect);
        prop.setTestOnReturn(testOnReturn);
        prop.setTestWhileIdle(testWhileIdle);
        prop.setFailoverPolicy(paramHostFailoverPolicy);
        prop.setMaxWait(maxWaitInMilli);
        return prop;
    }

    /**
     * If userName and password provided, Method prepares for
     * AuthenticationRequest.
     * 
     * @param props
     *            properties
     * 
     * @return simple authenticator request. returns null if userName/password
     *         are not provided.
     * 
     */
    public static SimpleConnectionAuthenticator getAuthenticationRequest(String userName, String password)
    {
        SimpleConnectionAuthenticator authenticator = null;
        if (userName != null || password != null)
        {
            authenticator = new SimpleConnectionAuthenticator(userName, password);
        }
        return authenticator;
    }

    /**
     * 
     * @param host
     * @param port
     * @return
     */
    public static boolean verifyConnection(String host, int port)
    {
        Socket socket = null;
        try
        {
            socket = new Socket(host, port);
            socket.setReuseAddress(true);
            socket.setSoLinger(true, 0);
            boolean isConnected = socket.isConnected();
            return isConnected;
        }
        catch (UnknownHostException e)
        {
            logger.warn("{}:{} is still down", host, port);
            return false;
        }
        catch (IOException e)
        {
            logger.warn("{}:{} is still down", host, port);
            return false;
        }
        finally
        {
            try
            {
                if (socket != null)
                {
                    socket.close();
                }
            }
            catch (IOException e)
            {
                logger.warn("{}:{} is still down", host, port);
            }
        }
    }

    /**
     * 
     * @param pool
     * @return
     */
    public static String getPoolName(IThriftPool pool)
    {
        org.scale7.cassandra.pelops.Cluster.Node[] nodes = ((CommonsBackedPool) pool).getCluster().getNodes();
        String poolName = PelopsUtils.generatePoolName(nodes[0].getAddress(), ((CommonsBackedPool) pool).getCluster()
                .getConnectionConfig().getThriftPort(), ((CommonsBackedPool) pool).getKeyspace());
        return poolName;
    }

    /**
     * Initialize.
     * 
     * @param tr
     *            the tr
     * @param m
     *            the m
     * @param entity
     *            the entity
     * @param tr
     * @return the object
     * @throws InstantiationException
     *             the instantiation exception
     * @throws IllegalAccessException
     *             the illegal access exception
     */
    public static Object initialize(EntityMetadata m, Object entity, Object id)
    {
        try
        {
            if (entity == null)
            {
                entity = m.getEntityClazz().newInstance();
            }
            if (id != null)
            {
                PropertyAccessorHelper.setId(entity, m, id);
            }
            return entity;
        }
        catch (Exception e)
        {
            throw new PersistenceException("Error occured while instantiating entity.", e);
        }
    }
}
