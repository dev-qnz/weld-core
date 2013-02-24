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
package org.jboss.weld.bean;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanAttributes;

import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.util.bean.ForwardingBeanAttributes;
import org.jboss.weld.util.reflection.Reflections;

/**
 * Common superclass for beans that are identified using id.
 *
 * @author Jozef Hartinger
 * @author Pete Muir
 *
 */
public abstract class CommonBean<T> extends ForwardingBeanAttributes<T> implements Bean<T> {

    public static final String BEAN_ID_PREFIX = RIBean.class.getPackage().getName();

    public static final String BEAN_ID_SEPARATOR = "-";

    private final String id;

    private final int hashCode;

    private volatile BeanAttributes<T> attributes;

    protected CommonBean(BeanAttributes<T> attributes, String idSuffix, BeanManagerImpl beanManager) {
        this.attributes = attributes;
        this.id = new StringBuilder().append(BEAN_ID_PREFIX).append(BEAN_ID_SEPARATOR).append(beanManager.getId()).append(BEAN_ID_SEPARATOR).append(idSuffix).toString();
        this.hashCode = this.id.hashCode();
    }

    protected Object unwrap(Object object) {
        if (object instanceof ForwardingBean<?>) {
            return Reflections.<ForwardingBean<?>> cast(object).delegate();
        }
        if (object instanceof ForwardingInterceptor<?>) {
            return Reflections.<ForwardingInterceptor<?>> cast(object).delegate();
        }
        if (object instanceof ForwardingDecorator<?>) {
            return Reflections.<ForwardingDecorator<?>> cast(object).delegate();
        }
        return object;
    }

    @Override
    public boolean equals(Object obj) {
        Object object = unwrap(obj);
        if (object instanceof CommonBean<?>) {
            CommonBean<?> that = (CommonBean<?>) object;
            return this.getId().equals(that.getId());
        } else {
            return false;
        }
    }

    protected BeanAttributes<T> attributes() {
        return attributes;
    }

    public void setAttributes(BeanAttributes<T> attributes) {
        this.attributes = attributes;
    }

    @Override
    public boolean isNullable() {
        /*
         * This is deprecated and the return type does not really matter anymore.
         * @see CDI-247
         */
        return false;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return id;
    }
}
