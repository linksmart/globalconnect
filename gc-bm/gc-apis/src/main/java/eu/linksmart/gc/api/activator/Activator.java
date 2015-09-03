package eu.linksmart.gc.api.activator;

import eu.linksmart.gc.api.engine.EngineContext;

/**
 * Activator interface is a hack to deal with life-cycle of the components, originally derived from OSGi specification, 
 * and part of the GC code where components have so called "Activate" method for initialization of the implementation.
 * Since such "activation" methods are only visible in an implementation, therefore an Activator interface is added and 
 * ALL GC interfaces would extend from this interface to be able to invoke activation methods to properly initialize the implementations. 
 * 
 */
public interface Activator {
	
	public void activate(EngineContext context);
	
	public void initialize();
	
	public void deactivate();

}
