/************************************************************************************
 * Copyright (C) 2012-2025 E.R.P. Consultores y Asociados, C.A.                   *
 * Contributor(s): Edwin Betancourt, EdwinBetanc0urt@outlook.com                  *
 * This program is free software: you can redistribute it and/or modify           *
 * it under the terms of the GNU General Public License as published by           *
 * the Free Software Foundation, either version 2 of the License, or              *
 * (at your option) any later version.                                            *
 * This program is distributed in the hope that it will be useful,                *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                 *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the                   *
 * GNU General Public License for more details.                                   *
 * You should have received a copy of the GNU General Public License              *
 * along with this program. If not, see <https://www.gnu.org/licenses/>.          *
 ************************************************************************************/
package org.solop.secure;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;

import org.compiere.util.Secure;
import org.compiere.util.SecureInterface;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Security provider that hashes user passwords with BCrypt, so ADempiere stores
 * the very same format as the Spring Boot (Sabana) applications that share the
 * AD_User.Password column.
 *
 * It is an independent {@link SecureInterface} implementation (peer of the legacy
 * {@link Secure}) and the default provider of this distribution
 * (see {@link SecureInterface#ADEMPIERE_SECURE_DEFAULT}); it can still be
 * overridden per JVM with {@code -DADEMPIERE_SECURE=<class>}.
 *
 * The reversible encryption / digest / SHA-512 operations are delegated to a
 * {@link Secure} instance, so column encryption and already-encrypted data keep
 * exactly the legacy behavior. Only the password hashing is BCrypt:
 * <ul>
 *   <li>{@link #getPasswordHash(String)} produces a self-contained BCrypt hash.</li>
 *   <li>{@link #isValidPasswordHash(String, String, String)} verifies both BCrypt
 *       and the legacy SHA-512 + AD_User.Salt format, so no user is locked out.</li>
 * </ul>
 * Whether a new password is written as BCrypt or as legacy SHA-512 depends only on
 * the active provider: MUser.setPassword stores whatever getPasswordHash returns,
 * and this provider always produces BCrypt.
 */
public class SecureBCrypt implements SecureInterface {

	/** Same defaults as Sabana: {@code new BCryptPasswordEncoder()} (version $2a, cost 10). */
	private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

	/** Legacy engine used for the reversible encryption / digest operations. */
	private final Secure legacy = new Secure();

	//	Password hashing (BCrypt) ------------------------------------------------

	@Override
	public boolean isPasswordHashEncrypted(String storedHash) {
		return storedHash != null && storedHash.startsWith("$2");
	}

	@Override
	public String getPasswordHash(String plainPassword) {
		return plainPassword == null ? null : ENCODER.encode(plainPassword);
	}

	@Override
	public boolean isValidPasswordHash(String plainPassword, String storedHash, String storedSalt) {
		if (plainPassword == null || storedHash == null) {
			return false;
		}
		if (isPasswordHashEncrypted(storedHash)) {
			return ENCODER.matches(plainPassword, storedHash);
		}
		//	Legacy SHA-512 hash requires the salt stored alongside it
		if (storedSalt != null) {
			try {
				return getSHA512Hash(1000, plainPassword, Secure.convertHexString(storedSalt)).equals(storedHash);
			} catch (Exception e) {
				return false;
			}
		}
		return false;
	}

	@Override
	public boolean isValidPasswordHash(String plainPassword, String storedHash) {
		return isValidPasswordHash(plainPassword, storedHash, null);
	}

	//	Reversible encryption / digest (delegated to the legacy engine) ----------

	@Override
	public String encrypt(String value) {
		return legacy.encrypt(value);
	}

	@Override
	public String decrypt(String value) {
		return legacy.decrypt(value);
	}

	@Override
	public Integer encrypt(Integer value) {
		return legacy.encrypt(value);
	}

	@Override
	public Integer decrypt(Integer value) {
		return legacy.decrypt(value);
	}

	@Override
	public BigDecimal encrypt(BigDecimal value) {
		return legacy.encrypt(value);
	}

	@Override
	public BigDecimal decrypt(BigDecimal value) {
		return legacy.decrypt(value);
	}

	@Override
	public Timestamp encrypt(Timestamp value) {
		return legacy.encrypt(value);
	}

	@Override
	public Timestamp decrypt(Timestamp value) {
		return legacy.decrypt(value);
	}

	@Override
	public String getDigest(String value) {
		return legacy.getDigest(value);
	}

	@Override
	public boolean isDigest(String value) {
		return legacy.isDigest(value);
	}

	@Override
	public String getSHA512Hash(int iterations, String value, byte[] salt)
			throws NoSuchAlgorithmException, UnsupportedEncodingException {
		return legacy.getSHA512Hash(iterations, value, salt);
	}

}	//	SecureBCrypt
