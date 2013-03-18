package com.seansouthern.anchoragebustimes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SimpleCursorAdapter;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Activity;
import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.widget.TabHost.TabContentFactory;

import com.seansouthern.anchoragebustimes.R;

// The main activity, it extends TabActivity for the tab functionality
// and some shortcut functions like getTabHost()
public class main extends TabActivity {

	// These three variables are required for the initial database setup.
	MySQLiteHelper myDbHelper = null;
	Cursor cursor = null;
	static SQLiteDatabase db = null;

	// These vars are used to direct tab switching behavior
	public static String ROUTE_NUM = "0";
	public static String STOP_NUM = "0";
	public static String TAB_FLAG = "";
	public static boolean FAV_FLAG = false;

	// A Regex pattern that looks for any amount of numbers at the beginning of a string
	public static Pattern p = Pattern.compile("^\\d+[C|N|A|J]?");

	// Here we are creating tabs and defining navigation.
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);

		// This pulls the layout xml file named 'main' and presents it
		// This is a required function, everything else alters this pulled layout
		setContentView(R.layout.main);

		final TabHost tabHost = getTabHost();

		TabHost.TabSpec spec;

		// This is used as the content of the Map tab
		Intent mapIntent = new Intent().setClass(this, map.class);


		// Setup Bus Times tab, add it to TabHost
		spec = tabHost.newTabSpec("busTimes").setIndicator(createTabView(this, "Bus Times")
				).setContent(new TabContentFactory(){
					public View createTabContent(String arg0){
						RouteList routeList = new RouteList(main.this, getPreferences(MODE_PRIVATE));
						return routeList;
					}
				});
		tabHost.addTab(spec);


		// Setup Favorite Stops tab, add it to TabHost
		spec = tabHost.newTabSpec("favStops").setIndicator(createTabView(this, "Favorites")
				).setContent(new TabContentFactory(){
					public View createTabContent(String arg0){
						FavoritesList favsList = new FavoritesList(main.this, getPreferences(MODE_PRIVATE));
						return favsList;
					}
				});
		tabHost.addTab(spec);

		// Setup Maps tab, add it to TabHost
		spec = tabHost.newTabSpec("mapActivity").setIndicator(createTabView(getBaseContext(), "Map")).setContent(mapIntent);
		tabHost.addTab(spec);

		// Tab Navigation! This was a painful chunk of code to hammer out. 
		// It's basically a big flow chart dictating what clicking a tab will do under 
		// certain circumstances. Tab Switching was sending you to unintuitive places 
		// and this solves that. Note the use of FLAGs and view tags.
		// TODO: Rewrite tab navigation if tree into more readable switch statements
		tabHost.setOnTabChangedListener(new OnTabChangeListener(){
			public void onTabChanged(String tabId) {
				if(tabId == "favStops"){
					main.FAV_FLAG = false;
					if(tabHost.findViewWithTag("StopsList") != null){
						tabHost.findViewWithTag("StopsList").setVisibility(View.GONE);
					}
					if(tabHost.findViewWithTag("TimesTable") != null){
						tabHost.findViewWithTag("TimesTable").setVisibility(View.GONE);
					}
					if(tabHost.findViewWithTag("RouteList") != null){
						tabHost.findViewWithTag("RouteList").setVisibility(View.GONE);
					}
					((FavoritesList) tabHost.findViewWithTag("FavoritesList")).grabFavorites(getPreferences(MODE_PRIVATE));
				}
				if(tabId == "busTimes"){
					if(tabHost.findViewWithTag("FavoritesList") != null){
						tabHost.findViewWithTag("FavoritesList").setVisibility(View.GONE);
						main.FAV_FLAG = false;
					}
					if(TAB_FLAG == "RouteList"){
						RouteList newList = new RouteList(main.this, getPreferences(MODE_PRIVATE));
						tabHost.getTabContentView().removeAllViews();
						tabHost.getTabContentView().addView(newList);
						tabHost.getTabContentView().invalidate();
					}
					if(TAB_FLAG == "StopsList" || TAB_FLAG == "TimesTable"){
						if(FAV_FLAG = true){}
						StopsList newList = new StopsList(main.this, ROUTE_NUM, getPreferences(MODE_PRIVATE));
						tabHost.getTabContentView().removeAllViews();
						tabHost.getTabContentView().addView(newList);
						tabHost.getTabContentView().invalidate();
					}
				}
				if(tabId == "mapActivity"){
					if(tabHost.findViewWithTag("StopsList") != null){
						tabHost.findViewWithTag("StopsList").setVisibility(View.GONE);
					}
					if(tabHost.findViewWithTag("TimesTable") != null){
						tabHost.findViewWithTag("TimesTable").setVisibility(View.GONE);
					}
					if(tabHost.findViewWithTag("RouteList") != null){
						tabHost.findViewWithTag("RouteList").setVisibility(View.GONE);
					}
					if(tabHost.findViewWithTag("FavoritesList") != null){
						tabHost.findViewWithTag("FavoritesList").setVisibility(View.GONE);
					}
				}
			}
		});


		// Silly Database Initialization Hoodoo
		// I'm using a neat hack to get a persistent database onto the users device.
		myDbHelper = new MySQLiteHelper(this);
		try {
			myDbHelper.createDatabase();
		} catch (IOException e) {
			e.printStackTrace();
		}
		db = myDbHelper.getReadableDatabase();


		//End onCreate
	}

	// Inflates the main menu accessed by pressing the menu button
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}

	// Menu for Bus Times Tabs
	// Under construction
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.about:
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}


	// Create new Tab Indicator View from layout xml, for style purposes
	public static View createTabView(final Context context, final String text) {
		View view = LayoutInflater.from(context).inflate(R.layout.tabs_bg, null);
		TextView tv = (TextView) view.findViewById(R.id.tabsText);
		tv.setText(text);
		return view;
	}

	// Favorites list context menu builder
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo){
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.favorites_menu, menu);
	}

	// Logic for the Favorite Stops long click contextual Menu
	@Override
	public boolean onContextItemSelected(MenuItem item){
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()) {
		case R.id.delete:
			String listItemText = (String) ((TextView)info.targetView).getText();
			SharedPreferences faves1 = getPreferences(MODE_PRIVATE);
			SharedPreferences.Editor editor1 = faves1.edit();
			editor1.remove(listItemText);
			editor1.commit();
			((FavoritesList) info.targetView.getParent()).grabFavorites(faves1);
			return true;
		case R.id.clear:
			SharedPreferences faves2 = getPreferences(MODE_PRIVATE);
			SharedPreferences.Editor editor2 = faves2.edit();
			editor2.clear();
			editor2.commit();
			((FavoritesList) info.targetView.getParent()).grabFavorites(faves2);
		default:
			return super.onContextItemSelected(item);
		}
	}

	// Dictate Back button navigation and list setup
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event){
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0){
			TabHost tabHost = getTabHost();
			Object childTag = tabHost.getTabContentView().getChildAt(0).getTag();
			if(childTag == "RouteList"){
				finish();
			}
			else if(childTag == "StopsList"){
				if(tabHost.getCurrentTab() == 1){
					super.finish();
				}
				else{
					RouteList newList = new RouteList(this, getPreferences(MODE_PRIVATE));
					tabHost.getTabContentView().removeAllViews();
					tabHost.getTabContentView().addView(newList);
					tabHost.getTabContentView().invalidate();
				}
			}
			else if(childTag == "TimesTable"){
				if(FAV_FLAG == true){
					FAV_FLAG = false;
					tabHost.setCurrentTab(1);
					FavoritesList newList = new FavoritesList(this, getPreferences(MODE_PRIVATE));
					tabHost.getTabContentView().removeAllViews();
					tabHost.getTabContentView().addView(newList);
					tabHost.getTabContentView().invalidate();
					FAV_FLAG = false;
				}
				else{
					TAB_FLAG = "StopsList";
					tabHost.setCurrentTab(0);
					StopsList newList = new StopsList(this, ROUTE_NUM, getPreferences(MODE_PRIVATE));
					tabHost.getTabContentView().removeAllViews();
					tabHost.getTabContentView().addView(newList);
					tabHost.getTabContentView().invalidate();
				}
			}
			else if(childTag == "FavoritesList"){
				super.finish();
			}
			else{
				super.finish();
			}
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	// Cleans up after the database hoodoo, necessary
	// It's Very Important to close the cursor, db and dbhelper in this order.
	@Override
	public void onDestroy() {
		super.onDestroy();
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

}

class RouteList extends ListView{
	public RouteList(Context context, SharedPreferences favStops) {
		super(context);
		this.setClicker(favStops);
		this.setLongClickable(false);
		this.setTag("RouteList");
		main.TAB_FLAG = "RouteList";
		this.grabRoutes();
		this.setVisibility(View.VISIBLE);
	}

	public void grabRoutes(){
		final List<String> routes = Arrays.asList("1 - CROSSTOWN", "2 - LAKE OTIS", "3C - NORTHERN LIGHTS","3N - NORTHERN LIGHTS", "7A - SPENARD", "7J - SPENARD", 
				"8 - NORTHWAY", "9 - ARCTIC", "13 - SR CTR HOSPITALS UAA", "14 - GOVT HILL", "15 - 15TH AVE/DEBARR", 
				"36 - 36TH AVE/WEST ANCH", "45 - MOUNTAIN VIEW", "60 - OLD SEWARD", "75 - TUDOR", "102 - EAGLE RIVER EXPRESS");

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(), R.layout.list_item, routes);
		this.setAdapter(adapter);
	}

	public void setClicker(final SharedPreferences favStops){
		this.setOnItemClickListener(new OnItemClickListener(){
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
				String item = (String) ((TextView)view).getText();
				Matcher m = main.p.matcher(item);
				if (m.find()) {
					final String num = m.group(0);
					if(m.group(0) != null){
						main.ROUTE_NUM = num;
						StopsList newList = new StopsList(getContext(), num, favStops);
						TabHost tabHost = ((TabActivity) parent.getContext()).getTabHost();
						tabHost.getTabContentView().removeAllViews();
						tabHost.getTabContentView().addView(newList);
						tabHost.getTabContentView().invalidate();
						parent.setVisibility(View.GONE);
					}
				}
			}
		});
	}

}

class StopsList extends ListView{
	public StopsList(Context context, String routeNum, SharedPreferences favStops) {
		super(context);
		this.setClicker();
		this.setLongClicker(favStops);
		this.setTag("StopsList");
		this.grabStops(routeNum);
		this.setVisibility(View.VISIBLE);
		main.TAB_FLAG = "StopsList";
		main.ROUTE_NUM = routeNum;
	}

	public void grabStops(String routeNum) {
		String subRouteFlag = null;

		if(routeNum.equals("7A")){
			routeNum = "7";
			subRouteFlag = "7A";
		}
		else if(routeNum.equals("7J")){
			routeNum = "7";
			subRouteFlag = "7J";
		}
		else if(routeNum.equals("3C")){
			routeNum = "3";
			subRouteFlag = "3C";
		}
		else if(routeNum.equals("3N")){
			routeNum = "3";
			subRouteFlag = "3N";
		}

		String where = null;
		String[] columns = {"_id", "num", "addr", "routes"};
		
		if(routeNum.equals("7") || routeNum.equals("3")){
			String[] dualDirections = { "", "" };
			if(subRouteFlag.equals("7A")){
				dualDirections[0] = "7A DOWNTOWN";
				dualDirections[1] = "7A DIMOND CENTER";
			}
			else if(subRouteFlag.equals("7J")){
				dualDirections[0] = "7J DOWNTOWN";
				dualDirections[1] = "7J DIMOND CENTER";
			}
			else if(subRouteFlag.equals("3C")){
				dualDirections[0] = "3C DOWNTOWN";
				dualDirections[1] = "3C CENTENNIAL";
			}
			else if(subRouteFlag.equals("3N")){
				dualDirections[0] = "3N DOWNTOWN";
				dualDirections[1] = "3N MULDOON";
			}
			
			String[] dirColumns = {"_id", "num", "route", "direction"};
			String dirWhere = "route LIKE '" + routeNum + "' AND direction LIKE '%, " + dualDirections[0] + ",%' OR direction LIKE '%, " + dualDirections[1] + ",%'";
			Cursor cursor = main.db.query("directions", dirColumns, dirWhere, null, null, null, null);
			cursor.moveToFirst();
			List<String> stopNumberList = new ArrayList<String>();
			for(int i = 0; i < cursor.getCount(); i++){
				int stopNum = cursor.getInt(cursor.getColumnIndex("num"));
				stopNumberList.add(String.valueOf(stopNum));
				cursor.moveToNext();
			}

			for(int j = 0; j < stopNumberList.size(); j++){
				if(j == 0){
					where = "num LIKE '" + stopNumberList.get(j) + "'";
				}
				else{
					where = where.concat(" OR num LIKE '" + stopNumberList.get(j) + "'");
				}
			}
		}

		else{
			where = "routes LIKE '%, " + routeNum + ",%'";
		}
		
		Cursor cursor = main.db.query("stops", columns, where, null, null, null, null);
		String[] from = new String[] {"num", "addr"};
		int[] to = new int[] {R.id.list_item, R.id.list_item};
		
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(getContext(), R.layout.list_item, cursor, from, to);
		adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
			public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
				String num = cursor.getString(cursor.getColumnIndex("num"));
				String addr = cursor.getString(cursor.getColumnIndex("addr"));
				String combined = num + " Addr: " + addr;
				TextView tv = (TextView) view;
				tv.setText(combined);
				return true;
			}
		});
		this.setAdapter(adapter);
	}

	public void setClicker(){
		this.setOnItemClickListener(new OnItemClickListener(){
			public void onItemClick(AdapterView<?> parent, View view, int position, long id){
				String item = (String) ((TextView)view).getText();
				Matcher m = main.p.matcher(item);
				if (m.find()) {
					item = m.group(0);
				}
				if(item != null){
					main.FAV_FLAG = false;
					main.STOP_NUM = item;
					TableLayout timesTable = new TimesTable(getContext(), item);
					ScrollView sv = new ScrollView(getContext());
					sv.addView(timesTable);
					sv.setTag("TimesTable");
					TabHost tabHost = ((TabActivity) parent.getContext()).getTabHost();
					tabHost.getTabContentView().removeAllViews();
					tabHost.getTabContentView().addView(sv);
					tabHost.getTabContentView().invalidate();
					parent.setVisibility(View.GONE);
				}
			}
		});
	}

	public void setLongClicker(final SharedPreferences favStops){
		this.setOnItemLongClickListener(new OnItemLongClickListener(){
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id){
				String item = (String) ((TextView) view).getText();
				if(item != null) {
					SharedPreferences.Editor editor = favStops.edit();			                       
					Cursor cursor = (Cursor) parent.getItemAtPosition(position);
					String stopName = cursor.getInt(cursor.getColumnIndex("num")) + " - " + 
							cursor.getString(cursor.getColumnIndex("addr"));
					editor.putString(stopName, stopName);
					editor.commit();
					Toast.makeText(getContext(), stopName + " added to Favorites", Toast.LENGTH_SHORT).show();
				}
				return true;
			}
		});
	}

}

class FavoritesList extends ListView{
	public FavoritesList(Context context, SharedPreferences stops){
		super(context);
		this.grabFavorites(stops);
		((Activity) context).registerForContextMenu(this);
		this.setClicker();
		this.setTag("FavoritesList");
		this.setVisibility(View.VISIBLE);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void grabFavorites(SharedPreferences stops){
		Collection<?> mapFin = stops.getAll().values();
		final ArrayList<String> favStops = new ArrayList(mapFin);
		ArrayAdapter<String> fsAdapter = new ArrayAdapter<String>(getContext(), R.layout.list_item, favStops);
		this.setAdapter(fsAdapter);
	}

	public void setClicker(){
		this.setOnItemClickListener(new OnItemClickListener(){
			public void onItemClick(AdapterView<?> parent, View view, int position, long id){
				String item = (String) ((TextView)view).getText();
				Matcher m = main.p.matcher(item);
				if (m.find()) {
					item = m.group(0);
				}
				if(item != null){
					main.FAV_FLAG = true;
					TableLayout timesTable = new TimesTable(getContext(), item);
					ScrollView sv = new ScrollView(getContext());
					sv.addView(timesTable);
					sv.setTag("TimesTable");
					TabHost tabHost = ((TabActivity) parent.getContext()).getTabHost();
					tabHost.setCurrentTab(0);
					tabHost.getTabContentView().removeAllViews();
					tabHost.getTabContentView().addView(sv);
					tabHost.getTabContentView().invalidate();
					parent.setVisibility(View.GONE);
					main.FAV_FLAG = true;
				}
			}
		});
	}

}

class TimesLoader extends AsyncTask<String, Integer, Document>{
	Context context;
	TimesTable timesTable;
	ProgressBar waitSpinner;

	public TimesLoader(Context context, TimesTable timesTable) {
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
		else if(values[0] == -1){
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

class TimesTable extends TableLayout{
	public TimesTable(Context context, String stopNum) {
		super(context);
		super.setTag("TimesTable");
		if(main.FAV_FLAG == false){
			main.TAB_FLAG = "TimesTable";
		}
		else{
			main.FAV_FLAG = false;
		}

		makeHeader(stopNum);
		setVisibility(View.VISIBLE);

		grabTimes(stopNum);
		setVisibility(View.VISIBLE);

	}

	public void makeHeader(String stopNum){
		this.removeAllViews();
		this.setStretchAllColumns(true);

		String[] columns = {"_id", "num", "addr"};
		String where = "num LIKE '" + stopNum + "'";
		Cursor cursor = main.db.query("stops", columns, where, null, null, null, null);
		cursor.moveToFirst();
		String addr = cursor.getString(cursor.getColumnIndex("addr"));

		TextView headerStopNum = new TextView(getContext());
		headerStopNum.setText("Stop " + stopNum);
		this.addView(headerStopNum);

		TextView headerAddr = new TextView(getContext());
		headerAddr.setText(addr);
		this.addView(headerAddr);

		View spacerThick = new View(getContext());
		spacerThick.setBackgroundColor(Color.LTGRAY);
		spacerThick.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, 4));
		this.addView(spacerThick);

	}

	public void grabTimes(String stopNum){
		TimesLoader timesLoader = new TimesLoader(getContext(), this);
		timesLoader.execute(stopNum);
	}

}