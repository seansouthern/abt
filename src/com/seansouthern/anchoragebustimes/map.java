package com.seansouthern.anchoragebustimes;

import java.io.IOException;
import java.util.ArrayList;
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

	DirectionsMap DirectionsMapClass = new DirectionsMap();
	Map<String, List<String>> DirectionsMap = DirectionsMapClass.directionsMap;
	LineCoords lc = new LineCoords();
	public static String route = null;

	MySQLiteHelper myDbHelper = null;
	Cursor cursor = null;
	SQLiteDatabase db = null;
	OverlayManager overlayManager;
	ManagedOverlay stopOverlay;
	ManagedOverlay busOverlay;
	TimerTask BusRefreshTimerTask;

	private Projection projection;

	public static String REGEX_PATTERN = "^\\d+";
	public static Pattern p = Pattern.compile(REGEX_PATTERN);

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.map);

		MapView mapView = (MapView) findViewById(R.id.mapview);
		mapView.setBuiltInZoomControls(true);

		SharedPreferences sp = getPreferences(MODE_PRIVATE);
		route = sp.getString("LAST_MAP_ROUTE", "1");

		Spinner spinner = (Spinner) findViewById(R.id.spinner);
		ArrayAdapter<CharSequence> SpinnerAdapter = ArrayAdapter.createFromResource(
				this, R.array.routes_array, android.R.layout.simple_spinner_item);
		SpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(SpinnerAdapter);

		final TextView leftButton = (TextView) findViewById(R.id.left_button);
		leftButton.setText(DirectionsMap.get(route).get(0));

		final TextView rightButton = (TextView) findViewById(R.id.right_button);
		rightButton.setText(DirectionsMap.get(route).get(1));

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
		stopOverlay.addAll(grabStopCoordsByDirection(route, DirectionsMap.get(route).get(0)));
		stopOverlay.setOnOverlayGestureListener(mogDetector);
		overlayManager.populate();

		busOverlay = overlayManager.createOverlay("route" + route + "bus", getResources().getDrawable(R.drawable.busmarker));
		ManagedOverlay.boundToCenter(getResources().getDrawable(R.drawable.busmarker));
		busOverlay.addAll(grabBusCoords(route));
		toCallAsync(route);
		overlayManager.populate();
		
		List<Overlay> mapOverlays = ((MapView) findViewById(R.id.mapview)).getOverlays();
		mapOverlays.add(new LineOverlay(route));
		((MapView) findViewById(R.id.mapview)).invalidate();
		overlayManager.populate();

		projection = mapView.getProjection();

	}

	@Override
	public void onPause(){
		super.onPause();
		BusRefreshTimerTask.cancel();

		// Save the route the user is on in order to display it on the next run
		SharedPreferences LastRoute = getPreferences(MODE_PRIVATE);
		SharedPreferences.Editor editor = LastRoute.edit();
		editor.putString("LAST_MAP_ROUTE", route);
		editor.commit();
	}

	@Override
	public void onResume(){
		super.onResume();
		toCallAsync(route);
	}

	public void toCallAsync(final String routeNum) {
		final Handler handler = new Handler();
		Timer timer = new Timer();
		if(BusRefreshTimerTask != null){
			BusRefreshTimerTask.cancel();
		}
		BusRefreshTimerTask = new TimerTask() {
			@Override
			public void run() {
				handler.post(new Runnable(){
					public void run() {
						try {
							BusRefresher performBackgroundTask = new BusRefresher();
							performBackgroundTask.execute(routeNum);
						}
						catch (Exception e) {
							Log.d("func toCallAsync", e.getMessage());
						}
					}
				});
			}
		};
		timer.schedule(BusRefreshTimerTask, 0,20000);
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
			String SpinnerItem = parent.getItemAtPosition(pos).toString();
			Matcher m = main.p.matcher(SpinnerItem);
			if (m.find()) {
				SpinnerItem = m.group(0);
			}
			Log.d("", "SpinnerItem: " + SpinnerItem + " | " + "route: " + route);
			if(!SpinnerItem.equals(null) && !SpinnerItem.equals(route)){
				overlayManager.removeOverlay(stopOverlay);
				overlayManager.removeOverlay(busOverlay);
				route = SpinnerItem;
				Log.d("", "SpinnerItem: " + SpinnerItem + " | " + "route: " + route);


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
					leftButton.setText(DirectionsMap.get(route).get(0));
					leftButton.setSelected(false);

					TextView rightButton = (TextView) findViewById(R.id.right_button);
					rightButton.setText("");
					rightButton.setSelected(false);
					rightButton.setClickable(false);

				}

				else{
					TextView leftButton = (TextView) findViewById(R.id.left_button);
					leftButton.setText(DirectionsMap.get(route).get(0));
					leftButton.setSelected(false);

					TextView rightButton = (TextView) findViewById(R.id.right_button);
					rightButton.setText(DirectionsMap.get(route).get(1));
					rightButton.setSelected(false);
				}
			}
		}

		public void onNothingSelected(AdapterView<?> parent) {

		}
	}

	@Override
	public void onDestroy(){
		super.onDestroy();

		// Save the route the user is on in order to display it on the next run
		SharedPreferences LastRoute = getPreferences(MODE_PRIVATE);
		SharedPreferences.Editor editor = LastRoute.edit();
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

}


