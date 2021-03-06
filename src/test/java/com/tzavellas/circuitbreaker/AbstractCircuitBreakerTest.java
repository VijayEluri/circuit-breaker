package com.tzavellas.circuitbreaker;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.tzavellas.circuitbreaker.support.CircuitBreakerAspectSupport;
import com.tzavellas.circuitbreaker.util.Duration;
import com.tzavellas.test.IStockService;

public abstract class AbstractCircuitBreakerTest {

	protected IStockService stocks;
	protected CircuitBreakerAspectSupport stocksBreaker;
	
	@Before
	public void resetCircuit() {
		// this is needed because in Spring the service and the aspect are singletons.
		CircuitInfo circuit = stocksBreaker.getCircuitInfo(); 
		circuit.close();
		circuit.resetToDefaultConfig();
		circuit.resetStatistics();
	}
	
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
			return;
		}
		fail("Should have raised an OpenCircuitException");
	}
	
	@Test
	public void the_circuit_is_half_open_after_the_timeout() throws Exception {
		CircuitInfo circuit = stocksBreaker.getCircuitInfo(); 
		circuit.setTimeout(Duration.millis(1));
		generateFaultsToOpen();
		Thread.sleep(2);
		assertTrue(circuit.isHalfOpen());
		assertEquals(5, stocks.getQuote("JAVA"));
		assertEquals(0, circuit.getCurrentFailures());
	}
	
	@Test
	public void the_circuit_moves_from_half_open_to_open_on_first_failure() throws Exception {
		CircuitInfo circuit = stocksBreaker.getCircuitInfo(); 
		circuit.setTimeout(Duration.millis(1));
		generateFaultsToOpen();
		Thread.sleep(2);
		assertTrue(circuit.isHalfOpen());
		try { stocks.faultyGetQuote("JAVA"); } catch (RuntimeException expected) { }
		assertTrue(circuit.isOpen());
	}
	
	@Test
	public void the_failure_count_gets_reset_after_an_amount_of_time() throws Exception {
		CircuitInfo circuit = stocksBreaker.getCircuitInfo();
		circuit.setCurrentFailuresDuration(Duration.millis(1));
		
		generateFaults(CircuitInfo.DEFAULT_MAX_FAILURES - 1);
		assertFalse(circuit.isOpen());
		Thread.sleep(5);
		
		generateFaults(1);
		assertFalse(circuit.isOpen());
		assertEquals(5, stocks.getQuote("JAVA"));
	}
	
	@Test
	public void almost_instant_failure_count_reset() {
		CircuitInfo circuit = stocksBreaker.getCircuitInfo();
		circuit.setCurrentFailuresDuration(Duration.nanos(1));
		generateFaultsToOpen();
		assertEquals(5, stocks.getQuote("JAVA"));
		assertFalse(circuit.isOpen());
	}
	
	@Test
	public void ignored_exceptions_do_not_open_a_circuit() {
		stocksBreaker.ignoreException(ArithmeticException.class);
		generateFaultsToOpen();
		assertEquals(5, stocks.getQuote("JAVA"));
		assertFalse(stocksBreaker.getCircuitInfo().isOpen());
		stocksBreaker.removeIgnoredExcpetion(ArithmeticException.class);
	}
	
	@Test
	public void ignored_exceptions_capture_subclasses() {
		stocksBreaker.ignoreException(RuntimeException.class);
		generateFaultsToOpen();
		assertEquals(5, stocks.getQuote("JAVA"));
		assertFalse(stocksBreaker.getCircuitInfo().isOpen());
		stocksBreaker.removeIgnoredExcpetion(RuntimeException.class);
	}
	
	@Test
	public void slow_metnod_executions_count_as_failures() {
		stocksBreaker.setMaxMethodDuration(Duration.nanos(1));
		for (int i = 0; i < CircuitInfo.DEFAULT_MAX_FAILURES; i++)
			stocks.getQuote("JAVA");
		assertTrue(stocksBreaker.getCircuitInfo().isOpen());
		stocksBreaker.setMaxMethodDuration(null);
	}
	
	
	//------------------------------------------------------------------------
	
	protected void generateFaultsToOpen() {
		generateFaults(CircuitInfo.DEFAULT_MAX_FAILURES);
	}
	
	protected void generateFaults(int numOfFaults) {
		for (int i = 0; i < numOfFaults; i++) {
			try { stocks.faultyGetQuote("JAVA"); } catch (ArithmeticException expected) { }
		}
	}
}
