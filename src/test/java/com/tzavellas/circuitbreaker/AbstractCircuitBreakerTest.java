package com.tzavellas.circuitbreaker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.tzavellas.circuitbreaker.support.CircuitBreakerAspectSupport;
import com.tzavellas.circuitbreaker.util.Duration;
import com.tzavellas.test.IStockService;

public abstract class AbstractCircuitBreakerTest {

	protected IStockService stocks;
	
	protected abstract CircuitBreakerAspectSupport getCircuitBreaker();
	
	@Test
	public void normal_operation_while_closed() {
		assertEquals(5, stocks.getQuote("JAVA"));
	}
	
	@Test(expected=OpenCircuitException.class)
	public void after_a_number_of_faults_the_circuit_opens() {
		generateFaultsToOpen();
		stocks.getQuote("JAVA");
	}
	
	@Test
	public void the_circuit_can_be_opened_after_being_closed() {
		generateFaultsToOpen();
		try {
			stocks.getQuote("JAVA");
		} catch (OpenCircuitException e) {
			CircuitInfo c = e.getCircuit();
			assertTrue(c.isOpen());
			c.close();
			assertFalse(c.isOpen());
			assertEquals(5, stocks.getQuote("JAVA"));
		}
	}
	
	@Test
	public void the_circuit_is_half_open_after_the_timeout() throws Exception {
		CircuitInfo circuit = getCircuitBreaker().getCircuitInfo(); 
		circuit.setTimeoutMillis(1);
		generateFaultsToOpen();
		Thread.sleep(2);
		assertTrue(circuit.isHalfOpen());
		assertEquals(5, stocks.getQuote("JAVA"));
		assertEquals(0, circuit.getCurrentFailures());
	}
	
	@Test
	public void the_circuit_moves_from_half_open_to_open_on_first_failure() throws Exception {
		CircuitInfo circuit = getCircuitBreaker().getCircuitInfo(); 
		circuit.setTimeoutMillis(1);
		generateFaultsToOpen();
		Thread.sleep(2);
		assertTrue(circuit.isHalfOpen());
		try { stocks.faultyGetQuote("JAVA"); } catch (RuntimeException expected) { }
		assertTrue(circuit.isOpen());
	}
	
	@Test
	public void the_failure_count_gets_reset_after_an_amount_of_time() {
		CircuitInfo circuit = getCircuitBreaker().getCircuitInfo();
		circuit.setCurrentFailuresDuration(Duration.nanos(1));
		generateFaultsToOpen();
		assertEquals(5, stocks.getQuote("JAVA"));
		assertFalse(circuit.isOpen());
	}
	
	@Test
	public void ignored_exceptions_do_not_open_a_circuit() {
		CircuitBreakerAspectSupport breaker = getCircuitBreaker();
		breaker.ignoreException(RuntimeException.class);
		generateFaultsToOpen();
		assertEquals(5, stocks.getQuote("JAVA"));
		assertFalse(breaker.getCircuitInfo().isOpen());
		breaker.removeIgnoredExcpetion(RuntimeException.class);
	}
	
	protected void generateFaultsToOpen() {
		for (int i = 0; i < CircuitInfo.DEFAULT_MAX_FAILURES; i++) {
			try { stocks.faultyGetQuote("JAVA"); } catch (RuntimeException expected) { }
		}
	}
}
