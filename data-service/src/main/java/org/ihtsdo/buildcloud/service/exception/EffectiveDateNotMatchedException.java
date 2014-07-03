package org.ihtsdo.buildcloud.service.exception;

/**
 * RuntimeException when effective date in file name doesn't match with expected date.
 *
 */
public class EffectiveDateNotMatchedException extends RuntimeException {
    
    public EffectiveDateNotMatchedException(String errormsg){
	super(errormsg);
    }
}
