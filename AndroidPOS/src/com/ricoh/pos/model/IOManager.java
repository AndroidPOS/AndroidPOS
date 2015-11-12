package com.ricoh.pos.model;

import java.util.List;

/**
 * This class manage I/O related database
 *
 * @author Takuya Mizuhara
 */
public interface IOManager {

	/**
	 * Search all record from database.
	 *
	 * @return search results
	 */
	public List<?> searchAlldata();
}