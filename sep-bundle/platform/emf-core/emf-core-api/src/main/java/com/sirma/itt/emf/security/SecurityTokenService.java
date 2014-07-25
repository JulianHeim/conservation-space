package com.sirma.itt.emf.security;

/**
 * Carries of security token management. Used to request a new and check the validity of an existing
 * security token.
 * 
 * @author Adrian Mitev
 */
public interface SecurityTokenService {

	/**
	 * Requests a security token from the Identity Provider.
	 * 
	 * @param username
	 *            authentication username.
	 * @param password
	 *            authentication password.
	 * @return the requested token.
	 */
	String requestToken(String username, String password);

	/**
	 * Checks the validity of a security token.
	 * 
	 * @param token
	 *            token to verify.
	 * @return true if valid, false otherwise.
	 */
	boolean validateToken(String token);

}
