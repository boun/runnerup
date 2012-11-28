package org.runnerup.export;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

import org.json.JSONException;
import org.json.JSONObject;
import org.runnerup.export.format.RunKeeper;
import org.runnerup.export.oauth2client.OAuth2Activity;
import org.runnerup.export.oauth2client.OAuth2Server;
import org.runnerup.util.Constants.DB;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;

public class RunKeeperUploader extends FormCrawler implements Uploader, OAuth2Server {

	/**
	 * @todo register OAuth2Server
	 */
	public static final String CLIENT_ID = "1f4fc840938f46278a9d019db93a8538";
	public static final String CLIENT_SECRET = "ffbba1f04b75476cabfb4d87ef54e250";

	public static final String AUTH_URL = "https://runkeeper.com/apps/authorize";
	public static final String TOKEN_URL = "http://runkeeper.com/apps/token";
	public static final String REDIRECT_URI = "http://localhost:8080/isohealth/index.html";

	public static final String REST_URL = "http://api.runkeeper.com";

	private long id = 0;
	private String authToken = null;
	private String fitnessActivitiesUrl = null;

	RunKeeperUploader() {
	}

	@Override
	public String getClientId() {
		return CLIENT_ID;
	}

	@Override
	public String getRedirectUri() {
		return REDIRECT_URI;
	}

	@Override
	public String getClientSecret() {
		return CLIENT_SECRET;
	}

	@Override
	public String getAuthUrl() {
		return AUTH_URL;
	}

	@Override
	public String getTokenUrl() {
		return TOKEN_URL;
	}

	@Override
	public String getRevokeUrl() {
		return null;
	}

	@Override
	public long getId() {
		return id;
	}

	@Override
	public String getName() {
		return "RunKeeper";
	}

	@Override
	public AuthMethod getAuthMethod() {
		return Uploader.AuthMethod.OAUTH2;
	}

	@Override
	public void init(ContentValues config) {
		authToken = config.getAsString(DB.ACCOUNT.AUTH_CONFIG);
		id = config.getAsLong("_id");
	}

	@Override
	public boolean isConfigured() {
		if (authToken == null)
			return false;
		return true;
	}

	@Override
	public Intent configure(Activity activity) {
		return OAuth2Activity.getIntent(activity, this);
	}

	@Override
	public void reset() {
		authToken = null;
	}

	@Override
	public Uploader.Status login() {
		if (isConfigured()) {
			if (this.fitnessActivitiesUrl != null) {
				return Uploader.Status.OK;
			}

			/**
			 * Get the fitnessActivities end-point
			 */
			String uri = null;
			HttpURLConnection conn = null;
			Exception ex = null;
			try {
				URL newurl = new URL(REST_URL + "/user");
				conn = (HttpURLConnection) newurl.openConnection();
				conn.setRequestProperty("Authorization", "Bearer " + authToken);
				InputStream in = new BufferedInputStream(conn.getInputStream());
				uri = new JSONObject(new Scanner(in).useDelimiter("\\A").next())
						.getString("fitness_activities");
			} catch (MalformedURLException e) {
				ex = e;
			} catch (IOException e) {
				ex = e;
			} catch (JSONException e) {
				ex = e;
			}

			if (conn != null) {
				conn.disconnect();
			}

			if (ex != null)
				ex.printStackTrace();

			if (uri != null) {
				fitnessActivitiesUrl = uri;
				return Uploader.Status.OK;
			}
			Uploader.Status s = Uploader.Status.ERROR;
			s.ex = ex;
			return s;
		}
		return Uploader.Status.INCORRECT_USAGE;
	}

	@Override
	public Uploader.Status upload(SQLiteDatabase db, final long mID) {
		/**
		 * Get the fitnessActivities end-point
		 */
		HttpURLConnection conn = null;
		Exception ex = null;
		try {
			URL newurl = new URL(REST_URL + fitnessActivitiesUrl);
			System.err.println("url: " + newurl.toString());
			conn = (HttpURLConnection) newurl.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.addRequestProperty("Authorization", "Bearer " + authToken);
			conn.addRequestProperty("Content-type",
					"application/vnd.com.runkeeper.NewFitnessActivity+json");
			RunKeeper rk = new RunKeeper(db);
			BufferedWriter w = new BufferedWriter(new OutputStreamWriter(
					conn.getOutputStream()));
			rk.export(mID, w);
			w.flush();
			int responseCode = conn.getResponseCode();
			String amsg = conn.getResponseMessage();
			conn.disconnect();
			conn = null;
			if (responseCode >= 200 && responseCode < 300) {
				return Uploader.Status.OK;
			}
			ex = new Exception(amsg);
		} catch (MalformedURLException e) {
			ex = e;
		} catch (IOException e) {
			ex = e;
		}

		if (ex != null)
			ex.printStackTrace();

		if (conn != null) {
			conn.disconnect();
		}
		Uploader.Status s = Uploader.Status.ERROR;
		s.ex = ex;
		return s;
	}

	@Override
	public void logout() {
		this.fitnessActivitiesUrl = null;
	}
};