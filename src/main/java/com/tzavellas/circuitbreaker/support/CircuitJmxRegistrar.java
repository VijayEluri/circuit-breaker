package com.tzavellas.circuitbreaker.support;

import java.lang.management.ManagementFactory;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.tzavellas.circuitbreaker.CircuitInfo;

/**
 * A class that knows how to register and unregister {@code CircuitInfo}
 * objects in JMX.
 * 
 * @see CircuitBreakerAspectSupport
 * @see CircuitInfo
 * @see JmxUtils
 * 
 * @author spiros
 */
class CircuitJmxRegistrar {
	
	private CircuitInfo circuit;
	private Class<?> targetClass;
	private ObjectName name;
	
	
	CircuitJmxRegistrar(CircuitInfo circuit, Class<?> targetClass) {
		this.circuit = circuit;
		this.targetClass = targetClass;
	}
	
	void register() {
		try {
			name = new ObjectName(JmxUtils.getObjectName(targetClass));
			MBeanServer server = ManagementFactory.getPlatformMBeanServer();
			server.registerMBean(circuit, name);
		} catch (JMException e) {
			throw new RuntimeException(e);
		}
	}
	
	void unregister() {
		if (isRegistered()) {
			try {
				MBeanServer server = ManagementFactory.getPlatformMBeanServer();
				server.unregisterMBean(name);
			} catch (JMException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	boolean isRegistered() {
		return name != null;
	}
}
