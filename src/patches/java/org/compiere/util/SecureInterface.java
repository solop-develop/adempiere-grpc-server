/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
package org.compiere.util;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;

/**
 * 	Adempiere Security Interface.
 * 	To enable your own class, you need to set the property ADEMPIERE_SECURE 
 * 	when starting the client or server.
 *  The setting for the default class would be:
 *  -DADEMPIERE_SECURE=org.compiere.util.Secure
 *	
 *  @author Jorg Janke
 *  @version $Id: SecureInterface.java,v 1.2 2006/07/30 00:52:23 jjanke Exp $
 */
public interface SecureInterface
{
	/** Class Name implementing SecureInterface	*/
	public static final String	ADEMPIERE_SECURE = "ADEMPIERE_SECURE";
	/** Default Class Name implementing SecureInterface (BCrypt provider, Sabana-compatible) */
	public static final String	ADEMPIERE_SECURE_DEFAULT = "org.solop.secure.SecureBCrypt"; // `org.compiere.util.Secure` is the legacy SHA-512 + salt provider

	
	/** Clear Text Indicator xyz	*/
	public static final String		CLEARVALUE_START = "xyz";
	/** Clear Text Indicator		*/
	public static final String		CLEARVALUE_END = "";
	/** Encrypted Text Indiactor ~	*/
	public static final String		ENCRYPTEDVALUE_START = "~";
	/** Encrypted Text Indiactor ~	*/
	public static final String		ENCRYPTEDVALUE_END = "~";

	
	/**
	 *	Encryption.
	 *  @param value clear value
	 *  @return encrypted String
	 */
	public String encrypt (String value);

	/**
	 *	Decryption.
	 *  @param value encrypted value
	 *  @return decrypted String
	 */
	public String decrypt (String value);

	/**
	 *	Encryption.
	 * 	The methods must recognize clear text values
	 *  @param value clear value
	 *  @return encrypted String
	 */
	public Integer encrypt (Integer value);

	/**
	 *	Decryption.
	 * 	The methods must recognize clear text values
	 *  @param value encrypted value
	 *  @return decrypted String
	 */
	public Integer decrypt (Integer value);
	
	/**
	 *	Encryption.
	 * 	The methods must recognize clear text values
	 *  @param value clear value
	 *  @return encrypted String
	 */
	public BigDecimal encrypt (BigDecimal value);

	/**
	 *	Decryption.
	 * 	The methods must recognize clear text values
	 *  @param value encrypted value
	 *  @return decrypted String
	 */
	public BigDecimal decrypt (BigDecimal value);

	/**
	 *	Encryption.
	 * 	The methods must recognize clear text values
	 *  @param value clear value
	 *  @return encrypted String
	 */
	public Timestamp encrypt (Timestamp value);

	/**
	 *	Decryption.
	 * 	The methods must recognize clear text values
	 *  @param value encrypted value
	 *  @return decrypted String
	 */
	public Timestamp decrypt (Timestamp value);
	
	
	/**
	 *  Convert String to Digest.
	 *  JavaScript version see - http://pajhome.org.uk/crypt/md5/index.html
	 *
	 *  @param value message
	 *  @return HexString of message (length = 32 characters)
	 */
	public String getDigest (String value);

	/**
	 * 	Checks, if value is a valid digest
	 *  @param value digest string
	 *  @return true if valid digest
	 */
	public boolean isDigest (String value);
	
	/**
	 *  Convert String and salt to SHA-512 hash with iterations
	 *  https://www.owasp.org/index.php/Hashing_Java
	 *
	 *  @param value message
	 *  @return HexString of message (length = 128 characters)
	 * @throws NoSuchAlgorithmException 
	 * @throws UnsupportedEncodingException 
	 */
	public String getSHA512Hash (int iterations, String value, byte[] salt) throws NoSuchAlgorithmException, UnsupportedEncodingException;

	/**
	 * 	Is the stored value a BCrypt hash (starts with the $2 version prefix).
	 * 	@param storedHash value stored in AD_User.Password
	 * 	@return true when it is a BCrypt hash
	 */
	public boolean isPasswordHashEncrypted (String storedHash);

	/**
	 * 	Self-contained password hash (e.g. BCrypt) for providers that support it.
	 * 	The legacy provider returns null, signalling the caller to use the SHA-512 +
	 * 	salt mechanism; the BCrypt provider ({@code SecureBCrypt}) returns a BCrypt
	 * 	hash. The write-policy is decided by the caller via the SysConfig
	 * 	USER_PASSWORD_HASH_BCRYPT.
	 * 	@param plainPassword plain text password
	 * 	@return self-contained hash, or null when this provider has no self-contained hashing
	 */
	public String getPasswordHash (String plainPassword);

	/**
	 * 	Verify a plain password against the stored credentials. The legacy provider
	 * 	validates the SHA-512 + AD_User.Salt format; the BCrypt provider
	 * 	({@code SecureBCrypt}) also accepts BCrypt hashes.
	 * 	@param plainPassword plain text password to check
	 * 	@param storedHash value stored in AD_User.Password
	 * 	@param storedSalt value stored in AD_User.Salt (may be null for BCrypt)
	 * 	@return true when the password matches
	 */
	public boolean isValidPasswordHash (String plainPassword, String storedHash, String storedSalt);

	/**
	 * 	Verify a plain password against a self-contained stored hash (e.g. BCrypt),
	 * 	i.e. when there is no separate salt.
	 * 	@param plainPassword plain text password to check
	 * 	@param storedHash value stored in AD_User.Password
	 * 	@return true when the password matches
	 */
	public boolean isValidPasswordHash (String plainPassword, String storedHash);

}	//	SecureInterface
