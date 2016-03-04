package com.ricoh.pos.model;

import java.nio.charset.Charset;
import java.util.List;

/**
 * This class manage I/O related database
 *
 * @author Takuya Mizuhara
 */
public interface IOManager {

	Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

	/**
	 * Search all record from database.
	 *
	 * @return search results
	 */
	public List<?> searchAll();
}