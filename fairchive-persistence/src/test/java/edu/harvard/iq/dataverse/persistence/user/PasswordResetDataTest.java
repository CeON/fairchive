package edu.harvard.iq.dataverse.persistence.user;


import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class PasswordResetDataTest {

	@Test 
	void isExpired() {		
		
		
		
		assertFalse(new PasswordResetData(null, null, 60*24).isExpired());
		assertTrue(new PasswordResetData(null, null, -60*24).isExpired());
	}
}
