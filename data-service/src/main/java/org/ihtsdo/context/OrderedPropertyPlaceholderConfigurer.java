package org.ihtsdo.context;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.ApplicationContextException;
import org.springframework.core.Ordered;
import org.springframework.core.io.support.PropertiesLoaderSupport;

import java.io.IOException;
import java.util.Properties;

//import org.springframework.beans.PropertyAccessException;

/**
 * Ordered gets initialized after the PriorityOrdered implementations (i.e. PropertyPlaceholderConfigurer).
 * This ensures that the properties that this class depends on have already been replaced.
 *
 * @author http://forum.spring.io/forum/spring-projects/container/82142-propertyplaceholderconfigurer-problems
 */
public class OrderedPropertyPlaceholderConfigurer extends PropertiesLoaderSupport implements Ordered, BeanFactoryPostProcessor {

	private int order;

	public OrderedPropertyPlaceholderConfigurer() {
		setProperties(new Properties());
	}

	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		try {
			loadProperties(localProperties[0]);
		} catch (IOException ioe) {
			throw new ApplicationContextException("OrderedPropertyPlaceholderConfigurer failed to load properties", ioe);
		}
		PropertyPlaceholderConfigurer bfPostProcessor = new PropertyPlaceholderConfigurer();
		bfPostProcessor.setProperties(localProperties[0]);
		bfPostProcessor.postProcessBeanFactory(beanFactory);
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}
}
