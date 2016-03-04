package com.ricoh.pos.model;

import android.database.sqlite.SQLiteDatabase;

import com.ricoh.pos.data.SingleSalesRecord;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;

public class SalesRecordManager {
	
	private static SalesRecordManager instance;
	public static final String SALES_DATE_KEY = "SalesDate";
	
	private SalesRecordManager(){
	}
	
	public static SalesRecordManager getInstance(){
		if (instance == null) {
			instance = new SalesRecordManager();
		}
		return instance;
	}
	
	public void storeSingleSalesRecord(SQLiteDatabase database, SingleSalesRecord record){
		if (record == null) {
			throw new IllegalArgumentException("The passing record is null");
		}
		
		WomanShopSalesIOManager.getInstance().insertSalesRecord(record);
	}

    /**
     * 指定されたその日の売り上げリストを返す
     * @param date 日付けデータ。時分秒のパラメータはあっても無視される。
     * @return 検索結果配列
     */
	public ArrayList<SingleSalesRecord> restoreSingleSalesRecordsOfTheDay(Date date){
        return WomanShopSalesIOManager.getInstance().searchByDate(date, true);
	}

    /**
     * 指定した日付けデータと一致するデータを返す。こちらは日付けをユニークデータとして扱って検索する。
     * @param date　日付けデータ
     * @return 該当するSingleSalesRecord形式のデータインスタンス。検索でhitしなければnullが返る。
     */
	public SingleSalesRecord getSingleSalesRecord(Date date){
        ArrayList<SingleSalesRecord> records = WomanShopSalesIOManager.getInstance().searchByDate(date, false);
        if (records.size() != 1) {
            return null;
        }

        return records.get(0);
    }

	/**
	 * 指定された日付の総売り上げ額を取得する
	 * @param date 日付データ
	 * @return 指定された日付の総売り上げ額(単位パイサ)
	 */
	public long getOneDayTotalSales(Date date){
		ArrayList<SingleSalesRecord> salesRecords = restoreSingleSalesRecordsOfTheDay(date);
		long totalSales = 0;
		for (SingleSalesRecord record : salesRecords) {
			totalSales += record.getTotalSales();
		}
		return totalSales;
	}

	/**
	 * 指定された日付の総利益(値引き前)を取得する
	 * @param date 日付データ
	 * @return 指定された日付の総利益(単位パイサ)
	 */
	public long getOneDayTotalRevenue(Date date){
		ArrayList<SingleSalesRecord> salesRecords = restoreSingleSalesRecordsOfTheDay(date);
		long totalRevenue =0;
		for (SingleSalesRecord record : salesRecords) {
			totalRevenue += record.getTotalRevenue();
		}
		return totalRevenue;
	}

	/**
	 * 指定された日付の総値引き額を取得する
	 * @param date 日付データ
	 * @return 指定された日付の総値引き額(単位パイサ)
	 */
	public long getOneDayTotalDiscount(Date date) {
		ArrayList<SingleSalesRecord> salesRecords = restoreSingleSalesRecordsOfTheDay(date);
		long totalDiscount = 0;;
		for (SingleSalesRecord record : salesRecords) {
			totalDiscount += record.getDiscountValue();
		}
		return totalDiscount;
	}

	/**
	 * 指定された日付の総純利益
	 * @param date 日付データ
	 * @return 指定された日付の総利益から総値引き額を除いた額(単位パイサ)
	 */
	public long getOneDayTotalNetProfit(Date date) {
		return (getOneDayTotalRevenue(date) - getOneDayTotalDiscount(date));
	}

}
