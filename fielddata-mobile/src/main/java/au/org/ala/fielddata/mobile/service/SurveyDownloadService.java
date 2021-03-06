package au.org.ala.fielddata.mobile.service;

import java.util.concurrent.ThreadPoolExecutor;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import au.org.ala.fielddata.mobile.FieldDataApp;
import au.org.ala.fielddata.mobile.service.FieldDataService.SurveyDownloadCallback;


public class SurveyDownloadService extends IntentService implements SurveyDownloadCallback {

	public static final String PROGRESS_ACTION = "Progress";
	public static final String FINISHED_ACTION = "Finished";
	public static final String NUMBER_EXTRA = "surveyNumber";
    public static final String SPECIES_EXTRA = "speciesForSurveyNumber";

    public static final String COUNT_EXTRA = "count";
    public static final String RESULT_EXTRA = "success";

    // This is a bit yuck, but the alternative is to use a bound service
	// and do my own threading. It's required because if the LoginActivity
	// gets destroyed due to screen rotation it may miss the finished 
	// broadcast so it also needs to check this field on resume.
	private static boolean downloading;
	
	public static synchronized boolean isDownloading() {
		return downloading;
	}
	
	private static synchronized void setDownloading(boolean newDownloading) {
		downloading = newDownloading;
	}
	
	public SurveyDownloadService() {
		super("SurveyDownloadService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (SurveyDownloadService.isDownloading()) {
			return;
		}
        try {
            SurveyDownloadService.setDownloading(true);
            new FieldDataService(this).downloadSurveys(this);


            // At this point the app can function correctly, however we will
            // continue to pre-cache the images.
            notifyFinished(true);

            // Wait for the image download to finish.
            ThreadPoolExecutor executor = ((FieldDataApp)getApplication()).getImageLoaderExecutor();
            int size = executor.getQueue().size();
            while (size > 0) {

                try {
                    Log.i("SurveyDownloadService", "Waiting for image download: "+size);

                    Thread.sleep(30*1000);
                    size = executor.getQueue().size();

                }
                catch(InterruptedException e) {
                    Log.i("SurveyDownloadService", "Interrupted waiting for image download");
                }
            }
            Log.i("SurveyDownloadService", "Image download finished.");
        }
        catch (Exception e) {
            Log.e("SurveyDownloadService", "Survey download failed.",e);
            notifyFinished(false);
        }
        finally {
            SurveyDownloadService.setDownloading(false);
        }

	}
	
	private void notifyFinished(boolean success) {
		Intent finishedIntent = new Intent(FINISHED_ACTION);
        finishedIntent.putExtra(RESULT_EXTRA, success);
		LocalBroadcastManager.getInstance(this).sendBroadcast(finishedIntent);
		
		Log.i("SurveyDownloadService", "Finished download");
	}

	public void surveysDownloaded(int number, int count) {
		
		Intent progressIntent = new Intent(PROGRESS_ACTION);
		progressIntent.putExtra(NUMBER_EXTRA, number);
		progressIntent.putExtra(COUNT_EXTRA, count);
		
		LocalBroadcastManager.getInstance(this).sendBroadcast(progressIntent);
		
		Log.i("SurveyDownloadService", "update...");
	}

    public void speciesDownloaded(int survey) {

        Intent progressIntent = new Intent(PROGRESS_ACTION);
        progressIntent.putExtra(SPECIES_EXTRA, survey);

        LocalBroadcastManager.getInstance(this).sendBroadcast(progressIntent);

        Log.i("SurveyDownloadService", "update...");
    }
	
}
