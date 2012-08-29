package com.seansouthern.anchoragebustimes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Vibrator;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

import de.android1.overlaymanager.ManagedOverlay;
import de.android1.overlaymanager.ManagedOverlayGestureDetector;
import de.android1.overlaymanager.ManagedOverlayGestureDetector.OnOverlayGestureListener;
import de.android1.overlaymanager.ManagedOverlayItem;
import de.android1.overlaymanager.OverlayManager;
import de.android1.overlaymanager.ZoomEvent;


public class map extends MapActivity{

	LineCoords lc = new LineCoords();
	public static String route = null;

	MySQLiteHelper myDbHelper = null;
	Cursor cursor = null;
	SQLiteDatabase db = null;
	OverlayManager overlayManager;
	ManagedOverlay stopOverlay;
	ManagedOverlay busOverlay;
	TimerTask doAsyncTask;

	private Projection projection;

	public static String REGEX_PATTERN = "^\\d+";
	public static Pattern p = Pattern.compile(REGEX_PATTERN);

	Map<String, List<String>> directionsMap = new HashMap<String, List<String>>();


	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.map);

		List<String> dirs1 = new ArrayList<String>();
		dirs1.add("1 MULDOON");
		dirs1.add("1 DIMOND CENTER");
		directionsMap.put("1", dirs1);

		List<String> dirs2 = new ArrayList<String>();
		dirs2.add("2 DIMOND CENTER");
		dirs2.add("2 DOWNTOWN");
		directionsMap.put("2", dirs2);

		List<String> dirs3 = new ArrayList<String>();
		dirs3.add("3C DOWNTOWN");
		dirs3.add("3N DOWNTOWN");
		dirs3.add("3C CENTENNIAL");
		dirs3.add("3N MULDOON");
		directionsMap.put("3", dirs3);

		List<String> dirs7 = new ArrayList<String>();
		dirs7.add("7 DIMOND CENTER");
		dirs7.add("7 DOWNTOWN");
		directionsMap.put("7", dirs7);
		
		List<String> dirs7A = new ArrayList<String>();
		dirs7A.add("7A DIMOND CENTER");
		dirs7A.add("7A DOWNTOWN");
		directionsMap.put("7A", dirs7A);

		List<String> dirs8 = new ArrayList<String>();
		dirs8.add("8 DOWNTOWN");
		dirs8.add("8 MULDOON");
		directionsMap.put("8", dirs8);

		List<String> dirs9 = new ArrayList<String>();
		dirs9.add("9 DIMOND CENTER");
		dirs9.add("9 DOWNTOWN");
		directionsMap.put("9", dirs9);

		List<String> dirs13 = new ArrayList<String>();
		dirs13.add("13 DOWNTOWN");
		dirs13.add("13 MULDOON");
		directionsMap.put("13", dirs13);

		List<String> dirs14 = new ArrayList<String>();
		dirs14.add("14 DOWNTOWN");
		directionsMap.put("14", dirs14);

		List<String> dirs15 = new ArrayList<String>();
		dirs15.add("15 DOWNTOWN");
		dirs15.add("15 MULDOON");
		directionsMap.put("15", dirs15);

		List<String> dirs36 = new ArrayList<String>();
		dirs36.add("36 DOWNTOWN");
		dirs36.add("36 APU");
		directionsMap.put("36", dirs36);

		List<String> dirs45 = new ArrayList<String>();
		dirs45.add("45 ANMC");
		dirs45.add("45 DOWNTOWN");
		directionsMap.put("45", dirs45);

		List<String> dirs60 = new ArrayList<String>();
		dirs60.add("60 DOWNTOWN");
		dirs60.add("60 HUFFMAN");
		directionsMap.put("60", dirs60);

		List<String> dirs75 = new ArrayList<String>();
		dirs75.add("75 DOWNTOWN");
		dirs75.add("75 TIKAHTNU");
		directionsMap.put("75", dirs75);

		List<String> dirs102 = new ArrayList<String>();
		dirs102.add("102 ANMC");
		dirs102.add("102 EAGLE RIVER");
		dirs102.add("102 PETERS CREEK");
		directionsMap.put("102", dirs102);

		MapView mapView = (MapView) findViewById(R.id.mapview);
		mapView.setBuiltInZoomControls(true);

		SharedPreferences sp = getPreferences(MODE_PRIVATE);
		route = sp.getString("LAST_MAP_ROUTE", "1");

		Spinner spinner = (Spinner) findViewById(R.id.spinner);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				this, R.array.routes_array, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);

		final TextView leftButton = (TextView) findViewById(R.id.left_button);
		leftButton.setText(directionsMap.get(route).get(0));

		final TextView rightButton = (TextView) findViewById(R.id.right_button);
		//This will be null for route14, account for it
		rightButton.setText(directionsMap.get(route).get(1));
		//Route 102, 7 and 3 need to be accounted for

		leftButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String direction = (String) ((TextView) v).getText();
				if(stopOverlay != null){
					if(!v.isSelected()){
						v.setSelected(true);
						rightButton.setSelected(false);
						overlayManager.removeOverlay(stopOverlay);
						overlayManager.createOverlay("route" + route, getResources().getDrawable(R.drawable.marker));
						stopOverlay = overlayManager.getOverlay("route" + route);
						stopOverlay.addAll(grabStopCoordsByDirection(route, direction));
						stopOverlay.setOnOverlayGestureListener(mogDetector);						
						overlayManager.populate();
					}
					else{
						v.setSelected(false);
						overlayManager.removeOverlay(stopOverlay);
						overlayManager.createOverlay("route" + route, getResources().getDrawable(R.drawable.marker));
						stopOverlay = overlayManager.getOverlay("route" + route);
						stopOverlay.addAll(grabStopCoordsByDirection(route, "ALL"));
						stopOverlay.setOnOverlayGestureListener(mogDetector);
						overlayManager.populate();
					}
				}
			}

		});

		rightButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String direction = (String) ((TextView) v).getText();
				if(!v.isSelected()){
					v.setSelected(true);
					leftButton.setSelected(false);
					overlayManager.removeOverlay(stopOverlay);
					overlayManager.createOverlay("route" + route, getResources().getDrawable(R.drawable.marker));
					stopOverlay = overlayManager.getOverlay("route" + route);
					stopOverlay.addAll(grabStopCoordsByDirection(route, direction));
					stopOverlay.setOnOverlayGestureListener(mogDetector);						
					overlayManager.populate();
				}
				else{
					v.setSelected(false);
					overlayManager.removeOverlay(stopOverlay);
					overlayManager.createOverlay("route" + route, getResources().getDrawable(R.drawable.marker));
					stopOverlay = overlayManager.getOverlay("route" + route);
					stopOverlay.addAll(grabStopCoordsByDirection(route, "ALL"));
					stopOverlay.setOnOverlayGestureListener(mogDetector);
					overlayManager.populate();
				}
			}
		});

		@SuppressWarnings("rawtypes")
		ArrayAdapter spinAdap = (ArrayAdapter) spinner.getAdapter();
		for(int i = 0; i < spinAdap.getCount(); i++){
			Matcher m = p.matcher(spinAdap.getItem(i).toString());
			if (m.find()) {
				if(m.group().equals(route)){
					spinner.setSelection(i);
				}
			}
		}

		spinner.setOnItemSelectedListener(new SpinnerItemListener());

		myDbHelper = new MySQLiteHelper(this);
		try {
			myDbHelper.createDatabase();
		} catch (IOException e) {
			e.printStackTrace();
		}
		db = myDbHelper.getReadableDatabase();

		//Start map off at center of Anchorage
		GeoPoint centerOfAnchorage = new LatLonPoint(61.157016,-149.861265);
		mapView.getController().animateTo(centerOfAnchorage);
		mapView.getController().setZoom(12);

		overlayManager = new OverlayManager(this, mapView);
		stopOverlay = overlayManager.createOverlay("route" + route, getResources().getDrawable(R.drawable.marker));
		stopOverlay.addAll(grabStopCoordsByDirection(route, "1 MULDOON"));
		stopOverlay.setOnOverlayGestureListener(mogDetector);
		overlayManager.populate();

		busOverlay = overlayManager.createOverlay("route" + route + "bus", getResources().getDrawable(R.drawable.busmarker));
		ManagedOverlay.boundToCenter(getResources().getDrawable(R.drawable.busmarker));
		busOverlay.addAll(grabBusCoords(route));
		toCallAsync(route);
		overlayManager.populate();

		projection = mapView.getProjection();



	}

	@Override
	public void onPause(){
		super.onPause();
		doAsyncTask.cancel();
	}

	@Override
	public void onResume(){
		super.onResume();
		toCallAsync(route);
	}

	public void toCallAsync(final String routeNum) {
		final Handler handler = new Handler();
		Timer timer = new Timer();
		if(doAsyncTask != null){
			doAsyncTask.cancel();
		}
		doAsyncTask = new TimerTask() {
			@Override
			public void run() {
				handler.post(new Runnable(){
					public void run() {
						try {
							BusRefresher performBackgroundTask = new BusRefresher();
							performBackgroundTask.execute(routeNum);
						}
						catch (Exception e) {
						}
					}
				});
			}
		};
		timer.schedule(doAsyncTask, 0,20000);
	}

	OnOverlayGestureListener mogDetector = new ManagedOverlayGestureDetector.OnOverlayGestureListener(){
		public boolean onDoubleTap(MotionEvent arg0, ManagedOverlay arg1,
				GeoPoint arg2, ManagedOverlayItem arg3) {
			return false;
		}

		public void onLongPress(MotionEvent arg0, ManagedOverlay arg1) {
			
		}

		public void onLongPressFinished(MotionEvent arg0,
				ManagedOverlay arg1, GeoPoint arg2, ManagedOverlayItem arg3) {
			if(arg3 != null){
				String stopNum = arg3.getTitle().substring(5);
				String stopAddr = arg3.getSnippet();
				String stopName = stopNum + " - " + stopAddr;
				SharedPreferences favoriteStops = getSharedPreferences("main", MODE_PRIVATE);
				SharedPreferences.Editor editor = favoriteStops.edit();
				editor.putString(stopName, stopName);
				editor.commit();
				Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				v.vibrate(150);
				// TODO: Should be a popup&fadeout footer, Toasts suck
				Toast.makeText(map.this, stopName + " added to Favorites", Toast.LENGTH_SHORT).show();

			}
		}

		public boolean onScrolled(MotionEvent arg0, MotionEvent arg1,
				float arg2, float arg3, ManagedOverlay arg4) {
			return false;
		}

		public boolean onSingleTap(MotionEvent arg0, ManagedOverlay arg1,
				GeoPoint arg2, ManagedOverlayItem arg3) {
			if(arg3 != null){
				Dialog dialog = new Dialog(map.this, R.style.dialogStyle);
				dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
				String stopNum = arg3.getTitle().substring(5);
				MapTimesTable dialogTable = new MapTimesTable(map.this, stopNum);
				ScrollView sv = new ScrollView(map.this);
				sv.addView(dialogTable);
				dialog.setContentView(sv);
				dialog.show();
				return true;
			}
			return false;
		}

		public boolean onZoom(ZoomEvent arg0, ManagedOverlay arg1) {
			return false;
		}
	};

	class MapTimesLoader extends AsyncTask<String, Integer, Document>{
		Context context;
		MapTimesTable timesTable;
		ProgressBar waitSpinner;

		public MapTimesLoader(Context context, MapTimesTable timesTable) {
			this.context = context;
			this.timesTable = timesTable;

			TableRow.LayoutParams params = new TableRow.LayoutParams();
			params.gravity = Gravity.CENTER;
			params.weight = 0;
			params.span = 2;

			waitSpinner = new ProgressBar(context, null, android.R.attr.progressBarStyle);
			waitSpinner.setLayoutParams(params);
		}

		@Override
		protected void onProgressUpdate(Integer... values){
			super.onProgressUpdate(values);
			if(values[0] == 0){
				TableRow row = new TableRow(context);
				row.addView(waitSpinner);
				timesTable.addView(row);	
			}
			if(values[0] == -1){
				TextView tv = new TextView(context);
				tv.setText("The network is not responding. Are you connected to the internet?");
				timesTable.addView(tv);
			}
		}

		@Override
		protected Document doInBackground(String... stopNum) {
			Document page = null;
			publishProgress(0);
			try {
				page = Jsoup.connect("http://bustracker.muni.org/InfoPoint/map/GetStopHtml.ashx?vehicleId=" + stopNum[0]).get();
			} catch (IOException e) {
				e.printStackTrace();
				publishProgress(-1);
			}
			return page;
		}

		@Override
		protected void onPostExecute(Document page){
			super.onPostExecute(page);
			if(page != null){
				// It might be dangerous to remove the Loading Wheel by assuming it's position
				// but if it's not there than something's gone seriously wrong. 
				if(timesTable.getChildAt(3) != null){
					timesTable.removeViewAt(3);
				}
				Elements elems = page.body().select("h3, h1, td, div");
				TableRow infoRow = new TableRow(context);
				TableRow timeRow = new TableRow(context);
				int flag = 0;

				for(Element src : elems){
					if(src.nodeName().equals("td") && flag == 0 || flag == 1 ){
						TextView tv = new TextView(context);
						tv.setText(src.text());
						if(flag == 1){
							tv.setGravity(Gravity.RIGHT);
						}
						infoRow.addView(tv);
						flag++;
					}
					else if(src.nodeName().equals("td") && flag == 2){
						TextView tv = new TextView(context);
						tv.setText(src.text());
						TableRow.LayoutParams rowSpanLayout = new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT);
						rowSpanLayout.span = 2;

						tv.setGravity(Gravity.CENTER);
						timeRow.addView(tv, rowSpanLayout);
						timesTable.addView(infoRow);
						infoRow = new TableRow(context);
						timesTable.addView(timeRow);
						timeRow = new TableRow(context);

						View spacerThin = new View(context);
						spacerThin.setBackgroundColor(Color.LTGRAY);
						spacerThin.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, 1));
						timesTable.addView(spacerThin);

						flag++;
					}
					else if(src.nodeName().equals("td") && flag == 3){
						flag = 0;
					}
				}

				if(page.text().equals("Stop departure data is too old.") || timesTable.getChildCount() == 0){
					TextView tv = new TextView(context);
					tv.setText("No upcoming departures.");
					infoRow.addView(tv);
					timesTable.addView(infoRow);
				}
			}

		}

	}

	class MapTimesTable extends TableLayout{
		public MapTimesTable(Context context, String stopNum) {
			super(context);
			makeHeader(stopNum);
			grabMapTimes(stopNum);
			setVisibility(View.VISIBLE);
		}

		public void makeHeader(String stopNum){
			this.removeAllViews();
			this.setStretchAllColumns(true);

			String[] columns = {"_id", "num", "addr"};
			String where = "num LIKE '" + stopNum + "'";
			Cursor cursor = db.query("stops", columns, where, null, null, null, null);
			startManagingCursor(cursor);
			cursor.moveToFirst();
			String addr = cursor.getString(cursor.getColumnIndex("addr"));

			TextView headerStopNum = new TextView(map.this);
			headerStopNum.setText("Stop " + stopNum);
			headerStopNum.setTextSize(20);
			headerStopNum.setGravity(Gravity.CENTER);
			this.addView(headerStopNum);

			TextView headerAddr = new TextView(map.this);
			headerAddr.setText(addr);
			headerAddr.setTextSize(20);
			headerAddr.setGravity(Gravity.CENTER);
			this.addView(headerAddr);

			View spacerThick = new View(map.this);
			spacerThick.setBackgroundColor(Color.LTGRAY);
			spacerThick.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, 4));
			this.addView(spacerThick);

		}

		public void grabMapTimes(String stopNum){
			MapTimesLoader timesLoader = new MapTimesLoader(getContext(), this);
			timesLoader.execute(stopNum);
		}
	}

	// Required by the MapActivity, unsure of it's exact purpose...
	@Override
	protected boolean isRouteDisplayed(){
		return false;
	}

	// Helper class to make correctly formatted GeoPoints for the ManagedOverlay Library
	private static final class LatLonPoint extends GeoPoint {
		public LatLonPoint(double latitude, double longitude) {
			super((int) (latitude * 1E6), (int) (longitude * 1E6));
		}
	}

	public List<ManagedOverlayItem> grabBusCoords(String route){
		List<ManagedOverlayItem> manBusList = new ArrayList<ManagedOverlayItem>();
		Pattern p = Pattern.compile(REGEX_PATTERN);
		Matcher m = p.matcher(route);
		if (m.find()) {
			String routeNum = m.group(0);
			try {
				Parser xPar= Parser.xmlParser();
				Document page = Jsoup.connect("http://bustracker.muni.org/InfoPoint/map/GetVehicleXML.ashx?RouteId=" + routeNum).parser(xPar).get();
				for(int i = 0; i < page.getElementsByAttribute("lng").size(); i++){
					Float lng = Float.parseFloat(page.getElementsByAttribute("lng").get(i).attr("lng"));
					Float lat = Float.parseFloat(page.getElementsByAttribute("lng").get(i).attr("lat"));
					ManagedOverlayItem bus = new ManagedOverlayItem(new LatLonPoint(lat, lng), "Bus", "Bus");
					manBusList.add(bus);
				}			
			}
			catch(IOException e){
				Log.d("grabBusCoords", e.getMessage());
			}
		}
		return manBusList;
	}

	public List<ManagedOverlayItem> grabStopCoordsByDirection(String routeNum, String direction){
		List<ManagedOverlayItem> manStopList = new ArrayList<ManagedOverlayItem>();
		if(direction == "ALL"){
			String[] columns = {"_id", "num", "addr", "lng", "lat", "routes"};
			String where = "routes LIKE '%, " + routeNum + ",%'";
			this.cursor = db.query("stops", columns, where, null, null, null, null);
			startManagingCursor(this.cursor);
			this.cursor.moveToFirst();
			for (int i = 0; i < this.cursor.getCount(); i++){
				Float lat = Float.parseFloat(this.cursor.getString(4));
				Float lng = Float.parseFloat(this.cursor.getString(3));
				GeoPoint stop = new LatLonPoint(lat, lng);
				ManagedOverlayItem overlayitem = new ManagedOverlayItem(stop, "Stop " + this.cursor.getInt(this.cursor.getColumnIndex("num")),
						this.cursor.getString(this.cursor.getColumnIndex("addr")));
				manStopList.add(overlayitem);
				this.cursor.moveToNext();
			}
		}
		else{
			String[] columns = {"_id", "num", "route", "direction"};
			String where = "route LIKE '" + routeNum + "' AND direction LIKE '%, " + direction + ",%'";
			this.cursor = db.query("directions", columns, where, null, null, null, null);
			startManagingCursor(this.cursor);
			this.cursor.moveToFirst();
			List<String> stopNumberList = new ArrayList<String>();
			for(int i = 0; i < this.cursor.getCount(); i++){
				int stopNum = this.cursor.getInt(this.cursor.getColumnIndex("num"));
				stopNumberList.add(String.valueOf(stopNum));
				this.cursor.moveToNext();
			}

			String[] dirColumns = {"_id", "num", "addr", "lng", "lat", "routes"};
			String dirWhere = "";
			for(int j = 0; j < stopNumberList.size(); j++){
				if(j == 0){
					dirWhere = "num LIKE '" + stopNumberList.get(j) + "'";
				}
				else{
					dirWhere = dirWhere.concat(" OR num LIKE '" + stopNumberList.get(j) + "'");
				}
			}
			Cursor singleStopsCursor = db.query("stops", dirColumns, dirWhere, null, null, null, null);
			startManagingCursor(singleStopsCursor);

			singleStopsCursor.moveToFirst();
			for (int i = 0; i < singleStopsCursor.getCount(); i++){
				Float lat = Float.parseFloat(singleStopsCursor.getString(4));
				Float lng = Float.parseFloat(singleStopsCursor.getString(3));
				GeoPoint stop = new LatLonPoint(lat, lng);
				ManagedOverlayItem overlayitem = new ManagedOverlayItem(stop, "Stop " + singleStopsCursor.getInt(singleStopsCursor.getColumnIndex("num")),
						singleStopsCursor.getString(singleStopsCursor.getColumnIndex("addr")));
				manStopList.add(overlayitem);

				singleStopsCursor.moveToNext();
			}
		}
		return manStopList;
	}

	public class SpinnerItemListener implements OnItemSelectedListener {
		public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
			String item = parent.getItemAtPosition(pos).toString();
			Matcher m = main.p.matcher(item);
			if (m.find()) {
				item = m.group(0);
			}
			Log.d("", "item: " + item + " | " + "route: " + route);
			if(!item.equals(null) && !item.equals(route)){
				overlayManager.removeOverlay(stopOverlay);
				overlayManager.removeOverlay(busOverlay);
				overlayManager.populate();
				route = item;

				List<Overlay> mapOverlays = ((MapView) findViewById(R.id.mapview)).getOverlays();
				mapOverlays.clear();
				mapOverlays.add(new LineOverlay(route));
				((MapView) findViewById(R.id.mapview)).invalidate();
				overlayManager.createOverlay("route" + route, getResources().getDrawable(R.drawable.marker));
				stopOverlay = overlayManager.getOverlay("route" + route);
				stopOverlay.addAll(grabStopCoordsByDirection(route, "ALL"));
				stopOverlay.setOnOverlayGestureListener(mogDetector);
				overlayManager.populate();
				overlayManager.createOverlay("bus" + route, getResources().getDrawable(R.drawable.busmarker));
				busOverlay = overlayManager.getOverlay("bus" + route);
				busOverlay.addAll(grabBusCoords(route));
				overlayManager.populate();
				toCallAsync(route);
				

				if(route.equals("14")){
					TextView leftButton = (TextView) findViewById(R.id.left_button);
					leftButton.setText(directionsMap.get(route).get(0));
					leftButton.setSelected(false);
					
					TextView rightButton = (TextView) findViewById(R.id.right_button);
					rightButton.setText("");
					rightButton.setSelected(false);
					rightButton.setClickable(false);
					
				}
					
				else{
					TextView leftButton = (TextView) findViewById(R.id.left_button);
					leftButton.setText(directionsMap.get(route).get(0));
					leftButton.setSelected(false);

					TextView rightButton = (TextView) findViewById(R.id.right_button);
					rightButton.setText(directionsMap.get(route).get(1));
					rightButton.setSelected(false);
				}
			}
			else if(item.equals(route)){
				
			}
		}

		public void onNothingSelected(AdapterView<?> parent) {
			
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		// Save the route the user is on in order to display it on the next run
		SharedPreferences favStops = getPreferences(MODE_PRIVATE);
		SharedPreferences.Editor editor = favStops.edit();
		editor.putString("LAST_MAP_ROUTE", route);
		editor.commit();

		if (cursor != null){
			cursor.close();
		}
		if (db != null){
			db.close();
		}
		if (myDbHelper != null){
			myDbHelper.close();
		}
	}

	public class BusRefresher extends AsyncTask<String, Void, List<ManagedOverlayItem>>{

		@Override
		protected List<ManagedOverlayItem> doInBackground(String... routeNum) {
			return grabBusCoords(routeNum[0]);
		}

		@Override
		protected void onPostExecute(List<ManagedOverlayItem> busList){
			overlayManager.createOverlay("bus" + route, getResources().getDrawable(R.drawable.busmarker));
			if(busList.size() > 0 && busList != null){
				busOverlay = overlayManager.getOverlay("bus" + route);
				busOverlay.addAll(busList);
				overlayManager.populate();
			}
		}
	}

	class LineOverlay extends Overlay{
		public LineOverlay(String routeNum){
			this.routeNum = routeNum;
		}

		private String routeNum = route;
		private GeoPoint gP1 = null;
		private GeoPoint gP2 = null;


		public void draw(Canvas canvas, MapView mapv, boolean shadow){
			super.draw(canvas, mapv, shadow);

			Paint mPaint = new Paint();
			mPaint.setDither(true);
			mPaint.setColor(Color.argb(200, 0, 135, 225));
			mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
			mPaint.setStrokeJoin(Paint.Join.ROUND);
			mPaint.setStrokeCap(Paint.Cap.ROUND);
			mPaint.setStrokeWidth(4);

			double[][] line = lc.l.get(1);
			int routeNumInt = Integer.parseInt(routeNum);
			switch(routeNumInt){
			case 1:			line = lc.l.get(0);break;
			case 2:			line = lc.l.get(1);break;
			case 3:			line = lc.l.get(2);break;
			case 7:			line = lc.l.get(3);break;
			case 8:			line = lc.l.get(4);break;
			case 9:			line = lc.l.get(5);break;
			case 13:		line = lc.l.get(6);break;
			case 14:		line = lc.l.get(7);break;
			case 15:		line = lc.l.get(8);break;
			case 36:		line = lc.l.get(9);break;
			case 45:		line = lc.l.get(10);break;
			case 60:		line = lc.l.get(11);break;
			case 75:		line = lc.l.get(12);break;
			case 102:		line = lc.l.get(13);break;
			default: System.out.println("Switch in LineOverlay draw func defaulted. Bad route number from lc.l"); break;
			}

			for(int i = 0; i < line.length; i++){
				for(int j = 0; j < (line[i].length - 3);j+=2){
					this.gP1 = new LatLonPoint(line[i][j+1], line[i][j]);
					this.gP2 = new LatLonPoint(line[i][j+3], line[i][j+2]);
					Point p1 = new Point();
					Point p2 = new Point();
					Path path = new Path();

					projection.toPixels(this.gP1, p1);
					projection.toPixels(this.gP2, p2);

					path.moveTo(p2.x, p2.y);
					path.lineTo(p1.x,p1.y);

					canvas.drawPath(path, mPaint);
				}
			}
		}
	}

	public static class LineCoords{

		public double[][] line1 = {
				{
					-149.733436,61.211426,-149.733436,61.210808,-149.733436,61.209732,-149.762445,61.209716,-149.762447,61.20919900000001,-149.762451,61.202225,-149.762471,61.202107,-149.762541,61.201934,-149.762654,61.201763,-149.763244,61.20113400000001,-149.763346,61.200928,-149.763386,61.200811,-149.763395,61.200698,-149.763405,61.193539,-149.7633939012783,61.18079027073536,-149.776473,61.180754,-149.7823720566774,61.18081674738851,-149.7891652321584,61.18082095512641,-149.7925619145991,61.18079128022476,-149.7953577370678,61.18076784824243,-149.795344,61.18167600000001,-149.795325,61.18178,-149.795311,61.18183999999999,-149.797586,61.181973,-149.798103,61.181971,-149.798942,61.181954,-149.80082,61.181895,-149.801833,61.18184600000001,-149.802294,61.181813,-149.802713,61.181851,-149.803361,61.18197800000001,-149.8024734014359,61.18300676306946,-149.8022407426849,61.18323064717367,-149.802517274707,61.18334702903534,-149.8029399346671,61.1834382970784,-149.804046,61.183636,-149.804468,61.183682,-149.804907,61.183701,-149.805458,61.183671,-149.805863,61.183608,-149.806588,61.183432,-149.8069670237214,61.18335263052352,-149.8071856338624,61.18330712594753,-149.8073271785985,61.18328113935325,-149.8075517140494,61.18328120068534,-149.807848,61.183278,-149.808199,61.183275,-149.808188,61.183724,-149.808199,61.183915,-149.808235,61.184056,-149.808294,61.18420499999999,-149.808342,61.18431600000001,-149.808446,61.184507,-149.80855,61.184648,-149.808685,61.184808,-149.8101,61.186263,-149.810314,61.18659300000001,-149.810443,61.186911,-149.810477,61.187122,-149.810494,61.18785400000001,-149.810503,61.18872,-149.810582,61.188731,-149.81071,61.18875500000001,-149.81567,61.18967499999999,-149.8163526139767,61.18979858263382,-149.8168757750698,61.18986727431253,-149.8172944387109,61.18986855020921,-149.8184318426046,61.18987018732256,-149.8203736391553,61.18985459282848,-149.8216054473206,61.18971496702785,-149.822074634519,61.18962089420953,-149.822957,61.189291,-149.8235295147368,61.18897612048163,-149.8242484670489,61.18866141713007,-149.8252580557574,61.18835630652459,-149.8262025469753,61.18815619018935,-149.8270603259535,61.18806652294229,-149.8277995909113,61.18806711705091,-149.832115,61.188044,-149.8382651425587,61.18806785098,-149.83819,61.182405,-149.838111,61.17477,-149.8381714334144,61.17054781215403,-149.838111,61.167514,-149.838088,61.165854,-149.838102,61.165716,-149.838035,61.165461,-149.837748,61.164983,-149.837467,61.164658,-149.837124,61.16437599999999,-149.836584,61.164026,-149.835937,61.163616,-149.8349956298117,61.16298956976659,-149.8344985781297,61.16243898105039,-149.8342312855193,61.16169693241477,-149.834176,61.159582,-149.834198123812,61.14630746185947,-149.8341128459627,61.13748295585422,-149.842014,61.137519,-149.842442,61.137542,-149.842692,61.137564,-149.842861,61.137585,-149.843193,61.137635,-149.843665,61.13773,-149.844129,61.137863,-149.844393,61.137954,-149.844703,61.138083,-149.845026,61.138247,-149.845696,61.138621,-149.846551,61.139095,-149.8470282012812,61.13937827457328,-149.8472878760127,61.13961112785086,-149.8474489207588,61.13991424490733,-149.8475184280628,61.14008472059194,-149.8475317097783,61.14081822681445,-149.8475285630743,61.14219833847448,-149.8477548007057,61.14269248321093,-149.8480478605736,61.14312350951681,-149.8484034494932,61.1434595007059,-149.8490382797152,61.14389320753277,-149.849861,61.144219,-149.850705,61.144485,-149.8515750710778,61.14462532788021,-149.8523493864208,61.14468511270776,-149.8530274368697,61.14472809145379,-149.8575384606368,61.14475730309131,-149.8606427511604,61.14470401810264,-149.8640172911017,61.14467821746456,-149.8671737114073,61.14467127008467,-149.8716647610036,61.14466764831426,-149.8716733657313,61.14388917402778,-149.8719686014185,61.14388672900873,-149.8720849782011,61.14388582248886,-149.8722196872576,61.14386437423736,-149.8723298414817,61.14382773083093,-149.8723895749437,61.14375574228355,-149.872389,61.143618,-149.8721158207208,61.14202181254781,-149.871993077772,61.1412732552138,-149.8718635257283,61.1411644109042,-149.8715989224959,61.14112968909607,-149.863998,61.141124,-149.863987,61.141124,-149.863987,61.141124,-149.8639737097228,61.14467012646395
				}
		};

		public double[][] line2 = {
				{
					-149.8640051615039,61.14116568558583,-149.8714830669352,61.14113265285638,-149.8718255416401,61.14120211569429,-149.8720053282228,61.14131453234106,-149.8724181855145,61.14359854650061,-149.8724037044151,61.14372782068273,-149.8723477347297,61.1438223650218,-149.8722491953377,61.14385008838186,-149.8721048764564,61.14387848045171,-149.8718149736473,61.14389465876206,-149.8715275293401,61.14392604816665,-149.8715263987388,61.14470115199469,-149.8691771657631,61.14470870354835,-149.8640559995951,61.14471671376082,-149.863939,61.126093,-149.863837,61.124429,-149.863751,61.123069,-149.863142,61.12307,-149.860467,61.123071,-149.858803,61.12307299999999,-149.857802,61.123072,-149.857465,61.123071,-149.85643,61.12307400000001,-149.856353,61.123075,-149.855168,61.12309799999999,-149.85278,61.12307,-149.850918,61.123075,-149.850918,61.12307599999999,-149.850918,61.123093,-149.850918,61.12309900000001,-149.850918,61.12313700000001,-149.850918,61.123143,-149.850919,61.123674,-149.850913,61.12416399999999,-149.850913,61.124506,-149.850908,61.12456,-149.85087,61.124633,-149.850772,61.12472200000001,-149.850644,61.124795,-149.850459,61.124856,-149.850233,61.124891,-149.850105,61.124899,-149.849195,61.12489800000001,-149.848828,61.12489800000001,-149.847979,61.12489699999999,-149.847994,61.12489699999999,-149.847994,61.124938,-149.847994,61.124948,-149.847988,61.125525,-149.847993,61.12613700000001,-149.847998,61.12639,-149.847972,61.126474,-149.847916,61.126583,-149.847813,61.126692,-149.847716,61.126783,-149.847552,61.126882,-149.847393,61.126962,-149.847316,61.127026,-149.8472283984165,61.12714214731791,-149.8471348177637,61.12725106951191,-149.846888228936,61.12757349781101,-149.8467336758095,61.1278642796059,-149.8465291552985,61.12798879219632,-149.8463045399486,61.12809150020689,-149.8459513374373,61.12822065911001,-149.844832347375,61.12886074760104,-149.845201,61.12911699999999,-149.845488,61.12936,-149.845673,61.129627,-149.845786,61.12984000000001,-149.845814040291,61.1300512387759,-149.8458226306837,61.13098924266285,-149.8458307231848,61.13124596601194,-149.845796,61.13139999999999,-149.845683,61.131618,-149.8455506292125,61.13177061864871,-149.8452261173085,61.13207608528188,-149.8448409852625,61.13237256569288,-149.8444677709839,61.13255909847375,-149.8432785980314,61.13305945564718,-149.8423220805454,61.13347271458773,-149.8420934227519,61.13361227298203,-149.8417743602673,61.13379984002177,-149.8416538265067,61.1340617516097,-149.84156,61.134327,-149.84157,61.135006,-149.84158,61.135695,-149.841592,61.137522,-149.834125,61.137523,-149.834129,61.138137,-149.834124,61.1462,-149.834165,61.14813600000001,-149.834114,61.152343,-149.8342028293573,61.16015631992391,-149.8342107776512,61.16167671981218,-149.834339,61.162142,-149.834647,61.16262700000001,-149.835016,61.163013,-149.835796,61.16352800000001,-149.836658,61.164067,-149.837376,61.164567,-149.837694,61.164903,-149.837919,61.16521500000001,-149.838104,61.165655,-149.838145,61.165996,-149.838124,61.169341,-149.838176,61.173585,-149.838217,61.18447500000001,-149.838232,61.188055,-149.8616059878507,61.18810113214361,-149.8697480264285,61.18807280570962,-149.872365,61.188069,-149.879095,61.188069,-149.886593,61.188075,-149.886604,61.191152,-149.886604,61.194508,-149.886624,61.196624,-149.886686,61.197316,-149.886994,61.198384,-149.887302,61.20078700000001,-149.887322,61.202843,-149.887343,61.204069,-149.887343,61.205453,-149.887343,61.207716,-149.887363,61.210752,-149.887363,61.21607999999999,-149.887352,61.216581,-149.89144,61.216584,-149.903744,61.216582,-149.903722,61.217563,-149.903462,61.217563,-149.899598,61.217575,-149.895403,61.217565,-149.894223,61.217565,-149.887043,61.217575,-149.883277,61.21756599999999,-149.883284,61.215592,-149.883294,61.213624,-149.883356,61.21264100000001,-149.883428,61.211648,-149.883191,61.209679,-149.883007,61.20853399999999,-149.882986,61.208109,-149.883007,61.20770399999999,-149.883171,61.206123,-149.883356,61.20466100000001,-149.883232,61.20348500000001,-149.883027,61.201766,-149.882986,61.200244,-149.883048,61.19821900000001,-149.88313,61.195305,-149.883089,61.191589,-149.883109,61.189505,-149.883124,61.188076
				}

		};

		public double[][] line3 = {
				{
					-149.887352,61.216581,
					-149.888498,61.21658,
					-149.889442,61.216583,
					-149.894508,61.216586,
					-149.899591,61.216584,
					-149.899592,61.216077,
					-149.899594,61.214608,
					-149.899598,61.21363,
					-149.899595,61.207177,
					-149.899569,61.206483,
					-149.899955,61.205913,
					-149.900726,61.205554,
					-149.901124,61.205446,
					-149.901478,61.205378,
					-149.901889,61.205329,
					-149.902217,61.205319,
					-149.902558,61.205313,
					-149.902918,61.205338,
					-149.904287,61.205505,
					-149.905599,61.205669,
					-149.905746,61.205694,
					-149.906422,61.205737,
					-149.907064,61.205725,
					-149.907862,61.205641,
					-149.908273,61.205555,
					-149.908671,61.205452,
					-149.909282,61.205236,
					-149.909796,61.205,
					-149.91157,61.2041,
					-149.912049,61.203787,
					-149.912316,61.203575,
					-149.91274,61.203096,
					-149.912971,61.202619,
					-149.913078,61.20054,
					-149.913637,61.20061,
					-149.914093,61.2007,
					-149.914421,61.200796,
					-149.914704,61.200901,
					-149.915425,61.201241,
					-149.915425,61.201241,
					-149.914525,61.202119,
					-149.913329,61.203284,
					-149.91275,61.203779,
					-149.911773,61.204343,
					-149.910706,61.204801,
					-149.908045,61.206101,
					-149.906426,61.206876,
					-149.904973,61.207359,
					-149.904459,61.207526,
					-149.903893,61.207898,
					-149.9037,61.208182,
					-149.903674,61.208554,
					-149.903674,61.210679,
					-149.9037,61.213625
				},
				{
					-149.9037,61.213625,
					-149.899597,61.213629,
					-149.89142,61.213627,
					-149.887358,61.213625,
					-149.887351,61.216581
				},

				{
					-149.913078,61.20054,
					-149.913088,61.195297,
					-149.905442,61.195299,
					-149.905422,61.197308,
					-149.905422,61.198448,
					-149.905474,61.198764,
					-149.905679,61.199111,
					-149.906142,61.199507,
					-149.906669,61.199779,
					-149.907865,61.200331,
					-149.908601,61.200677,
					-149.911379,61.200445,
					-149.911694,61.200431,
					-149.912028,61.200438,
					-149.912314,61.200453,
					-149.912468,61.200465,
					-149.913078,61.20054
				},

				{
					-149.905442,61.195299,
					-149.90537,61.195299,
					-149.897941,61.195301,
					-149.896801,61.195297,
					-149.892474,61.195297,
					-149.887765,61.195305,
					-149.883157,61.195301,
					-149.879632,61.195284,
					-149.871192,61.195284,
					-149.861684,61.195284,
					-149.858337,61.195276,
					-149.857621,61.195263,
					-149.858173,61.19516,
					-149.858783,61.194984,
					-149.859442,61.194714,
					-149.860181,61.194367,
					-149.860768,61.194092,
					-149.861098,61.193946,
					-149.861783,61.193706,
					-149.862522,61.193543,
					-149.863261,61.193457,
					-149.863858,61.193427,
					-149.866164,61.193423,
					-149.869333,61.193418,
					-149.874319,61.193419,
					-149.875431,61.193424,
					-149.876687,61.193484,
					-149.877399,61.193531,
					-149.87852,61.193642,
					-149.881895,61.194063,
					-149.882963,61.194153,
					-149.884014,61.19423,
					-149.885171,61.194251,
					-149.889275,61.194256,
					-149.890219,61.19426,
					-149.893451,61.19426,
					-149.896032,61.19426,
					-149.897955,61.194187,
					-149.898881,61.194161,
					-149.899647,61.194166,
					-149.900822,61.194213,
					-149.902264,61.194226,
					-149.903057,61.194187,
					-149.903689,61.19414,
					-149.905441,61.193945,
					-149.905442,61.195299
				},

				{
					-149.857628,61.195262,
					-149.857574,61.19527,
					-149.856926,61.19535,
					-149.85641,61.195367,
					-149.855386,61.195375,
					-149.839976,61.195384,
					-149.838987,61.195371,
					-149.838258,61.195375,
					-149.838251,61.192628,
					-149.838242,61.191371,
					-149.838234,61.188051,
					-149.837325,61.188066,
					-149.83582,61.188064,
					-149.83448,61.188056,
					-149.832989,61.188054,
					-149.829223,61.188039,
					-149.82864,61.188043,
					-149.827928,61.18804,
					-149.827523,61.188049,
					-149.827127,61.188064,
					-149.826526,61.188118,
					-149.825671,61.188245,
					-149.824861,61.188438,
					-149.824122,61.188691,
					-149.823356,61.189051,
					-149.823062,61.189232,
					-149.822893,61.18933,
					-149.822706,61.189395,
					-149.82227,61.18954,
					-149.822045,61.189612,
					-149.821972,61.189626,
					-149.821336,61.189748,
					-149.820329,61.189844,
					-149.819537,61.189846,
					-149.817957,61.189844,
					-149.817242,61.189845,
					-149.817172,61.189845,
					-149.816461,61.189806,
					-149.816064,61.189746,
					-149.815819,61.189705
				},

				{
					-149.822045,61.189612,
					-149.822047,61.189614,
					-149.822773,61.190424,
					-149.823396,61.191316,
					-149.824162,61.192423,
					-149.825292,61.194036,
					-149.825604,61.194495,
					-149.8258,61.194693,
					-149.826387,61.19507,
					-149.827091,61.195473,
					-149.827546,61.195723
				},

				{
					-149.827546,61.195723,
					-149.827546,61.195723,
					-149.827172,61.19589,
					-149.826318,61.196392,
					-149.82516,61.1971,
					-149.824039,61.197777,
					-149.823388,61.198082,
					-149.822685,61.198335,
					-149.821902,61.198545,
					-149.82094,61.198734,
					-149.819818,61.19885,
					-149.819106,61.19888,
					-149.813008,61.198872,
					-149.808361,61.198872,
					-149.801203,61.198872,
					-149.800268,61.198807,
					-149.799244,61.198623,
					-149.798612,61.198421,
					-149.797891,61.198108,
					-149.797286,61.197692,
					-149.796538,61.197139,
					-149.795006,61.196015,
					-149.794116,61.195621,
					-149.792888,61.195346,
					-149.791766,61.195243,
					-149.789914,61.195235,
					-149.785855,61.195235,
					-149.778614,61.195251,
					-149.778374,61.195252,
					-149.775704,61.195267,
					-149.771502,61.195258,
					-149.768778,61.195275,
					-149.760373,61.195275,
					-149.755904,61.195275,
					-149.748435,61.19527,
					-149.748435,61.195297,
					-149.748435,61.195317,
					-149.74843,61.196802,
					-149.748377,61.196802,
					-149.746,61.196805,
					-149.745537,61.196886,
					-149.74527,61.197032,
					-149.745163,61.197152,
					-149.745119,61.19729,
					-149.745136,61.197959,
					-149.745136,61.19886,
					-149.74511,61.19904,
					-149.745021,61.19928,
					-149.744789,61.199567,
					-149.744175,61.20034,
					-149.743845,61.200575,
					-149.743418,61.200829,
					-149.74251,61.201331,
					-149.742225,61.201532,
					-149.741993,61.201747,
					-149.741797,61.202064,
					-149.741744,61.202291,
					-149.741752,61.202875,
					-149.743063,61.202875,
					-149.743215,61.202884,
					-149.743339,61.202903,
					-149.743446,61.202921,
					-149.743566,61.202951,
					-149.744688,61.203335,
					-149.744915,61.20339,
					-149.745062,61.203412,
					-149.745271,61.203427,
					-149.745316,61.203427,
					-149.747475,61.203422,
					-149.748439,61.203425,
					-149.748431,61.204687,
					-149.748431,61.206403,
					-149.748423,61.208324,
					-149.748423,61.209426,
					-149.748417,61.209722,
					-149.736087,61.209726,
					-149.73344,61.209732,
					-149.733442,61.209854,
					-149.733441,61.2119,
					-149.733458,61.212526,
					-149.733441,61.215073,
					-149.733458,61.216266,
					-149.733485,61.218709,
					-149.733512,61.220565,
					-149.733512,61.222379,
					-149.733521,61.224177,
					-149.733312,61.224129,
					-149.732951,61.224129,
					-149.731486,61.224138,
					-149.729773,61.22414,
					-149.729759,61.222837,
					-149.729763,61.222373,
					-149.723205,61.222381,
					-149.722368,61.222373,
					-149.720383,61.222386,
					-149.717417,61.222375,
					-149.717417,61.222426,
					-149.717404,61.22321,
					-149.717421,61.223351,
					-149.717519,61.223578,
					-149.717751,61.223857,
					-149.71784,61.22402,
					-149.717867,61.224195,
					-149.717724,61.224392,
					-149.717466,61.224542,
					-149.71727,61.224581,
					-149.716793,61.224727,
					-149.716047,61.224958,
					-149.715982,61.224911

				},

				{
					-149.733512,61.222369,
					-149.729763,61.222373
				},

				{
					-149.778373,61.195252,
					-149.778376,61.198886,
					-149.778337,61.198886,
					-149.77719,61.198887,
					-149.777228,61.199032,
					-149.777339,61.199186,
					-149.77757,61.199343,
					-149.777704,61.199416,
					-149.777877,61.199566,
					-149.777935,61.199688,
					-149.777935,61.199859,
					-149.777949,61.200194,
					-149.777984,61.200971,
					-149.777979,61.201514,
					-149.77758,61.201523,
					-149.7771,61.201592,
					-149.776708,61.201734,
					-149.776414,61.201909,
					-149.776227,61.202141,
					-149.776191,61.20224,
					-149.77623,61.202849,
					-149.776646,61.202848,
					-149.776739,61.202858,
					-149.776835,61.202879,
					-149.776877,61.202902,
					-149.776911,61.202942,
					-149.776931,61.203004,
					-149.776929,61.203067,
					-149.776739,61.20385,
					-149.776703,61.204073,
					-149.77657,61.204359,
					-149.776401,61.204581,
					-149.773213,61.203996,
					-149.7732,61.203992,
					-149.77316,61.203984,
					-149.773133,61.203977,
					-149.772952,61.204224,
					-149.771136,61.205647,
					-149.771011,61.205742,
					-149.770077,61.207144,
					-149.77005,61.207277,
					-149.770059,61.208207,
					-149.770066,61.209722
				},

				{
					-149.770066,61.209722,
					-149.748417,61.209722
				}


		};

		public double[][] line7 = {
				{
					-149.899605,61.213631, -149.899591,61.21658300000001, -149.887348,61.216584, -149.887358,61.213625, -149.895518,61.21365500000001, -149.899601,61.213629
				},

				{
					-149.899605,61.213634, -149.899605,61.213629, -149.903706,61.213626, -149.903684,61.20822900000001, -149.903732,61.208099, -149.903938,61.207846, -149.904147,61.207703, -149.904602,61.20747600000001, -149.906482,61.206849, -149.907818,61.206191, -149.90983,61.205209, -149.911933,61.20424100000001, -149.912685,61.203783, -149.913146,61.203458, -149.914253,61.202401, -149.915419,61.201239, -149.914476,61.20081, -149.914016,61.200684, -149.913066,61.20053500000001, -149.912284,61.200453, -149.911955,61.20043600000001, -149.911606,61.20043500000001, -149.910882,61.20048500000001, -149.908601,61.20067800000001, -149.908953,61.200887, -149.909136,61.201039, -149.909411,61.201509, -149.909387,61.201812, -149.90865,61.202315, -149.908082,61.202521, -149.90704,61.202838, -149.906426,61.20308, -149.90605,61.203316, -149.905789,61.20353100000001, -149.905412,61.20383, -149.905197,61.20411100000001, -149.904903,61.204835, -149.904565,61.205065, -149.904324,61.205167, -149.903975,61.205241, -149.903653,61.205261, -149.902533,61.205269, -149.901839,61.20530900000001, -149.901437,61.20538300000001, -149.900892,61.205507, -149.900466,61.20565800000001, -149.90009,61.205857, -149.899817,61.20609, -149.899659,61.206297, -149.899578,61.210192, -149.899601,61.213631
				},

				{
					-149.908604,61.200679, -149.908591,61.200672, -149.906527,61.19972700000001, -149.905899,61.199335, -149.905471,61.198891, -149.905396,61.198439, -149.905403,61.196265, -149.905437,61.192327, -149.906897,61.191239, -149.908681,61.1889, -149.908768,61.18799, -149.908837,61.187813, -149.908999,61.18765000000001, -149.909405,61.18739500000001, -149.909834,61.187251, -149.91034,61.187133, -149.911214,61.18703, -149.913881,61.18671000000001, -149.916671,61.186298, -149.920717,61.185657, -149.921361,61.185613, -149.922449,61.185495, -149.922698,61.18545600000001, -149.923049,61.185376, -149.92386,61.185102, -149.925685,61.184341, -149.927157,61.18372, -149.928423,61.183083, -149.929374,61.18261, -149.929742,61.18246600000001, -149.930263,61.18231800000001, -149.93093,61.182212, -149.932493,61.182105, -149.933151,61.18203500000001, -149.934079,61.181877, -149.935014,61.18164800000001, -149.937359,61.18095600000001, -149.93886,61.179243, -149.940444,61.17772800000001, -149.94345,61.17625000000001, -149.943941,61.17554, -149.943462,61.17378800000001
				},

				{
					-149.943458,61.173785, -149.950554,61.17429700000001, -149.951895,61.17454600000001, -149.952787,61.17476400000001, -149.957986,61.176235, -149.959162,61.176484, -149.959989,61.176586, -149.961561,61.176671, -149.968123,61.176668, -149.968154,61.176742, -149.968158,61.17684800000001, -149.968179,61.17693100000001, -149.968204,61.17698000000001, -149.968381,61.177153, -149.968891,61.177377, -149.975122,61.1776, -149.979221,61.177787, -149.979883,61.177841, -149.980379,61.177894, -149.980949,61.178018, -149.981537,61.178267, -149.982729,61.179796, -149.984489,61.179484, -149.984595,61.17945300000001, -149.984809,61.179337, -149.98491,61.179177, -149.984892,61.17907100000001, -149.984276,61.178245, -149.984074,61.17814800000001, -149.983835,61.17808500000001, -149.983587,61.178072, -149.983412,61.178081, -149.9823,61.17827200000001, -149.981942,61.17832, -149.981548,61.178259
				},

				{
					-149.968124,61.176668, -149.969533,61.176768, -149.981485,61.17761500000001, -149.982156,61.177613, -149.982689,61.177516, -149.982965,61.1774, -149.98325,61.17718700000001, -149.983369,61.17693400000001, -149.983333,61.17681000000001, -149.983176,61.17655200000001, -149.9828,61.17613100000001, -149.982212,61.175416, -149.981707,61.17475000000001, -149.981348,61.17425300000001, -149.980869,61.173639, -149.980768,61.173546, -149.98041,61.17334600000001, -149.980116,61.173244, -149.979675,61.173168, -149.979178,61.173141, -149.978655,61.173155, -149.976844,61.17330500000001, -149.97655,61.173354, -149.976155,61.173469, -149.975263,61.174135, -149.974279,61.17489800000001, -149.973506,61.17547500000001, -149.972945,61.17591, -149.972665,61.17615600000001, -149.972237,61.176342, -149.971677,61.176495, -149.971212,61.176564, -149.970702,61.176595, -149.969388,61.176608, -149.968565,61.176614, -149.968122,61.176668
				},

				{
					-149.968122,61.176668, -149.968092,61.174196, -149.965293,61.174193, -149.961921,61.174188, -149.95818,61.17418600000001, -149.956333,61.174199, -149.954688,61.174194, -149.953613,61.174167, -149.951481,61.17406, -149.946059,61.173743, -149.945389,61.17368, -149.944847,61.173565, -149.944222,61.173325, -149.943827,61.173089, -149.943258,61.17269000000001, -149.942735,61.17233000000001, -149.942524,61.172246, -149.942248,61.17218400000001, -149.941917,61.17221, -149.941781,61.172232, -149.941459,61.172322
				},

				{
					-149.943459,61.17378600000001, -149.943342,61.173539, -149.943085,61.173282, -149.942663,61.173042, -149.94213,61.172766, -149.941946,61.17266, -149.941459,61.17232, -149.940839,61.171971, -149.940575,61.17178300000001, -149.93941,61.170992, -149.939153,61.17081, -149.938933,61.170583, -149.938731,61.170339, -149.938622,61.17000600000001, -149.938595,61.169696, -149.938696,61.169389, -149.939,61.168972, -149.939911,61.168298, -149.940068,61.16820100000001, -149.941342,61.167325, -149.941852,61.167081, -149.942293,61.166919, -149.942845,61.166748, -149.943224,61.16668, -149.944296,61.166532, -149.944875,61.166506, -149.946832,61.166515, -149.948605,61.16652500000001, -149.949542,61.166441, -149.95048,61.16621500000001, -149.951316,61.165869, -149.951832,61.165452, -149.952099,61.16515, -149.952218,61.164787, -149.952174,61.16408500000001, -149.951922,61.15912400000001, -149.940901,61.159211, -149.940079,61.159224, -149.939601,61.15924, -149.938062,61.159312, -149.936841,61.159376, -149.93682,61.16275300000001, -149.938873,61.162761, -149.940211,61.16454100000001, -149.943224,61.16668
				},

				{
					-149.951925,61.159136, -149.951921,61.158991, -149.951898,61.15192700000001, -149.951858,61.141087, -149.929458,61.141104, -149.92328,61.141101, -149.923041,61.141105, -149.922573,61.141167, -149.922389,61.14121600000001, -149.922049,61.14139700000001, -149.921948,61.141553, -149.922049,61.151936, -149.942282,61.15193, -149.951898,61.151926
				},

				{
					-149.92946,61.141104, -149.929458,61.137484, -149.921979,61.137484, -149.919441,61.13748200000001, -149.919019,61.13749600000001, -149.917729,61.137621, -149.916379,61.137904, -149.913724,61.13866100000001, -149.910977,61.139445, -149.909103,61.139976, -149.907651,61.14037500000001, -149.894548,61.144044, -149.892673,61.14454000000001, -149.891644,61.144681, -149.890487,61.14476, -149.885952,61.144739, -149.880369,61.144734, -149.871526,61.14473200000001, -149.871553,61.14387800000001, -149.871972,61.143876, -149.872239,61.143829, -149.872368,61.143749, -149.872396,61.143634, -149.871855,61.14113, -149.863997,61.141127, -149.863991,61.142565, -149.864027,61.144744
				},

				{
					-149.864027,61.144744, -149.871529,61.144731
				}

		};

		public double[][] line8 = {
				{
					-149.8246601509736,61.21534232490735, -149.823994,61.215502, -149.819085,61.215445, -149.81802,61.215559, -149.813702,61.21558799999999, -149.811159,61.215929, -149.809444,61.216072, -149.808438,61.215901, -149.804663087829,61.21593190474574, -149.8045845198007,61.21873294855961, -149.7933358362226,61.21873720827633, -149.7933022752355,61.22020010814831
				},

				{
					-149.793323464433,61.22020334416347,-149.7910575486326,61.22005365474301,-149.7904938173014,61.22024595730242,-149.789748,61.22024300000001
				},

				{
					-149.7933058358287,61.21921330688932, -149.7897639719926,61.21922330723491, -149.7897474279156,61.22024353951842, -149.786081,61.22024300000001, -149.786111,61.220599, -149.782651,61.220599, -149.78197,61.220557, -149.78132,61.2204, -149.780906,61.220087, -149.781261,61.22000100000001, -149.781527,61.219674, -149.781556,61.21864900000001, -149.77857,61.21864900000001
				},

				{
					-149.7785772094371,61.21864501408879, -149.7781396006606,61.21865710090141, -149.770851,61.218706, -149.770851,61.21693999999999, -149.76881,61.21693999999999, -149.767982,61.216698, -149.766918,61.216157, -149.76606,61.216542, -149.764493,61.21657, -149.760826,61.216528, -149.760264,61.216172, -149.758962,61.215787, -149.757987,61.215702, -149.755946,61.215702, -149.756035,61.222906, -149.755414,61.222892, -149.755177,61.222977, -149.755,61.223077, -149.755029,61.22366, -149.754674,61.224031, -149.754112,61.224159, -149.753876,61.22418699999999, -149.744797,61.224159, -149.744738,61.21693999999999, -149.733471,61.216955, -149.733441,61.20985
				},

				{
					-149.7935845945598,61.21960140261139,-149.7933180186637,61.21973145089694,-149.7930068878516,61.21960181891895		
				},

				{
					-149.7916004334599,61.22024243636955, -149.7913422445748,61.22006784379232, -149.7915988837374,61.21993711197702
				},

				{
					-149.7888552788364,61.22039361189561, -149.7886276177678,61.22024365033517, -149.7888627111143,61.22008775078595
				},

				{
					-149.7899944013216,61.21993178725366, -149.789727058682,61.21980185812641, -149.7894046497073,61.21993214222035
				},

				{
					-149.7914681569628,61.21933244181958, -149.7917354818843,61.21922196432928, -149.7914671161922,61.21905133691558
				},

				{
					-149.7940486617443,61.21885952253943, -149.7944522399802,61.21873141812904, -149.7940906288077,61.21858675909178
				},

				{
					-149.8684071345003,61.21666169528919, -149.8680940326198,61.21656122565501, -149.8684185976232,61.21646874967916
				},

				{
					-149.8246632363102,61.21533889669063, -149.8265093871396,61.21753967356953, -149.8366419233469,61.21758166400283, -149.8366801717173,61.21952230869301, -149.8455705810024,61.21952738535519, -149.8496371278223,61.21931072357388, -149.853199898913,61.21948795657193, -149.8709884575448,61.2195376417651, -149.8709628172993,61.21755255216542, -149.9037643828248,61.21759731432696, -149.9037519707703,61.21656780578929, -149.8669267949439,61.2165617553276, -149.8668912501268,61.21852458063639, -149.8621350239859,61.21854719014086, -149.8605463065569,61.21887232313584, -149.8596186956419,61.21921098647571, -149.8591149842196,61.2195029550652
				},

				{
					-149.8682705310232,61.21965742283987, -149.8687249543821,61.21953205002409, -149.8682703229582,61.21941917663888
				}

		};

		public double[][] line9 = {
				{
					-149.897939,61.166515, -149.89799,61.16554099999999, -149.897737,61.165127, -149.892024,61.162401, -149.892075,61.15186, -149.892024,61.144581, -149.891013,61.144703, -149.87797,61.14475099999999, -149.863967,61.14475099999999, -149.863916,61.1411, -149.871853,61.141149, -149.87246,61.143535, -149.872157,61.14387499999999, -149.8715,61.14390000000001, -149.8715,61.14477600000001
				},

				{
					-149.891418,61.21364599999999, -149.891468,61.20529499999999, -149.896928,61.20527100000001, -149.897484,61.205247, -149.897838,61.205052, -149.897787,61.201595, -149.897838,61.200621, -149.89799,61.198747, -149.897939,61.166515
				},

				{
					-149.887222,61.216591, -149.897636,61.216616, -149.897585,61.21364599999999, -149.887424,61.213573, -149.887323,61.216567
				}

		};

		public double[][] line13 = {
				{
					-149.733393,61.21106,
					-149.733511,61.195332,
					-149.736469,61.195303,
					-149.778347,61.195218,
					-149.778288,61.189976,
					-149.778347,61.189748,
					-149.778879,61.189663,
					-149.784085,61.189663,
					-149.78509,61.189777,
					-149.7858,61.189292,
					-149.785622,61.188722,
					-149.784794,61.188238,
					-149.784913,61.184135,
					-149.785682,61.183308,
					-149.785622,61.18177,
					-149.792129,61.181713,
					-149.79207,61.180801,
					-149.795442,61.180801,
					-149.795382,61.181912,
					-149.801061,61.181941,
					-149.802126,61.181827,
					-149.803486,61.181941,
					-149.802066,61.183194,
					-149.804492,61.183736,
					-149.80532,61.183707,
					-149.80739,61.183308,
					-149.808218,61.183308,
					-149.8081,61.184078,
					-149.810288,61.186385,
					-149.810465,61.188693,
					-149.816676,61.189862,
					-149.820461,61.189834,
					-149.822058,61.189634,
					-149.824129,61.188751,
					-149.82614,61.188153,
					-149.829334,61.18801,
					-149.838443,61.187982,
					-149.838266,61.202512,
					-149.83448,61.202512,
					-149.834421,61.202085,
					-149.836373,61.2016,
					-149.838266,61.201629
				},

				{
					-149.834598,61.202541,
					-149.827087,61.202541,
					-149.825726,61.204706,
					-149.824188,61.20519,
					-149.824011,61.205646,
					-149.82336,61.206187,
					-149.82336,61.211145,
					-149.824247,61.211117,
					-149.823833,61.211145
				},

				{
					-149.823952,61.211174,
					-149.824839,61.210604,
					-149.825489,61.210262,
					-149.826318,61.210234,
					-149.826968,61.210234,
					-149.826909,61.209692,
					-149.830399,61.209721,
					-149.831878,61.209322,
					-149.833061,61.208809,
					-149.834539,61.208011,
					-149.83661,61.207698,
					-149.839153,61.207669,
					-149.858909,61.207698,
					-149.858731,61.206131,
					-149.858199,61.205789,
					-149.858081,61.205105,
					-149.855064,61.20502,
					-149.855064,61.203424,
					-149.860624,61.203396,
					-149.861452,61.203453,
					-149.861334,61.203965,
					-149.86163,61.204991,
					-149.858022,61.205134
				},

				{
					-149.858909,61.207584,
					-149.858791,61.212057,
					-149.858791,61.21257,
					-149.859264,61.212598,
					-149.860979,61.212627,
					-149.860861,61.211687,
					-149.860506,61.211601,
					-149.858791,61.21163
				},

				{
					-149.860861,61.212598,
					-149.860742,61.21368,
					-149.866184,61.213623,
					-149.869023,61.213595,
					-149.868905,61.20972,
					-149.879138,61.20972,
					-149.87902,61.217669,
					-149.903507,61.217697,
					-149.903507,61.216643,
					-149.87896,61.216672
				}

		};

		public double[][] line14 = {
				{
					-149.855354,61.228983,
					-149.855006,61.229637,
					-149.855164,61.230093,
					-149.855859,61.230443,
					-149.856618,61.230565,
					-149.859524,61.23058,
					-149.861136,61.230747,
					-149.86142,61.230686,
					-149.862147,61.229865,
					-149.869224,61.230869,
					-149.871183,61.231112,
					-149.871688,61.231173,
					-149.872352,61.231158,
					-149.87409,61.230702,
					-149.874342,61.230397,
					-149.867929,61.229546,
					-149.858071,61.228147,
					-149.858545,61.228238
				},

				{
					-149.858229,61.228177,
					-149.857629,61.229303,
					-149.855322,61.228968
				},

				{
					-149.874373,61.230397,
					-149.875005,61.229348,
					-149.87529,61.228861,
					-149.875953,61.228497,
					-149.876585,61.228375,
					-149.878449,61.228268,
					-149.879555,61.228208,
					-149.880029,61.228132,
					-149.883378,61.227341,
					-149.88401,61.227143,
					-149.884515,61.226945,
					-149.884926,61.22652,
					-149.885021,61.226155,
					-149.885147,61.224147,
					-149.885147,61.221531,
					-149.884515,61.220604,
					-149.883283,61.219493,
					-149.883315,61.216559,
					-149.887359,61.216574,
					-149.887359,61.21957,
					-149.887011,61.219661,
					-149.885684,61.220543,
					-149.885179,61.221076,
					-149.885116,61.221547
				},

				{
					-149.887359,61.21662,
					-149.887391,61.213624,
					-149.897533,61.213624,
					-149.897533,61.216589,
					-149.887264,61.216574
				}


		};

		public double[][] line15 = {
				{
					-149.7333429799698,61.20955105455339, -149.7333685338082,61.2069930629086, -149.7390680768185,61.20699809352438, -149.7398132780544,61.20709348120564, -149.7404014852615,61.20729139555273, -149.7408390312864,61.20748991539252, -149.7410999243051,61.20774528712615, -149.7413103282351,61.2079560518442, -149.7414533999444,61.20817087943841, -149.7414563382843,61.20966588683565, -149.784058,61.20972299999999, -149.808299,61.20972299999999, -149.808334,61.21596000000001, -149.809077,61.215977, -149.80975,61.216011, -149.810741,61.215977, -149.812333,61.215739, -149.812899,61.215619, -149.813678,61.215534, -149.818101,61.215568, -149.819411,61.215483, -149.8237370245952,61.21552802367052, -149.8247419020062,61.21535003157119, -149.823516,61.213745, -149.823339,61.213267, -149.823374,61.21110300000001, -149.824153,61.21112, -149.824365,61.210865, -149.824967,61.210558, -149.825321,61.210404, -149.8258199377894,61.21017299160922, -149.82709,61.210234, -149.827161,61.209672, -149.82978,61.209706, -149.830806,61.20960300000001, -149.832222,61.209245, -149.832929,61.208888, -149.833885,61.208325, -149.834663,61.20801800000001, -149.835513,61.207848, -149.836468,61.20772899999999, -149.8462348746136,61.2076891506825, -149.853313,61.207712, -149.874829,61.207712, -149.879182,61.207695, -149.879182,61.216574, -149.897584,61.21660800000001, -149.8975842198851,61.21362166363069, -149.879191558634,61.21361255538483	
				}

		};

		public double[][] line36 = {
				{
					-149.913093,61.195291, -149.913079,61.201141, -149.913007,61.202355, -149.912936,61.202753, -149.91268,61.203171, -149.912224,61.203658, -149.911612,61.204083, -149.910431,61.204687, -149.909549,61.205126, -149.908737,61.205448, -149.90767,61.205681, -149.906673,61.20572899999999, -149.906033,61.20572899999999, -149.903257,61.205373, -149.902645,61.20533099999999, -149.901862,61.205325, -149.901065,61.205455, -149.900382,61.205695, -149.899983,61.20592099999999, -149.899684,61.206271, -149.899571,61.206552, -149.899613,61.210544, -149.899571,61.21658600000001, -149.887372,61.216579, -149.887372,61.213623, -149.899613,61.21363
				},

				{
					-149.899377,61.213631, -149.903678,61.213631, -149.903678,61.209692, -149.903648,61.20830599999999, -149.90392,61.207868, -149.904344,61.20756200000001, -149.906222,61.206979, -149.908554,61.205841, -149.91019,61.204908, -149.910614,61.204689, -149.91131,61.20425099999999
				},

				{
					-149.924153,61.195305, -149.920395,61.195284, -149.918345,61.195298, -149.913093,61.195291
				},

				{
					-149.942781,61.188974, -149.943521,61.189001, -149.944233,61.189152, -149.945371,61.18961800000001, -149.945997,61.18981000000001, -149.946908,61.189948, -149.947022,61.189948, -149.957811,61.189934, -149.95784,61.195284, -149.946595,61.195298, -149.945826,61.19531199999999, -149.945058,61.19533899999999, -149.942809,61.19538, -149.935322,61.19540799999999, -149.929714,61.19527100000001, -149.924077,61.195298, -149.922938,61.195229, -149.921971,61.19506500000001, -149.92069,61.194722, -149.920177,61.194557, -149.918469,61.193981, -149.917188,61.193775, -149.916021,61.193652, -149.91306,61.193693, -149.913089,61.19535299999999
				},

				{
					-149.805168,61.191978, -149.803631,61.191964, -149.802264,61.191539, -149.801553,61.19077100000001, -149.801581,61.190057, -149.802549,61.189234, -149.803375,61.188809, -149.804172,61.18857600000001, -149.80457,61.188507, -149.806079,61.188343, -149.806848,61.188315, -149.808129,61.188397, -149.810605,61.188699, -149.816498,61.18981000000001, -149.817181,61.189838, -149.820398,61.189865, -149.822049,61.18961800000001, -149.822989,61.189316, -149.823757,61.188809, -149.825038,61.18838400000001, -149.826319,61.188137, -149.826917,61.18808200000001, -149.827714,61.188027, -149.885817,61.188069, -149.908761,61.18808200000001, -149.908818,61.187835, -149.909188,61.187506, -149.909388,61.187369, -149.909986,61.187177, -149.913117,61.186806, -149.913317,61.18077, -149.914512,61.180852, -149.919409,61.18083800000001, -149.919409,61.17721700000001, -149.927864,61.17721700000001, -149.927864,61.182389, -149.927892,61.182663, -149.928405,61.183116, -149.929373,61.182622, -149.929857,61.18243, -149.93071,61.18225199999999, -149.932845,61.182059, -149.93424,61.18186699999999, -149.937371,61.180962, -149.93888,61.179247, -149.939449,61.17933, -149.941898,61.180084, -149.942468,61.180358, -149.942724,61.180715, -149.942838,61.180866, -149.942809,61.189001	
				}

		};

		public double[][] line45 = {
				{
					-149.859325,61.219505, -149.860077,61.21903, -149.861721,61.21860000000001, -149.86299,61.218532, -149.863882,61.218577, -149.865856,61.218532, -149.883239,61.218577, -149.883286,61.216609, -149.887421,61.21658600000001, -149.887374,61.21360000000001, -149.897663,61.213623, -149.897569,61.21656400000001, -149.887374,61.216609, -149.88728,61.217084, -149.887374,61.219527
				},

				{
					-149.802007,61.183221, -149.801819,61.18339000000001, -149.801373,61.183526, -149.800527,61.183617, -149.799729,61.18360599999999, -149.798765,61.183617, -149.797192,61.183526, -149.796463,61.183481, -149.795806,61.18337900000001, -149.795383,61.183198, -149.795148,61.183006, -149.794937,61.182689, -149.795007,61.182451, -149.795289,61.182043, -149.795312,61.181862, -149.795336,61.18183999999999, -149.797544,61.18197500000001, -149.797967,61.18196400000001, -149.798648,61.18195300000001, -149.799564,61.181941, -149.799893,61.18193, -149.800574,61.181919, -149.801537,61.181862, -149.802219,61.181817, -149.802547,61.18183999999999, -149.802994,61.181896, -149.803323,61.181987, -149.802782,61.182632, -149.801984,61.183221
				},

				{
					-149.808315,61.20978400000001, -149.808315,61.198883, -149.818698,61.19886, -149.819449,61.19886, -149.820013,61.198792, -149.820859,61.198747, -149.822268,61.198476, -149.823161,61.19813600000001, -149.824335,61.197706, -149.827483,61.195761, -149.827577,61.195671, -149.825933,61.194743, -149.825557,61.194517, -149.825322,61.193974, -149.82208,61.189564, -149.821234,61.189723, -149.820389,61.189836, -149.816818,61.189813, -149.816395,61.189791, -149.815832,61.189723, -149.810429,61.188682, -149.810523,61.187416, -149.810382,61.186805, -149.810194,61.186443, -149.80869,61.18477, -149.808455,61.184408, -149.80822,61.18415900000001, -149.80822,61.184001, -149.808173,61.183255, -149.807046,61.183322, -149.805918,61.18357100000001, -149.805026,61.183684, -149.803992,61.183684, -149.802019,61.183209
				},

				{
					-149.791777,61.223332, -149.791918,61.222541, -149.7922,61.22206599999999, -149.792482,61.22141000000001, -149.793374,61.220188, -149.793468,61.219578, -149.793327,61.219171, -149.793327,61.218786, -149.800657,61.218763, -149.80061,61.21494100000001, -149.800703,61.21494100000001, -149.802066,61.214986, -149.804603,61.214986, -149.804697,61.21595899999999, -149.808502,61.215936, -149.809771,61.216027, -149.810523,61.215981, -149.811698,61.215868, -149.812684,61.215665, -149.812167,61.21476, -149.812167,61.212747, -149.812449,61.211662, -149.812496,61.211187, -149.81259,61.210893, -149.812355,61.210531, -149.812402,61.209694, -149.808315,61.209716, -149.808456,61.211639, -149.809489,61.21159399999999, -149.810147,61.21152600000001, -149.812402,61.211662
				},

				{
					-149.887455,61.219475, -149.86124,61.219475, -149.853441,61.21952, -149.849588,61.219384, -149.846018,61.219565, -149.836715,61.21952, -149.834366,61.219837, -149.828634,61.223276, -149.826191,61.22409, -149.824312,61.224136, -149.808526,61.22422600000001, -149.808432,61.22952, -149.797251,61.22952, -149.797157,61.225719, -149.795278,61.22621699999999, -149.791707,61.226262, -149.791707,61.223276
				}


		};

		public double[][] line60 = {
				{-149.863945,61.130337,
					-149.863895,61.126376,
					-149.863895,61.125316,
					-149.863718,61.123085,
					-149.863769,61.120964,
					-149.863895,61.115845,
					-149.874019,61.115833,
					-149.878842,61.115833,
					-149.878792,61.109921,
					-149.878741,61.10362,
					-149.878085,61.10362,
					-149.877555,61.103742,
					-149.877176,61.103364,
					-149.876822,61.103242,
					-149.875459,61.103059,
					-149.874651,61.102937,
					-149.874272,61.102864,
					-149.873742,61.10262,
					-149.873237,61.102474,
					-149.872505,61.10245,
					-149.871142,61.102133,
					-149.8694,61.101755,
					-149.868162,61.101474,
					-149.867607,61.101438,
					-149.866976,61.101438,
					-149.865713,61.101292,
					-149.865461,61.101279,
					-149.863971,61.101389,
					-149.859553,61.101389,
					-149.859477,61.100475,
					-149.85925,61.099829,
					-149.859123,61.099695,
					-149.858694,61.099585,
					-149.853114,61.099609,
					-149.851625,61.099597,
					-149.862734,61.107764,
					-149.863517,61.108641,
				},

				{
					-149.863517,61.108641,
					-149.856371,61.108629,
					-149.856422,61.109726,
					-149.856775,61.109921,
					-149.858139,61.110153,
					-149.861497,61.110153,
					-149.861194,61.108642,
				},

				{
					-149.891428,61.213622,
					-149.891479,61.205256,
					-149.897336,61.20528,
					-149.897791,61.205134,
					-149.897791,61.204794,
					-149.89774,61.20117,
					-149.897942,61.200027,
					-149.897942,61.198714,
					-149.876683,61.198009,
					-149.876734,61.187674,
					-149.876431,61.186993,
					-149.875673,61.18619,
					-149.875673,61.185777,
					-149.875572,61.18439,
					-149.8682,61.184439,
					-149.868149,61.173665,
					-149.868099,61.166978,
					-149.867796,61.166273,
					-149.866937,61.165105,
					-149.864917,61.163014,
					-149.864312,61.162309,
					-149.864211,61.16046,
					-149.86411,61.158441,
					-149.864211,61.14723,
					-149.864059,61.144701,
					-149.871634,61.14475,
					-149.871684,61.143875,
					-149.872442,61.143875,
					-149.872391,61.14334,
					-149.871886,61.141127,
					-149.864009,61.141103,
					-149.863958,61.139376

				},
				{
					-149.897639,61.216613,
					-149.887389,61.216564,
					-149.887288,61.213573,
					-149.897538,61.213646,
					-149.897538,61.216613
				},

				{
					-149.86411,61.144726,
					-149.864009,61.14103
				},

				{
					-149.863971,61.139442,
					-149.86392,61.135542,
					-149.863945,61.130288
				}

		};

		public double[][] line75 = {
				{
					-149.733494,61.228746,-149.733494,61.22976899999999,-149.73396,61.230443,-149.734712,61.230784,-149.737323,61.231018,-149.7414627478697,61.23118225208985,-149.7416962359376,61.23035239943525,-149.7414607880745,61.22967675806887,-149.7418435002887,61.22949248011324
				},
				{
					-149.903618,61.217622,-149.88326,61.21757999999999,-149.883437,61.210973,-149.883083,61.208842,-149.883171,61.205859,-149.883348,61.204367,-149.883171,61.20364200000001,-149.882817,61.199934,-149.883083,61.19776,-149.883171,61.194265,-149.88326,61.188809,-149.883083,61.18753000000001,-149.88441,61.18637900000001,-149.886004,61.18501600000001,-149.886358,61.183652,-149.886181,61.180881,-149.882375,61.180839,-149.8606,61.180881,-149.778636,61.180711,-149.748453,61.180796,-149.748541,61.188085,-149.733052,61.187957,-149.733583,61.195416,-149.733494,61.228746
				},
				{
					-149.903684,61.217565,-149.903684,61.216584,-149.887398,61.216627,-149.887486,61.207844,-149.887398,61.20605299999999,-149.887221,61.202471,-149.887132,61.199487,-149.886955,61.19825,-149.886601,61.196588,-149.88669,61.195223,-149.88669,61.187165,-149.886336,61.183029
				},
				{
					-149.7414570662942,61.23118344462529,-149.7420406468548,61.23120458565209,-149.7418957732239,61.23181164612119,-149.7413799167596,61.23237654634806,-149.7443979413702,61.23253369327156,-149.7452464014962,61.23223801704003,-149.7445500900076,61.23196484750843,-149.7418930049435,61.23181059198334
				}
		};

		public double[][] line102 = {
				{
					-149.4438804293913,61.41090884032242, -149.443102,61.410513, -149.442728,61.41019000000001, -149.441408,61.40874800000001, -149.441925,61.408652, -149.442728,61.408597, -149.443862,61.408611, -149.444594,61.40861799999999, -149.444723,61.40861799999999, -149.445929,61.408439, -149.448096,61.407897, -149.450478,61.407354, -149.451512,61.407114, -149.45266,61.40675, -149.453794,61.406359, -149.454626,61.40596, -149.455358,61.40547299999999, -149.456305,61.404676, -149.459103,61.402182, -149.460337,61.40109700000001, -149.461571,61.400012, -149.462174,61.399531, -149.462433,61.398968, -149.462533,61.398648, -149.462526,61.39814299999999, -149.462503,61.39786900000001, -149.463283,61.397964, -149.463958,61.39810799999999, -149.464618,61.398349, -149.465192,61.398747, -149.465508,61.399098, -149.466584,61.400499, -149.467129,61.401365, -149.468342,61.40323099999999
				},

				{
					-149.462501,61.397874, -149.462501,61.397843, -149.462493,61.397744, -149.462386,61.396522, -149.4623698135163,61.39636788451036, -149.462386,61.396103, -149.462501,61.3958, -149.462774,61.39542200000001, -149.463333,61.394983, -149.463965,61.39449499999999, -149.466663,61.39242, -149.467108,61.392076, -149.467768,61.39154, -149.468256,61.391258, -149.468859,61.391025, -149.469433,61.390867, -149.469892,61.390798, -149.474384,61.390138, -149.476249,61.38985, -149.477483,61.389568, -149.478804,61.38921799999999, -149.480354,61.388717, -149.482047,61.388133, -149.485592,61.386944, -149.488075,61.386112, -149.49162,61.384717, -149.49238,61.38440799999999, -149.493672,61.383748, -149.496054,61.38231200000001, -149.498867,61.38022200000001, -149.499642,61.379494, -149.500732,61.378311, -149.501192,61.377253, -149.501335,61.376882, -149.502082,61.37616700000001, -149.503747,61.37523199999999, -149.507822,61.371273, -149.509487,61.36954, -149.510233,61.368798, -149.511037,61.368331, -149.512845,61.36692799999999, -149.514395,61.365607, -149.5154,61.364652, -149.516476,61.36359900000001, -149.518212,61.36188, -149.519332,61.360903, -149.520451,61.360326, -149.522489,61.359528, -149.52401,61.35868899999999, -149.527167,61.35688699999999, -149.528632,61.356282, -149.535721,61.354425, -149.536812,61.354054, -149.539173,61.353243, -149.540852,61.352672, -149.541569,61.35238999999999, -149.542287,61.352053, -149.542774,61.351771, -149.543743,61.351169, -149.54426,61.350852, -149.552146,61.345935, -149.554535,61.344438, -149.556788,61.343035, -149.559645,61.341253, -149.560664,61.340365, -149.561568,61.339546, -149.562113,61.339037, -149.562414,61.338761, -149.562816,61.33825899999999, -149.563046,61.337846, -149.563175,61.337309, -149.563218,61.33685499999999, -149.563319,61.336415, -149.563419,61.335912, -149.563721,61.33517599999999, -149.564008,61.33478400000001, -149.564309,61.334102, -149.564768,61.333276, -149.564955,61.33294599999999, -149.565227,61.332492, -149.565586,61.33203099999999, -149.566089,61.331515, -149.567007,61.330554, -149.568,61.329532, -149.56847,61.329631, -149.569332,61.329797, -149.569834,61.329835, -149.570329,61.32981399999999, -149.570781,61.329755, -149.571241,61.329631, -149.571678,61.329442, -149.571886,61.32931099999999, -149.572073,61.32913600000001, -149.572546,61.328564, -149.573171,61.327806, -149.573931,61.326973, -149.574369,61.32657, -149.575711,61.325459, -149.576012,61.32520699999999, -149.576356,61.32489, -149.576478,61.324667, -149.576486,61.32446, -149.576421,61.32428399999999, -149.57617,61.32403, -149.575746,61.323803, -149.575402,61.32370999999999, -149.574885,61.323624, -149.574663,61.32361700000001, -149.572962,61.32361400000001, -149.571527,61.323621, -149.570652,61.32362800000001, -149.570494,61.323252, -149.570232,61.32264099999999, -149.570153,61.322448, -149.570124,61.32234500000001, -149.570095,61.322118, -149.570074,61.32186200000001, -149.570095,61.321642, -149.570124,61.32147700000001, -149.570383,61.320809, -149.570928,61.320355, -149.571632,61.32006500000001, -149.57199,61.319962, -149.573834,61.319652, -149.574795,61.31946600000001, -149.575757,61.31928, -149.577623,61.318915, -149.577666,61.318894, -149.577895,61.31922099999999, -149.577881,61.319362, -149.577723,61.31950699999999, -149.577458,61.3196, -149.577214,61.319652, -149.576798,61.319621, -149.576582,61.319541, -149.576374,61.3194, -149.576159,61.31919700000001, -149.575822,61.318798, -149.575551,61.31847, -149.575344,61.31822399999999, -149.575303,61.31817, -149.575233,61.31805, -149.575147,61.317901, -149.575068,61.317748, -149.574968,61.31756699999999, -149.574862,61.317379, -149.574281,61.316294, -149.574193,61.31614200000001, -149.574161,61.31607799999999, -149.574154,61.316119, -149.574158,61.316214, -149.574161,61.316372, -149.574172,61.316961, -149.574228,61.317951, -149.574228,61.318247, -149.574106,61.318602, -149.573804,61.31897099999999, -149.573704,61.31914999999999, -149.57361,61.31947700000001, -149.57356,61.319695
				},

				{
					-149.574161,61.31608099999999, -149.574156,61.316027, -149.574344,61.31477400000001, -149.574731,61.313879, -149.575305,61.313031, -149.575879,61.31215, -149.57654,61.311185, -149.577228,61.310172, -149.57819,61.30883, -149.578936,61.308113, -149.581003,61.306418, -149.582639,61.30508200000001, -149.583658,61.304248, -149.585408,61.302808, -149.585925,61.30236, -149.586542,61.301788, -149.587001,61.301319, -149.587575,61.300651, -149.587963,61.30003799999999, -149.588408,61.299232, -149.58914,61.297123, -149.589513,61.295703, -149.59069,61.292657, -149.590977,61.291926, -149.592326,61.29041000000001, -149.593014,61.289872, -149.593588,61.28938999999999, -149.596143,61.287847, -149.599013,61.286661, -149.602056,61.285779, -149.626051,61.279944, -149.629266,61.278868, -149.632366,61.276965, -149.634145,61.274758, -149.634375,61.27177900000001, -149.634145,61.26982, -149.634834,61.26811, -149.636729,61.266207, -149.643503,61.261352, -149.646315,61.2592, -149.649185,61.25748999999999, -149.652457,61.256083, -149.656074,61.254566, -149.666981,61.250345, -149.677773,61.24607, -149.684604,61.24265, -149.689541,61.240112, -149.691722,61.23898100000001, -149.697635,61.236885, -149.705442,61.234457, -149.715374,61.23087099999999, -149.734432,61.22689900000001, -149.743846,61.224968, -149.745482,61.224678, -149.74686,61.224581, -149.749042,61.224581, -149.753591,61.224563, -149.754323,61.224332, -149.755241,61.22411799999999, -149.755815,61.22374499999999, -149.756016,61.223082, -149.75593,61.222806, -149.756332,61.22282, -149.756676,61.222903, -149.756791,61.223068, -149.756849,61.223704, -149.757136,61.223939, -149.757738,61.22414600000001, -149.758226,61.22416, -149.763837,61.224173, -149.76909,61.22418699999999, -149.776495,61.224159, -149.776854,61.224111, -149.777342,61.223945, -149.777557,61.22369000000001, -149.777615,61.2236, -149.777586,61.222563, -149.777658,61.222467, -149.777887,61.22239100000001, -149.77839,61.222405, -149.778418,61.224249, -149.7837,61.22441499999999, -149.78545,61.22464299999999, -149.778433,61.22541, -149.778418,61.226288, -149.777199,61.226281, -149.776381,61.22617, -149.775764,61.225977, -149.774142,61.22535500000001, -149.77341,61.22512, -149.77252,61.224982, -149.771515,61.224892, -149.768889,61.224726, -149.767726,61.22454699999999, -149.758298,61.22454699999999, -149.755661,61.22457399999999, -149.753584,61.224567
				},

				{
					-149.785417,61.224647, -149.785529,61.224636, -149.789973,61.22453, -149.791667,61.224468, -149.793145,61.224344, -149.796991,61.223612, -149.800794,61.222845, -149.804612,61.222023, -149.818949,61.218472, -149.822565,61.217629, -149.823684,61.217518, -149.834922,61.217554, -149.843963,61.217547, -149.849531,61.217547, -149.851641,61.217547, -149.857625,61.217554, -149.859936,61.217554, -149.869278,61.217561, -149.878836,61.217554, -149.903729,61.217568, -149.903735,61.216581, -149.897294,61.216591, -149.862836,61.216563, -149.862119,61.216598, -149.861616,61.216702, -149.860956,61.21695700000001, -149.860253,61.217324, -149.859492,61.21751699999999, -149.858799,61.21755499999999
				},

				{
					-149.8832615735888,61.21752796330087, -149.883268,61.216584, -149.883268,61.216066, -149.883276,61.215596, -149.883275,61.213927, -149.883433,61.211716, -149.883376,61.21099, -149.883189,61.209704, -149.883175,61.209698, -149.883003,61.20839099999999, -149.883103,61.20691299999999, -149.883319,61.205316, -149.883347,61.204369, -149.883204,61.203443, -149.883132,61.20276499999999, -149.883046,61.202053, -149.882974,61.201259, -149.882988,61.199939, -149.88306,61.198245, -149.883132,61.194174, -149.883103,61.191112, -149.883117,61.188074, -149.886596,61.188073, -149.88658,61.191888, -149.886623,61.194529, -149.886608,61.196051, -149.886651,61.196854, -149.886852,61.198009, -149.886981,61.198631, -149.887268,61.200388, -149.887311,61.200789, -149.887297,61.202415, -149.887311,61.204047, -149.88734,61.205444, -149.887355,61.206848, -149.887383,61.21159200000001, -149.88734,61.21238, -149.887369,61.21506400000001, -149.887351,61.21658299999999
				},

				{
					-149.883166,61.188076, -149.882549,61.188073, -149.838221,61.188042, -149.829152,61.188056, -149.827286,61.188042, -149.826597,61.188097, -149.825047,61.188388, -149.82387,61.188803, -149.822837,61.18934300000001, -149.822004,61.18959199999999, -149.820942,61.18978500000001, -149.819852,61.189827, -149.817182,61.189827, -149.816264,61.18978500000001, -149.81569,61.189661, -149.810437,61.188679, -149.810466,61.186963, -149.81038,61.186659, -149.810093,61.186202, -149.80949,61.185607, -149.808514,61.184611, -149.808256,61.184182, -149.808141,61.18373900000001, -149.808141,61.18329599999999, -149.807223,61.183269, -149.806132,61.183573, -149.80507,61.183712, -149.804266,61.183684, -149.803233,61.183476, -149.802286,61.183227, -149.802196,61.183201, -149.802028,61.183312, -149.801712,61.183435, -149.801066,61.183584, -149.800047,61.18361800000001, -149.79844,61.183611, -149.797378,61.183542, -149.796129,61.183438, -149.795411,61.183238, -149.794981,61.182774, -149.79501,61.182421, -149.795225,61.18202700000001, -149.795311,61.18183999999999, -149.797177,61.181944, -149.798109,61.181972, -149.800019,61.18193, -149.802286,61.181827, -149.802587,61.181827, -149.803247,61.181951, -149.803371,61.181979, -149.802521,61.182907, -149.802196,61.183201
				},

				{
					-149.589124,61.297182, -149.588902,61.29807400000001, -149.588608,61.299049, -149.588608,61.299049, -149.58817,61.300597, -149.587998,61.301231, -149.587567,61.30277399999999, -149.587137,61.30429, -149.586792,61.305324, -149.586477,61.305958, -149.585831,61.30678500000001, -149.585142,61.30748799999999, -149.584166,61.30821899999999, -149.582946,61.308929, -149.581956,61.309418, -149.58022,61.310176, -149.57759,61.311351, -149.576528,61.311916, -149.57591,61.312406, -149.575373,61.312949, -149.57531,61.313023
				},

				{
					-149.4413962154056,61.40874652779439, -149.4380091711,61.41005841753738, -149.4319847573008,61.41192043898595, -149.4302944876997,61.41197127958203, -149.4286974228384,61.4116789262129, -149.4276698315976,61.4117685904416, -149.4275216323501,61.41741863158474, -149.4256610359589,61.41912216386902, -149.4230706563832,61.42055499887868, -149.4275877427053,61.42158545115025, -149.4299091535999,61.41981054098947, -149.4335251300912,61.4181329042357, -149.4377482204323,61.41529045249995, -149.4418233717275,61.41179508138328, -149.4438824235703,61.41090835136991
				}

		};

		public List<double[][]> l =
				Arrays.asList(line1, line2, line3, line7, line8, line9, line13, line14, line15, line36, line45, line60, line75, line102);


	}

}


