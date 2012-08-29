package com.seansouthern.anchoragebustimes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class DirectionsMap{

	Map<String, List<String>> directionsMap = new HashMap<String, List<String>>();

	List<String> dirs1 = new ArrayList<String>();
	List<String> dirs2 = new ArrayList<String>();
	List<String> dirs3 = new ArrayList<String>();
	List<String> dirs7 = new ArrayList<String>();
	List<String> dirs7A = new ArrayList<String>();
	List<String> dirs8 = new ArrayList<String>();
	List<String> dirs9 = new ArrayList<String>();
	List<String> dirs13 = new ArrayList<String>();
	List<String> dirs14 = new ArrayList<String>();
	List<String> dirs15 = new ArrayList<String>();
	List<String> dirs36 = new ArrayList<String>();
	List<String> dirs45 = new ArrayList<String>();
	List<String> dirs60 = new ArrayList<String>();
	List<String> dirs75 = new ArrayList<String>();
	List<String> dirs102 = new ArrayList<String>();


	DirectionsMap(){

		dirs1.add("1 MULDOON");
		dirs1.add("1 DIMOND CENTER");
		directionsMap.put("1", dirs1);

		dirs2.add("2 DIMOND CENTER");
		dirs2.add("2 DOWNTOWN");
		directionsMap.put("2", dirs2);

		dirs3.add("3C DOWNTOWN");
		dirs3.add("3N DOWNTOWN");
		dirs3.add("3C CENTENNIAL");
		dirs3.add("3N MULDOON");
		directionsMap.put("3", dirs3);

		dirs7.add("7 DIMOND CENTER");
		dirs7.add("7 DOWNTOWN");
		directionsMap.put("7", dirs7);

		dirs7A.add("7A DIMOND CENTER");
		dirs7A.add("7A DOWNTOWN");
		directionsMap.put("7A", dirs7A);

		dirs8.add("8 DOWNTOWN");
		dirs8.add("8 MULDOON");
		directionsMap.put("8", dirs8);

		dirs9.add("9 DIMOND CENTER");
		dirs9.add("9 DOWNTOWN");
		directionsMap.put("9", dirs9);

		dirs13.add("13 DOWNTOWN");
		dirs13.add("13 MULDOON");
		directionsMap.put("13", dirs13);

		dirs14.add("14 DOWNTOWN");
		directionsMap.put("14", dirs14);

		dirs15.add("15 DOWNTOWN");
		dirs15.add("15 MULDOON");
		directionsMap.put("15", dirs15);

		dirs36.add("36 DOWNTOWN");
		dirs36.add("36 APU");
		directionsMap.put("36", dirs36);

		dirs45.add("45 ANMC");
		dirs45.add("45 DOWNTOWN");
		directionsMap.put("45", dirs45);

		dirs60.add("60 DOWNTOWN");
		dirs60.add("60 HUFFMAN");
		directionsMap.put("60", dirs60);

		dirs75.add("75 DOWNTOWN");
		dirs75.add("75 TIKAHTNU");
		directionsMap.put("75", dirs75);

		dirs102.add("102 ANMC");
		dirs102.add("102 EAGLE RIVER");
		dirs102.add("102 PETERS CREEK");
		directionsMap.put("102", dirs102);

	}

}